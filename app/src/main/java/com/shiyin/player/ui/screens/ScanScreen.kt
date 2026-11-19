package com.shiyin.player.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.shiyin.player.R
import com.shiyin.player.data.model.PermissionState
import com.shiyin.player.data.model.ScanUiState
import com.shiyin.player.data.permission.Permissions
import com.shiyin.player.ui.components.Glyph
import com.shiyin.player.ui.components.PermissionRationale
import com.shiyin.player.ui.components.ShiyinGlyph
import com.shiyin.player.ui.components.ShiyinScaffold
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinTheme

/**
 * 03 扫描本机：权限分流 + 真实 MediaStore 扫描 + 结果计数。
 * 权限申请用 activity-compose 的 ActivityResultContracts，不引 Accompanist。
 * 永久拒绝不重复弹窗，改引导去系统设置。
 */
@Composable
fun ScanScreen(
    state: ScanUiState,
    onPermissionResult: (Boolean, Boolean) -> Unit,
    onStartScan: () -> Unit,
    onOpenSettings: () -> Unit,
    onBackToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = LocalContext.current as? Activity
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val permanent = !granted && activity?.let { Permissions.isPermanentlyDenied(it) } == true
        onPermissionResult(granted, permanent)
    }
    ScanScreenContent(
        state = state,
        onRequestPermission = { launcher.launch(Permissions.audioPermission) },
        onStartScan = onStartScan,
        onOpenSettings = onOpenSettings,
        onBackToLibrary = onBackToLibrary,
        modifier = modifier
    )
}

@Composable
private fun ScanScreenContent(
    state: ScanUiState,
    onRequestPermission: () -> Unit,
    onStartScan: () -> Unit,
    onOpenSettings: () -> Unit,
    onBackToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    ShiyinScaffold(
        title = stringResource(R.string.scan_title),
        onBack = onBackToLibrary,
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
            ShiyinGlyph(
                glyph = Glyph.Scan,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(ProvisionalDimens.IconSizeLarge)
            )

            when {
                state.permission == PermissionState.PermanentlyDenied -> {
                    PermissionRationale(
                        title = stringResource(R.string.permission_audio_title),
                        body = stringResource(R.string.permission_denied_hint),
                        primaryLabel = stringResource(R.string.permission_open_settings),
                        onPrimary = onOpenSettings,
                        secondaryLabel = stringResource(R.string.scan_back_to_library),
                        onSecondary = onBackToLibrary
                    )
                }

                state.permission != PermissionState.Granted -> {
                    PermissionRationale(
                        title = stringResource(R.string.permission_audio_title),
                        body = stringResource(R.string.permission_audio_body),
                        primaryLabel = stringResource(R.string.permission_grant),
                        onPrimary = onRequestPermission,
                        secondaryLabel = stringResource(R.string.scan_back_to_library),
                        onSecondary = onBackToLibrary
                    )
                }

                else -> {
                    Text(
                        text = when {
                            state.isScanning -> stringResource(R.string.scan_scanning)
                            state.finished -> stringResource(R.string.scan_result, state.foundCount)
                            else -> stringResource(R.string.scan_ready)
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    state.error?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (state.finished) {
                        Button(onClick = onBackToLibrary) {
                            Text(text = stringResource(R.string.scan_back_to_library))
                        }
                    } else if (!state.isScanning) {
                        Button(onClick = onStartScan) {
                            Text(text = stringResource(R.string.scan_start))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ScanScreenPreview() {
    ShiyinTheme {
        ScanScreenContent(
            state = ScanUiState(permission = PermissionState.Granted),
            onRequestPermission = {},
            onStartScan = {},
            onOpenSettings = {},
            onBackToLibrary = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ScanScreenPermissionPreview() {
    ShiyinTheme {
        ScanScreenContent(
            state = ScanUiState(permission = PermissionState.Unknown),
            onRequestPermission = {},
            onStartScan = {},
            onOpenSettings = {},
            onBackToLibrary = {}
        )
    }
}
