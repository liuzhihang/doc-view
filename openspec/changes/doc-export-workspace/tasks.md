# Tasks

## 1. 项目 PSI 快照与稳定身份

- [x] 1.1 定义不持有 PSI 的项目导出快照和 canonical identity；对同路径重载、不同模块同 FQN/签名、行号变化、参数类型和缺失 FQN 编写身份契约测试，并运行 `./gradlew test --tests '*ProjectDocumentIdentityTest' --rerun-tasks`。
- [x] 1.2 在后台 read action 中遍历当前项目全部模块，复用现有 Spring、Feign、Dubbo 与普通接口筛选规则并逐方法构建文档；用真实 Java PSI fixture 验证来源路径、支持方法、去重和取消，并用项目导出 action 验证 Dumb Mode，运行 `./gradlew test --tests '*ProjectDocumentSnapshotTest' --tests '*ProjectDocumentExportActionTest' --rerun-tasks`。
- [x] 1.3 在离开 read action 前渲染当前 Spring/Dubbo 模板并构造普通链接快照；用固定模板和 golden 验证逐接口 Markdown 正文、模板变量及相对源码路径，运行 `./gradlew test --tests '*ProjectDocumentMarkdownTest' --rerun-tasks`。

## 2. 文档目录、索引与安全写入

- [x] 2.1 在 `docs/doc-view/apis/` 生成由完整 identity hash 决定的页面路径，并生成排序稳定的 `index.md`；用临时项目验证不同模块/重载链接、字节稳定及重复导出不追加，运行 `./gradlew test --tests '*ProjectDocumentExporterTest' --tests '*ProjectDocumentIdentityTest' --rerun-tasks`。
- [x] 2.2 为页面和索引加入所有权 marker 与正文 hash，预检整批目标后才写文件；用未标记文件、被编辑页面、被编辑索引、无效 UTF-8 页面/索引和目录冲突验证任一冲突均不改动任何成品，且无关文件保持原字节，运行 `./gradlew test --tests '*ProjectDocumentExporterTest' --rerun-tasks`。
- [x] 2.3 暂存完整新页面，取消时清理临时产物；页面替换成功后最后发布索引，覆盖文件系统失败、部分发布、项目关闭及重试收敛，并验证不删除过期/无关页面，运行 `./gradlew test --tests '*ProjectDocumentExporterTest' --rerun-tasks`。
- [x] 2.4 项目在 writer 开始前关闭时，在任何目录或文件变更前取消；回归断言不会创建 `docs/`、`docs/doc-view/`、`apis/`、索引或临时文件。`ProjectDocumentExporterTest` RED 复现了 `docs/` 遗留，修复后该类 15/15 通过。此 writer 测试不替代原生 IDEA 项目关闭验收。

## 3. 导出入口与既有覆盖行为

