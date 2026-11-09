# Spec Delta

## Purpose

为当前项目中的 Spring、Feign 与 Dubbo API 提供可重复生成、可追溯源码且能被普通 IDE 文本搜索读取的 Markdown 文档集合，同时保护项目中已有的用户文件。

## ADDED Requirements

### Requirement: 项目级文档导出入口与范围
Doc View SHALL 提供明确的项目文档导出动作，将当前项目发现范围内可生成的 Spring、Feign 与 Dubbo API 方法导出到项目内 `docs/doc-view/`。该动作 MUST 与单文档复制、单文档临时预览和现有选中节点导出用途区分。

#### Scenario: 导出当前项目 API
- **WHEN** 用户在项目的 Doc View 工具窗口启动“导出项目文档”
- **THEN** 导出包含当前项目发现范围内可生成文档的接口方法
- **AND** 每个接口方法有独立 Markdown 文件，目录包含一个项目级 `index.md`
- **AND** 单文档复制、预览和临时导出不因此变成项目级导出

#### Scenario: 不支持项目外源码
- **WHEN** 一个候选方法的源码文件不属于当前项目的可定位内容文件，或源码身份无法稳定取得
- **THEN** 该方法不进入项目文档快照
- **AND** 导出结果报告跳过数量或原因，不伪造项目相对源码链接

#### Scenario: 当前快照没有可导出接口
- **WHEN** 当前项目扫描得到空的接口快照
- **THEN** 项目级 `index.md` 更新为空索引
- **AND** 之前生成的接口页面保留在磁盘但不再出现在索引中

#### Scenario: 启用普通接口导出
- **WHEN** 项目设置允许导出普通接口
- **THEN** 导出包含当前项目模块范围内可生成文档的普通接口方法
- **AND** 注解类型不作为普通 API 接口
- **AND** Dubbo 与 Feign 接口仍保留对应来源分类

#### Scenario: 未启用普通接口导出
- **WHEN** 项目设置不允许导出普通接口
- **THEN** 未被 Spring、Feign 或 Dubbo 识别的普通接口不进入快照

### Requirement: 稳定接口身份与相对源码引用
每个导出接口 SHALL 具有由模块身份、项目相对源码路径、全限定类名、方法名和规范化参数类型签名构成的稳定身份；身份 MUST NOT 依赖业务显示名称、重名随机后缀或源码行号。导出文件名 MUST 由该身份确定并避免重载及不同模块之间冲突。

#### Scenario: 重载与跨模块方法身份不同
- **WHEN** 项目包含同名重载方法，或不同模块中存在相同类名与方法签名
- **THEN** 每个接口的稳定身份和输出路径互不相同
- **AND** 对同一源码快照再次导出时每个接口仍映射到同一输出路径

#### Scenario: 源码行号变化
- **WHEN** 方法上方插入代码但类、方法签名、模块和源码路径不变
- **THEN** 方法身份及其导出 Markdown 路径保持不变
- **AND** 文档中的源码链接仍指向对应项目相对源码文件

### Requirement: Markdown 页面与 AI 可读取索引
项目导出 SHALL 为每个 API 方法生成一份普通 UTF-8 Markdown 页面，并生成由稳定身份排序的 `index.md`。每页 MUST 保留当前 Spring/Feign/Dubbo 文档生成结果及项目相对源码引用；索引 MUST 提供稳定身份、接口签名、模块、源码相对链接和导出页面相对链接。

#### Scenario: 检索项目接口目录
- **WHEN** 用户或 AI 读取 `docs/doc-view/index.md`
- **THEN** 索引可逐项定位接口页面及项目中的源码文件
- **AND** 所有链接均相对项目文档目录或项目源码路径，不依赖本机绝对路径

#### Scenario: 使用自定义模板导出
- **WHEN** 项目配置了当前版本的 Spring 或 Dubbo Markdown 模板
- **THEN** 项目页面使用对应模板生成的接口正文
- **AND** 源码引用与 Doc View 所有权元数据不会要求用户更改模板变量

### Requirement: 幂等更新与冲突保护
相同项目快照的重复导出 MUST 替换受管理页面而不追加内容。系统 MUST 验证目标文件由 Doc View 生成且自上次生成后未被修改；遇到未受管理、所有权标记无效或内容已被修改的目标文件时，MUST 在写入前报告冲突并拒绝本次整批导出。系统 MUST 保留输出目录中的无关文件，且不得因重导出删除任何文件。

#### Scenario: 重复导出不累加
- **WHEN** 用户连续两次导出相同源码、设置和模板快照
- **THEN** 每个 API 页面仅有一份当前生成内容而不拼接上一次正文
- **AND** `index.md` 的接口条目不重复，输出文件集合不因同一快照而增长

#### Scenario: 更新未被修改的受管理页面
- **WHEN** 源码、设置或模板改变了同一稳定身份页面的生成内容，且磁盘页面仍与 Doc View 上次生成内容一致
- **THEN** 导出以新内容替换该页面并更新索引

#### Scenario: 同名用户文件或手动修改页冲突
- **WHEN** 任一预定目标已存在但无有效 Doc View 所有权记录，或其内容与最近一次 Doc View 输出不一致
- **THEN** 系统报告冲突路径并在任何成品写入前取消整批导出
- **AND** 冲突文件及其它现有项目文件保持不变

