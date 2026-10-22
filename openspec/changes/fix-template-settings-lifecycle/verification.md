# 验证记录

日期：2026-10-18。起点：`develop` / `00f1f944c7b22cc03219f06324d177fecf9e848f`。用户已授权 apply；不提交、发布、sync 或 archive。

## 当前结论

模板状态与模板页资源释放修复已完成。旧实现原生失败复现、修复后按钮/重启/双项目/主题验收已补齐；原生验收发现的 `isModified()` 读锁警告也已最小修复并复验。最终包的定向测试 10 项、全量测试 26 项均通过，IU-261.22158.277 Verifier 为 Compatible；实际桌面验收使用官方安装的 IU-262.10968.63。两种证据不能互相冒充。

下文按时间保留早期连接失败及当时未验收的记录；最终状态以末尾“原生验收完成”和“最终包复验”两节为准。发现的 `PreviewForm` 泄漏及一次预览停滞另列后续问题，本 change 没有修复预览窗口，也不构成完整发布验收。

## 范围与基线

- 保留原有 AGENTS、README、docs 和 `.agents/` 改动；本次生产修改限于 `TemplateSettingForm` 与 `TemplateConfigurable`。
- Java 21、Gradle 9.5.0、IntelliJ Platform Gradle Plugin 2.16.0、IU 2026.1、Doc View 1.3.14；不升级依赖或平台。
- 所有模板、项目及生成数据均为虚构验收样例。测试结果与原生 UI、实际磁盘、重启证据分别记录。

## 执行记录

### T1 初始测试基础设施

命令：`./gradlew test --tests 'com.liuzhihang.doc.view.contract.TemplateSettingsLifecycleTest' --rerun-tasks`。

1. 默认沙箱不能写入 Gradle wrapper 的 `.lck` 文件，未执行测试；授权范围内提升执行权限后重试。
2. 首次编译发现测试误用了不存在的 `EditorFactory.getEditors(Project)` 重载，失败于 `compileTestJava`，没有执行用例。这不是模板 bug 的 RED；先修正测试使用的目标平台 API。

### T1 模板覆盖 RED

修正测试 API 后使用同一命令重跑，实际 **1 tests / 1 failures / 0 errors / 0 skipped**。`testCreateAndResetKeepSavedCustomTemplates` 通过 `createComponent()` 后的保存值/编辑器检查，随后失败于 Reset 后的保存值断言：期望自定义 `SPRING_A` 模板，实际变为以 `**接口名称:**` 开头的内置模板。

原始 XML：`/tmp/doc-view-template-lifecycle-evidence/t1-red.xml`（测试时间 `2026-10-17T16:51:05.938Z`）。这证明真实项目 service 的内存状态被覆盖；不代表已经核对磁盘或真实 Cancel 按钮。

### 状态修复 GREEN：T1–T8 与默认输出

生产修改仅将 Reset 改为同步 `WriteCommandAction` 内把项目已保存字符串写回两个 Editor Document；Apply、比较语义、存储与默认资源不变。

命令：`./gradlew test --tests 'com.liuzhihang.doc.view.contract.TemplateSettingsLifecycleTest' --tests 'com.liuzhihang.doc.view.contract.SpringMarkdownContractTest' --rerun-tasks`。

实际 **10 tests / 0 failures / 0 errors / 0 skipped**，其中生命周期 9 项、默认 Spring golden 1 项（测试时间 `2026-10-17T16:56:32.379Z` 起）。覆盖 Reset 幂等、单项/双项 Apply、中文/空格/空行/Velocity/空字符串、未应用关闭、Apply 后再次编辑、重开、当前版本 XML serialize/deserialize/loadState 与 Spring/Dubbo 实际 PSI 生成选模板。

此时关闭用例通过 form disposal 模拟草稿丢弃，测试 tearDown 兜底释放 editor；尚不能证明 Configurable 关闭链路或生产 editor 释放。后续 T9 将关闭操作改为真实 `disposeUIResources()` 回调并在兜底之前断言资源释放。XML 往返不等于真实磁盘重启。

