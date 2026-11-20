# Villager Trade Finder


[![CI](https://github.com/CNTrident/villager-trade-finder/actions/workflows/build.yml/badge.svg)](https://github.com/CNTrident/villager-trade-finder/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.3-62B47A)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-DBD0B4)](https://fabricmc.net/)

面向 Minecraft Java Edition 1.20～1.21.11（全部 19 个正式版本）以及 26.1～26.3（26.1、26.1.1、26.1.2、26.2、26.3）+ Fabric 的客户端便捷模组。它与对应版本的
[Trade Cycling](https://www.curseforge.com/minecraft/mc-mods/trade-cycling)
配合，在未锁定的职业村民交易界面中自动刷新交易，直到出现满足条件的附魔书或物品。

## 下载

当前源码版本 **1.9.2**，覆盖 Minecraft 1.20～1.21.11 和 26.1～26.3 共 24 个正式版本。

- [1.9.1 历史安装包合集](https://github.com/CNTrident/villager-trade-finder/releases/download/v1.9.1/all-24-installers-1.9.1.zip)
- [1.9.1 历史逐版安装 JAR、源码 JAR 与 SHA-256](https://github.com/CNTrident/villager-trade-finder/releases/tag/v1.9.1)
- 1.9.2 可按下文构建。成品命名为 `villager_trade_finder-fabric-<Minecraft版本>-1.9.2.jar`，安装时选择对应游戏版本的一份 JAR。
- [更新记录](VERSION_HISTORY.md)。历史已发布版本保留在 [GitHub Releases](https://github.com/CNTrident/villager-trade-finder/releases)。

1.20～1.21.11 的源码位于 [`backports/versions/`](backports/versions/)，26.x 使用根目录源码与 [`mc26-matrix.json`](mc26-matrix.json) 中的对应兼容目录。源码 ZIP 不用于安装。

## 功能

- 制箭师的药水箭支持按具体药水类型筛选，中英文自动补全可区分普通、延长和强化版本；药水类型留空匹配任意药水箭。候选优先使用服务器的可交易药水标签。
- 每种村民职业都有独立的目标列表和 AND/OR 模式；物品候选随当前职业变化。村民改名后仍按实际职业显示设置和自动刷新按钮，C / R 用法相同；流浪商人不会启用本模组。
- 盔甲商、武器商、工具商、渔夫和制箭师的物品目标可用逗号添加多个中文/英文目标附魔及对应精确等级。
- 设置目标附魔后会严格匹配完整附魔集合；包含任何未指定的额外附魔都不会判定为命中。
- 其他村民的物品候补来自当前职业的原版数据驱动交易表，并支持中文名、英文名和完整 ID。
- 自动补全候补支持每页 6 项、上下翻页以及 Page Up/Page Down 键盘翻页。
- 每个目标支持 `ON`、`OFF`、`AUTO` 三种状态；`AUTO` 目标匹配并停止后会自动切换为 `OFF` 并保存。
- 可保存多个附魔书目标，并以“任一满足（OR）”或“全部满足（AND）”决定停止条件。
- 找到目标并停止时在聊天栏显示本轮刷新次数；每次重新开始时从 0 计数。
- 手动停止时也会显示本轮刷新次数。
- 附魔输入支持中文名、英文名和完整 ID，配置文件仍保存稳定的完整 ID。
- 目标列表同时显示附魔的中文名和英文名；批量状态按钮依次将全部目标设为 `ON`、`OFF`、`AUTO`。
- 每个目标可独立选择“初始交易”“后续交易”或“全部交易”；后两者会读取 VisibleTraders 提供的锁定交易。
- 新目标输入框使用示例提示；附魔等级和价格会按所选附魔及原版交易范围自动限制。
- 每个目标可设置附魔名称、精确等级、最低和最高原始绿宝石价格。
- 价格使用 `MerchantOffer#getBaseCostA()`，不计入治愈/声望/村庄英雄折扣，也不计入需求涨价。
- 每次只保留一个未完成刷新请求。等本轮交易数据到齐，并在至少 50 毫秒没有新交易回包后再判断。未命中时开始下一轮。
- 筛选后续交易时，VisibleTraders 的清空占位包不算完成；必须等真正的后续数据到齐才判断。找到目标后停止，不发送下一次刷新请求。
- 交易已锁定、服务端不支持、没有目标、网络响应超时或达到 1000000 次安全上限时自动停止。
- 内置简体中文和英文界面。

## 安装

客户端 `mods` 目录放入：

1. 与游戏版本对应的 Villager Trade Finder JAR
2. 与游戏版本匹配的 Trade Cycling（26.x 使用 1.0.21 或兼容新版；旧版本依赖见版本清单）
3. 与游戏版本匹配的 Fabric API

如需匹配“后续交易”或“全部交易”，客户端和服务器还需要安装对应游戏版本的 VisibleTraders；26.1 / 26.1.1 / 26.1.2 对应 2.4.x，26.2 对应 2.5.x，26.3 使用 2.5.1 或兼容新版，旧游戏版本依赖见版本清单。

Villager Trade Finder 仅安装在客户端。多人服务器需要 Trade Cycling 与对应前置；使用后续交易筛选时，服务器也需要 VisibleTraders。单人档在同一个客户端环境准备这些依赖即可。服务器维持原有收包调度和交易生成流程。
游戏运行时，1.20～1.20.4 使用 Java 17，1.20.5～1.21.11 使用 Java 21，26.1～26.3 使用 Java 25。26.3 搭配 Trade Cycling 1.0.23 时需 Fabric Loader 0.19.3 或更新版本。构建本项目使用 Java 25。不要跨 Minecraft 版本混用 JAR。

### 使用注意事项

- 药水箭是制箭师的大师级交易。添加物品“药水箭”（`minecraft:tipped_arrow`）后填写药水类型，例如 `minecraft:strong_poison`；在未锁定村民上预筛未来药水箭时选择“后续交易”或“全部交易”，并安装 VisibleTraders。原版已锁定的村民不能继续刷新。
- 只对尚未完成过交易、仍可刷新职业的村民使用；一旦交易锁定，模组会停止循环。
- 通过右键实际村民打开交易界面；命名村民可正常使用。没有对应实体交互的虚拟或远程交易菜单不启用，关闭菜单、断线和切换世界后须重新右键村民。
- 刷新必须由服务器上的 Trade Cycling 支持；网络响应超时或达到 1000000 次时会自动停止。
- “后续交易”和“全部交易”依赖 VisibleTraders；服务器未提供锁定交易数据时只能可靠判断初始交易。
- 显示和筛选的是原始绿宝石价格，不包含治愈、声望、村庄英雄或需求变化造成的价格调整。
- 自动操作前请确认目标条件与职业列表正确，避免长时间无效刷新。
- 1.20、1.20.3、1.21.2 的 Trade Cycling 未在 Modrinth 单独标注成品；请核对其官方对应版本，不能把其他版本的依赖 JAR 直接混装。

## 使用

1. 右键一个尚未交易锁定的职业村民打开交易界面；命名村民操作相同，不同职业会读取各自独立的目标列表。
2. 按 `C` 或点击齿轮按钮，添加一个或多个目标。例如：
   - 附魔名称：`经验修补`、`Mending` 或 `minecraft:mending`
   - 精确等级：`1`
   - 原始价格：`10` 到 `20`
   - 可附魔物品可填写多个附魔，例如：`耐久, 经验修补`，等级填写：`3, 1`
3. 保存后按 `R` 或点击播放按钮开始。
4. 找到目标后会自动停止并播放提示音；再次按 `R` 可手动停止。快速停止后重启时，会先处理同一窗口上次已经发出的刷新请求。

`C`、`R` 均可在 Minecraft 的按键设置中修改。配置保存在
`config/easyautocycler-filters.json`。

## 构建

使用 Java 25：

```powershell
# Java 25；默认构建 Minecraft 26.2
.\gradlew.bat build

# 构建全部 24 个版本并运行每版回归
python scripts/build_all_versions.py

# 单独构建旧版本
.\gradlew.bat -p backports build -Pmc=1.21.11

# 单独构建 26.3
.\gradlew.bat build -Pminecraft_version=26.3 -Pfabric_api_version=0.163.0+26.3 -Ptrade_cycling_file_id=9155650

# 准备 26.2 的真实收包处理函数回归（不打开窗口或世界）
.\gradlew.bat writeSmokeClasspath -PlowLatencySmoke
python scripts/verify_low_latency.py --java "Java25/bin/java.exe" --visible-traders "Visible Traders-2.5.1-universal.jar"
```

26.x 产物位于 `build/libs/`，旧版本位于 `backports/build/<Minecraft版本>/libs/`。全版本检查日志保留在 `build/matrix-1.9.2/`。构建脚本默认使用本地依赖缓存；首次下载依赖时添加 `--online`。26.x 参数见 [`mc26-matrix.json`](mc26-matrix.json)，旧版参数见 [`backports/matrix.json`](backports/matrix.json)。

## 开源来源与许可

本项目是一个派生作品，参考关系明确如下：

- **代码基础：** 基于 MIT 许可的
  [Uncraftbar/Easy-Auto-Cycler](https://github.com/Uncraftbar/Easy-Auto-Cycler)
  Fabric 26.1.2 分支定制，复用了其配置界面、Trade Cycling 集成和网络同步框架。原作者版权声明已保留在
  [`LICENSE`](LICENSE) 中。
- **功能与交互参考：** 参考 GPL-3.0-only 项目
  [Greeenman999/LibrarianTradeFinder](https://github.com/Greeenman999/LibrarianTradeFinder)
  的多目标附魔、等级和价格筛选思路；本项目未复制或合并其 GPL 源代码。
- **运行依赖：** 使用
  [henkelmax/trade-cycling](https://github.com/henkelmax/trade-cycling)
  提供的未锁定村民交易刷新协议。Trade Cycling 不会被打包进本项目 JAR，玩家需要单独安装它。
- **可选兼容：** 通过可选反射桥接兼容 MIT 许可的
  [Ramixin/VisibleTraders](https://github.com/Ramixin/VisibleTraders)，读取其提供的后续锁定交易；未复制或打包其源码。

本项目自身以 [MIT License](LICENSE) 发布。提交问题请使用
[GitHub Issues](https://github.com/CNTrident/villager-trade-finder/issues)，参与开发前请阅读
[CONTRIBUTING.md](CONTRIBUTING.md)。

本项目不是 Mojang/Microsoft 的官方产品，也不受其认可或关联。
