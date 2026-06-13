# Roadmap

本文档给出 Doc View 后续产品化开发顺序。路线图的核心目标是把 Doc View 从
“从 Java 源码生成 Markdown 的工具”逐步推进为“可浏览、可配置、可导入导出、可上传的
API 文档工作台”。

路线图用于拆分 Speckit feature、确定优先级和定义完成信号；不代表所有事项都会一次性实现。

## 当前产品目标

Doc View 的近期主线是优化文档体验，而不是单点堆叠能力：

- 生成的 Markdown 文档结构稳定、清晰、可验证。
- 大项目扫描、预览、刷新、导出和上传不阻塞 IDE。
- 右侧目录树成为项目 API 文档导航入口。
- 文档结构、字段展示、模板和导出行为可配置。
- 导入、导出和上传围绕同一份 `DocView` contract 工作。

## 维护原则

- 先定义 contract，再修改 Java。
- 先稳定 Markdown 文档结构，再扩展目录树、配置、导出和上传。
- 先解决可验证的性能瓶颈，再做批量能力。
- 每个 Speckit feature 应能独立设计、实现、验证和回滚。
- 生产插件代码保持 Java-only。
- 文档-only 变更不得夹带 runtime 行为变化。
- 用户可见行为变化必须更新验证路径，发布前补充 changelog。

## Speckit 开发节奏

每个较大的 feature 默认按以下顺序推进：

```text
speckit-specify
speckit-clarify
speckit-checklist
speckit-plan
speckit-tasks
speckit-analyze
speckit-implement
```

如果执行中发现问题，应回到对应阶段修正：

- 需求不清：回到 `speckit-clarify`。
- 技术方案不对：回到 `speckit-plan`。
- 任务拆分不对：回到 `speckit-tasks`。
- spec、plan、tasks 不一致：先跑 `speckit-analyze` 并修正，再实现。
- 只是实现细节问题：继续 `speckit-implement`。

`speckit-constitution` 只在项目治理规则变化时使用；`speckit-taskstoissues` 只在需要把任务同步到 GitHub Issues 时使用。

## 推荐开发顺序

| 顺序 | Speckit feature | 目标 | 建议版本 |
| --- | --- | --- | --- |
| 0 | release-readiness-baseline | 维护文档、发布流程和 2026.1+ 兼容基线 | 1.3.x |
| 1 | markdown-contract-baseline | 固定 Markdown 文档结构、`DocView` 输出和黄金样例 | 1.4.0 |
| 2 | doc-generation-performance | 优化 PSI 扫描、Markdown 渲染、缓存和后台任务 | 1.4.x |
| 3 | api-tree-navigation | 强化右侧目录树的分组、搜索、刷新、点击预览和批量入口 | 1.5.0 |
| 4 | doc-view-configuration | 统一文档结构、字段展示、模板 profile 和配置迁移 | 1.5.x |
| 5 | doc-export-workspace | 支持单接口、Controller、模块、项目级文档导出 | 1.6.0 |
| 6 | doc-import-viewer | 导入历史 Markdown/ZIP 作为文档库查看、对比和归档 | 1.6.x |
| 7 | platform-upload-stability | 稳定 YApi、ShowDoc、YuQue payload 和上传错误处理 | 1.7.0 |
| 8 | compatibility-deprecation-cleanup | 清理 deprecated API，维持 2026.1+ 兼容验证 | 可穿插 |
| 9 | mcp-design-prototype | 评估 MCP 辅助 contract、样例生成和发布预检 | 暂缓 |

## M0：发布与维护基线

目标：让项目具备可持续维护和发布能力。

已完成方向：

- 明确 Java 21、Gradle 9.5.0、IntelliJ IDEA 2026.1+ 的当前基线。
- 移除 1.3.12 中 Marketplace 阻塞的 internal API usage。
- 建立 docs、release checklist、compatibility contract 和 repo-local skill。

后续仅在发布流程变化时维护。

完成信号：

- `AGENTS.md`、`docs/` 和 release checklist 能指导一次完整发布。
- `./gradlew buildPlugin` 和 `./gradlew verifyPlugin` 是发布前固定验证路径。
- Marketplace token、平台 token 和私密地址不进入仓库。

## M1：Markdown Contract 基线

Speckit feature：`markdown-contract-baseline`

目标：先定义“好文档应该长什么样”，再实现模板或解析变更。

范围：

