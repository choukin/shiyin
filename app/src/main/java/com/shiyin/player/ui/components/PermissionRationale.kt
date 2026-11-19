package com.shiyin.player.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinDimens

/**
 * 无存储权限态（一期真实状态）。
 * 永久拒绝时 primaryLabel 应传「去系统设置」，且调用方不得重复弹权限框。
 */
@Composable
fun PermissionRationale(
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ShiyinDimens.CardCorner),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ShiyinGlyph(
                glyph = Glyph.Info,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(ProvisionalDimens.IconSizeLarge)
            )
            Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
            Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(ProvisionalDimens.SectionVerticalSpacing))
            Button(onClick = onPrimary) { Text(text = primaryLabel) }
            Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
            TextButton(onClick = onSecondary) { Text(text = secondaryLabel) }
        }
    }
}
