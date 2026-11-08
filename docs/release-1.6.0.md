# Doc View 1.6.0 发布记录

> **状态：`v1.6.0` 已打 tag，GitHub Release 已公开；Marketplace 已接收，尚未审核/上架。** 2026-11-09 已回读两个渠道的下载内容；自动门禁和代表性原生烟测通过，完整原生矩阵按维护者批准继续保留未验证项。

## 版本与兼容基线

- 目标版本：`1.6.0`。
- 生产 Java 基线：Java 25，classfile major 69。
- 最终 ZIP 回读的最低 IntelliJ build 为 `263.7031.23`。Plugin Verifier 使用的 build 与发布版 IDEA 原生回归 build 分别记录，不能互相代替。
- 2026-11-09 发布前实时预检：GitHub 最新为 `v1.5.0`、Marketplace 最新为 `1.5.0`，两处均未占用 1.6.0；远端 `develop` 与 `master` 均为 `877f0c9cafefc065a92650d99989b42a53ff1cde`。证据在 `build/release-1.6.0/publication-preflight-current.json`。创建发布提交/tag 与上传时仍须按实际操作需要重新回读。

## 交付范围

下列内容与已发布包一致；正式 MCP 与 Skill 继续单独开发。

| 范围 | 交付内容 | 使用边界 |
| --- | --- | --- |
| IntelliJ 与 Java 基线 | Java 25 / IDEA 263 | 不再支持低于最终 descriptor 所示最低 build 的 IDE；不据此声称未来 build 已验证 |
| HTTP 请求草稿 | 对适用 Spring Controller / Feign 方法，以 Doc View PSI 规则生成可编辑 `.http` 草稿并交给 IDEA HTTP Client 打开 | 生产路线为 Java 自生成单一路线；生成动作不发送请求。Dubbo、普通非 HTTP 接口及不支持的绑定形式不生成伪请求；未支持输入须明确拒绝 |
| 操作与 UI | 收敛常用工具栏与维护菜单、校准动作名称/适用范围、减少参数操作的常驻文字并保留图标提示与无障碍名称 | 最终的中英、主题、窄窗、键盘、弹窗遮挡和菜单表现以同包实测为准 |
| OpenAPI Schema | 使用 `@Schema.requiredMode` 表达字段必填状态，按字段写回并保留未编辑的 Schema 成员；修复描述内容的字符串及多行编辑行为 | Schema 类型缺少所需成员时保留草稿并提示；本版不保证所有 Schema 组合均已原生验收 |
| 项目文档库 | 通过显式生成/更新将人类可读 Markdown 与版本化 JSON 索引及接口详情写入项目 `docs/doc-view/`；工具窗口支持搜索和打开生成文档 | JSON 表示落盘快照，不是实时源码索引或语义检索；搜索/详情依赖已生成的文档库。旧 Markdown 不会被自动反解析为 JSON |

**本版不交付正式 Doc View MCP 查询工具，也不交付可安装或更新的 Doc View Skill。** 相关工作仍由独立 OpenSpec changes 跟踪，不得在版本摘要或 Marketplace 介绍中表述为现有能力。

## 最终 1.6.0 候选包与自动门禁

候选目录：`build/native-validation/2026-11-08/remaining-native/release-1.6.0-candidate/`。该候选通过了本轮完整自动门禁与 ZIP/JAR 元数据审计，同包代表性原生烟测已完成，完整矩阵缺口见下文。

| 项目 | 结果 |
| --- | --- |
| 插件 ZIP | `Doc View-1.6.0.zip`；SHA-256 `0f58a4365ac7c026ed588838a34cd71aaf54dfb17b1af7608f24302ba82c4cc4` |
| 插件 JAR | `doc-view-1.6.0.jar`；SHA-256 `868f87dee6f86727af49b32621c2fc759010eb9b4565d27ba6fdd89ce4abadde` |
| descriptor 与包清单 | 版本 `1.6.0`，`sinceBuild=263.7031.23`，未设置 `untilBuild`，`require-restart=true`；ZIP 仅有插件目录、`lib/` 与该 JAR；JAR 409 项 |
| Java classfile | 344 个插件 class，major 69 |
| 版本资源 | 四份英/中文欢迎页与更新页均打入；build info 版本匹配，change-notes 为当前版本 |
| 全量自动门禁 | 80 suites / 430 tests，0 failures / errors / skipped；18 个 Gradle actionable tasks 全部实际执行，`BUILD SUCCESSFUL`，1m54s |
| Plugin Verifier | `263.7031.23` 与正式 `263.7307.200` 均为 `Compatible`，各报告 16 项 Experimental API、0 项 blocking finding |
| JAR 测试/PoC 名称检查 | ZIP/JAR 结构为单一插件 JAR；对 JAR 条目名检查未发现测试、fixture 或 PoC 类路径 |

