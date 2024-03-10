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

package androidx.compose.ui.skiko

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PictureRecorder
import org.jetbrains.skiko.SkikoView

internal class RecordDrawRectSkikoViewDecorator(
    private val decorated: SkikoView,
    private val onDrawRectChange: (Rect) -> Unit
) : SkikoView by decorated {
    private val pictureRecorder = PictureRecorder()
    private var drawRect = Rect.Zero
        private set(value) {
            if (value != field) {
                field = value
                onDrawRectChange(value)
            }
        }

    fun close() {
        pictureRecorder.close()
    }

    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        drawRect = canvas.recordCullRect(pictureRecorder) {
            decorated.onRender(it, width, height, nanoTime)
        }.toComposeRect()
    }
}

private inline fun Canvas.recordCullRect(pictureRecorder: PictureRecorder, block: (Canvas) -> Unit): org.jetbrains.skia.Rect {
    val pictureCanvas = pictureRecorder.beginRecording(
        org.jetbrains.skia.Rect.makeLTRB(
            Float.MIN_VALUE, Float.MIN_VALUE, Float.MAX_VALUE, Float.MAX_VALUE
        )
        // TODO: Pass SkRTreeFactory as second parameter (requires skiko fix)
    )
    block(pictureCanvas)
    val picture = pictureRecorder.finishRecordingAsPicture()
    val cullRect = picture.cullRect

    drawPicture(picture, null, null)
    picture.close()

    return cullRect
}
