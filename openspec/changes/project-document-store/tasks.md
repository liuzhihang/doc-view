# Tasks

## 1. 结构快照与格式 v1

- [x] 1.1 将现有 `DocView` 单次解析投影为 immutable、PSI-free 的接口详情 DTO，并让项目 snapshot 同时生成既有 Markdown 与同 stem 的 `.json` 详情；解析前提交 IDE `Document` 到 PSI，不自动保存，确保 Markdown、详情与 `source.capturedContentSha256` 来自同一份实际捕获文本；用 Spring、Feign、Dubbo、参数/请求体/响应体（含 since/version/集合与Map标记）及无 PSI 引用断言验证：`./gradlew test --tests '*ProjectDocumentSnapshotTest' --tests '*ProjectDocumentDetailProjectionTest' --tests '*ProjectDocumentMarkdownTest' --tests '*ProjectDocumentExporterTest' --tests '*ProjectDocumentExportActionTest'`。
- [x] 1.2 按 sibling `format.md` 实现 index/detail schema v1、确定性序列化、RFC 8785 envelope（排除 integrity）完整性 hash、稳定 `snapshotId` 与 MCP design 大写错误码；确认 Markdown/detail 路径相对 `docs/doc-view/`、source path 相对项目根，详情 JSON 与既有 Markdown 共享稳定 stem；用 JSON golden、round-trip、字段顺序及篡改用例验证：`./gradlew test --tests '*ProjectDocumentFormatTest'`。
- [x] 1.3 写入并验证 source、producer、template/settings fingerprint 与 resource bounds；确认 `source.capturedContentSha256` 对应实际捕获的 Document 文本、index 顶层 `hasUnsavedDocuments` 明确记录本次捕获的未保存编辑，同源多方法只计算一次 hash，凭据与绝对项目路径不写入 JSON，超限时不提交新索引：`./gradlew test --tests '*ProjectDocumentSnapshotTest' --tests '*ProjectDocumentSnapshotFingerprintTest' --tests '*ProjectDocumentFormatTest'`。
- [x] 1.4 核对 sibling `format.md`、schema 和最小合法/无效样例均与本 change spec 一致，且 [`docs/mcp-format.md`](../../../docs/mcp-format.md) 可达；运行 `openspec validate project-document-store --strict` 检查链接与 artifact 内容。
- [x] 1.5 修复项目目录 Dubbo 候选发现：按 Dubbo 注解 FQN、注解实现类接口继承闭包、XML 声明模块依赖范围解析，并按接口源码所属模块去重归档；覆盖 XML-only、实现类注解、同模块重复、取消和 api/provider 同 FQN 隔离：`./gradlew test --tests '*AnnotationIndexCompatibilityTest' --tests '*DubboMultiModuleCatalogTest'`。

- [x] 1.6 用公开 XML PSI 替换自有 DOM model 及注册，保持 Dubbo 识别、目录归属与定义导航，补齐空属性、标签边界、未保存编辑、取消和 consumer 停止测试；全量构建与两个精确 build 的 Verifier 通过后，原生触发 XML 功能并禁用插件，用日志/GC 复验类加载器释放。普通窗口关闭不代替热卸载验收。

## 2. 只读项目索引服务

- [x] 2.1 实现项目绑定的索引发现、schema/自完整性验证、project-scoped immutable cache，以及首次磁盘加载为 `unknown` 的状态；覆盖缺失、损坏、未知 schema、重开语义和双项目 identity 重合：`./gradlew test --tests '*ProjectDocumentIndexServiceTest'`。
- [x] 2.2 实现 NFKC/`Locale.ROOT` 搜索文本归一化、空白 AND 分词、字段子串 OR、`module`/`framework`/`httpMethod` 规范值精确 filters、空查询、默认/最大 limit 与稳定 identity 排序；搜索摘要在可用时含 `httpMethod` 和 `httpPath`，`httpPath` 参与关键词匹配；覆盖 query 长度、词数、条目数和 timeout 边界：`./gradlew test --tests '*ProjectDocumentIndexServiceTest'`。
- [x] 2.3 实现 snapshot/query/filter 绑定的分页 cursor 和 `CURSOR_STALE`；覆盖连续翻页不重不漏，以及 snapshot、query、filter 改变时拒绝复用：`./gradlew test --tests '*ProjectDocumentIndexServiceTest'`。
- [x] 2.4 实现按索引 entry 读取详情和 Markdown 的同 stem 路径、identity、schema、payload 与 raw-byte hash 校验；Markdown/detail 路径相对 `docs/doc-view/`，source path 相对项目根；覆盖缺页、改写、越界、符号链接和 PSI-free 返回；恢复后仅清除同一资源路径的完整性错误，并防止较早成功读取清除较新失败：`./gradlew test --tests '*ProjectDocumentIndexServiceTest'`。
- [x] 2.5 实现已观察 source/module-root/settings/template epoch 变化后的 stale 与重开 unknown；查询仅使用索引和运行态 epoch，不调用 PSI 或项目文件扫描：`./gradlew test --tests '*ProjectDocumentIndexServiceTest'`。
- [x] 2.6 将项目内容根内 Java 与 XML Document 变化纳入当前会话 freshness 跟踪；`hasUnsavedDocuments` 保守覆盖这两类生成输入且排除 `docs/doc-view/`，无需改变 schema v1；覆盖项目根内和外部 content root XML 编辑、未保存 Dubbo XML 生成标记及不自动保存，并在一个已提交但未保存输入之后仍拒绝后续未提交输入：`./gradlew test --tests '*ProjectDocumentIndexServiceTest' --tests '*ProjectDocumentSnapshotTest'`。

