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

package androidx.compose.material

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.toIntRect
import kotlin.math.max

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalMaterialApi
@Composable
actual fun ExposedDropdownMenuBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier,
    content: @Composable ExposedDropdownMenuBoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    var width by remember { mutableIntStateOf(0) }
    var menuHeight by remember { mutableIntStateOf(0) }
    val verticalMarginInPx = with(density) { MenuVerticalMargin.roundToPx() }

    val scope =
        remember(density, menuHeight, width) {
            object : ExposedDropdownMenuBoxScope() {
                override fun Modifier.exposedDropdownSize(matchTextFieldWidth: Boolean): Modifier {
                    return with(density) {
                        heightIn(max = menuHeight.toDp()).let {
                            if (matchTextFieldWidth) {
                                it.width(width.toDp())
                            } else it
                        }
                    }
                }
            }
        }
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier
            .onGloballyPositioned {
                width = it.size.width
                val boundsInWindow = it.boundsInWindow()
                val visibleWindowBounds = windowInfo.containerSize.toIntRect()
                val heightAbove = boundsInWindow.top - visibleWindowBounds.top
                val heightBelow = visibleWindowBounds.height - boundsInWindow.bottom
                menuHeight = max(heightAbove, heightBelow).toInt() - verticalMarginInPx
            }
            .expandable(
                onExpandedChange = { onExpandedChange(!expanded) },
                menuLabel = getString(Strings.ExposedDropdownMenu)
            )
            .focusRequester(focusRequester)
    ) {
        scope.content()
    }

    SideEffect { if (expanded) focusRequester.requestFocus() }
}

@Composable
internal actual fun ExposedDropdownMenuBoxScope.ExposedDropdownMenuDefaultImpl(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    scrollState: ScrollState,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.exposedDropdownSize(),
        content = content
    )
}
