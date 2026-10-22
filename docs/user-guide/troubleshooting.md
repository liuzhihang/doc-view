# 故障排查

配置入口见[设置说明](settings.md)，模板语法和变量见[模板教程](templates.md)，平台字段见[平台集成](integrations.md)。

## 设置已修改但文档没变化

1. **确认项目和保存状态。** 在当前项目的 **Settings / Preferences → Tools → Doc View** 修改并点击 Apply 或 OK。不同项目的设置分别保存。
2. **重新生成结果。** 已生成、导出或上传的 Markdown 不会因设置变化而自动改写。重新打开预览，再按需要复制、导出或上传。
3. **检查模板。** 确认模板已 Apply，再生成新文档。需要撤销草稿或恢复内置模板时，先区分[Revert changes 与恢复默认模板](#模板-reset-后自定义内容异常)。

仅要重新扫描侧栏目录时，使用工具窗口的刷新按钮。刷新目录不会重写已导出的 Markdown。

## 侧栏没有接口或普通接口

侧栏按项目模块扫描。先在目标 Java 类或方法上使用右键 **Doc View** 验证入口，再检查：

- Spring 类需被识别为 `@Controller`、`@RestController` 等支持类型，方法需有当前支持的映射注解，例如 `@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping`、`@PatchMapping` 或 `@RequestMapping`。
- Feign、Dubbo 入口需符合插件当前识别规则；指南中的[源码入口示例](index.md#支持的源码入口)可用于核对形态。
- 普通接口默认关闭。在 Doc View 根设置页开启“普通接口识别与扫描”后 Apply，再刷新目录。它与编辑器行标记独立；无需为了扫描普通接口开启行标记。
- 当前扫描不代表依赖库、外部模块或任意接口都能生成完整 API 文档。检查类所在模块是否属于当前项目，及 IDE 是否已完成项目索引和依赖解析。

## 标题是 `DocView`、接口名为空或太短

先区分类文档标题、接口名和接口描述：

- **类标题**：`@DocView.Title` → 类 Javadoc → 类名 → 全类名 → 固定兜底 `DocView`。出现 `DocView` 通常表示前面的来源都没有值或已关闭。
- **接口名**：`@Operation.summary` → `@ApiOperation.value` → `@DocView.Name` → 方法 Javadoc 第一行 → Java 方法名。Javadoc 名称最多 15 个字符；空白或无法求值的 Swagger 值会被跳过。
- **接口描述**：先取已启用 Swagger 属性，再取方法 Javadoc。`description` 或 `notes` 为空白、缺省或无法解析时继续回退，没有可用来源才为空。字符串常量按实际值输出，保留有效文字中的空格和引号；若显示的是常量名，确认依赖解析完成后重新生成。

这些来源开关和次序见[设置说明](settings.md#文档标题)。模板可以隐藏或重排字段；排查模板表现时先用内置模板作为对照，但保留自定义内容时请先备份。

## 必填标记与预期不同

先看出问题的是字段还是直接方法参数，它们的判断顺序不同：

- **实体字段**：`@Schema(requiredMode)` 为 `REQUIRED` / `NOT_REQUIRED` 时优先；`AUTO`、未声明或无法解析时才检查 Validation/Lombok、Swagger 2 `required = true` 和启用的 `@DocView.Required`。旧 `@Schema(required = true/false)` 不再读取。
- **直接方法参数**：按 Spring 绑定与校验规则处理，不能照搬字段规则。`@Nullable` 和 `@Schema(nullable = true)` 都不决定必填。

完整优先级见[设置说明](settings.md#必填与字段名称)。

如果参数编辑器提示 Schema 缺少 `requiredMode`，在目标项目升级 `swagger-annotations` 到 2.2.5 或更高版本并等待 IDEA 解析依赖后重试。本次确认不会修改任何字段源码，弹窗保留草稿；此提示不是旧写法自动转换功能。

## 预览或生成结果出现 `ERROR`

`ERROR` 是 `VelocityEngine.evaluate()` 返回 `false` 时使用的固定文本，并不表示所有模板异常都会这样显示，也不保证附带模板行号。

1. 先备份 Spring、Dubbo 模板，再检查最近修改处是否有未闭合的 `#if` / `#foreach`、拼错变量或语法错误；对照[模板教程](templates.md)的变量清单。
2. 用可恢复的副本逐段定位。Revert changes / Reset 会放弃未应用的编辑；“恢复默认模板”会替换两份模板草稿，执行前先另存要保留的内容。
3. 模板页的固定示例预览会单独显示渲染失败原因；Velocity 提供行列时会显示位置。固定样例通过不代表任意项目输入都能渲染成功。

## 模板 Reset 后自定义内容异常

**Revert changes**（部分 IDE 页面称 Reset）恢复到最近一次 Apply/OK 保存的两份模板。Cancel 丢弃未应用草稿，但保留此前已 Apply 的修改。模板保存与重启的历史检查见[验证记录](../../openspec/changes/fix-template-settings-lifecycle/verification.md)。

**恢复默认模板** 是另一个操作：同时替换 Spring 和 Dubbo 的当前草稿，Apply 后才保存。若原自定义模板尚未被覆盖保存，可用 Revert changes 恢复；若已经保存默认内容，需要从自己的备份恢复自定义文本。

## Clear Cache 与刷新有什么区别

工具栏和目录菜单的 **Clear Cache** 只清当前项目 DTO schema 的内存缓存。它保留 Scratch 文档、打开的编辑器和项目文件；没有选中目录节点时也可执行。它不会重新扫描接口、重写 Markdown 或显示删除确认框。

重新扫描用 **Refresh**；更新文档则重新生成或导出。旧版 Clear 曾删除生成的 Scratch 文档，新行为不会恢复先前已删除的手写内容。

## 平台上传配置不生效或跳到了错误设置页

上传只使用当前项目的平台配置。核对字段并点击 Apply/OK；YApi 的 Project ID 必须为正数或留空。字段说明见[平台集成](integrations.md)。

语雀缺少配置时会打开 YuQue Settings；配置检查失败后不会继续发送上传请求。凭据保存在 PasswordSafe 中：留空表示保留，明确勾选清除并 Apply 才删除；保存失败时草稿保留。升级后需重新填写凭据，插件不迁移旧 XML。旧 XML 或备份可能仍含历史明文，详见[凭据操作说明](integrations.md#token-与本地保存)。

错误提示不包含原始异常、响应正文或文档对象；YApi 仍通过请求参数发送 Token。分享诊断信息前，移除凭据、私有地址和业务数据。

## 刷新仍看不到修改后的接口

刷新只重新扫描目录树，不会替你编辑源码或更新已导出的文件。确认目标类和方法满足支持条件、改动已保存、项目属于当前模块范围；重新执行 Doc View 生成，再检查新结果。若只改了标题/注释但导出的 `.md` 仍旧，应重新导出到新目录或按提示确认覆盖，避免把旧文件当成新生成结果。

## 上传超时、跳转或取消后的结果

平台地址必须指向最终 API 基址；插件不跟随重定向，也不自动重试。连接/连接池等待上限为 10 秒，socket 等待数据上限为 30 秒。失败通知不显示响应正文或 Token；不要把含凭据的 URL 或网络日志发到 issue。

语雀只有在查询返回真实 HTTP 404 时才会创建文档；权限失败、限流、服务器错误或无效响应都会停止该篇上传。出现超时或取消时，先去目标平台核对，因为远端可能已经写入；本地停止不等于远端回滚。批量取消后不再开始下一篇，已完成项保留。详见[平台集成](integrations.md#网络请求与错误)。

## `@Parameter` 没有覆盖名称或必填

按以下顺序检查：

- **名称**：Spring `name` / `value` 优先于 `@Parameter.name`；两个 Spring 别名冲突时回退到 Java 参数名。
- **必填**：配置中的正向 Validation/Lombok 标记优先，其次是 Spring `required` 或有效 `defaultValue`，最后才读取 `@Parameter.required`。
- **适用范围**：对象展开字段、`@RequestBody`、方法级容器及复合 schema 不读取这项 `@Parameter` 支持。

完整顺序见[设置说明](settings.md#openapi-3-方法参数说明)。

## 项目导出提示冲突或部分更新

`Export Project Docs` 会检查 `docs/doc-view/` 的索引和目标页面。未标记文件、手工改动的生成页或同名目录都可能造成冲突。备份要保留的内容，再按提示处理路径；手写说明应放在单独文件，不能期待它与生成内容自动合并。

磁盘故障可能造成部分页面已更新、索引仍旧。通知会列出成功、失败及未清理的临时路径。检查空间、权限和目录状态后重试；这不是整批事务。不要自动删除所有旧页。详见[项目文档说明](index.md#项目内-api-文档与-ai-检索)。