- [x] 3.1.a 在 Doc View Tool Window 增加独立的“导出项目文档”动作，提供进度、冲突与跳过结果；action 集成测试验证后台导出及 Dumb Mode 不导出；`ProjectDocumentExporterTest` 验证取消和将 `publicationAllowed` 设为 false 时不发布成品。该 predicate 测试不代表真实 IDEA 项目已关闭，运行 `./gradlew test --tests '*ProjectDocumentExportActionTest' --tests '*ProjectDocumentExporterTest' --rerun-tasks`。
- [x] 3.1.b 在 IU 2026.1 原生 IDEA 中验证空项目导出结果、用户取消任务和项目关闭时的实际 UI 路径；完成后记录真实页面/文件结果。空项目提示确认空索引已更新并保留旧页面；取消时 IDE 显示停止扫描且后台 indicator 已取消；关闭项目后确认没有创建输出目录、索引、页面或临时文件。证据见 `verification.md` 的 IU261 原生导出生命周期记录。
- [x] 3.2.a 批量 Markdown writer 在目标有效时替换旧字节，并在目标预检失败或写入取消时保留原字节；用 writer 测试覆盖替换、整批预检和取消，运行 `./gradlew test --tests '*BatchExportOverwriteTest' --tests '*BatchExportBackgroundTest' --rerun-tasks`。
- [x] 3.2.b 在 IU 2026.1 原生 IDEA 分别验证窗口合并导出与目录批量导出的覆盖确认：确认后替换旧文件，取消确认后旧文件字节保持不变。窗口导出及目录 `CatalogExportAction` 的确认/取消结果均已验证；目录动作通过 IDEA Find Action 键盘入口执行，真实右键菜单点击未覆盖。证据见 `verification.md` 的 IU261 原生覆盖验收记录。
- [x] 3.3 将窗口合并与目录选中节点批量导出改为后台 read-action 快照后写盘；测试断言 PSI 构建不在 EDT、写入阶段不持有 PSI，并覆盖取消/错误终态，运行 `./gradlew test --tests '*BatchExportBackgroundTest' --rerun-tasks`。
- [x] 3.4 更新用户指南、模板/导出 golden 与相关 change 文档，说明固定输出路径、源码链接、冲突恢复、旧页面保留、覆盖语义和普通 IDEA 搜索边界；运行 `git diff --check` 并核对指南示例与实际文件布局一致。
- [x] 3.5 项目文档 action 至少一个文件成功发布后（包括部分发布失败），在后台 read action 外仅刷新 `docs/doc-view/` 对应的 LocalFileSystem/VFS 路径；通过真实 action fixture 证明新 `index.md` 与接口页面在 VFS 中可发现，测试不得调用全项目刷新 API。Round 23 `ProjectDocumentExportActionTest` 2/2 通过，Action/UI 相关测试共 21 项通过；Round 26 全量门禁通过。

## 4. 集成验证

- [x] 4.1 在修改后先运行 `./gradlew test --tests '*ProjectDocument*' --tests '*BatchExportOverwriteTest' --tests '*BatchExportBackgroundTest' --tests '*DocViewBackgroundTasksTest' --rerun-tasks`，确认 PSI 快照、幂等、冲突、取消/失败、任务取消交付和追加回归全部通过。
- [x] 4.2 运行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`，确认完整自动门禁通过且发布包不含测试 fixture 或临时导出内容。
- [x] 4.3.a 在目标 IU 2026.1 原生 IDEA 中验证项目导出 action 的空项目、用户取消与项目关闭路径；分别验证窗口/目录批量导出的覆盖确认与取消。原生 UI 与文件结果证据见 `verification.md` 的 IU261 原生导出生命周期及覆盖验收记录；目录动作通过 Find Action 调用真实 action，右键菜单路径仍未实测。
- [x] 4.3.b 在 IU 262 原生 IDEA 中打开新生成的 `docs/doc-view/index.md`，检查纵向分组并实际点击一个方法项的 Document 和 Source 链接；Round 29 确认索引预览可读、接口页显示 `/user2/postTest` 与 `POST`，Source 打开 `User2Controller.java`。证据见 `verification.md`。本项只覆盖一个方法项，不代表多模块或重载组导航已验收。
- [x] 4.3.c 在 IU 262 原生 IDEA 中检查一个重载方法组的签名与对应页面导航。`UserFacade#queryUserDateAccount` 的单参数和双参数入口分别打开不同页面，页面请求参数与签名相符；证据见 `verification.md`。该验收只覆盖一个模块中的一个重载组。
- [x] 4.3.d 在正式 IU 262.10968.63 原生 IDEA 中检查多模块 Maven fixture 的索引分组与逐项导航：`billing-api` 和 `catalog-api` 两组均显示在索引中，分别实际打开 Document 与 Source；生成 4 个 API 页和 1 个索引页。证据及范围见 `verification.md` 的 IU262 多模块验收记录。
- [x] 4.4 在受支持的 IU 2026.1+ 原生 IDEA 中，对此前不存在的输出目录重新导出包含唯一接口词的新接口，确认 Find in Files 的 Project scope 无需 Reload from Disk 或全局刷新即可搜索到新索引和页面。Round 24 实际使用 IU262 profile，搜索 `UserFacade#queryUserDateAccount` 得到 3 个文件、4 处匹配；记录了 action 完成后搜索及命中，未测量毫秒级索引等待时长。具体证据见 `verification.md`。不得用修复前的 Reload 对照替代修复后验收。
