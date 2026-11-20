plugins {
    // AGP 9.0 起内置 Kotlin 支持，显式应用 org.jetbrains.kotlin.android 会直接报错。
    // 参考 https://kotl.in/gradle/agp-built-in-kotlin
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// ---------------------------------------------------------------------------
// 发版注入：仓库内不保存任何 keystore / 口令。
// 本地：~/.gradle/gradle.properties 写 SHIYIN_* 或用 -P 传；
// CI：GitHub Actions 通过 ORG_GRADLE_PROJECT_SHIYIN_* 环境变量注入（Gradle 自动映射为属性）。
// 四项齐全才创建 release signingConfig；缺任一项时 release 为未签名包，
// 不影响本地 debug 构建。详见 docs/RELEASE.md。
// ---------------------------------------------------------------------------
fun releaseProp(name: String): String? = (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }

val releaseStorePassword = releaseProp("SHIYIN_KEYSTORE_PASSWORD")
val releaseKeyAlias = releaseProp("SHIYIN_KEY_ALIAS")
val releaseKeyPassword = releaseProp("SHIYIN_KEY_PASSWORD")
val releaseKeystore = releaseProp("SHIYIN_KEYSTORE")?.let { rootProject.file(it) }
val hasReleaseSigning = releaseKeystore?.isFile == true &&
    releaseStorePassword != null && releaseKeyAlias != null && releaseKeyPassword != null

android {
    namespace = "com.shiyin.player"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.shiyin.player"
        minSdk = 26
        // Play 自 2026-08-31 起要求 targetSdk >= 36；保守取 36，Android 17 行为变更留待兼容测试后再升 37。
        targetSdk = 36
        // 本地默认 1 / 0.1.0；CI 发版时由 tag 与提交数注入，保证 versionCode 单调递增。
        versionCode = releaseProp("SHIYIN_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = releaseProp("SHIYIN_VERSION_NAME") ?: "0.1.0"
    }

    // AGP 9 默认 debug 包只启用 v2 签名（v1/v3 关闭）。部分国产 ROM（MIUI/HyperOS）
    // 的安装器解析"纯 v2 单签名"包时会在 PackageParser 阶段返回 null，
    // 表现为红米 K50 上的「解析软件包时出现问题(33) / PackageInfo is null」。
    // 这里显式开启 v1+v2+v3 混合签名：v1 提供 META-INF 清单兼容旧安装器，
    // v2/v3 满足 Android 7.0+/9.0+ 的全量校验，覆盖所有目标设备。
    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                // 与 debug 保持一致：v1 兼容国产 ROM 安装器，v2/v3 覆盖现代全量校验。
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    // 04 链接下载需要 INTERNET / ACCESS_NETWORK_STATE；除此之外无网络行为，
    // 封面仍只加载 MediaStore 的 content:// URI，不引 Coil 网络层。
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
    // 二期：音频裁剪 / 视频提取音频（ClippingConfiguration / setRemoveVideo），版本与播放链路一致。
    implementation(libs.media3.transformer)

    // MP3 导出：LAME native 编码器（MIT）。Android 平台与 Media3 均无 MP3 编码器，
    // 故 WAV/MP3 走 MediaCodec 解码 PCM 的独立链路；M4A 仍用 Transformer。
    implementation(libs.lame.mp3)

    // 04 链接下载：OkHttp 5.x，只用于用户显式粘贴的音/视频直链 GET。
    implementation(libs.okhttp)

    // 封面：Coil 3 已换 group 为 io.coil-kt.coil3；不需要 coil-network-okhttp（封面只走 content://）。
    implementation(libs.coil.compose)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}