原始 XML 备份：`/tmp/doc-view-template-lifecycle-evidence/state-green/`。既有 golden 未修改。

### 原生 IDE 验收尝试与当时阻碍（历史记录）

已准备两个不含真实数据或网络依赖的临时项目，各有 Spring Controller、Dubbo 接口及本地注解桩：`/tmp/doc-view-template-lifecycle-spring`、`/tmp/doc-view-template-lifecycle-dubbo`。

已执行 `./gradlew -I /tmp/doc-view-template-lifecycle-run.init.gradle -PlifecycleSandboxId=spring runIde --args='/tmp/doc-view-template-lifecycle-spring'`，目标分发包为 IU **261.22158.277**。runIde 到达启动 JVM 阶段，但不能据此确认页面加载或项目打开。临时脚本期望的 config/system/log 路径没有建立，隔离覆盖尚未确认。

原生工具 inventory 发现 `Main` / `com.jetbrains.jbr.java`，但按该标识、名称和 Java 路径绑定均返回 `Invalid app`；按 JBR bundle 路径绑定则解析为 `com.jetbrains.jbr.jdk` 并返回 `Running application not found`。已通过该 Gradle session 的 Ctrl-C 停止本次启动，未操作用户原有 IDEA 项目。

任务 1.2 和 4.3 保留未完成：真实 Apply/OK/Cancel、UI Reset、磁盘保存/重启、双项目隔离、深浅主题与重复关闭日志均没有验收证据。保留原验收范围，不以 fixture 或启动日志替代；自动修复与验证继续独立推进。


### 资源释放 RED → GREEN：T9

在仅修复 Reset 后加入 T9，使用真实 `Configurable.disposeUIResources()`，跟踪本页新建的三个 editor 和 form，另创建一个无关 editor。首次执行 targeted 命令实际 **10 tests / 1 failure / 0 errors / 0 skipped**，失败信息为 `Disposing should dispose the first DialogWrapper`，原始 XML 备份为 `/tmp/doc-view-template-lifecycle-evidence/resource-red.xml`。该断言发生在 tearDown 兜底之前；其余九项状态测试通过。

随后仅修改两个生产类：form 持有 description editor，在 `dispose()` 中释放三份 editor、清空引用并调用 `super.dispose()`；Configurable 清空 form 引用并调用公开、幂等的 `disposeIfNeeded()`。没有加入旧版本或 JCEF 兼容逻辑。

重新运行 `./gradlew test --tests 'com.liuzhihang.doc.view.contract.TemplateSettingsLifecycleTest' --rerun-tasks`，实际 **10 tests / 0 failures / 0 errors / 0 skipped**（时间 `2026-10-17T17:00:42.242Z`）。原始 XML：`/tmp/doc-view-template-lifecycle-evidence/resource-green.xml`。

T9 同时断言三个 editor 已从 factory 移除且 `isDisposed()` 为 true、DialogWrapper 已释放、Configurable 引用清空、同一 Configurable 重开会新建三个 editor、重复/未创建清理安全、无关 editor 仍存活。T5/T6 现在经真实 Configurable 关闭回调，T8 额外验证 Velocity 名称渲染且未应用草稿不影响两种新文档。仍不把这些回调模拟当作真实 Settings 按钮验收。


### 全量回归

`./gradlew test --rerun-tasks` 实际执行 **26 tests / 0 failures / 0 errors / 0 skipped**，6 个 test suites：AnnotationIndex 2、CollectionDependency 3、DocViewServiceDuplicateName 3、OpenApi3OperationSummary 7、SpringMarkdown 1、TemplateSettingsLifecycle 10。已读取 XML 数量；HTML 报告位于 `build/reports/tests/test/index.html`，XML 备份在 `/tmp/doc-view-template-lifecycle-evidence/full-green/`。任务全部重新执行，不以配置缓存复用当作测试结果。

编译仍提示既有 `SpringDocViewServiceImpl` 使用 deprecated API，Gradle 提示既有 Gradle 10 弃用兼容告警；本次未升级平台或依赖，最终 API 风险以本次 Verifier 实际报告单独核对。


### 配置、插件包与目标平台 API

