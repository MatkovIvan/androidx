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

package androidx.compose.mpp.demo.components.popup

import androidx.compose.mpp.demo.Screen
import androidx.compose.mpp.demo.components.material3.ButtonWithDropdown
import androidx.compose.mpp.demo.components.material3.TextFieldWithExposedDropdown
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

val Popups = Screen.Selection(
    "Popups",
    Screen.Example("ConfigurablePopup") { ConfigurablePopup() },
    Screen.Example("CompositionLocal inside Popup") { PopupCompositionLocalExample() },
    FixedSizePopup,
    HalfScreenPopup,
    Screen.Example("Dropdown inside Popup") {
        Popup {
            ButtonWithDropdown(5)
        }
    },
    Screen.Example("ExposedDropdownMenuBox inside Popup") {
        Popup(
            properties = PopupProperties(
                clippingEnabled = false
            )
        ) {
            TextFieldWithExposedDropdown()
        }
    }
)
