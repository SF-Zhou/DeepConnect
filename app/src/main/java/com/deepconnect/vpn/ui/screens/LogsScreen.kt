@file:OptIn(ExperimentalMaterial3Api::class)

package com.deepconnect.vpn.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepconnect.vpn.ui.AppViewModel
import com.deepconnect.vpn.ui.LogFilter
import com.deepconnect.vpn.ui.components.AuroraConfirmDialog
import com.deepconnect.vpn.ui.components.AuroraTextField
import com.deepconnect.vpn.ui.components.AuroraTopBar
import com.deepconnect.vpn.ui.components.ChoicePillRow
import com.deepconnect.vpn.ui.components.GlassIconButton
import com.deepconnect.vpn.ui.components.copyToClipboard
import com.deepconnect.vpn.ui.formatLogTimestamp
import com.deepconnect.vpn.ui.icons.AppIcons
import com.deepconnect.vpn.ui.theme.LocalAuroraColors
import com.deepconnect.vpn.vpn.LogEntry
import kotlinx.coroutines.launch

/**
 * Connection log viewer: filterable, searchable, tail-following, with copy/share actions.
 */
@Composable
fun LogsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    val tokens = LocalAuroraColors.current

    val logs by viewModel.logs.collectAsState()
    val showTimestamps by viewModel.showTimestampsInLog.collectAsState()

    var filterName by rememberSaveable { mutableStateOf(LogFilter.All.name) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val filter = LogFilter.valueOf(filterName)

    val filtered = remember(logs, filter, query) {
        val trimmed = query.trim()
        logs.filter { entry ->
            filter.matches(entry.level) &&
                (trimmed.isEmpty() || entry.message.contains(trimmed, ignoreCase = true))
        }
    }

    val listState = rememberLazyListState()
    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.totalItemsCount == 0) {
                true
            } else {
                val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                lastVisible >= info.totalItemsCount - 2
            }
        }
    }
    // Whether new lines should pull the list to the tail. Cleared when the user scrolls
    // away from the bottom, restored by the "Latest" action.
    var autoFollow by remember { mutableStateOf(true) }
    var initialScrollDone by remember { mutableStateOf(false) }
    LaunchedEffect(listState) {
        // The first emission happens before the initial jump, so it is ignored.
        snapshotFlow { listState.isScrollInProgress }
            .drop(1)
            .collect { scrolling ->
                if (!scrolling && initialScrollDone) autoFollow = atBottom
            }
    }
    LaunchedEffect(filtered.size) {
        if (filtered.isEmpty()) return@LaunchedEffect
        if (autoFollow || !initialScrollDone) {
            listState.animateScrollToItem(filtered.lastIndex)
        }
        initialScrollDone = true
    }

    val snackbarHostState = remember { SnackbarHostState() }

    fun buildLogText(): String = filtered.joinToString(separator = "\n") { entry ->
        buildString {
            if (showTimestamps) {
                append(formatLogTimestamp(entry.timestamp))
                append("  ")
            }
            append(levelTag(entry.level))
            append("  ")
            append(entry.message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AuroraTopBar(
                title = "Connection log",
                subtitle = buildString {
                    append(logs.size)
                    append(if (logs.size == 1) " entry" else " entries")
                    if (filtered.size != logs.size) {
                        append(" · ")
                        append(filtered.size)
                        append(" shown")
                    }
                },
                actions = {
                    GlassIconButton(
                        icon = if (searchOpen) AppIcons.Close else AppIcons.Search,
                        contentDescription = if (searchOpen) "Close search" else "Search log",
                        onClick = {
                            searchOpen = !searchOpen
                            if (!searchOpen) query = ""
                        },
                        size = 38.dp,
                        tint = if (searchOpen) scheme.primary else scheme.onSurfaceVariant,
                    )
                    Box {
                        GlassIconButton(
                            icon = AppIcons.MoreVert,
                            contentDescription = "Log actions",
                            onClick = { menuOpen = true },
                            size = 38.dp,
                            tint = scheme.onSurfaceVariant,
                        )
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Copy all") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = AppIcons.Copy,
                                        contentDescription = null,
                                        modifier = Modifier.size(19.dp),
                                    )
                                },
                                enabled = filtered.isNotEmpty(),
                                onClick = {
                                    menuOpen = false
                                    copyToClipboard(context, buildLogText(), "Connection log")
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Log copied to clipboard")
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = AppIcons.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(19.dp),
                                    )
                                },
                                enabled = filtered.isNotEmpty(),
                                onClick = {
                                    menuOpen = false
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "DeepConnect log")
                                        putExtra(Intent.EXTRA_TEXT, buildLogText())
                                    }
                                    context.startActivity(
                                        Intent.createChooser(send, "Share connection log"),
                                    )
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Clear log", color = scheme.error) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = AppIcons.Trash,
                                        contentDescription = null,
                                        tint = scheme.error,
                                        modifier = Modifier.size(19.dp),
                                    )
                                },
                                enabled = logs.isNotEmpty(),
                                onClick = {
                                    menuOpen = false
                                    confirmClear = true
                                },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = tokens.glassStrongTop,
                    contentColor = scheme.onSurface,
                    actionColor = scheme.primary,
                    dismissActionContentColor = scheme.onSurfaceVariant,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChoicePillRow(
                    options = LogFilter.entries,
                    selected = filter,
                    onSelect = { filterName = it.name },
                    modifier = Modifier.weight(1f),
                    label = { it.label },
                )
            }

            AnimatedVisibility(
                visible = searchOpen,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(tween(180)),
                exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(tween(120)),
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    AuroraTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = "Search log",
                        placeholder = "Filter by message",
                        singleLine = true,
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                Icon(
                                    imageVector = AppIcons.Close,
                                    contentDescription = "Clear search",
                                    tint = scheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable { query = "" }
                                        .padding(9.dp),
                                )
                            }
                        },
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (filtered.isEmpty()) {
                    EmptyLogs(
                        hasLogs = logs.isNotEmpty(),
                        onResetFilters = {
                            filterName = LogFilter.All.name
                            query = ""
                            searchOpen = false
                        },
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 96.dp,
                        ),
                    ) {
                        itemsIndexed(
                            items = filtered,
                            key = { index, entry -> "${entry.timestamp}-$index" },
                        ) { _, entry ->
                            LogRow(entry = entry, showTimestamp = showTimestamps)
                        }
                    }
                }

                if (!atBottom && filtered.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 18.dp, bottom = 18.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(tokens.glassStrongTop, tokens.glassStrongBottom),
                                ),
                            )
                            .border(1.dp, tokens.glassBorder, CircleShape)
                            .clickable {
                                autoFollow = true
                                scope.launch {
                                    listState.animateScrollToItem(filtered.lastIndex)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = AppIcons.ArrowDown,
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Latest",
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.primary,
                        )
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AuroraConfirmDialog(
            title = "Clear the log?",
            message = "All ${logs.size} captured log lines will be removed. The tunnel, if " +
                "connected, keeps running and new lines will appear again.",
            confirmLabel = "Clear log",
            icon = AppIcons.Trash,
            destructive = true,
            onDismiss = { confirmClear = false },
            onConfirm = {
                confirmClear = false
                viewModel.clearLogs()
                scope.launch { snackbarHostState.showSnackbar("Log cleared") }
            },
        )
    }
}

