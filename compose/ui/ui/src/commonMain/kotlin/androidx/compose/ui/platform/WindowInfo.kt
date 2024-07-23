/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.pointer.EmptyPointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

/** Provides information about the Window that is hosting this compose hierarchy. */
@Stable
interface WindowInfo {
    /**
     * Indicates whether the window hosting this compose hierarchy is in focus.
     *
     * When there are multiple windows visible, either in a multi-window environment or if a popup
     * or dialog is visible, this property can be used to determine if the current window is in
     * focus.
     */
    val isWindowFocused: Boolean

    /** Indicates the state of keyboard modifiers (pressed or not). */
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    val keyboardModifiers: PointerKeyboardModifiers
        get() = WindowInfoImpl.GlobalKeyboardModifiers.value

    /**
     * The available area (in pixels) where content can be placed and remain visible to users.
     * It reports the window bounds without all system bar areas relative to the window.
     */
    val availableContentBounds: IntRect
        get() = windowBounds

    /**
     * The bounds of the window in pixels relative to the screen.
     *
     * @see LayoutCoordinates.positionOnScreen
     */
    val windowBounds: IntRect
        get() = throw UnsupportedOperationException(
            "windowBounds is not implemented on this WindowInfo"
        )

    /** Size of the window's content container in pixels. */
    @Deprecated(
        message = "Maintained for binary compatibility",
        replaceWith = ReplaceWith("windowBounds.size"),
        level = DeprecationLevel.HIDDEN
    )
    val containerSize: IntSize
        get() = windowBounds.size
}

@Composable
internal fun WindowFocusObserver(onWindowFocusChanged: (isWindowFocused: Boolean) -> Unit) {
    val windowInfo = LocalWindowInfo.current
    val callback = rememberUpdatedState(onWindowFocusChanged)
    LaunchedEffect(windowInfo) {
        snapshotFlow { windowInfo.isWindowFocused }.collect { callback.value(it) }
    }
}

internal class WindowInfoImpl : WindowInfo {
    private val _isWindowFocused = mutableStateOf(false)
    private val _availableContentBounds = mutableStateOf(IntRect.Zero)
    private val _windowBounds = mutableStateOf(IntRect.Zero)

    override var isWindowFocused: Boolean
        get() = _isWindowFocused.value
        set(value) {
            _isWindowFocused.value = value
        }

    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    override var keyboardModifiers: PointerKeyboardModifiers
        get() = GlobalKeyboardModifiers.value
        set(value) {
            GlobalKeyboardModifiers.value = value
        }

    override var availableContentBounds: IntRect
        get() = _availableContentBounds.value
        set(value) {
            _availableContentBounds.value = value
        }

    override var windowBounds: IntRect
        get() = _windowBounds.value
        set(value) {
            _windowBounds.value = value
        }

    companion object {
        // One instance across all windows makes sense, since the state of KeyboardModifiers is
        // common for all windows.
        internal val GlobalKeyboardModifiers = mutableStateOf(EmptyPointerKeyboardModifiers())
    }
}
