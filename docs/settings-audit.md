# 设置与用户文档评估

评估日期：2026-10-18。源码基线：`develop` / `00f1f94`。

结论：需要同时整理设置分类、修正状态与字段行为、维护用户文档。先处理会误保存或误导操作的问题，再调整布局。多数设置有实际消费路径，但“存在字段、能保存”不等于它有效。

本文是截图、源码和公开文档评估，不是新增行为规范或实施记录；评估本身没有运行 Gradle、`runIde`、真实上传或修改语雀内容。模板生命周期后来由独立 change 获准并修复，自动回归、Verifier 与原生验收见[验证记录](../openspec/changes/fix-template-settings-lifecycle/verification.md)。最终包重启核验已完成，修复尚未发布；已发布的 1.3.14 仍有设置页覆盖自定义模板的风险。后续顺序见[路线图](roadmap.md)，UI 总体原则见[设计研究](design.md)。

实施跟踪：路线图第 2～7 项已有独立 change。本文台账、截图和“当前”均指 2026-10-18 的评估基线；源码链接可能已更新，不能据此判断问题仍然存在。现行操作、默认值及验收边界见[用户指南](user-guide/index.md)、[路线图](roadmap.md)和各 change 的 verification。原先因 Mac 锁定而未完成的 UI 核对中，部分场景已在后续补验；完整设置页、多项目和重启矩阵仍有待验项。当前状态集中见[2026-10-20 验证记录](validation-2026-10-20.md)。真实平台上传仍未验证。

## 1. 五个页面的体验评估

截图来源为本轮用户提供的五张设置页图片，编号对应消息中的图 1–5；不是本轮运行插件取得的截图。

| 步骤 / 页面 | 当前可用基础 | 主要问题 | 建议 |
| --- | --- | --- | --- |
| 1. 主设置页 | 已有原生控件与分组，基本来源可选择 | 一页混合来源、字段、扫描、输出；四五个来源横排看不出先后；灰色勾选同时表示固定规则和无效功能；“其他配置”语义模糊 | 按任务拆分；明确固定优先级与回退；固定规则改说明；移除无效果入口时按新版设置结构处理，不加旧字段别名或自动清理用户数据 |
| 2. Markdown Template | Spring/Dubbo 分开；已有编辑器与变量清单 | 外层、模板和说明多处滚动；说明位于编辑区域下方；没有输入样例、输出预览和错误解释 | 保留原生编辑器；变量帮助可搜索/插入，样例与预览并排或切换；减少嵌套滚动；恢复默认是显式草稿动作 |
| 3. ShowDoc | 三个核心字段齐全，展示接口后缀 | 基址与完整请求地址含义不够明确；凭据普通文本；没有就近帮助 | 给出“输入基址 → 最终请求地址”示例，凭据遮蔽，提供配置指南 |
| 4. YApi | 地址、项目 ID、Token 具备实际用途 | 缺格式示例；修改检测与 ID 校验耦合，有漏报和部分提交风险 | ID 文本比较与合法性校验分开；校验通过后再提交；错误定位到字段 |
| 5. YuQue | 站点与 API 地址分别存储，已有默认值 | 两个字段都写 `url`；`username/repos` 不够具体；缺配置时跳错页面 | 明确“站点地址 / API 基址 / 用户路径 / 知识库路径 / Token”，说明组合后的目标，不展示凭据 |

屏幕阅读器、键盘 Tab 顺序、不同缩放/窄窗口和实际对比度未测试。表单的标签关联需要在实现时检查；不能凭截图宣称通过可访问性验收。留白本身不是缺陷，主要问题是缺少必要说明和反馈，不应为了填满平台页增加无关内容。

## 2. 设置项有效性台账

除特别注明外，以下“有效”仅表示已沿源码找到使用路径，未运行实际开关对照测试。主设置是项目级，默认值来自 [Settings.java](../src/main/java/com/liuzhihang/doc/view/config/Settings.java)，读回、比较与提交来自 [SettingsForm.java](../src/main/java/com/liuzhihang/doc/view/ui/SettingsForm.java)。截图中的用户选择不能当作产品默认值。

