/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Rule

class OnUnplacedModifierTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun onUnPlacedCalledOnReuseInsideLazyColumn() {
        lateinit var density: Density
        val items = 200
        val visibleItems = 2
        val itemSize = 50.dp
        val invocations = arrayOf(0, 0)

        // It's important to share lambda across all iterations
        val placedCallback0: (LayoutCoordinates) -> Unit = { invocations[0] = invocations[0] + 1 }
        val placedCallback1: (LayoutCoordinates) -> Unit = { invocations[1] = invocations[1] + 1 }
        val scrollState = LazyListState()
        rule.setContent {
            density = LocalDensity.current
            LazyColumn(Modifier.size(itemSize, itemSize * visibleItems), scrollState) {
                items(items) {
                    Box(Modifier.size(itemSize).onPlaced2(placedCallback0)) {
                        Box(Modifier.size(itemSize).onPlaced2(placedCallback1))
                    }
                }
            }
        }

        var expectedInvocations = visibleItems
        val delta = with(density) { (itemSize * visibleItems).toPx() }
        repeat(items / visibleItems) {
            rule.runOnIdle {
                assertThat(invocations[0]).isAtLeast(expectedInvocations)
                assertThat(invocations[1]).isAtLeast(expectedInvocations)

                scrollState.dispatchRawDelta(delta)
                expectedInvocations += visibleItems
            }
        }
    }
}


private fun Modifier.onPlaced2(
    onPlaced: (LayoutCoordinates) -> Unit,
    onUnplaced: () -> Unit
) = this then OnPlacedElement(onPlaced, onUnplaced)

private data class OnPlacedElement(
    val onPlaced: (LayoutCoordinates) -> Unit,
    val onUnplaced: () -> Unit
) :
    ModifierNodeElement<OnPlacedNode>() {
    override fun create() = OnPlacedNode(onPlaced, onUnplaced)

    override fun update(node: OnPlacedNode) {
        node.onPlacedCallback = onPlaced
        node.onUnplacedCallback = onUnplaced
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "onPlaced"
        properties["onPlaced"] = onPlaced
        properties["onUnplaced"] = onUnplaced
    }
}

private class OnPlacedNode(
    var onPlacedCallback: (LayoutCoordinates) -> Unit,
    var onUnplacedCallback: () -> Unit
) : LayoutAwareModifierNode, OnUnplacedModifierNode, Modifier.Node() {
    override fun onPlaced(coordinates: LayoutCoordinates) {
        onPlacedCallback(coordinates)
    }

    override fun onUnplaced() {
        onUnplacedCallback()
    }
}
