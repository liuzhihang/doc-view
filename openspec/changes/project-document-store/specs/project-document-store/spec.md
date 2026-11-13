# Spec Delta

## Purpose

为当前 IDEA 项目提供可校验、可分页的 Doc View 结构化文档库，使接口搜索和详情读取复用已生成快照，而不重新扫描源码或解析自定义 Markdown。

存储与查询错误码 MUST 使用 [MCP design](../../../doc-view-mcp-tools/design.md) 定义的大写枚举，不得引入小写别名。存储场景使用 `INDEX_NOT_FOUND`、`UNSUPPORTED_SCHEMA`、`INDEX_INVALID`、`INDEX_INCONSISTENT`、`API_NOT_FOUND`、`CURSOR_STALE`、`SNAPSHOT_CHANGED`、`BUSY`、`CONFLICT`、`SOURCE_CHANGED`、`LIMIT_EXCEEDED`、`CANCELLED`、`TIMEOUT`、`PARTIAL_PUBLICATION` 和 `IO_ERROR`；参数错误使用 `INVALID_ARGUMENT`。

## ADDED Requirements

### Requirement: 项目文档库固定绑定当前项目
系统 SHALL 只从当前项目根目录下的 `docs/doc-view/` 发现和读取项目文档库，且不得在查询时创建目录、生成文档或扫描源码。

#### Scenario: 首次查询时文档库不存在
- **WHEN** 当前项目没有有效的 `docs/doc-view/index.json`
- **THEN** 查询返回 `INDEX_NOT_FOUND`
- **AND** 不创建任何文档目录或文件
- **AND** 不以 PSI、Markdown 搜索或遍历 `apis/` 作为回退

#### Scenario: 两个项目包含相同接口身份
- **WHEN** 两个打开的项目具有相同模块名、FQN、方法签名和项目相对源路径
- **THEN** 每个项目的查询只读取自身项目根目录的文档库
- **AND** 不把 identity id 当作跨项目全局身份
- **AND** 当前项目的同一 identity id 不得解析到另一项目的详情

#### Scenario: 项目打开后只做读取发现
- **WHEN** 项目文档库在项目打开或首次查询时被发现
- **THEN** 系统只读取固定目录中的已提交索引
- **AND** 不创建输出目录、不修复文件、不触发文档生成

### Requirement: 生成文档使用一致的 PSI 输入
项目文档生成 MUST 从已提交到 PSI 的 Java 与 XML `Document` 读取输入，不得自动保存用户文件；无法证明输入一致时 MUST 在发布前失败。

#### Scenario: 当前项目存在未保存 Java 或 XML Document
- **WHEN** 生成开始时项目内容根内存在未保存的 Java 或 XML `Document`
- **THEN** 生成先将其提交到 PSI，并从已提交文本解析，不调用保存
- **AND** `hasUnsavedDocuments` 为 true；该保守标记可包含未被当前接口引用的 Java 文件或不含 Dubbo 服务声明的 XML 文件
- **AND** `source.capturedContentSha256` 对应每个 API Java 来源的确切 Document 文本 UTF-8 字节，不使用磁盘 hash 代替

#### Scenario: 没有未保存的生成输入
- **WHEN** 生成开始时项目内容根内不存在未保存 Java 或 XML `Document`
- **THEN** `hasUnsavedDocuments` 为 false

#### Scenario: 项目文档输出根不是源码输入
- **WHEN** `docs/doc-view/` 下的生成文件变化
- **THEN** 该变化不计入 freshness 输入代际，也不计入 `hasUnsavedDocuments`

### Requirement: 同一解析快照生成可读页面和结构详情
项目文档生成 MUST 从同一份接口解析快照产出 Markdown 页面、机器索引和结构详情；结构详情 MUST 只包含 PSI-free 的文档数据。

