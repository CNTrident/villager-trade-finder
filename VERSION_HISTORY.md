# Villager Trade Finder 版本记录

## 1.9.4 · 全部 24 版启用加速 · 2026-11-20

- 原版本 1.9.3 → 新版本 1.9.4。把带轮次的最终报价移植到 Minecraft 1.20～1.21.11。五个 26.x 安装包继续使用已验证的加速流程。
- 旧版初始、后续、全部模式用同一轮次确认。Trade Cycling 只返回初始报价时，后续与全部模式等待并读取 VisibleTraders 为本轮生成的后续交易。确认本轮未命中后开始下一轮。
- 全部支持的 Minecraft 版本单人自动启用。多人在对应服务端选装同版 JAR 即可启用。未提供编号报价的服务器保留原兼容流程。
- 验证与成品清单见 [VALIDATION.md](VALIDATION.md)。本地安装包、源码包、全 24 版 ZIP 与 SHA-256 在 `releases/1.9.4/`。
- 修改前备份为 `backups/villager-trade-finder-1.9.3-before-all-versions-acceleration-2026-11-20-dd1de100/`，1027 个文件核对 SHA-256，另存 Git bundle。更新前远端提交为 `1059097b9042f654a2e4fe393fc08fbf727ec702`，回滚标签为 `backup/pre-vtf-1.9.4-20261120-dd1de100`。
- 首次创建旧版共享编号回包适配源码、1.9.4 的完整发行、版本清单、[发布说明](release-notes/1.9.4.md) 和本地验证记录。

## 1.9.3 · 全部 24 个游戏版本 · 2026-11-20

- Villager Trade Finder 1.9.2 → 1.9.3，覆盖 1.20～1.21.11 的 19 个正式版本，以及 26.1、26.1.1、26.1.2、26.2、26.3。
- 26.x 单人和选装同一 JAR 的多人服务端提供带轮次的最终报价。客户端收到本轮所需报价后立即筛选，旧轮次报价不能更新当前菜单。相同报价在不同轮次仍会正常处理。
- 1.20～1.21.11 为客户端兼容构建，沿用 50 毫秒报价稳定窗口。26.x 普通服务端也走兼容流程。后续与全部交易仍等待 VisibleTraders 正式数据。
- 验证：24 个版本均完成构建与自动回归。19 个旧版 JAR 除版本声明外与 1.9.2 内容一致。26.3 单人 135 次初始刷新从 15.212 秒降到 6.861 秒；后续交易的两条路径各完成并检查 113 轮。26.3 本地 TCP 实测和 26.2 实际回包处理器测试见 [VALIDATION.md](VALIDATION.md)。
- 备份：加速开发前保留 `backups/villager-trade-finder-1.9.2-before-confirmed-refresh-2026-11-20-89080285/`。全量发行前另存 `backups/villager-trade-finder-1.9.3-before-all-versions-release-2026-11-20-79385eae/`，973 个文件均核对 SHA-256，含源码、构建配置、已存在的成品及 Git bundle。
- 回滚点：发行前远端提交 `64300478290ed4d1c80ebad43026bd35aa5f0532`，备份标签 `backup/pre-vtf-1.9.3-20261120-79385eae`。可用 `git revert` 撤销本次提交，或从备份标签恢复文件后追加提交。
- 首次创建：26.x 带轮次报价的网络处理、ServerPlayer 报价 Mixin、`releases/1.9.3/`、19 个旧游戏版本的 1.9.3 安装及源码 JAR、全 24 版 ZIP、[发布说明](release-notes/1.9.3.md) 和本地逐版验证日志。按游戏版本选择一个安装 JAR。

## 1.9.2 · 全部 24 个游戏版本 · 2026-11-20