| 设置项 | 当前默认 / 状态 | 实际作用与限制 | 处理建议 |
| --- | --- | --- | --- |
| 标题：`@DocView.Title`、类注释、类名、全类名 | 四项 true，可编辑 | 按上述顺序获取，最终回退 `DocView`；类名启用且非空时，全类名不会被选中 | 保留，按优先级纵向展示；说明“启用来源”不等于拼接所有来源 |
| 标题尾部 `DocView` | 固定、不可编辑 | 固定回退文本，不是开关 | 改为“无可用来源时使用 DocView”说明 |
| 接口名：`@Operation`、`@ApiOperation`、`@DocView.Name`、方法注释 | 四项 true，可编辑 | 按顺序获取；方法注释截取前 15 字符；最后回退方法名 | 显示具体属性与回退顺序；空值边界另立回归 |
| 接口名尾部“方法名” | 固定、不可编辑 | 最后回退 | 改为说明，不使用禁用复选框 |
| 描述：`@Operation`、`@ApiOperation` | true，可编辑 | 分别读取 `description` / `notes`，再回退方法注释；属性存在但为空时当前不会继续回退 | 先记录当前行为；是否统一空值回退需独立 contract |
| 描述尾部“方法注释” | 固定、不可编辑 | 最后来源 | 改为说明 |
| Validation/Lombok 必填来源 | 固定规则，灰色勾选 | 固定判断配置集合中的 `@NotBlank/@NotEmpty/@NotNull/@NonNull` | 展示“自动识别的来源”，不要伪装为可选项 |
| Swagger 必填来源 | 固定规则，灰色勾选 | 当前读取 `@Schema.required`、`@ApiModelProperty.required`；不是注解出现就一律必填，也不代表完整支持 `requiredMode` | 说明具体属性；`requiredMode` 保留独立待办 |
| `@DocView.Required` | true，可编辑 | 仅控制是否识别字段 Javadoc 必填 tag，代码写回另有逻辑；参数的 required 判断读取注解及 `@RequestParam(required=...)` 等另一条路径 | 保留，明确作用于字段读取 |
| `@JsonProperty` | true，可编辑 | 字段名优先取 Jackson 注解 value，否则 Java 字段名 | 从“是否必填”分组移到“字段名称” |
| 快捷图标 | true，可编辑 | 控制编辑器 gutter 的类/方法标记 | 改名“显示编辑器行标记”，与扫描范围分开 |
| 普通接口 | false，会被动态禁用 | 开启后扫描模块接口；普通接口由 Dubbo 文档服务处理。实际扫描不依赖 gutter 开关，但 UI 按 gutter 状态禁用它 | 修复错误联动，改名“包含普通 Java 接口”，说明适用范围 |
| 独立实体 | false，可编辑 | 将请求/响应 Body 的嵌套对象用独立表格/段落展示；不是生成独立文件 | 移到输出分组，改名“嵌套对象分表展示”，提供前后示例 |
| 合并导出 | true，可编辑 | 控制批量文件导出合并到类名文件还是按接口分文件；不控制平台上传 | 放在输出/导出分组；解释粒度与重复导出行为 |
| 隐藏左侧 | true，UI 禁用 | 仅保存/读回，没有运行时消费。预览目录由是否传入具体方法决定 | 候选为从新版 UI 与配置结构移除，不增加旧字段兼容；不自动清理用户项目数据 |
| 前缀符号 1 / 2 | 空字符串 / `-->` | 作用于普通递归 Markdown 参数表；保存时 trim；独立实体模式的分表不使用这两个前缀 | 命名为层级缩进相关参数，显示渲染示例；明确空格和分表限制 |
| Spring/Dubbo 模板 | bundle 内置值，可编辑 | 预览、复制、导出、ShowDoc/语雀上传使用；YApi 有独立 payload 与描述拼接。当前工作树的 Reset 回读最近一次 Apply 值，Apply/OK 持久化，Cancel 丢弃未应用草稿；公开旧版仍有覆盖风险 | 保留原生编辑器并让用户指南说明真实生效范围；实时预览、错误定位仍是后续建议 |

使用点索引：

