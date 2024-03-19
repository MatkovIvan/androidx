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

package androidx.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry

/**
 * Provides [this] [NavBackStackEntry] as [LocalViewModelStoreOwner], [LocalLifecycleOwner] and
 * [LocalSavedStateRegistryOwner] to the [content] and saves the [content]'s saveable states with
 * the given [saveableStateHolder].
 *
 * @param saveableStateHolder The [SaveableStateHolder] that holds the saved states. The same
 * holder should be used for all [NavBackStackEntry]s in the encapsulating [Composable] and the
 * holder should be hoisted.
 * @param content The content [Composable]
 */
@Composable
public fun NavBackStackEntry.LocalOwnersProvider(
    saveableStateHolder: SaveableStateHolder,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalViewModelStoreOwner provides this,
        LocalLifecycleOwner provides this,
        LocalSavedStateRegistryOwner provides this
    ) {
        saveableStateHolder.SaveableStateProvider(content)
    }
}

@Composable
internal expect fun SaveableStateHolder.SaveableStateProvider(content: @Composable () -> Unit)
