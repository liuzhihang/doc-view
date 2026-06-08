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

## Markdown 和 Velocity

模板渲染可能在批量导出和预览刷新中反复执行。

- 模板变量要稳定，避免在模板层触发重型 PSI 查询。
- 批量导出时尽量复用已构建的 `DocView`。
- 用户模板错误应给出可理解提示，而不是吞掉异常。
- 示例 JSON 生成要控制递归深度和集合大小。
- 输出 Markdown 前后不应做大规模低效字符串拼接。

## 导出

导出可能涉及多个接口和文件写入。

- 遵守用户选择的导出路径和合并导出配置。
- 批量导出要避免重复解析同一类。
- 文件写入错误要报告路径和原因。
- 不覆盖用户文件，除非用户操作明确表示覆盖。
- 大批量导出需要考虑进度和取消能力。

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

## 性能验证

根据变更选择验证方式：

- PSI 解析变更：使用包含泛型、嵌套对象、递归引用和大量字段的样例。
- UI 变更：在 `runIde` 中验证打开、刷新、切换节点、关闭项目。
- 导出变更：验证单接口、整类、多类和合并导出。
- 网络变更：使用非生产 endpoint 或 mock，验证失败路径。
- 兼容性变更：运行 `verifyPlugin`。

文档-only 变更只需要文件存在性和 diff 范围检查。