`./gradlew verifyPluginProjectConfiguration buildPlugin verifyPlugin` 完成，BUILD SUCCESSFUL；本次 Verifier 1.410 对 **IU-261.22158.277** 输出 `Compatible`，实际 verdict：`build/reports/pluginVerifier/IU-261.22158.277/plugins/com.liuzhihang.doc-view/1.3.14/verification-verdict.txt`。没有生成 compatibility、internal API、override-only 或 deprecated usage 问题文件；目标只验证当前 2026.1 矩阵，不宣称未来 IDE 全部通过。

Verifier 输出 IDE layout 中部分模块 jar 不存在的告警，依赖报告有 33 条带 optional 标记的未解析项，另有可选 Windows Radler 插件分支下的 Windows/x86_64 模块未解析；这些未使本次目标判定失败，Doc View 的平台、Java、Markdown 直接依赖已解析。保留该环境事实，不把它写成无告警运行。

包为 `build/distributions/Doc View-1.3.14.zip`，386169 bytes，SHA-256 `39b9c6484dd1189790eaf4a546c436e983779004167eb8a870520727749e3856`。descriptor 版本 1.3.14、since-build 261、无 until-build；平台/Java/Markdown dependencies 未变。包内仅有生产 jar，无 JUnit、test-framework 或本次 contract/testsupport classes。三个 searchable-options 任务按仓库已有禁用配置跳过，与测试跳过不同。

### Full access 后的原生重试

修正临时 init script，先创建目录再设置 sandbox properties；同一 runIde 命令重跑后，`/tmp/doc-view-template-lifecycle-ide/spring/log/idea.log` 明确显示：build **IU-261.22158.277**，config/system/log 均在该临时目录，并加载 **Doc View 1.3.14**。仓库构建配置未修改。

Full access 下再次绑定 `com.jetbrains.jbr.java` 仍返回 `Invalid app`。这是原生工具的应用绑定限制，不是文件权限缺失。已停止本次 Gradle/runIde session；没有通过非 UI 路径冒充按钮操作，也没有安装额外 UI 测试依赖。**任务 1.2、4.3 仍未验收**，旧实现原生复现和修复后按钮/真实磁盘重启/双项目/深浅主题场景必须在可控制该窗口的环境补验。

### 首轮范围检查（当时 10/12）

- 本轮生产 diff 仅两个 Java 类；新增一个 Java contract test，更新 Unreleased changelog 和本 change 的规划状态/验证记录。
- 初始已存在的 AGENTS、README、docs、`.agents/` 和规划文档改动保留，没有纳入额外 runtime 实现。
- `TemplateSettings.java`、`.form`、默认模板资源、Velocity 变量、`plugin.xml`、Gradle、wrapper 和既有 golden 无 diff。
- 独立只读审查未发现阻塞代码问题；未把审查替代测试或原生验收。
- `git diff --check`、`openspec validate fix-template-settings-lifecycle --strict` 通过。任务完成 10/12，原生两项保留未勾选。
- 没有提交、推送、发布、sync 或 archive。插件版本未提升。


### 用户启动沙箱后的连接核对

用户确认已启动 IDEA 并授权直接使用 IDEA / Computer Use。最新日志从 `2026-10-18 01:11:44` 开始（临时沙箱 `idea.log:464`），确认 IU-261.22158.277、Doc View 1.3.14 及 Spring fixture，config/system/log 仍隔离在临时目录。

本次 Computer Use inventory 能发现运行中的 `Main` / `com.jetbrains.jbr.java`，但绑定依然返回 `Invalid app`。普通 IntelliJ IDEA 的原生读取成功，其窗口为用户的 `doc-demo`，并非本次沙箱；未在该窗口修改模板或安装插件。没有取得新的按钮/重启验收证据，任务 1.2、4.3 保持未完成。后续优先直接使用 IDE 工具或 Computer Use 完成可操作的验收，不重复请求已授予的控制权限。

## 原生验收完成：2026-10-18 01:20–01:55 +08

### 环境、隔离与样例