- 为 Spring Controller、Feign 风格接口、Dubbo Service 建立典型 Java 输入样例。
- 定义预期 `DocView`、`Body`、`Param`、`Header` 输出。
- 定义 Markdown 标题、接口摘要、Header、Path、Query/Form、Body、Response、示例 JSON 的顺序和展示规则。
- 定义无参数、无响应体、void 返回、缺少注释、枚举、递归对象和空字段的展示规则。
- 建立 golden Markdown 样例，作为后续模板和解析修改的回归基线。
- 明确用户自定义 Velocity 模板变量的兼容边界。

非目标：

- 不做大范围 PSI 解析修复。
- 不重做右侧目录树。
- 不改变上传、导出、设置持久化行为。

完成信号：

- 常见 Java 输入样例有对应 `DocView` contract。
- Markdown golden sample 能说明当前和目标输出。
- 修改 Markdown 模板前能知道哪些输出是稳定 contract。

验证建议：

- golden Markdown 对比。
- 最小 DTO 构造或 fixture 测试。
- `./gradlew test`。
- 必要时在 `runIde` 中手动验证 preview、复制和导出。

## M2：文档生成性能

Speckit feature：`doc-generation-performance`

目标：让扫描、预览、刷新、导出和上传准备阶段不阻塞 IDE。

范围：

- 明确 PSI read action、background task、EDT 更新的边界。
- 优化右侧目录树刷新和接口扫描的任务模型。
- 建立 `DocView` 或 schema 缓存的失效规则。
- 优化 Markdown 渲染缓存，避免重复 Velocity 渲染。
- 支持取消、重试、进度提示和项目关闭防御。
- 记录大项目扫描的性能验证样例。

非目标：

- 不改变 Markdown 文档结构 contract。
- 不新增目录树产品能力。
- 不改变平台上传 payload。

完成信号：

- 常用操作不会在 EDT 上执行重 PSI 或重渲染逻辑。
- 重复预览同一接口时能复用已计算结果。
- 项目关闭、文件失效、Dumb Mode 等状态不会产生未处理异常。

验证建议：

- `./gradlew test`。
- `./gradlew runIde` 手动验证大文件、大 Controller、刷新、预览、导出和上传入口。
- 记录至少一个性能对比或可复现手动场景。

## M3：右侧目录树导航

Speckit feature：`api-tree-navigation`

目标：让右侧 tool window 成为 API 文档的主导航入口。

范围：

- 支持按模块、Controller、path、框架类型或 tag 分组。
- 支持搜索、过滤、刷新、展开/折叠、清理缓存。
- 保留或恢复目录树展开状态和选中状态。
- 点击节点直接预览对应 Markdown。
- 定义空状态、扫描中、扫描失败、无可识别接口等 UI 状态。
- 为批量导出和批量上传预留入口，但不在本阶段实现完整批量流程。

非目标：

- 不改变文档内容 contract。
- 不实现导入历史文档。
- 不重做平台上传协议。

完成信号：

- 用户可以从右侧目录树完成“发现接口 -> 预览文档”的主流程。
- 目录树状态在刷新后尽量稳定，不产生明显跳动。
- 失败和空状态有可理解反馈。

验证建议：

- `./gradlew runIde` 手动验证 Spring、Feign、Dubbo 项目。
- 验证有项目、无项目、Dumb Mode、项目关闭和文件删除场景。

## M4：文档配置体系

Speckit feature：`doc-view-configuration`

目标：让文档结构和字段展示规则可控，而不是散落在模板和解析逻辑中。

范围：

- 定义文档结构配置：是否显示摘要、Headers、示例 JSON、空字段、枚举说明。
- 定义字段展示配置：字段过滤、必填标记、递归深度、示例值策略。
- 定义模板 profile：默认模板、自定义模板、团队模板的边界。
- 定义旧配置迁移和默认值策略。
- 明确 settings UI 的保存、取消、重置和项目级隔离行为。

非目标：

- 不改变目录树导航能力。
- 不实现完整导入导出工作台。
- 不把配置写入生成 Markdown 之外的外部平台。

完成信号：

- 每个配置项都有默认值、持久化字段和对 Markdown 的影响说明。
- 旧版本配置能平滑读取。
- 配置变化后 preview、复制和导出行为一致。

验证建议：

- 设置持久化手动验证。
- Markdown golden sample 覆盖关键配置组合。
- `./gradlew test` 和 `./gradlew runIde`。

## M5：文档导出工作台

Speckit feature：`doc-export-workspace`

目标：把导出从单点能力扩展为可选择范围、格式和命名规则的工作流。

范围：

- 支持单接口、Controller、模块、项目级导出范围。
- 支持 Markdown、HTML、ZIP 等格式的 contract。
- 定义导出目录结构、文件命名、重复文件处理和失败回滚策略。
- 支持导出前预览或摘要确认。
- 记录导出大批量文档时的进度和取消策略。

