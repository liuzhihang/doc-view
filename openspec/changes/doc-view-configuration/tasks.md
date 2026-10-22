# Tasks

## 1. 设置层级与规则页面

- [x] 1.1 将项目级配置整理为 Doc View 常规行为、生成规则、模板与输出、平台集成页面及三个集成子页；保留现有 Configurable ID 和项目作用域，并用 `parentId` 建立层级；新增导航/作用域测试并运行 `./gradlew test --tests 'com.liuzhihang.doc.view.contract.ConfigurationExperienceTest' --rerun-tasks`。
- [x] 1.2 从设置状态、表单和缓存哈希中移除无效的 `hideLeft`、`excludeParamTypes`，保留实际过滤用的 `excludeParameterType`；增加状态与过滤回归用例并运行 `./gradlew test --tests 'com.liuzhihang.doc.view.contract.ConfigurationExperienceTest' --rerun-tasks`。
- [x] 1.3 将 12 个生成规则控件按来源分类并提供可选/固定规则及优先级说明；修复行标记与普通接口识别/扫描选项相互独立的编辑、Apply、Reset 行为并保持各自行为范围；准确说明参数分表嵌套层级前缀；增加配置与输出回归用例并由全量测试覆盖。
- [x] 1.4 将较长的生成规则说明改为左对齐、可换行的原生文本布局，不改变规则内容或来源顺序；`GenerationRulesLayoutTest` 2 项通过，IU 262 原生窗口 2268×1694 下长文显示为左对齐两行且未裁切（`/tmp/doc-view-reliability-20261018/native-generation-rules-fixed.png`）。该截图只证明此窗口尺寸，不外推为窄窗口验收。

## 2. 模板编辑与预览

- [x] 2.1 保留原生 Spring/Dubbo 编辑器，加入可搜索变量说明和光标插入、仅修改草稿的恢复默认；增加变量有效性及 Apply/Reset/Cancel 用例并由全量测试覆盖。
- [x] 2.2 用固定虚构 DTO 样例异步渲染 Spring/Dubbo Markdown 预览，展示渲染错误并丢弃过期结果；增加后台执行、最新结果和错误场景测试，并由全量测试覆盖。
- [x] 2.3a 用平台 `OnePixelSplitter` 呈现按需展开的变量侧栏；搜索提示/无障碍名称、变量多行说明、初始/生成中/成功/过期与 Reset 状态由 `TemplateConfigurationExperienceTest` 覆盖，IU 262 新包原生确认侧栏搜索、插入和 Spring/Dubbo 预览流程。
- [x] 2.3b 补充模板为空和渲染失败时，预览结果页可见状态文案的表单级回归用例；真实表单测试先验证成功结果，再验证空模板的空状态、未闭合 Velocity 指令的错误状态及两者清除旧 Markdown。Round 30 `TemplateConfigurationExperienceTest` 8/8 通过；仅新增测试，无运行时变化。
- [x] 2.3c 根据 IU 262 浅色主题反馈，详情滚动区首选高度设为 120 logical、文本区显示六行，使长插入表达式、中文标签和用途/适用性在常规窗口高度下均可读，短窗口可纵向滚动；底部显示“插入到当前模板光标处”，tooltip 保留完整说明。Round 34 全量门禁通过；IU 262 浅色原生复验中 `responseParamDataList` 的属性名/中文标签分行，详情完整表达式、标签和用途均可读且无需滚动，提示可见；Round 33 自动化辅助功能证据覆盖完整表达式插入及 Revert 后 Apply 禁用。详见 `verification.md`。
- [x] 2.4 将模板与输出页改为“模板编辑 / 预览结果 / 输出选项”三个主页签，默认打开模板编辑；保留 Spring/Dubbo 子页签，变量目录默认收起并通过按钮展开/收起，帮助与恢复默认保留在简短页头，输出选项改为独立纵向表单；Round 29 新包的 IU 262 原生验收确认三工作区与主要导航流程。
- [x] 2.5 为三主页签默认选择、变量目录显示/隐藏与当前编辑器插入、预览请求时切换及回调不抢占后续选择、输出设置 Apply/Reset/Cancel 增加 UI 回归用例；Round 28 `TemplateConfigurationExperienceTest` 7/7、`TemplateSettingsLifecycleTest` 10/10 通过，并在自动用例中核对侧栏编辑器/变量区最小宽度。

## 3. 搜索、文档与集成验收

