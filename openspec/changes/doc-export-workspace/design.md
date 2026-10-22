# Design

## Context

见 [proposal.md](proposal.md)。现有 `CatalogExportAction` 在 action 回调中调用 `docViewNode.docViewList()`，该路径会读取 PSI 并构建文档；`WindowExportAction` 把 `rootNode.docViewList()` 放入后台 `runTask`，但读取没有显式 read action。两者最终写文件时都使用 `FileUtil.writeToFile(..., true)`，因此“覆盖”确认后的行为是追加。当前 `MethodNode` 以业务名称作为导出目录/文件名，`DocViewService.buildDoc(class, null)` 还会为重复名称追加随机后缀，不能作为可重现的项目文档身份。

项目扫描已经按 `ModuleManager` 模块范围执行，Spring/Feign/Dubbo 候选来自现有 PSI finder；普通接口是否扫描受当前项目设置控制。页面正文由 `DocViewService.buildClassMethodDoc` 和 `DocViewData.markdownText` 构建，可继续使用当前版本的 Spring/Dubbo 用户模板。新的项目导出不使用 UI 树上可能过期的 PSI 缓存作为项目快照。

JetBrains 文档说明 IDE 可以复用现有文件/符号索引，并提供项目范围的 Find in Files；这支持先使用普通项目 `.md` 文件的方向，但不证明本插件生成的具体目录已经通过原生搜索验收，也不保证用户排除/搜索范围设置会包含它们：[Indexing and PSI Stubs](https://plugins.jetbrains.com/docs/intellij/indexing-and-psi-stubs.html)、[Search and replace a target within a project](https://www.jetbrains.com/help/idea/finding-and-replacing-text-in-project.html)。

## Goals / Non-Goals

**Goals:**

- 从当前项目的全部模块生成可重现的逐接口 Markdown 页面和索引。
- 页面可追溯到项目相对 Java 源文件；重载和跨模块同名接口有不同身份。
- 避免重复导出追加、静默覆盖用户修改或误删无关文件。
- 将 PSI 快照与文件写入分开，保持 EDT 响应并提供可取消、可恢复的导出过程。
- 使项目文档使用 IDEA 可处理的普通 Markdown 文件，不新增自定义索引。

**Non-Goals:**

- 改变 Spring、Feign、Dubbo 的候选识别或当前 Markdown 模板正文及模板变量。
- 导入、迁移、重命名或清理旧导出文件；识别或采用旧的业务名称文件。
- 自动删除失去当前来源身份的旧受管理页面；它们保留在磁盘但不再出现在当前索引。
- 构建 Wiki 页面生成/维护、向量存储、全仓库 AI 总结、自定义 IntelliJ index 或 MCP 工具。
- 保证 IDEA 搜索会包含用户排除的目录、忽略文件或搜索范围之外的文件。

## Decisions

### 1. 固定项目输出根目录，独立于临时导出

项目级动作只写项目根下的 `docs/doc-view/`，其中 `index.md` 为入口，`apis/` 保存接口页面。选择固定、可见且可纳入版本控制的普通 Markdown 目录，避免新增配置项或要求用户每次选择目录。文件选择器仍服务于当前单文档/选中节点导出，不更改其用途。

在读快照外解析真实项目根路径和输出路径，拒绝目标路径通过符号链接逃出项目根；拒绝输出根或预定目标不是普通文件/目录的情况。根目录中未与 Doc View 目标冲突的现有文件不参与本次操作。

### 2. 从模块范围扫描，按方法创建确定性快照

项目导出在后台读任务中枚举 `ModuleManager` 的全部项目模块，并沿用现有 module-scope、Spring/Feign/Dubbo finder、普通接口设置和 `DocViewUtils.isDocViewMethod` 过滤规则。启用普通接口后，仍保留框架 finder 对 Dubbo/Feign 的来源分类，再补充未被识别为这些框架的普通接口；注解类型不作为 API 接口。不要在 EDT 展开目录树或依赖它上一次刷新留下的 PSI。对每个有效方法单独调用对应服务构建文档，不使用会为类内重复业务名称添加随机后缀的类级 `buildDoc(..., null)` 路径。

稳定身份由版本号及模块名、相对项目根的正斜线源码路径、全限定类名、方法名和参数类型列表构成；每个 UTF-8 字符串以长度前缀编码，避免分隔符歧义。身份不含业务显示名、随机后缀、参数名或行号。FQN、虚拟文件或参数签名缺失/无效时跳过该项并计入结果。每次方法遍历检查取消状态，对重复来源身份去重，最终按身份字典序排序。

派生页路径固定为 `docs/doc-view/apis/<类名-方法名>-<身份 SHA-256>.md`；可读 slug 只用于显示，完整 identity hash 用于区分重载和同名模块方法。索引提供稳定身份、模块、类/方法签名、源码链接和页面链接；当前索引的纵向分组与隐藏身份元数据格式由后续独立的[索引可读性 change](../doc-export-index-readability/proposal.md)定义。链接使用规范化的相对路径，不写入本机绝对路径；源码链接定位到文件，不依赖易变化的行号。

读任务返回完全不持有 PSI 的不可变快照：identity、模块、签名、相对源码路径、来源框架、渲染后的 Markdown 和预定页面路径。`DocView` 本身持有 `PsiClass`/`PsiMethod`，不可传入后续 IO 阶段。渲染在读阶段结束前完成，以保证它使用同一份解析结果和当前项目模板；剩余索引组装、冲突检查和文件操作在 read action 外执行。

### 3. 保留模板正文，增加可验证的来源和所有权头

每个页面的有效 Markdown 正文由普通源码链接和现有 `DocViewData.markdownText(project, docView)` 组成，接口模板正文保持原样。文件顶部使用不可见 HTML 注释记录格式版本、完整稳定身份和后续正文 UTF-8 字节的 SHA-256；索引同样带所有权注释与正文哈希。hash 不覆盖自身的注释行，重写时先解析 marker，再对剩余字节验证。

此 marker 用于区分 Doc View 页面和用户文件，也用于确认用户没有在上次生成后手工修改。marker 缺失/损坏、正文哈希不匹配或路径类型不符均是冲突，不尝试猜测、接管、合并或覆盖。索引是当前接口快照的可读 Markdown 清单，不新增旁路 manifest、数据库或 IDE 索引。

### 4. 先整体预检，再暂存并逐文件替换；索引最后发布

writer 入口先检查任务取消状态及项目发布资格；若项目已关闭，则在解析输出路径、检查/创建目录或创建临时文件之前抛出取消，不能留下空的 `docs/doc-view/` 目录。此检查只覆盖写入尚未开始的关闭场景；暂存完成、进入发布前仍保留发布资格检查。磁盘阶段随后按确定性顺序执行：解析 `docs/doc-view/` 和当前文件列表；检查所有页面目标及 `index.md` 的所有权与哈希；任意冲突时在成品写入前取消整批操作并报告相对路径。通过预检后，将完整新页面和索引写入同目录临时文件，再用覆盖替换而不是 append 发布页面，最后发布索引。单文件替换保持文件系统原子性；临时文件名随机且不进入索引。

暂存期间继续检查取消；取消时删除本次临时文件，保持旧页面/索引不变。开始文件替换后不承诺因项目随后关闭而回滚已经开始的发布；索引最后发布，使其只有在所有页面替换成功后才表示新快照。若磁盘错误发生于发布中，停止后续页面、保留旧索引、报告已成功/失败的相对路径并清理剩余临时文件；所有已经替换的页面仍有独立所有权 marker，重试可完成收敛。不得报告整批成功。

导出不清理不在本次快照中的文件，包括旧来源身份页面和其它用户内容。旧页面不再列入新索引；维护者可在确认后手动清理。已存在的未标记旧导出，即使内容看似相同，也按用户文件冲突处理。

### 5. 用既有后台任务和普通文件索引

新 Tool Window action 提交 cancellable background task。项目索引尚未 smart 时不生成部分快照，改为禁用/提示稍后重试。PSI 枚举、文档构建和渲染在后台 read action 中完成，I/O 阶段只接收不可变字符串/路径数据。不得把磁盘写入包在 PSI read action 中，也不得在 action `update` 中扫描项目或渲染 Markdown。

普通 Markdown 放在项目可见目录，可被 IDEA 常规文本搜索按用户当前 scope 搜索；搜索范围、ignore/exclude 配置和索引状态仍由 IDE 决定。IU 2026.1 的原生验收发现，首次项目导出后 23 个接口、24 个文件均已写入磁盘，但 Find in Files 的 Project scope 搜索 `UserFacade#queryUserDateAccount` 返回 Nothing found；对照执行 Reload All from Disk 后，同一查询返回 3 个文件中的 4 处匹配（两个重载页面各一处，索引两处）。这支持“新输出未进入当前 VFS”是缺口来源，但该对照仍不能证明自动索引完成时序。

因此，导出 action 在成功发布或部分发布后，只要至少一个目标文件落盘，就对固定 `docs/doc-view/` 输出根执行本地 VFS 刷新，并在现有后台写入任务中调用，确保不在 EDT 或 PSI read action 内同步刷新。VFS 发现新文件后由 IDEA 现有文件索引机制处理；本插件不触发全项目刷新，不新增自定义索引。普通 Markdown 的搜索范围、ignore/exclude 配置和索引状态仍由 IDE 决定。该 native fail 与 Reload 对照是修复前的根因证据，不是修复通过证据；修复后需在全新输出目录和唯一新接口词上复验，不手动 Reload。

### 6. 修正旧批量导出的线程和 append 行为，不引入旧输出适配

`WindowExportAction` 和 `CatalogExportAction` 的批量路径均改为先取得后台 read-action 的不可变 Markdown 快照，再在 read action 外执行文件 IO。`WindowExportAction` 不再在后台 `runTask` 中无 read action 访问 `rootNode.docViewList()`；`CatalogExportAction` 不再在 EDT 构建 `docViewList()`。目标文件写入改为替换：用户仍先得到现有覆盖确认，确认后每个目标只有本次内容，拒绝确认则保持原字节不变。窗口/目录批量导出的范围、文件名、合并设置和单文档临时输出保持其既有目的，不在此处增加身份 marker、项目索引或旧版文件识别。

### 7. 验证策略

- 真实 Java PSI fixture 通过现有框架 finder 和 `DocViewService` 验证 Spring、Feign、Dubbo 快照、源码身份、参数重载和 golden 页面；按模块身份覆盖相同 FQN/签名，若当前轻量 fixture 不能创建多个模块，使用可控的 identity/input 边界测试并把多模块原生验收保留到手工项。
- 用临时项目文件树验证两次导出字节稳定、索引不重复、源码链接相对化、未标记/已编辑页面或索引阻止所有写入、无关文件及旧页面保留、取消清理临时文件、IO 故障报告和重试收敛。
- 对现有窗口/目录批量导出补回归，确认“确认覆盖”变成替换、“取消覆盖”保留原始字节。
- 构建 IU 2026.1 原生验收：从 Tool Window 导出多模块/重载 fixture，打开入口索引和页面、检查源码链接；再用 IDEA Find in Files 在 Project scope 搜索一个唯一接口词，并单独记录被排除或不在 scope 内的限制。单测/Verifier 不能替代该原生搜索验收。
- action 自动化测试在运行真实项目导出后检查项目根 VFS 中的 `docs/doc-view/index.md` 与接口页面可见，不调用全项目 refresh API；该测试证明 VFS 发现，不单独证明原生 Find in Files 索引完成。

## Risks / Trade-offs

- [模块名或项目相对源码路径变化会形成新的身份并留下旧页面] → 使用当前身份生成新页、索引仅列当前快照，不删除旧页；明确旧页需要人工清理。
- [文档目录可能已有同名用户页或用户编辑过上次结果] → 所有目标先做所有权 marker 与正文 hash 检查；任一冲突时整批预检失败，不接管文件。
- [发布中发生磁盘错误时少量页面可能已替换而索引未变] → 页面逐文件原子替换、索引最后发布、报告完成路径；页面 marker 让重试可识别并收敛。
- [项目 PSI/index 尚未就绪或项目规模大] → 在 smart mode 执行、每个模块/类/方法/页面阶段传播取消，并分阶段报告状态；不在 EDT 同步扫描。
- [其他进程在导出预检后同时改写目标或替换目录] → 发布前对暂存后的整批目标再检查，每个文件替换前重查目标和目录边界；跨平台文件 API 不提供覆盖整个多文件批次的原子 compare-and-swap，因此极窄的外部 check-to-move 竞态仍无法完全消除，不建设额外守护进程或跨进程锁。
- [IDE 搜索受用户范围和排除规则影响] → 只承诺普通 Markdown 具备进入现有项目文本搜索的条件；原生 Find in Files 验收单独记录，不引入自定义索引来掩盖未验证。
- [修正批量覆盖会让用户确认后的文件变为替换而非旧版追加结果] → 这是“覆盖”按钮的字面语义；在新版用户指南注明，取消仍保留文件，旧导出不迁移或自动删除。

## Migration Plan

无需持久化设置或旧数据迁移。新目录不存在时创建；目录已存在时只在所有 Doc View 目标都可证明为本插件未修改的页面后更新。现有任意位置的 `DocView.md`、业务名称 `.md` 和临时导出保持原样，不导入、不重命名、不删除。若用户拒绝冲突或希望恢复旧批量导出文件，须由用户自行备份/处理；新行为不提供旧版追加写法开关。

实现和验证通过后更新用户指南及 Markdown contract/golden；在用户批准该 change 后才 apply。按当前版本策略不为旧导出格式添加兼容分支。