#### Scenario: 生成 Spring、Feign、Dubbo 或已启用的普通接口
- **WHEN** 用户生成或更新当前项目文档
- **THEN** 当前已支持的接口解析结果同时用于 `.md` 页面和 `.json` 详情
- **AND** Markdown 模板正文和页面 identity 规则保持既有 contract
- **AND** 详情包含已有解析结果中的参数、请求/响应字段层级和文档说明
- **AND** 启用 includeNormalInterface 时保留普通接口，framework 为 interface，不误标为 Dubbo
- **AND** 不直接序列化 PSI、原始源码全文、IDE 绝对路径或集成凭据
- **AND** `source.path` 相对项目根目录，来源 hash 对应参与本次解析的已提交 Java Document 文本

#### Scenario: 模块扫描发现所有受支持的 Dubbo 接口
- **WHEN** 项目中的接口直接带有 Dubbo FQN 的 `@Service` / `@DubboService`、注解服务实现直接或间接实现该接口，或模块 XML `<dubbo:service interface="…">` 注册该接口
- **THEN** 该接口及其现有 Dubbo 方法进入当前模块的目录和项目文档快照
- **AND** 注解索引可发现跨模块 provider 中的声明，并只按完整注解 FQN 接受 Dubbo 注解
- **AND** XML FQN 在声明该 XML 的模块及其依赖范围内解析，再按接口源码归属模块加入目录
- **AND** 来自 XML、接口注解或注解实现类的重复声明只生成一个接口条目；同名跨模块接口不串入错误模块
- **AND** 扫描遵循平台进度取消，在扫描注解、继承关系或 XML 服务时立即结束
- **AND** 不改变既有 Dubbo 方法、参数和 Markdown 生成规则

#### Scenario: 匿名返回值可生成结构化详情

- **WHEN** 现有解析器返回没有字段名的标量返回值或集合根节点
- **THEN** 详情用空字符串保留该节点的 `name`，保留类型、说明、集合标记和子节点，不捏造属性名称，也不因合法匿名节点拒绝整个项目生成

#### Scenario: 重复生成同一输入
- **WHEN** 源码、设置和模板输入未变化，用户再次生成项目文档
- **THEN** 机器索引与对应详情内容稳定且不会重复添加条目
- **AND** 内容快照身份不因序列化时间或文件更新时间改变

#### Scenario: 文档字段超出模型限制
- **WHEN** 单个接口详情超过单接口字段数或字段树深度上限
- **THEN** 生成操作报告超限原因并且不静默截断字段
- **AND** 不提交不完整的新机器索引

#### Scenario: 捕获输入在最终提交前变化
- **WHEN** 快照捕获后、`index.json` 提交前观察到相关源码、模块、Settings 或模板变化
- **THEN** 系统以 `SOURCE_CHANGED` 停止提交新机器索引
- **AND** 若此前尚未发布任何成品，所有既有文件保持原字节
- **AND** 若此前已有页面或详情发布，则以 `PARTIAL_PUBLICATION` 报告已发布路径并保留旧 `index.json`

### Requirement: Dubbo XML 发现与导航可取消且随插件释放
系统 SHALL 保留当前受支持的 XML 声明形式：名称为 `beans` 的根标签下，直接子标签 `dubbo:service` 的非空 `interface` 属性。缺少或空属性、其它根或其它子标签 SHALL 被忽略，不因不完整编辑抛出空指针。模块目录归属、Java 注解发现、方法解析和 Markdown/JSON 输出 SHALL 保持既有规则。XML 查询及定义导航 SHALL 响应平台取消，导航 consumer 返回 false 后 SHALL 停止处理。

#### Scenario: XML-only 接口与定义导航
- **WHEN** 受支持的 XML 服务声明注册一个 Java 接口
- **THEN** 该接口保持可识别，目录按声明模块依赖解析并归属接口源码模块
- **AND** 定义导航仍返回对应的 XML 服务标签，未保存且已提交到 PSI 的 XML 编辑立即参与下一次识别
- **AND** 不新增命名空间别名、嵌套 beans、外部 XML 解析或自动保存支持

