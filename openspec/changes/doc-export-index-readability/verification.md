# 验证记录

## 自动验证

- **Round 28 定向测试通过：33 项、0 失败。** `ProjectDocumentExporterTest` 14/14，`ProjectDocumentExportActionTest` 2/2。Round 27 曾有 Action 测试沿用旧的 `FQN#method` 单行布局断言；断言改为核对类标题、方法签名和 Document 链接后，Round 28 通过。证据：`/tmp/doc-view-reliability-20261018/round28-density-index-targeted.log`、`round28-density-index-TEST-*.xml`。
- **Round 29 完整门禁通过：37 个测试套件、185 项测试。** Plugin Verifier 对 IU 261.22158.277 报告 `Compatible`。证据：`/tmp/doc-view-reliability-20261018/round29-fullgate.log` 与 `round29-fullgate-TEST-*.xml`。
- `openspec validate doc-export-index-readability --strict`：Change valid。

## IU 262 原生预览与链接验收

Round 29 使用 IU 262 打开实际项目生成的 `docs/doc-view/index.md` Markdown split preview。按模块/类分组和方法项可以纵向阅读，预览中不再出现身份哈希宽列。再次执行项目导出时，23 个接口中只有索引更新，23 个 API 页面 hash 保持不变。实际点击一项 Document 链接打开 `User2Controller-postTest-d434ef8a9da70e9901016d0ed900ebc810b037b8320a8a07767c43a5006513fd.md`，页面显示 `/user2/postTest` 和 `POST`；点击 Source 链接打开 `User2Controller.java`。

证据：截图 [`native-wiki-grouped-index.png`](/tmp/doc-view-reliability-20261018/native-wiki-grouped-index.png)、[`native-wiki-document-link-ax.txt`](/tmp/doc-view-reliability-20261018/native-wiki-document-link-ax.txt)、[`native-wiki-source-link-ax.txt`](/tmp/doc-view-reliability-20261018/native-wiki-source-link-ax.txt) 和 [`native-export-readable-index.json`](/tmp/doc-view-reliability-20261018/native-export-readable-index.json)。修复前宽表证据见 [`native-wiki-wide-table-before.png`](/tmp/doc-view-reliability-20261018/native-wiki-wide-table-before.png)。

## IU 262 原生重载阅读与导航验收

后续在同一 `doc-demo` 模块的索引中打开 `UserFacade` 的两个 `queryUserDateAccount` 重载。单参数 `queryUserDateAccount(java.lang.String)` 的 Document 链接打开独立页面 `UserFacade-queryUserDateAccount-dffe07be578c6b869b11a229f7386ba53a49eeca9ac9fc13be37ed33eb3a5e7c.md`，正文请求参数只有 `mobile`；返回索引后，双参数 `queryUserDateAccount(java.lang.String, java.time.LocalDate)` 的 Document 链接打开另一独立页面 `UserFacade-queryUserDateAccount-a4a1c7169328f65a7b198e325ee330cc914679d3d76eb90deefff87461149df4.md`，正文请求参数含 `mobile` 和 `date`。这确认两个重载有不同页面身份，且点击对应索引项会到达相符页面。此轮只覆盖一个模块中的一个重载组。

证据：[`native-wiki-overload-group.png`](/tmp/doc-view-reliability-20261018/native-wiki-overload-group.png)、[`native-wiki-overload-one-ax.txt`](/tmp/doc-view-reliability-20261018/native-wiki-overload-one-ax.txt)、[`native-wiki-overload-two-ax.txt`](/tmp/doc-view-reliability-20261018/native-wiki-overload-two-ax.txt)。

## IU 261 多模块索引分组与导航验收（2026-10-22）

在隔离的官方 IntelliJ IDEA IU `261.22158.277` 实例中打开临时 Maven 聚合 fixture `/tmp/doc-view-native-20261022-all/export-plan/fixture`，并从 Doc View 工具窗口执行项目文档导出。IDEA 实际识别并展示 `billing-api` 与 `catalog-api` 两个模块；完成通知为 `4 interface(s), skipped 0 item(s), Updated 5 file(s)`。生成结果包含 `index.md` 和四个独立 API 页面。原生索引预览按模块与类分组展示四个方法条目，每项均有 Document 与 Source 链接；四个 Document 和四个 Source 相对链接也都解析到 fixture 中存在的目标文件。

