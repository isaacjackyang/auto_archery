# Auto Archery

Auto Archery 是一個以 Android 為目標平台的射箭訓練輔助 App 原型。它使用手機相機觀察單一標靶，嘗試自動偵測新箭、計算落點分數與時鐘方向、進行語音播報，並把訓練資料保存到本機。

這個專案目前屬於 MVP / prototype 階段，重點放在離線本機流程、即時判定 UI、逐箭確認與基礎數據分析。

---

## 目前已完成的功能

- 建立訓練 Session
  - 可選擇選手、距離、每局箭數、靶紙規格與訓練目標。
  - 目前內建距離選項為 `18m`、`30m`、`50m`、`70m`。
  - 目前支援靶紙規格為 `122 cm` 與 `40 cm`。

- 相機預覽與裝置控制
  - 使用 CameraX 顯示相機預覽。
  - 會優先挑選後鏡頭中焦段最長的鏡頭作為長焦；若沒有明顯長焦，會退回主鏡頭並套用數位變焦。
  - 進入 App 後會保持螢幕常亮，適合固定在腳架上使用。

- 靶面校正與箭點偵測
  - 會根據畫面亮度特徵估計標靶橢圓、中心與半徑。
  - 將畫面正規化後，用前後 frame 差異偵測新箭。
  - 偵測到新箭後會換算成標準化座標並記錄信心值。

- 自動判分與即時畫面
  - 支援 line-cutter 判分邏輯。
  - 可計算分數、X、時鐘方向與 X/Y 偏移。
  - 即時畫面會顯示數位靶、逐箭列表、本局分數與待確認 / 低信心數量。

- 人工確認與備援操作
  - 可選中任一箭後進行 `確認`、`重新播報`、`微調`。
  - 微調模式可直接在數位靶上拖曳箭點，儲存後重新計分。
  - 提供 `手動新增箭`、`刪除上一箭`、`重判選中箭`、`暫停 / 恢復自動判定`、`確認高信心箭`。

- 數據頁與本機資料保存
  - 使用 Room 將 Session、End、Arrow 與操作紀錄保存在本機資料庫。
  - 數據頁可查看訓練摘要、各局摘要、逐箭資料與和上一筆訓練的基礎比較。
  - 會計算每局的 `CTC`、`Accuracy`、`Precision Mean Radius`、`Cpk X/Y`。

- 設定頁
  - 可新增選手、設定預設選手。
  - 可查看最近訓練紀錄摘要。
  - 可保存顯示語言偏好與 `TTS follows display language` 設定。

---

## 使用方式

建議在實體 Android 手機上執行，並把手機固定在腳架或穩定支架上。由於此 App 依賴相機與語音播報，模擬器只適合做基本 UI 驗證，不適合完整體驗。

1. 安裝並啟動 App。
2. 首次進入時允許相機權限。
3. 點 `New Session` 建立新的訓練。
4. 選擇選手、距離、每局箭數、靶紙規格，必要時輸入訓練目標。
5. 建立後把相機對準單一標靶，按 `Calibrate` 完成校正；若畫面偏掉可再按 `Recalibrate`。
6. 開始射箭，系統會在偵測到新箭後：
   - 更新數位靶
   - 加入逐箭列表
   - 計算分數與方向
   - 用 TTS 播報結果
7. 若某箭需要複查，可在逐箭列表點選後使用：
   - `Confirm Selected Shot`
   - `Adjust`
   - `Replay TTS`
8. 訓練中也可以用快速操作處理例外情況：
   - `Manual Add`
   - `Delete Last`
   - `Reanalyze Selected`
   - `Pause Auto` / `Resume Auto`
   - `Confirm High Confidence`
9. 一局結束後可按 `Confirm End` 或 `Next Round` 進入下一局。
10. 點 `Metrics` 查看摘要與逐箭數據，點 `Settings` 管理選手與查看近期訓練。

---

## 開發與執行

---

### 環境需求

