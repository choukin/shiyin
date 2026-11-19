package com.shiyin.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.shiyin.player.data.permission.Permissions
import com.shiyin.player.ui.navigation.ShiyinNavHost
import com.shiyin.player.ui.theme.ShiyinTheme

/**
 * 单一 Activity 入口：ShiyinTheme + ShiyinNavHost（8 条路由）。
 *
 * 主背景取 colorScheme.background（#F5F6F3），白色 #FFFFFF 只用于卡片表面。
 * POST_NOTIFICATIONS 非阻塞：字段初始化注册 launcher（必须在 CREATED 之前），
 * 结果不写入任何 UiState，被拒也不影响 MediaSessionService 后台播放。
 */
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // 一期仅请求、不门控：媒体会话通知本就豁免，被拒亦不阻断播放。
        // 结果暂不持久化；如需埋点，二期在此接入。
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            ShiyinTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    ShiyinNavHost(container = (application as ShiyinApp).appContainer)
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        val permission = Permissions.notificationPermission ?: return
        notificationPermissionLauncher.launch(permission)
    }
}
