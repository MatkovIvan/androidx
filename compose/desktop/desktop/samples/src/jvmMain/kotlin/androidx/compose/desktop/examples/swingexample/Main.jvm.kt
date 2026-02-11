/*
 * Copyright 2026 The Android Open Source Project
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

@file:OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)

package androidx.compose.desktop.examples.swingexample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.AwtSkiaAdapter
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.unit.dp
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.RepaintManager
import javax.swing.SwingUtilities
import javax.swing.UIManager
import org.jetbrains.skia.Canvas


fun main() {
    println("=== JDK Runtime Information ===")
    println("java.home: ${System.getProperty("java.home")}")
    println("java.version: ${System.getProperty("java.version")}")
    println("java.vendor: ${System.getProperty("java.vendor")}")
    println("java.runtime.version: ${System.getProperty("java.runtime.version")}")

    System.setProperty("sun.java2d.metal", "true")

    SwingUtilities.invokeLater {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        SwingWindow()
    }
}

private fun SwingWindow() {
    val frame = JFrame("Compose over Swing over Skia")
    RepaintManager.currentManager(frame).setDoubleBufferingEnabled(false)
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.minimumSize = Dimension(800, 600)
    frame.isResizable = false

    val composePanel = ComposePanel(SkiaAdapter)
    composePanel.setContent {
        App()
    }
    frame.contentPane = composePanel
//    frame.contentPane = SkiaPanel()
//    frame.contentPane.add(composePanel)

    frame.isVisible = true
}

internal class SkiaPanel : JPanel() {
    override fun paint(g: Graphics) {
        if (width <= 0 || height <= 0) return
        val g2 = g as? Graphics2D ?: return

        // Background and title drawn with Java2D for clarity
        g2.color = background
        g2.fillRect(0, 0, width, height)
        g2.color = Color.WHITE
        g2.font = Font("SansSerif", Font.PLAIN, 24)
        g2.drawString("The text is rendered via Java2D(before skia)", 18, 36)

        super.paint(g)

        g2.color = Color.WHITE
        g2.font = Font("SansSerif", Font.PLAIN, 24)
        g2.drawString("The text is rendered via Java2D(after skia)", 18, height - 20)
    }
}


object SkiaAdapter : AwtSkiaAdapter {
    override fun Graphics.withSkiaCanvas(
        width: Int,
        height: Int,
        block: (Canvas) -> Unit
    ) {
        this as Graphics2D
        runExternal(object : RenderingTask {
            override fun run(surfaceType: String?, pointers: List<Long>, names: List<String?>) {
                val device = pointers[RenderingTask.MTL_DEVICE_ARG_INDEX]
                val queue = pointers[RenderingTask.MTL_COMMAND_QUEUE_ARG_INDEX]
                val texture = pointers[RenderingTask.MTL_TEXTURE_ARG_INDEX]
                if (device == 0L || queue == 0L || texture == 0L) return

                val gc = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
                val scale = gc.defaultTransform.scaleX.toFloat()

                val physicalWidth = (width * scale).toInt().coerceAtLeast(1)
                val physicalHeight = (height * scale).toInt().coerceAtLeast(1)

                val grCtx = org.jetbrains.skia.DirectContext.makeMetal(device, queue) ?: return
                val backendRT = org.jetbrains.skia.BackendRenderTarget.makeMetal(physicalWidth, physicalHeight, texture)
                val surface = org.jetbrains.skia.Surface.makeFromBackendRenderTarget(
                    grCtx,
                    backendRT,
                    org.jetbrains.skia.SurfaceOrigin.TOP_LEFT,
                    org.jetbrains.skia.SurfaceColorFormat.BGRA_8888,
                    null,
                    null
                ) ?: return

                val canvas = surface.canvas
                block(canvas)

                grCtx.flushAndSubmit(surface)

                surface.close()
                backendRT.close()
                grCtx.close()
            }
        })
    }

    override fun Canvas.drawComponent(component: Component) {
        TODO("Not yet implemented")
    }

}

@Composable
private fun App() {
    Box(Modifier.background(androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.8f)).width(50.dp).fillMaxHeight())
    Button(onClick = {}, Modifier.offset(20.dp, 50.dp)) {
        Text("Compose Button")
    }
    LottieAnimation()
}
