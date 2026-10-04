# MyDiary

个人打卡 + 周期任务管理 App（Android）

> ⚡ 本项目全程由 AI 作为主力完成开发（代码生成、架构设计、调试、构建、发布），人类负责需求定义与验收。

## 功能 / Features

- ✅ 每日打卡（标签分类）
- 📅 周期任务模板（weekly / monthly，可指定触发日）
- 📊 统计页（完成率、历史明细）
- 📤 JSON / SQL 导出导入（换机迁移）
- 🌙 睡前提醒
- 🎨 三套主题（清晨绿 / 午间蓝 / 傍晚黄）+ 暗色模式

## 技术栈 / Tech Stack

| 层 | 技术 |
|---|---|
| UI | Jetpack Compose + Material 3 |
| 数据 | Room (SQLite) |
| 语言 | Kotlin |
| 构建 | Gradle + AGP 9 + R8 |
| 最低 SDK | Android 12 (API 31) |

## 构建 / Build

```bash
cd app
./gradlew assembleRelease
```

签名配置见 `app/gradle.properties`（本地文件，不入库）。

## Release APK

下载最新正式版：[Releases](https://github.com/Smog0422/myDiary/releases)

---

# MyDiary

Personal check-in + recurring task manager for Android.

> ⚡ This project was developed entirely with AI as the primary driver (code generation, architecture, debugging, build, and release). The human handled requirements definition and acceptance.

## Features

- Daily check-ins (tag-based categories)
- Recurring task templates (weekly / monthly, configurable trigger day)
- Statistics page (completion rate, history details)
- JSON / SQL export & import (device migration)
- Bedtime reminder
- Three themes (Morning Green / Noon Blue / Dusk Yellow) + dark mode

## Tech Stack

| Layer | Tech |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Data | Room (SQLite) |
| Language | Kotlin |
| Build | Gradle + AGP 9 + R8 |
| Min SDK | Android 12 (API 31) |

## Build

```bash
cd app
./gradlew assembleRelease
```

Signing config lives in `app/gradle.properties` (local file, not committed).

## Release APK

Download the latest signed build: [Releases](https://github.com/Smog0422/myDiary/releases)
