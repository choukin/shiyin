package com.shiyin.player.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.shiyin.player.R
import com.shiyin.player.data.model.DownloadError
import com.shiyin.player.data.model.DownloadUiState
import com.shiyin.player.data.model.ExportState
import com.shiyin.player.ui.components.ShiyinScaffold
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinDimens

/**
 * 04 链接下载（二期真实实现）：输入音视频直链 → 真实下载进度 → 成功/失败态。
 * 次要入口保留 SAF「导入本地文件」，与已验收能力并存。
 *
 * 尺寸全部取自 ProvisionalDimens / ShiyinDimens，屏幕层零魔数、零硬编码色值。
 */
@Composable
fun LinkDownloadScreen(
    state: DownloadUiState,
    onUrlChange: (String) -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onImport: (Uri?) -> Unit,
    onReset: () -> Unit,
    onConsumeError: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> onImport(uri) }

    ShiyinScaffold(
        title = stringResource(R.string.link_download),
        onBack = onBack,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = ProvisionalDimens.ScreenHorizontalPadding,
                    vertical = ProvisionalDimens.ContentVerticalPadding
                ),
            verticalArrangement = Arrangement.spacedBy(ProvisionalDimens.ItemVerticalSpacing)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ShiyinDimens.CardCorner),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = stringResource(R.string.link_download_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding)
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ShiyinDimens.CardCorner),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = stringResource(R.string.link_download_notice),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding)
                )
            }

            OutlinedTextField(
                value = state.url,
                onValueChange = onUrlChange,
                label = { Text(text = stringResource(R.string.link_download_label)) },
                placeholder = { Text(text = stringResource(R.string.link_download_hint)) },
                singleLine = true,
                enabled = !state.isDownloading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onDownload,
                enabled = !state.isDownloading && state.url.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.link_download_action))
            }

            if (state.isDownloading) {
                if (state.progress >= 0) {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = stringResource(R.string.link_download_progress, state.progress),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = stringResource(R.string.link_download_progress_unknown),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onCancel) {
                    Text(text = stringResource(R.string.link_download_cancel))
                }
            }

            when (val export = state.exportState) {
                ExportState.Idle, ExportState.Running -> Unit
                is ExportState.Success -> Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ShiyinDimens.CardCorner),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.link_download_success, export.message),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(ProvisionalDimens.CardInnerPadding)
                        )
                        TextButton(onClick = onReset) {
                            Text(text = stringResource(R.string.editor_result_dismiss))
                        }
                    }
                }
                is ExportState.Failure -> Text(
                    text = export.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            state.error?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ShiyinDimens.CardCorner),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Column(modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding)) {
                        Text(
                            text = stringResource(error.toMessageRes()),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        state.errorDetail?.let { detail ->
                            Text(
                                text = stringResource(R.string.link_download_detail, detail),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = onConsumeError) {
                            Text(text = stringResource(R.string.editor_result_dismiss))
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ShiyinDimens.CardCorner),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = stringResource(R.string.link_download_import_section),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding)
                )
            }

            OutlinedButton(
                onClick = { picker.launch(arrayOf(MIME_AUDIO)) },
                enabled = !state.isDownloading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.editor_pick_audio))
            }
        }
    }
}

private fun DownloadError.toMessageRes(): Int = when (this) {
    DownloadError.INVALID_URL -> R.string.link_error_invalid_url
    DownloadError.NO_NETWORK -> R.string.link_error_no_network
    DownloadError.REQUEST_FAILED -> R.string.link_error_request_failed
    DownloadError.HTTP_ERROR -> R.string.link_error_http
    DownloadError.UNSUPPORTED_MEDIA -> R.string.link_error_unsupported
    DownloadError.FILE_TOO_LARGE -> R.string.link_error_too_large
    DownloadError.EMPTY_FILE -> R.string.link_error_empty
    DownloadError.WRITE_FAILED -> R.string.link_error_write_failed
    DownloadError.IMPORT_FAILED -> R.string.link_error_import_failed
}

private const val MIME_AUDIO = "audio/*"
