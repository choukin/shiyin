package com.shiyin.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.shiyin.player.R
import com.shiyin.player.data.model.PlayerUiState
import com.shiyin.player.data.model.Track
import com.shiyin.player.data.util.Formatters
import com.shiyin.player.ui.components.PlaybackControls
import com.shiyin.player.ui.components.RotatingVinyl
import com.shiyin.player.ui.components.ShiyinScaffold
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinMonoText
import com.shiyin.player.ui.theme.ShiyinTheme

/**
 * 07 播放器：Compose 自绘 UI（不引 media3-ui）。
 * 播放状态来自 PlayerController 的 StateFlow，后台/锁屏/通知由 MediaSessionService 承载。
 */
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onBack: () -> Unit,
    resolveArtwork: suspend (Track) -> ImageBitmap? = { null },
    modifier: Modifier = Modifier
) {
    val duration = state.durationMs.coerceAtLeast(0L)
    val progress = if (duration > 0L) {
        (state.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val currentTrack = state.currentTrack
    val artwork by produceState<ImageBitmap?>(initialValue = null, key1 = currentTrack?.id) {
        value = currentTrack?.let { resolveArtwork(it) }
    }

    ShiyinScaffold(
        title = stringResource(R.string.player_now_playing),
        onBack = onBack,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(
                    horizontal = ProvisionalDimens.ScreenHorizontalPadding,
                    vertical = ProvisionalDimens.ContentVerticalPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ProvisionalDimens.SectionVerticalSpacing)
        ) {
            RotatingVinyl(
                isPlaying = state.isPlaying,
                artwork = artwork
            )
            Text(
                text = state.currentTrack?.title ?: stringResource(R.string.player_no_track),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = state.currentTrack?.artist ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = progress,
                onValueChange = { fraction -> onSeek((fraction * duration).toLong()) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Formatters.formatDuration(state.positionMs),
                    style = ShiyinMonoText.TimeCode
                )
                Text(
                    text = Formatters.formatDuration(duration),
                    style = ShiyinMonoText.TimeCode
                )
            }
            PlaybackControls(
                isPlaying = state.isPlaying,
                onPrevious = onPrevious,
                onToggle = onToggle,
                onNext = onNext
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PlayerScreenPreview() {
    ShiyinTheme {
        PlayerScreen(
            state = PlayerUiState(
                isConnected = true,
                isPlaying = true,
                positionMs = 62_000L,
                durationMs = 204_000L
            ),
            onToggle = {},
            onPrevious = {},
            onNext = {},
            onSeek = {},
            onBack = {}
        )
    }
}
