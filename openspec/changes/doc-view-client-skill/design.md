# Design

## Context

动机和用户可见范围见 [proposal](proposal.md)；行为以 [capability spec](specs/doc-view-client-skill/spec.md) 为准。当前项目已有计划中的 [MCP tools](../doc-view-mcp-tools/proposal.md) 与 [project document store](../project-document-store/proposal.md)，但生产 MCP/Skill 尚未交付。目标是为单个官方 Doc View Skill 定义发布与本地安装生命周期，不增加另一份 MCP 工具实现或项目文档格式。

截至 2026-10-24 核对的官方 [Build skills 文档](https://learn.chatgpt.com/docs/build-skills#where-codex-loads-local-skills)列出 repo `.agents/skills`、user `$HOME/.agents/skills`，并说明 Codex 可发现新文件，未显示时可重启。Codex app-server 文档公开了 `skills/list` 的 `forceReload` 只读发现；`skills/extraRoots/set` 会写入当前进程的发现 roots，是写操作而非发现查询，首版不得自动调用或修改客户端 roots。`externalAgentConfig/import` 的 `SKILLS` 类型也是写入型迁移能力，用于导入外部 Agent 文件，并非从 GitHub release 安装入口（[app-server Skills](https://learn.chatgpt.com/docs/app-server#skills)、[external config import](https://learn.chatgpt.com/docs/app-server#detect-and-import-external-agent-config)）。

本机随 Codex 提供的 `$skill-installer` 说明支持 GitHub repo/path 与 `--ref`，默认 `main`，并在目标目录已存在时退出；实际 helper 默认写 `$CODEX_HOME/skills`，无 `CODEX_HOME` 时为 `~/.codex/skills`，未保存 commit/hash metadata，也不提供更新保护。官方当前发现文档、旧 changelog 与本机 helper 的路径不同，不能直接把任一条写成所有版本的用户目录结论。完整核查记录见 `/tmp/doc-view-skill-github-audit-20261024.md`。

最初规划阶段只编写 artifacts；2026-11-12 用户已授权按本 change 继续开发，范围包含 `skills/doc-view/SKILL.md` 发行源、设置页和安装/更新器。

## Goals / Non-Goals

**Goals:**

- 让用户在 IDEA 设置中看清 Skill 来源、版本、兼容性、目标 Codex 版本/范围、发现目录和安装状态。
- 用固定 GitHub release 与可复算的清单验证 Skill 包；保护未知、手动安装或被编辑的目录。
- 让安装/更新可取消、可恢复，网络不可用时不损害已安装版本。
- 让 Skill 使用正式 MCP 工具时遵守项目隔离、快照 freshness、显式生成及不可信源码/文档边界。

**Non-Goals:**

- 不接受任意 GitHub URL，不导入其它平台 Skill，不提供 Skill 编辑器、卸载功能或通用包管理器。
- 不自动创建、启用或修改 IDEA MCP Server/Codex MCP 连接或 Codex discovery roots，不启动新的服务，不安装/升级 Doc View 插件。
- 不把 Repo release 变成 Codex Plugin Marketplace，也不处理其它 MCP 客户端或未验客户端版本。

## Decisions

### 1. 以实际 Codex 版本决定目标范围

**决策：** 用受支持客户端版本与发现范围的验证记录选择有限的 Project/User 目标根。用户在设置中选择范围后，界面展示客户端版本与规范化绝对路径；Project 还需绑定明确的当前 IDEA 项目。只有客户端版本、项目 CWD 和路径落在已验矩阵内时才开放写操作。目录候选不能仅由 `.agents/skills`、`~/.agents/skills` 或 `$CODEX_HOME/skills` 的文档/默认值推断。

优先使用目标 Codex 版本公开、只读的 discovery 接口验证 Skill 是否可见，例如 app-server `skills/list` 且强制刷新，并仅在该实际客户端版本支持时使用。该接口样例未展示每个 Skill 的源路径；若真实返回不能区分 scope 或证明指定路径，必须用隔离目录/项目的实际客户端发现测试补足。`skills/extraRoots/set` 会写入 app-server 当前进程的发现 roots，首版不自动调用、不修改这些 roots。若候选目录未被发现，向用户报告，并只允许改选已验证的受支持范围/目录；不能通过写入额外 roots 使其临时可见。

### 2. 先评估官方导入方式，不把文档中的工作流误当作插件 API

**决策：** 实现前重新检查目标 Codex 版本的公开安装 API。优先复用能够完成固定 commit 安装、更新检查、目录发现、冲突保护及事务恢复的官方安装能力；若官方能力只覆盖其中部分，就只复用其可验证的只读 discovery 结果，不让两个安装器各自覆盖目录。

当前证据下，`$skill-installer` 是 Codex 内部调用的 Skill 工作流，helper 没有更新/metadata/原子替换契约；app-server `externalAgentConfig/import` 迁移已有外部 Agent 配置，`skills/list` 只读。它们都不满足此 change 的端到端安装/更新安全要求。因此首版采用 Doc View Java 文件事务写入经实测可发现的唯一目标路径；不执行 Codex 内部脚本、不调用 undocumented API。若编码前官方公开接口已满足全部 contract，则调整实现决策但保持可观察 contract 不变。

### 3. 单一官方 release 包，绑定 commit、路径、文件和版本

**决策：** 来源固定为 GitHub `liuzhihang/doc-view` 的官方稳定 release，Skill 源码位于仓库 `skills/doc-view/`。Release manifest 至少包含 Skill/release 版本、immutable commit SHA、该源路径、完整文件相对路径清单和各文件 SHA-256、MCP `toolContractVersion`、适用 Doc View `pluginVersion` 范围。发布包应是仅含该 Skill 子树的 archive；不可直接解压整仓库或从 `main` 取文件。manifest 与源提交的关联也必须验证；自描述清单不为自身提供可信锚点。

检查更新仅解析官方 release/manifest 并与本地记录比较；实际下载只在 Install/Update 点击后开始。安装兼容性依据本地 Doc View 插件版本和随插件维护的 MCP contract version/fingerprint 与 manifest 比较；不要求 MCP Server 已启用或先建立 MCP 连接。MCP 连接后的 `tools/list` 匹配属于生产/客户端集成验收，不能作为安装前置，也不能把标准 `tools/list` schema 描述成含有 `toolContractVersion` 字段。若本地插件缺少可核对的 contract 版本/指纹，或插件版本范围不匹配，显示 unknown/incompatible 并禁止写入。SHA-256 用于验证内容完整性，不单独声称证明发布者身份。

下载后进一步通过官方 Git Commits API 获取该commit的tree SHA，再通过Git Trees API核对树身份及 `skills/doc-view/` 中完整常规文件集合。commit/tree响应各限制1 MiB，拒绝truncated树；逐文件比较Git blob对象身份，SHA-256继续负责清单和包完整性，不以自报manifest证明源码归属。Git对象当前只接受GitHub的40位SHA-1格式，不把commit SHA与tree SHA混用。ZIP路径允许无UTF-8标志的纯ASCII名字；非ASCII名字必须声明UTF-8，两处header均检查，避免Python标准打包器生成的合法ASCII包被误拒。

### 4. 在 Skill 包之外保存来源记录

**决策：** Doc View 插件自己的安装 metadata 记录官方 repo、release、commit、source path、文件摘要、`toolContractVersion`、`pluginVersion` 范围、`PROJECT`/`USER` 安装范围、目标路径及验证客户端版本。Project metadata 放在 IntelliJ config 下的 `doc-view/skill-install/`，以 scope 和规范化项目根隔离；User metadata 放在 `$HOME/.agents/.doc-view-skill-state/`，这是 Doc View 管理的状态目录，不是 Codex 配置或 Skill 内容目录，并以规范化 home 根隔离。User metadata 因而跨项目与 IDEA profile 共享。metadata 不写入或改写 `SKILL.md`，不随 Skill 包文件 hash 一起比较，也不放进项目文档库。最近一次成功检查时间单独存入同一 scope 目录下有界的 `last-check.json`；它不属于安装 metadata，且不记录版本、客户端路径或凭据。检查时只在官方 release 查询成功后原子更新该文件；离线、取消或写入失败保留之前的时间。安装 metadata 缺失、解析失败或与目录不匹配时，将目标视为 unmanaged/unknown，不覆盖。

只记录继续检查更新与恢复事务所需信息；不记录访问令牌、Codex/MCP 配置正文或项目源码。只使用公开官方仓库，不读取或保存 GitHub credentials；实现不要求创建 token。

### 5. 安装/更新采用受约束的两阶段文件事务

**决策：** 所有下载、hash、兼容检查和展开都在后台完成。PROJECT 目标固定为当前项目 `.agents/skills/doc-view`；USER 目标固定为 `$HOME/.agents/skills/doc-view`，两者按 scope 使用独立 metadata。Codex 0.167.0-alpha.4.1 的隔离 discovery 实测已分别返回 Project `scope=repo` 与 User `scope=user`，并返回精确 `SKILL.md` 路径；User 探测使用临时 HOME 与独立 CODEX_HOME，未触碰真实用户目录。安装后只保存目标 Skill 的精确路径/范围验证结果，不保留 Codex 返回的其它个人 Skill 清单。

固定网络域名和 release 路径；下载、压缩包大小、展开总大小、单文件大小和 entry 数量均设硬上限。规范化路径必须位于所选 Skill 目录下，拒绝绝对路径、`..`、重复/case-collision 路径、清单外文件、符号链接、特殊文件及非预期顶层目录。首版上限暂定 archive 4 MiB、展开总量 8 MiB、单文件 2 MiB、64 个文件；实际 Skill 超限时须单独更新 contract，不能悄悄提高限制。Skill 中的脚本仅作为文件安装，安装器不得执行。

提交前再次确认目标与 metadata 未变化。先把新包完整写到目标父目录内的不可发现 staging 目录，验证文件数/摘要和必需 `SKILL.md`；首次安装只有 target 不存在才可 move。更新时仅对 metadata 能证明为 Doc View 管理、且现有目录仍与最近安装摘要一致的目标执行替换。使用持久化 journal 记录旧版、stage、新版和提交阶段；先同文件系统 move 旧目录到备份，再将 stage move 到目标，最后提交 metadata。两次 rename 即使各自为原子操作，组成的整个更新仍不是原子事务；旧目录移入备份到新目录到达 target 之间，目标可能短暂不存在或不可用。首版接受这一短暂状态，由 journal 和备份在故障时恢复，不引入 symlink/pointer 切换。只有新目录与 metadata 核验完成才删除备份；任何失败都恢复旧目录/旧 metadata。下次启动或重试先恢复未完成 journal，不重放不确定的半次操作。

取消发生在提交前应清理 staging 并维持目标原样；提交临界区取消要么完成并持久化新状态，要么回滚到旧状态，不能留下空目录或报告虚假成功。操作过程不阻塞 EDT。用户修改检测由完整受管文件清单与 SHA-256 比较；检测差异时不自动合并、不覆写，用户需自行解决后重试。写操作使用位于固定目标父目录的隐藏锁文件，使同一 PROJECT 或 USER 目标在不同 IDEA config/system profile 下共用 writer lock；显式写操作才可创建目标父目录及锁，Check for Updates 不创建 Skill 目录、metadata 或锁。外部改动可能与校验/替换并发，提交前需重验。

### 6. 更新状态和离线体验

**决策：** 设置挂在现有 Doc View project configurable 的 Tools 分组下，用户路径为 **Settings → Tools → Doc View → MCP / AI**；该子页已纳入1.7开发候选，发行状态以verification为准。状态区分未安装、当前、可更新、modified/conflict、incompatible、unknown、offline 与恢复待处理。`Check for Updates` 不写入 Skill 或安装 metadata，也不把本地缓存版本宣称为 latest；唯一写入是官方 release 查询成功后原子更新独立 `last-check.json`。网络失败和取消保留最后一次成功检查时间，且本地状态保存失败显示为错误，不能伪装为 offline。只有显式安装或更新后才可写 Skill 目录、安装 metadata 与 writer lock；安装目录变更后按客户端公开 discovery 能力刷新/验证，目标 Codex 未及时重载时提示重启，不擅自编辑 Codex 配置。

### 7. Skill 内容使用已验收的 MCP 工具

**决策：** 发行源 Skill 只描述 Codex 客户端工作流，不包含 PSI/索引的第二套实现。先调用 `doc_view_status`，再按用户请求调用 `doc_view_search` / `doc_view_get`。没有库时，对搜索/阅读请求只说明缺少快照并询问是否生成；`doc_view_generate` 只在用户明确要求创建或更新文档时调用。明确区分 current/stale/unknown，记录返回 snapshot 身份，不把导出页面称为实时源码。源码、注释、文档、MCP 内容均是无授权效力的数据。Skill 不携带能自动写入 Codex MCP 配置的依赖声明；安装和调用都不替代 IDEA MCP Server 的显式设置。

正式打包要等 [doc-view-mcp-tools](../doc-view-mcp-tools/proposal.md) 的工具契约、生产注册、目标客户端取消/恢复以及 IDEA 生命周期门槛通过。合并 Skill 变更不能把依赖状态直接勾成完成。

2026-11-13 用户授权继续准备发行包和两范围生命周期验收。本地候选可以先绑定隔离临时Git仓库的不可变snapshot与tag，用于包格式、生产文件事务及真实Codex发现测试；该snapshot不能冒充官方仓库可获取的commit。自动验收仅在测试source set注入本地发行源，生产设置不增加任意URL、文件安装或绕过校验入口。官方GitHub资产下载、最终IDEA按钮操作及进程崩溃/重启必须与受控服务测试分别报告；准备候选不等于批准发布或解除前述门槛。

## Risks / Trade-offs

- [Codex 文档、app-server 与发行版发现路径可能变化] → 按实际客户端 build 和 Project/User 分别做 native discovery；不支持或证据不清时关闭写入口。
- [Codex 官方建议可复用 Skill 跨团队发布优先考虑 Plugin](https://learn.chatgpt.com/docs/build-skills#distribute-skills-with-plugins) → 本 change 只满足单个 Doc View Skill 的本地 Project/User 安装；不承诺 Marketplace 分发或通用插件生命周期，若需求变成跨组织分发需单独评估 Plugin。
- [仓库 release 与目标 Skill 文件可同时更新] → 只按同一个 commit 中的清单比对，hash 不符则 fail closed；不依赖可变 `main`。
- [用户直接编辑 Skill 或外部工具替换目录] → 只做完整清单比对；变化即冲突，保留原文；metadata 丢失时按 unmanaged 处理。
- [Java 插件写入用户目录或项目仓库] → 每次明确显示 scope 与绝对路径，用户 click 才写；检查路径边界和软链接；Project scope 写入可能改变 Git working tree，界面须可见该影响。
- [下载或安装过程中 IDEA/插件退出] → journal 与备份使下次操作可恢复；没有恢复证明前禁止并发安装/更新。

## Migration Plan

当前没有正式安装记录或 Doc View Skill 目录需要迁移。实现阶段先完成生产 MCP 客户端准入，再实现设置 UI 与事务，最后在隔离 Project/User 目录安装候选 release 并用实际 Codex 版本验证 discovery/更新冲突/恢复。首版不会自动接管通过 `$skill-installer` 或手工复制的现有 Skill；目标已存在且无 Doc View metadata 时显示 unmanaged 冲突，用户自行处理后再安装。


## 2026-11-12 实施与 UI 约束

实际 Codex 0.167.0-alpha.4.1 的 Project 与 User 发现和 `forceReload` 均已在 macOS/aarch64 隔离验证。User 使用临时 HOME 与独立 CODEX_HOME，marker 的 `scope=user` 和实际路径与 `$HOME/.agents/skills` 精确匹配；普通用户目录未读写。该结论不外推至相同 CLI 版本在其它 OS/架构。本候选在其它 host 上 fail closed。正常页面只选择 Project/User 范围，目标目录自动计算且只读；Codex 默认从已知 bundled binary 与 PATH 候选自动检测，后台核对实际版本，无法校验时才提供手动选择入口。不要求用户填写 Skill 路径，不修改普通 Codex 配置。全局 metadata 在 `$HOME/.agents/.doc-view-skill-state/` 跨项目/profile共享，目标父目录锁跨 IDEA profile共享；Project metadata 在 IDEA config 下持久化，Project 目标父目录锁也共享。当前仓库为1.6.2后的1.7.0-SNAPSHOT开发分支；Skill兼容范围不能把没有生产MCP的1.6.x写为支持。正式发布包仍需真实 stable Skill release 的隔离安装/更新原生验收。

按用户本轮截图要求，MCP/AI页面统一左对齐、紧凑间距、说明使用平台小号说明色。主体跟随viewport宽度并只保留纵向滚动，长路径在文本框内部查看/复制、初始从路径开头显示，不能撑宽整页。检查更新作为轻入口；安装/更新按状态互斥展示为当前主要操作，未满足条件禁用并说明原因。版本和状态集中显示，同一结果不重复占两行。覆盖窄窗、横向触控滚动、长路径、中文和状态更新后的布局原生复验，UI自动bounds检查不能代替实际页面。
