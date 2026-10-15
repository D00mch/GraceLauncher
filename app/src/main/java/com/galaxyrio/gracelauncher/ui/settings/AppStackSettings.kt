package com.galaxyrio.gracelauncher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.galaxyrio.gracelauncher.data.*
import com.galaxyrio.gracelauncher.ui.LauncherActions
import com.galaxyrio.gracelauncher.ui.LauncherUiState

/** The same create/manage screen, with membership checkboxes rather than another app list. */
@Composable
internal fun AppStackPickerScreen(state: LauncherUiState, actions: LauncherActions, selectedKeys: Set<String>,
    onBack: () -> Unit, onToggle: (LauncherApp) -> Unit) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = editingId == null, onBack = onBack)
    if (editingId != null) {
        LauncherSettingsScreen(state, actions, { editingId = null }, initialFolderId = editingId, closePrivateSpaceOnDispose = false)
    } else FolderSettings(state, actions, onBack, appStacks = true, selectionChangesFavorites = false,
        newPlacement = FolderPlacement.None,
        selectedIds = state.folders.filter { it.key in selectedKeys }.map { it.id }.toSet(),
        onToggle = { id -> state.folders.firstOrNull { it.id == id }?.let { onToggle(state.folderItem(it)) } },
        onEdit = { editingId = it })
}