## 3. 受保护的可取消发布

- [x] 3.1 扩展项目文档 exporter 一次 preflight `.md`、detail `.json`、`index.md`、`index.json`，验证既有所有权/hash 后暂存完整输出，并按 API 文件、`index.md`、`index.json` 顺序逐文件原子替换；覆盖 JSON 冲突、用户编辑和 retry 收敛：`./gradlew test --tests '*ProjectDocumentExporterTest'`。
- [x] 3.2 增加同文档根的跨进程 writer 排他和同进程读写协调；后台以受期限和取消约束方式取得锁，不无限阻塞 EDT；用两个相同根的独立 writer 验证后启动者收到 `BUSY` 且文件不变，并验证不同根可并发：`./gradlew test --tests '*ProjectDocumentWriterLockTest' --tests '*DocumentStoreLocksTest'`。
- [x] 3.3 在每个 move 边界检查取消、project disposal 及捕获后的来源 epoch 变化，显式报告首次 move 后的 `PARTIAL_PUBLICATION`、`SOURCE_CHANGED`、清理剩余 temp，并保证最终 `index.json` 原子替换的 commit-wins 语义；覆盖页面、详情、`index.md`、最终索引前后取消及 disposal：`./gradlew test --tests '*ProjectDocumentCancellationPublicationTest' --tests '*ProjectDocumentExporterTest'`。
- [x] 3.4 将 `PARTIAL_PUBLICATION`、部分 I/O 失败及提交成功结果传到项目导出动作；确认普通 `ProcessCanceledException` 静默路径不会吞掉 partial 结果，且只有在文件落盘后对 `docs/doc-view/` 做局部 VFS refresh：`./gradlew test --tests '*ProjectDocumentExportActionTest' --tests '*ProjectDocumentCancellationPublicationTest'`。

## 4. 文档库 UI 集成

- [x] 4.1 按 `project-document-library-ui` spec 区分刷新接口目录、生成/更新、搜索、打开 Markdown、设置与清缓存入口，并使用统一动作名称/平台图标；新增相应 action/layout regression tests：`./gradlew test --tests '*ProjectDocumentLibraryUiTest' --tests '*DocViewActionLayoutTest' --tests '*ApiTreeRefreshTest'`。
- [x] 4.2 覆盖 UI 的无索引、载入、无结果、stale/unknown、partial、writer busy、详情校验失败和取消状态；确认搜索/打开只查询落盘快照、不触发生成，并在真实工具窗口面板上断言可见状态与操作结果，包括恢复原字节后重新打开并清除对应错误：`./gradlew test --tests '*ProjectDocumentLibraryUiStateTest' --tests '*ProjectDocumentPartialLibraryUiTest' --tests '*ProjectDocumentIndexServiceTest'`。

## 5. 整体验收与性能基线

- [x] 5.1 用记录环境信息的 10,000-entry fixture 测量 warm search p95、cold index load 和 query cancel p95；另用合法近上限索引确认工作线程进入 `ProjectDocumentFormat.readIndex` 后再取消；记录实际采样并对照 200 ms、2 s、500 ms 目标，不把 light fixture 数据写成 native IDE 结果：`./gradlew test --tests '*ProjectDocumentStorePerformanceTest'`。
- [x] 5.2 运行项目文档 store、导出、UI 与生命周期定向回归，然后对完整当前源码运行 `./gradlew test`；保留失败、跳过和未执行项的真实状态。
- [x] 5.3 对当前源码按发布清单运行 `./gradlew verifyPluginProjectConfiguration`、`./gradlew buildPlugin`、`./gradlew verifyPlugin`，并记录目标平台与报告；确认文档库实现没有新增 production dependency。并行的获准基线 change 负责 `sinceBuild` / Java 25 / 构建工具升级，文档库验收必须记录实际使用的基线，不混用261与263证据。
- [ ] 5.4 按发布清单的原生矩阵，在固定官方 IU 263 隔离实例安装最终 `buildPlugin` ZIP（或使用 `./gradlew runIde`）完成 UI/项目生命周期烟测：新旧项目发现、生成/更新、搜索/分页、详情与 Markdown 打开、外部改写 fail-closed、取消/partial/retry、项目关闭/重开 freshness；记录实际 IDEA build、profile/fixture、结果和未覆盖项。只有实际启动的目标实例可计入验收，不把未运行的 `runIde` 记为通过。
- [x] 5.5 运行 `openspec status --change project-document-store`、`openspec validate project-document-store --strict` 和 `git diff --check`；核对 change 仅覆盖获准 artifacts、文档 links 有效、各 task 状态与真实实现/验证证据一致，并将未完成的原生验收保留为未勾选。
