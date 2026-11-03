# Tasks

## 1. 分析和基线
- [x] 1.1 核对远端六条开放 Issue、原图/评论与代码；验证当前基线和隔离副本，保留原工作区改动。
- [x] 1.2 运行 `./gradlew --offline test --rerun-tasks --no-build-cache` 建立基线；237 tests、49 suites、0 failures/errors/skipped，14 tasks executed。

## 2. 依赖源码与文档标签
- [x] 2.1 实现 #101 已关联源码字段注释及无源码降级，以 `DependencySourceCommentsTest` 的真实 JAR PSI RED/GREEN 验证并记录使用条件。
- [x] 2.2 实现 #89 接口和字段标签、默认/自定义模板、模板说明，以 `SinceVersionContractTest` 与 `TemplateGuideExamplesTest` 验证。

## 3. Spring 路径与参数描述
- [x] 3.1 修复 #90 数组/别名/常量路径并复验模板生命周期，以 `SpringRouteSelectionTest` 和 `TemplateSettingsLifecycleTest` 验证。
- [x] 3.2 复验 #91 原始 Dubbo 参数场景并修复确认存在的参数匹配缺陷，以 `ParameterDocumentationTest` 验证；记录已修复与新增修复的区别。

## 4. 字段表与实体复制
- [x] 4.1 修复 #96 合成容器层级，以 `ContainerMarkdownHierarchyTest` 验证普通/分表、递归与真实同名字段，JSON 不变。
- [x] 4.2 实现 #97 独立实体复制 Markdown，以编辑器 action 测试验证编辑值、分表、禁用和关闭取消，并更新用户指南。

## 5. 整体验证与证据
- [x] 5.1 运行 `./gradlew --offline test --rerun-tasks --no-build-cache`、`buildPlugin verifyPluginProjectConfiguration verifyPlugin`，检查真实测试计数和兼容性报告。
- [x] 5.2 已尝试 `runIde` 和相同 build 原生启动器；工具绑定失败与 macOS spawn163 阻碍已记录，原生交互仍未验证，不冒称通过。
- [x] 5.3 更新 CHANGELOG、逐项结论及 verification.md；执行 `openspec validate open-issue-backlog --strict`、`git diff --check`，核对代码范围和未交付状态。

## Workflow follow-up
- 完成后同步 specs 并按用户要求归档，再验证归档和长期规格。
- 不自行合并或发布；本轮支持项在修复未交付前保持 Issue 打开，报告本地提交与未推送事项。
