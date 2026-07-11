# AGENTS.md

## 项目概览

Doc View 是一个 JetBrains IDE 插件，用于从 Java 源码生成 Markdown API 文档。项目支持 Spring/Spring Boot Controller、Feign 风格接口和 Dubbo
Service，并提供 Markdown 预览、复制、导出，以及上传到 YApi、ShowDoc、YuQue 等平台的能力。

- 生产代码语言：Java 21
- 构建系统：Gradle 9.5.0
- IntelliJ Platform Gradle Plugin：`org.jetbrains.intellij.platform` 2.16.0
- 目标平台：IntelliJ IDEA IU 2026.1+
- 插件版本：1.3.13
- Bundled plugin：Java、Markdown
- 主包名：`com.liuzhihang.doc.view`

版本与兼容性事实以 `gradle.properties`、`build.gradle`、Gradle wrapper 和 `plugin.xml` 为准；上述摘要随基线 change 同步更新。

## 维护范围与变更管理

- 具体变更范围以用户请求、当前分支上下文和活动 OpenSpec change 为准，不在本文件维护一次性文件白名单。
- 新功能、用户可见行为、兼容性或架构变化先创建 OpenSpec proposal/design/spec/tasks，再修改运行时代码。
- 低风险机械文档修正可以直接实施；描述运行行为变化的文档必须与对应 contract 同步。
- 生产实现保持 Java-only；引入新生产语言、依赖、extension 或平台基线必须在 change 中明确批准。
- 精准修改当前任务需要的文件，不回滚维护者或其他工具的无关改动。
- token、账号、cookie、生产私密地址和私有代码不得进入仓库、artifact、日志或示例。

## Canonical 文档

- [架构](docs/architecture.md)：模块边界、核心数据流和扩展入口。
- [Contract 设计](docs/contract-design.md)：外部可观察行为、输入输出和验证选择。
- [性能指南](docs/performance-guide.md)：PSI、EDT、缓存、渲染、导出和上传约束。
- [IntelliJ 兼容性](docs/intellij-compatibility.md)：平台、Java、Gradle、Verifier 和 API 策略。
- [AI 协作工作流](docs/ai-workflow.md)：OpenSpec lifecycle 与 repo-local skill 分工。
- [路线图](docs/roadmap.md)：产品化开发顺序和独立 change 边界。
- [发布检查清单](docs/release-checklist.md)：发布前、手工验证、发布后和回滚步骤。

## Java-only 生产代码策略

- 生产插件实现保持 Java-only；不新增 Kotlin、Groovy、脚本 runtime 或生成式生产源码。
- Markdown、OpenSpec artifacts、YAML metadata、Codex skill、Gradle 文件和 IDE form 属于仓库基础设施，不改变 Java-only 策略。
- 新实现优先沿用现有 IntelliJ Platform API、service、DTO 和工具类。

## Contract-first 策略

修改 PSI、DTO、Markdown、上传、导出、复制/写回、设置、IntelliJ 兼容性或 UI 行为前，必须按 [Contract 设计](docs/contract-design.md)在 OpenSpec 中定义输入、可观察输出、不支持场景和验证。除非 contract 明确变化，否则保持生成文档行为稳定。

## AI 协作工作流

- OpenSpec 管 change lifecycle；`.codex/skills/doc-view-*` 管 contract、文档、Java 和发布领域门禁。
- artifacts 默认使用中文；默认在 `develop` 开发，实际分支和 scope 以用户请求及 Git 状态为准。
- 工作区可能有维护者或其他工具的未提交改动；不得回滚无关文件。
- 文档-only 任务不得夹带 runtime 行为；checkbox 只有在实现和验证完成后勾选。
- 完整流程见 [AI 协作工作流](docs/ai-workflow.md)。

## 验证与发布

- 按 [发布检查清单](docs/release-checklist.md)选择能证明结果的最小命令和手工场景。
- 不把未运行、0 tests、缓存命中或部分检查描述为完整通过。
- 用户可见变化更新 `CHANGELOG.md`；正式发布通常从 `develop` 合入 `master`。
- 只有维护者明确授权且凭据就绪时才执行 `publishPlugin`、推送 tag 或创建 Release。

## 维护提醒

- Lombok 注解处理、Spring/Dubbo 共用 PSI、Velocity 模板变量和 `plugin.xml` extension 都是高影响边界。
- Maven 依赖使用 Aliyun mirror 和 Maven Central；依赖变化必须在 design 中说明。
