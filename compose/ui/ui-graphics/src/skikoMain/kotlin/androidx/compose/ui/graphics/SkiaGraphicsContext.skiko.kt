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

package androidx.compose.ui.graphics

import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.layer.GraphicsLayer
import kotlin.js.JsName

/**
 * Create a new [GraphicsContext].
 */
@InternalComposeUiApi
@JsName("createGraphicsContext")
fun GraphicsContext(snapshotObserver: SnapshotStateObserver): GraphicsContext =
    SkiaGraphicsContext(snapshotObserver)

private class SkiaGraphicsContext(
    private val snapshotObserver: SnapshotStateObserver
) : GraphicsContext {
    override fun createGraphicsLayer(): GraphicsLayer {
        return GraphicsLayer(snapshotObserver)
    }

    override fun releaseGraphicsLayer(layer: GraphicsLayer) {
        layer.release()
    }
}
