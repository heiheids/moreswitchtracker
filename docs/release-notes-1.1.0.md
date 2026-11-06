## 1.1.0 — 自由界面 / Free GUI scale

在「追踪器」和语言按钮之间新增第三个选项卡 **自由界面**，把界面尺寸从原版的四档整数倍率放开成无级调整。

A third tab, **Free GUI scale**, is added between *Tracker* and the language button. It replaces vanilla's four integer steps with continuous control.

### 下载 / Downloads

| 游戏版本 | 加载器 | 文件 |
|---|---|---|
| 1.21.1 | NeoForge 21.1.x | `moreswitchtracker-neoforge-1.21.1-1.1.0.jar` |
| 26.3 | NeoForge | `moreswitchtracker-neoforge-26.3-1.1.0.jar` |
| 26.3 | Fabric | `moreswitchtracker-fabric-26.3-1.1.0.jar` |

同一个游戏版本只放一个 jar。Never install two of these into the same `mods` folder.

### 新增 / Added

- 0.5× ～ 8× 无级滑块，步长 0.05×，松手才提交。
- `原版` `1×` `2×` `3×` `4×` 五个快捷档位。
- 手动输入框，支持 `1.75` 这样的小数。
- 上限按窗口尺寸自动计算，保证界面至少有 320×180 的逻辑分辨率。
- 倍率写入 `config/moreswitches.json`，下次启动自动恢复。

### 修复 / Fixed

**修复点击档位按钮导致游戏直接退出（无异常、无崩溃报告）。** 早期构建在按钮回调里既调用原版「界面尺寸」选项、又显式重建一次界面，两次重建发生在同一次点击中：第一次把正在响应点击的控件从界面上摘掉，第二次在空窗口上继续重建。现在所有尺寸改动都推迟到下一个客户端 tick 统一应用一次，并且不再触碰原版选项。

**Fixes a crash where clicking a preset button quit the game outright** — no exception, no crash report. The button callback both changed the vanilla GUI scale option and called an explicit resize, rebuilding the screen twice within one click: the first rebuild removed the widget that was handling the click, the second rebuilt on an empty window. All scale changes are now deferred to the next client tick and applied once, and the vanilla option is no longer touched.

### 说明 / Notes

- 自由倍率生效期间，原版「视频设置 → 界面尺寸」不再起作用（它只能表达整数档位）。点「原版」按钮即可交回。
- 非整数倍率下文字边缘可能略软，这是所有非整数缩放的固有代价。

完整说明见 [README](https://github.com/heiheids/moreswitchtracker#自由界面110-新增) / [English README](https://github.com/heiheids/moreswitchtracker/blob/main/README_EN.md).
