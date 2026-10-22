# 验证记录

## 修复前 RED

主代理运行 `./gradlew test --tests '*ClearCacheActionTest' --rerun-tasks`，报告 3 tests、2 failures、0 errors、0 skipped。XML 保存于 `/tmp/doc-view-reliability-20261018/round15-red-TEST-com.liuzhihang.doc.view.contract.ClearCacheActionTest.xml`。

Window 与 Catalog 的选中文档用例都在 `assertPreserved` 处发现 Extensions 文件已无效；目录无选择用例通过。round14 中 Window fixture 曾因把文本写到 `Doc View` 目录而无法有效复现，已改为该目录下的合成 Markdown 文件后，round15 得到上述有效 RED。

## 修复后 targeted GREEN

主代理运行 `./gradlew test --tests '*ClearCacheActionTest' --rerun-tasks`，round18 XML 显示 4 tests、0 failures、0 errors、0 skipped。报告位于 `/tmp/doc-view-reliability-20261018/round18-TEST-com.liuzhihang.doc.view.contract.ClearCacheActionTest.xml`。四项覆盖 toolbar action、Catalog 选中与无选择、以及两处 action descriptor 名称/说明一致；两个 action 的缓存清除和手写 Scratch/项目文件保留断言均通过。此前静态 `rg` 确认 `CustomFileUtils.delete` 已无调用点，专用方法与 import 已移除。

## 修复后完整自动门禁

主代理运行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks` 成功。共 35 个 test suite、180 tests，0 failures、0 errors、0 skipped；Plugin Verifier 结果为 Compatible，目标 IU build 为 `IU-261.22158.277`。Gradle 测试 XML 保存在 `/tmp/doc-view-reliability-20261018/round21-fullgate-TEST-*.xml`。

截至随后记录的 IU262 部分验收时，专用 IU 2026.1 原生验收尚未完成；任务 3.2 保持未勾选。自动测试与 Verifier 通过不代表原生 IDE 中的 toolbar、目录交互和 Scratch 保留行为已手工验收。

## IU 262 原生部分验收（2026-10-18）

- 环境：IntelliJ IDEA IU `262.10968.63`（`/Applications/IntelliJ IDEA.app`），JBR `25.0.4`；隔离 profile `/tmp/doc-view-reliability-20261018/native`。加载 round21 插件包，SHA-256：`01e67f753ce9fa388c0dbb607fe940ddb9a371dc8570aa6347ad27a3c2c42020`。
- 实际点击工具窗口 toolbar 的 Clear Cache 后，先前生成、再通过编辑器添加 `<!-- QA_KEEP_SCRATCH -->` 并执行 Cmd+S 的 Scratch 文档仍保持打开，磁盘内容仍含该标记。观察记录：`/tmp/doc-view-reliability-20261018/native-clear-cache-ax.txt`。
- 目录树右键菜单未能打开，因此目录入口及其显示说明、选中/未选中上下文仍未原生验证；本次也不据 toolbar 的一次操作宣称完整 IU 262 验收通过。任务 3.2 保持未勾选。
- 启动日志中唯一已知 `SEVERE` 来自平台 `accessibilityStatusBar` → Maven `TransactionGuard` 的 write-unsafe 调用栈，栈中没有 Doc View 帧；不将本次日志描述为零错误，也不把该平台日志归因于插件。

## IU261 原生补验（2026-10-22）

在 IU261 中通过 IDEA Find Action 检查了同名的 `Clear Cache` action 条目。IDEA 描述区显示 “Clear the current project's Doc View schema cache; generated documents are preserved.”；JDI 分别命中无目录选择和选中 `UserController` 后的 `CatalogClearAction.actionPerformed`。前后 AX 记录保留了 `UserController` 选择和目录节点；无选择场景同样执行了真实 action。证据：`/tmp/doc-view-native-20261022-all/iu261-cache-find-actions.txt`、`/tmp/doc-view-native-20261022-all/iu261-cache-catalog-description.txt`、`/tmp/doc-view-native-20261022-all/iu261-cache-selected-description.txt`、`/tmp/doc-view-native-20261022-all/debug/native-catalog-clear-no-selection.log`、`/tmp/doc-view-native-20261022-all/debug/native-selected-clear-actions.log`、`/tmp/doc-view-native-20261022-all/iu261-cache-selected-before.txt`、`/tmp/doc-view-native-20261022-all/iu261-cache-selected-after.txt`。

另外，实际点击 toolbar 的 Clear Cache 后，已打开并手写标记 `<!-- QA_ALL_CLEAR_CACHE -->` 的 Scratch Markdown 文档仍保持打开，编辑器/文件内容仍含该标记；基线 hash 记录在 `cache-scratch-before.json`。证据：`/tmp/doc-view-native-20261022-all/iu261-cache-toolbar-before.txt`、`/tmp/doc-view-native-20261022-all/iu261-cache-toolbar-after.txt`、`/tmp/doc-view-native-20261022-all/iu261-scratch-handwritten.txt`、`/tmp/doc-view-native-20261022-all/cache-scratch-before.json`、`/tmp/doc-view-native-20261022-all/iu261-cache-toolbar-after.jpg`。

本轮的目录 action 通过 IDEA Find Action 键盘入口调用，没有实际点击目录右键菜单；因此该路线不能代替右键菜单入口验收，任务 3.2 继续保持未勾选。自动测试、toolbar 实际点击和 Find Action action 执行记录分别证明各自覆盖范围，不合并描述为完整的目录菜单原生验收。

## 2026-10-22 系统鼠标补验

在官方 IU261.22158.277 的 doc-demo、candidate235 包上，实际打开 UserController 类的目录右键菜单并点击 Clear Cache。菜单正常关闭，目录中的 5 个类/接口节点仍保留。证据为 `/tmp/doc-view-release-20261022/cache-context-open.jsonl` 和 `cache-context-after.jsonl`。本次仅补充实际菜单入口的证据，不把它计为最终1.4.0包的全部缓存场景或勾选整个组合验收任务。
