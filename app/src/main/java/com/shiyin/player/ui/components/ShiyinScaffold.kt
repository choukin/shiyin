package com.shiyin.player.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 全 App 统一的 Scaffold：固定浅色背景（#F5F6F3），白色只用于卡片表面。
 * 状态栏 / 安全区策略 PROVISIONAL：由 MainActivity 的 enableEdgeToEdge() +
 * Material3 Scaffold 默认 contentWindowInsets 统一处理，屏幕层不各自加 padding。
 */
@Composable
fun ShiyinScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    screenContent: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            ShiyinTopBar(title = title, onBack = onBack, actions = actions)
        },
        bottomBar = bottomBar
    ) { padding ->
        screenContent(padding)
    }
}
