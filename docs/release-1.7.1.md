# 1.7.1 补丁发布准备

状态：**补丁构建与恢复准入通过，准备发布官方资产；尚未提交Marketplace。** 本补丁修复 1.7.0 正式包暴露的 Skill 安装事务恢复入口问题。GitHub 上的 v1.7.0 已设为 prerelease 且非 Latest，Marketplace 尚未提交；官方来源的更新/取消仍需资产上线后补验。

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
| GitHub 稳定 Release 与 Marketplace | 未提交 | 保持等待补丁门禁 |

子 JVM 的进程强制退出恢复、16 项文件事务服务测试与6次子进程故障恢复，是独立的自动化证据；它们不能覆盖 1.7.0 原生 UI 恢复失败。

## 下一步

在隔离工作区完成 1.7.1 正式包构建、全量测试、配置验证及两个目标 IDEA build 的 Plugin Verifier。随后使用相同隔离 project/home 重验项目和全局范围：先注入可恢复 journal 并重启 IDEA，再确认恢复操作启用；点击后确认旧目标/metadata 恢复、stage/backup/journal 清理，之后再处理官方检查。更新源遭限流时明确保留该项为未验证，不影响已读取的本地恢复结果。

修复门禁通过后，再更新版本 release 文档，冻结新 ZIP/Skill 资产及 SHA，并按既有发布授权流程提交 GitHub stable Release 和 Marketplace。不得移动或覆盖 v1.7.0 tag；保留其 prerelease 状态和失败记录。

## 最终包与失败保留

最终 ZIP SHA-256：`6183ec072f8ebc213003a8e1cf061279ed066093d384f183760e9d94074b57d5`；JAR：`5117df66d76e6eac89793de4b6686a8645227d0e5c5cbd8947ce2d3c8e9462a6`。408个class均为major69，descriptor版本1.7.1、最低build263.7031.23、require-restart=true。证据目录为 `build/mcp-release-gates-2026-11-13/patch-1.7.1/`；正式全量命令/日志为 `build/release-readiness-2026-11-13/patch-final.{json,log}`。

首轮默认测试实际为551项通过，未启用外部Codex两项聚合测试；统计脚本错误预期553，断言失败后通过逐测试名对照定位到缺少opt-in init script。最终已显式启用该suite并全部553项通过，不以默认551冒充完整检查。37项定向首轮有一次不支持CLI返回ERROR而非预期CLIENT_UNSUPPORTED，单项和整组复跑通过；未保存当时底层错误，原因未查明，不能断言为冷启动超时。首轮两个Verifier并行共用默认提取缓存，最低build在检查前抛出DirectoryNotEmptyException，属于验收编排失败；最终改为串行，不据此改变生产实现。

首轮补丁原生两范围均已恢复原Skill/metadata字节并清理事务残留，但无稳定Release状态仍误写“未修改文件”；已修正文案，以上最终包重新验收。恢复完成后、第二次客户端检查期间取消的精细提示尚未原生验证，保留为未覆盖项。

最终包已在263.7307.200原生重启并完成两范围恢复：检查返回待恢复且Update启用，明确点击后旧Skill和metadata逐字节恢复；两范围journal/stage/backup/metadata备份均清理。GitHub 1.7.0已为预发布，随后检查准确显示“无兼容稳定版，保留现有版本”，详情明确已恢复旧安装。首次正式安装证据仍属于1.7.0，不能冒充本补丁的官方更新/取消验收。文件与状态证据见 `formal-native/final-recovery-summary.json`。原工作区584个文件哈希、HEAD和porcelain状态核对不变。