- Android Studio
- JDK 17
- Android SDK 36
- Android 8.0+ 裝置或模擬器

---

### 指令

```bash
./gradlew assembleDebug
```

Windows:

```bat
gradlew.bat assembleDebug
```

安裝到已連接的 Android 裝置：

```bat
gradlew.bat installDebug
```

APK 產出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## 技術堆疊

- Kotlin
- Jetpack Compose
- Navigation Compose
- CameraX
- Room
- Android TextToSpeech

---

## `app/build.gradle.kts` 逐段解讀

以下用這個專案的 [app/build.gradle.kts](app/build.gradle.kts) 當例子，說明每一段設定的功能與用途。

---

### 第 1 行

- `import org.jetbrains.kotlin.gradle.dsl.JvmTarget`
  - 匯入 Kotlin Gradle DSL 裡的 `JvmTarget` 型別。
  - 下面 `kotlin { compilerOptions { ... } }` 會用它指定 Kotlin 編譯輸出的 JVM 版本。

---

### 第 3 到 7 行：`plugins`

- `plugins { ... }`
  - 宣告這個 `app` 模組會套用哪些 Gradle plugin。

- `alias(libs.plugins.android.application)`
  - 套用 Android Application plugin。
  - 讓這個模組可以被當成 Android App 來編譯、打包成 APK。
  - `alias(...)` 代表 plugin 版本不是直接寫在這裡，而是從 `gradle/libs.versions.toml` 讀取。

- `alias(libs.plugins.compose.compiler)`
  - 套用 Kotlin Compose compiler plugin。
  - 讓 Kotlin 編譯器知道這個模組有使用 Jetpack Compose。

- `alias(libs.plugins.legacy.kapt)`
  - 套用 `kapt` 註解處理器 plugin。
  - 這個專案的 Room 需要靠 `kapt` 產生資料庫相關程式碼。

---

### 第 9 到 46 行：`android`

- `android { ... }`
  - Android 模組的主要建置設定都放在這裡。

---

### 第 10 到 11 行：基本識別與編譯版本

- `namespace = "com.autoarchery.app"`
  - 指定 Android namespace。
  - 這會影響產生的 `R` 類別與套件名稱空間。

- `compileSdk = 36`
  - 指定編譯時使用的 Android SDK 版本。
  - 代表這個專案用 API 36 的 SDK 來編譯。

---

### 第 13 到 20 行：`defaultConfig`

- `defaultConfig { ... }`
  - 放這個 app 預設的執行與打包設定。

- `applicationId = "com.autoarchery.app"`
  - App 在 Android 系統裡的唯一識別值。
  - 安裝到手機後，系統就是靠它辨識這個 App。

- `minSdk = 26`
  - App 最低支援 Android API 26，也就是 Android 8.0。

- `targetSdk = 36`
  - 告訴系統這個 App 是以 API 36 的行為規範為目標。
  - 它不代表最低需求，而是代表你有針對較新的 Android 行為做相容。

- `versionCode = 1`
  - 給系統與商店判斷升級順序用的整數版本號。
  - 每次正式發新版通常都要遞增。

- `versionName = "0.1.0"`
  - 給使用者看的版本字串。
  - 這個專案目前標成 `0.1.0`，符合 MVP / prototype 階段。

- `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`
  - 指定 Android instrumentation test 使用的測試執行器。
  - 如果之後要跑裝置上的 UI / integration test，會用到它。

---

### 第 22 到 30 行：`buildTypes`

- `buildTypes { ... }`
  - 定義不同建置型態，例如 `debug`、`release`。
  - 這個檔案目前只特別設定 `release`。

- `release { ... }`
  - 指定 release 版的打包規則。

- `isMinifyEnabled = false`
  - 代表 release 版目前不啟用程式碼壓縮、混淆與裁剪。
  - 這樣比較方便原型開發與除錯，但正式上架前通常會再評估開啟。

- `proguardFiles(...)`
  - 指定 release 版打包時使用的 ProGuard / R8 規則檔。