- `billing-api` / `com.example.billing.BillingService`：`getInvoice(java.lang.String)` 打开 [`BillingService-getInvoice-40437899aa55c64cda5d48a703a25b8ee6054e3b259e5faf322c9125920c792d.md`](/tmp/doc-view-native-20261022-all/export-plan/fixture/docs/doc-view/apis/BillingService-getInvoice-40437899aa55c64cda5d48a703a25b8ee6054e3b259e5faf322c9125920c792d.md)，正文含 `invoiceId`；`getInvoice(java.lang.String, int)` 打开 [`BillingService-getInvoice-f432d7ff53ff252903809a5ebb6b61857da4b0cf15fa9c6438e8c9925fe098e5.md`](/tmp/doc-view-native-20261022-all/export-plan/fixture/docs/doc-view/apis/BillingService-getInvoice-f432d7ff53ff252903809a5ebb6b61857da4b0cf15fa9c6438e8c9925fe098e5.md)，正文含 `invoiceId` 与 `year`。Source 导航打开 `BillingService.java`，IDEA 标签显示模块 `billing-api`。
- `catalog-api` / `com.example.catalog.CatalogController`：`getCatalog(java.lang.String)` 打开 [`CatalogController-getCatalog-8a7e13275c8aea904ec5f3a39e557119649129bc13eef3034d1f0ab765935a4d.md`](/tmp/doc-view-native-20261022-all/export-plan/fixture/docs/doc-view/apis/CatalogController-getCatalog-8a7e13275c8aea904ec5f3a39e557119649129bc13eef3034d1f0ab765935a4d.md)，正文路径为 `GET /api/catalog/{catalogId}`；`searchCatalog(java.lang.String, boolean)` 打开 [`CatalogController-searchCatalog-6ae00970f40b00444ac8da1ebbda310c5d06312ab5120a1e124aea6c0e68210b.md`](/tmp/doc-view-native-20261022-all/export-plan/fixture/docs/doc-view/apis/CatalogController-searchCatalog-6ae00970f40b00444ac8da1ebbda310c5d06312ab5120a1e124aea6c0e68210b.md)，正文路径为 `GET /api/catalog/search`，参数含 `keyword` 与 `includeArchived`。Source 导航打开 `CatalogController.java`，IDEA 标签显示模块 `catalog-api`。

证据：隔离实例日志 [`idea.log`](/tmp/doc-view-native-20261022-all/iu261/log/idea.log) 标明 IU `261.22158.277`；安装包哈希记录 [`package.json`](/tmp/doc-view-native-20261022-all/iu261/package.json) 标明本轮验证的 ZIP SHA-256 为 `df21ba775bd79032a3667e7ddef3984efee271a0511140977ceb33415cf50adb`；导出通知与双模块树见 [`iu261-multi-project-export-triggered.txt`](/tmp/doc-view-native-20261022-all/iu261-multi-project-export-triggered.txt)，索引原生预览见 [`iu261-multi-index.jpg`](/tmp/doc-view-native-20261022-all/iu261-multi-index.jpg)。四个文档页与两个 Source 目标的原生窗口/内容记录分别为 [`iu261-billing-document-one.txt`](/tmp/doc-view-native-20261022-all/iu261-billing-document-one.txt)、[`iu261-billing-document-two-verified.txt`](/tmp/doc-view-native-20261022-all/iu261-billing-document-two-verified.txt)、[`iu261-billing-source-verified.txt`](/tmp/doc-view-native-20261022-all/iu261-billing-source-verified.txt)、[`iu261-catalog-document-verified.txt`](/tmp/doc-view-native-20261022-all/iu261-catalog-document-verified.txt)、[`iu261-catalog-search-document-verified.txt`](/tmp/doc-view-native-20261022-all/iu261-catalog-search-document-verified.txt) 与 [`iu261-catalog-source-verified.txt`](/tmp/doc-view-native-20261022-all/iu261-catalog-source-verified.txt)。

本次原生多模块证据满足本 change 的 task 3.4。该验证运行于 IU 261，不能用于勾选 `doc-export-workspace` 中要求 IU 262 原生验收的 task 4.3.d。

## 仍待验证

- IU 262 原生记录已验证一个重载组中两个签名各自的 Document 导航，以及一个方法项的 Document/Source 导航；本 change 的跨模块索引分组与导航现由上述 IU 261 原生验收覆盖。该记录不代表所有重载组或所有页面导航均已验收；`doc-export-workspace` 的 IU 262 task 4.3.d 仍需独立完成。
- 本 change 未进行发行或发布验证；Round 29 Plugin Verifier 和原生结果只描述该轮构建与隔离 IDE 验收。
