# Tasks

## 1. 建立可重复基线

- [x] 1.1 准备固定的 Spring Controller、Dubbo service 与 DTO fixture，覆盖小/中/大方法数以及不同字段数和递归深度，使用唯一业务名称以避免随机重名后缀干扰；用真实 Java PSI 构建 `DocView`，并确认每个 fixture 的方法、字段和层级计数可读回。
- [x] 1.2 增加 test-only 测量 harness，分别测整类 `DocViewService.buildDoc()` 与 `DocViewData.markdownText()`；冷缓存在计时外清空，热缓存在固定 PSI/settings/template 下预热，记录运行环境、7 次样本的 p50/p95，并确认重复生成与渲染结果一致。
- [x] 1.3 用固定项目 fixture 分开测项目目录扫描，记录 module/class/method 数量、普通接口扫描开关和刷新分布；索引就绪后重复运行，并断言预期识别类及普通接口开关结果。
- [x] 1.4 对固定 schema 反复变更 PSI/settings/template 并重新生成，记录 `bodyCache` entry 数和其中 DTO 节点规模；安排旧版本生成晚于新 epoch 完成，确认能复现或排除过期 snapshot 留存/回填风险，并把基线结果写入本 change 的 `verification.md`。

## 2. 按证据修复缓存与取消边界

- [x] 2.1 读取第 1 组基线和 profiler 结果，仅对稳定热点实施一个局部优化；若没有超过测量波动的明确热点，则记录无证据支持运行时优化的结论并保持生成/扫描算法不变。
- [x] 2.2 当缓存保留测试证明旧 epoch 条目会累积时，令 schema cache 仅保留当前版本可用的 snapshots，并阻止迟到旧版本任务回填；测试覆盖重复 PSI/settings/template 变化、当前 epoch 命中和旧 epoch 完成顺序，确认 retained entry/node 数稳定。
- [x] 2.3 在整类方法构建、DTO 字段递归和项目目录的受控遍历边界传播当前取消状态；目录中的 `ClassNode` 必须在每个方法筛选/构造前检查取消。新增真实 PSI `ClassNode` 用例：test-only `Settings` service seam 在首个命中方法的 `MethodNode` 构造期间取消 indicator，断言只进入首个方法的过滤/名称解析，后续方法未被筛选/构造且 PCE 向外传播；不要求可能被中断的局部 `MethodNode` 构造完成或留存。与 `ApiTreeRefreshTest` 的完整快照发布边界一起确认取消不替换已发布目录。现有方法级 Spring/Dubbo 测试验证首个完整方法后取消会阻止整类列表返回；低层 DTO 递归 fixture 验证首个嵌套字段入列后取消会阻止后续字段解析。真实 Spring PSI 的 `GenerationCancellationPublicationTest` 用例通过 project `Settings.getExcludeFieldNames().contains(唯一嵌套字段名)` 在递归字段检查期间取消，并验证当前 endpoint 的方法构建未返回、生产 Preview 进入取消终态、无部分文档发布且 response schema 未入缓存；该 fixture 只有一个 endpoint，因此不据此声称后续 endpoint 被阻止。此触发发生在字段过滤检查期间，不证明目标 `Body` 已入列，低层 traversal 用例负责验证入列后的下一字段边界；该 full-chain 用例不得加生产 hook 或被描述为精确的 post-insertion 取消。Round43 targeted 和 Round44 全量测试已通过；Round45 验证了旧 render cancel 回调不覆盖较新预览的文本、Markdown 快照与当前文档。真实 IU 2026.1 目录扫描取消原生验收已完成，证据见 `verification.md` 最新章节；整类 Spring/Dubbo 预览的原生耗时与取消体验仍由 3.3 验收。
- [x] 2.4 对受影响的 Spring、Dubbo、缓存冷/热、Markdown golden 和目录 fixture 运行 `./gradlew test --tests '*GenerationPerformance*' --tests '*SpringMarkdownContractTest' --tests '*DocViewServiceDuplicateNameCompatibilityTest' --tests '*ApiTreeRefreshTest' --rerun-tasks`，确认输出与目录语义保持稳定。

## 3. 集成验收

- [x] 3.1 在相同环境/fixture 上复测基线 workload，比较优化前后的分阶段 p50/p95 与测量波动；只有提升超过波动且 p95 未退化时才报告性能改善，并记录实际结果和未覆盖场景到 `verification.md`。
- [x] 3.2 运行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`，确认自动化门禁通过且插件包不包含测试 fixture/harness。
- [ ] 3.3 使用 IU 2026.1 原生实例验收整类 Spring/Dubbo 预览与 Tool Window 刷新；在生成/刷新中取消并确认无部分结果，记录实际耗时范围、响应状态和环境，不把 light fixture 结果替代原生体验验收。
