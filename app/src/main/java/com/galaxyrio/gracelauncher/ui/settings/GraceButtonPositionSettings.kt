@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.galaxyrio.gracelauncher.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.galaxyrio.gracelauncher.R
import com.galaxyrio.gracelauncher.data.GraceButtonAlignment
import com.galaxyrio.gracelauncher.data.GraceButtonLayout
import com.galaxyrio.gracelauncher.data.icons.GraceButtonIcon
import com.galaxyrio.gracelauncher.ui.LauncherActions
import com.galaxyrio.gracelauncher.ui.LauncherUiState
import com.galaxyrio.gracelauncher.ui.components.GraceButtonArtwork
import com.galaxyrio.gracelauncher.ui.components.stableStatusBarInset

@Composable
internal fun GraceButtonPositionSettings(uiState: LauncherUiState, actions: LauncherActions, onBack: () -> Unit) {
    val saved = uiState.settings.graceButton.layout.normalized()
    var draft by remember(saved) { mutableStateOf(saved) }
    val defaults = remember { GraceButtonLayout() }
    val enabled = LocalSettingsStorageState.current.canEdit
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val window = LocalWindowInfo.current.containerSize
    val insets = WindowInsets.safeDrawing
    val viewportWidth = with(density) {
        (window.width - insets.getLeft(this, direction) - insets.getRight(this, direction)).toDp().value.coerceAtLeast(1f)
    }
    val viewportHeight = with(density) { window.height.toDp().value.coerceAtLeast(1f) }
    val topInset = stableStatusBarInset().value
    val bottomInset = insets.asPaddingValues().calculateBottomPadding().value
    fun commit(value: GraceButtonLayout = draft) {
        if (!enabled) return
        draft = value.normalized()
        val placement = draft
        actions.updateSettings { it.copy(graceButton = it.graceButton.copy(layout = placement)) }
    }
    SettingsScaffold(stringResource(R.string.grace_button_position_size), "grace_button_position_page", onBack,
        fixedCollapsed = true) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("grace_button_position_controls"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                item {
                    DesignerSegment(0, 3, "grace_button_alignment") {
                        Text(stringResource(R.string.grace_button_alignment))
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                            GraceButtonAlignment.entries.forEachIndexed { index, alignment ->
                                ToggleButton(checked = draft.alignment == alignment,
                                    onCheckedChange = { commit(draft.copy(alignment = alignment)) }, enabled = enabled,
                                    shapes = when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        2 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    },
                                    modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp)
                                        .testTag("grace_button_alignment:${alignment.name}").semantics { role = Role.RadioButton },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)) {
                                    Text(stringResource(when (alignment) {
                                        GraceButtonAlignment.Left -> R.string.grace_button_align_left
                                        GraceButtonAlignment.Center -> R.string.grace_button_align_center
                                        GraceButtonAlignment.Right -> R.string.grace_button_align_right
                                    }))
                                }
                            }
                        }
                    }
                }
                item {
                    DesignerSegment(1, 3, "grace_button_distances") {
                        val sideLimit = (viewportWidth - draft.size).coerceIn(1f, 2000f)
                        DesignerSlider(stringResource(R.string.grace_button_side_distance), draft.sideMargin.coerceAtMost(sideLimit),
                            defaults.sideMargin.coerceAtMost(sideLimit), 0f..sideLimit,
                            enabled && draft.alignment != GraceButtonAlignment.Center, "grace_button_side_distance", suffix = " dp",
                            onFinished = { commit() }, onReset = { commit(draft.copy(sideMargin = defaults.sideMargin)) }) {
                            draft = draft.copy(sideMargin = it)
                        }
                        val bottomLimit = (viewportHeight - topInset - bottomInset - draft.size).coerceIn(1f, 2000f)
                        DesignerSlider(stringResource(R.string.grace_button_bottom_distance), draft.bottomMargin.coerceAtMost(bottomLimit),
                            defaults.bottomMargin.coerceAtMost(bottomLimit), 0f..bottomLimit, enabled, "grace_button_bottom_distance", suffix = " dp",
                            onFinished = { commit() }, onReset = { commit(draft.copy(bottomMargin = defaults.bottomMargin)) }) {
                            draft = draft.copy(bottomMargin = it)
                        }
                    }
                }
                item {
                    DesignerSegment(2, 3, "grace_button_size_control") {
                        DesignerSlider(stringResource(R.string.grace_button_size), draft.size, defaults.size, 32f..120f,
                            enabled, "grace_button_size", suffix = " dp", onFinished = { commit() },
                            onReset = { commit(draft.copy(size = defaults.size)) }) { draft = draft.copy(size = it) }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 12.dp + LocalSettingsBottomInset.current),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.grace_button_position_hint), Modifier.padding(bottom = 10.dp),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                GraceButtonPositionPreview(uiState, draft, viewportWidth, viewportHeight, topInset, bottomInset, enabled,
                    onChange = { draft = it }, onCommit = { commit() }, onCancel = { draft = saved }, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** A scaled whole-screen viewport, not a different bottom-half coordinate system. */
@Composable
private fun GraceButtonPositionPreview(uiState: LauncherUiState, layout: GraceButtonLayout,
    viewportWidth: Float, viewportHeight: Float, topInset: Float, bottomInset: Float, enabled: Boolean,
    onChange: (GraceButtonLayout) -> Unit, onCommit: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val latest by rememberUpdatedState(layout)
    val change by rememberUpdatedState(onChange)
    val commit by rememberUpdatedState(onCommit)
    val cancel by rememberUpdatedState(onCancel)
    val density = LocalDensity.current
    val shape = MaterialTheme.shapes.large
    val description = stringResource(R.string.grace_button_position_hint)
    val artwork = uiState.graceButtonApp?.icon.takeIf { GraceButtonIcon.key in uiState.itemIcons }
    val design = GraceButtonIcon.choice(uiState.itemIcons[GraceButtonIcon.key]).design ?: GraceButtonIcon.defaults
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val scale = minOf(maxWidth.value / viewportWidth, maxHeight.value / viewportHeight).coerceAtLeast(.001f)
        val pixelsPerDp = density.density * scale
        val bounds = layout.bounds(viewportWidth, viewportHeight, topInset, bottomInset)
        Box(Modifier.size((viewportWidth * scale).dp, (viewportHeight * scale).dp)
            .testTag("grace_button_position_preview").semantics { contentDescription = description }
            .drawWithContent {
                drawOutline(shape.createOutline(size, layoutDirection, this), Color.Transparent, blendMode = BlendMode.Clear)
                drawContent()
            }.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape).clip(shape)
            .pointerInput(viewportWidth, viewportHeight, topInset, bottomInset, scale, enabled) {
                if (!enabled) return@pointerInput
                var dragging = false
                var position = Offset.Zero
                detectDragGestures(
                    onDragStart = { point ->
                        val current = latest.bounds(viewportWidth, viewportHeight, topInset, bottomInset)
                        val hit = Rect(current.left * pixelsPerDp, current.top * pixelsPerDp,
                            (current.left + current.size) * pixelsPerDp, (current.top + current.size) * pixelsPerDp)
                        dragging = hit.inflate(24.dp.toPx()).contains(point)
                        position = Offset(current.left, current.top)
                    },
                    onDragEnd = { if (dragging) commit(); dragging = false },
                    onDragCancel = { if (dragging) cancel(); dragging = false },
                ) { event, amount ->
                    if (dragging) {
                        event.consume()
                        val side = latest.bounds(viewportWidth, viewportHeight, topInset, bottomInset).size
                        val moved = position + amount / pixelsPerDp
                        position = Offset(moved.x.coerceIn(0f, (viewportWidth - side).coerceAtLeast(0f)),
                            moved.y.coerceIn(topInset, (viewportHeight - bottomInset - side).coerceAtLeast(topInset)))
                        change(latest.movedTo(position.x, position.y, viewportWidth, viewportHeight, topInset, bottomInset))
                    }
                }
            }) {
            GraceButtonArtwork(artwork, design, (bounds.size * scale).dp,
                modifier = Modifier.align(AbsoluteAlignment.TopLeft)
                    .absoluteOffset(x = (bounds.left * scale).dp, y = (bounds.top * scale).dp))
        }
    }
}
