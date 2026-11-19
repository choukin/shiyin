package com.shiyin.player.playback

/**
 * 播放层常量集中处。一期只用 MediaController.setMediaItems 设置队列，
 * 不实现自定义 SessionCommand；若二期需要自定义命令，在此登记 action / extra key。
 */
object PlaybackCommand {
    const val EXTRA_START_INDEX = "com.shiyin.player.extra.START_INDEX"
    const val EXTRA_QUEUE_IDS = "com.shiyin.player.extra.QUEUE_IDS"
}
