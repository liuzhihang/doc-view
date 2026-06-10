# AGENTS.md

## 项目概览

Doc View 是一个 JetBrains IDE 插件，用于从 Java 源码生成 Markdown API 文档。项目支持 Spring/Spring Boot Controller、Feign 风格接口和 Dubbo Service，并提供 Markdown 预览、复制、导出，以及上传到 YApi、ShowDoc、YuQue 等平台的能力。

- 生产代码语言：Java 21
- 构建系统：Gradle 9.0.0
- IntelliJ Platform Gradle Plugin：`org.jetbrains.intellij.platform` 2.16.0
- 目标平台：IntelliJ IDEA IU 2024.2+
- 插件版本：1.3.11
- Bundled plugin：Java、Markdown
- 主包名：`com.liuzhihang.doc.view`

## 当前变更范围

`establish-modern-intellij-build-compatibility-baseline` 是构建与兼容性基线变更，用于迁移现代 IntelliJ Platform 构建链路、Java 21 基线和 2024.2+ 兼容范围。

本次变更允许修改：

- `AGENTS.md`
- `docs/**/*.md`
- `.gitignore`
- `build.gradle`
- `gradle.properties`
- `gradle/wrapper/**`
- `gradlew`
- `gradlew.bat`
- `src/main/java/com/liuzhihang/doc/view/notification/DocViewNotification.java`，仅用于修复 IntelliJ 2024.2+ 下类初始化期间请求 service 的兼容性问题
- `openspec/changes/establish-modern-intellij-build-compatibility-baseline/**`

本次变更禁止修改：

- `src/main/java` 下除 `DocViewNotification.java` 兼容性修复外的 Java 生产代码
- `src/test` 下的测试代码
- `src/main/resources/META-INF/plugin.xml`
- runtime 资源、图标、message bundle、模板、IDE form 文件
- 插件行为、生成 Markdown 行为、上传行为、导出行为、解析行为、设置持久化行为或 UI 行为

## 文档索引

- [架构说明](docs/architecture.md)：模块边界、扩展点、核心服务、PSI 工具、UI、配置、平台集成。
- [路线图](docs/roadmap.md)：基础设施、契约、解析正确性、UI、集成、兼容性、MCP 探索的阶段规划。
- [契约设计](docs/contract-design.md)：解析、DTO、Markdown、平台 payload、IntelliJ 兼容和验证策略。
- [性能指南](docs/performance-guide.md)：PSI、read/write action、EDT、模板渲染、导出、HTTP、缓存和性能验证。
- [IntelliJ 兼容性](docs/intellij-compatibility.md)：目标 IDE、bundled plugin、API 兼容、插件验证和升级流程。
- [Vibe Coding 工作流](docs/vibe-coding-workflow.md)：OpenSpec 与 Codex 协作生命周期、review checkpoint 和文档维护规则。
- [MCP 设计](docs/mcp-design.md)：未来 MCP 能力路线图、安全边界和本次不实现 MCP 的说明。
- [i18n](docs/i18n.md)：UI 文案、message bundle、生成文档语言、设置项和未来翻译策略。
- [发布检查清单](docs/release-checklist.md)：发布前验证、changelog、插件验证、手动 IDE 检查、Marketplace 准备和回滚。

## 常用命令

```bash
# 构建插件
./gradlew buildPlugin

# 在沙箱 IDE 中运行插件
./gradlew runIde

# 运行测试
./gradlew test

# 验证插件兼容性
./gradlew verifyPlugin

# 更新 changelog
./gradlew patchChangelog

# 发布到 JetBrains Marketplace
./gradlew publishPlugin
```

文档-only 变更优先做轻量验证：

```bash
openspec status --change "<change-name>"
git diff --name-only
```

当变更触及 Java、Gradle、`plugin.xml`、模板、资源或任何运行时行为时，必须运行相应 Gradle 验证。

## 项目结构

```text
src/
  main/
    java/com/liuzhihang/doc/view/
      action/         IDE action：预览、编辑、工具栏、上传
      config/         持久化配置和 Configurable
      constant/       注解、字段类型、框架常量
      data/           IntelliJ DataKey
      dom/            XML DOM 解析和 scoped search
      dto/            DocView、Body、Param、Header 等内部模型
      enums/          框架类型、content type 等枚举
      exception/      自定义异常
      integration/    YApi、ShowDoc、YuQue facade 和 DTO
      listener/       事件监听
      notification/   IDE 通知
      provider/       line marker、action provider
      service/        Spring/Dubbo 解析、写入、上传等核心服务
      ui/             Swing form、预览、参数编辑、tool window
      utils/          PSI、Velocity、HTTP、导出、Dialog、文件工具
    resources/
      META-INF/plugin.xml
      icons/
      image/
      messages/
  test/
    java/
    http/
```