- Villager Trade Finder 1.9.1 → 1.9.2。检查 24 个游戏版本的 20 套源码，均存在按窗口 ID 收到一个报价包就立即开始下一轮的逻辑。26.3 配合 Trade Cycling 1.0.23 实测一次请求会回三份报价，重复确认会造成请求积压，覆盖已经命中的初始交易。
- 修复：每轮等所需交易数据到齐，并在 50 毫秒内没有新交易数据后，由客户端 tick 判断结果。未命中时只发一份下一轮请求。快速停止再开始时，等同一窗口已发请求处理完再继续。
- 验证：26.3 临时单人世界完成三轮经验修补目标和快速停止再开始检查。三轮 135 次请求收到 405 份报价，命中后请求数未增加，重开窗口目标保留。另以本地 TCP 连接 26.3 专用服务器，66 次请求和 198 份报价同样通过。逐版构建与验证范围见 [VALIDATION.md](VALIDATION.md)。
- 成品与清单：`releases/1.9.2/` 首次创建，包含 24 份安装 JAR、源码包、网盘 ZIP 和 SHA-256。旧版成品可从原路径或备份恢复。
- 修改前备份：`backups/villager-trade-finder-1.9.1-before-offer-fix-2026-11-20-3faa1b4b/`，1022 个文件的 SHA-256 已核对，含源码、构建配置、说明、旧成品和 Git bundle。
- 回滚点：远端旧提交 `4575f4ec61fd0e85719913f28c4c33dc643e402b`，标签 `backup/pre-vtf-1.9.2-20261120-3faa1b4b`。可以用 `git revert` 撤销这次更新，也可从备份标签恢复文件后追加提交。

## 1.9.1-docs.1 · GitHub 同步与发布准备 · 2026-11-01

- 原说明版本 1.9.1 → 1.9.1-docs.1；24 个游戏版本的安装 JAR 仍为已验证的 1.9.1，源码 JAR、安装合集和校验值均保留。
- README 将命名村民使用方式并入原功能和使用注意事项，下载入口定位到 v1.9.1；更新内容另存 release-notes/1.9.1.md，验证摘要见 VALIDATION.md。
- 旧版生成模板补齐现有各版源码已包含的菜单关联调用；发布脚本兼容 README 附件并检查源码包及所有附件 SHA-256。未改动运行时源码或成品类文件。
- 修改前备份：`backups/villager-trade-finder-1.9.1-before-github-sync-2026-11-01-0bd6fb2d/`，902 个文件已核对 SHA-256，另存 Git bundle。
- 更新前远端提交：`e44ae3d60909c20a72a8ef275bdcd25e98afd4b2`；回滚标签：`backup/pre-1.9.1-sync-20261101-0bd6fb2d`。可对本次更新提交执行 git revert，或从回滚标签恢复文件后新建提交；不改写原有历史。
- 验证：沿用 24 版构建及 26.2 实体关联/回包检查；本批次核对成品校验值、模板一致性、README 链接和发布脚本语法。GitHub CI 与发布结果单独核对。
- 首次创建：VALIDATION.md、release-notes/1.9.1.md、release-manifests/1.9.1.json 和 GitHub v1.9.1 Release。

## 1.9.1 · 全部 24 个版本 · 2026-11-01

- 原版本：当前根目录及旧版本适配源码 1.9.0；新版本：1.9.1。
- 修复：移除通过交易界面标题解析职业的逻辑。记录客户端实际右键交互的村民，将首次打开的交易菜单与该实体关联，直接读取其同步职业数据；自定义名称、语言或修改标题不会改变职业识别。
- 更新：返回设置界面后保留同一菜单关联；关闭或更换菜单、断线、切换世界、交互其他实体及打开菜单超时会清理或拒绝旧关联。没有对应村民交互的菜单不猜测职业；无职业、傻子和流浪商人仍不启用。
- 兼容：26.x 使用合并后的实体交互方法；1.20～1.21.11 同时记录 interact / interactAt。根据各版实际 API 读取职业注册表键，不依赖界面标题或名称。
- 修改前备份：`backups/villager-trade-finder-1.9.0-before-named-villager-fix-2026-11-01-45c6aa17/`；保留原源码、构建配置、说明文档、各版本已有成品，SHA-256 核对通过。
- 首次创建：VillagerTradeContext、VillagerInteractionMixin、NamedVillagerSmoke、覆盖层同步脚本、`releases/1.9.1/` 和 `validation/1.9.1/`。历史副本 `librarian trade cycler/` 仍保留原 1.8.2，不替换其源码或历史成品。
- 验证及范围：[1.9.1 验证记录](validation/1.9.1/README.md)。未在真实存档中操作或进行完整游戏内人工验证。
- 成品：`releases/1.9.1/villager_trade_finder-fabric-<Minecraft版本>-1.9.1.jar`；仅选择匹配游戏版本的一份替换客户端旧版 JAR，原配置继续使用。