/* -------------------------------------------------------------------------- */
/* rows                                                                      */
/* -------------------------------------------------------------------------- */

@Composable
private fun LogRow(entry: LogEntry, showTimestamp: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val tokens = LocalAuroraColors.current
    val levelColor = levelColor(entry.level)
    SelectionContainer {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(levelColor),
            )
            Spacer(Modifier.width(10.dp))
            if (showTimestamp) {
                Text(
                    text = formatLogTimestamp(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = scheme.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.width(64.dp),
                )
            }
            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp,
                ),
                color = when (entry.level) {
                    0 -> scheme.error
                    1 -> scheme.onSurface
                    2 -> scheme.onSurfaceVariant
                    else -> scheme.onSurfaceVariant.copy(alpha = 0.72f)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun levelColor(level: Int): Color {
    val tokens = LocalAuroraColors.current
    return when (level) {
        0 -> MaterialTheme.colorScheme.error
        1 -> tokens.status.connected
        2 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }
}

private fun levelTag(level: Int): String = when (level) {
    0 -> "E"
    1 -> "I"
    2 -> "D"
    else -> "T"
}

@Composable
private fun EmptyLogs(hasLogs: Boolean, onResetFilters: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tokens = LocalAuroraColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(tokens.glassStrongTop)
                .border(1.dp, tokens.glassBorder, RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (hasLogs) AppIcons.Search else AppIcons.Terminal,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = if (hasLogs) "No matching log lines" else "No log activity yet",
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (hasLogs) {
                "Try a different filter or clear the search."
            } else {
                "Connection activity and handshake details will appear here once you " +
                    "start a session."
            },
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (hasLogs) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(tokens.glassStrongBottom)
                    .border(1.dp, tokens.glassBorder, CircleShape)
                    .clickable(onClick = onResetFilters)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "Reset filters",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary,
                )
            }
        }
    }
}
