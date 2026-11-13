# Spec Delta

## Purpose

让外部 MCP 客户端通过 IDEA 自带服务调用 Doc View 的同一套项目文档服务，可靠地区分落盘查询、显式生成、项目作用域和任务取消，并避免把隔离探针或旧快照描述为生产实时能力。

## ADDED Requirements

### Requirement: 同一插件内的可选工具注册
系统 SHALL 将正式工具作为 Doc View 同一安装包的可选 MCP 扩展注册到 IDEA `com.intellij.mcpServer`；MUST NOT 自建 MCP server、要求安装第二个 Doc View 插件或自动修改客户端配置。

#### Scenario: IDEA MCP 组件可用
- **WHEN** Doc View 与受支持的 IDEA MCP 组件启用，用户已启用服务
- **THEN** tools/list 能发现规定的 Doc View 工具及其输入 schema
- **AND** 连接地址、传输和用户访问确认由 IDEA MCP Server 管理

#### Scenario: MCP 组件缺失或禁用
- **WHEN** MCP 组件缺失或被禁用
- **THEN** Doc View 普通文档 UI 可加载并继续工作
- **AND** 不加载依赖 MCP 类型的工具实现，且不创建备用服务

### Requirement: 明确项目与文档根
每个工具 SHALL 要求明确的项目绑定（显式 `projectPath`，或 IDEA 已解析的调用项目上下文），只绑定该已打开 IDEA 项目的固定 `docs/doc-view/`；MUST 拒绝模糊项目、已关闭项目及任意输出目录参数。API 身份 SHALL 在所选项目内解释，不能跨项目搜索或回退。

#### Scenario: 两项目具有相同接口身份
- **WHEN** A、B 项目存在相同模块、源码路径、类和方法签名
- **THEN** A 的查询仅返回 A 的文档根和快照
- **AND** 请求 B 时不会复用 A 的缓存或写入 A 的文件

#### Scenario: 项目关闭或路径无效
- **WHEN** 指定项目不再打开或路径无法唯一匹配
- **THEN** Doc View 已接收到的调用返回 `PROJECT_NOT_OPEN` 或 `INVALID_ARGUMENT`；若 IDEA 在路由到工具前拒绝，则保留宿主的 MCP 错误
- **AND** 不以当前焦点窗口或同名项目代替

### Requirement: 只读查询使用磁盘快照
`doc_view_status`、`doc_view_search`、`doc_view_get` SHALL 复用文档库只读服务，MUST NOT 生成文件、更新索引、保存源码、重扫 PSI 或自动启动生成任务。返回内容 SHALL 包含所读快照的身份和新鲜度状态。

#### Scenario: 尚未生成结构化文档库
- **WHEN** 项目只存在 Markdown 或文档目录不存在
- **THEN** status 返回 `not_generated`，search/get 返回 `INDEX_NOT_FOUND`
- **AND** 提示可显式执行生成工具，文件系统保持不变

#### Scenario: IDE 正在索引
- **WHEN** IDEA 处于 Dumb Mode 而磁盘文档库有效
- **THEN** 搜索与详情继续读取已有快照并标注新鲜度
- **AND** 生成请求返回 `INDEXING`，不等待无期限 PSI 扫描

#### Scenario: 外部源码修改已经被 IDEA 刷新
- **WHEN** 当前会话生成后，IDEA VFS 已观察到所属项目 Java/XML 输入的外部修改
- **THEN** status/search/get 保持读取同一旧快照并报告 `stale`
- **AND** 不因 Skill 查询自动调用 generate；用户明确更新后，成功提交的新快照可恢复为 `current`

### Requirement: 搜索与详情契约
搜索 SHALL 遵循文档库的确定性关键字、过滤、分页和大小约束；结果包含模块、框架、完整签名、源码位置和文档路径。详情 SHALL 接收 API ID 与搜索返回的 snapshotId，返回同快照结构化字段及可选 Markdown，MUST 校验文件 hash 并拒绝混合版本。

#### Scenario: 分页期间快照更新
- **WHEN** 客户端使用旧 cursor 或旧 snapshotId 读取新一代索引
- **THEN** 返回 `CURSOR_STALE` 或 `SNAPSHOT_CHANGED`
- **AND** 不静默返回另一代的条目

#### Scenario: 详情文件被手改或部分发布
- **WHEN** Markdown/JSON 的内容摘要与索引记录不符
- **THEN** 返回 `INDEX_INCONSISTENT` 和安全的相对路径
- **AND** 不反解析 Markdown、拼接旧索引与新详情或执行隐式修复

### Requirement: 生成与更新共用显式写工具
`doc_view_generate` SHALL 复用界面的生成/更新服务，扫描指定项目支持的全部模块并维护 Markdown、索引和详情。MUST 保留手改冲突、路径边界、旧页不删、同项目单写者和有限发布规则，不提供强制覆盖或隐式上传参数。

#### Scenario: 首次生成及重复更新
- **WHEN** 用户明确请求生成，客户端调用写工具且宿主授权允许
- **THEN** 在固定项目文档根生成或更新同一格式，成功返回 generated/updated/unchanged 和接口数量；部分发布返回 PARTIAL_PUBLICATION 错误及已发布路径
- **AND** 相同输入重复执行不追加重复页面，无变化时不为更新时间戳重写产物