## 1.9.0 · 全部 24 个版本 · 2026-10-30

- 原版本：26.2 本地版 1.8.2；其余版本 1.8.0。统一升级到 1.9.0。
- 修复漏判：VisibleTraders 的 Optional.empty 清空包表示后续交易仍在生成，不能作为完成信号。只有实际后续数据及本轮初始数据都收到后才检查，命中时不再发送下一包。直接保存本轮后续包快照，避免读取 UI 合并缓存。
- 流程边界：保留本轮生成、完整回包、目标匹配及必要等待；仅取消本轮未命中后、下一次请求前的额外等待。撤销 1.8.1/1.8.2 的服务端收包调度和 PacketProcessor 提前处理修改，恢复仅客户端安装。
- 兼容迁移：1.20～1.21.11 的 19 版使用同一完整交易包完成本轮判断；26.1～26.3 的 5 版区分 VisibleTraders 占位包与完整后续包。补齐旧版 VisibleTraders net.ramgames.visibletraders.ducks API 名称。
- 上限：所有版本 MAX_CYCLES_SAFETY 从 100000 放宽到 1000000；配置、价格与严格附魔集合匹配规则不变。
- 修改前备份：`backups/villager-trade-finder-1.8.2-and-backports-1.8.0-2026-10-30-f1cc61f0/`（2396 文件 SHA-256 核对，含 Git bundle）；修复前再次备份：`backups/villager-trade-finder-1.8.2-before-offer-readiness-fix-2026-10-30-9545bbb0/`。新增检查、脚本和交付目录首次创建；删除的服务器 Mixin 已在备份与原成品保留。
- 验证：三种回包顺序使用真实 vanilla/VisibleTraders 处理函数进入实际 AutomationManager，延迟耐久 III 钻石镐命中后额外刷新请求为 0；同一检查加载 1.8.2 准确复现提前续刷。各版本构建与筛选/收包回归、最终成品检查结果见交付版本清单。未进行 24 个版本的真实多人世界速度实测。
- 成品：`releases/1.9.0/` 中逐版 JAR、全部安装包 ZIP、版本清单和 SHA-256。GitHub 同步本项目源码与成品，不包含其他 Mod、构建缓存或本机诊断日志。

## 1.8.2 · Minecraft 26.2 · 2026-10-30