- `getDefaultProguardFile("proguard-android-optimize.txt")`
  - 使用 Android 預設提供的最佳化規則。

- `"proguard-rules.pro"`
  - 再額外套用這個專案自己的規則檔。

---

### 第 32 到 35 行：`compileOptions`

- `compileOptions { ... }`
  - 設定 Java 編譯器相關版本。

- `sourceCompatibility = JavaVersion.VERSION_17`
  - 原始碼語法以 Java 17 為基準。

- `targetCompatibility = JavaVersion.VERSION_17`
  - 編譯後輸出的位元碼也以 Java 17 相容為目標。

---

### 第 37 到 39 行：`buildFeatures`

- `buildFeatures { compose = true }`
  - 明確告訴 Android Gradle Plugin 這個模組啟用了 Jetpack Compose。
  - 沒有這行時，Compose UI 相關功能不會完整開啟。

---

### 第 41 到 45 行：`packaging`

- `packaging { resources { ... } }`
  - 用來處理打包 APK 時的資源衝突與排除規則。

- `excludes += "/META-INF/{AL2.0,LGPL2.1}"`
  - 排除某些第三方函式庫內附的授權檔。
  - 這類 `META-INF` 檔案有時會在多個依賴裡重複出現，造成打包衝突。

---

### 第 48 到 52 行：`kotlin`

- `kotlin { compilerOptions { ... } }`
  - 設定 Kotlin 編譯器的選項。

- `jvmTarget = JvmTarget.JVM_17`
  - 指定 Kotlin 編譯輸出目標也是 JVM 17。
  - 這要和前面的 Java 17 設定保持一致，避免編譯結果不一致。

---

### 第 54 到 59 行：`kapt`

- `kapt { ... }`
  - 設定 Kotlin 註解處理器。
  - Room 的 `@Database`、`@Dao`、`@Entity` 會在這裡觸發產碼流程。

- `correctErrorTypes = true`
  - 讓 kapt 在某些型別尚未完整產生時，仍能更穩定處理 annotation processing。
  - 這常見於 Room、Dagger 之類會自動產碼的工具。

- `arguments { arg("room.schemaLocation", "$projectDir/schemas") }`
  - 傳參數給 Room compiler。
  - 告訴 Room 把 schema JSON 輸出到 `app/schemas`。
  - 這個專案已經把 schema 檔追蹤進 git，方便資料庫版本管理與 migration 檢查。

---

### 第 61 到 88 行：`dependencies`

- `dependencies { ... }`
  - 宣告這個模組需要的所有函式庫。

---

### 第 62 到 72 行：AndroidX 與 CameraX 基礎依賴

- `implementation(libs.androidx.core.ktx)`
  - 提供 Android KTX 擴充函式，讓 Android API 更好寫。

- `implementation(libs.androidx.lifecycle.runtime.ktx)`
  - 提供 Lifecycle 與 coroutine 相關整合。

- `implementation(libs.androidx.lifecycle.runtime.compose)`
  - 讓 Compose 可以更方便觀察 lifecycle-aware 狀態。

- `implementation(libs.androidx.lifecycle.viewmodel.ktx)`
  - 提供 ViewModel 與 coroutine 的擴充支援。

- `implementation(libs.androidx.lifecycle.viewmodel.compose)`
  - 讓 Compose 畫面中可以直接取得 ViewModel。

- `implementation(libs.androidx.activity.compose)`
  - 提供 Compose 與 Android Activity 的整合。

- `implementation(libs.androidx.navigation.compose)`
  - 提供 Compose Navigation。
  - 這個專案的 Live / Metrics / Settings 頁面切換就是靠它。

- `implementation(libs.androidx.camera.core)`
  - CameraX 核心功能。

- `implementation(libs.androidx.camera.camera2)`
  - 讓 CameraX 能透過 Camera2 後端與裝置相機溝通。