#### Scenario: 使用 XML 功能后卸载插件
- **WHEN** 用户已使用 XML 接口识别、目录、文档生成和定义导航，再关闭 Doc View 窗口并禁用插件
- **THEN** 插件管理的 XML 状态随项目或插件释放，不通过平台静态代理或异常回溯长期保留插件类加载器
- **AND** 精确目标 IDEA 的原生卸载、GC 与日志结果必须独立记录；自动测试或普通窗口关闭不能代替此项

### Requirement: 机器文件版本与完整性必须可验证
系统 MUST 使用版本化 JSON 格式，并将内容快照身份、序列化完整性和源码新鲜度作为不同信息处理。

接口 Markdown 与结构化详情 MUST 使用现有稳定页面文件 stem：`apis/<stem>.md` 对应 `apis/<stem>.json`。索引中的 Markdown/detail 路径 MUST 相对 `docs/doc-view/` 文档根；来源路径 MUST 相对项目根。HTTP 搜索摘要在有值时 MUST 包含 `httpMethod` 与 `httpPath`，且 `httpPath` 是关键词匹配字段。

#### Scenario: 读取有效的当前 schema
- **WHEN** 当前项目根索引和详情的格式、schema、identity 与完整性摘要均有效
- **THEN** 系统按索引记录读取当前快照
- **AND** 序列化完整性失败返回 `INDEX_INVALID`，索引与详情 identity/hash 不一致返回 `INDEX_INCONSISTENT`
- **AND** 不猜测修复

#### Scenario: 读取未知 schema
- **WHEN** `index.json` 或详情 JSON 使用当前实现不支持的 `schemaVersion`
- **THEN** 查询返回 `UNSUPPORTED_SCHEMA`
- **AND** 不把旧 schema 自动改写成当前 schema，也不解析 Markdown 来补字段

#### Scenario: 显式重新生成旧 schema 文档
- **WHEN** 用户显式运行项目文档生成，且旧 JSON 可以证明仍由 Doc View 生成且未被修改
- **THEN** 系统可以从当前源码重新生成当前 schema 的文档库
- **AND** 不通过迁移适配器读取旧详情
- **AND** 任一旧目标无法证明所有权或完整性时以 `CONFLICT` 报告冲突并保留文件

#### Scenario: 区分快照与文件完整性摘要
- **WHEN** 系统写入或读取 JSON 索引/详情
- **THEN** 内容 `snapshotId` 表示一组确定的接口内容及来源指纹
- **AND** JSON envelope（排除 integrity）完整性摘要验证规范化后的序列化内容
- **AND** Markdown 与详情文件原始字节 hash 用于确认对应文件未被外部改写
- **AND** 这些摘要不得互相替代或被描述为源码实时性证明

机器格式字段、规范化算法、错误 envelope 与示例由 [格式说明](../../format.md) 定义；用户可读入口见 [`docs/mcp-format.md`](../../../../../docs/mcp-format.md)。

### Requirement: 项目文档写入保护现有内容
项目文档更新 MUST 在写入前验证所有目标路径及其 Doc View 所有权和既有内容完整性，并拒绝覆盖无法证明未被修改的文件。

#### Scenario: 目标是用户文件或手工编辑文件
- **WHEN** 当前计划写入的任一 Markdown 或 JSON 目标没有有效 Doc View 所有权记录，或内容与文件自身 marker/self-hash 不一致，或 JSON 字节不再满足规定的 JCS+LF 成品形式
- **THEN** 系统以 `CONFLICT` 报告冲突路径并在发布任何成品前拒绝本次更新
- **AND** 用户文件、既有索引及其他目标保持原字节

#### Scenario: JSON 被外部修改后重新生成
- **WHEN** 某个详情 JSON 或 `index.json` 在上次生成后被外部编辑，导致 self-hash、所有权标记或规定的成品字节形式无效
- **THEN** 后续写入以 `CONFLICT` 拒绝把它当作可覆盖的普通缓存
- **AND** 保留该文件