- 原版本：1.8.1；新版本：1.8.2。
- 原因核查：已检查实际安装的 Trade Cycling 1.0.21+26.2 字节码，其刷新路径没有固定冷却、sleep 或时间戳限速。26.2 的 PacketProcessor 先排队，再由 MinecraftServer 在 tick 开头取包；1.8.1 只去掉回调中的重复调度，未处理此等待层。
- 优化：仅在 Trade Cycling 刷新包进入当前服务器的原版收包队列后，唤醒主线程 executor 提前处理该 FIFO；同样适用于单人内置服务器。原始入队、包顺序、断线校验、错误处理、菜单检查、职业锁定检查保留。
- 并发：合并尚未执行的唤醒任务；处理前复位标志，防止并发到包丢失唤醒。不在网络线程修改村民或世界，不修改服务器 tick 速度。
- 修改前备份：`D:/Desktop/mcmod/backups/villager-trade-finder-1.8.1-2026-10-30-7224f292/`，2370 文件（212362014 字节），源码、配置、说明和成品 SHA-256 全部核对。
- 首次创建：TradePacketProcessorMixin、PacketProcessingChecks、1.8.2 成品与验证目录。根目录与 librarian trade cycler 两处项目同步。
- 验证：Gradle build、原筛选与 100000 轮收包回归通过；最终 JAR 通过真实 Fabric Loader 下客户端/专用服务端 × 有/无 VisibleTraders 四种加载检查。新检查使用真实 PacketProcessor 验证 10000 包在 tick 计数保持 0 时完成，并检查 FIFO、合并唤醒、关闭菜单、断线、其他处理器与关服路径。
- 旧版对照：使用同一新检查加载 1.8.1 JAR，准确复现“刷新入队但未唤醒主线程”并按预期失败；1.8.2 四组合通过。
- 验证范围：上述为调度与加载回归，不是用户存档或完整交易生成流程的每秒速度实测。客户端帧处理、交易生成、VisibleTraders 全等级生成、繁忙主线程和网络往返仍可能影响速度。未保证固定刷新次数。
- 依赖：Java 25、Fabric Loader ≥0.19.3、Fabric API 0.158.0+26.2、Trade Cycling 1.0.21+26.2；可选 VisibleTraders 验证为 2.5.1。仅验证 Minecraft 26.2。
- 成品：`releases/1.8.2-26.2/villager_trade_finder-fabric-26.2-1.8.2.jar`；验证：`validation/1.8.2-26.2/`。单人只需替换客户端中的该模组 JAR 并重启游戏，内置服务器一起生效；多人须服务端同装。

## 1.8.1 · Minecraft 26.2 · 2026-10-30

- 原版本：1.8.0；新版本：1.8.1。本记录文件首次创建。
- 优化：客户端取消交易更新后的 tick 等待；移除每轮反射调用、重复构造无状态 payload 和逐包 trace 日志；只筛初始交易时跳过 VisibleTraders 合并拷贝。
- 更新：按本轮数据包事件判断结果，初始/锁定交易回包支持两种先后顺序；命中即停止，未命中立即发下一请求。菜单身份、超时和单次在途请求校验保留。
- 服务端：新增 Mixin，仅针对 Trade Cycling 收包回调中的 MinecraftServer.execute；已在主线程则直接运行，其他线程保留一次必要调度。保留 Trade Cycling 的村民、工作站和交易锁定检查。元数据允许双端加载，客户端代码仍仅从 client 入口及 client mixin 加载。
- 构建默认改为 Minecraft 26.2，Java 25 / Fabric Loader 0.19.3 / Fabric API 0.158.0+26.2 / Trade Cycling 1.0.21+26.2。VisibleTraders 验证版本 2.5.1。
- 修改前备份：`D:/Desktop/mcmod/backups/villager-trade-finder-1.8.0-2026-10-30-34e20bc8/`，完整源码、构建配置、文档和原成品共 2334 文件（211775605 字节），逐文件 SHA-256 复核通过；不含可再生成缓存及旧备份嵌套。
- 首次创建：OfferUpdateState、两个 Mixin、收包回归测试、实际 Fabric 加载检查、smoke.gradle、校验脚本、本记录及 `releases/1.8.1-26.2/`。
- 验证：Gradle build、100000 轮无 tick 收包状态回归、原药水箭/交易筛选回归均通过；最终成品在真实 Fabric Loader 0.19.3 下完成客户端/专用服务端 × 有/无 VisibleTraders 四组合加载检查。每组合复现原嵌套调度入队，并验证 10000 次主线程调用立即执行、非主线程正确交接且仅执行一次。
- 验证资料：工程 `validation/1.8.1-26.2/` 保存最终构建及四组合加载日志；测试在创建世界或窗口前退出。
- 验证限制：不含真实多人服务器吞吐量或每秒刷新次数测量，不将主线程/网络往返时间宣称为零；其他 23 版本保留 1.8.0，未重新构建。
- 本地成品：`villager_trade_finder-fabric-26.2-1.8.1.jar`，客户端与服务端使用同一文件。未上传外部平台。
