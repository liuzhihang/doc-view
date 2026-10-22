# Design

## Context

见 [proposal.md](proposal.md) 的动机。当前整类预览在 `PreviewForm.buildDoc()` 中将 `DocViewService.buildDoc()` 放入一个 read action；入口显式检查一次取消状态，随后 Spring/Dubbo 服务按方法构建文档，`ParamPsiUtils` 递归解析字段。递归深度已限制为 8，但上述受控循环没有显式取消检查（`PreviewForm.java`、`SpringDocViewServiceImpl.java`、`DubboDocViewServiceImpl.java`、`ParamPsiUtils.java`）。

`DtoSchemaCacheService` 按 role/type、项目级 PSI modification count、生成设置和模板版本构造 key，并在读写缓存时深拷贝 `Body`。`PreviewForm` 另有每个 popup 生命周期内的 Markdown 缓存。现有测试验证 Spring Markdown golden、索引扫描结果、模板/预览生命周期和目录刷新回调；没有耗时、分配或取消延迟测量，也没有 schema 缓存命中率证据。

`bodyCache` 是按完整版本 key 存储的 `ConcurrentHashMap`。生产路径会插入新 key；`invalidate(key)` 没有生产调用点，自动回收只发生在显式 clear 或项目服务释放时。因此不同 PSI/settings/template epoch 下的旧 `Body` snapshot 可能继续占用项目内存。该保留量需要用固定 schema、多次版本变化后的 entry/node 计数实测，并覆盖旧任务迟到写入。

核查位置：`PreviewForm.buildDoc()` 738–779 行；Spring/Dubbo `buildClassDoc()` 方法循环；`ParamPsiUtils.buildBodyParam()` 37–170 行；`DtoSchemaCacheService` 的 key、读写与复制 30–110 行；Tool Window 刷新状态机 103–178 行、`RootNode.updateNode()` 32–44 行和 `ModuleNode.updateNode()` 41–65 行。现有 `SpringMarkdownContractTest`、`ApiTreeRefreshTest` 等检查内容或生命周期，不含耗时分布、cache entry/node 留存或生成中途取消测量。

Tool Window 刷新是另一条重路径：`RootNode.updateNode()` 遍历项目 modules；每个 `ModuleNode` 默认通过注解索引查候选项，开启普通接口扫描时改用 `AllClassesSearch` 获取整个 module 的 classes 后筛选接口；随后 `ClassNode` 遍历方法。现有代码已在模块、候选类和类节点构建的外层循环检查取消，但 `ClassNode.updateNode()` 对 `psiClass.getMethods()` 的逐方法筛选和 `MethodNode` 构建仍缺少检查。一个方法很多的类因此会拖延取消/项目关闭响应；panel 在 `snapshot.updateNode(project)` 完成后才检查 indicator，虽不会发布被取消的快照，却不能及时中断该类内工作。测试侧单独测 `RootNode.updateNode()` 的扫描主体，原生验收另测从点击刷新到树更新的端到端时间；两项均与单类文档生成分开采集。

## Goals / Non-Goals

**Goals:**

- 先建立可重放、可读回的基线，分别观察整类生成、Markdown 渲染和目录刷新，区分冷/热 schema cache 与普通接口扫描设置。
- 让现有 Backgroundable/read-action 任务在方法、DTO 字段和目录遍历的受控边界协作响应取消，并阻止部分结果发布。
- 测量并释放旧版本 DTO schema snapshots，阻止较早 generation 的迟到写入重新保留已过期条目。
- 以 `DocView`、完整 Markdown golden 和目录成员集合/顺序验证优化前后的输出等价。
- 只有 profile/测量显示具体路径有稳定热点时，才对该路径作最小调整；没有明确收益时保留代码并报告测量结论。

**Non-Goals:**

