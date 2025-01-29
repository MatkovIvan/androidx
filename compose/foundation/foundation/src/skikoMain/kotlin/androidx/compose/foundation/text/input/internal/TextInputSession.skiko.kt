/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.content.internal.ReceiveContentConfiguration
import androidx.compose.foundation.text.computeSizeForDefaultText
import androidx.compose.foundation.text.input.setSelectionCoerced
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
internal actual suspend fun PlatformTextInputSession.platformSpecificTextInputSession(
    state: TransformedTextFieldState,
    layoutState: TextLayoutState,
    imeOptions: ImeOptions,
    receiveContentConfiguration: ReceiveContentConfiguration?,
    onImeAction: ((ImeAction) -> Unit)?,
    updateSelectionState: (() -> Unit)?,
    stylusHandwritingTrigger: MutableSharedFlow<Unit>?,
    viewConfiguration: ViewConfiguration?
): Nothing {
    val editProcessor = EditProcessor()
    fun onEditCommand(commands: List<EditCommand>) {
        editProcessor.reset(
            value = with(state.visualText) {
                TextFieldValue(
                    text = toString(),
                    selection = selection,
                    composition = composition
                )
            },
            textInputSession = null
        )

        val newValue = editProcessor.apply(commands)

        state.replaceAll(newValue.text)
        state.editUntransformedTextAsUser {
            val untransformedSelection = state.mapFromTransformed(newValue.selection)
            setSelectionCoerced(untransformedSelection.start, untransformedSelection.end)

            val composition = newValue.composition
            if (composition == null) {
                commitComposition()
            } else {
                val untransformedComposition = state.mapFromTransformed(composition)
                setComposition(untransformedComposition.start, untransformedComposition.end)
            }
        }
    }


    coroutineScope {
        launch {
            state.collectImeNotifications { _, newValue, _ ->
                val newTextFieldValue = TextFieldValue(newValue.text.toString(), newValue.selection, newValue.composition)
                updateSelectionState(newTextFieldValue)
            }
        }

        launch {
            snapshotFlow {
                val layoutResult = layoutState.layoutResult ?: return@snapshotFlow null
                val layoutCoords = layoutState.textLayoutNodeCoordinates ?: return@snapshotFlow null
                focusedRectInRoot(
                    layoutResult = layoutResult,
                    layoutCoordinates = layoutCoords,
                    focusOffset = state.visualText.selection.max,
                    sizeForDefaultText = {
                        layoutResult.layoutInput.let {
                            computeSizeForDefaultText(it.style, it.density, it.fontFamilyResolver)
                        }
                    }

                )
            }.collect { rect ->
                if (rect != null) {
                    notifyFocusedRect(rect)
                }
            }
        }

        startInputMethod(
            SkikoPlatformTextInputMethodRequest(
                state = TextFieldValue(
                    state.visualText.toString(),
                    state.visualText.selection,
                    state.visualText.composition,
                ),
                imeOptions = imeOptions,
                onEditCommand = ::onEditCommand,
                onImeAction = onImeAction,
                editProcessor = editProcessor,
            )
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private data class SkikoPlatformTextInputMethodRequest(
    override val state: TextFieldValue,
    override val imeOptions: ImeOptions,
    override val onEditCommand: (List<EditCommand>) -> Unit,
    override val onImeAction: ((ImeAction) -> Unit)?,
    override val editProcessor: EditProcessor?
): PlatformTextInputMethodRequest

/**
 * Computes the bounds of the area where text editing is in progress, relative to the root.
 */
// Adapted from TextFieldDelegate.notifyFocusedRect
// TODO: Move this function into TextFieldDelegate.kt, and call it from both places.
private fun focusedRectInRoot(
    layoutResult: TextLayoutResult,
    layoutCoordinates: LayoutCoordinates,
    focusOffset: Int,
    sizeForDefaultText: () -> IntSize
): Rect {
    val bbox = when {
        focusOffset < layoutResult.layoutInput.text.length -> {
            layoutResult.getBoundingBox(focusOffset)
        }
        focusOffset != 0 -> {
            layoutResult.getBoundingBox(focusOffset - 1)
        }
        else -> { // empty text.
            val size = sizeForDefaultText()
            Rect(0f, 0f, 1.0f, size.height.toFloat())
        }
    }
    val globalLT = layoutCoordinates.localToRoot(Offset(bbox.left, bbox.top))
    return Rect(Offset(globalLT.x, globalLT.y), Size(bbox.width, bbox.height))
}
