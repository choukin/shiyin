# 发版与签名配置

[返回 README](../README.md) · 工作流：[`.github/workflows/release.yml`](../.github/workflows/release.yml) / [`ci.yml`](../.github/workflows/ci.yml)

## 1. 一句话结论

**打 `v*` tag 就会自动生成 release 包并创建 GitHub Release**。仓库里不放任何密钥，签名通过 GitHub Secrets 注入。你需要提供的只有下面第 2 节的 4 个 Secrets（外加第 3 节确认一次仓库权限）；不提供也能跑，只是产出的是「CI 临时签名」的测试包。

## 2. 需要你提供的东西

### 2.1 必须：4 个 Repository Secrets

在 **GitHub 仓库 → Settings → Secrets and variables → Actions → New repository secret** 依次添加：

| Secret 名 | 值 | 说明 |
|---|---|---|
| `SIGNING_KEYSTORE_BASE64` | release keystore 文件的 **单行 Base64** | 不是文件内容文本，是二进制文件编码后的字符串 |
| `SIGNING_STORE_PASSWORD` | keystore 口令 | 与创建 keystore 时的 `-storepass` 一致 |
| `SIGNING_KEY_ALIAS` | key alias，例如 `shiyin` | 与 `-alias` 一致 |
| `SIGNING_KEY_PASSWORD` | key 口令 | 与 `-keypass` 一致。**PKCS12 keystore 必须与上一项填同一个值**，见下方注意 |

四项**必须齐全**：工作流只在四者都非空时才走正式签名，缺任一项就整体降级（不会部分使用）。

#### 已有 keystore（推荐，能覆盖安装旧版本）

```bash
# macOS：结果直接进剪贴板，粘贴到 Secret 值里
base64 -i /path/to/shiyin-release.jks | tr -d '\n' | pbcopy

# Linux
base64 -w0 /path/to/shiyin-release.jks
```

> ⚠️ 如果 App 之前已用某个 keystore 发布过，**务必用同一个**，换密钥会导致用户无法覆盖安装。

#### 还没有 keystore

```bash
STORE_PASS='<自定义口令>'
keytool -genkeypair -v \
  -keystore shiyin-release.jks \
  -alias shiyin \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$STORE_PASS" \
  -keypass  "$STORE_PASS" \
  -dname "CN=choukin, OU=Shiyin, O=Shiyin, L=Beijing, ST=Beijing, C=CN"

# 生成后立即把文件本身离线备份；它不入库、GitHub 里只存 Base64。
base64 -i shiyin-release.jks | tr -d '\n' | pbcopy
```

此时 `SIGNING_STORE_PASSWORD` 和 `SIGNING_KEY_PASSWORD` **都填 `$STORE_PASS`**。

> ⚠️ **PKCS12 口令陷阱（本地实测踩过）**：新版 `keytool` 默认生成 PKCS12 格式，它不支持 store 口令与 key 口令不同 —— 传了不同的 `-keypass` 会被静默忽略并打印「正在忽略用户指定的 -keypass 值」，之后 AGP 用 key 口令解 key 会报 `Given final block not properly padded`，`packageRelease` 直接失败。
> 解决办法二选一：**两个口令填相同值**（推荐，上面命令即如此），或创建时加 `-storetype JKS`（JKS 支持不同口令）。工作流的 CI 降级模式已按相同口令处理。

`.gitignore` 已排除 `*.jks` / `*.keystore` / `keystore.properties` / `signing/`，本地生成的 keystore 不会被误提交。

### 2.2 必须确认一次：Actions 权限

**Settings → Actions → General → Workflow permissions** 选 **Read and write permissions**，然后 Save。工作流文件里已声明 `permissions: contents: write`，但若仓库 / 组织默认限制为只读，创建 Release 仍可能失败（见第 6 节）。

### 2.3 可选（不影响自动发版）

| 项 | 用途 |
|---|---|
| README 截图 | `preview/` 目前被 `.gitignore` 忽略，README 未引用截图。要配图的话，把要入库的图放到 `docs/screenshots/` 并告诉我 |
| 应用图标 / 商店素材 | 目前用 `@mipmap/ic_launcher` 占位，上架前需替换 |
| 发版命名规范 | 默认 `v<semver>`，含 `-` 的 tag（如 `v0.2.0-rc1`）自动标为预发布；如需 `v1.0.0+build` 之类规则可以改 |
| Play Console 服务账号 JSON | 只有在你想让 CI **自动上传 AAB 到 Google Play** 时才需要；当前工作流只把 AAB 挂到 GitHub Release，不含 Play 发布步骤 |
| Release 是否需要人工审核 | 当前是「tag 即发布」，没有审批环境；如需 Environment protection rules 再加 |

## 3. 触发方式

| 方式 | 操作 | 结果 |
|---|---|---|
| 正式发版 | `git tag -a v0.2.0 -m "release 0.2.0" && git push origin v0.2.0` | 单测 → APK + AAB → SHA256 → 创建 GitHub Release |
| 手动测试 | Actions → Release → Run workflow，填 `version` | 构建 + 上传 artifact；默认不建 Release |
| 手动并发布 | 同上并勾选 `create_release` | 额外创建 `v<version>` tag 与 Release |
| 日常提交 / PR | push 到 `main` 或开 PR | 只跑 `ci.yml`：单测 + debug APK artifact，不建 Release |

版本号规则：

- `versionName` = tag 去掉前缀 `v`；手动运行时取输入的 `version`。
- `versionCode` = `git rev-list --count HEAD`，随提交单调递增，不在代码里手改。
- 本地 / 未注入时回落为 `versionCode 1 / versionName 0.1.0`（`app/build.gradle.kts`）。
- tag 含 `-` → Release 标为 `prerelease`。