经用户授权，正常退出普通 IDEA 后，通过 macOS LaunchServices 启动官方 `/Applications/IntelliJ IDEA.app`，使用独立的 IDEA_PROPERTIES 将 config/system/log/plugins 分别放到 `/tmp/doc-view-template-native-20261018/fixed/` 与 `old/`。实际 build 是 **IU-262.10968.63（2026.2.3）**，不是 Gradle 的 261 测试环境。原生应用可绑定；所有编辑、Apply、OK、Cancel、Revert changes、关闭和重启均经 Computer Use 真实界面完成，XML 只读取证，没有直接写入模板 XML 来替代按钮操作。

261 的 runIde 窗口绑定限制仍成立；缓存分发包的另一启动尝试退出 137，未当作测试成功。使用已安装官方 IDEA 的隔离配置解决了本次原生操作入口，不新增 UI 自动化依赖、不修改 Gradle 或平台基线、不绕过系统签名校验。

- 临时项目：`/tmp/doc-view-template-native-20261018/fixtures/project-a`、`project-b`、`project-old`，使用虚构订单接口。
- 专用验证应用：用户指定的 `/Users/liuzhihang/workspace/doc-demo`。保留已有暂存/未暂存改动，仅通过界面临时应用测试模板，完成后恢复。
- 首轮修复包 SHA-256：`39b9c6484dd1189790eaf4a546c436e983779004167eb8a870520727749e3856`；加载 jar SHA-256：`03d4ced8766e15515b06450031e0ca812ef1a041639c54034740699278a61e03`。
- 对照包只在临时构建副本将两个模板生产类还原为 HEAD 旧实现，不回滚工作区。旧包 SHA-256：`4441e427eb4dd1becca3e99539cdb3e2e7234845eeec513a23440edccf30dafb`；加载 jar：`5f832df39b1a2c8b7031c269bdb766601ef845b9bd0653b28ddb90e0ba678205`。

隔离配置中的临时项目曾显示 `JDK 21 is missing`：本次没有为该 profile 配置项目 SDK，也不声称通过 IDEA 编译或运行了样例应用。此处的原生证据是实际 PSI 文档生成和设置生命周期；插件编译与测试由上列 Gradle 命令单独证明。

### 旧版对照复现（任务 1.2）

1. project-old 在两份内置模板第一行分别添加 `OLD_SPRING_A`、`OLD_DUBBO_A`，真实 Apply/OK。
2. 生成 `OrderController.findSummary` 与 `OrderDirectoryService.findSummary`，预览 Markdown 分别以对应 A 标记开头；路径分别为 `/api/orders/summary` 与 `OrderDirectoryService#findSummary`。
3. XML 确实落盘，mtime 01:51:17，SHA-256 `30ac875fa75e31900a928d7b793e87af6f3fef37e2be5e63bbc5fd34ca18d9b0`。
4. 仅重新打开模板设置，两编辑器仍显示 A，但 **Apply 和 Revert changes 错误启用**；不做任何编辑，点击真实 Cancel。
5. 再生成两个接口，均以默认的 `**接口名称:**` 开头，A 标记消失。Cancel 后 XML 已缺失；01:55:12 正常退出后仍缺失。结合源码 reset 写默认值以及原生生成结果，确认已保存自定义状态被覆盖，默认状态被平台省略存储。

该复现分别取得页面内容、实际生成和磁盘变化证据；没有把“页面仍显示 A”误判为保存状态安全。

### 修复后场景矩阵（任务 4.3）

