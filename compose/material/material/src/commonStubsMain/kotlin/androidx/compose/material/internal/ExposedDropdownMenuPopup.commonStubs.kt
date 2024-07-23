package androidx.compose.material.internal

import androidx.compose.material.implementedInJetBrainsFork
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.PopupPositionProvider

@Composable
internal actual fun ExposedDropdownMenuPopup(
    onDismissRequest: (() -> Unit)?,
    popupPositionProvider: PopupPositionProvider,
    content: @Composable () -> Unit
) {
    implementedInJetBrainsFork()
}
