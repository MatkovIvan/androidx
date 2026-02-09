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

package androidx.compose.ui.viewinterop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.awt.AwtSkiaAdapter
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.scene.ComposeSceneMediator
import java.awt.Component

/**
 * A container that controls interop views/components.
 *
 * It receives [root] native view to use it as parent for all interop views. It should be
 * the same component that is used in [ComposeSceneMediator] to avoid issues with transparency.
 *
 * @property root The Swing container to add the interop views to.
 */
internal class SwingInteropContainer(
    override val root: InteropViewGroup,
    private val skiaAdapter: AwtSkiaAdapter,
) : InteropContainer {

    /**
     * Map to reverse-lookup of [InteropViewHolder] having an [InteropViewGroup].
     */
    private var interopComponents = mutableMapOf<InteropViewGroup, InteropViewHolder>()

    override var rootModifier: TrackInteropPlacementModifierNode? = null

    override val snapshotObserver: SnapshotStateObserver = SnapshotStateObserver { command ->
        command()
    }

    override fun contains(holder: InteropViewHolder): Boolean =
        interopComponents.contains(holder.group)

    override fun holderOfView(view: InteropView): InteropViewHolder? {
        val component = view as Component
        val group = checkNotNull(component.parent) {
            "InteropView is assumed to be added to its group for its entire lifetime"
        }
        return interopComponents[group]
    }

    override fun place(holder: InteropViewHolder) {
        val group = holder.group
        if (interopComponents.isEmpty()) {
            snapshotObserver.start()
        }

        // Note that compose state must be read here, but AWT/Swing state must be read inside
        // scheduleUpdate

        // Add this component to [interopComponents] to track count and clip rects
        val isNewInteropView = interopComponents.putIfAbsent(group, holder) == null

        // [ComposeSceneMediator] might keep extra components in the same container.
        val interopComponentsCount = interopComponents.size

        // Iterate through a Compose layout tree in draw order and count interop view below this one
        val interopComponentsBelowCount = countInteropComponentsBelow(holder)

        // Update AWT/Swing hierarchy
        scheduleUpdate {
            // Based on [placeInteropAbove] interop views should go below or under all interop views
            val lastInteropIndex = interopComponentsCount - 1

            // AWT/Swing uses the **REVERSE ORDER** for drawing and events
            val awtIndex = lastInteropIndex - interopComponentsBelowCount
            if (isNewInteropView) {
                holder.insertInteropView(root = root, index = awtIndex)
            } else {
                holder.changeInteropViewIndex(root = root, index = awtIndex)
            }
        }
    }

    override fun unplace(holder: InteropViewHolder) {
        scheduleUpdate {
            holder.removeInteropView(root = root)
        }

        interopComponents.remove(holder.group)

        if (interopComponents.isEmpty()) {
            snapshotObserver.stop()
        }
    }

    override fun DrawScope.draw(holder: InteropViewHolder, canvas: Canvas) {
        with(skiaAdapter) {
            canvas.nativeCanvas.drawComponent(holder.group)
        }
    }

    fun dispose() {
    }

    override fun scheduleUpdate(action: () -> Unit) {
        action()
    }

    // TODO: Should be the same as [Owner.onInteropViewLayoutChange]?
//    override fun onInteropViewLayoutChange(holder: InteropViewHolder) {
//        // No-op.
//        // On Swing it's called after relayout for specific interop view was requested.
//        // It means that the validate and repaint will be executed after it.
//    }

    @Composable
    operator fun invoke(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalInteropContainer provides this,
        ) {
            TrackInteropPlacementContainer(
                content = content
            )
        }
    }
}
