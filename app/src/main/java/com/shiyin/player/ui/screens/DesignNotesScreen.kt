package com.shiyin.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.shiyin.player.R
import com.shiyin.player.ui.components.ShiyinScaffold
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinBackground
import com.shiyin.player.ui.theme.ShiyinDimens
import com.shiyin.player.ui.theme.ShiyinOnSurface
import com.shiyin.player.ui.theme.ShiyinPrimary
import com.shiyin.player.ui.theme.ShiyinPrimaryContainer
import com.shiyin.player.ui.theme.ShiyinSurface
import com.shiyin.player.ui.theme.ShiyinTheme

/**
 * 08 广告位规范与设计说明：App 内只读静态页（标注「设计说明，非产品功能」），
 * 与 docs/DESIGN_NOTES.md、docs/AD_SPEC.md 内容对应。
 */
@Composable
fun DesignNotesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ShiyinScaffold(
        title = stringResource(R.string.design_notes_title),
        onBack = onBack,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(
                    horizontal = ProvisionalDimens.ScreenHorizontalPadding,
                    vertical = ProvisionalDimens.ContentVerticalPadding
                ),
            verticalArrangement = Arrangement.spacedBy(ProvisionalDimens.SectionVerticalSpacing)
        ) {
            item {
                Text(
                    text = stringResource(R.string.design_notes_disclaimer),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                DesignNoteCard(title = stringResource(R.string.design_notes_palette)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(ProvisionalDimens.ItemVerticalSpacing)) {
                        listOf(
                            ShiyinBackground,
                            ShiyinSurface,
                            ShiyinOnSurface,
                            ShiyinPrimary,
                            ShiyinPrimaryContainer
                        ).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(ProvisionalDimens.IconSizeLarge)
                                    .clip(RoundedCornerShape(ShiyinDimens.ButtonCorner))
                                    .background(color)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
                    Text(
                        text = stringResource(R.string.design_notes_provisional),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                DesignNoteCard(
                    title = stringResource(R.string.design_notes_radius),
                    body = stringResource(R.string.design_notes_radius_body)
                )
            }

            item {
                DesignNoteCard(
                    title = stringResource(R.string.design_notes_type),
                    body = stringResource(R.string.design_notes_type_body)
                )
            }

            item {
                DesignNoteCard(title = stringResource(R.string.design_notes_ad_rule)) {
                    listOf(
                        R.string.design_notes_splash_rule,
                        R.string.design_notes_banner_rule,
                        R.string.design_notes_edit_rule
                    ).forEach { resId ->
                        Text(
                            text = stringResource(resId),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
                    }
                }
            }

            item {
                DesignNoteCard(title = stringResource(R.string.design_notes_states)) {
                    listOf(
                        R.string.design_notes_state_empty,
                        R.string.design_notes_state_permission,
                        R.string.design_notes_state_link_error,
                        R.string.design_notes_state_export
                    ).forEach { resId ->
                        Text(
                            text = stringResource(resId),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
                    }
                }
            }

            item {
                DesignNoteCard(
                    title = stringResource(R.string.design_notes_license_title),
                    body = stringResource(R.string.design_notes_license)
                )
            }

            item {
                DesignNoteCard(
                    title = stringResource(R.string.design_notes_copyright_title),
                    body = stringResource(R.string.design_notes_copyright)
                )
            }
        }
    }
}

@Composable
private fun DesignNoteCard(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    extra: @Composable () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(ShiyinDimens.CardCorner),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
            if (body != null) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            extra()
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DesignNotesScreenPreview() {
    ShiyinTheme { DesignNotesScreen(onBack = {}) }
}
