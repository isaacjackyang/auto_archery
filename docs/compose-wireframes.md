# Auto Archery Compose Wireframes

## 1. 目的

本文件將 [mvp-spec.md](/Users/USER/Documents/GitHub/Auto_Archery/docs/mvp-spec.md) 的產品需求轉為可落地的 Jetpack Compose 畫面結構，重點是：

- 教練在訓練中只需要操作兩個主頁面。
- 頁面一優先支援快速判斷、微調、確認與備援操作。
- 頁面二優先支援文字化檢視、比較、註記與匯出。
- 設定頁維持輕量，但要支援選手管理與語言切換。

## 2. Compose 路由建議

### 2.1 Route 結構

- `live_session/{sessionId}`：頁面一，即時判定頁
- `session_metrics/{sessionId}`：頁面二，數據明細頁
- `settings`：設定頁

### 2.2 Modal / Sheet

- `AthletePickerSheet`
- `SessionSetupSheet`
- `ArrowAdjustSheet`
- `CoachNoteSheet`
- `ExportSheet`
- `HistoryPickerSheet`

## 3. 全域 UI 原則

- 訓練進行中預設橫向畫面。
- 平板與大螢幕優先採左右雙欄。
- 直向手機可退化為上下堆疊，但互動邏輯不變。
- 危險操作如刪除上一箭需二次確認。
- 低信心箭與待確認箭需有高對比警示色。
- 所有分數、箭序、操作鈕都要適合教練站在旁邊快速點擊。

## 4. 資訊架構

```text
App
├─ LiveSessionScreen
│  ├─ TopSessionBar
│  ├─ CameraPanel
│  ├─ TargetPanel
│  ├─ ShotListPanel
│  ├─ ReviewQueueCard
│  ├─ CoachActionBar
│  └─ EndSummaryCard
├─ MetricsScreen
│  ├─ SessionSummaryCard
│  ├─ ComparisonCard
│  ├─ EndMetricsList
│  ├─ ArrowTable
│  ├─ CoachNotesSection
│  └─ ExportActions
└─ SettingsScreen
   ├─ AthleteManagementSection
   ├─ TrainingHistoryEntry
   ├─ LanguageSection
   └─ AppPreferencesSection
```

## 5. 頁面一：即時判定頁

### 5.1 主要目標

- 左看相機，右看數位靶與逐箭結果。
- 每箭出現後立即判定並播報。
- 教練可快速 `確認` 或 `微調 -> 儲存`。
- 教練可執行手動備援操作。

### 5.2 橫向 Wireframe

```text
+---------------------------------------------------------------------------------------------------+
| TopSessionBar                                                                                    |
| [Back] [Athlete: Lin] [Session: 70m Training] [End 3/12] [Arrow 4/6] [Paused?] [Metrics] [Menu] |
+-----------------------------------------------+---------------------------------------------------+
| CameraPanel                                    | TargetAndReviewPanel                              |
|                                               |                                                   |
|  +-----------------------------------------+  |  +---------------------+  +-------------------+ |
|  | Camera Preview                          |  |  | Digital Target      |  | Review Queue      | |
|  |                                         |  |  |                     |  | Pending: 2        | |
|  |  target frame / center guide            |  |  |   1  2  3  4        |  | Low confidence: 1 | |
|  |                                         |  |  |     shot points     |  | [Filter pending]  | |
|  |                                         |  |  |   selected point    |  | [Filter low conf] | |
|  |                                         |  |  +---------------------+  +-------------------+ |
|  +-----------------------------------------+  |                                                   |
|                                               |  +-----------------------------------------------+ |
|  [Zoom x10] [Recalibrate] [TTS On]            |  | Shot List                                      | |
|  [Auto Detect Active/Paused]                  |  | # | Score | Clock | Status | Edited | Action | |
|                                               |  | 1 | 10    | 12    | OK     | No     | View   | |
|                                               |  | 2 | 9     | 10    | Review | Yes    | Adjust | |
|                                               |  | 3 | 8     | 4     | OK     | No     | Confirm| |
|                                               |  +-----------------------------------------------+ |
|                                               |                                                   |
|                                               |  +-----------------------------------------------+ |
|                                               |  | Selected Arrow Card                           | |
|                                               |  | Score: 9   Clock: 10 o'clock                  | |
|                                               |  | Confidence: 0.71                              | |
|                                               |  | [Replay TTS] [Adjust] [Confirm] [Save]       | |
|                                               |  +-----------------------------------------------+ |
+-----------------------------------------------+---------------------------------------------------+
| CoachActionBar: [Manual Add] [Delete Last] [Reanalyze] [Pause Auto] [Resume Auto] [Confirm High]|
+---------------------------------------------------------------------------------------------------+
| EndSummaryCard (only visible when end complete): Score 56 | X:2 | CTC | Accuracy | [Confirm End] |
+---------------------------------------------------------------------------------------------------+
```

