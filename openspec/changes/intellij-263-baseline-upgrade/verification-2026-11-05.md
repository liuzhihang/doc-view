# 2026-11-05 至 06 正式版补验

## 环境与产物

JetBrains [正式发布元数据](https://data.services.jetbrains.com/products/releases?code=IIU&latest=true&type=release) 在本次查询时返回 `type=release`、发布日期 2026-11-05、版本 2026.3、build `263.7307.200`。本机 `/Applications/IntelliJ IDEA.app/Contents/Resources/product-info.json` 一致。独立 app 副本来自该安装，不改签名或普通 app；config/system/log/plugins 均位于本次忽略目录 `build/native-validation/2026-11-05/profile/`，虚构工程位于同目录 `fictional-project/`。

正式版是本轮额外精确验证目标；生产 Gradle 目标、Verifier 配置和最低 build 仍为原 `263.7031.23`。EAP 与正式版结果分开记录，不因为用户确认正式版已发布就宣称原生验收或最低版本升级完成。此前 `/tmp/doc-view-*` 原始证据已不在本机；历史文档保留其原始 hash/结果，但本轮不能重新检查那些临时文件。

| 产物 | 实际值 |
| --- | --- |
| full-11 ZIP SHA-256 | `95c47deb64df9de9c40fab39e5ad07521e8ae61348e9184a08006e5e6d61e6dc` |
| 嵌套 JAR SHA-256 | `2969ebd58a1051cd32f6b399a68d8350644d45d1c9308aa05e138e1863491319` |
| 插件 / 最低 build | `com.liuzhihang.doc-view` 1.4.0 / `263.7031.23` |
| 内容 | 一个插件 JAR；329 个生产 class，均 major 69；无测试依赖 |
| 原生平台 / JBR | IU `263.7307.200` / `25.0.4.1+1-b635.79`，macOS 27.0.1 aarch64 |
| full-11 隔离 PID | `16942`；启用隔离实例 screen-reader support 后重启为 `17875` |

## XML 依赖失败、修复与复验

旧 full-10 ZIP（SHA `b238f5412d134ff4311ba5422fb78d50562bfecf80826964858984d5d69bcc0b`）首次对正式版运行 Plugin Verifier 1.411，出现 **13 compatibility problems**：XML PSI / DOM 类不可解析。已有 `BeansDescription`、`DubboPsiUtils` 使用这些 API，而生产 descriptor 未声明 XML owner plugin；原本只在测试中加入 XML。正式版的 API 所在 bundled plugin 为 `com.intellij.xml`。

受控 descriptor-only 候选加入 `<depends>com.intellij.xml</depends>` 后，同一正式版 Verifier 的 13 项全部消失；该诊断候选不作为最终包。按官方 [Plugin Dependencies](https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html) 同时补齐 `build.gradle` bundled dependency 和生产 descriptor，并移除重复的 test-only 声明。不复制平台 XML JAR，不改变解析规则，也没有新增第三方生产依赖。

真实重建命令为 `JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' ./gradlew test buildPlugin verifyPluginProjectConfiguration verifyPlugin --rerun-tasks --console=plain`。full-11 在 2 分 56 秒完成，18 tasks 全部 executed，67 suites / **352 tests / 0 failures / 0 errors / 0 skipped**。旧 EAP `IU-263.7031.23` 与新正式版 `IU-263.7307.200` 的严格 Verifier 均 Compatible；没有 compatibility、deprecated、internal、override-only 问题，仍有 **16 Experimental API usages**。测试源码另有 15 条 deprecated/removal warnings，Gradle 有未来 10 版本弃用提醒，不宣称全项目零警告。

原始证据均在仓库忽略目录 `build/native-validation/2026-11-05/`：`full11-build.log`、`full11-tests.json`、`full11-package.json`，以及 `verifier/final-full11/verification.md`、`reports/IU-263.7307.200/report.html`。首次失败与 descriptor 候选报告单独保留。

## 原生观察（尚未完整通过）

- 新包正常加载。Spring `findUser` 预览显示 `/api/users/{id}`、GET 和参数；仅由 `fictional-dubbo.xml` 引用、无 Dubbo 注解的 `FictionalXmlService#readXmlUser` 成功显示 Dubbo 文档，覆盖本次 XML 依赖修复路径。
- 参数编辑器先用旧 Schema 2.1.2 验证 deprecated `required=true` 不作为正向来源，两个字段均 false，符合已有 contract。随后使用单独下载并核验的 fixture-only Swagger annotations 2.2.38，`NOT_REQUIRED` / `REQUIRED` 在原生表格中分别显示 false / true。未完成 Confirm 后的源码写回，不能替代 requiredMode、字符串与 Undo 的完整写回矩阵。
- 原生工具窗口显示 Refresh API Catalog、Export、Upload、Expand、Collapse 五个主操作，以及原生 Options。隔离实例开启 screen-reader support 并重启后，通过原生 Tab 聚焦 Export、Space 展开菜单，观察到 Export Markdown、Generate or Update Project Docs、Open Project Docs、Search Project Docs 四项。执行 Export Markdown，在原生 Select Path 中选择本次 `exports/` 目录并确认，出现 `Markdown exported successfully.`。磁盘 `exports/DocView.md` 包含 Feign `create` 和 Spring `findUser` 的路径、请求方法、header、参数与响应；这是工具窗口全量 Markdown 导出代表路径通过，不代表预览 Export 或完整菜单矩阵通过。
- 参数弹窗的 `Copy as JSON` 原生点击成功，出现 `FictionalProfileRequest JSON copied to the clipboard.`；随后在 IDEA 文件/类搜索输入框执行原生粘贴，读回 `{ "displayName": "", "age": 0 }`。此项验证实际剪贴板内容，未把编辑器普通 Copy 当成参数工具栏动作。参数表可选择 Description 单元格，但双击/F2 后未取得可确认的编辑状态，Confirm、转义和 Undo 的写回矩阵继续未通过。
- 预览 Markdown Copy 点击后观察到预览关闭，没有成功通知；再从剪贴板粘贴仍是此前的参数 JSON，故本次 **未通过**。SDK 核对显示 Copy 动作本身不会关闭 popup，自定义 mouse-out 取消仅在事件坐标落到实际 content 外时触发。当前证据不足以区分 CUA 坐标/窗口路由和平台窗口几何问题；没有据此修改生产 popup 逻辑。
- 常规模式的 ActionToolbar 按钮通常不接受 Tab 焦点；screen-reader 模式使工具窗口的 Export 键盘路径可达，但未解决所有 popup/右键动作。原生 Options、方法右键菜单、非方法节点限制、预览 Export 和参数 Confirm 仍未通过，不用底层自动测试代替原生点击。

## 资源 / GC 观察

在隔离 PID 上采集强制 GC 的 live class histogram、线程栈与两份 heap dump。文件名中的 `closed` 仅指执行关闭操作并看到主窗口，不能直接证明所有 popup 都 dispose。`gc/histogram-summary.json` 保留逐样本精确计数；以下计数不能当作同等空闲状态的增长曲线：

| 样本 | PreviewForm | ParamDocEditorForm | MarkdownJCEFHtmlPanel |
| --- | ---: | ---: | ---: |
| PID 16942，第三次预览关闭操作后 | 1 | 0 | 0 |
| PID 16942，第一次参数弹窗关闭操作后 | 0 | 1 | 0 |
| PID 16942，第二次参数弹窗关闭操作后 | 0 | 2 | 0 |
| PID 16942，关闭项目并进入 IDEA Home 后 | 0 | 1 | 0 |
| PID 17875，JSON Copy 成功、Preview Copy 未成功后 | 1 | 2 | 0 |

第一个 dump 中 MAT 的可达参数表单为 1 个，`popup.myDisposed=false`。其强引用路径经过匿名 action、`ActionToolbarImpl.myVisibleActions` 和平台静态 toolbar 集合；这不足以证明“已关闭对象泄漏”。本机 263 SDK 显示 toolbar 在 `addNotify` 加入集合、在 `removeNotify` 移除，公开 `ActionToolbar` 没有可直接调用的 dispose 方法；没有手动调用平台生命周期 hook 或使用 Internal API 修补。

第二个 dump 在确认 JSON Copy 成功后采集，MAT 中剩余可达参数表单也是 1 个：`popup.myDisposed=true`，`myWindow=null`、`myComponent=null`，root/tail panel 的 peer 均为 null。MAT 的合并持有树中出现 action、toolbar、Swing 容器、参数表和 accessibility node，GC-root 类型为线程上的 **JNI Local**；不把合并树强行解释成一条线性路径。这证明该实例的 popup 已释放窗口/内容且组件不再 displayable；不能从该瞬时线程引用推导持续泄漏，也不能据此宣称完整无泄漏。histogram 与后续 dump 是不同时刻，计数不应强行对应。未取得 EDT 事件栈退场后的又一次空闲 dump，是否随后自动收集仍未验证。

项目关闭后会进入 IDEA Home，工具窗口/Welcome 对象数量不可直接按“没有项目”解释。线程样本未显示仍执行中的 Doc View 业务栈，但未完成消息总线 listener 计数、EditorFactory 同项目身份对照、JCEF/native memory 稳态和固定次数循环；冷/热各 7 次性能样本也未执行。资源门禁仍是 **partial**。堆与 MAT 索引只保留在本机受限权限目录，仓库文档只记录类/字段、状态与 root 类型，不写堆内字符串或账户数据。

## 收尾与剩余门槛

- 通过原生 Confirm Exit 退出隔离实例；随后进程读回确认 PID 16942、17875 不存在，没有该隔离 app/JCEF 子进程。普通 IDEA PID 14893 仍运行。
- 普通 2026.3 profile 的 3,622 个基线文件中，3,621 个 hash 未变；只有正在运行的 Air `session-identities.json` 改变，没有回滚该文件。Junie 安装设置 hash 未变，普通 app 与隔离副本的 18 个 Junie 命名 JAR 一致，未卸载或禁用 Junie。
- 日志没有 ERROR/FATAL 行；保留启动警告，以及原生 AX 文本选择期间出现的 AWT `Invalid range` 异常，不能写成零警告或无任何异常。
- 本轮仅修改 XML 依赖及其 contract/验证文档；范围快照保留原有未提交改动。`CLAUDE.md` 是 `AGENTS.md` 的软链接，摘要修改随链接反映。没有提交、推送、发布、归档或 spec sync。
- `openspec validate intellij-263-baseline-upgrade --strict`、`openspec validate project-document-store --strict` 和 `git diff --check` 通过。R2、4.2 继续未勾选；下一步优先取得预览 Copy/Export、参数单元格编辑/Confirm 与菜单的可靠原生操作，再执行等状态的资源循环。正式 MCP 继续等待原生与文档库门槛，未新增 provider、未启用测试 MCP Server。

本机证据根目录仍为 `build/native-validation/2026-11-05/`：`export-readback.json`、`exports/DocView.md`、`gc/histogram-summary.json`、`gc/param-doc-editor-gc-analysis.md`、`cleanup.json`、`normal-profile-after.json`、`junie-final-comparison.json` 与 `scope-after.json`。这些是本机忽略文件，后续清理 build 前应保留所需证据；本记录中的结论和产物 hash 独立保留。47 个 heap/MAT 证据条目已迁移到仓库外 `/Users/liuzhihang/Library/Caches/doc-view-native-validation/2026-11-05/heap/`，目录 0700、文件 0600，没有删除原始证据；共享记录仅保留脱敏结构摘要和 hash。
