# 架构

本文描述 Doc View 当前模块边界和核心数据流。实现与版本事实以源码、`build.gradle`、`gradle.properties` 和 `plugin.xml` 为准；后续方案单独标明并由 OpenSpec 定义，不表示已实现。

## 运行边界

- 生产实现保持 Java-only。
- 插件运行在 IntelliJ IDEA Ultimate，依赖 Java、XML 与 Markdown bundled plugin；XML 为 Dubbo 服务声明与导航提供公开 PSI API，本地欢迎与更新页面由 IDE JCEF 呈现。
- PSI 解析、DTO 组装、Markdown 渲染、导出和上传共享同一份 `DocView` 数据模型。
- 新的运行时依赖、extension、生产语言或平台基线必须由独立 OpenSpec change 批准。

## 包职责

| 包 | 职责 |
| --- | --- |
| `action` | 编辑器、预览、上传和 tool window 动作入口 |
| `config` | 项目/应用设置、持久化状态和 Configurable |
| `constant` | Spring、Dubbo、Swagger、Validation 等注解和类型常量 |
| `dom` | Dubbo XML PSI 声明读取与 scoped search，不注册自有 DOM model |
| `dto` | `DocView`、`Body`、`Param`、`Header` 等文档模型 |
| `integration` | YApi、ShowDoc、YuQue facade 和平台 DTO |
| `provider` | line marker 和 IDE provider |
| `service` | Spring/Dubbo 文档构建、写入和上传服务 |
| `service/export` | 项目接口快照、稳定身份、共享生成服务及受管理 Markdown/JSON 文件发布 |
| `service/document` | PSI-free 结构化详情、格式 v1、项目索引查询、缓存与读写协调 |
| `service/mcp`、`integration/mcp` | 开发候选的共享文档工具契约、可选IDEA MCP provider与取消/项目边界 |
| `service/skill` | 开发候选的固定发行源、范围解析、校验、安装metadata、可恢复文件事务与Codex发现 |
| `ui` | Swing form、预览、参数编辑和 tool window |
| `utils` | PSI、Velocity、HTTP、导出、文件和对话框工具 |

## 核心数据流

```text
用户动作 / line marker / tool window
                 │
                 ▼
     Spring 或 Dubbo DocViewService
                 │
                 ▼
   PSI/Javadoc/annotation/type analysis
                 │
                 ▼
 DocView + Body/Param/Header + example
          │              │
          ▼              ▼
 Velocity Markdown   平台 payload
          │              │
          ▼              ▼
 preview/copy/export  upload/integration
```

## 关键入口

- `SpringDocViewServiceImpl`：构建 Spring Controller 与 Feign 风格方法文档。
- `DubboDocViewServiceImpl`：构建 Dubbo Service 方法文档。
- `SpringPsiUtils`、`DubboPsiUtils`、`ParamPsiUtils`、`DocViewUtils`：解析路径、方法、参数、注解、字段树和示例。
- `VelocityUtils`：消费 `DocView` 渲染 Markdown；模板变量属于当前版本的输出 contract。
- `WriterService`：通过 IntelliJ write command 写回 Javadoc、注解或编辑器文本。
- `DocViewWindowPanel`、`PreviewForm`、`ParamDocEditorForm`：导航、预览和编辑 UI。
- `YApiServiceImpl`、`ShowDocServiceImpl`、`YuQueServiceImpl`：平台 payload 与上传业务。
- `ProjectDocumentGenerationService`：UI 与后续 MCP 共用的显式生成入口；提交 Document 到 PSI 而不保存源文件，协调取消、项目生命周期、输入代次和发布结果。
- `ProjectDocumentSnapshotService` / `ProjectDocumentExporter`：后台 read action 内复用生成器和模板，捕获不带 PSI 的 Markdown 与结构化详情；离开 read action 后预检、暂存并逐文件原子发布，最终 `index.json` 是提交点。
- `ProjectDocumentIndexService`：只读固定项目目录的 JSON 文档库，验证版本、路径、身份和 hash；提供状态、搜索分页、详情及 Markdown 读取。查询不扫描 PSI。
- `IntegrationCredentialService`：按项目和平台隔离的 PasswordSafe 访问；普通项目状态不保存新密钥。
- `DocViewWelcomeService` / `DocViewWelcomeEditorProvider`：每个项目复用一个欢迎/更新编辑器；正文使用随包 HTML/CSS 资源，完整用户指南由 GitHub 维护。

## 配置与扩展