### 5.3 直向 Wireframe

```text
+-----------------------------------------------+
| TopSessionBar                                 |
+-----------------------------------------------+
| CameraPanel                                   |
| preview / overlays / zoom / recalibrate       |
+-----------------------------------------------+
| DigitalTarget                                 |
+-----------------------------------------------+
| ReviewQueueCard                               |
+-----------------------------------------------+
| ShotListPanel                                 |
+-----------------------------------------------+
| SelectedArrowCard                             |
+-----------------------------------------------+
| CoachActionBar                                |
+-----------------------------------------------+
| EndSummaryCard                                |
+-----------------------------------------------+
```

### 5.4 Compose 區塊拆分

- `TopSessionBar`
  - 返回
  - 選手名稱
  - Session 名稱
  - 局數 / 箭序
  - 暫停狀態
  - 前往頁面二
  - 設定選單
- `CameraPanel`
  - `CameraPreviewSurface`
  - `TargetFrameOverlay`
  - `CameraStatusChips`
  - `CameraControlsRow`
- `TargetPanel`
  - `DigitalTargetCanvas`
  - `ShotMarker`
  - `SelectedMarkerHalo`
  - `TargetLegend`
- `ReviewQueueCard`
  - 待確認數
  - 低信心數
  - 篩選按鈕
- `ShotListPanel`
  - `LazyColumn`
  - `ShotRow`
- `SelectedArrowCard`
  - 箭序
  - 分數
  - 時鐘方向
  - 信心值
  - 操作按鈕
- `CoachActionBar`
  - 手動新增
  - 刪除上一箭
  - 重判
  - 暫停 / 恢復
  - 一鍵確認高信心箭
- `EndSummaryCard`
  - 本局總分
  - X / 10
  - CTC
  - Accuracy
  - Precision
  - `Confirm End`

### 5.5 微調模式

建議不要切新頁，而是在頁面一右側或 Bottom Sheet 進行。

```text
+------------------------------------------------------+
| ArrowAdjustSheet                                     |
| Arrow #3 | Score 9 | Clock 10 o'clock                |
|                                                  [X] |
|  +-----------------------------------------------+   |
|  | Zoomed Digital Target                         |   |
|  |                draggable point                |   |
|  +-----------------------------------------------+   |
|  [Score -] [Score +] [Toggle X]                     |
|  [Cancel] [Save]                                    |
+------------------------------------------------------+
```

微調規則：

- 進入 `微調` 後即鎖定目前選中箭。
- 拖曳時即時更新候選座標與預估分數。
- 按 `Save` 才寫回正式資料。
- 按 `Cancel` 應恢復原值。

### 5.6 頁面一狀態

- `Calibrating`
- `Monitoring`
- `DetectingNewArrow`
- `ReviewRequired`
- `Paused`
- `EndCompleted`

### 5.7 頁面一 ViewState 建議

```kotlin
data class LiveSessionUiState(
    val sessionTitle: String,
    val athleteName: String,
    val currentEndIndex: Int,
    val totalEnds: Int,
    val currentArrowIndex: Int,
    val arrowsPerEnd: Int,
    val isPaused: Boolean,
    val zoomLabel: String,
    val ttsEnabled: Boolean,
    val reviewPendingCount: Int,
    val lowConfidenceCount: Int,
    val selectedShotId: String?,
    val shotMarkers: List<ShotMarkerUi>,
    val shotRows: List<ShotRowUi>,
    val selectedShot: SelectedShotUi?,
    val endSummary: EndSummaryUi?,
    val mode: LiveSessionMode
)
```

## 6. 頁面二：數據明細頁

### 6.1 主要目標

- 給教練快速閱讀而不是純視覺炫技。
- 顯示當次訓練、歷史比較、教練註記與匯出。
- 能切換不同局與不同歷史 Session。

### 6.2 Wireframe

