# Tasks

## 1. 字段 requiredMode 解析与输出

- [x] 1.1 为 `OpenApi3SchemaRequiredModeTest` 建立 Java PSI fixture 和注解桩，覆盖 `REQUIRED`、`NOT_REQUIRED`、`AUTO`、无法解析的模式值按 AUTO 处理、未声明模式、配置验证注解、Swagger 2 `@ApiModelProperty`、Doc View tag 与 deprecated `Schema.required` 被忽略；先运行 `./gradlew test --tests 'com.liuzhihang.doc.view.contract.OpenApi3SchemaRequiredModeTest' --rerun-tasks`，确认新增模式/旧属性断言在旧实现上按预期失败。
- [x] 1.2 更新字段 required 解析，使显式 mode 覆盖其他来源，AUTO 保持 Doc View 当前配置来源，并将结果送入现有 `Body.required` 与 Spring 扁平 `Param.required`；重跑 `./gradlew test --tests 'com.liuzhihang.doc.view.contract.OpenApi3SchemaRequiredModeTest' --rerun-tasks`，所有映射断言通过。
- [x] 1.3 为 Spring 请求/响应 DTO 与扁平字段参数生成真实 `DocView` 和专用 Markdown golden，验证 required 显示变化不改字段其他元数据、章节或层级；重跑上述 targeted test 并检查 golden 完全匹配。

## 2. Schema 注解源码写回

- [x] 2.1 在 `OpenApi3SchemaRequiredModeTest` 加入 PSI 写回场景，覆盖 true/false mode、被编辑字段清除 deprecated `required`、保留其他 Schema 成员、含引号/反斜杠/换行的 description 往返，以及 `Schema` 类型缺少 `requiredMode` 时提示依赖版本（官方 `swagger-annotations` 2.2.5+）且注解不变；先运行 targeted test，确认当前 writer 存在对应失败。
- [x] 2.2 将已有 `@Schema` 写回改为成员级 PSI 更新，以当前编辑结果写入 `requiredMode`、移除该字段旧 `required` 并保留未编辑成员；description 使用合法 Java 字符串字面量。Schema 类型缺少 `requiredMode` 时抛带版本提示的 `DocViewException`，Confirm 捕获该预期异常并通过 `DocViewNotification.notifyError` 提示，同时保持弹窗和用户编辑内容；仅写回成功后关闭，不捕获 `Throwable`。重跑 `./gradlew test --tests 'com.liuzhihang.doc.view.contract.OpenApi3SchemaRequiredModeTest' --rerun-tasks` 并断言源码重解析有效。

- [x] 2.3 先为字段级 Schema.description 增加失败回归：覆盖带引号、反斜杠、LF 的 Java 字符串字面量和常量拼接表达式；读取字段描述后仅切换 requiredMode，断言表格输入及源码重读都保持原 Java 字符串值。先运行该测试类并记录 RED。
- [x] 2.4 使用 IntelliJ Java PSI 常量求值读取字段级 Schema.description 的 Java 字符串值，不增加自定义解析/fallback，也不改 Swagger 2 或其他注解读取；重跑 targeted 测试并记录 GREEN。
- [x] 2.5 为显式空串/纯空白 `Schema.description` 且存在 Javadoc 竞争来源增加 PSI mode-only 往返回归；targeted RED 复现了读取回退和写回丢值，修复后 targeted GREEN，并确认未声明 Schema.description 时仍使用 Swagger 2 fallback。
- [x] 2.6 用 `LinkedHashMap` 回归验证普通可写字段排在缺少 `requiredMode` 的 Schema 字段之前时，targeted RED 复现了前序字段已写源码；修复为写回前预检本批次所有 Schema 字段后 targeted GREEN。此保证仅覆盖 requiredMode 能力预检失败，不代表所有异常具备批次事务原子性。
- [x] 2.7 增加 Javadoc 来源竞争回归：字段带原有 Schema.description 后清空，或字段无 Schema.description 时将 Javadoc 回退改为空白；targeted RED 复现了确认后重新读取回到 Javadoc。最小修复为 Schema 写回始终显式保存非 null 编辑器字符串（包括空串/空白），targeted GREEN（12 项通过）；null 描述写回不新增行为。

## 3. 集成验证

- [x] 3.1 运行 OpenAPI 3 字段契约和现有 Spring Markdown contract：`./gradlew test --tests 'com.liuzhihang.doc.view.contract.OpenApi3SchemaRequiredModeTest' --tests 'com.liuzhihang.doc.view.contract.SpringMarkdownContractTest' --rerun-tasks`；确认新 golden 与旧 Spring golden 均通过。
- [x] 3.2 运行构建、平台验证与 OpenSpec strict 校验；主线全量门禁 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks` 成功，`openspec validate openapi3-schema-required-mode --strict` 有效。
- [x] 3.3 更新用户指南 `docs/user-guide/settings.md` 与 `docs/user-guide/troubleshooting.md`，说明字段 `@Schema.requiredMode` 的优先级、AUTO 来源、deprecated `required` 不再读取，以及缺少成员时提示依赖版本并在任何字段写入前拒绝整批确认；该前置能力预检不承诺其他异常下的整批事务原子性。在 `CHANGELOG.md` 记录用户可见变化，在本 change 新建 `verification.md` 记录实际测试/构建/Verifier 结果，并通过 `git diff --check`。共享文档与 CHANGELOG 的修改由主线维护者完成。
- [ ] 3.4 补齐原生参数编辑写回验收：在专用 doc-demo 验证 requiredMode true/false、description 转义、其余 Schema 成员保留，以及缺少成员时保留草稿/源码并给出通知；恢复测试源码后记录环境与结果。当前 verification 仅有 fixture/自动化证据，不据任务全勾宣称原生已验或直接归档。