| 场景 | 实际操作与观察 | 结果 |
| --- | --- | --- |
| 保存和重开 | project-a 两模板 A 经 Apply/OK 保存；重开均为 A，Apply 禁用；直接 Cancel 再开仍为 A | 通过 |
| Reset | 两模板加入 `C_RESET` 草稿，点击平台 `Revert changes`，两者回 A、Apply 禁用 | 通过；此入口调用 Reset，不是恢复默认 |
| 单项 Apply / OK | 仅 Spring 改 B 并 Apply，Dubbo 保留 A；之后仅 Dubbo 改 B 并 OK，Spring 保留 B | 通过 |
| Apply 后继续编辑再取消 | B 应用后两模板加入 `C_CANCEL` 草稿，再 Cancel；重开保留 B/B | 通过 |
| 两种新生成结果 | Spring 输出 `SPRING_B`、GET `/api/orders/summary`；Dubbo 输出 `DUBBO_B`、`OrderDirectoryService#findSummary` | 通过 |
| 真实保存/退出/重启 | B/B 后编辑 `C_RESTART` 并 Cancel，正常保存与退出；01:38:20 关闭、01:38:27 重启；两模板与两种新文档均为 B | 通过 |
| 多项目隔离 | project-b 在独立窗口保存 `PROJECT_B_SPRING` / `PROJECT_B_DUBBO`；project-a Apply `ISOLATION_APPLIED` 再编辑 C/Cancel，不改变 B 项目；随后在 A 撤去临时隔离标记 | 通过 |
| 深浅主题 | Islands Dark / Islands Light 下模板可编辑、切页和取消；Light 下添加 `LIGHT_DRAFT` 并 Cancel，重开仍为 project-b 保存值；恢复 Dark | 通过基础操作；不宣称像素级全 UI 审查 |
| 反复关闭 | 模板页打开/关闭超过五轮，包含 Apply/OK、Cancel；其他 Java 编辑器仍可用 | 通过操作；三个 editor 的精确释放由 T9 断言证明 |

首轮重启及隔离检查后，project-a XML SHA-256 `c0848572052931257d43f1ff8130b746905e4b95d3b83c718328ddf86c359ffa`，两值为 B/B；project-b XML SHA-256 `bcc19c6c1c2e58c1eef5710e49902bcb78ee97d92b5c7fe564e950c05d9e0f5e`，两值为 PROJECT_B 标记。没有未应用的 C_RESTART、LIGHT_DRAFT 或隔离草稿残留。去除首行标记后，两项目模板正文一致。

### doc-demo 验证与恢复

在用户专用测试应用中，通过真实设置页保存 `DOC_DEMO_SPRING`、`DOC_DEMO_DUBBO` 前缀。`UserController.test2()` 生成 GET `/user/test2` 且包含 Spring 标记；`XmlDubboService.queryUserDateAccount` 生成 `XmlDubboService#queryUserDateAccount` 且包含 Dubbo 标记。重开两模板仍为保存值，Apply 禁用。

验证前没有 `.idea/DocViewTemplateSettings.xml`。完成后通过模板编辑器去除两项前缀、OK 并正常退出，XML 仍不存在（恢复到内置默认值后平台省略默认状态），项目内未发现 DOC_DEMO 标记。只读 Git 状态与本次操作前基线一致，既有维护者改动保留。后续优先使用 doc-demo 做真实应用验证；双项目隔离或旧实现破坏性复现仍使用临时副本。

### 原生验收额外发现及范围

1. 首轮包在 `Dispatchers.UI` 回调出现 `SettingsEditor.isModified() failed for Markdown Template` / `LockAccessDisallowed`，栈为 `DocumentImpl.getText → TemplateSettingForm.isModified:136 → TemplateConfigurable.isModified:54`。这是当前状态修复的必要补充，按下节修复并复验。
2. 正常退出日志的 ObjectTree/Disposer editor 泄漏注册栈来自 **`PreviewForm.markdownSourcePanel:396 → createViewer()`**。模板页 T9 对其自身三个 editor 的释放断言通过；原生没有 TemplateSettingForm 泄漏栈。两者不是同一资源所有权，不能将本 change 描述为解决所有 Doc View editor 泄漏。
3. doc-demo 第二次打开某个 Dubbo 预览曾停在 `Rendering Doc View…`。该次没有完成输出，不计为通过；没有日志证据确认根因。第一次 Spring/Dubbo 生成及其他新生成场景已分别确认。
4. 切到 ShowDoc 空配置页会启用全局 Apply，返回模板页时模板自己的 Revert 仍禁用。这属于已有集成配置状态审计范围，不把全局 Apply 变化归因于模板草稿。

第 2、3 项已列为路线图中的独立预览生命周期候选；本轮没有修改 PreviewForm、渲染或其他设置页。不宣称 IDE 日志完全无 WARN/SEVERE，也不将本次验收替代完整插件发布检查。