#### Scenario: 旧受管理页面不再属于当前快照
- **WHEN** 接口从当前源码中消失或 identity 改变
- **THEN** 新索引不再列出旧页面
- **AND** 系统不静默删除旧页面或其他目录文件

#### Scenario: 上次部分发布后显式重试
- **WHEN** 页面或详情与旧 index 的 raw-byte hash 不符，但文件自身 Doc View 所有权、目标 identity 和 marker/self-hash 仍有效
- **THEN** writer 允许重新生成并替换这些受管理目标，最终提交完整新 index
- **AND** 旧代详情读取仍拒绝该 hash 不符文件；自身完整性无效的手改文件仍返回 CONFLICT，不放宽覆盖保护

### Requirement: 搜索只使用已发布的项目索引
系统 SHALL 在已加载的项目索引摘要上执行只读搜索，不得在每次查询中运行 PSI、重新解析接口或遍历 Markdown 页面目录。

#### Scenario: 部分发布后的摘要仍可搜索
- **WHEN** index.json 自身有效，但上次 partial 导致个别详情不再匹配旧 index
- **THEN** status/search 不遍历所有详情进行预检，仍可查询已提交的旧摘要并标注快照
- **AND** 实际读取不匹配详情时返回 INDEX_INCONSISTENT，不返回混代详情

#### Scenario: 使用关键词搜索
- **WHEN** 查询包含一至十六个词，且归一化查询长度不超过二百五十六个 Unicode code point
- **THEN** 系统对 query 与可检索字段执行 NFKC 归一化和 `Locale.ROOT` 小写转换
- **AND** 按空白字符切词，每个词都必须命中同一接口记录的至少一个 name、description、qualifiedClass、fullSignature、module、source path、`httpPath` 或 tags 子串
- **AND** 所有词之间为 AND，每词在字段之间为 OR

#### Scenario: 只按模块或框架筛选
- **WHEN** 查询没有非空关键词但包含 `module`、`framework` 或 `httpMethod` filter
- **THEN** 系统允许在当前项目已加载的索引中筛选
- **AND** filter 与条目中的对应规范值精确相等匹配；不对 filter 做模糊匹配或小写归一化
- **AND** 不扫描源码或详情目录

#### Scenario: 空查询和空筛选
- **WHEN** query 为空且未提供任何 filter
- **THEN** 系统允许按稳定顺序浏览当前项目索引，并受分页上限约束

#### Scenario: limit 不合法
- **WHEN** limit 小于一或大于一百
- **THEN** 系统返回 `INVALID_ARGUMENT`
- **AND** 不静默改写 limit 或返回不完整成功页

#### Scenario: 查询超过边界
- **WHEN** query 超过二百五十六个 Unicode code point、超过十六个词，或索引条目/字节数超过上限
- **THEN** 系统返回 `LIMIT_EXCEEDED`，索引格式或完整性损坏时返回 `INDEX_INVALID`
- **AND** 不静默截断 query、索引或搜索结果

### Requirement: 搜索分页稳定且绑定查询快照
搜索结果 MUST 按既有 identity canonical form 稳定排序，使用有上限的分页，并拒绝跨查询或跨快照复用的 cursor。

#### Scenario: 默认分页
- **WHEN** 搜索未指定 limit
- **THEN** 每页默认返回二十条
- **AND** limit MUST 大于零且不得超过一百
- **AND** 响应提供 `snapshotId` 和可选下一页 cursor

#### Scenario: 连续读取下一页
- **WHEN** 客户端使用同一 `snapshotId`、归一化 query 和 filters 继续查询
- **THEN** cursor 从上一页最后一个 identity 之后继续
- **AND** 不重复或遗漏稳定排序中的条目

#### Scenario: cursor 跨快照或查询复用
- **WHEN** cursor 中的 snapshot、query 或 filters 与当前请求不一致
- **THEN** 系统返回 `CURSOR_STALE`
- **AND** 不把不同快照的结果拼成同一分页序列

