# 2026-10-20 验证与整理

> 历史验证记录。2026-10-22 后续已获维护者授权升级、提交、PR 合并和发布 1.4.0；最终包与实际发布状态以 [1.4.0 发布记录](release-1.4.0.md) 为准。下文的“当前”“未授权”“未发布”均指各段记录发生时，不覆盖新授权，也不表示未完成验收已通过。

本轮使用专用 `doc-demo` 和隔离 IDEA profile，先验证当前候选包，再根据原生反馈调整生成规则说明。当前记录只覆盖实际执行项，不代表路线图全部完成；未提交、推送、发布、sync 或 archive。

范围更新（2026-10-22）：维护者要求“真实上传忽略”，三平台真实上传验收现为本轮未执行的非阻断排除项。下方历史记录中的“真实上传待验”不再作为当前发布阻断；其它原生缺口保持。详见文末最新范围说明。

## 最新候选与当前缺口（2026-10-22 更新）

本节覆盖下方各轮历史“最新包”称谓。caaf ZIP（`caaf605f2a3599a65e6cc0a69b2a6900328a86442d3315a0b4e6bc322a833bc3`）及其 234 项门禁是历史候选证据，不再是当前包。当前 candidate 235 ZIP 为 `2221a77261d1de30f09c202446cd4565080b1aeff733585a9c44587e496f448d`，生产 JAR 为 `62b06ff49d5f18c89f104983bd7dce5e028282134a30fe23c6689bbd481a9f78`，版本仍为 1.3.14；未确定新的正式发布版本。

- candidate 235 汇总 `/tmp/doc-view-native-20261022-all/candidate235-gate-summary.json` 显示完整18个 Gradle tasks 实际执行、49 suites / 235 tests，0 failures/errors/skipped；IU261.22158.277 Plugin Verifier 为 `Compatible`。ZIP 只有生产 JAR，没有 JUnit、测试夹具或 MCP PoC 扩展。Gradle 弃用提示及测试 CEF 进程日志仍按实际记录，不写成“零警告/零运行日志错误”。
- 原生使用官方签名 IU261.22158.277、IU262.10968.63 的独立 profile；`doc-demo` 是专用主验收项目，跨项目/多模块使用 `/tmp` 下的虚构源码夹具。IU262 多模块索引/页面/源码导航证据使用历史 caaf 包，不属于 candidate 235 的完整原生矩阵；不同包的场景不冒用为同一包完整验收。
- 本轮修复 Schema 描述读取时未解码 Java 字符串、显式空描述回退旧注释，以及整批确认遇到不支持的 Schema 时可能先写部分字段的问题；新增失败回归后通过。关闭项目后导出仍创建空目录的问题已修复并用新包原生复验；暂存后关闭的清理回归也已通过。

| 原生范围 | 最新实际证据 |
| --- | --- |
| 项目导出与覆盖 | IU261 空项目、目录/窗口合并导出的覆盖取消与确认、扫描取消、关闭项目后不创建输出目录均已验。目录动作经 Find Action 执行，不宣称点击了右键菜单。 |
| 多模块阅读 | IU261/IU262 的 billing-api、catalog-api 索引分组、4份接口页、两个重载及两模块 Source 目标均已实际打开；IU262 使用历史 caaf 包，不外推为 candidate 235 的矩阵结果。 |
| 目录扫描取消 | IU261 真实停止后台任务后进入 onCancel，旧目录保留、不发布部分树；受控暂停不用于推算自然耗时。 |
| 方法 Scratch | IU261 Enter/双击打开、停止生成，以及关闭项目后丢弃迟到结果已验；没有生成取消中的部分文件。 |
| 缓存清理 | 工具栏真实点击及目录动作有/无选中执行已验，手写 Scratch 和普通文件保持；目录动作的实际右键菜单入口仍未补齐。 |
| Preview/JCEF | 235 包已补 Spring 8 项、Dubbo 2 项整类源码预览、空结果、模板渲染失败及恢复；取消/失败预览关闭时，JDI 观察 panel/editor 引用清空。HTML 可见终态、快速切换、完整导出及资源基线矩阵仍不完整。 |
| 凭据状态 | caaf 包曾观察到 A/B 共用 serviceName 后 A 回读未配置；根因已确认是 KeePass/KeyChain 保存时按 serviceName 与空 account 查找并更新条目 account。修复为生成键按平台和项目位置隔离，真实 IU261 `InMemoryCredentialStore` 回归取得修复前 RED、修复后 GREEN。candidate 235 已在 IU261 原生验证 A/B 保存、同包重启回读，以及清除 A 后 B 凭据状态仍保留；测试凭据已通过 UI 清理。OS 授权/拒绝和后端存储失败后的重试仍未验证。 |

