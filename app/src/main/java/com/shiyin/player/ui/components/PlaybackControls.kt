package com.shiyin.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.shiyin.player.ui.theme.ProvisionalDimens

/** 07 屏播放控制：全部图标 Canvas 自绘，不引 material-icons-extended。 */
@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            ShiyinGlyph(
                glyph = Glyph.Previous,
                modifier = Modifier.size(ProvisionalDimens.PlayControlIconSize)
            )
        }
        Box(
            modifier = Modifier
                .size(ProvisionalDimens.PlayButtonSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            ShiyinGlyph(
                glyph = if (isPlaying) Glyph.Pause else Glyph.Play,
                modifier = Modifier.size(ProvisionalDimens.IconSizeLarge),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        IconButton(onClick = onNext) {
            ShiyinGlyph(
                glyph = Glyph.Next,
                modifier = Modifier.size(ProvisionalDimens.PlayControlIconSize)
            )
        }
    }
}
