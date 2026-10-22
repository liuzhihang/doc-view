# 验证记录

## 自动化

- **Round 19：29/29 通过。** 覆盖生成性能基线 8 项、缓存清理 4 项、`DocViewBackgroundTasksTest` 1 项、Preview 生命周期 12 项和目录刷新 4 项。该轮用于并行主线回归，不是项目导出完整验收。
- **Round 20：40/40 通过。** 项目导出相关覆盖：快照 2、Markdown golden 1、导出 action 2、writer/exporter 13、并发 guard 2、identity 3、snapshot path 1；既有批量覆盖 4；另含 Annotation fallback 4、operation 7、Spring 1。结果 XML 保存在 `/tmp/doc-view-reliability-20261018/round20-TEST-*.xml`。
- Round 20 使用 `JAVA_21` 项目描述符运行真实本地文件 PSI fixture；源码路径、Spring/Feign/Dubbo、重载、模板 golden、无效 UTF-8 冲突、取消和发布前关闭检查均在对应自动测试中验证。
- 首次 Round 18 的 Spring fixture 断言因测试描述符未配置 JDK 失败；修正为 `JAVA_21` 后，Round 20 对应快照测试通过。此前失败不作为通过证据。
- **Round 21 全量自动门禁通过。** 执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`，35 个测试套件、180 项测试，0 failure、0 error、0 skipped；`verifyPluginProjectConfiguration`、`buildPlugin` 和 `verifyPlugin` 完成。Plugin Verifier 报告为 `Compatible`，目标 IU 版本为 `261.22158.277`。该轮插件包审计记录见 `/tmp/doc-view-reliability-20261018/round21-package-audit.json`。
- **Round 23 VFS 回归通过。** `ProjectDocumentExportActionTest` 2/2 GREEN，UI 相关测试合计 21 项通过；Action fixture 在输出根原先不存在于 VFS 时运行项目文档导出，并验证新 `index.md` 与接口页面可被 VFS 定位，没有调用全项目刷新 API。
- **Round 26 全量自动门禁通过。** 执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`，37 个测试套件、184 项测试，0 failure、0 error、0 skipped。Plugin Verifier 报告为 `Compatible`，目标 IU 版本为 `261.22158.277`。
- **Round 29 全量自动门禁通过。** 37 个测试套件、185 项测试通过；Plugin Verifier 对 IU `261.22158.277` 报告 `Compatible`。本轮包括新的项目索引布局定向回归。证据：`/tmp/doc-view-reliability-20261018/round29-fullgate.log` 与 `round29-fullgate-TEST-*.xml`。

## 原生验证

- **修复前 IU262 对照（2026-10-18）：** 导出 action 通知成功 23 个接口/24 个文件，`docs/doc-view/index.md` 和接口页在磁盘上且链接有效；Find in Files / In Project 搜索 `UserFacade#queryUserDateAccount`（无 file mask）显示 Nothing found。对同一结果执行 IDEA 的 Reload All from Disk 后，同一搜索显示 3 个文件、4 处匹配（两个重载页面各 1 处，索引 2 处）。证据截图：`/tmp/doc-view-reliability-20261018/native-project-search.png`。这是修复前的历史对照，不代表修复后的结果。
- **Round 24 IU262 VFS 修复验收（2026-10-18）：** 隔离 profile 重启并加载 SHA-256 为 `a5395a808cf481d9392cb21222186044f88fe2dc0280770a8eee83943c9b7e95` 的插件包。先按 manifest 校验并移走先前生成的 24 个文件，确认项目输出目录不存在；随后真实点击 “Export Project Docs”，没有执行 Reload from Disk 或全局刷新。在 Find in Files 的 In Project scope、无 file mask 搜索 `UserFacade#queryUserDateAccount`，得到 3 个文件、4 处匹配：两个重载 API 页面各 1 处，`index.md` 2 处。新生成 24 个文件的磁盘 hash 与 Round 21 首次导出 manifest 完全一致。证据：`/tmp/doc-view-reliability-20261018/native-project-search-fixed.png`、`/tmp/doc-view-reliability-20261018/native-project-search-fixed-ax.txt`、`/tmp/doc-view-reliability-20261018/native-export-fixed-first.json`。
- Round 24 证明该隔离 profile、项目和查询在导出完成后无需手动 Reload 即可发现并搜索这些新文档；只记录了“导出动作完成后进行搜索”的顺序，没有测量毫秒级索引等待时间，也不保证其他项目范围、排除规则或索引状态下的搜索结果。

## Round 24 原生重复导出与索引冲突保护