- 不调整上传、导出、`CustomFileUtils` 文件预览或文件写入语义；这些由独立变更处理。
- 不更换、扩大或新增通用缓存，不提前修改 `DtoSchemaCacheService` 的 key 或深拷贝策略。
- 不加入生产计时日志、遥测、JMH 或其他依赖，不制定未经基线支持的毫秒 SLA。
- 不改变 PSI 扫描范围、普通接口设置语义、`DocView` 字段或 Markdown 内容。

## Decisions

### 分别测量生成、渲染和目录扫描

测试侧使用固定 Java fixture，至少包含 Spring Controller 与 Dubbo service 两种整类构建路径；固定方法数、DTO 字段数和嵌套深度，使用唯一业务名称以避免既有重名随机后缀影响输出对照。以 `DocViewService.buildDoc(targetClass, null)` 测整类构建，再对同一结果直接调用 `DocViewData.markdownText(project, docView)` 测模板渲染，避免 `PreviewForm` 的 per-popup Markdown cache 把第二阶段耗时隐藏。对 schema cache 分别清空后测冷路径，以及 PSI/settings/template 稳定后先预热再测热路径。重复测量并报告 p50/p95、fixture 规模、IDE/JBR、运行次数和 cache 状态；不把机器相关时间写成自动化测试的硬阈值。

Tool Window 目录刷新另用固定项目 PSI 单独测量，分别记录 `includeNormalInterface` 开关、module/class/method 数量和刷新分布。只测索引已就绪后的重复刷新，不把 IDE 首次索引时间混入插件扫描时间；若 native profiler 显示索引调用本身占主导，再另行评估公开、范围受限的扫描 API，不更换扫描语义。

现有 fixture/golden 用于输出一致性；新增的测量 harness 只位于 test source，不增加 runtime instrumentation 或时间阈值。基线在任何生产性能改动之前记录到该 change 的 verification 证据中，优化后在同环境、同 fixture、同测量流程下复测。

### 测量缓存并淘汰旧版本快照

先观察 `DtoSchemaCacheService` 在 cold/warm fixture 下对构建时间的影响，并记录重复 PSI/settings/template 版本后的 cache entry 与 `Body` node 数。全局 PSI modification count 可能在任意源码变更后令旧 key 不再命中，但 map 当前不会自动删除旧 key。基线后以单一当前 epoch 管理有效条目：出现新 epoch 时清除旧 snapshot，`putBody` 只接受当前 epoch，避免迟到旧任务重新插入过期项。保留现有 project scope 和手动 clear；不引入 LRU、通用 cache 框架或 runtime 命中计数器。

如果采样显示缓存命中节省的构建成本低于 `Body` 深拷贝成本，则在相同内容 contract 下评估减少重复复制的局部方案；不根据猜测改变 key 语义。

### 复用现有任务的取消上下文

沿用 `PreviewForm` 与 Tool Window 已有的 cancellable Backgroundable task 及其 `ProgressIndicator`，在类方法、递归 DTO 字段、项目模块/候选类以及 `ClassNode` 方法列表的受控循环中增加协作检查；目录节点逐方法检查必须发生在方法筛选/构造之前，使取消后不再访问后续方法。取消以 `ProcessCanceledException` 退出当前完整快照构建；面板仅在当前刷新完整成功后替换根节点，因此不得回投部分目录。测试用真实 PSI 构造 `ClassNode`，在首个命中方法的 `MethodNode` 构造期间由 test-only `Settings` service seam 取消 indicator，并断言只进入首个方法的过滤/名称解析、后续方法未被筛选、异常向外传播。取消可能在 `MethodNode` 构造完成并加入局部列表之前被读操作边界观察到，因此测试不要求未完成节点保留。此用例与 `ApiTreeRefreshTest` 的刷新发布边界共同覆盖取消不会替换当前根目录；不通过人为延时或对生产遍历逻辑的测试镜像制造取消。对于平台 `AllClassesSearch.findAll()` 内部是否及时响应取消，单独通过测量/profile 验证，不假设本地循环检查能中断平台 API。

### 完整 Spring 递归取消的生产链路用例

