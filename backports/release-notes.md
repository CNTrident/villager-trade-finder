支持 Minecraft Java Edition **1.20～1.21.11 的全部 19 个正式版本**，并提供 26.1.2、26.2 的对应构建。

功能：

- 每个村民职业独立保存交易目标；支持多个目标、AND/OR 和 ON/OFF/AUTO。
- 支持中文、英文和完整 ID 输入及分页自动补全。
- 附魔书和附魔装备支持等级、原始价格及完整附魔集合筛选。
- 制箭师药水箭可指定药水类型，区分普通、延长和强化版本；留空匹配任意药水箭。
- 支持初始、后续和全部交易范围。预筛大师级药水箭等未来交易，需要双方安装对应版本的 VisibleTraders。
- 每次收到新的村民交易数据后再进行下一次刷新；找到目标和手动停止时输出本轮次数，单轮上限 100000 次。

安装：

1. 只选择**与游戏版本完全对应的一份安装 JAR**放入客户端 `mods` 目录；文件名含 `-sources` 的是源码，不能作为模组安装。
2. 安装对应版本的 Fabric Loader、Fabric API 和 Trade Cycling。多人服务器也必须安装 Trade Cycling。
3. 1.20～1.20.4 使用 Java 17 或游戏指定的兼容 Java；1.20.5～1.21.11 使用 Java 21；26.x 使用 Java 25。
4. 打开尚未交易锁定的职业村民，按 `C` 编辑目标，按 `R` 开始/停止。药水箭目标先输入“药水箭”，再选择具体药水类型。
5. 后续/全部交易需要对应游戏版本的 VisibleTraders。原版已交易锁定的村民不能继续刷新。

附件 `version-manifest.json` 列出每个游戏版本的 Fabric API、依赖下载信息及安装包校验值。1.20、1.20.3、1.21.2 的 Trade Cycling 未在 Modrinth 单独标注成品，请核对官方对应版本，不能直接混用其他版本依赖。

`installers.zip` 包含 1.20～1.21.11 的 19 份安装包，解压后只安装匹配游戏版本的一份。`SHA256SUMS.txt` 可核对所有附件。每版源码可在仓库 `backports/versions/<Minecraft版本>/` 查看；26.x 使用仓库根目录源码。

协议：MIT。保留 Easy Auto Cycler 原作者 Uncraftbar 及 CNTrident 的版权声明；Trade Cycling、VisibleTraders 需单独安装，未打包进本模组。