主要配置通过 `PersistentStateComponent` 持久化：`Settings`、`WindowSettings`、`TemplateSettings`、`YApiSettings`、`ShowDocSettings`、`YuQueSettings`。配置字段变化必须定义新版默认值、必要的重新配置说明、缓存失效和 UI 保存/取消行为；按[版本演进策略](intellij-compatibility.md#版本演进策略)不增加旧字段适配或跨版本迁移。

`plugin.xml` 注册 service、startup activity、line marker、settings、tool window、notification、definitions scoped search 和 actions。修改 descriptor 或 extension 必须参考 [IntelliJ 兼容性](intellij-compatibility.md) 并运行相应验证。

配置按常规行为、生成规则、模板与输出、平台集成组织，仍使用项目级状态。凭据保留、替换和清除在 PasswordSafe 操作成功后才提交同页普通字段；未应用的密钥草稿不进入持久化状态。版本提醒是应用级偏好。旧 XML 凭据不做自动迁移或主动清理。

预览和目录刷新由各自的 Disposable 所有者管理后台任务与 UI 资源。任务带请求代次，只接受仍属于当前请求的结果；关闭窗口取消任务并释放编辑器、渲染面板和监听器。模板示例预览使用固定虚构 DTO，不扫描用户项目。欢迎/更新编辑器持有 JCEF browser 并加载本地静态资源；用户点击外链后才交由默认浏览器打开 GitHub，不内嵌远程正文。

项目导出身份包含模块名、项目相对源码路径、FQN 和完整方法签名，不包含行号或业务标题。它使用固定的 `docs/doc-view/`，不新增自定义 IDE 索引。Markdown 所有权 marker、JSON envelope 和内容 hash 防止接管未标记或手改文件；同进程读写锁与 `.doc-view.lock` 跨进程 writer 锁协调访问。所有输出暂存后逐文件原子替换；最终索引提交前的取消或失败可能留下部分已发布页面，按报告核对后重试，不能描述为跨文件事务。跨进程读者使用单次捕获的不可变索引字节，并校验所读取页面与该索引的 hash；旧摘要仍可查询，已被替换且 hash 不符的详情拒绝读取，不保证旧快照的页面始终可用。

1.6.x项目文档服务同时落盘人读 Markdown、JSON 摘要索引及接口详情，并提供文档搜索和打开 UI；实现与验收由 [`project-document-store`](../openspec/changes/project-document-store/tasks.md) 跟踪，仍保留未完成的原生验收。三种输出共用一次 PSI-free 快照。缓存仅保留不可变索引；首次加载/重开为 unknown，本会话生成后未观察到输入变化时为 current，观察到变化后为 stale。清缓存不删除文档，也不清除本会话新鲜度依据。这不等同于 `FileBasedIndex` 或语义索引。

1.7.0待发布源码已加入内置MCP adapter，由 [`doc-view-mcp-tools`](../openspec/changes/doc-view-mcp-tools/proposal.md) 在同一个 Doc View 插件中声明可选 provider，注册到 IDEA 自带 MCP Server，并复用文档库服务：`doc_view_status`、`doc_view_search`、`doc_view_get` 只读，`doc_view_generate` 仅在显式请求时生成或更新文件。两个精确263 build的构建/Verifier、最低build可选加载与真实Codex取消已验证，完整生命周期矩阵仍有未覆盖项，见[取消说明](mcp-cancellation.md)与[路线图](roadmap.md)。客户端 Skill 由 [`doc-view-client-skill`](../openspec/changes/doc-view-client-skill/proposal.md) 维护为另一种分发物，调用这些 MCP 工具，不另建服务或搬移 Java/PSI 解析；当前尚无正式可安装包，操作状态见[MCP 使用说明](user-guide/mcp.md)。

平台 HTTP 仍使用现有 Apache 客户端，每次请求和响应使用 closeable 资源，连接/连接池等待10秒、socket读取30秒，不自动重试或跳转。facade负责有效响应判断；语雀只有HTTP404表示未找到。service保留平台级脱敏通知，透传取消；项目凭据与运行数据不进入异常正文。

## 架构规则

- UI 和 action 不直接复制 PSI 解析逻辑，优先消费 service/DTO。
- Spring 与 Dubbo 共享的 PSI 工具变更要评估两条路径。
- 网络、批量导出和重 PSI 分析不在 EDT 执行。
- 长期缓存不保存 PSI element；缓存必须有 project 生命周期和失效规则。
- 上传配置中的 token、私密地址和真实/敏感项目或页面标识不进入日志、文档或示例；公开的虚构示例 ID 可以保留。
- 可观察行为变化先按 [Contract 设计](contract-design.md)写入 OpenSpec spec。