仍未完成的 change 为 `fix-preview-lifecycle`、`doc-generation-performance`、`openapi3-schema-required-mode`、`clear-cache-safety`、`integration-credential-storage` 和 `mcp-java-feasibility`，具体以各自 tasks/verification 为准。参数编辑写回受弹窗输入限制；MCP 临时扩展仅构建检查，尚未安装或开启服务。真实上传按维护者要求排除；完整键盘专项仍按此前决定暂缓；公开 GitHub 指南正文需获准推送后复核。当前不能称为全部验收完成或正式发布就绪。

本轮证据集中于 `/tmp/doc-view-native-20261022-all/`；详细结论已进入各 change 的 verification。测试 IDEA 已正常退出，普通 `doc-view – ai-context-assessment.md [doc-view]` 窗口已实际恢复；主仓库与 doc-demo 的 HEAD/index 均不变，doc-demo 的35份 tracked/non-ignored 文件及两份既有 DocView XML 与基线一致。本轮相对起始 manifest 的31处修改均为当前修复、测试、规格及验收文档，没有新增或删除路径；既有混合工作树不作回滚。范围证据为 `final-restoration-scope.json`，恢复画面为 `ordinary-idea-restored.txt`。

### 当前剩余入口及环境限制

- 参数编辑、预览切换及目录右键的 CUA 输入会落到主 IDEA 窗口或收起浮窗；已有 Raise、重新绑定、坐标及键盘尝试，不能据此判定插件按钮正常或异常。系统鼠标点击/键盘的额外授权请求仍待答复；现有授权仅明确涵盖系统鼠标移动。未绕过输入限制调用生产 action，也未将 Find Action 计为右键菜单点击。
- MCP 有界探针仅构建检查，未安装、未开启 loopback 服务；对应的安装和项目访问同意请求待答复。不会绕过 IDEA MCP consent。
- PasswordSafe 的 OS 授权/拒绝、真实后端失败后的 Apply 重试仍未安全复现；没有修改 Keychain 或系统安全设置来制造故障。
- 235 包单次原生可见结果上界为 Spring 695 ms、Dubbo 547 ms、刷新 867 ms，包含工具输入和观察延迟，不是纯生成耗时或性能提升。受控取消的调试暂停时间不计入耗时。
- 官方 IU261 在 `/tmp` 与 `/private/tmp` 别名路径运行时，日志记录 JCEF sandbox 的 bundle 路径不一致警告；本轮没有主动修改 sandbox 配置。该环境警告与 HTML 可视矩阵未完成一并保留，不能称为完整 JCEF 发布环境验收。
- 新包最终核对：ZIP 仅含生产 JAR（309 entries），没有 JUnit、测试夹具或 MCP PoC；OpenSpec 全量 strict 为23 passed、0 failed，本地8份指南/入口文档未发现失效的相对文件链接，`git diff --check` 通过。公开 GitHub 正文是否可访问仍需获准推送后核对。

## 历史候选包基线

