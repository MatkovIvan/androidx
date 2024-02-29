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

package androidx.compose.ui.awt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.InteropContainer
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.TrackInteropContainer
import androidx.compose.ui.node.TrackInteropModifierElement
import androidx.compose.ui.node.TrackInteropModifierNode
import androidx.compose.ui.node.countInteropComponentsBefore
import androidx.compose.ui.scene.ComposeSceneMediator
import androidx.compose.ui.unit.IntRect
import java.awt.Component
import java.awt.Container
import org.jetbrains.skiko.ClipRectangle

/**
 * Providing interop container as composition local, so [SwingPanel] can use it to add
 * native views to the hierarchy.
 */
internal val LocalSwingInteropContainer = staticCompositionLocalOf<SwingInteropContainer> {
    error("LocalSwingInteropContainer not provided")
}

/**
 * A container that controls interop views/components.
 *
 * It receives [container] native view to use it as parent for all interop views. It should be
 * the same component that is used in [ComposeSceneMediator] to avoid issues with transparency.
 *
 * @property container The Swing container to add the interop views to.
 * @property placeInteropAbove Whether to place interop components above non-interop components.
 */
internal class SwingInteropContainer(
    val container: Container,
    private val placeInteropAbove: Boolean
): InteropContainer<InteropComponent> {
    /**
     * @see SwingInteropContainer.addInteropView
     * @see SwingInteropContainer.removeInteropView
     */
    private var interopComponents = mutableMapOf<Component, InteropComponent>()

    override var rootModifier: TrackInteropModifierNode<InteropComponent>? = null

    override fun addInteropView(nativeView: InteropComponent) {
        val component = nativeView.container
        val nonInteropComponents = container.componentCount - interopComponents.size
        // AWT uses the reverse order for drawing and events, so index = size - count
        val index = maxOf(0, interopComponents.size - countInteropComponentsBefore(nativeView))
        interopComponents[component] = nativeView
        container.add(component, if (placeInteropAbove) {
            index
        } else {
            index + nonInteropComponents
        })

        // Sometimes Swing displays the rest of interop views in incorrect order after removing,
        // so we need to force re-validate it.
        container.validate()
        container.repaint()
    }

    override fun removeInteropView(nativeView: InteropComponent) {
        val component = nativeView.container
        container.remove(component)
        interopComponents.remove(component)

        // Sometimes Swing displays the rest of interop views in incorrect order after removing,
        // so we need to force re-validate it.
        container.validate()
        container.repaint()
    }

    fun getClipRectForComponent(component: Component): ClipRectangle =
        requireNotNull(interopComponents[component])

    @Composable
    operator fun invoke(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalSwingInteropContainer provides this,
        ) {
            TrackInteropContainer(
                content = content
            )
        }
    }
}

/**
 * Modifier to track interop component inside [LayoutNode] hierarchy.
 *
 * @param component The Swing component that matches the current node.
 */
internal fun Modifier.trackSwingInterop(
    component: Component
): Modifier = this then TrackInteropModifierElement(
    nativeView = component
)

/**
 * Provides clipping bounds for skia canvas.
 *
 * @param container The container that holds the component.
 * @param clipBounds The rectangular region to clip skia canvas. It's relative to Compose root
 */
internal open class InteropComponent(
    val container: Container,
    var clipBounds: IntRect? = null
) : ClipRectangle {
    override val x: Float
        get() = (clipBounds?.left ?: container.x).toFloat()
    override val y: Float
        get() = (clipBounds?.top ?: container.y).toFloat()
    override val width: Float
        get() = (clipBounds?.width ?: container.width).toFloat()
    override val height: Float
        get() = (clipBounds?.height ?: container.height).toFloat()
}