### Requirement: 详情读取校验身份、路径和文件内容
读取 API 详情 MUST 以当前项目索引 entry 为唯一入口，并验证 identity、相对路径、schema 和对应文件 hash。

#### Scenario: 读取有效接口详情
- **WHEN** 请求指定当前 `index.json` 中存在的接口 identity
- **THEN** 响应返回 module、完整方法签名、参数、响应字段、相对项目根的源码位置、相对 `docs/doc-view/` 的 Markdown/detail 路径、`snapshotId` 和 freshness
- **AND** Markdown 与详情文件使用同一稳定 stem，例如 `apis/<stem>.md` 与 `apis/<stem>.json`
- **AND** 若存在 HTTP 元数据，搜索摘要包含 `httpMethod` 与 `httpPath`；`httpPath` 也参与关键词匹配
- **AND** 若行号存在，必须标记为生成时行号而不作为当前实时位置

#### Scenario: 详情被编辑或缺失
- **WHEN** 当前 root entry 指定的详情缺失、详情字节 hash 不匹配或详情 identity 不匹配
- **THEN** 系统返回 `INDEX_INCONSISTENT`
- **AND** 不返回已修改详情、不退回解析 Markdown、也不扫描 PSI

#### Scenario: 请求的接口身份不在当前索引
- **WHEN** 请求的 API identity 不存在于当前 `index.json`
- **THEN** 系统返回 `API_NOT_FOUND`
- **AND** 不扫描源码、详情目录或 Markdown 来猜测目标

#### Scenario: 详情路径越界或经过符号链接
- **WHEN** 索引中的详情路径不是规范化的项目相对 `docs/doc-view/apis/` 路径，或路径解析会逃逸项目文档根
- **THEN** 系统拒绝读取并返回 `INDEX_INCONSISTENT`
- **AND** 不跟随符号链接访问项目文档根以外的内容

#### Scenario: 成功重读后只清除已恢复资源的完整性错误
- **WHEN** 某个已索引的 Markdown 页面、详情 JSON 或 `index.md` 曾因路径、字节 hash、schema、identity 或 UTF-8 校验失败而被标记，之后文件恢复为索引记录的精确内容并被完整读取验证
- **THEN** 系统只清除该规范相对资源路径对应的完整性错误，并通知状态观察者
- **AND** 其他资源仍有错误时继续报告 `INDEX_INCONSISTENT`；全部错误清除后回到原有的 freshness 状态，不把文件恢复误报为新的 `current`
- **AND** 如果读取开始后该路径出现了更新的失败记录，则成功读取不得清除较新的错误
- **AND** 不扫描其它页面、不重解析 PSI，freshness epoch 不变

### Requirement: freshness 只表达可证明的来源代际
查询响应 MUST 将源码/设置新鲜度与 JSON/页面完整性分开，并明确标记 `current`、`stale` 或 `unknown`。

#### Scenario: 当前会话生成后输入未变化
- **WHEN** 当前 IDE 会话成功提交快照，且已跟踪的源码、模块根、模板和输出相关设置代际未变化
- **THEN** 查询 freshness 为 `current`
- **AND** 系统只表示“没有观察到输入变化”，不声称源码绝对实时

#### Scenario: 当前会话观察到来源变化
- **WHEN** 快照提交后系统观察到项目内容根内 Java/XML 输入、模块根、模板或设置代际变化
- **THEN** 查询将可完整读取的旧快照标记为 `stale`
- **AND** 不因查询触发自动重生成

#### Scenario: 外部编辑通过 VFS 刷新进入当前会话
- **WHEN** 外部程序修改项目内容根内的 Java/XML 输入，IDEA 的 VFS 刷新已观察到变更，即使该文件没有已加载的 PSI 或 Document
- **THEN** 旧快照标记为 `stale`，只读 status/search/get 不重新生成文件
- **AND** 事件仅影响所属项目；其他项目、非 Java/XML 文件与 `docs/doc-view/` 下的输出不推进该项目输入代际
- **AND** 不将尚未被 IDEA 观察到的磁盘变更宣称为已检测；监听器随项目服务释放

