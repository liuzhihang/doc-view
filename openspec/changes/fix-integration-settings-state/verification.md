# 验证记录

验证日期：2026-10-18。测试针对当前工作树的集成设置修复；没有运行真实平台上传。

## RED 基线

运行：

```bash
./gradlew test --tests 'com.liuzhihang.doc.view.contract.IntegrationSettingsLifecycleTest' --rerun-tasks
```

IntelliJ fixture 初始化与测试源码编译成功。`IntegrationSettingsLifecycleTest` 共 16 项，14 项按旧行为在目标断言处失败，0 错误、0 跳过；覆盖 null/空值、地址规范化、YApi ID 状态、部分写入风险、Reset 和 descriptor ID。失败列表由 XML 报告确认，未把 fixture 或编译失败计作 RED。

## 定向 GREEN 与并行导航回归

运行：

```bash
./gradlew test \
  --tests 'com.liuzhihang.doc.view.contract.IntegrationSettingsLifecycleTest' \
  --tests '*ApiTreeNavigationTest' \
  --rerun-tasks
```

`compileJava`、`compileTestJava` 和对应测试任务执行成功。XML 按测试类分别统计：

| 测试类 | 数量 | 失败 | 错误 | 跳过 |
| --- | ---: | ---: | ---: | ---: |
| `IntegrationSettingsLifecycleTest` | 16 | 0 | 0 | 0 |
| `ApiTreeNavigationTest` | 5 | 4 | 0 | 0 |

设置回归全部通过。联合 Gradle 命令整体以退出码 1 结束，原因是导航测试中的以下四项仍失败：

- `testClassPresentationKeepsCodeAndBusinessNamesDistinct`
- `testMethodNameFallbackIsNotRepeatedAsDescription`
- `testOverloadsHaveDistinctSignaturesAndMethodIcons`
- `testRefreshDoesNotAccumulateMethods`

导航失败属于并行的 `api-tree-navigation` change；联合命令不作为全量测试通过的证据。

## 其他检查与未覆盖项

- `openspec validate fix-integration-settings-state --strict`：通过。
- `git diff --check`：通过。
- 上述定向/导航结果记录的是较早轮次；最终自动回归见下节 Round 7。
- IDEA 原生核对的部分结果见下节；Round 46 已触发 YuQue 实际缺配置分支并核对本地导航，远端账号、请求和上传结果仍未验证。
- YApi、ShowDoc、YuQue 账号、凭据和目标服务：本轮没有可用账号，也没有发起请求、上传或修改远端文档。

## Round 8 最终自动回归与 Plugin Verifier

主 Agent 运行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks` 并检查 XML 汇总 `/tmp/doc-view-reliability-20261018/round8-summary.json`：18 个测试类共 96 项，0 失败、0 错误、0 跳过。`IntegrationSettingsLifecycleTest` 的 16 项和 `ApiTreeNavigationTest` 的 7 项均通过。该结果覆盖了本 change 的设置状态与导航自动回归，也包括当轮其它活动 change 的测试。

`IU 261.22158.277` Plugin Verifier 报告为 `Compatible`，无 deprecated/experimental 问题；报告保存在 `/tmp/doc-view-reliability-20261018/verifier-1-7-green/IU-261.22158.277/report.html`。构建插件 ZIP 的 SHA-256 为 `b9e1365161403069ec251311b7403ab8f27caaff943ebd2bd2efae2f91997b1d`，包内只有插件生产 JAR。`CHANGELOG.md` 已记录本 change 的空值/URL 规范化、YApi ID 校验和 YuQue 导航修复。`openspec validate fix-integration-settings-state --strict` 与 `git diff --check` 已通过；本机原生核对仅覆盖下节列出的页面/表单状态，没有对 ShowDoc、YApi、YuQue 发起真实请求或上传。

## Round 24 原生设置局部核对

在隔离 IDEA profile 中打开 YuQue 设置页后，站点地址与 API 地址字段标签对应正确。原生 YApi 设置页对无效项目 ID 的 Apply 显示 `Cannot Save Settings`；点击 Revert 后草稿回到空值。核对只涉及设置 UI 和本地草稿，不执行真实平台请求。

证据文件存在：`/tmp/doc-view-reliability-20261018/native-acceptance-progress.md`、`/tmp/doc-view-reliability-20261018/round24-native-package.log`。Round 24 当时尚未触发 YuQue 缺配置分支；后续 Round 46 原生触发记录见下节。

## Round 46 YuQue 缺配置导航原生验收

在 IU 262 隔离 profile 的 `UserController` 方法光标处，通过 Find Action 执行 `Upload To YuQue`。实际打开 `Settings → Tools → Doc View → Platform Integrations → YuQue Settings`；页面显示未配置状态且 Apply 禁用，随后用 Cancel 退出。辅助功能快照：`/tmp/doc-view-reliability-20261018/native-round46-yuque-missing-settings-ax.txt`。该操作证明缺配置分支的本地导航及设置页状态，不代表提交或验证了 YuQue 上传。

当前源码中 `YuQueServiceImpl.checkSettings()` 在必需的站点/API 地址、仓库、登录名或凭据状态缺失时打开设置页并返回 `false`（`src/main/java/com/liuzhihang/doc/view/service/impl/YuQueServiceImpl.java:36-53`）；`DocViewUploadService.upload()` 先调用该检查，只有通过后才调用 `doUpload()`（`src/main/java/com/liuzhihang/doc/view/service/DocViewUploadService.java:71-79`）。因此本次缺配置路径在平台上传调用前结束。未读取或记录凭据值，也没有真实平台账号、远端请求或上传结果。
