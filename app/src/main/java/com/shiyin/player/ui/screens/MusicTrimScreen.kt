package com.shiyin.player.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.shiyin.player.R
import com.shiyin.player.ui.components.FeaturePlaceholderScreen
import com.shiyin.player.ui.components.WaveformPlaceholder
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinTheme

/** 05 音乐裁剪：一期占位（静态波形柱条 + 导出成功静态示意），零广告。 */
@Composable
fun MusicTrimScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    FeaturePlaceholderScreen(
        title = stringResource(R.string.music_trim),
        body = stringResource(R.string.placeholder_trim_body),
        badge = stringResource(R.string.feature_phase_two),
        demoLabel = stringResource(R.string.placeholder_trim_export_demo),
        onBack = onBack,
        modifier = modifier,
        extra = {
            Column(modifier = Modifier.fillMaxWidth()) {
                WaveformPlaceholder()
                Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
                WaveformPlaceholder()
            }
        }
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MusicTrimScreenPreview() {
    ShiyinTheme { MusicTrimScreen(onBack = {}) }
}
