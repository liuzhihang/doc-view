# Performance Guide

Doc View 运行在用户 IDE 内，性能问题会直接影响编辑体验。任何涉及 PSI、UI、模板、导出、上传或缓存的变更都必须参考本文档。

## 总原则

- 不在 EDT 上执行长耗时任务。
- PSI 读取必须遵守 IntelliJ read action 规则。
- PSI 写入必须通过 write command。
- 避免对整个项目做无界扫描。
- 网络请求、导出和模板批量生成必须可取消或至少可反馈。
- 异常输入不能导致 IDE 卡顿、死循环或内存持续增长。

## PSI 读取

PSI 是性能敏感区域。变更 `SpringPsiUtils`、`DubboPsiUtils`、`ParamPsiUtils`、`CustomPsiUtils` 等工具时：

- 限定分析范围到当前类、当前方法或用户选择的模块。
- 避免在循环中重复解析同一类型、注解或继承结构。
- 对递归对象、泛型嵌套和循环引用设置深度或已访问集合。
- 不在 action `update` 中做重型 PSI 分析。
- 在 Dumb Mode 下确认能力是否可用；不能安全运行时应降级或隐藏入口。

## PSI 写入

写入 Javadoc、注解或编辑器文本必须通过 IntelliJ 写入模型：

- 使用 `WriteCommandAction`。
- 写入前确认 `Project`、`Editor`、`PsiElement` 和 containing file 仍有效。
- 写入后只格式化必要范围。
- 不把生成或格式化扩大到整个文件，除非 contract 明确要求。

## EDT 和 UI 响应

UI 层包括 tool window、preview、settings、parameter editor 和 action。

- EDT 只做 UI 状态更新和短逻辑。
- 解析、导出、上传、批量生成应放到 background task。
- 长任务需要进度提示或明确用户反馈。
- action `update` 应保持轻量，只读取必要状态。
- UI 组件需要处理项目关闭、文件失效、无编辑器、无选中节点等状态。
- Doc View 内部后台扫描、渲染和导出任务应优先通过统一 helper 执行，确保具备 read action、取消、project disposal 检查和 EDT 结果回投。
- 不在后台任务内部再启动不受 `ProgressIndicator` 管理的 pooled thread；否则外层任务可能提前结束，取消状态也无法可靠传递。
- Preview 和参数编辑器应先创建 UI，再后台构建 `DocView` 或参数树；EDT 上只展示 loading、应用列表、更新 editor/html 内容。

## Markdown 和 Velocity

模板渲染可能在批量导出和预览刷新中反复执行。

- 模板变量要稳定，避免在模板层触发重型 PSI 查询。
- 批量导出时尽量复用已构建的 `DocView`。
- 用户模板错误应给出可理解提示，而不是吞掉异常。
- 示例 JSON 生成要控制递归深度和集合大小。
- 输出 Markdown 前后不应做大规模低效字符串拼接。
- Preview 中包含多个 `DocView` entry 时，应优先延迟渲染 Markdown：只在用户选择或 copy/export/upload 明确需要时渲染对应 entry。
- 已渲染 Markdown 可以按当前 `DocView` 与 template/settings/schema 版本 memoize；源码、settings 或 template 变化后必须失效。

## 导出

导出可能涉及多个接口和文件写入。

- 遵守用户选择的导出路径和合并导出配置。
- 批量导出要避免重复解析同一类。
- 文件写入错误要报告路径和原因。
- 不覆盖用户文件，除非用户操作明确表示覆盖。
- 大批量导出需要考虑进度和取消能力。
- 文件选择和覆盖确认保留在 UI 侧；Markdown 渲染、批量文件写入和 payload 准备放到可取消后台任务中。

## 网络集成

YApi、ShowDoc、YuQue 上传必须避免阻塞 IDE。

- HTTP 请求要设置合理超时。
- 请求失败要区分网络错误、认证错误、参数错误和服务端错误。
- 日志和通知中不得暴露 token。
- 上传前尽量在本地构造完整 payload，减少远端交互次数。
- 重试必须谨慎，避免重复创建远端文档。

## 缓存

缓存可以改善体验，但必须可清理、可失效。

- 缓存 key 应包含项目、模块、类、方法或配置版本等必要维度。
- 配置变化后必须考虑缓存失效。
- tool window 的 clear cache 操作应覆盖用户可感知缓存。
- 不缓存 PSI element 的长期引用，除非确认生命周期安全。
- DTO schema cache 使用 project scope。推荐 key 维度包括 schema role、class/method/type identity、PSI modification count、schema 相关 settings hash 和 template hash。
- DTO schema cache 应保存派生数据或不带 PSI 引用的 snapshot；参数编辑器等需要写回 PSI 的场景应使用实时 PSI 构建结果。
- PSI modification count、settings/template hash 变化会让旧 key 自然 miss；显式 clear cache action 应调用 project cache clear。
- Project disposal 后不得保留 schema cache entry 或长期 PSI 引用。

## DTO 递归和深度

DTO schema 和示例 JSON 生成必须对异常输入有边界。

- 递归解析上下文应记录当前深度、最大深度、已访问 effective type path、泛型替换和 request/response role。
- 直接对象循环、collection element、map value、array component 和泛型替换后的类型都必须参与循环检测。
- 达到最大深度或检测到循环时，保留当前字段元数据，但停止展开 children。
- 默认深度限制应覆盖常见嵌套 DTO，同时避免 stack overflow、无限循环和 IDE freeze。

## 性能验证

根据变更选择验证方式：

- PSI 解析变更：使用包含泛型、嵌套对象、递归引用和大量字段的样例。
- UI 变更：在 `runIde` 中验证打开、刷新、切换节点、关闭项目。
- 导出变更：验证单接口、整类、多类和合并导出。
- 网络变更：使用非生产 endpoint 或 mock，验证失败路径。
- 兼容性变更：运行 `verifyPlugin`。
- 启动验证：`runIde` 打开项目后确认没有全项目扫描、DTO schema 预热、Markdown 预渲染、export、upload 或 cache 预加载。
- EDT 验证：打开 preview、参数编辑器、tool window refresh、批量 export/upload 时确认 UI 可继续响应，并可取消后台任务。
- 缓存验证：同一 DTO 重复 preview/export/upload 复用 schema；修改源码、settings/template 或执行 clear cache 后重新生成。
- 递归验证：使用直接自引用、`List<Self>`、`Map<String, Self>`、嵌套泛型和超过深度限制的 DTO，确认不会卡死或 stack overflow。
- 延迟渲染验证：class preview 打开后只渲染选中 entry，切换 entry 后生成 Markdown；copy/export/upload 使用的 Markdown 与旧 eager 路径保持一致。

文档-only 变更只需要文件存在性和 diff 范围检查。
