package com.shiyin.player.data.media

import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest

/**
 * 曲库删除真实媒体文件。
 *
 * - API 30+：用 MediaStore.createTrashRequest（回收站，可恢复）或
 *   createDeleteRequest（永久删除），两者都返回一个 IntentSender，
 *   由 UI 通过 ActivityResultContracts.StartIntentSenderForResult 弹出系统确认框；
 * - API 26-29：直接 ContentResolver.delete（需 WRITE_EXTERNAL_STORAGE，manifest 已声明 maxSdk=29）。
 *
 * 注意：createTrashRequest 需要系统支持回收站；部分 ROM 不支持时会抛异常，
 * UI 层据此回退到永久删除。
 */
class MediaDeleteRepository(private val context: Context) {

    sealed interface DeleteMode {
        data object Trash : DeleteMode
        data object Permanent : DeleteMode
    }

    /** 构建删除请求。返回 null 表示无需系统确认（API<30，已直接删除）。 */
    fun requestDelete(uris: List<Uri>, mode: DeleteMode): IntentSenderRequest? {
        if (uris.isEmpty()) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intentSender = when (mode) {
                DeleteMode.Trash -> MediaStore.createTrashRequest(context.contentResolver, uris, true)
                DeleteMode.Permanent -> MediaStore.createDeleteRequest(context.contentResolver, uris)
            }
            return IntentSenderRequest.Builder(intentSender).build()
        }
        // API 26-29：直接删，不弹确认。
        deleteDirect(uris)
        return null
    }

    /** 回收站请求在不支持的 ROM 上会抛异常，UI 可先探测再决定是否提供该选项。 */
    fun canTrash(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    private fun deleteDirect(uris: List<Uri>) {
        val resolver: ContentResolver = context.contentResolver
        uris.forEach { uri ->
            runCatching { resolver.delete(uri, null, null) }
        }
    }
}
