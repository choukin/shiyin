package com.shiyin.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.shiyin.player.R
import com.shiyin.player.ui.components.FeaturePlaceholderScreen
import com.shiyin.player.ui.components.VideoPreviewPlaceholder
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinDimens
import com.shiyin.player.ui.theme.ShiyinTheme

/** 06 视频提取音频：一期占位（矢量视频预览 + 整段/片段置灰选项），零广告。 */
@Composable
fun ExtractAudioScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    FeaturePlaceholderScreen(
        title = stringResource(R.string.extract_audio),
        body = stringResource(R.string.placeholder_extract_body),
        badge = stringResource(R.string.feature_phase_two),
        demoLabel = stringResource(R.string.placeholder_trim_export_demo),
        onBack = onBack,
        modifier = modifier,
        extra = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ProvisionalDimens.ItemVerticalSpacing)
            ) {
                VideoPreviewPlaceholder()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ProvisionalDimens.ItemVerticalSpacing)
                ) {
                    DisabledOption(
                        label = stringResource(R.string.placeholder_extract_whole),
                        modifier = Modifier.weight(1f)
                    )
                    DisabledOption(
                        label = stringResource(R.string.placeholder_extract_range),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}

/** 置灰示意：一期不可点击，不写任何二期实现。 */
@Composable
private fun DisabledOption(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(ShiyinDimens.ButtonCorner)
            )
            .padding(ProvisionalDimens.CardInnerPadding)
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ExtractAudioScreenPreview() {
    ShiyinTheme { ExtractAudioScreen(onBack = {}) }
}
