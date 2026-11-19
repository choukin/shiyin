// 根构建脚本：只声明插件版本，不应用到根项目。
// 插件版本统一由 gradle 目录下的版本目录文件管理。
plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9.0 起内置 Kotlin 支持，不再声明 org.jetbrains.kotlin.android。
    alias(libs.plugins.compose.compiler) apply false
}