- 名称与描述：[DocViewUtils.java](../src/main/java/com/liuzhihang/doc/view/utils/DocViewUtils.java)，`getTitle` 102–144、`getName` 159–209、`getMethodDesc` 221–249。
- 字段/参数必填及名称：同文件 351–450；`@ApiOperation(value="")` 在 174–180 返回空文本，不保证最终回退方法名。
- 普通接口：[ModuleNode.java](../src/main/java/com/liuzhihang/doc/view/ui/window/ModuleNode.java) 41–65、[DocViewService.java](../src/main/java/com/liuzhihang/doc/view/service/DocViewService.java)；错误 UI 联动在 `SettingsForm` 106、155、182。
- 输出：[DocViewData.java](../src/main/java/com/liuzhihang/doc/view/dto/DocViewData.java) 96–136、161–244、341–375；[ExportUtils.java](../src/main/java/com/liuzhihang/doc/view/utils/ExportUtils.java) 54–100。
- 无效隐藏项：[基线 Settings.form](https://github.com/liuzhihang/doc-view/blob/00f1f944c7b22cc03219f06324d177fecf9e848f/src/main/java/com/liuzhihang/doc/view/ui/Settings.form) 450–458（此 form 已在配置改造中移除）；[PreviewForm.java](../src/main/java/com/liuzhihang/doc/view/ui/PreviewForm.java) 260–273。

另有未暴露为 UI 的无效字段 `excludeParamTypes`：仅被缓存 hash 读取，实际排除参数使用另一集合 `excludeParameterType`。后续可从新版配置结构移除此死字段，不新增用户编辑框、不迁移旧键，也不自动清理用户项目文件。

不能统一宣称“改设置后缓存不失效”：[DtoSchemaCacheService.java](../src/main/java/com/liuzhihang/doc/view/service/DtoSchemaCacheService.java) 已把相关设置和模板纳入 key，`PreviewForm` 的 Markdown key 也包含模板/分表/前缀。设置 Apply 是否立即刷新已打开的每个视图，需要独立验证；新生成结果与旧文件不是同一验收对象。

## 3. 三个平台的字段与可靠性

| 平台 / 字段 | 源码确认的用途 | 文案与帮助建议 |
| --- | --- | --- |
| ShowDoc 地址 | 在输入后追加 `/api/item/updateByApi` | 标明是接口基址。用虚构例子 `https://docs.example.test/server` 展示最终地址，避免用户重复输入后缀 |
| ShowDoc ApiKey / ApiToken | 上传请求体的鉴权字段 | 各字段给获取路径与作用；不要在日志、预览或示例中回显 |
| YApi 地址 / 项目 ID / Token | 地址构建分类/保存 API；ID 选择项目；Token 鉴权 | ID 是数字项目标识，给无效输入提示；表单可全部留空表示尚未配置，不强迫未使用平台的用户配置 |
| YuQue 站点地址 | 构造上传成功后的浏览链接 | 与 API 基址区分；不删除这个有实际用途的字段 |
| YuQue API 基址 | 查询、创建和更新文档的请求地址 | 第二个 `url` 改为明确的 API 标签；当前代码 binding 正确，label key 错误 |
| YuQue username / repos | 拼成 `login/repos` 命名空间 | 提示填路径标识，不是展示名称或整条文档 URL；目标预览不含 Token |
| YuQue Token | 请求头鉴权 | 遮蔽显示，提供获取指南；不在 Apply 时偷偷发测试上传 |

三家平台集成的配置字段都有实际调用用途，不能因为页面简单就删除。调用实现见 [ShowDocFacadeServiceImpl](../src/main/java/com/liuzhihang/doc/view/integration/impl/ShowDocFacadeServiceImpl.java)、[YApiFacadeServiceImpl](../src/main/java/com/liuzhihang/doc/view/integration/impl/YApiFacadeServiceImpl.java)、[YuQueFacadeServiceImpl](../src/main/java/com/liuzhihang/doc/view/integration/impl/YuQueFacadeServiceImpl.java)。本轮未验证第三方服务端兼容性。

需要优先复现的源码问题：

1. **YApi 修改检测与校验混杂。** URL 不变时，空/非数字 ID 让 `isModified()` 提前返回 false，遮蔽 ID/Token 的修改。数字与保存的 null ID 比较存在拆箱风险，超出 Long 范围会在修改检测中抛异常。`reset()` 在保存 ID 为 null 时不清空旧输入；`apply()` 会先写 URL，再校验 ID，可能部分保存。依据：[YApiSettingForm.java](../src/main/java/com/liuzhihang/doc/view/ui/YApiSettingForm.java) 35–92。
2. **空状态和地址规范化不一致。** 未配置的 nullable 字段与文本框空串比较可能显示无编辑却已修改；YuQue 两个 URL 有默认值，login/repos/token 初始为 null。Apply 去掉一个末尾斜线但未回填文本框，可能 Apply 后仍报告修改。依据：[ShowDocSettingForm.java](../src/main/java/com/liuzhihang/doc/view/ui/ShowDocSettingForm.java)、[YuQueSettingForm.java](../src/main/java/com/liuzhihang/doc/view/ui/YuQueSettingForm.java) 及 YApi form。截图中的 Revert changes 不能单独证明其原因，需用干净状态复现。
3. **语雀缺配置跳错页面。** [YuQueServiceImpl.java](../src/main/java/com/liuzhihang/doc/view/service/impl/YuQueServiceImpl.java) 34–48 实际打开 ShowDoc Configurable，应修正导航目标。
4. **YApi 设置 ID 不一致。** [plugin.xml](../src/main/resources/META-INF/plugin.xml) 注册 ID 与 [YApiSettingsConfigurable.java](../src/main/java/com/liuzhihang/doc/view/config/YApiSettingsConfigurable.java) 的 `getId()` 不同。影响搜索/定位的程度待 IDE 复现；统一时采用一个新版 ID，不为旧 ID 增加别名。
5. **凭据与普通设置混存。** 三组 Settings 的 token/key 是项目 XML state 中的普通 String，UI 使用普通 `JBTextField`，没有 PasswordSafe。建议独立设计新版凭据存储，界面遮蔽、日志脱敏；切换后由用户重新填写凭据，不读取或迁移旧 XML 值，也不自动清理用户文件。本轮未读取用户真实配置文件，也没有实际泄露事件证据。

前三项应独立于模板修复处理；凭据存储另立 change，定义用户重新配置、取消、项目区分和不可用时的反馈，不建立旧值迁移或自动删除逻辑。新增“测试连接”只有在确认服务有合适的只读检查路径后再做，不能用创建文档充当无副作用连接测试。

## 4. 推荐的信息结构与布局

建议入口为 **Settings → Tools → Doc View**，保持当前项目级作用域。移动分类不等于把设置改成全局，也不需要再增加一个“项目”子页面。页面简短注明“作用于当前项目”；未来全局模板预设/覆盖继承另行设计。

| 页面 | 内容 |
| --- | --- |
| Doc View / 常规 | 编辑器行标记、普通接口扫描、项目作用域说明、用户指南入口 |
| 生成规则 | 标题/名称/描述来源和固定回退、字段必填规则、字段命名 |
| 模板与输出 | Spring/Dubbo 模板、变量帮助、样例/预览；嵌套对象分表、层级前缀、批量导出方式作为分组 |
| 平台集成 | ShowDoc、YApi、YuQue 的统一入口，各自配置表单与帮助链接；按新版 `parentId` 层级组织，每个 configurable 使用一个规范 ID，不添加旧 ID 别名 |

目前只有一个合并导出开关，不建议立即单独建空旷 Export 页面。未来项目目录、命名、覆盖策略真正落地后再拆。该建议细化了前一轮研究中五页结构，最终层级在配置 change 内确认。

布局规则：

- 来源按固定优先级从上到下排列，显示具体属性名与命中示例。保留多选来源语义，不误改为只能选一个的单选项，也不额外引入拖拽排序能力。
- 固定回退/自动规则使用普通说明文本；无效项与固定项分别处理。主设置里的“字段名称”与必填规则分组分离。
- 模板编辑器优先使用可调分隔区，变量说明和预览按任务切换；小窗口仍能看到编辑器、帮助入口与错误，不机械压成三栏。
- 平台表单同列对齐，字段宽度依据输入类型；字段下给虚构例子及获取方法，不只依赖 hover tooltip。URL、用户路径、Token 的校验结果分别反馈。
- 凭据遮蔽与平台存储改造分开验收；PasswordSafe 切换后允许用户重新填写凭据，不迁移旧 XML，也不自动清理其中的数据。平台配置为空时不阻塞本地生成。统一术语、冒号、大小写与本地化。
- 当前构建关闭 `buildSearchableOptions`。后续需验证“模板 / template / 导出 / YApi”等关键词；实现了 `SearchableConfigurable` 不等于字段关键词都能搜索。

依据：JetBrains [Settings Guide](https://plugins.jetbrains.com/docs/intellij/settings-guide.html) 明确建议使用 `tools` 而非 `other`，子层级使用 `parentId`；[Layout](https://plugins.jetbrains.com/docs/intellij/layout.html) 与 [Checkbox](https://plugins.jetbrains.com/docs/intellij/checkbox.html) 说明分组和选择语义；[Input Field](https://plugins.jetbrains.com/docs/intellij/input-field.html)、[Validation Errors](https://plugins.jetbrains.com/docs/intellij/validation-errors.html) 与 [Persisting Sensitive Data](https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html) 支持字段帮助、错误定位及凭据存储设计。具体 Java API 仍需在项目目标 build 验证。

## 5. 使用文档现状：已有，但过时且不完整

本轮在内置 Browser 成功读取语雀知识库和以下页面；不能因搜索工具打不开就认定文档不可访问。知识库首页显示 15 篇文档，本次不是对全部页面或外部服务 API 的验收。

| 文档入口 | 已有内容 | 需要维护 |
| --- | --- | --- |
| [README](../README.md) | 原有功能、安装、右键入口 | 原使用部分过短且功能清单漏语雀；本轮已补功能与新版用户指南入口 |
| [配置](https://www.yuque.com/liuzhihangs/doc-view/lvfpc4) | 基本设置与模板两张旧截图，页面显示 2022-06-17 | 不能只换截图，要有条件、动作、预期结果 |
| [基础设置](https://www.yuque.com/liuzhihangs/doc-view/td21vx) | 标题、名称、描述、必填来源，页面显示 2022-06-17 | 全类名/类名顺序与现代码相反；未解释当前新增/复杂开关；将“注解存在即必填”修正为真实判断属性；注解名用行内代码，避免被当作语雀用户链接 |
| [模版设置](https://www.yuque.com/liuzhihangs/doc-view/ksz0nq) | Velocity 说明、10 个变量、部分 Spring 模板，页面显示 2022-06-17 | 当前读到的 Spring 示例停在返回参数标题，Dubbo/生成示例只有标题；补完整可复制模板、输入/结果对照、空值与错误说明 |
| [YApi](https://www.yuque.com/liuzhihangs/doc-view/wo7099) | 三字段用途和获取位置 | 补基址/项目 ID 示例、配置边界、错误恢复和批量部分成功说明 |
| [ShowDoc](https://www.yuque.com/liuzhihangs/doc-view/obcybw) | 完整请求地址和 key/token 获取提示 | 明确设置输入与最终请求地址的区别，避免重复追加接口后缀；第三方部署差异按当前官方资料重新核对 |
| [语雀](https://www.yuque.com/liuzhihangs/doc-view/yuque) | username/repos/token 获取示意和旧截图 | 补两个 URL 区别、准确路径例子、上传步骤和结果；当前部分上传小节只有标题；权限与平台 UI 另核对现行官方说明 |
| [维护者文档](ai-workflow.md) / OpenSpec / changelog | 架构、开发、验收与更新记录 | 保持其用途；不当作普通用户指南 |

已读基础设置文档写的是“全类名先于类名”，而 `DocViewUtils.getTitle` 实际先读取类名。这是文档与当前源码的直接冲突，应修正文档，不靠换 UI 顺序让旧文档看似成立。

模板的简要变量说明已经存在，不能说完全没有帮助；主要缺的是能完成任务的教程。基本变量多为已渲染的 Markdown 字符串；要教用户删改表格列，还需要说明 `DocViewData` 的列表变量和 `DocViewParamData` 字段，不能把 `${DocView.requestBody}` 误写成任意结构的原始 DTO。

## 6. 文档整体维护方案

2026-10-19 用户明确完整使用文档只维护在 GitHub `docs/user-guide/`，随代码分支/tag 审阅和回溯。IDE 内置页面呈现随包 HTML 欢迎与本版本更新说明；完整手册不打包，设置帮助在默认浏览器打开对应 GitHub 章节。该运行时方案已实施；本轮自动及原生验收状态见[验证记录](validation-2026-10-20.md)和各 change 的 verification。新指南正文尚未推送，公开章节链接可能暂时返回 404。渠道比较与实施边界见[文档来源与阅读入口](design.md#44-文档来源与阅读入口)。

用户随后授权补全文档，本轮已创建以下五页，并在 README 加入入口；当前仅写入本地仓库，没有上传 GitHub 或修改远程语雀：

| 文档 | 必须解决的用户任务 |
| --- | --- |
| [快速开始](user-guide/index.md) | 安装、支持范围、虚构 Spring/Feign/Dubbo 示例，解释预览、编辑、导出和上传入口 |
| [设置说明](user-guide/settings.md) | 默认值、作用范围、优先级、限制、何时重新生成；区分现状与待修复问题 |
| [模板教程](user-guide/templates.md) | 改标题与改参数表；变量类型、条件/循环、输入/输出、Reset/Apply/Cancel 当前语义与公开旧版风险；当前模板快照 |
| [平台集成](user-guide/integrations.md) | 三平台配置矩阵、获取途径、最终地址示例、上传范围及结果；安全的虚构配置 |
| [故障排查](user-guide/troubleshooting.md) | 无接口、无名称、模板 ERROR、配置未保存、网络失败、旧文件未更新；解释 Refresh 与 Clear 的差别 |

模板生命周期的 Apply/Reset/Cancel 步骤已按专项验证结果同步到[模板教程](user-guide/templates.md)；原生保存、重启和草稿状态验收见模板 lifecycle change。设置导航、模板预览、PasswordSafe 和各页帮助入口已按实现更新到指南；当前各项验证以[本轮验证记录](validation-2026-10-20.md)和对应 change verification 为准，文档变更本身不代替运行时证据。每次后续行为 change 继续同步受影响指南与版本记录。后续如迁移语雀，先建立旧页面到新章节的映射并保留旧链接，再单独授权发布，不直接删除原有知识库。

需要显式说明的操作边界：[WindowClearAction.java](../src/main/java/com/liuzhihang/doc/view/action/toolbar/window/WindowClearAction.java) 22–23 不仅清缓存，还删除 Doc View 的生成文件目录；[CustomFileUtils.java](../src/main/java/com/liuzhihang/doc/view/utils/CustomFileUtils.java) 69–86 执行删除。故障排查不能把 Clear 描述为无损刷新，也不能自动替用户执行。

## 7. 后续验收与拆分

`fix-template-settings-lifecycle` 已按原范围完成，不再扩展该 change。集成设置状态、PasswordSafe 和设置分类已有实现；各项原生证据和剩余 task 见[本轮验证记录](validation-2026-10-20.md)及对应 OpenSpec change。字段校验、Apply/Reset/Cancel 和导航使用虚构值核对；真实平台上传需要非生产测试账号和写入目标，缺少账号时应跳过并记录。

用户指南五页已按当前源码补全。模板生命周期及欢迎/更新页已有独立原生验收证据；设置帮助入口和其他原生范围按各自验证记录跟踪。教程任务、真实平台上传和 GitHub 正文公开状态分别核对，不互相替代。

本评估在 2026-10-18 完成时未执行集成设置的运行时复现、第三方上传或新指南远程发布；这是评估当时的范围，不是后续验收状态。模板生命周期的原生 UI 与磁盘重启验收由独立 change 完成，证据见[验证记录](../openspec/changes/fix-template-settings-lifecycle/verification.md)。欢迎页、更新页和设置帮助的验收范围见[2026-10-20 验证记录](validation-2026-10-20.md)及当前 change verification。真实平台上传和新指南远端公开状态仍需各自验证，本文不将其写成通过。
