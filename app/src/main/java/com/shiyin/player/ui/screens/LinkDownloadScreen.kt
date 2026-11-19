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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.shiyin.player.R
import com.shiyin.player.data.model.EditorError
import com.shiyin.player.data.model.ExportState
import com.shiyin.player.data.model.ImportUiState
import com.shiyin.player.ui.components.ShiyinScaffold
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinDimens

/**
 * 04 导入本地文件（二期合规实现）。
 *
 * 一期设计为「链接下载」，因解析第三方平台链接涉及版权授权与平台条款，
 * 按 docs/DESIGN_NOTES.md 的合规替代路径改为 SAF 选本机音频导入曲库：
 * 不声明 INTERNET、不引任何网络库、不解析链接。零广告。
 */
@Composable
fun LinkDownloadScreen(
    state: ImportUiState,
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
                    text = stringResource(R.string.import_body),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding)
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ShiyinDimens.CardCorner),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = stringResource(R.string.import_notice),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding)
                )
            }

            Button(
                onClick = { picker.launch(arrayOf(MIME_AUDIO)) },
                enabled = state.exportState !is ExportState.Running,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.editor_pick_audio))
            }

            when (val export = state.exportState) {
                ExportState.Idle -> Unit
                ExportState.Running -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = stringResource(R.string.import_running),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                is ExportState.Success -> Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ShiyinDimens.CardCorner),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.import_success, export.message),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(error.toMessageRes()),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(ProvisionalDimens.CardInnerPadding)
                        )
                        TextButton(onClick = onConsumeError) {
                            Text(text = stringResource(R.string.editor_result_dismiss))
                        }
                    }
                }
            }
        }
    }
}

private fun EditorError.toMessageRes(): Int = when (this) {
    EditorError.EXPORT_FAILED -> R.string.import_error
    EditorError.NO_AUDIO -> R.string.editor_error_no_audio
    EditorError.NO_SOURCE -> R.string.editor_error_no_source
    EditorError.PROBE_FAILED -> R.string.editor_error_probe_failed
}

private const val MIME_AUDIO = "audio/*"
