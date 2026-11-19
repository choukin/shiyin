package com.shiyin.player.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.shiyin.player.R
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinDimens

/**
 * 02 屏条形广告占位条。规范（docs/AD_SPEC.md）：
 *  - 必须带 R.string.ad_label「广告」标识；
 *  - 可关闭，关闭态由调用方写入 DataStore，重启不再展示；
 *  - 一期为本地占位，零广告 SDK、零网络请求。
 *  - 只允许由 LibraryScreen 使用；04/05/06 屏幕与导航层均不引用本组件。
 */
@Composable
fun AdBannerPlaceholder(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(ProvisionalDimens.AdBannerHeight),
        shape = RoundedCornerShape(ShiyinDimens.CardCorner),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = ProvisionalDimens.ElevationCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ProvisionalDimens.CardInnerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.ad_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(ProvisionalDimens.ItemVerticalSpacing))
            Text(
                text = stringResource(R.string.library_ad_placeholder),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                ShiyinGlyph(
                    glyph = Glyph.Close,
                    modifier = Modifier.size(ProvisionalDimens.IconSizeSmall)
                )
            }
        }
    }
}
