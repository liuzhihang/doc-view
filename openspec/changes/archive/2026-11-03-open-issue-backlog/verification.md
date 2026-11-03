# 验证记录

## 基线与工作区

- 2026-11-03 重新读取 GitHub：开放 Issue 为 #89、#90、#91、#96、#97、#101；远端 develop/master 均为 `7b2e0a849fe0fe54df6df08792579a361b712428`。
- `/Users/liuzhihang/workspace/doc-view` 的 develop 有维护者未提交开发工作。本轮未修改该工作区；在独立副本 `/Users/liuzhihang/Documents/Codex/2026-11-03/task/doc-view` 的 `fix/open-issue-backlog` 分支实施。
- 基线保留 Java 21、Gradle 9.5.0、IPGP 2.16.0、IU 2026.1；不包含原工作区 Java 25/IU 263 升级。
- 先创建并验证 OpenSpec proposal/design/3 specs/tasks，达到 apply-ready 后开始测试和运行时修改。适用技能为仓库 `.codex/skills/doc-view-contract-design`、`doc-view-java-maintenance` 与 OpenSpec proposal/apply/sync/archive。

## 逐项结论

| Issue | 证据与实现 | 结论 |
| --- | --- | --- |
| #89 | 接口级 `DocView.since/version`、字段自身标签、模板条件段落/变量目录；保留正文中的标签字样；多行标签按数据元素读取并保留词间空格 | 可以支持，已实现；自定义模板仍由用户显式插入变量 |
| #90 | 数组路由、`path` 别名、常量选取第一条；既有模板生命周期修复重跑 10 项测试 | 路由已修复；设置丢失原问题已有修复，不重复改设置逻辑 |
| #91 | 原图确认为 Dubbo 多参数。原截图的普通参数场景在本轮修改前已通过；新增精确名称、多行及空标签回归，修复 `id` 误匹配 `identifier`、多行截断和多余空白 | 原始基础场景已有实现，本轮补齐可复现边界 |
| #96 | 查看两张原图与维护者评论；仅在 Markdown 投影去掉合成 element/key/value，并重算普通表前缀、修正分表标题 | 已修复；不扩大此前不支持的任意容器类型解析 |
| #97 | 实体编辑器复制当前单元格及草稿值为普通/分表 Markdown；无 PSI 快照、后台渲染、空/加载/关闭/项目释放检查 | 已实现并自动测试；续验已完成 IU 261 普通/分表、草稿值、空态禁用和无源码写回的原生烟测，见下文 |
| #101 | 真实编译 JAR + sources JAR，使用 IntelliJ 公共导航读取源码字段，仅替换注释来源；覆盖继承、嵌套、泛型、注解优先级与缓存刷新 | 已实现；需 IDE 关联正确版本源码，纯字节码无法恢复 Javadoc，不自动下载源码 |

六项均存在可支持路径，没有以“实现困难”或“未验证”为由关闭。不支持的输入条件已说明，不等同于整个 Issue 不支持。本轮不修改 GitHub Issue 状态。

## 自动验证

- 未修改代码基线：`./gradlew --offline test --rerun-tasks --no-build-cache`，49 suites / 237 tests，0 failures/errors/skipped，14 tasks executed。
- 首轮新增测试 28 项：23 失败。#90、#91、#89、#97 的失败直接指向功能；#96 缺完整 JDK、#101 测试 JBR 无编译器属于夹具问题，未当作功能 RED。
- 校正夹具后：#96 的 6 项全部因合成层失败，输出与原图一致；#101 的 3 项因预期源码注释为空失败，1 项既有本地源码行为通过。
- 第一轮 GREEN 修正了标签尾部 Javadoc 格式字符。参数版本标签追加 2 项多行 RED，确认原 `joining("")` 丢失词间空格，再统一使用解析工具修复。
- 全量首轮发现默认模板旧 SHA 与复制关闭测试的同步时序假设两处失败。模板 SHA 按批准的模板变更更新，保留两个语言资源完全一致和无标签 golden；关闭测试通过公开进度监听器与 latch 固定真实后台先后顺序，不删除或放宽断言，不修改生产实现来迎合测试。
- 最终命令：`./gradlew --offline test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks --no-build-cache`，`BUILD SUCCESSFUL in 1m 15s`，18 tasks 全部实际执行。55 suites / **267 tests，0 failures/errors/skipped**；其中本轮新增 30 tests。配置校验与打包成功，Plugin Verifier 1.411 对 **IU-261.22158.277** 给出 **Compatible**，没有 compatibility/internal/deprecated API 问题。
- ZIP/JAR 读回校验：descriptor 版本 1.4.0、since-build 261；未包含 contract 测试类、testData 或 JUnit 类。无标签基础 Markdown golden 仍通过。
- 开发候选 ZIP SHA-256：`8534643cc7b8ba75ef9c893a84b414796c49e7a811631532fd9c7fa3423b33be`。

测试过程没有新增运行时依赖、平台版本或 extension。日志中既有测试 API 弃用警告不等于插件兼容问题；以 Verifier 报告为准。

## 原生 UI 烟测：首次尝试与续验

