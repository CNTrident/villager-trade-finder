# Villager Trade Finder 版本记录

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
