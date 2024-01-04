/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.platform.PlatformRootForTest
import androidx.compose.ui.semantics.SemanticsNode

// TODO https://youtrack.jetbrains.com/issue/COMPOSE-742/Merge-1.6.-implement-checkIsDisplayedassertIsFullyVisible
internal actual fun SemanticsNodeInteraction.checkIsDisplayed(
    assertIsFullyVisible: Boolean
): Boolean {
    // hierarchy check - check layout nodes are visible
    val errorMessageOnFail = "Failed to perform isDisplayed check."
    val node = fetchSemanticsNode(errorMessageOnFail)

    fun isNotPlaced(node: LayoutInfo): Boolean {
        return !node.isPlaced
    }

    val layoutInfo = node.layoutInfo
    if (isNotPlaced(layoutInfo) || layoutInfo.findClosestParentNode(::isNotPlaced) != null) {
        return false
    }

    // check node doesn't clip unintentionally (e.g. row too small for content)
    val globalRect = node.boundsInWindow
    if (!node.isInScreenBounds(assertIsFullyVisible)) {
        return false
    }

    return (globalRect.width > 0f && globalRect.height > 0f)
}

internal actual fun SemanticsNode.clippedNodeBoundsInWindow(): Rect {
    return boundsInRoot.translate(Offset(0f, 0f))
}

@OptIn(InternalComposeUiApi::class)
// TODO https://youtrack.jetbrains.com/issue/COMPOSE-742/Merge-1.6.-implement-checkIsDisplayedassertIsFullyVisible
internal actual fun SemanticsNode.isInScreenBounds(assertIsFullyVisible: Boolean): Boolean {
    val platformRootForTest = root as PlatformRootForTest
    val visibleBounds = platformRootForTest.visibleBounds

    // Window relative bounds of our node
    val nodeBoundsInWindow = clippedNodeBoundsInWindow()
    if (nodeBoundsInWindow.width == 0f || nodeBoundsInWindow.height == 0f) {
        return false
    }

    // Window relative bounds of our compose root view that are visible on the screen
    return nodeBoundsInWindow.top >= visibleBounds.top &&
        nodeBoundsInWindow.left >= visibleBounds.left &&
        nodeBoundsInWindow.right <= visibleBounds.right &&
        nodeBoundsInWindow.bottom <= visibleBounds.bottom
}

/**
 * Executes [selector] on every parent of this [LayoutInfo] and returns the closest
 * [LayoutInfo] to return `true` from [selector] or null if [selector] returns false
 * for all ancestors.
 */
private fun LayoutInfo.findClosestParentNode(
    selector: (LayoutInfo) -> Boolean
): LayoutInfo? {
    var currentParent = this.parentInfo
    while (currentParent != null) {
        if (selector(currentParent)) {
            return currentParent
        } else {
            currentParent = currentParent.parentInfo
        }
    }

    return null
}