#### Scenario: Dubbo XML 注册变更
- **WHEN** 当前会话已提交项目快照后，项目任一内容根内的 XML Document 发生变化
- **THEN** 旧快照标记为 `stale`，因为该 XML 可能参与 Dubbo 服务声明发现
- **AND** 不自动保存 XML，也不因查询触发重生成

#### Scenario: 从磁盘加载旧快照
- **WHEN** 项目重开后从 `index.json` 加载上一个 IDE 进程生成的快照
- **THEN** freshness 为 `unknown`
- **AND** 不扫描 PSI 来证明磁盘快照的新鲜度

### Requirement: 项目查询与发布并发时 fail closed
同一项目的查询和文件发布 MUST 保持可观察的一致性；同一文档根的并发 writer MUST 被拒绝，外部修改必须由完整性检查发现或阻断。

获取文档根写锁 MUST 在后台以受期限和取消约束的方式执行，不得让 EDT 无限等待。锁不可用或等待超限时 MUST 返回 `BUSY`。

#### Scenario: 同一文档根已有 writer
- **WHEN** 另一个线程、进程或 IDE 实例已持有当前项目 `docs/doc-view/` 的发布权
- **THEN** 新 writer 返回 `BUSY`
- **AND** 不覆盖任何索引、详情或 Markdown 文件

#### Scenario: 查询遇到本地发布
- **WHEN** writer 正在替换一批文档文件
- **THEN** 同项目查询不会读取中间替换状态
- **AND** 发布终止于部分结果后，对 root index 中 hash 不匹配的详情返回 `INDEX_INCONSISTENT`

#### Scenario: 外部修改索引或详情
- **WHEN** 磁盘索引或详情在加载后被外部进程改动
- **THEN** 系统使受影响缓存失效并重新验证文件完整性
- **AND** 无法验证时拒绝返回该索引或详情，不把缓存命中当作完整性证明

### Requirement: 批量发布支持逐文件取消和明确提交点
项目文档发布 MUST 在每个单文件原子替换前后检查取消及项目生命周期，并以 `index.json` 的最终原子替换作为机器快照提交点。

#### Scenario: snapshot、预检或暂存阶段取消
- **WHEN** 用户在生成、预检或完整暂存完成前取消任务
- **THEN** 系统清理本次临时文件并保持所有既有成品字节不变
- **AND** 不提交新缓存或部分 snapshot
- **AND** 任务以 `CANCELLED` 结束

#### Scenario: 普通页面替换后取消
- **WHEN** 一个 API 页面或详情文件的 atomic move 已完成后观察到取消
- **THEN** 当前 move 保留，系统停止后续 move 并清理剩余临时文件
- **AND** 旧 `index.json` 保持不变并作为未提交 snapshot 指针
- **AND** 系统报告已发布相对路径及 partial 状态，不把部分取消静默吞掉
- **AND** 任务以 `PARTIAL_PUBLICATION` 结束

#### Scenario: 人读 index 已更新但机器 index 尚未提交
- **WHEN** 新 `index.md` 已替换而最终 `index.json` 尚未替换时任务取消或失败
- **THEN** 机器查询继续以旧 `index.json` 为目录
- **AND** hash 不匹配的旧详情读取 fail closed
- **AND** 导出状态明确为 partial
- **AND** 任务以 `PARTIAL_PUBLICATION` 结束

#### Scenario: 取消在最终 index 原子替换期间到达
- **WHEN** final `index.json` move 已开始且取消在该单次 move 期间到达
- **THEN** writer 允许该 move 完成，并将已替换的 index 视为提交
- **AND** project 仍存活时立即同步安装同一 immutable snapshot 到内存服务
- **AND** 不回滚已提交快照或将任务报告为取消成功