## 最终包复验：isModified 无锁快照（任务 4.5）

最小修改只有 `TemplateSettingForm.isModified()` 两处 `getText()` 改为公开的 `getImmutableCharSequence().toString()`，仍使用 `String.equals` 精确比较。没有增加 ReadAction、版本条件、兼容层或新依赖。设计和任务已同步。

重跑并读取报告：

- `./gradlew test --tests 'com.liuzhihang.doc.view.contract.TemplateSettingsLifecycleTest' --rerun-tasks`：10 tests，0 failures/errors/skipped。
- `./gradlew test --rerun-tasks`：26 tests，0 failures/errors/skipped，6 suites。
- `./gradlew verifyPluginProjectConfiguration buildPlugin verifyPlugin`：成功，IU-261.22158.277 verdict 为 `Compatible`。
- 最终 zip：386207 bytes，SHA-256 `514ced7800c6c09273550cae51e7aa68d368097dbfaf10e861a4c3214f78e624`；安装到 fixed 隔离 profile 的 jar SHA-256 `8c117f146548e5b48815511897111e1177f01cfb6204908febcf31dfe781d8e8`。版本仍 1.3.14，未发布。

原生最终包会话 **01:55:27.169–01:56:53.695**（IU-262.10968.63）中，重新打开 B/B 模板为未修改状态；编辑两项后 Revert 恢复 B/B；再为两项加 `FINAL_APPLIED` 并 Apply，切换 ShowDoc/模板分类，随后加入 `FINAL_CANCEL_DRAFT` 并 Cancel。重开两项均保留 FINAL_APPLIED，Apply 禁用。Spring 新文档包含 FINAL_APPLIED 与 SPRING_B。

完整会话内 `SettingsEditor.isModified` 失败、`LockAccessDisallowed`、`TemplateSettingForm` 日志匹配项均为 **0**。退出仍有前述 PreviewForm viewer 泄漏，未掩盖或算作模板页问题已重现。

真实退出后 XML SHA-256 为 `eef89f4e1f103d05503c862747930d68631ccbe48e37b003d414050ee516838e`，两项开头分别为 `FINAL_APPLIED\nSPRING_B` 和 `FINAL_APPLIED\nDUBBO_B`，没有 FINAL_RESET_DRAFT 或 FINAL_CANCEL_DRAFT。**01:57:03.872** 再次启动同一隔离配置，两编辑器读取同样值且 Apply 禁用；新生成 Dubbo 文档含 FINAL_APPLIED 与 DUBBO_B，路径为 `OrderDirectoryService#findSummary`。

最后一轮会话于 **01:57:58.715** 正常退出；该完整会话的 `isModified() failed`、SettingsEditor WARN、`LockAccessDisallowed`、`TemplateSettingForm` 匹配项仍均为 0。

随后在不带 IDEA_PROPERTIES 的普通配置中重新打开 `/Users/liuzhihang/workspace/doc-view`；Computer Use 确认窗口为 `doc-view – DubboPsiUtils.java [doc-view.main]`。01:58:05 的普通 IDEA 日志确认配置/缓存/插件/日志均回到用户 Library 下的标准 IntelliJIdea2026.2 路径，未使用临时隔离路径。普通 profile 未安装本次测试包。用户原有项目文件和未提交改动保留。

本次补验完成任务 1.2、4.3、4.5。自动证据运行于 261，原生证据运行于 262；261 原生窗口本身仍未完成手工验收。没有运行真实上传、完整产品烟测、Marketplace 安装或发布，未 sync/archive/提交/推送。

最终 `openspec validate fix-template-settings-lifecycle --strict` 和 `git diff --check` 通过；`openspec instructions apply --change fix-template-settings-lifecycle --json` 为 **13/13、remaining 0、all_done**。生产 diff 仍仅两个模板 Java 类；默认资源、存储类、`.form`、Gradle、plugin.xml 和既有 golden 未变化。路线图、设计研究/设置评估及模板教程同步当前未发布实现，避免继续把已修复行为写成现状。
