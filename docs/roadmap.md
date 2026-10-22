# 路线图

Doc View 的近期目标是稳定“Java 源码 → `DocView` → Markdown/导出/上传”主链路，再逐步补齐 IDE 内的文档查找、配置和 AI 检索能力。产品与 UI 方向及其证据、假设和验收建议见[设计研究](design.md)。

设置项有效性、平台配置状态及现有用户文档的补充核对见[设置与用户文档评估](settings-audit.md)。有源码证据的集成设置可靠性与凭据存储排在视觉改版前，各项使用独立 change。

## 1.4.0 发布状态

截至 2026-10-22，1.4.0 最终候选包已构建：49 suites、237 tests 全部通过，18 个 Gradle actionable tasks 执行，IU 261.22158.277 Plugin Verifier 为 `Compatible`。最终包已在官方 IU261 中进行部分原生 smoke：加载 1.4.0、JCEF sandbox enabled、升级通知显示 1.4.0、参数编辑器能打开并读取字段、欢迎页 HTML 能显示；Schema 写回及其他残余场景仍未全验。ZIP SHA-256、包清单、原生烟测和剩余验收边界见[1.4.0 发布评估](release-1.4.0.md)。提交、PR、合并、Release tag、GitHub Release 和 Marketplace 发布尚未执行；维护者已授权继续完成这些操作。

真实平台上传按维护者要求排除；OS PasswordSafe 异常路径、Schema 原生写回完整性和 JCEF 特殊状态仍有验证缺口，不得写为全量验收通过或归档。MCP Java 探针是独立开发 PoC，不属于 1.4.0 生产交付或自动发布阻断项。

## 有序队列