#### Scenario: 项目在最终 index move 前关闭
- **WHEN** project dispose 在最终 `index.json` move 开始前被观察到
- **THEN** writer 停止后续发布，保留旧 `index.json` 并报告已有 partial 文件
- **AND** 不在已 dispose project 上投递 UI 回调
- **AND** 若此前已有文件发布则以 `PARTIAL_PUBLICATION` 结束，否则以 `CANCELLED` 结束

#### Scenario: 项目在最终 index move 期间关闭
- **WHEN** project dispose 在最终 atomic move 已开始后发生，且该 move 成功完成
- **THEN** 已替换的 `index.json` 作为已提交磁盘快照保留
- **AND** 不调用已 dispose 的 project service 或 UI，项目下次打开后将快照标记为 `unknown`

#### Scenario: 发布中的文件系统错误
- **WHEN** 任一成品替换在最终 `index.json` 提交前失败
- **THEN** 系统报告已发布和失败的相对路径，保留旧机器索引并清理剩余临时文件
- **AND** 详情 hash 不匹配时拒绝读取，重试完整生成后可收敛
- **AND** 有已发布文件时以 `PARTIAL_PUBLICATION` 结束；尚无文件发布时以 `IO_ERROR` 结束

### Requirement: 持久化和查询受资源上限约束
系统 MUST 限制索引、详情、字段树、搜索请求和单次操作时间；超过上限时显式失败，不静默截断或返回不完整 snapshot。

#### Scenario: 生成达到存储容量上限
- **WHEN** 索引超过 32 MiB、接口条目超过 10,000、单份 `.md` 或详情 JSON 超过 1 MiB、字段树深度超过 32 或单接口字段节点超过 10,000
- **THEN** 系统在最终索引提交前报告 `LIMIT_EXCEEDED` 和对应限制原因
- **AND** 不截断条目/字段，不替换旧 `index.json`

#### Scenario: 查询操作超时或取消
- **WHEN** 单次查询超过五秒预算或用户取消查询
- **THEN** 系统停止查询并返回 `TIMEOUT` 或 `CANCELLED` 终态
- **AND** 不返回被截断为成功的部分结果
- **AND** 读取索引时，UTF-8 解码、JSON 解析和完整性校验都必须响应同一个查询期限及取消状态
- **AND** 取消或超时不得被包装成 `INDEX_INVALID`
- **AND** 项目文档 UI 以当前界面语言显示通用的“项目文档操作已取消”提示，不将搜索或打开操作误称为状态检查

#### Scenario: 性能基线验收
- **WHEN** 使用记录了机器、IDE build、JDK 和 fixture 大小的 10,000-entry 基准环境测量
- **THEN** 温索引搜索 p95 目标不超过 200 ms，冷索引载入目标不超过 2 s，查询取消响应 p95 目标不超过 500 ms
- **AND** 实际采样、分位数和偏差必须如实记录；未测或未达目标不得标记通过

#### Scenario: 项目生成超过整体预算
- **WHEN** 从 PSI 快照开始到最终机器索引提交超过 120 秒预算
- **THEN** 任务请求取消，并按当前 move 边界/最终索引提交规则报告 `CANCELLED`、`PARTIAL_PUBLICATION` 或已提交完成

### Requirement: 旧页面和非目标搜索能力保持边界
结构化项目文档库 MUST 只暴露最近一次有效索引列出的当前接口，不自动清理过期文件，也不依赖专用 IDE 索引、语义检索或外部数据库。

#### Scenario: 磁盘保留过期 API 页面
- **WHEN** `apis/` 中存在旧索引未引用的 Doc View 页面或无关用户文件
- **THEN** 搜索和详情只读取当前 `index.json` entries
- **AND** 不递归扫描目录或删除这些文件

#### Scenario: 检索相近表达或未生成源码文档
- **WHEN** 查询需要语义相似、跨项目合并或当前 JSON snapshot 未包含的源码接口
- **THEN** 系统明确表示不支持或无匹配
- **AND** 不建立 FileBasedIndex、StubIndex、向量库、SQLite 或隐式 PSI 回退来补齐结果