```text
+--------------------------------------------------------------------------------------------------+
| TopBar: [Back to Live] [Athlete: Lin] [Session Summary] [Compare] [Export] [History Picker]     |
+--------------------------------------------------------------------------------------------------+
| SessionSummaryCard                                                                               |
| Goal: Group Tightness | Tags: Match Sim, Windy | Indoor/Outdoor | Equipment | Coach Summary      |
+--------------------------------------------------------------------------------------------------+
| ComparisonCard                                                                                   |
| This Session vs Previous Similar Session                                                         |
| Score +6 | CTC -12mm | Accuracy +4mm | Precision +3mm                                           |
+------------------------------------------+-------------------------------------------------------+
| EndMetricsList                            | ArrowTable                                            |
| End 1 | 54 | CTC | Acc | Prec             | # | End | Score | X/Y | Clock | Status | Note      |
| End 2 | 56 | CTC | Acc | Prec             | 1 | 1   | 10    | ... | 12    | OK     | ...       |
| End 3 | 53 | CTC | Acc | Prec             | 2 | 1   | 9     | ... | 10    | Edited | ...       |
| ...                                       | ...                                                   |
+------------------------------------------+-------------------------------------------------------+
| CoachNotesSection                                                                               |
| [Session note] [End note] [Arrow note]                                                           |
+--------------------------------------------------------------------------------------------------+
| ExportActions: [Export PNG] [Export PDF] [Export CSV]                                            |
+--------------------------------------------------------------------------------------------------+
```

### 6.3 Compose 區塊拆分

- `MetricsTopBar`
- `SessionSummaryCard`
- `ComparisonCard`
- `EndMetricsList`
- `ArrowMetricsTable`
- `CoachNotesSection`
- `ExportActionsRow`

### 6.4 頁面二 ViewState 建議

```kotlin
data class MetricsUiState(
    val athleteName: String,
    val sessionTitle: String,
    val trainingGoal: String?,
    val tags: List<String>,
    val coachSummary: String?,
    val comparison: SessionComparisonUi?,
    val endSummaries: List<EndSummaryRowUi>,
    val arrowRows: List<ArrowMetricsRowUi>,
    val notes: List<CoachNoteUi>,
    val exportFormats: List<ExportFormatUi>
)
```

## 7. 設定頁

### 7.1 目的

- 管理選手與 App 偏好。
- 維持簡單，不搶主操作頁的焦點。

### 7.2 Wireframe

```text
+--------------------------------------------------------------+
| Settings                                                     |
+--------------------------------------------------------------+
| Athlete Management                                           |
| [Default Athlete: Lin]                                       |
| [Add Athlete]                                                |
| - Lin         [Edit] [Disable]                              |
| - Chen        [Edit] [Disable]                              |
+--------------------------------------------------------------+
| Training Records                                             |
| [Open History]                                               |
+--------------------------------------------------------------+
| Language                                                     |
| (o) 繁體中文   ( ) English                                   |
| [x] TTS follows display language                             |
+--------------------------------------------------------------+
```

### 7.3 Compose 區塊拆分

- `SettingsScreen`
- `AthleteManagementSection`
- `AthleteRow`
- `TrainingHistoryEntry`
- `LanguageSection`

## 8. 設計 Token 建議

### 8.1 顏色語意

- `statusConfirmed`: 綠
- `statusPending`: 橙
- `statusLowConfidence`: 紅
- `statusEdited`: 藍
- `sessionAccent`: 深藍或墨綠

### 8.2 字級

- 分數字級需大於一般列表文案。
- 箭序與狀態應次於分數但仍清楚可辨。
- `EndSummaryCard` 的總分需可在遠距離快速辨識。

### 8.3 觸控尺寸

- 所有核心操作鈕至少 48dp。
- `Confirm`, `Adjust`, `Save` 要預留更大點擊範圍。

## 9. Navigation 建議

- 頁面一與頁面二用 top-level route 切換。
- 設定頁由 overflow menu 開啟。
- 微調與註記使用 sheet，不切 route。
- 歷史 Session 切換可用 `HistoryPickerSheet`。

## 10. 推薦 Compose 檔案切分

```text
ui/
  live/
    LiveSessionRoute.kt
    LiveSessionScreen.kt
    CameraPanel.kt
    TargetPanel.kt
    ShotListPanel.kt
    SelectedArrowCard.kt
    CoachActionBar.kt
    EndSummaryCard.kt
    ArrowAdjustSheet.kt
  metrics/
    MetricsRoute.kt
    MetricsScreen.kt
    SessionSummaryCard.kt
    ComparisonCard.kt
    ArrowMetricsTable.kt
    CoachNotesSection.kt
    ExportActionsRow.kt
  settings/
    SettingsRoute.kt
    SettingsScreen.kt
    AthleteManagementSection.kt
    LanguageSection.kt
```

## 11. Wireframe 驗收重點

- 頁面一能在不切頁的情況下完成 `看靶 -> 判定 -> 微調 -> 確認 -> 下一箭`。
- 頁面一能一眼看出待確認與低信心箭數。
- 頁面一有明確的教練備援操作入口。
- 頁面二能看懂訓練表現，而不是只看圖。
- 設定頁能管理多位選手與語言。
