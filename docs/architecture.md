# 架构

本文描述 Doc View 当前模块边界和核心数据流。实现与版本事实以源码、`build.gradle`、`gradle.properties` 和 `plugin.xml` 为准；本文件不定义新功能。

## 运行边界

- 生产实现保持 Java-only。
- 插件运行在 IntelliJ IDEA Ultimate，依赖 Java 与 Markdown bundled plugin。
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
- `VelocityUtils`：消费 `DocView` 渲染 Markdown；模板变量属于兼容 contract。
- `WriterService`：通过 IntelliJ write command 写回 Javadoc、注解或编辑器文本。
- `DocViewWindowPanel`、`PreviewForm`、`ParamDocEditorForm`：导航、预览和编辑 UI。
- `YApiServiceImpl`、`ShowDocServiceImpl`、`YuQueServiceImpl`：平台 payload 与上传业务。

## 配置与扩展

主要配置通过 `PersistentStateComponent` 持久化：`Settings`、`WindowSettings`、`TemplateSettings`、`YApiSettings`、`ShowDocSettings`、`YuQueSettings`。配置字段变化必须定义默认值、旧值兼容、缓存失效和 UI 保存/取消行为。

`plugin.xml` 注册 service、startup activity、line marker、settings、tool window、notification、DOM/search 和 actions。修改 descriptor 或 extension 必须参考 [IntelliJ 兼容性](intellij-compatibility.md) 并运行相应验证。

## 架构规则

- UI 和 action 不直接复制 PSI 解析逻辑，优先消费 service/DTO。
- Spring 与 Dubbo 共享的 PSI 工具变更要评估两条路径。
- 网络、批量导出和重 PSI 分析不在 EDT 执行。
- 长期缓存不保存 PSI element；缓存必须有 project 生命周期和失效规则。
- 上传配置中的 token、私密地址和真实/敏感项目或页面标识不进入日志、文档或示例；公开的虚构示例 ID 可以保留。
- 可观察行为变化先按 [Contract 设计](contract-design.md)写入 OpenSpec spec。