#### Scenario: 源码身份变化后的旧页面
- **WHEN** 源码路径、模块或方法签名变化导致接口身份改变
- **THEN** 新身份生成新的页面并由新索引引用
- **AND** 旧页面不被静默删除，且不再作为当前接口出现在新索引中

### Requirement: 后台快照、文件写入和任务终态
项目导出 SHALL 在可取消后台任务中完成项目 PSI 发现和接口数据快照；磁盘 IO MUST 在 PSI read action 之外执行，且文件写入阶段 MUST NOT 持有 PSI 引用。用户取消或项目关闭 MUST 阻止未开始的写入或丢弃未完成快照；失败和部分发布 MUST 给出有限、可操作的结果，不显示虚假的成功状态。

#### Scenario: 从 PSI 到磁盘的线程边界
- **WHEN** 项目导出开始扫描接口并渲染 Markdown
- **THEN** PSI 读取在后台受保护的 read action 中完成并产生不持有 PSI 的不可变快照
- **AND** 目录扫描及 Markdown 生成不阻塞 EDT
- **AND** 后续文件 IO 在 read action 结束后进行

#### Scenario: 快照或预发布阶段取消
- **WHEN** 用户在 PSI 快照或文件暂存阶段取消导出
- **THEN** 任务丢弃未完成结果并清理临时暂存文件
- **AND** 已有页面与索引保持不变

#### Scenario: 写入开始前项目已关闭
- **WHEN** 项目在项目文档 writer 开始执行前已关闭
- **THEN** writer 在任何输出文件系统变更前取消
- **AND** 不创建 `docs/`、`docs/doc-view/`、`apis/`、索引或临时文件
- **AND** 已有页面与索引保持不变

#### Scenario: 写入失败
- **WHEN** 磁盘写入在发布阶段失败
- **THEN** 索引仅在全部页面发布成功后更新
- **AND** 系统报告失败原因及已完成或未完成的相对路径，不声称整批成功
- **AND** 保留无关文件，重试仍可收敛到当前源码快照

### Requirement: IDEA 常规文本搜索
导出页面与索引 SHALL 使用普通 Markdown 文件，不依赖自定义 IntelliJ 索引、MCP 或外部数据库；系统 MUST NOT 将“文件已生成”描述为 IDEA 原生搜索验收已完成。

#### Scenario: 使用 IDE 文本搜索查找文档
- **WHEN** 用户在 IDEA 项目文本搜索中搜索导出页面中的接口词语
- **THEN** 页面作为项目普通 Markdown 文件参与 IDE 现有文本搜索范围
- **AND** 搜索是否包含该路径遵循用户当前 IDEA 搜索范围与项目文件排除设置

### Requirement: 项目文档发布后同步本地 VFS
项目文档导出 action 成功发布一个或多个文件后 MUST 在后台、PSI read action 之外刷新 `docs/doc-view/` 输出根对应的本地 VFS 内容，使新建目录和 Markdown 文件可被 IDEA 发现。批量发布出现部分失败时，只要至少一个文件已发布也 MUST 刷新。刷新 MUST NOT 执行全项目刷新或创建自定义索引；VFS 可见不代表 IDEA 文本索引已经同步完成。

#### Scenario: 首次导出后 IDEA 发现新文档
- **WHEN** `docs/doc-view/` 尚不存在于当前项目 VFS，且用户成功运行“导出项目文档”
- **THEN** action 完成后，项目 VFS 可定位新生成的 `index.md` 和接口页面，无需用户手动执行全局 Reload from Disk
- **AND** 后续文本搜索仍由 IDEA 现有文件索引、搜索范围和排除规则决定

### Requirement: 既有批量覆盖语义
现有窗口合并导出及目录批量导出在用户明确确认覆盖后 MUST 替换对应目标文件内容而不是追加；用户取消覆盖确认时 MUST 保持目标文件不变。该修正 MUST NOT 将这些导出改成项目级索引或迁移旧输出。

#### Scenario: 确认覆盖既有批量导出
- **WHEN** 用户对窗口合并导出或目录批量导出的现有目标明确确认覆盖
- **THEN** 每个目标文件只包含本次生成的内容
- **AND** 再次确认覆盖不会将相同接口文档追加到文件末尾

#### Scenario: 取消覆盖既有批量导出
- **WHEN** 用户拒绝覆盖确认
- **THEN** 对应已有目标文件保持原字节内容不变

### Requirement: 既有批量导出使用受保护的文档快照
窗口合并导出和目录选中节点批量导出 SHALL 在后台受保护的 PSI read action 中取得并渲染本次文档快照；文件 IO MUST 在 read action 结束后使用不持有 PSI 的数据完成。action `update` 和 EDT 回调 MUST NOT 执行 PSI 文档构建。

#### Scenario: 从目录选择后批量导出
- **WHEN** 用户对 Tool Window 中选中的类、模块或根目录启动批量导出
- **THEN** PSI 读取与 Markdown 渲染在后台 read action 中完成
- **AND** 写盘在 read action 外完成，EDT 不同步遍历 PSI 或渲染接口 Markdown

#### Scenario: 窗口合并导出
- **WHEN** 用户启动窗口级合并导出
- **THEN** 待写入文档从一个后台 read-action 快照生成
- **AND** 后续文件 IO 不访问 PSI，取消或错误遵循有限终态报告
