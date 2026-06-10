# Architecture

本文档描述 Doc View 当前架构边界，供维护者和 Codex 在开发前建立共同上下文。它不定义新功能，也不替代源码；当源码结构变化时，应同步更新本文档。

## 运行环境

- 生产代码语言：Java 21
- 构建系统：Gradle 9.0.0
- IntelliJ Platform Gradle Plugin：`org.jetbrains.intellij.platform` 2.16.0
- 目标平台：IntelliJ IDEA IU 2024.2+
- 插件依赖：`com.intellij.modules.platform`、`com.intellij.modules.java`、`org.intellij.plugins.markdown`
- 插件 ID：`com.liuzhihang.doc-view`

## 模块边界

| 包 | 责任 |
| --- | --- |
| `action` | 编辑器右键菜单、预览、编辑、上传、工具栏动作入口 |
| `config` | 项目级和应用级配置、Settings UI、持久化状态 |
| `constant` | Spring、Dubbo、Swagger、Validation、Lombok 等注解和类型常量 |
| `data` | IntelliJ DataKey 定义 |
| `dom` | Spring XML Beans 和 Dubbo definition 搜索 |
| `dto` | `DocView`、`Body`、`Param`、`Header` 等内部文档模型 |
| `enums` | 框架类型、请求内容类型等枚举 |
| `exception` | Doc View 自定义异常 |
| `integration` | YApi、ShowDoc、YuQue facade 及平台 DTO |
| `listener` | 服务事件监听 |
| `notification` | IDE 启动和业务通知 |
| `provider` | Line marker、动作提供器等 IDE 扩展 |
| `service` | Spring/Dubbo 解析、写入、上传等核心服务 |
| `ui` | Swing form、预览、参数编辑、tool window 树和面板 |
| `utils` | PSI、Velocity、HTTP、导出、Dialog、文件等工具 |

## 核心数据流

1. 用户通过编辑器菜单、line marker 或 tool window 触发文档生成。
2. `SpringDocViewServiceImpl` 或 `DubboDocViewServiceImpl` 判断目标方法是否可生成文档。
3. PSI 工具读取类、方法、参数、注解、返回值和注释。
4. 服务将分析结果组装为 `DocView`，其中包含接口名称、描述、路径、方法、Header、请求参数、请求体、响应体和示例。
5. UI 层展示 `DocView`，或通过 Velocity 模板生成 Markdown。
6. 导出、复制、写回注释、上传到 YApi/ShowDoc/YuQue 等动作消费同一份文档模型。

## 核心服务

- `SpringDocViewServiceImpl`：识别 Spring Controller/Feign 风格方法，解析路径、HTTP 方法、content type、form 参数、JSON body、Header 和响应体。
- `DubboDocViewServiceImpl`：识别 Dubbo 方法，将类名和方法名作为接口路径语义，默认使用 JSON body 表达请求参数。
- `WriterService`：通过 IntelliJ `WriteCommandAction` 写入 Javadoc 或编辑器文本，并触发代码格式化。
- `YApiServiceImpl`、`ShowDocServiceImpl`、`YuQueServiceImpl`：平台上传业务服务。
- `YApiFacadeServiceImpl`、`ShowDocFacadeServiceImpl`、`YuQueFacadeServiceImpl`：平台接口 facade。

## PSI 工具

PSI 工具是解析准确性的核心，任何变更都具有较高影响面。

- `SpringPsiUtils`：Spring 注解、路径、HTTP 方法、请求体、form 参数、Header 等解析。
- `DubboPsiUtils`：Dubbo service 方法、请求体和示例解析。
- `ParamPsiUtils`：请求/响应字段树、泛型、嵌套对象、示例 JSON 生成。
- `CustomPsiUtils`、`CustomPsiCommentUtils`：通用 PSI 和注释读取。
- `DocViewUtils`：标题、名称、描述等文档元信息聚合。

## UI 结构

- `DocViewToolWindowFactory` 创建右侧 `Doc View` tool window。
- `DocViewWindowPanel` 承载目录树、刷新、导出、上传、展开/折叠、清理缓存和设置入口。
- `PreviewForm` 展示 Markdown 预览和右侧操作。
- `ParamDocEditorForm` 支持参数编辑和写回注释或注解。
- `SettingsForm`、`TemplateSettingForm`、`YApiSettingForm`、`ShowDocSettingForm`、`YuQueSettingForm` 提供项目级配置界面。

UI 变更必须关注 EDT 响应、Dumb Mode、空项目状态、无编辑器状态和项目关闭状态。

## 配置模型

主要设置以 IntelliJ `PersistentStateComponent` 持久化：

- `ApplicationSettings`：应用级设置。
- `Settings`：通用项目级设置，例如标题、名称、字段过滤、注解策略、line marker、导出行为。
- `WindowSettings`：tool window 状态。
- `TemplateSettings`：Markdown 模板。
- `YApiSettings`、`ShowDocSettings`、`YuQueSettings`：平台集成配置。

配置变更需要考虑默认值、旧版本配置兼容、序列化字段稳定性和 UI 表单同步。

## plugin.xml 扩展点

`src/main/resources/META-INF/plugin.xml` 注册：

- application service 和 project service
- startup notification
- Java line marker provider
- project configurable
- `Doc View` tool window
- notification group
- XML DOM metadata 和 scoped search
- editor popup、上传、预览、tool window toolbar、catalog menu 等 actions

修改 `plugin.xml` 必须运行插件验证，并在 `docs/intellij-compatibility.md` 记录兼容性判断。

## 架构维护规则

- 优先沿用现有包边界和服务职责。
- 新行为先定义 contract，再修改 PSI、DTO、模板或集成代码。
- PSI 相关变更必须同时评估 Spring 和 Dubbo 路径。
- UI 行为不应直接绕过 service/DTO 层消费 PSI 细节。
- 平台集成代码必须保持 token、地址、项目 ID 等敏感配置在 settings 中，不写入文档或日志。
- 新 runtime 能力需要 OpenSpec proposal，不在文档-only 变更中夹带实现。