1. 使用 `runIde --args=<workspace>/qa-project` 启动虚构实体工程，日志确认 IU-261.22158.277 及 `Loaded custom plugins: Doc View (1.4.0)`，config/system/log/plugins 均在独立 Gradle sandbox。
2. Computer Use inventory 可发现 `Main / com.jetbrains.jbr.java`，但 `getApp("com.jetbrains.jbr.java")` 返回 `Invalid app`，无法操作按钮。
3. 停止本轮 runIde 进程（exit 130）后，以相同官方 IU 261 的现有原生 app wrapper 和相同隔离 properties 重试；macOS 返回 `Launch failed / NSPOSIXErrorDomain Code=163 / Launchd job spawn failed`。
4. 首次尝试没有修改日常 IDEA 配置，没有安装到用户日常 IDE，也没有通过写剪贴板/配置文件冒充 UI 操作。当时原生按钮可见性与交互未验证；以下续验补齐了本次复制功能的原生证据。

这两次是工具绑定/应用启动失败，不是自动审批拒绝。其他构建命令因沙箱不能写 `~/.gradle` 锁文件，正常申请执行权限后成功，未发生自动审批拒绝。

### 2026-11-03 原生续验

- 启动诊断：旧 wrapper 的 `Contents` 指向 Gradle 扁平缓存，系统 `amfid`/launchd 日志报告 invalid Info.plist、broken signature 与 OS_REASON_EXEC；这解释了 spawn163。严格验签还识别缓存额外的 `Resources/.toolbox-ignore`。没有通过重签名、移除隔离属性或关闭安全机制来启动。
- 正常替代路径：从已缓存的官方 `idea-2026.1-aarch64.dmg` 只读挂载并完整复制应用，得到物理 `Contents` 结构；`codesign --verify --deep --strict --verbose=4` 返回 0，报告 `valid on disk`、`satisfies its Designated Requirement`。镜像已正常卸载。完整应用用独立 `IDEA_PROPERTIES` 启动成功，可通过 Computer Use 正常绑定。
- 精确目标为 **IU-261.22158.277**；另用日常已安装的官方 **IU-262.11879.100 / 2026.2.4** 做补充验证。两者都使用任务目录内的独立 config/system/log/plugins 和虚构 Java 工程，日志确认加载同一个 `Doc View (1.4.0)` 候选 JAR。日常安装及其插件没有被改装。
- IU 261 实际流程：在类名处执行 `Doc Editor`，看到 `Copy as Markdown`；把 `id` 的 Required 从 false 改为 true，在 Description 中键入 `Edited active cell QA 261`，保持单元格处于编辑中直接点击复制。弹窗关闭，显示 `Issue97Entity Markdown copied to the clipboard.`；通过系统粘贴快捷键粘贴到 `result.md` 后，读到 `|id|String|Y|Edited active cell QA 261||` 和嵌套字段 `|-->name|String|N|Displayname.||`。
- IU 261 空态：对没有字段的 `EmptyEntity` 打开实体编辑器，AX 明确显示 `Copy as Markdown` 为 disabled。
- IU 261 分表：通过设置 UI 的 `Template and Output > Output options > Separate entities` 启用分表；重新打开原实体时看到原始 false/Identifier.，复制并实际粘贴得到顶层字段表、`- Detail detail` 标题和只含 `name` 的子表。
- IU 262 补充流程：同样验证按钮可达、活动描述单元格直接复制、成功提示/弹窗关闭、系统粘贴以及普通嵌套表。必填值的完整修改与分表验收以 IU 261 记录为准。
- 两个样例的 `Issue97Entity.java` 验收前后 SHA-256 均为 `d3563025a2f3ea957a215afb7584454c367df63ab0e95f74cc36cf90ad4cb2d7`，复制没有写回源码。所有复制/编辑/粘贴均由原生按钮与键盘操作完成；shell 只用于准备虚构工程、启动官方应用及读取/保存证据，没有写入剪贴板。
- 两个安装环境的插件 JAR 与候选 ZIP 中的 JAR SHA-256 均为 `bd12fb75391470d1fa60f7428b42e3ee234afbf3a44f0fdf7ec38b0501aa9083`；两份 IDE 日志的 ERROR 计数均为 0。
- 测试实例正常退出；日常 `/Applications/IntelliJ IDEA.app` 已恢复至测试前的 Welcome 页面，未打开维护者工作区。候选包及生产代码未在续验中修改。

证据位于副本同级 `evidence/native261-launch-preparation.txt`、`native261-copied-markdown.md`、`native261-split-markdown.md`、`native262-copied-markdown.md` 和 `native-ui-final-checks.txt`；原始日志保留在 `qa-native261/log/` 与 `qa-native262/log/`。此次是本 change 的实体复制烟测，不宣称覆盖发布清单中的所有 UI、上传、Dumb Mode 或高负载场景；关闭/释放等时序边界由前述自动测试覆盖。

## 交付和后续

- 本轮只形成独立分支与开发候选包；未合并、未发布、未发 tag、未关闭 Issue。
- 发布版本仍为当前基线 1.4.0；候选 ZIP 不代表新的正式版本。正式发布需另行选定新版本，并按发布清单确认整体发布范围；本次实体复制的原生烟测已完成。
- 修复尚未进入远端发布分支，支持项保持打开。原工作区未提交功能不应随本轮修复一并推送。
- 原始测试 XML、构建日志和 UI 阻碍记录位于副本同级 `evidence/`；最终汇总以本文件记录为准。
- 已按用户要求执行 `openspec archive open-issue-backlog --yes --json`，归档至 `2026-11-03-open-issue-backlog`，同步 3 份规格、6 项 requirements；`openspec validate --specs --strict` 为 8 passed / 0 failed，`git diff --check` 通过。续验仅补充归档中的事实与 task 记录，不改变 runtime 或长期 requirement，不代表发布或 Issue 已交付。