#### Scenario: 冲突或并发写入
- **WHEN** 发现未受管理/手改文件，或另一生成者占用同一根
- **THEN** 返回 `CONFLICT` 或 `BUSY` 并保留成品
- **AND** 不绕过冲突保护或并行覆盖同一目录

### Requirement: 版本化 MCP 输出
工具 SHALL 按 design 中的 JSON 契约返回版本化对象，而非直接序列化包含 PSI 或父引用的生产 DTO。工具参数错误 SHALL 使用 `INVALID_ARGUMENT` 与其他业务错误区分；外层 JSON-RPC 格式错误由宿主处理；业务失败 MUST NOT 返回成功正文。协议包装与磁盘索引 schema MUST 分开维护。

#### Scenario: 工具结果可解析
- **WHEN** 客户端获得由 Doc View handler 返回的工具结果
- **THEN** MCP `content` 中的单个 Text 块为可按约定解析的 JSON，包含 schemaVersion、status、projectPath、snapshotId、freshness、hasUnsavedDocuments 和 data
- **AND** 字段与手册、schema 和工具测试一致，不依赖任意 Java record 自动序列化
- **AND** 调度前的宿主拒绝、协议错误或客户端取消可能不经过 handler，客户端不得把其文本错误强行当成 Doc View JSON envelope

#### Scenario: 诊断路径数量超过展示上限
- **WHEN** 生成或 partial 结果包含超过100个路径
- **THEN** 每组路径最多返回确定顺序的前100条，并返回实际 *Total；客户端可明确计算省略数量
- **AND** 路径统一相对文档根，不重复在错误位置和已发布清单中列同一批文件，本地服务保留完整结果
- **AND** 文档字段/正文不使用此规则静默截断，仍受 RESULT_TOO_LARGE 约束

#### Scenario: 拒绝超限响应
- **WHEN** 详情 JSON/Markdown 或总结果超过公开上限
- **THEN** 返回 `RESULT_TOO_LARGE`，不静默截断字段或返回不完整 JSON

### Requirement: 取消与生命周期必须可观测
系统 SHALL 将平台取消传播至查询、PSI 遍历与发布边界，限制每个项目最多一个生成任务与四个查询任务，并分别设置 5 秒与 120 秒服务端期限。项目关闭或插件所属 IDE 正常退出/重启 MUST 终止所属任务并释放资源；本版 require-restart，不承诺原位扩展热卸载；超时、退出和 turn 中断 MUST NOT 相互冒充。

#### Scenario: 提交前收到真正取消
- **WHEN** 客户端取消信号到达仍在执行、尚未提交最终 index.json 的调用
- **THEN** 停止后续计算和成品发布，清理暂存及运行对象且只终结一次
- **AND** 已经替换文件则按文档库 partial cancellation 规则记录，不发送迟到成功结果

#### Scenario: 提交后收到迟到取消
- **WHEN** 最终 index.json 已原子提交，随后才收到取消
- **THEN** 不回滚已提交文件，也不将已提交操作宣称为成功取消
- **AND** 后续 status 反映实际提交快照，重复清理不重复执行

#### Scenario: 客户端没有传递取消
- **WHEN** 客户端回合停止但没有发出协商协议要求的取消信号
- **THEN** 不能声称服务端已由客户端取消
- **AND** 服务端仍以期限和生命周期限制资源，验收保持失败直到客户端复验通过

### Requirement: 生产准入与发布证据
正式工具实现 SHALL 基于已通过的目标 Codex 客户端取消准入、已发布精确 IDEA/Java 基线及文档库 contract。历史非 MCP 完整原生矩阵按维护者批准保留待验，不再阻止本轮 adapter 开发；MCP 自身的实际调用、取消/恢复和项目隔离必须单独验证。只读工具通过真实调用后才能开放生成工具；MUST NOT 以 schema 反射、源码补丁存在、官方 SDK 对照或文档校验替代目标客户端验收。

#### Scenario: 当前取消仍失败
- **WHEN** 当前 Codex/IDEA 组合仍未证明在途取消和同会话恢复
- **THEN** 正式工具运行时实现保持 blocked，计划与不依赖 MCP 的文档库可继续
- **AND** 保留生产最低版本、旧失败记录与 HTTP/UI 未验项

### Requirement: 宿主绑定与参数边界
系统 SHALL 在宿主会话已移除 `projectPath` 时，仅使用公开的已解析调用项目上下文补齐规范路径；显式参数与宿主项目不同 MUST 拒绝。公开 SDK 无法表达的额外字段约束 SHALL 在 adapter 内严格执行，不依赖客户端自律。

#### Scenario: 无法解析错误请求的项目
- **WHEN** projectPath 缺失、类型错误或超过输入上限，且没有有效的宿主绑定
- **THEN** 返回 INVALID_ARGUMENT 错误，projectPath 为 null
- **AND** 不猜测项目，不返回空路径或回显超长参数
