# Auto Archery Backlog Phase 1-3

## 1. 目的

本文件將 MVP 拆成 3 個主要交付階段，讓我們可以用最短路徑做到：

1. 先把 App 骨架與訓練資料流跑起來。
2. 再把逐箭判定、確認、微調工作流做穩。
3. 最後補齊教練分析、比較與匯出。

## 2. 優先順序原則

- 先做會影響整體資料流的基礎能力。
- 教練現場一定會用到的流程優先於進階分析。
- 能先手動完成的流程，先不要被自動化阻塞。
- 任何自動判定功能都要保留手動備援。

## 3. 里程碑概覽

### Phase 1

目標：
建出可操作的 App 外殼，完成選手、Session、頁面導航、基本資料儲存與手動訓練記錄。

### Phase 2

目標：
完成看靶主流程，讓教練可以在頁面一完成逐箭判定、語音播報、微調、確認與備援操作。

### Phase 3

目標：
完成頁面二的教練分析能力，包括歷史比較、摘要、註記匯整與匯出。

## 4. Phase 1：Foundation

### 4.1 交付成果

- Android 專案骨架
- Compose Navigation
- Room schema
- 設定頁
- 選手管理
- 手動建立訓練與手動記錄箭資料
- 兩頁主 UI 雛形

### 4.2 Epic A：Project Setup

任務：

- 建立 Android 專案
- 設定 Kotlin、Compose、Navigation、Room、ViewModel
- 建立基本目錄結構
- 設定多語系基礎：`values-zh-rTW`、`values`
- 建立主題、色票、共用元件

完成條件：

- App 可啟動
- 可切換到 Live、Metrics、Settings
- 可正確載入繁中與英文文案

### 4.3 Epic B：資料模型與 Repository

任務：

- 建立 `athlete_profile` entity / dao
- 建立 `training_session` entity / dao
- 建立 `training_end` entity / dao
- 建立 `arrow_shot` entity / dao
- 建立 `coach_note` entity / dao
- 建立 `operation_log` entity / dao
- 建立 `session_export` entity / dao
- 建立 `app_preference` entity / dao
- 建立 repository 與基本 use case

完成條件：

- 可新增選手
- 可建立 Session
- 可插入/更新箭資料
- 可讀取單次訓練完整資料

### 4.4 Epic C：設定頁與選手管理

任務：

- 建立 SettingsScreen
- 建立 AthleteManagementSection
- 支援新增/編輯/停用選手
- 支援設定預設選手
- 支援切換顯示語言
- 支援 TTS 跟隨語言設定
- 建立訓練紀錄入口

完成條件：

- 可管理至少 2 位選手
- 語言切換後 UI 立即更新
- 預設選手可帶入新訓練流程

### 4.5 Epic D：Session 建立與手動資料流

任務：

- 建立 SessionSetupSheet
- 建立新訓練欄位：
  - athlete
  - distance
  - target face
  - arrows per end
  - training goal
  - drill tags
  - environment
  - equipment note
- LiveSessionScreen 先接假資料與手動輸入模式
- 支援手動新增箭
- 支援手動結束一局

完成條件：

- 不靠相機也能手動完成一場測試訓練
- 可在頁面一與頁面二之間切換

### 4.6 Phase 1 Demo

- 教練可新增選手
- 教練可建立一場訓練
- 教練可手動記錄 1 局 3 箭
- 資料能出現在頁面二

### 4.7 Phase 1 風險

- 如果先不把 schema 定穩，後面判定流程容易反覆改表
- 若不先做手動資料流，CV 尚未完成時將無法驗證整體 UX

## 5. Phase 2：Live Session Loop

### 5.1 交付成果

- CameraX 預覽
- 靶面校正
- 逐箭事件式判定
- 語音播報
- 微調與確認
- 手動備援操作
- 複查佇列

### 5.2 Epic E：Camera 與 Calibration

任務：

- 整合 CameraX Preview
- 顯示鏡頭倍率與校正狀態
- 建立靶面定位與透視校正流程
- 儲存 calibration 結果
- 支援重新校正

完成條件：

- 可顯示相機畫面
- 可成功完成單靶初始化校正
- 校正結果可提供後續數位靶座標轉換

### 5.3 Epic F：逐箭偵測與數位靶更新

任務：

- 建立低頻監看 loop
- 建立新箭事件觸發條件
- 建立穩定等待與多 frame 分析
- 將新箭映射到數位靶
- 產生 score / clock / confidence
- 寫入 `arrow_shot`

完成條件：