下表记录推进顺序；当前授权与执行规则见[本轮授权边界](#本轮授权边界)。每项进入开发时都应有独立 OpenSpec change 和相应验证，并非每项都在技术上依赖前一项。未完成项以各 change 的 tasks 和 verification 为准。

| 顺序 | Change / 范围 | 目标与推进条件 |
| --- | --- | --- |
| 1 | [`fix-template-settings-lifecycle`](../openspec/changes/fix-template-settings-lifecycle/proposal.md) | 模板草稿、Apply/Reset/Cancel 状态与资源释放问题已修复，自动回归及原生验收完成；本地未提交、未发布。详见[验证记录](../openspec/changes/fix-template-settings-lifecycle/verification.md)。 |
| 2 | [`fix-preview-lifecycle`](../openspec/changes/fix-preview-lifecycle/proposal.md) | 预览生成、后台任务列表和渲染切换已实现并完成自动验证；原生打开/重开与主题场景已验。JCEF 预览窗口关闭及释放的剩余边界见[验证记录](../openspec/changes/fix-preview-lifecycle/verification.md)。 |
| 3 | [`fix-integration-settings-state`](../openspec/changes/fix-integration-settings-state/proposal.md) | 设置空值、地址规范化、YApi 校验/部分提交、语雀导航和配置注册 ID 已修复并通过回归；原生 Apply/Revert 与缺配置导航已验。真实平台上传按维护者要求从本轮验收排除，不作阻断；详见[验证记录](../openspec/changes/fix-integration-settings-state/verification.md)。 |
| 4 | [`integration-credential-storage`](../openspec/changes/integration-credential-storage/proposal.md) | 已修复 PasswordSafe 同 serviceName 的跨项目覆盖：生成键按平台与项目位置隔离，并由真实 IU261 InMemoryCredentialStore 取得 RED/GREEN。最新235候选包在IU261原生验证 A/B 保存、同包重启回读及清除 A 后 B 仍保留；OS 授权/拒绝和后端失败重试仍待验。真实上传本轮排除，旧 XML 不迁移。详见[验证记录](../openspec/changes/integration-credential-storage/verification.md)。 |
| 5 | [`api-tree-navigation`](../openspec/changes/api-tree-navigation/proposal.md) | 目录排序、源码/文档导航、刷新/隐藏、菜单、上传入口、深浅主题及 Compact Mode 等已实现；主要原生矩阵已验。证据范围和未覆盖项见[验证记录](../openspec/changes/api-tree-navigation/verification.md)。 |
| 6 | [`doc-view-configuration`](../openspec/changes/doc-view-configuration/proposal.md) | 设置分类、模板预览、规则说明、60个搜索词已验；2026-10-22补齐各页代表字段保存/撤销/取消、A/B项目隔离和重启读回，并修复三平台窄窗口清除标签裁切。即时主题切换由维护者人工确认通过，当前29/29任务完成、未归档；凭据原生验收另见credential change。详见[验证记录](../openspec/changes/doc-view-configuration/verification.md)。 |
| 7 | [`doc-view-welcome`](../openspec/changes/doc-view-welcome/proposal.md) | 已改为随包 HTML/CSS + JCEF 欢迎页与更新页，7 个设置帮助入口打开 GitHub 指南。当前自动门禁和原生验收状态见[本轮验证入口](#本轮验证与续作入口)及[验证记录](../openspec/changes/doc-view-welcome/verification.md)；键盘检查按用户要求跳过，新指南正文尚未推送。 |
| 8 | [`openapi3-schema-required-mode`](../openspec/changes/openapi3-schema-required-mode/proposal.md) | `requiredMode` 三态、优先级、安全写回及 DTO golden 已实现并通过自动回归；原生参数编辑写回待补。详见[验证记录](../openspec/changes/openapi3-schema-required-mode/verification.md)。 |
| 9 | [`openapi3-parameter-metadata`](../openspec/changes/openapi3-parameter-metadata/proposal.md) | 方法参数名称、描述、示例及 Spring 绑定/required 冲突规则已实现并通过自动回归。详见[验证记录](../openspec/changes/openapi3-parameter-metadata/verification.md)。 |
| 10 | [`doc-generation-performance`](../openspec/changes/doc-generation-performance/proposal.md) | 已处理缓存 epoch、过期结果写回和递归取消。IU261 原生目录扫描取消后保留旧树、不发布部分结果已验；235 包已补整类 Spring/Dubbo 源预览和刷新的一次可见时间上界；取消后的可见终态、重复测量及 HTML 矩阵仍待补。详见[验证记录](../openspec/changes/doc-generation-performance/verification.md)。 |
| 11 | [`doc-export-workspace`](../openspec/changes/doc-export-workspace/proposal.md) | 项目 Markdown、稳定身份/源码链接、冲突保护和后台快照已实现。IU261 已补覆盖确认、扫描取消和关闭项目不发布；IU261/IU262 已补两模块索引及页面/源码导航。导出前关闭项目仍创建空目录的缺陷已修复并原生复验；目录动作通过 Find Action 执行，未冒充右键菜单点击。详见[验证记录](../openspec/changes/doc-export-workspace/verification.md)。 |
| 12 | [`mcp-java-feasibility`](../openspec/changes/mcp-java-feasibility/proposal.md) | 隔离探针的 schema/调用边界已有证据；真实 IDE Application、服务注册、调用和生命周期未验。本轮跳过需额外同意的项目访问权限；第 13 项以本项 PoC 通过为前置条件。详见[验证记录](../openspec/changes/mcp-java-feasibility/verification.md)。 |
| 13 | `doc-view-mcp-tools`（仅 PoC 通过后） | 另立 change 实现只读 API 搜索与详情工具；仅复用稳定文档快照，不扩大 IDE 基线或改动现有生成链路，除非另行确认。 |
| 14 | 轻量 API Wiki / [`doc-export-index-readability`](../openspec/changes/doc-export-index-readability/proposal.md) | 已将宽表改为模块/类/方法分组；IU261/IU262 两模块的接口页、重载及 Document/Source 导航已原生验证。模型目录只在实测缺口后考虑；不拆独立 Wiki 插件，也不要求 MCP 先成功。 |
| 15 | Air 可选协作评估 | 已核对官方 ACP/MCP 边界并定义只读流程；原生账户/会话和文件读取未验。保持可选评估，不把 Air、ACP 或 MCP 视为已集成能力。见[评估记录](ai-context-assessment.md#可执行的只读验收流程)。 |
| 16 | [`platform-upload-stability`](../openspec/changes/platform-upload-stability/proposal.md) | 语雀错误分类、响应校验、编码、超时、资源关闭与取消已修复并通过本机 HTTP/上传服务测试。真实上传按维护者要求移为本轮非阻断排除项，第三方账户未访问，不标记实测通过。详见[验证记录](../openspec/changes/platform-upload-stability/verification.md)。 |
| 17 | `doc-import-viewer` | [源码评估完成](ai-context-assessment.md#历史文档导入与查看路线图-17)：不重复建设 Markdown 查看器。先完成第 11/14 项原生阅读验证；只有历史目录/差异存在实测缺口时，才另立只读快照 change。 |
| 18 | [`clear-cache-safety`](../openspec/changes/clear-cache-safety/proposal.md) | Clear Cache 已改为只清当前项目 schema 缓存，不再删除手写 Scratch；自动回归及工具栏入口已验，目录菜单入口仍待验。详见[验证记录](../openspec/changes/clear-cache-safety/verification.md)。 |
| 19 | [`annotation-text-fallback`](../openspec/changes/annotation-text-fallback/proposal.md) | 空白注解回退及 Swagger2 常量名处理已修复，并通过回归测试。详见[验证记录](../openspec/changes/annotation-text-fallback/verification.md)。 |
| 20 | [`async-method-doc-preview`](../openspec/changes/async-method-doc-preview/proposal.md) | 方法文档以可取消后台文本快照生成，再于 EDT 打开 Scratch；IU261 原生 Enter/双击、任务取消和项目关闭后丢弃迟到结果已验，未产生部分 Scratch。受控断点不作为自然耗时证据。详见[验证记录](../openspec/changes/async-method-doc-preview/verification.md)。 |

## 用户文档维护

用户追加的主要界面中英化由独立 [`localize-plugin-ui`](../openspec/changes/localize-plugin-ui/proposal.md) 跟踪，不把中英文设置搜索视为 UI 翻译。该 change 保持用户模板、源码和生成文档语言不变；原生语言/窄窗/问号验收与提交发布判断集中在[本轮记录](validation-2026-10-20.md)。

文档工作与迭代同步。仓库已建立[用户指南](user-guide/index.md)入口，包含快速开始、设置、模板、平台集成和故障排查，并由 README 链接。语雀内容作为迁移和核对来源；没有更新远程语雀。

完整使用文档只在 GitHub 仓库 `docs/user-guide/` 维护，按代码分支和 tag 审阅回溯；IDE 内置页面只提供欢迎信息和当前版本更新说明，不打包完整手册。设置帮助在默认浏览器打开对应 GitHub 章节。指南的 `master` 链接须在合并后核对公开可达；本地文件存在不等于线上正文已发布。详见[文档维护评估](settings-audit.md#6-文档整体维护方案)和[设计研究](design.md#44-文档来源与阅读入口)。

Clear Cache 的删除语义与名称/描述空字符串回退已分别纳入第 18、19 项。设置中的 `hideLeft` 与 `excludeParamTypes` 死字段由第 6 项处理，不另立重复待办。

## 本轮授权边界

用户授权按路线图第 1～7 项逐项完成 OpenSpec 方案、实现和验证，并继续处理独立队列中可推进的任务。各 change 的状态只以对应 tasks 和 verification 为准；当前的自动门禁或局部原生验收不能推导为整条队列已完成。

每项先完成 proposal/design/specs/tasks 再 apply，并同步受影响的用户文档；不为旧功能、旧配置或旧 IDE/JCEF 增加兼容层。缺少账号、凭据或外部环境时，记录具体未验证项，继续不依赖这些条件的任务；跳过不等于通过，相关 task 保持未勾选。不得因此访问真实生产服务、扩大权限或伪造验收证据。

本轮明确排除项：2026-10-22维护者要求“真实上传忽略”，三平台真实上传验收由待办移入非阻断排除记录，不再因缺少测试账号阻断本轮发布评估，也不标为已通过。自动/mock回归及OS凭据存储等独立验收要求保持。

订阅额度已用 100%、剩余 0 或普通额度不可用时停止任务；不使用 Credits、不激活重置权益。维护者已授权本轮完成提交、PR、合并、Release tag、GitHub Release 和 Marketplace 发布，但截至当前这些操作均待执行；不自动 sync/archive 未完成的 OpenSpec change。原生验证使用专用 `doc-demo`，保留其既有改动并恢复临时设置。

## 本轮验证与续作入口

下方 candidate 235 记录是此前阶段的历史证据，不是当前 1.4.0 最终包。当前包的测试数、ZIP/JAR hash、Verifier 结果、未完成的原生验收和发布状态以[1.4.0 发布评估](release-1.4.0.md)为准。此前 candidate 235 的 A/B 凭据隔离、项目重启读回和部分原生交互仍保留为各 change 的增量证据；不能将不同候选包结果合并为同一个包的全量矩阵。

此前 candidate 235 的 ZIP SHA-256 为 `2221a77261d1de30f09c202446cd4565080b1aeff733585a9c44587e496f448d`，生产 JAR SHA-256 为 `62b06ff49d5f18c89f104983bd7dce5e028282134a30fe23c6689bbd481a9f78`；该候选完整门禁为 18 个 Gradle tasks、49 suites / 235 tests，0 failures/errors/skips，IU261 Plugin Verifier 为 Compatible。该段仅作旧候选历史记录。

完整的本轮环境、范围、证据和跳过项集中记录在[本轮验证记录](validation-2026-10-20.md)；此文件由主任务维护。每个功能的长期证据保留在对应 OpenSpec verification。仓库目前没有独立 handoff 文件可清理；无需为清理而删除正式 OpenSpec artifacts、skills 或验证证据。

## 暂缓范围

- 全仓库 AI Wiki、LLM 自动总结/维护、向量存储及独立 Wiki 插件：先验证轻量 API Wiki 的需求，不提前增加第二套解析、存储或生成体系。
- 完整 OpenAPI 规范导入/导出：需要覆盖参数位置、响应码、多媒体类型、components、security、`$ref` 和组合 schema；当前不纳入近期队列。
- 双向同步、多人协作和新生产语言：没有明确差异化需求前暂缓。
- Air 深度集成：保持可选评估，不把 Air、ACP 或 MCP 视为已集成能力。

## 历史归档记录

下列目录名仅说明相应 OpenSpec change 已位于归档目录；本轮没有复核其实现、测试或发布状态，不将归档本身作为本轮验证证据。

- `maintenance-workflow-baseline`（`2026-07-11`）
- `markdown-contract-baseline`（`2026-07-11`）
- `openapi3-operation-summary`（`2026-07-11`）
- `compatibility-deprecation-cleanup`（`2026-07-11`）
- `remove-commons-collections`（`2026-09-16`）

## 横向门禁

- 行为 change 按 [Contract 设计](contract-design.md)定义新版输入、可观察输出、不支持场景和验证方式。新版 Markdown shape 或模板变量可直接替换旧格式，并同步更新现有 golden 与指南；不实现向后兼容、双轨输出或迁移逻辑。正常错误状态、Dumb Mode、取消、持久化和资源释放仍按当前目标 IDE 的 contract 验证；详见[版本演进策略](intellij-compatibility.md#版本演进策略)。
- 触及解析或文档输出的 change 按范围补充真实 PSI fixture 与 Markdown contract 覆盖，关注 Feign、Dubbo、递归、泛型、集合/Map、void 和缺少注释等场景。
- PSI/UI/网络路径遵守[性能指南](performance-guide.md)；IntelliJ API 与平台升级遵守[兼容性策略](intellij-compatibility.md)。
- 发布前执行[发布检查清单](release-checklist.md)。未执行的检查不描述为通过，归档状态不替代当前验证。
