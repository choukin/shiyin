package com.shiyin.player.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.shiyin.player.R
import com.shiyin.player.ui.components.FeaturePlaceholderScreen
import com.shiyin.player.ui.theme.ShiyinTheme

/**
 * 04 链接下载：一期占位（零广告、不联网、不 import 二期库）。
 * 「链接解析失败」只在占位文案里静态示意；二期启动前须先解决版权合规。
 */
@Composable
fun LinkDownloadScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    FeaturePlaceholderScreen(
        title = stringResource(R.string.link_download),
        body = stringResource(R.string.placeholder_link_body),
        badge = stringResource(R.string.feature_phase_two),
        demoLabel = stringResource(R.string.placeholder_link_error_demo),
        onBack = onBack,
        modifier = modifier
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LinkDownloadScreenPreview() {
    ShiyinTheme { LinkDownloadScreen(onBack = {}) }
}
