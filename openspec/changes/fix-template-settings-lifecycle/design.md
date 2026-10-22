# Design

## Context

动机和范围见 [proposal.md](proposal.md)。本设计基于 `develop` / `00f1f94`，Java 21、IU 2026.1（261）；版本以仓库配置为准。用户已于 2026-10-18 授权 apply，以下描述保留修复前证据及验证设计，实际执行结果见 [verification.md](verification.md)。

修复前的调用关系（保留作为复现依据）：

```text
TemplateConfigurable.createComponent()
  -> TemplateSettingForm
     -> 读取 TemplateSettings 创建 Spring/Dubbo editor documents
     -> 创建一个只读 description editor
TemplateConfigurable.reset()
  -> TemplateSettingForm.reset()
     -> 把两个 TemplateSettings 字段写成默认模板（错误方向）
```

修复前的关键证据：

- [TemplateConfigurable.java](../../../src/main/java/com/liuzhihang/doc/view/config/TemplateConfigurable.java) 的 `createComponent()`、`reset()`、`apply()` 委托同一个 form，没有 `disposeUIResources()`。
- [TemplateSettingForm.java](../../../src/main/java/com/liuzhihang/doc/view/ui/TemplateSettingForm.java) 第 74–126 行创建三个编辑器，第 130–150 行比较并提交模板，第 154–158 行在 Reset 中改写保存状态；description editor 目前是局部变量。
- [TemplateSettings.java](../../../src/main/java/com/liuzhihang/doc/view/config/TemplateSettings.java) 保持项目 service、`TemplateSettingsComponent` 与 `DocViewTemplateSettings.xml`，两项字符串初始值已来自 bundle。
- [JetBrains Settings Guide](https://plugins.jetbrains.com/docs/intellij/settings-guide.html) 说明平台会在 `createComponent()` 后调用 `reset()`，关闭设置时调用 `disposeUIResources()`。
- [SpringMarkdownContractTest](../../../src/test/java/com/liuzhihang/doc/view/contract/SpringMarkdownContractTest.java) 已使用真实 `LightJavaCodeInsightFixtureTestCase`；Gradle 配置了 JUnit、Platform/Java test framework 和零测试失败门禁。没有现成模板设置测试或 UI Robot 工具链。

规划时确定的源码事实是 Reset 调用了保存状态 setter，设置页初始化可能错误改写内存状态；当时磁盘保存时点、真实 Cancel 以及重启表现尚未实测。实施后已补齐自动与原生对照证据，详见 verification；此处不把修复前描述当作当前实现。

## Goals / Non-Goals

**Goals:**

- 明确状态流：已保存模板 → UI 草稿 → 用户应用 → 新的已保存模板。
- 平台 Reset 只回读最近保存值；未应用修改不进入 `TemplateSettings`。
- 正确处理 Apply 后继续编辑再 Cancel；清理仅属于本页的三个 editor。
- 用可重复的失败/通过测试及 IDE 烟测证明状态、持久化和生命周期行为。

**Non-Goals:**

- 不把本页从 DialogWrapper 改造为新 UI 架构，不调整 `.form`、设置树或布局。
- 不在此修复中新增“恢复默认”按钮；后续配置 change 再设计显式的草稿恢复动作。平台 Reset 不承担该含义。
- 不增加模板语法校验、自动格式化、null/损坏 XML 自动修复或配置并发冲突合并策略。
- 不修改 PSI、DTO、Velocity、默认模板、上传/导出或缓存体系；已打开预览的自动刷新不在此 change，验证采用新生成文档。
- 不宣称恢复已经被旧 bug 覆盖且没有备份的模板内容。

## Decisions

### 1. 保留现有状态模型，修正 Reset 的数据方向

两份 Editor Document 就是草稿，`TemplateSettings` 就是最近保存值，无需引入第三份 snapshot、通用 settings binder 或 Cancel rollback。

| 动作 | 编辑器草稿 | 保存状态 | `isModified` |
| --- | --- | --- | --- |
| 创建页面后 Reset | 读入已保存 A | A 不变 | false |
| 编辑为 B | B | A 不变 | true |
| Reset | 恢复 A | A 不变 | false |
| Apply | B | 写入 B | false |
| Apply B 后编辑 C | C | B 不变 | true |
| Cancel/不保存关闭 | 丢弃当前草稿 | 最近 Apply 的 B 保留 | 下次打开 false |
| OK | 提交当前草稿后关闭 | 当前草稿成为保存值 | 下次打开 false |

`TemplateSettingForm.reset()` 在 UI/EDT 与平台允许的文档写入上下文中，将两项 getter 的结果写到各自 Document；不调用模板 setter、不读取 bundle 的默认模板覆盖状态。保持文本原样，包括空字符串、中文、空行、前后空格和 Velocity 表达式；不额外 trim 或 normalize。

`apply()` 保持两项独立写回；`isModified()` 继续比较当前草稿与最近保存值。原生验收在 IU-262.10968.63 发现 `Document.getText()` 在 `Dispatchers.UI` 回调中申请读锁导致 `SettingsEditor.isModified() failed`。比较改用平台公开、无需读写锁的 `getImmutableCharSequence().toString()`，仍使用 `String.equals` 保持逐字符语义；不包裹 ReadAction、不增加版本分支。首次默认值继续由 `TemplateSettings` 初始化，不在设置生命周期中重建默认值。

放弃的方案：Cancel 时恢复“打开前快照”会错误撤销本次已经 Apply 的 B；给 Reset 加首次调用标记则仍混淆“加载/放弃草稿”和“恢复默认”。

### 2. 补齐本页资源所有权

Configurable 的 `disposeUIResources()` 委托 form 的清理入口，并清空其引用。form 跟踪 Spring、Dubbo 和 description 三个自行创建的 editor，通过目标平台公开 `EditorFactory.releaseEditor` 路径释放；同时结束其 DialogWrapper 自身生命周期。

清理需要空值/重复调用保护，不保存草稿，不枚举并销毁项目全部 editor。保留现有表单结构，使用窄清理方法或现有 disposable 注册点实现；不为了这个问题重构其他配置页。

把该清理纳入同一 change 的理由是：本修复的主要验收动作就是重复打开和取消设置，且真实编辑器 fixture 必须能清理自己创建的资源。仅在测试结束时清掉遗留 editor 会掩盖生产生命周期缺失。

### 3. 沿用当前存储格式，不修改输出合同

实现沿用当前版本的 Configurable ID、项目 service、state 名称、XML 文件名、Spring/Dubbo 字段与默认值；这些是本 change 的实现边界，不构成跨版本兼容承诺。序列化只经现有平台持久化机制；本修复不自行写配置 XML，也不新增旧格式迁移、降级读取或兼容分支。

调用 `DocViewData.markdownText` 生成新文档应读取最近应用模板。已有默认 Spring golden 继续精确比较；自定义模板用两个不同框架的标记验证选择，避免扩展为整套解析器改造。

### 4. 实施文件边界

| 文件 | 计划修改 |
| --- | --- |
| `src/main/java/com/liuzhihang/doc/view/ui/TemplateSettingForm.java` | Reset 回读两份文档；isModified 无锁快照比较；跟踪并释放本页三个 editor |
| `src/main/java/com/liuzhihang/doc/view/config/TemplateConfigurable.java` | 接入页面资源释放；重复清理安全 |
| `src/test/java/com/liuzhihang/doc/view/contract/TemplateSettingsLifecycleTest.java` | 新增真实设置页生命周期回归；测试名在本计划中固定，命令可直接筛选 |
| `src/test/testData/contracts/templates/` | 仅在内联虚构样例不清楚时添加必要测试数据，不放真实用户模板 |
| `CHANGELOG.md` | 实施完成后补 Fixed 说明，不提前写“已修复” |
| 当前 change 下的 `verification.md` | 实施时记录 RED/GREEN、测试数量、IDE build、实际结果与未覆盖项 |

`TemplateSettings.java`、`.form`、bundle、`plugin.xml`、Gradle、生产解析/渲染文件预期不变。若复现要求突破此边界，先更新方案说明具体原因，不顺手扩大实现。

## Reproduction and Verification

### 自动复现：先在旧实现取得正确的失败证据

沿用 `LightJavaCodeInsightFixtureTestCase` 和现有测试依赖，在 fixture project 的 `TemplateSettings` 保存两份非默认且互不相同的文本，例如 Spring 以 `SPRING_A` 开头、Dubbo 以 `DUBBO_A` 开头，均带可渲染的 `${DocView.name}`。

1. 保存初始项目设置以便 tearDown 恢复，记录测试前存活 editor 集合。
2. 按真实平台顺序创建 `TemplateConfigurable`、调用 `createComponent()`、`reset()`；UI 操作按框架要求在 EDT 执行。
3. 断言两项保存状态仍是 A，两个 editor 也为 A，`isModified()` 为 false。旧实现预期会在“保存值被换成默认模板”处失败；没有测试发现、GUI 初始化失败或依赖下载失败不能当作该 RED。
4. 新增编辑/Apply/Reset/Cancel 等矩阵。使用真实 editor documents，不用手工 state holder 或 mock form 代替。首个回归仅用公开 Configurable/settings API 即可取证；编辑器操作优先走组件/editor 公共路径，必要时使用测试侧窄反射辅助，避免新增生产公共 API 或通用适配层。
5. 断言生产清理前后的本页 editor 存活状态；在断言之后用测试 finally 兜底清理本测试创建的资源并恢复设置，避免旧实现的失败污染其他用例。兜底不能替代生产清理的断言。

建议测试矩阵：

| ID | 操作 | 核心断言 |
| --- | --- | --- |
| T1 | 自定义 A → create/reset → 无编辑关闭 → 重开 | A 未被覆盖，初始/重开均未修改 |
| T2 | A → 草稿 B → Reset → 再 Reset | 草稿回 A，保存状态始终 A，无额外修改 |
| T3 | 仅改 Spring 并 Apply；独立用例仅改 Dubbo 并 Apply | 对应一项更新，另一项逐字符不变，Apply 后未修改 |
| T4 | 同时编辑两项并 Apply，再无修改 Apply | 两项精确保留，重复 Apply 幂等；包含中文/空白/空字符串样例 |
| T5 | A → 草稿 B → 不调用 Apply 而 dispose → 重开 | 保存值和重开为 A；这是模拟 Cancel 回调序列，不等同真实按钮验收 |
| T6 | A → Apply B → 草稿 C → dispose → 重开 | B 保留，C 丢弃，不能回滚 A |
| T7 | 当前版本格式的保存状态经现有 XML 序列化/`loadState` 往返 | 已应用值保留，草稿不进入 state；不覆盖跨版本迁移或降级读取，也不证明真实磁盘保存时机 |
| T8 | 新项目默认模板及自定义模板生成 | 默认 Spring golden 不变；新生成的 Spring/Dubbo 文档选用对应最近 Apply 的模板 |
| T9 | 连续创建/清理、重复清理、未创建即清理 | 3 个本页 editor 均释放，其他 editor 保留，无重复释放异常 |

T8 复用现有 Java fixture 与注解桩；Dubbo 仅补一个最小虚构接口确认模板选择，不扩展其解析支持范围。跨真实项目隔离与平台 OK/Cancel 由下面手工矩阵覆盖，不把轻量 fixture 当成多项目/持久化实测。

### 手工复现与回归：仅在获准 apply 后运行

优先使用 `./gradlew runIde` 启动开发沙箱，在独立临时项目准备一个虚构 Spring Controller 和一个 Dubbo 接口。若原生工具无法绑定 JBR 窗口，可把同一构建包安装到官方 IDEA 的隔离 config/system/plugins/log 目录，通过系统 LaunchServices 启动并记录实际 build 和包哈希；这不等于原目标 build 的原生验收。不要用用户的正式项目或私有模板复现。用户已明确 `/Users/liuzhihang/workspace/doc-demo` 为专用验证工程，后续优先用它补充真实项目 smoke；先记录既有改动与模板状态，验证后恢复临时模板，不回滚源码。记录实际 IDE build、插件版本和操作截图/结果。

1. 打开当前 `Other Settings → Doc View → Markdown Template`。给两份内置模板各加唯一标记 `SPRING_A` / `DUBBO_A`，Apply/OK；重新生成两种文档，确认标记已生效，作为前置基线。
2. 再打开模板页、不编辑，观察文本和 Apply 按钮状态，点击 Cancel。重新生成文档，记录 A 是否丢失；旧实现可能仍在 editor 中显示 A，但保存状态已成为默认，不能只看页面文本判断。
3. 修复后重复第 1–2 步：A 应保留且无未保存修改。编辑两项后点击平台 Reset（若当前 UI 未显示该入口，记录并由 T2 直接覆盖回调），应恢复最近保存值而非默认模板。
4. 仅把 Spring 改为 `SPRING_B` 并 Apply，随后把 Spring/Dubbo 再改成 C 并 Cancel。重开和新生成结果应为 `SPRING_B` / `DUBBO_A`。
5. 仅把 Dubbo 改为 `DUBBO_B` 并 OK，重开与新生成结果应为两份 B。重复打开/关闭，观察日志中是否有 editor/disposable 泄漏、异常或已释放对象访问。
6. 在仍使用 B 时编辑 C 并 Cancel，正常保存项目、退出沙箱 IDE，再启动并打开同一项目。检查实际项目配置中的 `DocViewTemplateSettings.xml` 与页面/生成结果仍为 B；仅截取虚构标记证据，不复制其他设置。
7. 在第二个临时项目保持不同模板，检查第一项目 Apply/Cancel/重开不修改第二项目。覆盖深/浅主题下页面正常加载、输入与关闭；不将此小修复扩大为整个设置页可访问性审计。

### 计划命令与证据层次

命令在仓库根执行；以下为实施阶段验证顺序，实际执行结果单独记录。

| 时点 | 命令/动作 | 证明内容 |
| --- | --- | --- |
| 改生产实现前 | `./gradlew test --tests 'com.liuzhihang.doc.view.contract.TemplateSettingsLifecycleTest' --rerun-tasks` | 至少 T1 在错误的模板覆盖断言处失败 |
| 最小修复后 | 同一 targeted 命令 | 新增矩阵实际执行并通过，记录数量/跳过项 |
| 回归 | `./gradlew test --rerun-tasks` | 现有契约与新增测试共同通过，默认 golden 未被重写 |
| API/分发包 | `./gradlew verifyPluginProjectConfiguration buildPlugin verifyPlugin` | 配置、打包、目标矩阵公开 API 兼容；检查报告而非仅退出码 |
| 原生体验 | `./gradlew runIde` + 上述 7 步 | 实际 OK/Cancel、持久化重启、双项目与页面重复关闭 |

测试报告读取 `build/test-results/test/` 与 `build/reports/tests/test/`；核对测试被发现、实际执行、非零且无未解释的跳过。构建成功不等于 UI、持久化或 Plugin Verifier 通过。

## Risks / Trade-offs

- [真实编辑器 fixture 尚未运行] → 首先确认 UI 初始化条件；基础设施失败单独记录，不用 fake editor 换掉核心回归，也不未经授权安装 UI Robot 等依赖。
- [DialogWrapper 与 editor 各有生命周期] → 只释放本页所有权下资源，验证三份 editor、重复清理与框架 disposable；保持原表单结构。
- [自动序列化不等于磁盘持久化] → T7 只证明当前版本格式的 state 往返，真实保存/重启单独记录；未完成时保留验收缺口。
- [关闭时自行 rollback 会撤销已 Apply 的值] → 不引入 Cancel 快照回滚，按 T6 证明最近 Apply 保留。
- [回退版本] → 发布前保留当前模板配置备份。用户若回退到旧插件版本，旧版缺陷仍在；本 change 不承诺旧版本能够读取新版本保存的配置，不自动清空、重置或迁移用户数据。

## Migration Plan

用户已确认 apply，依次取得 RED、最小修复、GREEN、全量/打包/API 与 IDE 验证，记录实际结果并更新 changelog；不自动提交、发布、sync 或 archive。

没有数据迁移。发布前保留当前版本与模板配置备份，供维护者或用户按需恢复。用户可自行回退插件版本，但旧版本中的模板设置缺陷仍会存在，且旧版本读取新版本配置的能力不作保证。本 change 不新增配置清空、重置或降级适配。