- 生产版本 1.3.14，最低 build 261；正式 Verifier 为 IU-261.22158.277，原生环境为 IU-262.10968.63。
- 执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`：43 suites、207 tests，0 failures/errors/skipped；18 tasks 实际执行，构建成功，Verifier Compatible。
- 基线 ZIP SHA-256：`97f5b300a5637644e96a970a258b4759ff4266da97b628ff621f12b2b18e745c`；JAR：`cb21f4d347a43190233e49687f730b4473d32d24ec1692c42eb351e1e2fea99c`。
- 上述是新增生成规则 UI 调整前的结果；调整后门禁与原生范围将单独记录，不能混为同一包。

## 生成规则 UI 调整后的候选包

- 移除五组明显的四边框，使用平台 `TitledSeparator` 和 `ContextHelpLabel`；来源顺序、固定必填规则集中在组级问号说明中。页面仅保留小号灰字摘要，允许按词换行；12 个规则控件和保存行为不变。
- `GenerationRulesLayoutTest` 先取得预期布局失败，再与 `ConfigurationExperienceTest` 合计8项通过；之后重新执行完整命令，43 suites、208 tests、0 failures/errors/skipped，18 tasks 实际执行，Verifier IU-261.22158.277 Compatible。
- 最新 ZIP SHA-256：`2dce669c6d16bc07f6a1d527812c15171eb6b7035fbfc9db4f927376bde489df`；JAR：`41c95d26d6473df608a12ba5455bda9aef83bd0533a61c38efe79ae4d5a65902`。此包已安装到隔离 profile。
- IU262 原生浅色、深色主题下的 2268×1694 设置窗口中，五组浅分隔线、问号图标、短灰字及12个控件均可见，未出现明显方框或文本裁切。Compact Mode 保持开启；这不外推为本次改动的四组主题/密度矩阵。
- 原生生成规则页核对：修改后 Apply 启用；Revert 恢复保存值并禁用 Apply；Cancel 后重开未保存草稿；实际 Apply 后重开看到 `@DocView.Title` 关闭。正常退出后，项目 XML 中也读到 `titleUseCommentTag=false`；待原进程完全退出后重新启动隔离实例，原生页面仍为关闭且 Apply 禁用。最终通过 UI 恢复开启。
- 工具能读取五个帮助图标的无障碍名称及完整说明，但尚未取得悬浮气泡的可靠画面；调整窄窗口的操作没有缩小窗口，不计为窄窗口通过。自动布局用例已覆盖可换行属性和较窄宽度；原生窄窗口及气泡阅读仍待验。

## 本轮原生进展

- 当前贡献的60个中英文设置搜索词均已逐词检查，对应常规、生成规则、模板及平台页面 breadcrumb 正确。原始 AX 按词保存；搜索可能自动选中目标页，分类树屏幕外的行无法被工具点击时，通过实际页面和 breadcrumb 核对，没有把工具点击失败算成插件缺陷。
- 常规设置的行标记修改启用 Apply，Revert 恢复草稿并禁用 Apply；Cancel 后重开仍为保存值；实际 Apply 后重开确认状态，随后通过 UI 恢复原值。此处不外推为所有设置页、多项目或重启矩阵通过。
- 用户在生成规则页观察到常驻长说明、深色边框影响阅读，已作为当前 change 的 UI 修正处理；规则和保存语义不变。
- 预览/JCEF 探针仅附加并记录 PreviewForm 尚未加载，随后断开，没有据此勾选预览释放验收。

## 文档整理

路线图保留20项队列和真实待办，删除重复的历史轮次、旧包身份、PID及过时续作描述；历史证据仍保留在相应 OpenSpec verification。设置与 AI 评估中的历史基线和当前状态已区分。仓库没有独立 handoff 文件，因此没有为清理而删除正式规格、领域技能或沙箱。

## 仍待验证

- 完整设置页 Apply/Reset/Cancel、项目隔离与重启矩阵；密码管理的 OS 提示拒绝及存储失败交互。
- API Preview 的 JCEF 终态、取消/快速切换和关闭释放完整矩阵；目录扫描取消、项目关闭、导出覆盖确认与多模块导航等按各 change tasks 继续。
- 欢迎页完整纯键盘专项按用户要求暂缓；新 GitHub 指南公开正文待获准推送后核对。
- 真实平台上传、Air及正式 MCP 服务验证缺少账号或独立准入条件时跳过，不视为通过。

## 环境恢复与范围核对

- 测试实例已正常退出，普通 IDEA 的 `doc-view – ai-context-assessment.md [doc-view]` 窗口已恢复。
- 隔离 profile 恢复 Islands Light，Compact Mode 配置与原备份逐字一致；doc-demo 的两份既有 DocView XML 均与本轮备份字节一致。
- doc-demo 的 tracked/non-ignored 文件没有新增、修改或删除，HEAD 与 index 均未变化；doc-view HEAD 和 index 也未变化。本轮仅修改生成规则 UI、对应测试/说明、当前 OpenSpec 及文档整理所需15个文件，没有删除文件。
- 首次重启尝试因旧退出确认框仍在而遇到 profile DirectoryLock；确认正常退出并等待旧进程结束后，第二次重启成功。这是工具操作时序问题，未作为插件缺陷或持久化失败。
- `openspec validate doc-view-configuration --strict` 和 `git diff --check` 通过。未提交、推送、发布或归档。

## 本机证据

`/tmp/doc-view-validation-20261020/` 保存基线 manifest、Gradle日志、测试 XML、Verifier 报告、安装包哈希、逐词搜索 AX、设置截图和范围核对。临时目录不是永久发布附件；发布前将必要结论转入正式发布记录。

## 窄窗口、问号与多语言续验

本次续验的基线 ZIP 仍为上方 `2dce…89df`。专用 doc-demo 的隔离 profile 预设 Settings 窗口为 900×700 logical，实际原生截图为 1800×1400 pixels，五组规则及灰字可读，底部可纵向滚动。用户实际悬浮截图确认气泡能够出现，但来源优先级仍是自动折行的长箭头句；因此不能据此完成格式验收。新修订要求编号逐行、回退另段及必填规则分组，后续用新包复验。

用户已明确选择本轮完成主要界面中英化。独立 `localize-plugin-ui` 已按 OpenSpec 建立并通过 strict；旧资源回归取得3项 RED：英文 base 含中文、简中资源缺失及双 locale 模板稳定性无法验证。这里的中英搜索与 UI 翻译是不同验收项。生成文档和用户模板不随界面语言翻译。

续验原始证据位于 `/tmp/doc-view-validation-20261020-rules-locale/`。本节的后续门禁、候选包和语言矩阵在实际执行后补记，不把旧包208项结果冒用到本地化新包。

## 归档、提交与发布判断

以下为本轮源码、tasks、verification 与 canonical 发布清单的交叉核对；不是归档或发布操作。

| 范围 | 当前结论 |
| --- | --- |
| 模板状态、集成设置状态、目录导航 | 三项已有列明的自动及原生闭环证据，可供单独审阅/归档候选。 |
| 注解文本回退、OpenAPI直接参数元数据 | 两项为 PSI/输出变更，已有 fixture/golden/完整门禁，可供单独审阅/归档候选。 |
| Preview/JCEF | HTML终态、取消/快速切换与关闭释放的组合原生验收仍未完成。 |
| 凭据存储 | OS提示拒绝、存储失败重试、项目隔离等真实交互仍待补。 |
| 设置体验 | 完整项目隔离/各页生命周期矩阵仍待补；本轮正在修订气泡和语言呈现。 |
| requiredMode写回 | tasks原先全勾，但verification明确未做原生写回；已补列未验3.4，暂不作为稳妥归档候选。 |
| 欢迎/帮助 | 纯键盘专项按用户要求延期；完整指南尚未推送，公开GitHub设置页本轮仍为404，发布前应在推送后复核。 |
| 导出/索引/性能/异步打开 | 覆盖确认、取消/项目关闭、多模块及迟到结果等未验边界继续按各change tasks推进。 |
| 三平台真实上传、MCP、Air | 真实非生产账号或独立准入条件不足时跳过；不改标通过。 |

当前可以整理明确 scope 后按独立 change 审阅并准备提交；不要求所有未来路线图完成才能保存开发提交。但不能把混合工作树或实现中/未验任务标为正式发布就绪。发布前还需完成本次语言新包门禁、高影响原生缺口、公开文档复核、版本/Unreleased说明和包内容/凭据检查。当前 HEAD 仍带 `v1.3.14`，不能直接复用为本次新正式版本。未完成 change 不为凑进度强行归档；本轮不执行 stage/commit/push/sync/archive/publish。

## 2026-10-21 中英界面最终候选包

本节取代上方历史包的“最新”身份；旧包证据仍仅证明对应历史场景。

- 完整门禁实际执行48 suites、223 tests，0 failures/errors/skipped，18 tasks；IU261 Verifier Compatible。ZIP SHA-256 `66a6c82ea3b64d31e0b5f290e4f89b3ec332e2b28775160a71fb9e34546d5657`，生产JAR `7366dd5c80ac07b9092a036a2b13cead656a79ff83c25fd1baf01a04e337ea85`，已核对隔离安装身份。
- 主要界面采用英文 base / 简体中文资源，跟随 IDE 界面语言；396个键及占位符对应。默认模板、业务源码与生成文档内容不翻译。上传显示文案与稳定平台枚举分离，取消仍无上传副作用。
- 真实英语 IDE 在中文系统中暴露了五个表单错误使用 JVM 默认语言的问题，已改为按 IDE UI locale 赋值并增加相反 locale 回归。另一轮原生发现英语模板说明省略/固定示例提示裁切，已分离可换行说明和按钮行并通过专项与新包原生复验。
- 最终包在900×700逻辑窗口核对英语/简中 × Islands Light/Dark 的生成规则排版；Compact保持开启。英语模板摘要/预览提示完整显示；Preview complete 和独立输出选项可达。先前双语包另有平台页、参数编辑表头/动作、Upload工具栏和欢迎HTML的英语原生记录，不能作为最终包全部交互的替代。
- 气泡内容已按编号、独立回退段及必填规则分组实现；HTML片段和无障碍描述自动验证通过。真实悬停另行记录，不把旧长箭头截图或AX Help当作修复视觉通过。
- OpenSpec全量strict为23 passed、0 failed。当前仍不提交、推送、发布、sync或archive；版本仍1.3.14，正式发布需另行定版。

## 2026-10-21 指南问号最新候选包

用户进一步要求指南入口只用原生问号。模板及其它设置的指南大按钮已统一为 `ContextHelpLabel`；悬浮说明包含对应GitHub链接，文档不打包为离线手册。“恢复默认模板”移入编辑工作区，与变量入口同组，预览/输出工作区不含此操作。页头仅保留短摘要与问号，保持既有草稿保存语义。

- 合并专项6 suites、38 tests通过；完整门禁48 suites、226 tests，0 failures/errors/skipped，18 tasks实际执行；IU261 Verifier Compatible。最新ZIP `e0b695126fc6cc089b9db4bfcc076aa76cead5080802a105c03080309ac5edca`，JAR `d86007e940efc7753aad6435c9532e73fc2dad9745d6be6085ca87a2b17d15fe`，隔离安装已核对同包身份；此前各包的测试数和原生记录保留为历史范围。
- 最新包在中英文900×700逻辑窗口分别核对了常规、规则、模板、集成总览、ShowDoc/YApi/YuQue的问号入口/文案；无指南大按钮。英文总览简短说明和模板操作行可完整阅读；中文规则页可滚动到底，字段名和灰字完整可达。两语言资源各399个键。
- 中文模板页用临时Spring草稿实际点恢复，原生编辑器回到默认内容；Preview complete可达且恢复按钮不属于预览页；Cancel后重开Apply禁用。所有DocView项目XML字节保持，无新增模板XML。未点击Apply、未录入凭据、未上传。
- 工具没有悬停动作，点击问号不能取得真实气泡；旧用户长箭头截图仅为缺陷证据。本轮分行HTML/可访问描述回归已通过，完整气泡视觉和气泡内指南链接点击仍待人工验收，相关tasks保持未勾选。

完整检查、包身份与证据目录见[中英界面验证](../openspec/changes/localize-plugin-ui/verification.md)。本节环境恢复及深色主题结果在实际完成后追加。

### 最新包收尾结果

- 最新e0b6包的模板/规则完成英语/简中 × Islands Light/Dark四组900×700截图；Compact一直开启，不能描述为本轮Compact开/关完整矩阵。其它设置页本轮覆盖中英浅色。真实悬停/气泡链接仍待人工，其余目录/弹窗/欢迎双语完整原生矩阵按localize tasks继续。
- 最后测试实例正常退出，隔离语言/主题/Compact/编辑器颜色配置按备份恢复，设置窗口几何属性恢复。普通 `doc-view – ai-context-assessment.md [doc-view]` IDEA窗口已实际读取确认。
- doc-demo所有DocView XML字节未变、无新增模板XML，tracked/non-ignored 35个文件无新增/修改/删除；doc-view与doc-demo HEAD/index均保持。当前轮91条路径属于本地化/UI/配套测试/规格/指南及验收记录，无删除；原生产源码在最终打包后未再变化。
- 可以按独立scope准备开发提交，当前整批归档/正式发布仍不就绪：真实气泡、本地化完整原生矩阵，以及上文高影响生命周期/凭据等缺口仍须按任务推进；另外需要定新版本和公开GitHub指南核对。本轮没有stage/commit/push/sync/archive/publish。

### 2026-10-21 气泡/指南点击续验状态

复用e0b6包，在隔离doc-demo打开中文浅色生成规则；Computer Use仍无悬停API，现有指针动作没有显示真实气泡。本次没有修改运行时代码，尚未完成气泡阅读或气泡内链接点击。已询问是否明确指定macOS系统鼠标移动，或由用户手动悬停，等待选择；未擅自运行其它鼠标技术或改变系统权限。

结束前已正常退出测试实例并恢复本轮profile备份，项目DocView XML保持字节一致，普通doc-view IDEA窗口已实际确认恢复。未验项不勾选，未提交、发布或归档。证据在 `/tmp/doc-view-tooltip-20261021/`，详见[联合验证](../openspec/changes/localize-plugin-ui/verification.md)。

### 系统鼠标移动授权后的实际结果

用户允许后已执行 macOS CoreGraphics 鼠标移动，并读回系统指针位置；在同一 e0b6 候选包的中文浅色900×700隔离 doc-demo中，窗口 Raise、系统移动及定向 mouseMoved 后的截图仍没有显示气泡。当前不能据此归因为插件缺陷，也不能将指针移动成功记作气泡通过。已请求测试窗口保持桌面前台，本轮结束前未收到确认；五组气泡、双语视觉和指南气泡内点击仍未完成，相关 checkbox 保持未勾选。

测试实例正常退出；隔离语言/主题/Compact/编辑器颜色和窗口几何按新备份恢复，doc-demo两份既有DocView XML全程字节未变。证据目录 `/tmp/doc-view-tooltip-20261021-approved/`；本轮仅补充记录，不改运行时代码，不重复运行或计入此前226项门禁，未提交、发布或归档。

普通 `doc-view – ai-context-assessment.md [doc-view]` 窗口已实际确认恢复；本轮仅两份验收记录有变化，doc-demo文件及两仓库HEAD/index不变。OpenSpec strict本次23 passed/0 failed，diff检查通过；未使用Credits或重置额度。

### 气泡人工验收已完成（2026-10-21）

用户确认“人工验收气泡完毕”，现将气泡阅读和分行复验登记为维护者人工验收完成；configuration任务4.2c/4.3b与localize任务4.2c同步更新。该确认没有逐项列明语言/主题/窗口或包身份，也未确认指南气泡内链接点击与草稿保持，因此不扩展为完整矩阵或指南点击通过；后两项继续见[本地化任务4.2d](../openspec/changes/localize-plugin-ui/tasks.md)。此前工具未显示气泡的记录保留为历史过程。

本次只维护文档和任务记录，没有再次操作IDEA、修改运行时代码、提交、发布或归档。

### 最终包双语主要界面续验

同一e0b6候选包在隔离IU262/doc-demo补齐中英浅色目录扫描与工具栏Upload列表、DTO参数编辑及方法预览按钮、欢迎/更新HTML正文和目录/模板/平台设置主要导航。参数弹窗仅阅读并Esc关闭，设置未Apply，未选择真实上传平台，源码业务名称和原默认Markdown保持。英语和简中更新页返回首页均已实际点击并核对截图。本轮不扩展为完整深浅/Compact矩阵。

目录右键与预览内Upload仍受工具输入限制。维护者确认三处指南已点击、气泡覆盖简中及英语；模板和集成指南实际地址仍为develop并返回404，明确要求master。工具核对指南往返保留临时模板草稿，Cancel后重开草稿标记消失且Apply禁用；旧测试实例已正常退出。随后针对分支缺陷修复并复验，见[双语联合验证](../openspec/changes/localize-plugin-ui/verification.md)最新节；远端master正文亦尚未可用，不因修复链接而宣称404解决。不提交、发布或归档。

### 2026-10-22 文档分支修正及收尾

设置帮助、欢迎/更新页的指南与完整历史链接统一master，精确白名单同步且不接受旧develop链接。先取得2项RED，再专项3 suites/20 tests通过；全量18 tasks实际执行，48 suites/228 tests零失败、错误、跳过，IU261 Verifier Compatible。新ZIP `af61112b30c0b15cae24fdfdb7bdc5c933d1112a2991ad197c2bcfd3248f796b`。

新包在隔离IU262/doc-demo实际点击首页/更新页指南及首页完整历史，浏览器新标签分别核对为master指南目录和master/CHANGELOG；更新页进入模板设置后Apply禁用、无临时草稿，Cancel正常。三处设置气泡的master目标由自动回归覆盖，本轮未重做其人工点击；不外推新包全语言/主题矩阵。master模板/集成文档独立公开读取仍404，本地新指南尚待正式提交并进入发布分支后核对。

测试实例正常退出，隔离语言/主题/Compact/颜色及设置窗口几何已恢复；doc-demo既有DocView XML全程不变、没有新模板XML，tracked/non-ignored文件不变。普通 `doc-view – ai-context-assessment.md [doc-view]` 窗口已实际确认。两仓库HEAD/index不变，本轮14条修改路径在链接、回归、规格和验收范围；未stage/commit/push/sync/archive/publish，未使用Credits或重置额度。localize当前13/14，最终菜单双语矩阵及其它活动change原生缺口仍使整批归档/发布未就绪。

### 2026-10-22 最终包菜单、指南与扫描续验

使用同一af61候选，未修改runtime或重跑上轮228项门禁。维护者完成简中/英语 × Islands Light/Dark × Compact开/关八组类/方法右键Upload、Preview Upload及Esc返回，均正常；这部分以人工反馈为证据，不冒充工具菜单截图。新包模板、常规、集成总览三处指南人工确认master；工具核对指南往返保留草稿，Cancel后重开草稿消失且Apply禁用。localize-plugin-ui任务现为14/14，未归档。

另在英语doc-demo实际应用普通接口开关并刷新，目录五类→关闭后三类→恢复后五类，行标记/参数分表保持；退出后项目XML逐字回到基线。configuration的3.4b只补齐扫描部分，项目隔离、其余页面生命周期/重启矩阵仍未完成。

本轮隔离实例正常退出，语言/主题/Compact/颜色及设置窗口几何恢复，普通doc-view IDEA窗口已实际确认。完整键盘仍按用户要求跳过，真实上传未验；模板/集成master指南此前公开读取返回404，本轮没有重新联网核对，目标正确不代表正文已上线，仍需发布流程核对。其它change的生命周期、凭据、导出等未验项保持，因此不能把本地化完成当作整批可发布。没有提交、推送、sync/archive或发布。

原始证据 `/tmp/doc-view-native-20261022-menus/`，详见[本地化验证](../openspec/changes/localize-plugin-ui/verification.md)及[设置扫描验证](../openspec/changes/doc-view-configuration/verification.md)。

### 2026-10-22 项目隔离与设置生命周期续验

在同进程隔离IU262中，以专用doc-demo(A)和本轮创建的临时B项目验证：常规、生成规则、模板/输出及三平台普通字段的Apply/Revert/Cancel后重开均保持最近保存值；A/B分别保存模板标记、输出前缀和虚构平台字段，切回A与正常退出重启后逐页读回均未串项目。平台集成总览没有独立保存字段，导航不产生脏状态。本轮每页使用代表字段，未逐个点击所有控件；凭据留空、清除未选、未触发上传。具体字段和AX/磁盘证据见[设置验收](../openspec/changes/doc-view-configuration/verification.md#2026-10-22-项目隔离设置生命周期及窄窗口修复)。

验收发现ShowDoc窄窗口的清除标签裁切；先新增布局测试取得1项RED，再将三平台清除选项移至对应密码框下方。专项1/1通过，全量49 suites/229 tests零失败、错误、跳过，18 tasks执行，IU261.22158.277 Verifier Compatible。新ZIP `df21ba775bd79032a3667e7ddef3984efee271a0511140977ceb33415cf50adb`，JAR `6eec14f6752b1a31aa9f16e1bffde276bacf7d92b6f18b2a24fd85534b0e4c2e`。新包在900×700逻辑窗口完成英语深色/简中浅色三平台视觉复验，清除标签完整，独立视觉复核一致。首轮生命周期使用af61；新包完成重启A/B读回及布局复验，不把两者混为同包全部交互重跑。

深浅主题启动后的保存值已核对；外观页即时主题切换受工具菜单限制未成功，单独留在configuration任务3.4c，不能用启动主题变化代替。OS凭据存储/替换/清除、真实上传、完整键盘及其它change未验项保持原状态，未达到整批归档/发布条件。

三次测试实例均正常退出。doc-demo两份既有设置XML按备份恢复字节一致，本轮新增的三份设置XML先保存证据再移除；隔离语言/主题/Compact/颜色及原有设置窗口几何恢复。普通 `doc-view – ai-context-assessment.md [doc-view]` 已实际读回确认。doc-demo tracked/non-ignored文件未变，两仓库HEAD/index保持。证据与日志在 `/tmp/doc-view-native-20261022-settings/`；本轮未stage/commit/push/sync/archive/publish，未使用Credits或额度重置。

### 2026-10-22 即时主题切换人工确认

维护者随后明确反馈“切主题验收完毕”，configuration任务3.4c据此登记为人工验收通过，该change现为29/29完成、未归档。此前工具未能即时切换的记录保留为历史过程；本次不新增语言/Compact/窗口/包身份矩阵声明，也没有重跑229项自动门禁。

本次只维护四份任务/验收/路线图文档，没有操作IDEA或修改运行时代码。凭据、真实上传、完整键盘及其它change缺口保持，整批发布就绪结论不变；未提交、推送、sync/archive或发布。详见[主题人工验收记录](../openspec/changes/doc-view-configuration/verification.md#2026-10-22-即时主题切换人工验收完成)。

### 2026-10-22 真实上传从本轮验收排除

维护者明确要求“真实上传忽略”。据此将platform-upload-stability原任务3.2移出必需checkbox清单，记为本轮非阻断排除项；没有执行真实请求、验证真实返回链接或真实平台版本，也不标记这些场景通过。自动/mock响应契约、超时、取消与错误脱敏回归继续保留，OS凭据原生场景不受此次排除影响。

路线图和canonical发布清单已同步本轮例外。真实上传不再是本轮提交/归档/发布评估的阻断项；其它change未完成项与提交/发布授权边界保持，没有据此宣称整批可发布。本次仅修改五份文档，没有修改运行时代码、重跑Gradle、操作IDEA、提交、推送、sync/archive或发布。