实际命令为 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks --no-build-cache --no-parallel --console=plain`，使用隔离 IDEA 的 JBR。证据文件为 `artifact.json`、`commands.json`、`package-audit.json`、`full-gates.log` 与 `verifier-summary.json`。Verifier 的 Experimental API 数量按报告原样披露，不描述为零警告。

## 发布门禁状态

| 项目 | 当前状态 | 最终记录要求 |
| --- | --- | --- |
| 版本、平台和 descriptor | 目标为 1.6.0、Java 25、最低 IDEA build 263.7031.23；源码 descriptor 已声明 `require-restart="true"` | 从最终 ZIP 读回版本、最低 build、Java 字节码、重启属性和 change-notes |
| 最终插件包 | **已构建并审计** | 上表记录同一 ZIP/JAR 的路径、SHA-256、descriptor、classfile 与清单 |
| 自动测试与项目配置 | **通过** | 最终候选 80 suites / 430 tests，0 failures / errors / skipped；完整门禁命令成功 |
| Plugin Verifier | **通过，含 Experimental API** | 同一最终候选针对最低 build 与正式发布 build 均 Compatible；每个报告 16 项 Experimental API、0 blocking findings |
| 原生 IDEA 验收 | **部分通过，矩阵未完成** | 已通过的 Copy、项目文档生成/搜索和停用后重启场景见下文；其余未运行场景单列保留 |
| Git 提交与 tag | **完成** | #113 合入 develop，#114 合入 master；`v1.6.0` 指向 `67d54b700d56d2a662107e81b8ee039bbafdd25c`，与冻结源码 tree 一致 |
| GitHub Release | **已公开且下载校验通过** | Release ID `438524712`，正式非预发布；公开 ZIP 与最终制品 SHA 完全相同 |
| Marketplace | **已接收，待审核/上架** | update ID `1215457`；owner API 回读 `approve=false`、`listed=false`，公开更新列表未出现 1.6.0；下载内部文件与最终包一致 |

自动门禁和制品读回已完成；具体任务状态见[1.6.0 发布 change](../openspec/changes/release-doc-view-1-6-0/tasks.md)。原生结果与实际发布回读见下文，不把自动门禁替代原生验收。

## 最终包原生烟测

在官方 IU `263.7307.200`、无 JDWP 的隔离 `unload-clean` profile 中，对本节所列同一最终 ZIP（SHA-256 `0f58a4365ac7c026ed588838a34cd71aaf54dfb17b1af7608f24302ba82c4cc4`）执行了部分原生验收。实例 PID 为 89512；停用后重启复验 PID 为 90274。

| 场景 | 结果与边界 |
| --- | --- |
| 预览复制 | 原生 Copy 得到当前预览 `nativeGet` Markdown，482 个字符 / 618 字节，与预览编辑器内容逐字一致；测试后恢复 clipboard。首次 helper 断言误用了 DTO 展示名，改用实际 GET fixture 后通过，确认是验收 helper 输入问题，不是产品缺陷。 |
| 项目文档生成/更新 | 生成 65 个 API、跳过 0 项；既有文档库升级仅更新 1 个文件。`index.json` 显示 `producerVersion=1.6.0`、`hasUnsavedDocuments=false`。 |
| 项目文档搜索 | 搜索 `lifecycle` 返回 3/3 匹配，界面显示当前会话 65 个 API；搜索前后 133 个文档文件哈希不变。 |
| 停用与重启 | 对 Doc View 点 Apply、OK 后显示 “Restart IntelliJ IDEA to apply changes in plugins?”；日志记录需要重启。PID 89512 正常退出后显式重启隔离 IDEA，PID 90274 中插件设置显示 Disabled，主窗口无 Doc View 工具窗口，随后正常退出。该结果仅覆盖停用并重启，不代表安装、更新、卸载组合都验证通过。 |
| 环境清理 | HEAD 与原工作区 584 个文件未变，Junie 未操作；20 个既有 fixture 源文件未变，3 个临时源文件已删除，117 个既有文档库文件恢复，测试 profile 的插件启用状态离线恢复，325 个构建输入哈希未变。 |

原生证据位于 `build/native-validation/2026-11-08/remaining-native/release-1.6.0-candidate/`，包括 `native-summary.json`、`preview-copy-result.json`、`after-generate.json`、`search-readonly-result.json`、`restart-prompt.json`、`normal-exit-result.json`、重启后 Settings/Main snapshots 和 `cleanup-preservation.json`。以上结论绑定最终候选哈希，不覆盖未执行场景。

## 明确保留的验证边界

维护者于 2026-11-09 批准本次在部分原生验收仍未完成时发布：已知功能缺陷仍须修复并复验，完整自动构建/测试/Verifier 门禁仍须通过；确实未完成的原生场景可留作后续维护者人工验证和用户反馈。发布时必须按场景记录未验证项并保持相应功能 task 未勾选。该例外只适用于本版。

以下场景在最终包上**未执行**，按本次例外保留为未验证，由维护者后续人工验证并结合用户反馈处理：

- OS 凭据授权拒绝、异步写入失败后的重试；
- 尚未覆盖的原生资源/GC 情况，以及主题、窗口尺寸和交互方式组合；
- HTTP Client 实际 Send、完整异常/取消/性能矩阵；
- OpenAPI Schema 写回组合；
- 中英/浅色/窄窗/键盘/跨应用弹窗的完整 UI 矩阵，以及剩余资源/GC 矩阵；
- 上文已通过的项目文档生成与搜索以外的 JSON 文档库和项目隔离场景；
- 完整安装、更新、卸载生命周期组合；本轮只验证停用提示和显式重启后的 Disabled 状态。

OS 凭据授权拒绝、异步写入失败后的重试也未在最终包原生验证。对应活动 change 的 task 保持未勾选；以上未验证项不会被表述为通过。此前候选的结果仅作为历史参考，不能替代最终包结果。

此前的热卸载验收出现插件类仍被保留的结果；本版以 `require-restart="true"` 描述安装、更新和卸载后的重启生效方式，**不宣称修复了热卸载引用，也不承诺原位热更新或热卸载**。若最终包没有读回该 descriptor 属性，则不能按本计划发布。

## 历史候选参考（不代表 1.6.0 最终验收）

`cancel-wording-r2-candidate` 历史包 SHA-256 为 `7661036f4edb4c7cb53ecb0bc729466f00188eed75500b376230352e62ad253a`，其 JAR SHA-256 为 `5618784756441003feeb18de1bd890fe28bdd450782140d6c098feaace8ef997`。该候选的 429 项自动测试和英语查询取消提示原生回读（133 个文档文件未改变、重试 3/3）属于历史证据；中文新提示未单独原生验收。以上结果均不可替代最终 1.6.0 包的重新构建、门禁和原生验收。

## 发布与回滚

本次已在最终自动门禁通过、制品读回且未验证原生场景列明后，完成合并、tag、GitHub Release 和 Marketplace 上传。Marketplace 的人工审核与上架仍待平台完成；不得将上传成功描述为已上架。

如发布后发现阻断问题，先停止继续发布，保留制品、日志和远端状态，按发布清单回滚或准备新的 patch 版本；不重写已公开 tag 或替换已发布 ZIP。

## 2026-11-09 发布渠道回读

- 冻结源码提交：`ca51becdd2d0499018f696d96448604cccb94aa3`，232 个 literal paths。通过 [#113](https://github.com/liuzhihang/doc-view/pull/113) 合入 develop，再由 [#114](https://github.com/liuzhihang/doc-view/pull/114) 合入 master。
- `v1.6.0` 指向发布提交 `67d54b700d56d2a662107e81b8ee039bbafdd25c`；该提交与冻结源码 tree 相同，325 项构建/测试输入哈希逐项匹配。未移动原工作区的本地 develop/HEAD。
- [GitHub Release](https://github.com/liuzhihang/doc-view/releases/tag/v1.6.0) 已公开。GitHub 将空格归一化，下载名为 `Doc.View-1.6.0.zip`；已修正 `SHA256SUMS` 对应文件名，公开下载并执行 `shasum -a 256 -c SHA256SUMS` 通过。ZIP SHA-256 仍为 `0f58a4365ac7c026ed588838a34cd71aaf54dfb17b1af7608f24302ba82c4cc4`，未替换插件 ZIP。
- [Marketplace](https://plugins.jetbrains.com/plugin/15305-doc-view) 上传任务成功，update `1215457` 版本 `1.6.0`、最低 build `263.7031.23`、未设最高 build，审核和上架字段均为 false。发布后的变更说明回读含本版 HTTP、JSON 文档库和重启策略；其中打包前“重启流程仍未验证”的文字属于当时快照，本记录补充的是最终包停用/重启路径通过，完整生命周期组合仍未验证。
- Marketplace 下载端点已可取得包；外层 ZIP 被平台重新压缩，SHA-256 为 `1d3c2ddd4f18bfeb01f73647a41087dee919ee9df77d47d77b190da578cf6dc0`。解压后全部文件内容与本地最终包一致，JAR SHA-256 为 `868f87dee6f86727af49b32621c2fc759010eb9b4565d27ba6fdd89ce4abadde`，descriptor 版本、最低 build、重启标志也相同。可下载不等于公开列表上架。
- `publishPlugin` 成功执行，上传前后本地 ZIP SHA 未变；其附带 `patchChangelog` 只产生本地格式改写，已保存诊断副本并恢复冻结源文件，未夹带到发布后文档提交。
- 公开包内容与原生烟测所安装包相同；没有重新执行第二轮完整原生矩阵。正式 MCP、Skill、未完成的原生验收继续保持独立活动 change，不批量完成/归档。

发布证据位于 `build/release-1.6.0/`：`release-commit-binding.json`、`github-release-readback.json`、`marketplace-publish-result.json`、`marketplace-owner-update-readback.json`、`marketplace-artifact-readback.json`。回滚选择既有 1.5.0 或准备新 patch，不改写本次 tag 或已公开的插件包。