- `implementation(libs.androidx.camera.lifecycle)`
  - 把相機生命週期綁到 `LifecycleOwner`，避免畫面切換時資源沒釋放。

- `implementation(libs.androidx.camera.view)`
  - 提供 `PreviewView` 等相機預覽 UI 元件。

---

### 第 74 到 80 行：Compose UI 依賴

- `implementation(platform(libs.androidx.compose.bom))`
  - 先引入 Compose BOM。
  - BOM 會統一 Compose 相關套件版本，避免每個 Compose library 各自版本不一致。

- `implementation(libs.androidx.compose.ui)`
  - Compose 基本 UI 元件。

- `implementation(libs.androidx.compose.ui.graphics)`
  - Compose 圖形與繪圖能力。
  - 這個專案在數位靶與箭點標記會用到。

- `implementation(libs.androidx.compose.ui.tooling.preview)`
  - 支援 Compose Preview。

- `implementation(libs.androidx.compose.foundation)`
  - 提供 `Column`、`Row`、`LazyColumn`、手勢等基礎元件。

- `implementation(libs.androidx.compose.material3)`
  - 提供 Material 3 元件。
  - 這個專案大部分畫面按鈕、卡片、TopAppBar 都來自這裡。

- `implementation(libs.androidx.compose.material.icons.extended)`
  - 提供更多 Material Icons。

---

### 第 82 到 84 行：Room 依賴

- `implementation(libs.androidx.room.runtime)`
  - Room 執行期核心函式庫。

- `implementation(libs.androidx.room.ktx)`
  - Room 的 Kotlin / coroutine 擴充。

- `kapt(libs.androidx.room.compiler)`
  - Room annotation processor。
  - 會依照 `@Dao`、`@Entity`、`@Database` 產生實際的資料庫實作程式碼。

---

### 第 86 到 87 行：只在 debug 版使用的依賴

- `debugImplementation(libs.androidx.compose.ui.tooling)`
  - 只在 debug build 加入 Compose tooling。
  - 正式版不會把這些除錯工具打包進去。

- `debugImplementation(libs.androidx.compose.ui.test.manifest)`
  - 提供 Compose UI 測試在 debug 環境下需要的 manifest 支援。

---

### 這份檔案在這個專案裡的角色

- 它是 `app` 模組的建置腳本。
- 它決定這個 Android App：
  - 用哪些 plugin
  - 支援哪些 Android 版本
  - 用哪個 Java / Kotlin 版本編譯
  - 是否啟用 Compose
  - 依賴哪些函式庫
  - release 版怎麼打包

如果你把它想成一張「App 建置說明書」，會很好理解：Gradle 讀這份檔案後，才知道要怎麼把這個專案編譯成可安裝的 Android App。

---

## 專案結構

```text
app/src/main/java/com/autoarchery/app/
  data/        資料庫、DAO、Repository
  domain/      判分、座標與時鐘方向邏輯
  navigation/  App 導航
  speech/      TTS 協調器
  ui/          Live / Metrics / Settings 畫面
  vision/      靶面校正與箭點偵測

docs/
  mvp-spec.md              MVP 規格
  compose-wireframes.md    UI 草圖
  backlog-phase-1-3.md     開發待辦
```

---

## 目前限制

- 目前定位為單一標靶、離線本機使用，不包含雲端同步或多人帳號。
- 影像辨識目前是規則式 MVP 演算法，最適合固定腳架、穩定構圖、單一靶面場景。
- 目前支援的靶紙規格只有 `122 cm` 與 `40 cm`。
- 匯出按鈕 UI 已經放上，但 `PNG / PDF / CSV` 匯出流程尚未接完成。
- 設定頁已可保存顯示語言偏好，但完整的執行中 UI locale 切換仍有擴充空間；目前語音播報語言的行為較完整。

---

## 參考文件

- [MVP 規格](docs/mvp-spec.md)
- [Compose Wireframes](docs/compose-wireframes.md)
- [Backlog](docs/backlog-phase-1-3.md)