补充真实 PSI 的 `GenerationCancellationPublicationTest` 场景：通过生产 `PreviewForm.buildDoc()` 后台任务进入 `SpringDocViewServiceImpl.buildClassMethodDoc()`，让 response DTO 的真实嵌套字段包含一个唯一名字。测试只替换 project `Settings` service；其 `getExcludeFieldNames()` 返回测试集合，并在 `contains(唯一嵌套字段名)` 时记录已进入该递归字段检查、取消从 Spring service 当前任务捕获的 `ProgressIndicator`。触发点是现有 `DocViewUtils.isExcludeField()` 的字段检查，不新增生产回调或解析镜像。断言该字段被访问、indicator 已取消、Spring 方法构建未正常返回，Preview 显示取消终态且文档列表/当前文档/导出快照/Markdown cache 均未发布；查询该 response schema key 仍无缓存条目，因为 `cachedBody` 仅在递归 supplier 正常返回后写入。

该完整链路测试在唯一嵌套字段的排除检查期间取消，平台可能在 `isExcludeField()` 的读操作返回边界传播 PCE，也可能在该字段的下一次 `buildBodyParam` 检查传播。因此它证明取消发生在生产 Spring DTO 递归路径中并阻止预览与 schema 发布，但不声称取消发生在该字段 `Body` 入列之后。已有低层真实 PSI traversal 测试通过 `Body` 子列表插入事件证明首字段加入后取消会阻止后续字段解析；两者覆盖不同边界。若没有生产 hook 或生产列表注入，这个 full-chain seam 不用于断言递归列表的精确插入时刻。

### 先证据后选择优化点

若基线和 CPU/allocation sampling 未显示稳定可复现的热点，则不实施以降低耗时为目标的运行时优化；报告无证据支持的候选项。已确认的旧 schema 快照持续留存和取消不能及时响应属于缓存生命周期/取消正确性问题，仍须按对应 requirement 修复，不以发现耗时热点作为前置条件。若有耗时热点，限定到对应阶段，使用相同输入对照结果并检查较小 fixture 未回退。只有改善超过本机重复运行波动且 p95 不变差时，才记录为性能优化；本地测量结果不转写为跨机器 SLA。

## Risks / Trade-offs

- [JVM 预热、索引状态和后台 IDE 活动会影响耗时] → 固定 PSI/settings/template、区分 cold/warm、预热后重复采样，并将环境元数据与分位数一起保存；native 首次索引不计入插件扫描。
- [不同 epoch 同时在途的 generation 可能竞态回填旧项] → epoch 更新与 cache put 使用可验证的原子边界；测试在版本切换后完成旧任务并确认旧项不复活。
- [项目级 modification count 使热缓存数据容易被编辑污染] → 热路径测量期间不改 PSI，并在每轮前确认设置、模板和 cache 状态；不据此调整 key 的业务语义。
- [取消检查本身会给字段级循环增加少量开销] → 用同一 fixture 比较前后分位数和输出；只有取消延迟有可复现问题且开销可接受时保留检查。
- [平台搜索 API 可能占目录刷新大部分时间] → 把 `AllClassesSearch.findAll()` 与后续候选处理分开采样，不以更换 API 作为默认方案。

## Migration Plan

1. 先增加固定测试 fixture/测量 harness，记录生成、渲染、目录扫描的基线以及当前取消延迟；不改生产性能路径。
2. 依据基线和采样结果决定是否实施以降低耗时为目标的局部优化；若无稳定热点，只跳过此类优化。缓存旧快照留存和取消响应属于正确性边界，按对应测试与 requirement 处理。
3. 对确认的路径实施最小改动，补充取消/丢弃结果测试和输出等价测试，再用相同基线复测。
4. 运行目标测试、完整 Gradle 门禁和 IU 2026.1 原生预览/刷新取消场景；验证未执行项单独报告。

无需持久化数据迁移。若优化引入回归，回滚该局部运行时改动，保留基线 fixture 与记录供后续决策。
