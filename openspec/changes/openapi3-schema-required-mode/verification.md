# Verification

验证日期：2026-10-18。测试由主线程串行运行；本 change 未自行启动 Gradle。

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| 旧实现 RED | `OpenApi3SchemaRequiredModeTest` 8 项，7 失败、0 error、0 skipped；失败对应 requiredMode、写回和缺少成员等新增行为。 | `/tmp/doc-view-reliability-20261018/schema-required-mode-red.xml` |
| OpenAPI 3 字段契约 GREEN | 8/8 通过，覆盖字段模式、AUTO 来源、真实 Spring request/response DTO、扁平参数、完整 Markdown golden、Schema 写回与缺少成员提示。 | `/tmp/doc-view-reliability-20261018/round10-TEST-com.liuzhihang.doc.view.contract.OpenApi3SchemaRequiredModeTest.xml` |
| 既有 Spring Markdown contract | 1/1 通过。 | `/tmp/doc-view-reliability-20261018/round10-TEST-com.liuzhihang.doc.view.contract.SpringMarkdownContractTest.xml` |
| 串行联合 targeted 回归 | 总计 32/32 通过，包含 OpenAPI 3（8）、Spring Markdown（1）、Preview（12）、Configuration（5）和 Template Configuration（6）。 | `/tmp/doc-view-reliability-20261018/round10-TEST-*.xml`；主线程记录 session 33498 |
| OpenSpec strict validation | `openspec validate openapi3-schema-required-mode --strict` 通过。 | 命令输出：Change is valid |
| `git diff --check` | 通过，无输出。 | 本地检查 |

## 全量门禁

- 主线执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks` 成功：35 个测试套件、180 tests、0 failures、0 errors、0 skipped；Gradle 执行了 18/18 项任务，构建耗时约 1 分 3 秒。
- Plugin Verifier 对 IU 261.22158.277 的实际 verdict 为 `Compatible`。JUnit 报告备份：`/tmp/doc-view-reliability-20261018/round21-fullgate-TEST-*.xml`。
- `openspec validate openapi3-schema-required-mode --strict` 通过（`Change is valid`）；task 3.2 已完成。

未做原生 IDE 手工确认。自动化 PSI fixture、全量测试与 Plugin Verifier 不替代原生界面验收。

2026-10-20 发布就绪复核：为上述既有缺口补列 task 3.4。没有撤销已通过的自动验证，也没有新增运行行为；在原生写回验收完成前，不把本 change 作为稳妥归档候选。

## 2026-10-22 Schema description / 批次预检自动回归与原生只读查看

- 自动回归先以 RED 重现 Java 字符串 description 的读取缺陷：9 项中 1 项失败，反斜杠、引号和 LF 被当作源码转义文本；修复后该轮 9/9 通过。后续 RED 又覆盖显式空串/空白与 Javadoc 回退，以及 `LinkedHashMap` 中普通字段先于缺少 `requiredMode` 的 Schema 字段时的整批写入问题；11 项中对应 2 项失败。最终工作区测试 XML 列出 `OpenApi3SchemaRequiredModeTest` 12 项，0 failures、0 errors、0 skipped；其用例覆盖转义字符串和 PSI 可求值拼接、显式空/空白、清空后不回退、以及缺少 requiredMode 时预检在任何字段写入前拒绝整批。RED/GREEN 日志位于 `/tmp/doc-view-native-20261022-all/schema-plan/`，最终类级 XML 为 `build/test-results/test/TEST-com.liuzhihang.doc.view.contract.OpenApi3SchemaRequiredModeTest.xml`。
- candidate235 全量门禁摘要 `/tmp/doc-view-native-20261022-all/candidate235-gate-summary.json`：49 suites、235 tests，0 failures、0 errors、0 skipped；构建成功，IU 261.22158.277 Plugin Verifier Compatible。ZIP SHA-256：`2221a77261d1de30f09c202446cd4565080b1aeff733585a9c44587e496f448d`；插件 JAR SHA-256：`62b06ff49d5f18c89f104983bd7dce5e028282134a30fe23c6689bbd481a9f78`。
- 同一候选包在 IU `261.22158.277` 的 `doc-demo` 中打开真实 Doc Editor，但只读查看后按 Esc 退出。表格中的 `memo` description 显示为已解码的 Java 字符串，保留实际引号、反斜杠和换行；源上 `deprecated required=true` 搭配 `requiredMode=AUTO` 的字段显示 Required=false，符合当前 contract。证据：`/tmp/doc-view-native-20261022-all/iu261-2221-schema-read.txt` 与 `.jpg`。Esc 后源码保持原样；`checkpoint-restoration-0959.json` 也记录 `doc-demo` 的文件、index 与 HEAD 未变化。

本次原生操作没有切换 true/false、点击 Confirm、触发缺少成员通知或执行写回，故不能作为原生写回通过。任务 3.4 仍未勾选；该原生只读场景不替代其指定的写回、成员保留与错误通知验收。