- 新箭出現後能新增到右側數位靶
- 逐箭列表會同步更新
- 低信心箭會進入待確認狀態

### 5.4 Epic G：TTS 與語言同步

任務：

- 整合 TextToSpeech
- 播報分數與時鐘方向
- 支援繁中 / 英文播報
- 支援指定箭重新播報

完成條件：

- 判定成功後可自動播報
- 切換語言後播報同步切換

### 5.5 Epic H：微調、確認與複查

任務：

- 建立 SelectedArrowCard
- 建立 ArrowAdjustSheet
- 支援拖曳點位
- 支援 `Confirm`
- 支援 `Save`
- 支援 `Review Queue`
- 支援 `只看待確認`
- 支援 `只看低信心`
- 支援 `全部確認高信心箭`

完成條件：

- 教練可只處理有問題的箭
- 微調後分數與統計即時更新

### 5.6 Epic I：教練備援操作

任務：

- `Manual Add`
- `Delete Last`
- `Reanalyze Selected`
- `Pause Auto`
- `Resume Auto`
- 寫入 `operation_log`

完成條件：

- 就算自動判定失敗，教練仍能完成整場訓練
- 所有手動操作皆可追溯

### 5.7 Phase 2 Demo

- 用真實靶面完成 1 局自動判定
- 至少演示 1 次 `微調 -> 儲存`
- 至少演示 1 次 `低信心箭複查`
- 至少演示 1 次 `手動新增` 或 `刪除上一箭`

### 5.8 Phase 2 風險

- Camera / 校正是最大技術風險
- 若先追求很高精度，可能拖慢整體交付
- 建議先接受「可人工修正的自動判定」

## 6. Phase 3：Coach Analytics

### 6.1 交付成果

- 頁面二完整資料檢視
- 教練註記
- 歷史比較
- 文字化摘要
- 匯出

### 6.2 Epic J：Metrics Screen

任務：

- 建立 SessionSummaryCard
- 建立 EndMetricsList
- 建立 ArrowMetricsTable
- 顯示 CTC / Accuracy / Precision / Cpk
- 顯示目標、標籤、器材、環境資訊

完成條件：

- 教練可在頁面二完整閱讀該次訓練

### 6.3 Epic K：教練註記與摘要

任務：

- 支援 session note
- 支援 end note
- 支援 arrow note
- 在頁面二顯示註記列表
- 產生簡單規則式教練摘要

完成條件：

- 註記可新增、編輯、查看
- 頁面二有清楚摘要區塊

### 6.4 Epic L：歷史比較

任務：

- 選擇同選手歷史 Session
- 建立 comparison use case
- 顯示 score / CTC / accuracy / precision 差異
- 顯示相同訓練條件比較

完成條件：

- 教練可比較本次與上一筆訓練

### 6.5 Epic M：Export

任務：

- 匯出 PNG 摘要
- 匯出 PDF 報告
- 匯出 CSV 原始資料
- 寫入 `session_export`

完成條件：

- 教練可把結果交給選手
- 匯出內容包含選手名與 Session 資訊

### 6.6 Phase 3 Demo

- 完整跑一場訓練後進入頁面二
- 新增教練註記
- 查看與上一筆訓練比較
- 匯出 PNG / PDF / CSV

## 7. 建議執行順序

1. Phase 1 先完成可手動運作的 App 骨架。
2. Phase 2 把頁面一做成真正能帶訓的主操作頁。
3. Phase 3 補齊教練最需要的回顧與交付能力。

## 8. Definition of Done

每個 backlog item 完成至少需符合：

- 有對應 UI 或資料層實作
- 有基本錯誤處理
- 有至少一個測試或可重現驗證步驟
- 有文字資源支援繁中 / 英文
- 不破壞既有 Session 資料流

## 9. 建議第一輪 Sprint 切法

### Sprint 1

- 專案骨架
- Navigation
- Room schema
- 選手管理
- Session setup

### Sprint 2

- 頁面一 wireframe 落地
- 手動新增箭流程
- 頁面二基本資料顯示

### Sprint 3

- CameraX
- 校正
- 自動偵測骨架
- TTS

### Sprint 4

- 微調
- 複查佇列
- 備援操作

### Sprint 5

- 歷史比較
- 教練註記
- 匯出

## 10. 建議先做的三個技術 Spike

- Spike 1：CameraX + 單靶校正可行性
- Spike 2：數位靶座標與 line-cutter 計分邏輯
- Spike 3：頁面一右側數位靶的拖曳微調互動