## 4. 产物

Release 附件固定三件（外加 GitHub 自动生成的 changelog）：

| 文件 | 说明 |
|---|---|
| `Shiyin-<version>.apk` | 可直接安装，v1 + v2 + v3 混合签名 |
| `Shiyin-<version>.aab` | 应用商店上架用，同一密钥签名 |
| `SHA256SUMS.txt` | APK / AAB 校验和 |

每次运行还会上传一份 30 天保留期的 artifact（含 `signature.txt`，记录 `apksigner verify --verbose` 结果）。

## 5. 签名降级模式

若 4 个 Secrets 未配齐，工作流用 `keytool` 在 runner 上现生成一次性密钥完成签名，流程不中断：

- ✅ 产出的 APK 可以正常安装；
- ❌ **每次构建密钥都不同**，无法覆盖安装上一版，也不能上架应用商店；
- ⚠️ Release 正文顶部会写明显警告，正文「签名」一栏标为「CI 临时密钥」。

补齐 Secrets 后重新打一个 tag 即可得到正式签名包。判断当前是哪种模式，看 Release 正文或 workflow 日志里的 `signing mode: official | ci`。

## 6. 本地复现 CI 构建

不需要 GitHub 也能验证签名链路（临时密钥放 `/tmp`，不入库）：

```bash
cd shiyin
KEY=/tmp/shiyin-release.jks
# PKCS12 要求 store/key 口令一致
keytool -genkeypair -keystore "$KEY" -alias shiyin \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass local-pass -keypass local-pass \
  -dname "CN=Local, OU=Dev, O=Shiyin, L=Beijing, ST=Beijing, C=CN"

ORG_GRADLE_PROJECT_SHIYIN_KEYSTORE="$KEY" \
ORG_GRADLE_PROJECT_SHIYIN_KEYSTORE_PASSWORD=local-pass \
ORG_GRADLE_PROJECT_SHIYIN_KEY_ALIAS=shiyin \
ORG_GRADLE_PROJECT_SHIYIN_KEY_PASSWORD=local-pass \
./gradlew :app:assembleRelease :app:bundleRelease \
  -PSHIYIN_VERSION_NAME=0.2.0 \
  -PSHIYIN_VERSION_CODE="$(git rev-list --count HEAD)"

"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose \
  app/build/outputs/apk/release/app-release.apk
```

Gradle 会把 `ORG_GRADLE_PROJECT_<name>` 环境变量自动映射成 project property，构建脚本用 `findProperty` 读取；口令不会出现在命令行里。等价写法是把这四项写进 `~/.gradle/gradle.properties`（同样不入库）。

## 7. 故障排查

| 现象 | 原因 | 处理 |
|---|---|---|
| 产出 `app-release-unsigned.apk`，工作流报「没有生成 app-release.apk」 | 四项签名属性没全部注入 | 检查 4 个 Secrets 是否都非空、Base64 解出的文件是否非空；看日志 `signing mode` |
| `Keystore was tampered with, or password was incorrect` | Base64 被换行 / 截断，或口令 / alias 写错 | 重新用 `tr -d '\n'` 生成 Base64；核对 `-storepass` / `-keypass` / `-alias` |
| `Get Key failed: Given final block not properly padded` | PKCS12 keystore 的 key 口令与 store 口令不一致（`-keypass` 被静默忽略） | 两个 Secret 填同一个口令，或改用 `-storetype JKS` 重建 keystore |
| `Resource not accessible by integration` / 创建 Release 403 | `GITHUB_TOKEN` 只有读权限 | Settings → Actions → General → Workflow permissions → Read and write |
| `versionCode` 没有递增 | tag 指向旧提交，或历史被改写 | 用完整历史（工作流已设 `fetch-depth: 0`）；必要时手动 `-PSHIYIN_VERSION_CODE` 覆盖 |
| 手机提示「应用未安装 / 签名冲突」 | 用了 CI 临时密钥或换过正式密钥 | 配齐正式 Secrets 重发；用户侧需先卸载临时签名版本 |
| `Failed to find Platform SDK with path: platforms;android-37` | runner 上 SDK 组件缺失且 AGP 未能自动下载 | 在 `SDK 许可与组件` 步骤里显式 `sdkmanager --install "platforms;android-37"`，或降低 `compileSdk` |
| `bundleRelease` 产物找不到 | AGP 版本差异导致输出目录变化 | 查 `app/build/outputs/bundle/release/`，同步改工作流里的 `cp` 路径 |
| `signature.txt` 里 `Verified using v1 scheme: false` | apksigner 在 v2/v3 存在时走强签名校验路径，v1 行显示 false 是显示行为 | 用 `unzip -l *.apk \| grep -E 'MANIFEST\.MF\|CERT\.SF\|CERT\.RSA'` 与 `jarsigner -verify` 佐证；AGP 已写入 v1 实体（本地实测确认） |

## 8. 设计约束

- 仓库内**零密钥、零口令**：`app/build.gradle.kts` 只在四项属性齐全时 `create("release")` signingConfig，否则 release 构建退化为未签名，不干扰 debug。
- 不做 `force push`、不改远端历史；tag 只增不删。
- `ci.yml` 与 `release.yml` 分离：日常提交不会误触发发布，也不会消耗 Release 配额。
- 目前**不接** Play Console / Firebase App Distribution，AAB 只作为 GitHub Release 附件；需要自动分发时再单独加 job 与对应凭据。
