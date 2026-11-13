# 1.7.1 发布与验收记录

状态（2026-11-13）：**[GitHub v1.7.1](https://github.com/liuzhihang/doc-view/releases/tag/v1.7.1) 已正式发布；Marketplace 已接收 update `1219746`，`approve=false`、`listed=false`，等待审核上架。** 当前项目和隔离全局范围均完成官方 1.7.0→1.7.1 更新、进行中取消后重试、用户修改保护及重启 journal 恢复。GitHub v1.7.0 保留原 tag/附件，已标为 prerelease 且非 Latest；1.7.0 未提交 Marketplace。

## 变更范围

恢复待处理时，状态检查和显式安装/更新入口均先读取并验证自动检测到的 Codex 客户端 build，再执行本地 journal 恢复。受支持客户端可以从 IDEA 设置继续操作；如果本地恢复成功、后续官方 release 查询离线，界面应准确保留已恢复的结果。未知或不支持的客户端依然不能触发恢复写入。

生产 MCP、Skill 内容、发现路径和项目文档格式均不在此补丁范围。Junie 和普通工作区不属于本次验收对象。

## 1.7.0 原生失败证据

263.7307.200 隔离 IDEA 使用正式插件包、正式 Skill GitHub release 和虚构项目/隔离用户 HOME：

- 当前项目与全局首次安装均完成；两个范围的 Skill metadata 指向各自显示路径、官方 v1.7.0 commit 和 Codex 0.167.0-alpha.4.1。IDEA 状态均显示 1.7.0 已安装且由 Codex 发现；Skill 文件 SHA-256 为 01d69274b1ee5bc757cb8b7273ba7082314652ac99a7624dd2c1c804bb5ff99f。
- 后续官方更新检查遇到 GitHub 匿名 Core API 403 限流；旧 Skill 和 metadata 未变。该检查不能算作更新成功或失败恢复验收。
- 为重启验收，在两个隔离范围写入旧目录备份和格式有效的 OLD_BACKED_UP journal 后重启 IDEA。这是文件系统注入夹具，并非真实 IDEA 崩溃自然产生的事务。
- 重启后 Settings → Tools → Doc View → MCP / AI 将当前项目显示为“需要恢复”，但更新按钮 disabled。**项目范围原生恢复入口失败**。全局范围在这次重启后的 UI 状态未单独读取，不能判为通过。

已定位原因：状态检查在检查 journal 之后提前返回，因此 RECOVERY_PENDING 没有受支持 Codex build 身份；UI 按客户端准入规则正确禁用了写操作。随后还发现显式更新入口会在 Codex build 验证前恢复 journal，且恢复成功后若网络离线可能报告“未变更”，这两条相邻路径已修复，并分别先复现失败再复验。

## 发布门禁

| 检查 | 状态 | 证据 |
| --- | --- | --- |
| 1.7.0 首次 Project/Global 官方安装与 Codex 发现 | 通过 | build/mcp-release-gates-2026-11-13/formal-native/ |
| 1.7.0 官方更新下载和替换 | 未验证 | GitHub Core API 403 限流；旧文件保持不变 |
| 1.7.0 项目范围原生重启 journal 恢复 | 失败 | journal 由隔离验收夹具注入；状态识别但按钮 disabled |
| 1.7.0 全局范围重启 journal UI 恢复 | 未验证 | 本轮没有读取该范围的重启后状态 |
| 1.7.1 首轮定向 service/UI 测试（19项） | 中间版本通过 | 之后发现两条相邻恢复路径问题；不能作为最终复验 |
| 1.7.1 恢复路径与事务/UI定向检查 | 37项复验通过 | 4个新增回归先RED后GREEN；另保留一次未查明的CLI状态ERROR偶发失败 |
| 1.7.1 最终正式包全量测试、配置与构建 | 553项通过，0失败/错误/跳过 | 显式启用真实Codex与子JVM套件；17个Gradle任务均实际执行 |
| 1.7.1 Plugin Verifier | 两build通过 | IU263.7031.23 / 263.7307.200均Compatible，各16项既有Experimental；无Internal/Deprecated/Override-only/兼容性问题 |
| 1.7.1 原生注入 journal、重启、Project/Global 恢复 | 两范围通过 | 最终ZIP实际按钮可用，旧Skill/metadata原字节恢复，所有事务残留清理；无稳定版提示已纠正 |
| 1.7.1 官方更新、Codex 发现与范围隔离 | 两范围通过 | 生产安装器下载官方资产，另一范围文件与 metadata 不变 |
| 1.7.1 进行中取消、重试及用户修改保护 | 两范围通过 | 原生 Working 状态关闭设置；旧文件不变，重试成功；不推断具体下载/暂存时点 |
| GitHub 稳定 Release | 已发布并回读 | tag commit、四个附件及 SHA 核对一致 |
| Marketplace | 已提交，待审核 | update 1219746；approve=false、listed=false |

子 JVM 的进程强制退出恢复、16 项文件事务服务测试与6次子进程故障恢复，是独立的自动化证据；它们不能覆盖 1.7.0 原生 UI 恢复失败。

## 正式来源的原生闭环

最终 ZIP 在 IU `263.7307.200`、macOS Apple Silicon、`codex-cli 0.167.0-alpha.4.1` 上使用未修改的生产 GitHub 下载器测试。Project 位于虚构项目，Global 位于隔离 HOME；没有安装到用户真实全局 Skill 目录。

两范围分别检查到官方 1.7.1，点击更新并观察到 Working 状态和按钮禁用后，点击设置页 Cancel。两范围旧 Skill 与 metadata 均逐字节不变，没有 journal/stage/backup 残留。重新打开、检查并重试后，两个范围均显示 1.7.1 已安装并由 Codex 发现；metadata 绑定正式 tag commit。没有观察下载或暂存的精确阶段，因此只记录“更新操作进行中取消”。

项目更新后，全局的 1.7.0 文件保持原样；全局更新后，项目的 1.7.1 文件保持原样。分别修改两范围 Skill 测试文件后，检查更新显示修改冲突并禁用更新，保留修改内容与原 metadata；验收结束恢复测试文件。最终无事务残留，隔离 IDEA 正常退出，本阶段 MCP Server 未启用。

首次原生安装证据属于 1.7.0；最终 1.7.1 包的上述证据属于真实升级，不能宣称重新验证了 1.7.1 空目录首次安装。机器记录位于 `build/mcp-release-gates-2026-11-13/formal-native/official-update-summary.json`，对应两范围取消、更新、修改保护的独立 JSON 和 `final-recovery-summary.json`。

## GitHub 与 Marketplace

源码经 [PR #130](https://github.com/liuzhihang/doc-view/pull/130) 进入 develop，再经 [PR #131](https://github.com/liuzhihang/doc-view/pull/131) 进入 master。`v1.7.1` 精确指向 `0894d9e427827a49eb3e2142bc674c8a5941fc83`，发布 tree 与已验收源码一致。GitHub Release `444147029` 于 2026-11-13 11:27:37 UTC 公开，非 draft/prerelease，作为 Latest；四个附件均重新下载并核对字节。

| 资产 | SHA-256 |
| --- | --- |
| `Doc.View-1.7.1.zip` | `6183ec072f8ebc213003a8e1cf061279ed066093d384f183760e9d94074b57d5` |
| `doc-view-skill-1.7.1.zip` | `f0f50211f7492167f99f391d0e740c4880bf69146d2989d6342b6267333955f9` |
| `doc-view-skill-1.7.1.manifest.json` | `6292b59596c11394c00374da20c12e7c3db76e6e62511ce6deb22b21d7da9828` |

Skill manifest 绑定同一 tag commit、contract version 1、指纹 `sha256:400aacb3bc373117e01288d995be7969d3e962c2ace58880a4a1775752ec2a17`，插件范围 `[1.7.1, 2.0.0)`。ZIP 只包含 Skill 文档，不包含凭据或客户端 MCP 连接配置。

Marketplace 上传直接使用从 GitHub 回读且 SHA 匹配的最终 ZIP，未重新构建；`publishPlugin` 成功，所有者接口读回 update `1219746`，版本 1.7.1、最低 build 263.7031.23、稳定 channel。审核及列出状态仍为 false，不能描述为已上架；公开列表当前仍为 1.6.2。证据见 `patch-1.7.1/marketplace-owner-readback.json` 和 `build/release-readiness-2026-11-13/patch-marketplace-publish.{json,log}`。

Marketplace 重新封装后的 ZIP SHA-256 为 `1f9f9e00281b0687673ce3ff16b44e7a656b8e3c273ed91c57ea6180b656771c`，与上传 ZIP 的外层字节不同；逐个非目录 entry 比较全部相同，内部插件 JAR 仍为 `5117df66d76e6eac89793de4b6686a8645227d0e5c5cbd8947ce2d3c8e9462a6`。下载 descriptor 的版本、最低 build、require-restart 及 Marketplace 更新说明均匹配。读回记录：`patch-1.7.1/marketplace-artifact-readback.json`。

## 最终包与失败保留

最终 ZIP SHA-256：`6183ec072f8ebc213003a8e1cf061279ed066093d384f183760e9d94074b57d5`；JAR：`5117df66d76e6eac89793de4b6686a8645227d0e5c5cbd8947ce2d3c8e9462a6`。408个class均为major69，descriptor版本1.7.1、最低build263.7031.23、require-restart=true。证据目录为 `build/mcp-release-gates-2026-11-13/patch-1.7.1/`；正式全量命令/日志为 `build/release-readiness-2026-11-13/patch-final.{json,log}`。

首轮默认测试实际为551项通过，未启用外部Codex两项聚合测试；统计脚本错误预期553，断言失败后通过逐测试名对照定位到缺少opt-in init script。最终已显式启用该suite并全部553项通过，不以默认551冒充完整检查。37项定向首轮有一次不支持CLI返回ERROR而非预期CLIENT_UNSUPPORTED，单项和整组复跑通过；未保存当时底层错误，原因未查明，不能断言为冷启动超时。首轮两个Verifier并行共用默认提取缓存，最低build在检查前抛出DirectoryNotEmptyException，属于验收编排失败；最终改为串行，不据此改变生产实现。

首轮补丁原生两范围均已恢复原Skill/metadata字节并清理事务残留，但无稳定Release状态仍误写“未修改文件”；已修正文案，以上最终包重新验收。恢复完成后、第二次客户端检查期间取消的精细提示尚未原生验证，保留为未覆盖项。

最终包已在263.7307.200原生重启并完成两范围恢复：检查返回待恢复且Update启用，明确点击后旧Skill和metadata逐字节恢复；两范围journal/stage/backup/metadata备份均清理。当时GitHub 1.7.0已为预发布、1.7.1尚未公开，随后检查准确显示“无兼容稳定版，保留现有版本”，详情明确已恢复旧安装。此恢复结果与后续官方更新/取消证据分别记录。原工作区584个文件哈希、HEAD和porcelain状态核对不变，Junie未操作。

## 保留的边界与后续

- 两个精确 IDEA build 的 Verifier 各保留16项既有 Experimental API 提示；没有承诺未来 build。Skill 自动写入目前只准入上述 macOS Apple Silicon 与精确 Codex build。
- 原生 journal 由测试夹具注入，不等于真实 IDEA 崩溃留下的事务；独立子 JVM 的强制退出恢复已另行自动验证。
- 具体下载/暂存阶段取消、恢复完成后第二次客户端检查期间取消的提示、最终文档索引提交后迟到取消等精确时点未全部原生覆盖。历史完整 HTTP/UI、中文/浅色/键盘组合和全部生命周期矩阵仍未重跑；保留 OpenSpec 未完成任务，不批量归档。
- MCP 最低 build 查询、Dumb Mode、可选加载及真实 Codex 取消证据沿用本系列对应候选记录；1.7.1 未改变 MCP 适配实现，不将这些历史记录改称全部在补丁包重跑。
- 安装、更新及停用需重启 IDEA；不承诺热卸载。Marketplace 仍需审核，审核完成前可从 GitHub 下载最终 ZIP 手动安装。若发现阻断性回归，停止推广并发布修复；1.6.2 可作为不含 MCP/Skill 的回退版本，不保证反向配置迁移。
