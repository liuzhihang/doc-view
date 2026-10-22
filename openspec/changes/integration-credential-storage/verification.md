# 验证记录

验证日期：2026-10-18。自动化测试使用 fake PasswordSafe 存储，不访问系统钥匙串或真实平台服务。

## Round 8 自动回归与 Plugin Verifier

主 Agent 运行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks` 并检查 XML 汇总 `/tmp/doc-view-reliability-20261018/round8-summary.json`：18 个测试类共 96 项，0 失败、0 错误、0 跳过。其中凭据生命周期 `IntegrationCredentialLifecycleTest` 5 项、Apply 原子性 `IntegrationCredentialApplyTest` 1 项、凭据服务 `IntegrationCredentialServiceTest` 6 项、上传后台路径 `DocViewUploadServiceTest` 2 项均通过；设置生命周期 `IntegrationSettingsLifecycleTest` 的 16 项也通过。

`IU 261.22158.277` Plugin Verifier 报告为 `Compatible`，无 deprecated/experimental 问题；报告保存在 `/tmp/doc-view-reliability-20261018/verifier-1-7-green/IU-261.22158.277/report.html`。构建插件 ZIP 的 SHA-256 为 `b9e1365161403069ec251311b7403ab8f27caaff943ebd2bd2efae2f91997b1d`，包内只有插件生产 JAR。测试保留了 PasswordSafe 操作必须在后台线程执行的生产断言；设置 Apply 通过平台 modal task 完成后台安全存储，测试使用合成凭据和内存 fake。

## Round 24 原生凭据 UI 局部核对

隔离 IDEA profile 重启后重新打开 ShowDoc 设置：合成的本地地址（端口 65530）仍在设置中，API Key 与 Token 两项显示为已保存。核对项目 XML 未发现本轮测试值。未查看凭据值、系统 PasswordSafe/keychain 内容或凭据存储诊断。

随后实际勾选两个凭据清除框并 Apply；两项均显示未配置，Apply 随后禁用。证据文件：`/tmp/doc-view-reliability-20261018/native-showdoc-restart-ax.txt`、`/tmp/doc-view-reliability-20261018/native-showdoc-cleared-ax.txt`。此次仅验证设置 UI 状态，不执行外部平台请求或上传。

## Round 29 清除后重启核对

正常退出 Round 24 实例后，在同一隔离 profile 安装 SHA-256 为 `92879354d4be39148439627e573b76579bcecfec680c98a432624a7bdee2c029` 的包并重新打开 `doc-demo`。通过 Settings 搜索 ShowDoc 后，API Key 与 API Token 均显示未配置，Apply 禁用。证据文件：`/tmp/doc-view-reliability-20261018/native-showdoc-cleared-after-restart-ax.txt`。这验证了清除状态在本次 profile 重启后仍由设置 UI 报告为未配置；未读取系统 PasswordSafe/keychain 内容，也未进行真实平台请求或上传。

## 尚未完成的验证

- 原生 OS PasswordSafe 授权/拒绝提示，以及后端存储失败后的 Apply 错误与重试仍未验证；因此任务 `4.3.d` 继续未勾选。项目间隔离、Reset/Cancel、普通设置保留和隔离 profile 重启后的 UI 状态已由后续 2026-10-22 的原生补验覆盖，见下文。
- 未发起 ShowDoc、YApi 或 YuQue 的真实上传，也未验证外部账号或远端文档状态。
- `openspec validate integration-credential-storage --strict` 与 `git diff --check` 已通过；最终 diff 范围复核已完成。

## IU 261 candidate caaf 设置草稿与项目切换补验（2026-10-22）

在 IU `261.22158.277` 隔离 profile、candidate caaf ZIP（SHA-256：`caaf605f2a3599a65e6cc0a69b2a6900328a86442d3315a0b4e6bc322a833bc3`）中，以 `/Users/liuzhihang/workspace/doc-demo` 为项目 A，以 `/tmp/doc-view-native-20261022-all/export-plan/fixture` 为项目 B，使用合成凭据且没有发送远端请求。总步骤索引为 `/tmp/doc-view-native-20261022-all/credential-native-reproduction.json`。

- ShowDoc 密码输入在设置界面以遮蔽形式呈现。输入未应用草稿后使用 Revert changes，草稿被清空；另一轮输入后按 Cancel 并重新打开设置，草稿没有保存。证据快照：`iu261-caaf-credential-draft.txt`、`iu261-caaf-credential-draft-blur.txt`、`iu261-caaf-credential-before-apply.txt`、`iu261-caaf-credential-reset.txt`、`iu261-caaf-credential-cancel-reopened.txt`。这里验证的是 UI 遮蔽和未应用草稿的界面行为，不是读取 PasswordSafe 内容。
- 项目 A 输入合成 key 与 token 并 Apply 后，UI 显示两项 Saved；项目 B 只保存合成 key 后，UI 显示 key Saved、token Not configured。随后重新打开 A 时，两项都显示 Not configured；重新打开 B 时仍显示 key Saved、token Not configured。证据快照：`iu261-caaf-credential-applied.txt`、`iu261-caaf-credential-project-b-applied.txt`、`iu261-caaf-credential-a-after-b.txt`、`iu261-caaf-credential-b-reopened.txt`。A 的回读状态与之前保存状态不一致，需要后续定位；本观察尚不能确定原因，也不据此归因于 `serviceName`、认定项目隔离通过或宣称修复完成。
- 随后通过设置界面清除 A、B 的合成凭据并重置测试 URL。清理快照为 `iu261-caaf-credential-a-old-cleaned.txt`、`iu261-caaf-credential-b-old-cleaned.txt`。`checkpoint-restoration-0959.json` 记录 `doc-demo` 的文件、Git index 与 HEAD 均未改变，`changed` / `added` 为空。没有查看系统 Keychain/PasswordSafe 的实际 secret 值，也没有进行任何平台上传。

截至本节记录时，这组原生验收不覆盖 OS 凭据授权/拒绝提示，也没有验证后端存储失败后的重试，项目 A 回读异常尚未定位。后续根因与修复见下文；此处保留当时的观察结论。

## 凭据条目跨项目覆盖修复与候选包门禁（2026-10-22）

### 根因与修复

IU 261 SDK 字节码检查发现，`BaseKeePassCredentialStore` 和 macOS `KeyChainCredentialStore` 对非默认 service 的保存查找使用 `serviceName` 与空 account；若同一 `serviceName` 已有条目，后续保存会更新该条目的 account。旧实现虽把项目 hash 放在 `userName`，不同项目仍会因共用 `serviceName` 而覆盖。caaf 原生 metadata-only trace 记录了项目 A、B 使用同一个 ShowDoc `serviceName`、不同 account，以及 B 写入后 A 状态变为未配置；未读取凭据值。字节码证据见 `/tmp/doc-view-native-20261022-all/debug/BaseKeePassCredentialStore.javap.txt`、`/tmp/doc-view-native-20261022-all/debug/KeyChainCredentialStore.javap.txt`，运行期元数据见 `/tmp/doc-view-native-20261022-all/debug/caaf-credential-metadata-breakpoints.log`。

修复把项目位置 hash 与固定平台标识一并纳入生成的 `serviceName`，`userName` 仍使用项目 hash。插件不查找、迁移或回退读取旧共享 service 条目；升级后的用户需重新录入凭据。

### 自动 RED/GREEN 与候选包门禁

新增测试以 IU 261 的真实 `InMemoryCredentialStore`（继承 `BaseKeePassCredentialStore`）适配凭据服务，覆盖同平台两个项目保存和分别读取 ShowDoc bundle、保存 YApi token，以及清除项目 A 后项目 B 凭据仍在。修复前针对性测试 RED：项目 B 写入后，项目 A 的读取断言失败，日志 `/tmp/doc-view-native-20261022-all/debug/credential-kee-pass-red.log`。纳入项目 hash 的平台 service key 后，`IntegrationCredentialServiceTest` 的 7 项测试通过，0 failures、errors、skipped；日志 `/tmp/doc-view-native-20261022-all/debug/credential-kee-pass-green.log`。

最终 candidate 235 门禁汇总 `/tmp/doc-view-native-20261022-all/candidate235-gate-summary.json` 显示 49 suites、235 tests 全部通过，0 failures、errors、skipped，构建成功且 IU 261 Plugin Verifier 为 `Compatible`。候选 ZIP SHA-256：`2221a77261d1de30f09c202446cd4565080b1aeff733585a9c44587e496f448d`；生产 JAR SHA-256：`62b06ff49d5f18c89f104983bd7dce5e028282134a30fe23c6689bbd481a9f78`。这是自动门禁证据，不代替原生 OS 凭据存储验收。

### IU 261 原生补验进度

在 IU `261.22158.277` 中使用上述修复后的 ZIP、两个独立项目和合成凭据，未发出平台请求。项目 A Apply 后 UI 显示 ShowDoc API Key、API Token 均 Saved；项目 B Apply 后显示 Key Saved、Token Not configured；随后重新查看项目 A，仍显示两项 Saved。状态快照分别为 `/tmp/doc-view-native-20261022-all/iu261-2221-a-applied.txt`、`iu261-2221-b-applied.txt`、`iu261-2221-a-after-b.txt`，并有 `iu261-2221-a-after-b.jpg`。这为修复后初次保存及 A→B→A 项目切换提供了原生 UI 证据；快照只核对界面状态，未读取或展示 secret。

随后正常退出 IU 261 进程 43709，并在同一隔离 profile、同一候选包下以进程 43816 重启。重启后项目 A 两项仍显示 Saved，项目 B 显示 Key Saved、Token Not configured；两项目的本地 QA URL 也保留。快照为 `/tmp/doc-view-native-20261022-all/iu261-2221-a-after-restart.txt`、`iu261-2221-b-after-restart.txt`。

在该重启后的 profile 中清除并 Apply 项目 A 的 ShowDoc 凭据后，A 显示两项 Not configured；重新打开项目 B，Key 仍为 Saved、Token 仍为 Not configured。随后通过 UI 清除 B，并清空 A、B 的本地 QA URL；收尾快照为 `/tmp/doc-view-native-20261022-all/iu261-2221-a-cleared.txt`、`iu261-2221-b-after-a-cleared.txt`、`iu261-2221-b-cleaned.txt`、`iu261-2221-a-final-clean.txt`。这些结果证明同一 profile 下重启回读和单独清除 A 不影响 B 的凭据状态；验证仅观察设置 UI，不读取凭据值或系统 Keychain 内容。

原生项目隔离、重启回读、单项目清除、设置草稿 Reset/Cancel 和普通设置保留均已有 UI 证据；OS 凭据授权/拒绝提示与后端存储失败后的原生重试仍未完成，故任务 `4.3.d` 继续未勾选。没有真实上传、提交或发布。

## JVM Error 传播回归（2026-10-22）

新增的生命周期回归先在旧实现下以 pooled thread 执行，确认直接后台调用会把合成 `AssertionError` 转成 `ConfigurationException`，断言失败（RED）。修复后，直接后台路径与 EDT 发起的 modal task 路径两项回归通过；后者确认 modal task 清理完成后向调用方传播同一个 `AssertionError`。命令：`./gradlew test --tests 'com.liuzhihang.doc.view.contract.IntegrationCredentialLifecycleTest.testFatalCredentialStorageErrorPropagates' --tests 'com.liuzhihang.doc.view.contract.IntegrationCredentialLifecycleTest.testFatalCredentialStorageErrorPropagatesAfterModalTaskCleanup' --rerun-tasks`；2 tests、0 failures、0 errors、0 skipped。普通 `RuntimeException` 的脱敏失败与 Apply 草稿保持语义由既有回归覆盖；本轮只运行这两项目标测试，未重跑全量门禁或原生 UI。
