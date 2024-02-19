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

package androidx.compose.mpp.demo.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SelectionExample() {
    var count by remember { mutableStateOf(0) }
    Column {
        Button(onClick = { count++ }) {
            Text("Outside Count: $count")
        }
        SelectionContainer(
            Modifier.padding(24.dp).fillMaxWidth()
        ) {
            Column {
                Text(
                    "I'm a selection container. Double tap on word to select a word." +
                        " Triple tap on content to select whole paragraph.\nAnother paragraph for testing.\n" +
                        "And another one."
                )
                Row {
                    DisableSelection {
                        Button(onClick = { count++ }) {
                            Text("DisableSelection Count: $count")
                        }
                    }
                    Button(onClick = { count++ }) {
                        Text("SelectionContainer Count: $count")
                    }
                }
                Text("I'm another Text() block. Let's try to select me!")
                Text("I'm yet another Text() with multiparagraph structure block.\nLet's try to select me!")
            }
        }
        Column(
            Modifier
                .height(100.dp)
                .padding(2.dp)
                .border(1.dp, Color.Blue)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            SelectionContainer {
                Text(
                    text = "Select text and scroll\n".repeat(100),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}