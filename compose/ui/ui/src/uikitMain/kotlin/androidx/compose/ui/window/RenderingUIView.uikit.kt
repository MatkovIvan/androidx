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

package androidx.compose.ui.window

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.interop.UIKitInteropTransaction
import kotlinx.cinterop.*
import org.jetbrains.skia.Canvas
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLDeviceProtocol
import platform.Metal.MTLPixelFormatBGRA8Unorm
import platform.QuartzCore.CAMetalLayer
import platform.UIKit.*

internal class RenderingUIView(
    private val renderDelegate: Delegate,
    private val transparency: Boolean,
) : UIView(
    frame = CGRectMake(
        x = 0.0,
        y = 0.0,
        width = 1.0, // TODO: Non-zero size need to first render with ComposeSceneLayer
        height = 1.0
    )
) {

    interface Delegate {
        fun retrieveInteropTransaction(): UIKitInteropTransaction
        fun render(canvas: Canvas, targetTimestamp: NSTimeInterval)
    }

    companion object : UIViewMeta() {
        override fun layerClass() = CAMetalLayer
    }

    var onAttachedToWindow: (() -> Unit)? = null
    private val _isReadyToShowContent: MutableState<Boolean> = mutableStateOf(false)
    val isReadyToShowContent: State<Boolean> = _isReadyToShowContent

    private val _device: MTLDeviceProtocol =
        MTLCreateSystemDefaultDevice()
            ?: throw IllegalStateException("Metal is not supported on this system")
    private val _metalLayer: CAMetalLayer get() = layer as CAMetalLayer
    internal val redrawer: MetalRedrawer = MetalRedrawer(
        _metalLayer,
        callbacks = object : MetalRedrawerCallbacks {
            override fun render(canvas: Canvas, targetTimestamp: NSTimeInterval) {
                renderDelegate.render(canvas, targetTimestamp)
            }

            override fun retrieveInteropTransaction(): UIKitInteropTransaction =
                renderDelegate.retrieveInteropTransaction()
        },
        transparency = transparency,
    )

    init {
        userInteractionEnabled = false
        opaque = !transparency

        _metalLayer.also {
            // Workaround for KN compiler bug
            // Type mismatch: inferred type is platform.Metal.MTLDeviceProtocol but objcnames.protocols.MTLDeviceProtocol? was expected
            @Suppress("USELESS_CAST")
            it.device = _device as objcnames.protocols.MTLDeviceProtocol?

            it.pixelFormat = MTLPixelFormatBGRA8Unorm
            doubleArrayOf(0.0, 0.0, 0.0, 0.0).usePinned { pinned ->
                it.backgroundColor =
                    CGColorCreate(CGColorSpaceCreateDeviceRGB(), pinned.addressOf(0))
            }
            it.setOpaque(!transparency)//todo check if remove
            it.framebufferOnly = false
        }
    }

    fun needRedraw() = redrawer.needRedraw()

    var isForcedToPresentWithTransactionEveryFrame by redrawer::isForcedToPresentWithTransactionEveryFrame

    fun dispose() {
        redrawer.dispose()
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()

        window?.screen?.let {
            contentScaleFactor = it.scale
            redrawer.maximumFramesPerSecond = it.maximumFramesPerSecond
        }
        if (window != null) {
            onAttachedToWindow?.invoke()
            _isReadyToShowContent.value = true
        }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        updateMetalLayerSize()
    }

    internal fun updateMetalLayerSize() {
        val scaledSize = bounds.useContents {
            CGSizeMake(size.width * contentScaleFactor, size.height * contentScaleFactor)
        }

        // If drawableSize is zero in any dimension it means that it's a first layout
        // we need to synchronously dispatch first draw and block until it's presented
        // so user doesn't have a flicker
        val needsSynchronousDraw = _metalLayer.drawableSize.useContents {
            width == 0.0 || height == 0.0
        }

        _metalLayer.drawableSize = scaledSize

        if (needsSynchronousDraw) {
            redrawer.drawSynchronously()
        }
    }

    override fun canBecomeFirstResponder() = false

}