- [x] 3.1a 为设置页、控件和帮助增加中英文搜索词并注册公开平台搜索贡献，保持 `buildSearchableOptions = false`；在目标 IDEA 的 Settings 中搜索“模板”和“Generation Rules”，分别能定位 Template & Output 与 Generation Rules 页面；另手工核对 `nested prefix` 和模板变量关键词定位到 Template & Output，后者可见正确 breadcrumb 且变量按钮高亮。
- [x] 3.1b 在 IU262 隔离 doc-demo Settings 中逐词核对当前贡献的全部60个中英文关键词，对应常规/生成规则/模板/平台页面 breadcrumb 均正确；模板变量词进入模板页且控件可见。未为索引生成额外启动IDE。2026-10-20 原始逐项 AX 与汇总见本轮 verification。
- [x] 3.2 更新用户设置指南与 `CHANGELOG.md`，说明页面入口、生成规则帮助和模板预览；运行 `git diff --check` 并核对指南与实现行为一致。
- [x] 3.3 在相关设置页接入指南入口，平台页打开集成指南，模板页打开模板指南，常规/生成页打开设置指南；后续欢迎页 change 已将这些入口统一为默认浏览器打开 GitHub；增加帮助打开不改变草稿/保存状态的用例并运行 `./gradlew test --tests 'com.liuzhihang.doc.view.contract.ConfigurationExperienceTest' --rerun-tasks`。
- [x] 3.4a 在 IU 262 Round 29 的 doc-demo 项目会话中原生验收常规页开关生命周期：会话初始两个开关均开启；将 `lineMarker=false`、保持 `includeNormalInterface=true` 并应用后 Apply 禁用，取消并重开仍观察到 false/true；将普通接口草稿设为 false 后 Revert 回 true 且行标记保持 false；最后仅将行标记设为 true 并应用，恢复两者均开启。最终 XML 只读读回中 `includeNormalInterface=true`，`lineMarker` 未显式序列化；根据 `Settings.java` 的字段默认值其值为 true，不能把字段缺省解读为 false。证据见 `verification.md`。这记录的是该项目会话状态，不代表所有新项目的默认值。
- [x] 3.4b 普通接口扫描已在af61原生验证五类→关闭并Apply/刷新后三类→恢复开启并Apply/刷新后五类。2026-10-22 续验以各页代表字段覆盖常规、生成规则、模板/输出、ShowDoc、YApi、语雀的 Apply/Revert/Cancel 后重开，并用同进程 A/B 两项目及重启后的逐页读回验证五份项目设置相互隔离；深浅主题启动后的保存值保持。平台集成总览没有独立持久化字段，核对其导航不产生脏状态。凭据输入/清除不在本项范围，详见 credential change；即时主题切换另列 3.4c。恢复结果与包身份见 verification 最新记录。
- [x] 3.4c 在 IDEA 运行期间即时切换主题后的设置状态检查由维护者人工完成；2026-10-22 用户明确确认“切主题验收完毕”。此前工具未能完成即时切换的记录保留，不作为通过证据；本次人工确认未逐项列出语言、Compact、窗口尺寸或包身份，不外推额外矩阵。
- [x] 3.5 运行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`；Round 34 执行 38 个测试套件、187 tests、0 failure/error/skip，build 成功，Verifier 为 IU 261 Compatible。Round 34 包 SHA-256 见 `verification.md`。
- [x] 3.6 在 IU 262 隔离 profile 中检查 Round 29 新包：默认编辑页/变量侧栏收起，展开后搜索并插入变量，Spring 与 Dubbo 预览进入结果页，输出选项纵向排布，Revert 恢复默认草稿并禁用 Apply，Cancel 后重开仍为编辑页；详细截图与未覆盖项见 `verification.md` 最新原生记录。

## 4. 原生验收反馈：生成规则说明

- [x] 4.1 以现有布局回归复现常驻长说明与明显四边框，改为组级原生说明图标、短灰字和浅分隔线，验证规则值及草稿语义不变。
- [x] 4.2a 执行布局/配置专项8项及完整自动门禁208项；在 doc-demo 核对深浅主题的常规宽度布局、说明图标及可访问描述、Revert/Apply/Cancel，详见本轮 verification。
- [x] 4.2b 在最新中英/指南包e0b6的900×700逻辑窗口核对英语/简中 × 深浅主题的生成规则布局；中文实际纵向滚动可达底部。Compact保持开启，完整设置页/多项目矩阵仍见3.4b。
- [x] 4.2c 真实悬浮说明气泡的阅读检查由维护者人工验收完成；2026-10-21 用户确认“人工验收气泡完毕”。证据来源为人工反馈，不是工具截图或AX描述；具体语言/主题组合未逐项列明。
- [x] 4.3 根据实际气泡截图将优先级改为编号逐行、回退与必填规则分段；中英资源、可访问描述和气泡HTML结构回归通过。
- [x] 4.3b 气泡分行修复由维护者人工复验确认完成（2026-10-21）；不将旧长句截图或此前工具未触发气泡的画面作为通过证据。
- [x] 4.4 依据追加反馈将各设置指南统一为原生问号，模板恢复默认操作移入编辑区；由localize-plugin-ui联合门禁和原生复验。

## 5. 窄窗口中的凭据清除标签

- [x] 5.1 在调整表单前新增 `IntegrationSettingsLayoutTest`，以 900×700 logical Settings 可用内容宽度约 650 logical 检查 ShowDoc、YApi、YuQue 的清除选项完整可见、对应密码字段可用且清除行顺序正确；专项 RED 为 1 test / 1 failure，首先命中 `ShowDoc API key clear action must be on a separate row below its password field`，证据：`/tmp/doc-view-native-20261022-settings/layout-red.log` 与 `build/test-results/test/TEST-com.liuzhihang.doc.view.contract.IntegrationSettingsLayoutTest.xml`。此测试只布局表单，不读取或修改凭据。
- [x] 5.2 将三平台表单的清除选项移到对应密码框下方并使用剩余列宽；不改文案、凭据状态说明或 Keep/Clear/Replace、Apply/Reset/Cancel 语义。专项 GREEN 1/1，完整门禁49 suites/229 tests零失败、错误、跳过，IU261 Verifier Compatible；df21候选在IU262原生900×700窗口完成英语深色/简中浅色三平台视觉复验，标签均完整可见。凭据存储/删除/替换原生验收仍属独立 credential change，本轮未操作凭据。
