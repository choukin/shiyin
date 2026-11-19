package com.shiyin.player.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinDimens

/**
 * 04 / 05 / 06 二期功能占位页统一骨架。
 * 刻意不提供 showAd / adSlot 等广告参数：编辑类页面零广告由 API 层杜绝，
 * 而不是靠调用方传 false（docs/AD_SPEC.md）。
 */
@Composable
fun FeaturePlaceholderScreen(
    title: String,
    body: String,
    badge: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    demoLabel: String? = null,
    extra: @Composable () -> Unit = {}
) {
    ShiyinScaffold(title = title, onBack = onBack, modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(
                    horizontal = ProvisionalDimens.ScreenHorizontalPadding,
                    vertical = ProvisionalDimens.ContentVerticalPadding
                ),
            verticalArrangement = Arrangement.spacedBy(ProvisionalDimens.SectionVerticalSpacing)
        ) {
            Surface(
                shape = RoundedCornerShape(ShiyinDimens.ButtonCorner),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(
                        horizontal = ProvisionalDimens.CardInnerPadding,
                        vertical = ProvisionalDimens.ItemVerticalSpacing
                    )
                )
            }
            Text(text = body, style = MaterialTheme.typography.bodyLarge)
            extra()
            if (demoLabel != null) {
                Surface(
                    shape = RoundedCornerShape(ShiyinDimens.CardCorner),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = demoLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding)
                    )
                }
            }
        }
    }
}