同一 IU262 项目中再次执行 Export Project Docs，IDE 通知显示 23 个接口的内容未变化。随后在 `index.md` 末尾加入 `QA_NATIVE_INDEX_CONFLICT` 并保存，再次导出时 action 提示项目文档导出已取消且 `docs/doc-view/index.md` 未修改；终端校验冲突前后 24 个输出文件的 hash 均与冲突前 manifest 相同。执行真实 Undo 并保存后，24 个文件又全部与 `native-export-fixed-first.json` 中首次发布结果一致。

证据：`/tmp/doc-view-reliability-20261018/native-export-repeat-ax.txt`、`native-export-conflict-final-ax.txt`、`native-export-conflict-before.json`、`native-export-fixed-first.json`。此场景验证的是项目文档索引所有权冲突保护，不是窗口合并/目录批量导出的覆盖确认对话框；在当时的 Round 24，tasks 3.2.b 尚未完成。后续 IU261 覆盖对话框验收见下文。

## Round 29 IU262 原生索引阅读与单项导航

在 IU 262 Markdown split preview 中打开实际生成的 `docs/doc-view/index.md`，确认模块/类分组可读且没有身份哈希宽列。再次导出 23 个接口时只有 `index.md` 更新，23 个接口页 hash 不变。实际点击一个 Document 链接打开 `User2Controller-postTest-d434ef8a9da70e9901016d0ed900ebc810b037b8320a8a07767c43a5006513fd.md`，页面显示 `/user2/postTest` 与 `POST`；点击 Source 链接打开 `User2Controller.java`。对应的索引阅读性验收另记于[索引 change 验证记录](../doc-export-index-readability/verification.md)。

证据：`/tmp/doc-view-reliability-20261018/native-wiki-grouped-index.png`、`native-wiki-document-link-ax.txt`、`native-wiki-source-link-ax.txt`、`native-export-readable-index.json`。此处只证明一个方法项及其两个链接，不代表多模块或所有重载的原生导航已验收。

## IU 262 原生重载页面导航

后续在同一 `doc-demo` 模块返回索引并分别点击 `UserFacade` 的两个 `queryUserDateAccount` 重载 Document 链接。单参数 `queryUserDateAccount(java.lang.String)` 打开 `UserFacade-queryUserDateAccount-dffe07be578c6b869b11a229f7386ba53a49eeca9ac9fc13be37ed33eb3a5e7c.md`，正文请求参数只有 `mobile`；双参数 `queryUserDateAccount(java.lang.String, java.time.LocalDate)` 打开独立的 `UserFacade-queryUserDateAccount-a4a1c7169328f65a7b198e325ee330cc914679d3d76eb90deefff87461149df4.md`，正文请求参数含 `mobile` 和 `date`。索引回退与逐项点击均在 IU 262 原生 IDE 完成，确认两个 overload 页面身份不同且对应所选签名。该证据覆盖 `doc-demo` 中这一个重载组，不覆盖多模块导航或其他重载组。

证据：`/tmp/doc-view-reliability-20261018/native-wiki-overload-group.png`、`native-wiki-overload-one-ax.txt`、`native-wiki-overload-two-ax.txt`。

## IU261 原生项目导出生命周期与覆盖验收（2026-10-22）

在 IU261 隔离 profile 与 `doc-demo` 中执行新包原生验证。正常项目导出通知显示 23 个接口、跳过 0 项、更新 24 个文件（`index.md` 加 23 个接口页）；实际输出暂存于 `/tmp/doc-view-native-20261022-all/export-plan/doc-demo-normal-export-23-interfaces/`。完成后恢复 `doc-demo`，35 个基线文件 hash 均未变且原索引未变；临时生成的 24 个文档保留在上述目录。证据：`/tmp/doc-view-native-20261022-all/iu261-doc-demo-export-history.txt`、`/tmp/doc-view-native-20261022-all/demo-restoration-files.json`。

空项目原生导出显示 “There are no exportable interfaces. The empty index was checked and existing pages were kept. Skipped 0 item(s): Updated 1 file(s).”，确认空索引会生成/更新。项目导出取消的原生状态栏显示 “Stopping - Scan project API documentation”；JDI 观察到项目快照扫描的 `ProgressIndicator` 已取消。另在项目关闭时暂停工作线程、关闭项目并恢复线程，确认 `docs/doc-view/` 目录、API 页、索引和临时文件均未创建。证据：`/tmp/doc-view-native-20261022-all/iu261-empty-project-export.txt`、`/tmp/doc-view-native-20261022-all/iu261-project-export-stop-confirmed.txt`、`/tmp/doc-view-native-20261022-all/debug/native-project-export-cancel.log`、`/tmp/doc-view-native-20261022-all/debug/fixed-project-export-close.log`、`/tmp/doc-view-native-20261022-all/iu261-fixed-export-project-closed.txt`、`/tmp/doc-view-native-20261022-all/export-plan/fixed-project-close-result.txt`。

