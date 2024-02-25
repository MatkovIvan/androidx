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

package androidx.compose.desktop.examples.nasa

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import gov.nasa.worldwind.BasicModel
import gov.nasa.worldwind.awt.WorldWindowGLCanvas
import java.awt.Component


fun main() = application {
//    System.setProperty("compose.swing.render.on.graphics", "true")
//    System.setProperty("compose.interop.blending", "true")

    Window(
        onCloseRequest = ::exitApplication
    ) {
        MaterialTheme {
            App()
        }
    }
}

@Composable
private fun App() {
    Column(Modifier.background(Color.LightGray)) {
        var leftPanelShown by remember { mutableStateOf(false) }
        var rightPanelShown by remember { mutableStateOf(false) }
        var bottomPanelShown by remember { mutableStateOf(false) }
        var swingComposeShown by remember { mutableStateOf(true) }
        var composeShown by remember { mutableStateOf(true) }
        var mapShown by remember { mutableStateOf(true) }
        var overlaysLeft by remember { mutableStateOf(true) }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(onClick = { leftPanelShown = !leftPanelShown }) {
                Text(if (leftPanelShown) "Hide Left" else "Show Left")
            }
            Button(onClick = { bottomPanelShown = !bottomPanelShown }) {
                Text(if (bottomPanelShown) "Hide Bottom" else "Show Bottom")
            }
            Button(onClick = { rightPanelShown = !rightPanelShown }) {
                Text(if (rightPanelShown) "Hide Right" else "Show Right")
            }
            Button(onClick = { mapShown = !mapShown }) {
                Text(if (mapShown) "Hide Map" else "Show Map")
            }
            Button(onClick = { swingComposeShown = !swingComposeShown }) {
                Text(if (swingComposeShown) "Hide Swing Compose" else "Show Swing Compose")
            }
            Button(onClick = { composeShown = !composeShown }) {
                Text(if (composeShown) "Hide Compose" else "Show Compose")
            }
            Button(onClick = { overlaysLeft = !overlaysLeft }) {
                Text(if (overlaysLeft) "Move Right" else "Move Left")
            }
        }
        Row(Modifier.weight(1f)) {
            if (leftPanelShown) {
                Box(
                    Modifier.width(200.dp).fillMaxHeight().background(Color.Blue),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Left Panel")
                }
            }
            Box(Modifier.weight(1f)) { // <--- Here we use Box to make layering of SwingPanels
                if (mapShown) {
                    // Bottom MAP layer
                    SwingPanel( // <--- Here we use Swing panel to show WorldWindGLCanvas from Swing world (proper use of SwingPanel)
                        modifier = Modifier.fillMaxSize().background(Color.Black).clip(RoundedCornerShape(12.dp)),
                        factory = { createWorldWindMap() }
                    )
                }
                Popup(alignment = Alignment.Center) {
                    Box(
                        Modifier.size(200.dp, 100.dp).background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Popup")
                    }
                }
                Row {
                    if (!overlaysLeft) {
                        Spacer(Modifier.weight(1f))
                    }
                    OverlaysUI(Modifier.padding(8.dp).width(100.dp), swingComposeShown, composeShown)
                    if (overlaysLeft) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            if (rightPanelShown) {
                Box(
                    Modifier.width(200.dp).fillMaxHeight().background(Color.Magenta),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Right Panel")
                }
            }
        }
        if (bottomPanelShown) {
            Box(Modifier.fillMaxWidth().height(100.dp).background(Color.Cyan), contentAlignment = Alignment.Center) {
                Text("Bottom Panel")
            }
        }
    }
}

@Composable
private fun OverlaysUI(modifier: Modifier, swingComposeShown: Boolean, composeShown: Boolean) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (swingComposeShown) {
//            Box(Modifier.weight(1f).fillMaxWidth().background(Color.DarkGray, RoundedCornerShape(12.dp))) {
            SwingPanel( // <--- Here we use Swing panel to make a trick wrapping Compose layout (Swing/Compose switching trick START). If we do not wrap Compose with SwingPanel here, you will not see Compose content over WorldWindGLCanvas(Swing)
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).weight(1f).fillMaxWidth().clip(RoundedCornerShape(6.dp)),
                factory = {
                    ComposePanel().apply { // <-- Swing/Compose switching trick END
                        background = java.awt.Color.PINK
                        setContent {
                            Box(
                                Modifier.fillMaxSize().background(Color.Green, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Swing Compose")
                            }
                        }
                    }
                }
            )
//            }
        }
        if (composeShown) {
            Box(
                Modifier.fillMaxWidth().weight(1f).background(Color.Green, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Compose")
            }
        }
    }
}


private fun createWorldWindMap(): Component {
    return WorldWindowGLCanvas().apply {
        model = BasicModel()
    }
}
