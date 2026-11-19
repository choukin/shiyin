package com.shiyin.player

import android.app.Application
import com.shiyin.player.di.AppContainer

/**
 * 应用入口：挂载手动 DI 容器。
 * 一期刻意不引入 Hilt/Koin/KSP：MediaSessionService 由系统实例化，
 * 单模块小项目用手动容器更稳，也避免注解处理器带来的构建失败面。
 */
class ShiyinApp : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
