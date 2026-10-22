# Tasks

## 1. 前置依赖与 PSI 测试基座

- [x] 1.1 确认 `openapi3-schema-required-mode` 的实现和 targeted 测试已通过后再开始本 change；核对该 change 最新 `verification.md` 与测试报告，若前置实现未通过则停止本 change 的运行时代码修改。
- [x] 1.2 在 Java PSI fixture 中增加 Swagger `@Parameter` 与 Spring `@RequestParam`、`@PathVariable`、`@RequestHeader` 注解 stub，并新建 `OpenApi3ParameterMetadataTest` 覆盖默认值、属性是否显式声明、name/value alias 与可生成的直接 Param/Header 路径；运行 `./gradlew test --tests 'com.liuzhihang.doc.view.contract.OpenApi3ParameterMetadataTest' --rerun-tasks`，确认断言针对预期 PSI 行为。

## 2. 直接参数元数据映射

- [x] 2.1 实现直接 Spring 绑定名解析：Spring 非空 `name`/`value` 优先，其次非空 `@Parameter.name`，最后 Java 名；同一 Spring 注解的 alias 值冲突时回退 Java 名且不采用 `@Parameter.name`。在 PSI 测试中覆盖三种 Spring 注解的 alias、OpenAPI 回退、空值和冲突，并运行 `./gradlew test --tests 'com.liuzhihang.doc.view.contract.OpenApi3ParameterMetadataTest' --rerun-tasks`。
- [x] 2.2 将非空 `@Parameter.description`、单值 `example` 写入现有 `Param`/`Header` 字段；空值保留当前 Javadoc 或字段回退，不裁剪接受的文本，并确认 Param 名称/示例同步反映到现有请求参数示例。增加 DocView 断言并运行上述 targeted 测试类。
- [x] 2.3 按 contract 实现 required 优先级：Doc View 正向标记对直接 `Param` 与 String `@RequestHeader` 生成的 `Header` 一致优先；`@RequestParam`/`@RequestHeader` 的 `defaultValue` 与显式或默认 `required` 其次；`@PathVariable` 的显式或默认 `required` 其次；仅在无前述结论时使用显式 `@Parameter.required`，否则保留当前回退。用矩阵测试覆盖默认、显式 true/false、defaultValue、冲突、Header 配置标记及无 `@Parameter` 输入，并运行上述 targeted 测试类。
- [x] 2.4 仅当 `@Parameter.examples`、`content` 或 `array` 有直接声明的有效复合值时忽略同注解中的标量 `example`；不解析复合内容。使用真实 PSI 注解 stub 覆盖非空与空复合值，并运行参数元数据 targeted 测试（round13 10/10 通过）。

## 3. 支持边界与 Markdown 输出

- [x] 3.1 增加边界回归：对象参数扁平化字段、Spring `@RequestBody`、方法级 `@Parameter`/`@Parameters`/`@Operation.parameters`、参数级 `@Schema` 不得被本 change 解析或改变分类；运行上述 targeted 测试类确认其仍走原路径。
- [x] 3.2 增加带 `@Parameter` 的 Spring Markdown golden，并运行 `./gradlew test --tests 'com.liuzhihang.doc.view.contract.OpenApi3ParameterMetadataTest' --rerun-tasks`，核对参数名、描述、示例和 required 的渲染结果。
- [x] 3.3 在当前参数映射改动后运行既有 Spring Markdown contract golden，确认未受影响输出保持逐字不变（round13 1/1 通过）。

## 4. 集成验证

- [x] 4.1 运行全量测试并检查报告：主线执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`，35 个测试套件、180 tests 均通过，无失败、错误或跳过。
- [x] 4.2 运行 `verifyPluginProjectConfiguration`、`buildPlugin`、`verifyPlugin`；Plugin Verifier 对 IU 261.22158.277 的 verdict 为 `Compatible`。
- [x] 4.3 运行 `openspec validate openapi3-parameter-metadata --strict` 与 scoped `git diff --check`；核对改动范围未扩展到 DTO 字段解析、Dubbo、payload、依赖或设置。
- [x] 4.4 实现完成后更新 `docs/user-guide/settings.md` 与 `docs/user-guide/troubleshooting.md`，说明 `@Parameter` 四项元数据、Spring 绑定名和 required 优先级、配置标记对 Param/Header 的作用及未支持边界；更新 `CHANGELOG.md` 与本 change 的 `verification.md`，仅记录已经实现和实际验证的行为。检查链接、示例和验证状态，并运行 `git diff --check`。