窗口合并导出和目录批量导出均出现原生 “The file already exists. / Overwrite the existing file?” 确认框。窗口导出的取消保留原文件 hash，确认后目标文件被四个接口的完整新内容替换；目录导出的取消保留原文件，确认后选中类的文档被替换。目录动作通过 IDEA Find Action 键盘入口调用真实 `CatalogExportAction`，JDI 记录了 `selectedNode=ClassNode`；本轮没有实际点击右键菜单，因此只确认 action 与覆盖交互，不声称右键菜单入口已验收。证据：`/tmp/doc-view-native-20261022-all/iu261-window-overwrite-confirm.txt`、`/tmp/doc-view-native-20261022-all/iu261-window-overwrite-cancelled.txt`、`/tmp/doc-view-native-20261022-all/iu261-window-overwrite-success.txt`、`/tmp/doc-view-native-20261022-all/export-plan/window-overwrite-result.json`、`/tmp/doc-view-native-20261022-all/iu261-catalog-export-command-selected.txt`、`/tmp/doc-view-native-20261022-all/debug/native-doc-demo-catalog-export.log`、`/tmp/doc-view-native-20261022-all/iu261-catalog-overwrite-cancelled.txt`、`/tmp/doc-view-native-20261022-all/iu261-catalog-overwrite-confirm-second.txt`、`/tmp/doc-view-native-20261022-all/iu261-catalog-overwrite-success.txt`、`/tmp/doc-view-native-20261022-all/export-plan/overwrite-doc-demo-catalog/result.json`。目录覆盖结果明确记录 `cancel_preserved_original: true`、`confirm_replaced_existing: true`；窗口成功结果记录目标文档包含全部四个接口。

## IU262 多模块原生索引分组与导航（2026-10-22）

使用正式 IntelliJ IDEA Ultimate `IU-262.10968.63` 原生应用、英文界面、Islands Light 与 Compact Mode；加载 Doc View 1.3.14，ZIP SHA-256 为 `caaf605f2a3599a65e6cc0a69b2a6900328a86442d3315a0b4e6bc322a833bc3`，隔离 profile 中安装 JAR SHA-256 为 `a143d7698663e54d31f905186950616232a93bcbfed663d856216f0e2a0ea950`。在已导入并信任的多模块 Maven fixture 中执行验证，Project 树和生成索引均显示 `billing-api` 与 `catalog-api` 两个模块。第一次键盘操作误打开 `Export to Eclipse` 并取消，该次未计为导出证据；后续结果以 IDEA 中实际打开的索引与落盘文件 hash manifest 为准。

打开实际生成的 `docs/doc-view/index.md` 后，预览按 `billing-api`、`catalog-api` 分组，分别列出 Billing 的两个 `getInvoice` 重载和 Catalog 的 `getCatalog`、`searchCatalog`。实际点击两个模块各自的 Document 和 Source 链接：Billing 的单参数和双参数 Document 分别打开对应的独立页面，参数分别为 `invoiceId` 与 `invoiceId`/`year`，Source 打开 `[billing-api]` 的 `BillingService.java`；Catalog 的 Document 页面分别显示 `/api/catalog/{catalogId}` 与 `/api/catalog/search`，Source 打开 `[catalog-api]` 的 `CatalogController.java`。这确认两个模块的文档页和源码目标都匹配其所属模块。

生成文件清单包含一个 `index.md` 和四个 API 页面，hash 见 `/tmp/doc-view-native-20261022-all/export-plan/iu262-generated-files.json`。UI 与逐项导航证据：`/tmp/doc-view-native-20261022-all/iu262-project-trusted.txt`、`/tmp/doc-view-native-20261022-all/iu262-project-imported.txt`、`/tmp/doc-view-native-20261022-all/iu262-multimodule-export-triggered.txt`（仅记录被取消的错误入口）、`/tmp/doc-view-native-20261022-all/iu262-multimodule-export-result.txt`、`/tmp/doc-view-native-20261022-all/iu262-multimodule-index.jpg`、`/tmp/doc-view-native-20261022-all/iu262-billing-document-one.txt`、`/tmp/doc-view-native-20261022-all/iu262-billing-document-two.txt`、`/tmp/doc-view-native-20261022-all/iu262-billing-source.txt`、`/tmp/doc-view-native-20261022-all/iu262-catalog-document.txt`、`/tmp/doc-view-native-20261022-all/iu262-catalog-search-document.txt`、`/tmp/doc-view-native-20261022-all/iu262-catalog-source.txt`。本项覆盖该 fixture 中的两个独立模块，不代表其他多模块布局、重复 FQN 或任意项目结构均已验收。

## 尚未完成

- IDEA 搜索仍遵循用户当前 Project scope、排除规则和索引状态。Round 24 只证明该隔离 profile 和项目中的指定查询结果；不声称所有项目设置下均可搜索或搜索延迟固定。