非目标：

- 不实现导入查看。
- 不改变平台上传逻辑。
- 不引入新的生产语言或外部运行时。

完成信号：

- 用户能明确选择导出范围和格式。
- 导出的目录结构稳定、可重复、可归档。
- 导出失败时不会留下难以理解的半成品状态。

验证建议：

- 构造多 Controller、多模块样例。
- 手动验证不同导出范围和重复导出。
- `./gradlew runIde`。

## M6：文档导入与查看

Speckit feature：`doc-import-viewer`

目标：让历史导出的 Markdown 或 ZIP 能回到 Doc View 中查看、归档或对比。

范围：

- 定义支持的导入格式和目录结构。
- 导入历史导出结果并在独立文档库视图中查看。
- 支持导入文档与当前项目扫描结果的基础对比。
- 明确导入文档不参与 PSI 解析、不写回源码、不上传敏感配置。
- 定义损坏文件、格式不匹配和版本不兼容的反馈。

非目标：

- 不把任意 Markdown 自动反解析成 `DocView`。
- 不做复杂双向同步。
- 不覆盖当前项目源码生成结果。

完成信号：

- 用户能查看历史文档，不需要重新打开旧项目。
- 导入失败原因可理解。
- 导入数据和当前项目扫描数据边界清晰。

## M7：平台上传稳定性

Speckit feature：`platform-upload-stability`

目标：让 YApi、ShowDoc、YuQue 上传更可验证、更安全、更易排障。

范围：

- 建立每个平台的 payload contract。
- 定义上传前预检：配置缺失、token 缺失、目标项目或目录缺失。
- 统一 HTTP 超时、错误处理、重试边界和用户通知。
- 避免 token、生产地址和敏感请求体进入日志。
- 定义上传成功、失败、部分失败和取消的 UI 反馈。

非目标：

- 不改变 Markdown 文档主 contract。
- 不实现跨平台双向同步。
- 不在仓库中保存真实 token 或生产地址。

完成信号：

- 每个平台有最小验证流程和非生产测试建议。
- 失败信息能指导用户修复配置或网络问题。
- 平台 payload 变更可通过 contract 回归验证。

## M8：兼容性和 Deprecated API 清理

Speckit feature：`compatibility-deprecation-cleanup`

目标：降低后续 IntelliJ 平台升级风险。

范围：

- 清理 Plugin Verifier 报告中的 deprecated API usage。
- 优先处理 `JavaAnnotationIndex.get(...)`、`UiCompatibleDataProvider.getData(String)`、`TreeSpeedSearch(JTree)` 等告警。
- 每个替换都要确认目标 API 的最低 IntelliJ 版本。
- 保持 `platformVersion=2026.1`、`pluginSinceBuild=261` 的兼容策略。

非目标：

- 不为了清理告警改变解析结果。
- 不重新支持 2025.x 或更早 IDE。
- 不把 deprecated cleanup 和大型产品功能混在一个 PR。

完成信号：

- `./gradlew verifyPlugin` 不再报告目标 deprecated API，或剩余项有明确跟踪说明。
- 清理前后的 Spring、Feign、Dubbo 扫描 contract 不回退。

## M9：MCP 探索

Speckit feature：`mcp-design-prototype`

目标：评估 MCP 是否能辅助 contract 查询、样例生成、文档同步或发布预检。

原则：

- 先设计能力边界，不直接引入插件 runtime 依赖。
- 原型必须和插件安装包解耦。
- 必须明确认证、文件访问、网络访问和用户确认策略。
- 不影响当前插件性能和用户 IDE 稳定性。

该阶段暂缓，等 M1 到 M5 的文档主线稳定后再评估。

## 横向验证门禁

按变更类型选择最小但足够的验证：

- Markdown contract 或 DTO 变化：golden sample、`./gradlew test`。
- PSI 解析变化：Java 输入样例、预期 `DocView`、`./gradlew test`。
- UI 和目录树变化：`./gradlew runIde` 手动烟测。
- 导出变化：导出目录结构和文件内容检查。
- 上传变化：非生产平台或 mock endpoint 手动验证。
- 兼容性变化：`./gradlew verifyPlugin`。
- 文档-only 变化：`git diff --name-only` 和 `git diff --check`。

不要把未运行的验证写成已通过。

## 暂缓事项

以下方向有价值，但不应抢在文档主线之前：

- AI 自动生成接口描述。
- Markdown 到 `DocView` 的完整反解析。
- YApi、ShowDoc、YuQue 双向同步。
- 多人协作和远端文档冲突解决。
- 新增非 Java runtime 语言或外部服务依赖。
