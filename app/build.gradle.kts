plugins {
    // AGP 9.0 起内置 Kotlin 支持，显式应用 org.jetbrains.kotlin.android 会直接报错。
    // 参考 https://kotl.in/gradle/agp-built-in-kotlin
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.shiyin.player"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.shiyin.player"
        minSdk = 26
        // Play 自 2026-08-31 起要求 targetSdk >= 36；保守取 36，Android 17 行为变更留待兼容测试后再升 37。
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // AGP 9 内置 Kotlin：kotlinOptions DSL 已移除。
    // Kotlin jvmTarget 由下方 compileOptions 的 Java 17 兼容性自动对齐（见 BUILD.md）。

    buildFeatures {
        compose = true
    }

    // 一期不声明 INTERNET：纯单机、离线字体、无广告 SDK，最小化权限面。
    // 二期链接下载功能上线时再补 INTERNET / ACCESS_NETWORK_STATE。
}

dependencies {
    // BOM 必须以 platform 引入，且 androidTestImplementation 与 implementation 各一份（本期暂无插桩测试）。
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    // 媒体播放：MediaSessionService 提供后台播放与锁屏/通知控制。
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.common)
    implementation(libs.media3.datasource)
    implementation(libs.media3.extractor)

    // 封面：Coil 3 已换 group 为 io.coil-kt.coil3；不需要 coil-network-okhttp（一期无网络图片）。
    implementation(libs.coil.compose)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}