## 架构入口

- `SpringDocViewServiceImpl`：从 Spring Controller/Feign 风格方法构建 `DocView`。
- `DubboDocViewServiceImpl`：从 Dubbo Service 方法构建 `DocView`。
- `WriterService`：通过 IntelliJ write command 写入 Javadoc 或编辑器文本。
- `DocViewWindowPanel`、`DocViewToolWindowFactory`：右侧 tool window 入口。
- `PreviewForm`、`ParamDocEditorForm`：预览和参数编辑 UI。
- `CustomPsiUtils`、`SpringPsiUtils`、`DubboPsiUtils`、`ParamPsiUtils`：PSI 分析和模型生成工具。
- `VelocityUtils`：Markdown 模板渲染。
- `ExportUtils`：导出能力。
- `HttpUtils`：平台集成使用的 HTTP 工具。
- `Settings`、`TemplateSettings`、`YApiSettings`、`ShowDocSettings`、`YuQueSettings`：项目级设置。

## Java-only 生产代码策略

- 生产插件实现必须保持 Java-only，除非未来有单独通过的 OpenSpec 变更明确修改该策略。
- 不新增 Kotlin、Groovy、脚本语言 runtime 代码、生成式 runtime source 或新的生产语言。
- Markdown 文档、OpenSpec artifacts、YAML metadata、Codex skill、现有 Gradle 文件和 IDE form 文件属于仓库基础设施；只要符合任务范围，可以维护。
- 新增实现优先沿用现有 IntelliJ Platform API、Java service、DTO 和工具类，不轻易引入新抽象。

## Contract-first 策略

在修改以下行为前，必须先定义契约：

- PSI 解析规则
- DTO 字段和默认值
- 生成 Markdown 结构
- 上传、导出、复制、写回注释或注解
- 设置持久化
- IntelliJ 兼容性
- UI 可见行为

推荐流程：

1. 创建或更新 OpenSpec change。
2. 按 `docs/contract-design.md` 定义外部可观察契约。
3. 明确 Java 输入样例、预期 `DocView`、预期 Markdown、平台 payload 和边界场景。
4. 能自动化时先补验证；不能自动化时写明手动验证步骤。
5. 除非契约明确变化，否则保持生成文档行为稳定。

## AI 协作工作流

- 新功能、行为变化、兼容性变化和架构变化优先走 OpenSpec。
- OpenSpec artifacts 默认使用中文撰写，除非维护者明确要求使用其他语言。
- 任务匹配时使用 `.codex/skills/` 下的 repo-local skill。
- 保持改动小而聚焦，遵循现有包边界和服务职责。
- 工作区可能存在维护者或其他工具的未提交变更；不要回滚无关文件。
- 文档-only 任务不得夹带 runtime 行为变化。
- OpenSpec task 只有在对应改动或验证真实完成后才能勾选。
- 跳过验证时必须说明原因。

## 验证策略

按变更类型选择能证明结果的最小命令集：

- 文档-only：`openspec status --change "<change-name>"`、文件存在性检查、`git diff --name-only`。
- Java 解析或 service 变更：`./gradlew test`。
- 插件打包、descriptor 或资源变更：`./gradlew buildPlugin`，通常还需要 `./gradlew verifyPlugin`。
- IntelliJ 兼容性升级：`./gradlew verifyPlugin`、`./gradlew runIde` 中的核心手动流程，以及发布检查清单。
- 上传或平台集成：可用的单元测试，加非生产 endpoint 的手动验证。

不要把未运行的命令描述为已通过。

## 发布纪律

- 用户可见行为变化需要更新 `CHANGELOG.md`。
- 发布前按 [发布检查清单](docs/release-checklist.md) 执行。
- 不提交 Marketplace token、平台 token、账号、cookie 或生产私密地址。
- 只有维护者明确要求并准备好凭据时，才执行 `./gradlew publishPlugin`。

## 维护提醒

- Lombok 使用较多，维护时要注意注解处理。
- PSI 工具变更会同时影响 Spring 和 Dubbo 路径，属于高影响面。
- Velocity 模板驱动 Markdown 输出，模板变量需要兼容用户自定义模板。
- `plugin.xml` 注册 services、providers、actions、settings pages、tool window、notification group、DOM/search extension；修改 descriptor 必须做兼容性验证。
- Maven 依赖使用 Aliyun mirror 和 Maven Central。
