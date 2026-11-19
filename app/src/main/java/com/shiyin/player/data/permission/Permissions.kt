package com.shiyin.player.data.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限分流：
 *  - API 33+ → READ_MEDIA_AUDIO
 *  - API ≤32 → READ_EXTERNAL_STORAGE（Manifest 已用 maxSdkVersion="32" 限定）
 *  - POST_NOTIFICATIONS 仅 API 33+ 存在，由 Activity 非阻塞请求，被拒不阻断播放。
 */
object Permissions {
    val audioPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    val notificationPermission: String?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED

    fun shouldShowRationale(activity: Activity): Boolean =
        ActivityCompat.shouldShowRequestPermissionRationale(activity, audioPermission)

    /** 永久拒绝 = 未授权且系统不再显示申请理由；此时不重复弹窗。 */
    fun isPermanentlyDenied(activity: Activity): Boolean =
        !isGranted(activity) && !shouldShowRationale(activity)

    fun appSettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
