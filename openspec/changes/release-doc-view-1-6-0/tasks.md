# Tasks

## 1. 固定交付范围与发布元数据

- [x] 1.1 冻结拟发布源码提交；逐项核对 IDEA 263/Java 25、HTTP 请求生成、UI/Schema 修复、JSON 项目文档库的对应活动 change 与实际差异，并确认正式 MCP 和可安装 Skill 不在交付范围内；用 change 状态和冻结清单验证范围，不批量勾选或归档其他 change。本次冻结 232 个 literal paths；源码/测试/构建 325 项输入哈希与通过门禁的候选一致。其他 change 继续保留活动状态及未完成验收，不批量同步或归档。
- [x] 1.2 将插件版本、CHANGELOG、双语欢迎/更新摘要、README、Marketplace 介绍和发布记录统一到 1.6.0；回读源码/打包输入，确认能力描述只包含最终版本实际交付内容，不宣称未交付的 MCP/Skill。源码/资源与最终包审计均为 1.6.0，四份欢迎/更新页及 change-notes 已从 ZIP 回读。
- [x] 1.3 将插件 descriptor 设为 `require-restart="true"`，以定向 descriptor contract 验证源码，再从最终 ZIP 读回该属性；发布说明须写明安装、更新、卸载需重启，不声称已解决热卸载或支持热更新。`PluginDescriptorLocalizationTest.testPluginDescriptorRequiresRestartForLifecycleChanges` 通过；最终 ZIP descriptor 回读为 `require-restart=true`。
- [x] 1.4 发布前重新读取 GitHub 与 Marketplace 最新版本、1.6.0 tag/Release 占用状态和远端分支；确认版本仍可用且分支基线符合发布流程，并保存实际回读结果。2026-11-09 实时预检显示 GitHub `v1.5.0`、Marketplace `1.5.0` 最新，1.6.0 均未占用，远端 `develop` / `master` 同为 `877f0c9cafefc065a92650d99989b42a53ff1cde`；证据 `build/release-1.6.0/publication-preflight-current.json`。发布操作前如状态变化仍须重新回读。

## 2. 最终制品与自动门禁

- [x] 2.1 对冻结的最终源码实际运行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks --no-build-cache`；记录真实 suites/tests、失败/跳过、目标 IDE build 与 Plugin Verifier 结果，失败先修复再重跑，不能用历史候选结果替代。最终候选 80 suites / 430 tests，0 failures/errors/skipped；18/18 Gradle tasks 执行成功；Verifier 对 `263.7031.23` 和 `263.7307.200` 均 Compatible、各有 16 项 Experimental API 且无 blocking findings。详见发布记录与候选目录证据。
- [x] 2.2 审计同一最终 ZIP/JAR 的 SHA-256、目录和依赖清单、descriptor 版本/最低 build/`require-restart`、classfile major、更新说明与双语资源；确认无测试/PoC 类、凭据或越界制品，并将读回结果与门禁日志关联。最终 ZIP/JAR SHA、仅插件 ZIP 布局、409 项 JAR、344 个 major 69 class、版本/最低 build/重启标志、四份页面和 change-notes 均已读回；JAR 条目名未见 test/fixture/PoC 路径。详见 `package-audit.json` 与发布记录。

## 3. 最终包原生验收与未验证项

- [x] 3.1 在隔离目标 IDEA 安装精确 ZIP，执行本轮实际可完成的代表性原生场景，并记录 build、fixture、包 hash、结果与日志；只把实际执行且通过的场景标为通过。本轮精确包在 IU 263.7307.200 / 无 JDWP 的隔离 profile 完成 Markdown Copy、65 API 文档生成/搜索和“停用后重启”场景，具体结果及边界见 `docs/release-1.6.0.md`；其余矩阵不据此推定通过。
- [x] 3.2 对未执行或未通过的原生场景逐项说明原因、边界和后续人工验证方式；保留对应活动 change 的未完成 task。不得将旧候选或 2026-11-09 之前的结果写成最终 1.6.0 原生通过，也不得把发布例外写成完整验收。发布记录列明最终包未运行的 HTTP Send/异常取消性能、Schema 写回、完整 UI/GC、凭据拒绝/重试及完整安装更新卸载场景，均交维护者后续人工验证并结合用户反馈；不宣称已通过热卸载。

## 4. 发布及回读

- [ ] 4.1 按仓库流程将已验收的冻结版本合入发布分支，创建 1.6.0 tag 并核对其 commit；确认 Release/tag 指向同一已验收源码和制品，发生远端状态变化时先回读、不得覆盖既有版本。
- [ ] 4.2 发布 GitHub Release 和 Marketplace 更新；分别回读版本、兼容范围、更新说明、制品 SHA 与审核/公开状态。上传成功不代表 Marketplace 已审核或上架。
- [ ] 4.3 发布后核对公开下载制品与最终 ZIP/JAR 内容及哈希，记录实际安装 smoke test、已知未验证项与回滚版本；若 Marketplace 仍待审，明确标为已接收待审而非公开。
