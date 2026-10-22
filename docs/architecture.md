# 架构

本文描述 Doc View 当前模块边界和核心数据流。实现与版本事实以源码、`build.gradle`、`gradle.properties` 和 `plugin.xml` 为准；本文件不定义新功能。

## 运行边界

- 生产实现保持 Java-only。
- 插件运行在 IntelliJ IDEA Ultimate，依赖 Java 与 Markdown bundled plugin；本地欢迎与更新页面由 IDE JCEF 呈现。
- PSI 解析、DTO 组装、Markdown 渲染、导出和上传共享同一份 `DocView` 数据模型。
- 新的运行时依赖、extension、生产语言或平台基线必须由独立 OpenSpec change 批准。

## 包职责

| 包 | 职责 |
| --- | --- |
| `action` | 编辑器、预览、上传和 tool window 动作入口 |
| `config` | 项目/应用设置、持久化状态和 Configurable |
| `constant` | Spring、Dubbo、Swagger、Validation 等注解和类型常量 |
| `dom` | Spring XML/Dubbo DOM 与 scoped search |
| `dto` | `DocView`、`Body`、`Param`、`Header` 等文档模型 |
| `integration` | YApi、ShowDoc、YuQue facade 和平台 DTO |
| `provider` | line marker 和 IDE provider |
| `service` | Spring/Dubbo 文档构建、写入和上传服务 |
| `service/export` | 项目接口快照、稳定身份、索引及受管理 Markdown 文件发布 |
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
- `ProjectDocumentSnapshotService` / `ProjectDocumentExporter`：后台 read action 内复用生成器和模板，转换为不带 PSI 的 `ProjectDocumentPage`；离开 read action 后预检、暂存并发布项目 Markdown，索引最后更新。
- `IntegrationCredentialService`：按项目和平台隔离的 PasswordSafe 访问；普通项目状态不保存新密钥。
- `DocViewWelcomeService` / `DocViewWelcomeEditorProvider`：每个项目复用一个欢迎/更新编辑器；正文使用随包 HTML/CSS 资源，完整用户指南由 GitHub 维护。

## 配置与扩展

主要配置通过 `PersistentStateComponent` 持久化：`Settings`、`WindowSettings`、`TemplateSettings`、`YApiSettings`、`ShowDocSettings`、`YuQueSettings`。配置字段变化必须定义新版默认值、必要的重新配置说明、缓存失效和 UI 保存/取消行为；按[版本演进策略](intellij-compatibility.md#版本演进策略)不增加旧字段适配或跨版本迁移。

`plugin.xml` 注册 service、startup activity、line marker、settings、tool window、notification、DOM/search 和 actions。修改 descriptor 或 extension 必须参考 [IntelliJ 兼容性](intellij-compatibility.md) 并运行相应验证。

配置按常规行为、生成规则、模板与输出、平台集成组织，仍使用项目级状态。凭据保留、替换和清除在 PasswordSafe 操作成功后才提交同页普通字段；未应用的密钥草稿不进入持久化状态。版本提醒是应用级偏好。旧 XML 凭据不做自动迁移或主动清理。

预览和目录刷新由各自的 Disposable 所有者管理后台任务与 UI 资源。任务带请求代次，只接受仍属于当前请求的结果；关闭窗口取消任务并释放编辑器、渲染面板和监听器。模板示例预览使用固定虚构 DTO，不扫描用户项目。欢迎/更新编辑器持有 JCEF browser 并加载本地静态资源；用户点击外链后才交由默认浏览器打开 GitHub，不内嵌远程正文。

项目导出身份包含模块名、项目相对源码路径、FQN 和完整方法签名，不包含行号或业务标题。它使用固定的 `docs/doc-view/`，不新增自定义 IDE 索引。所有权 marker 与正文 hash 防止直接接管未标记或手改文件；磁盘失败可能留下部分已替换页面，需按报告核对后重试。单文件原子替换不等于整批事务或跨进程锁。

平台 HTTP 仍使用现有 Apache 客户端，每次请求和响应使用 closeable 资源，连接/连接池等待10秒、socket读取30秒，不自动重试或跳转。facade负责有效响应判断；语雀只有HTTP404表示未找到。service保留平台级脱敏通知，透传取消；项目凭据与运行数据不进入异常正文。

## 架构规则

- UI 和 action 不直接复制 PSI 解析逻辑，优先消费 service/DTO。
- Spring 与 Dubbo 共享的 PSI 工具变更要评估两条路径。
- 网络、批量导出和重 PSI 分析不在 EDT 执行。
- 长期缓存不保存 PSI element；缓存必须有 project 生命周期和失效规则。
- 上传配置中的 token、私密地址和真实/敏感项目或页面标识不进入日志、文档或示例；公开的虚构示例 ID 可以保留。
- 可观察行为变化先按 [Contract 设计](contract-design.md)写入 OpenSpec spec。
