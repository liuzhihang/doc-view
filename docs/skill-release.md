# Doc View Codex Skill 发行与验收

本文说明如何从固定源码生成 Skill archive，以及怎样分别验收当前项目和全局安装。检查更新读取公开发行信息和安装记录；查询成功时只原子更新独立的最近成功检查时间，不写 Skill、安装记录或 writer lock。安装或更新只由 IDEA 设置页中的明确操作触发。Skill 包内的文件作为数据写入，不会执行包内脚本。

## 发布包组成

官方发行需要同一 Doc View GitHub release 中的两个资产：

- `doc-view-skill-X.Y.Z.zip`：仅含 `doc-view/SKILL.md` 等 Skill 正文文件。
- `doc-view-skill-X.Y.Z.manifest.json`：绑定稳定 `vX.Y.Z` tag、tag commit、仓库源目录、每个文件的路径/长度/SHA-256、MCP tool contract 版本与指纹、兼容的 Doc View 插件版本区间，以及 ZIP 大小和 SHA-256。

打包器读取 tag commit 中的文件，不从工作树复制，也拒绝未提交改动。ZIP 固定路径顺序、时间戳、权限和压缩方式；archive ≤4 MiB，展开后≤8 MiB，单文件≤2 MiB，最多64个文件。SHA-256用于校验字节与manifest相符；manifest本身不签名，因此发布准备还必须确认commit已进入官方仓库、tag指向该commit且GitHub Release资产回读与本地摘要相同。

从干净的官方仓库checkout开始，版本文件应为稳定插件版本，且release tag已存在并解析到待发布commit。示例：

安装器下载时还会核对commit对应的tree与Skill文件集合，逐文件比对Git blob身份；Git commit与tree是不同对象，参见[GitHub Commit API](https://docs.github.com/en/rest/git/commits)和[Tree API](https://docs.github.com/en/rest/git/trees)。两种API响应分别限制1 MiB，递归树被截断时停止，不把不完整树当作验证成功。

```bash
python3 tools/doc-view-skill/package_skill.py \
  --repo . \
  --commit <40-character-commit-sha> \
  --tag v1.7.1 \
  --tool-contract-version 1 \
  --expected-tool-contract-fingerprint <64-character-sha256> \
  --minimum-plugin-version 1.7.1 \
  --maximum-plugin-version-exclusive 2.0.0 \
  --output build/skill-release
```

打包器核对 `origin` 指向 `https://github.com/liuzhihang/doc-view` 的 HTTPS 或 SSH 地址、工作树干净、tag匹配commit、commit中的插件版本落在指定区间、MCP契约匹配。它不会创建tag、推送代码或上传Release资产；发布者仍需在目标GitHub release上核对tag、commit和两个资产。不要把本地生成的manifest单独发给客户端。

## 本地候选包

2026-11-13候选包位于隔离验收目录 `build/skill-acceptance-2026-11-13/package/artifacts/`。它绑定一个只存在于该目录下临时Git仓库的snapshot commit和本地tag `v1.7.0`，来源字节为当前 `skills/doc-view/SKILL.md` 与 MCP contract；snapshot中的插件版本属性按候选插件版本规范化为 `1.7.0`。该commit/tag未在GitHub存在或发布，假设的origin配置也只在临时仓库使用。它用于测试包格式、hash、兼容字段和两范围文件事务，不能被描述为官方Release或直接通过官方更新检查获得。

archive内仅有 `doc-view/SKILL.md`，大小2,514 bytes；ZIP SHA-256为 `f0f50211f7492167f99f391d0e740c4880bf69146d2989d6342b6267333955f9`，manifest SHA-256为 `769fa25fb8eec8174dd6413b45f9bc3647f1e5539f69bd2b391ce16e3226c634`。绑定snapshot commit `2f5d64a30ade5e3cfaa50c8c42b58273f497d37a`，contract version `1`，contract fingerprint `sha256:d52b6361ac04342f08c41b83f2ac2d7aa5acdc20c99f24d23bc9646d3fbca3ff`，插件兼容范围 `[1.7.0, 2.0.0)`。两次重复构建的ZIP和manifest相同，且ZIP成员、完整性校验和内容摘要回读通过。机器摘要见 `build/skill-acceptance-2026-11-13/package/package-verification.json`。

## 两种安装范围的验收

每个测试使用独立临时根目录，避免读写真实个人Skill目录：

| 范围 | 验收根 | 自动计算的目标 |
| --- | --- | --- |
| 当前项目 | 隔离的临时项目目录 | `<project>/.agents/skills/doc-view` |
| 全局 | 隔离的临时HOME | `<HOME>/.agents/skills/doc-view` |

对每个范围分别执行相同场景，并记录CLI build、项目cwd、显示目标路径、操作状态及目标/安装metadata/最近检查状态/journal/备份的前后hash。明确区分Check for Updates 唯一允许更新的最近检查状态与安装记录、目标Skill和writer lock。测试客户端必须是已验证的精确Codex build，并用真实发现接口核对安装后的`name=doc-view`、scope和`SKILL.md`路径。Project与Global可同时存在同名Skill；不可据此推断Codex对模型提示的优先级。

1. **首次安装**：目标不存在。明确点击安装后，archive通过完整manifest校验，再写入所选路径和独立metadata；客户端发现精确文件。另一范围不变。
2. **更新**：先用合法的旧版本安装与metadata创建基线，再安装较新候选。确认新文件和metadata提交，备份只在新版本验证通过后清理；另一范围hash不变。
3. **用户修改保护**：修改已管理文件，再检查更新。显示冲突，不替换/删除任何目标文件；保留完整原始hash。另覆盖预先存在但无Doc View metadata的目录。
4. **下载或暂存取消**：通过可控测试源在下载、展开或staging阶段阻塞，触发取消；确认任务最终状态只报告一次、staging已清理、目标与metadata未变。不要用取消一个已立即完成的操作代替在途取消。
5. **替换阶段故障/恢复**：用故障注入分别覆盖旧目录备份rename失败、新目录rename失败、metadata提交失败和进程中断留下journal。重启服务后先恢复旧的完整可用版本，清理仅本次创建的暂存/备份；目标不得长期缺失或留下半成品。记录两次rename间目标可能短暂不存在这一已知窗口。
6. **离线及重试**：更新检查离线时保留原版文件、metadata和最近成功检查时间；连通性恢复后再次检查应恢复可用状态。离线缓存不能被报告为最新。

Project和Global各自完成安装、更新、用户修改冲突、在途取消、阶段故障恢复、离线保留和客户端发现后，才满足两范围事务验收。单测或注入测试源不替代官方GitHub下载和最终隔离IDEA原生安装；要分别标注已验证的边界。

## 重跑隔离服务与进程恢复验收

生产Gradle配置不默认运行外部CLI。准备本地候选后，使用Java25和显式opt-in脚本运行：

```bash
env \
  DOC_VIEW_ACCEPTANCE_OUTPUT="$PWD/build/skill-acceptance-2026-11-13/real-client" \
  DOC_VIEW_ACCEPTANCE_CODEX="/Applications/ChatGPT.app/Contents/Resources/codex-cli/bin/codex" \
  DOC_VIEW_ACCEPTANCE_MANIFEST="$PWD/build/skill-acceptance-2026-11-13/package/artifacts/doc-view-skill-1.7.0.manifest.json" \
  DOC_VIEW_ACCEPTANCE_ARCHIVE="$PWD/build/skill-acceptance-2026-11-13/package/artifacts/doc-view-skill-1.7.0.zip" \
  ./gradlew test -I tools/doc-view-skill/acceptance.init.gradle \
    --tests '*CodexSkillReleaseAcceptanceTest' --rerun-tasks --no-build-cache --no-parallel
```

测试将Project、用户home、metadata、CLI进程配置及崩溃子JVM限制在新建的测试目录。`real-codex-lifecycle.json`记录16个场景，`jvm-crash-recovery.json`记录6次独立JVM退出恢复。文件事务由生产服务执行，网络源被明确替换为本地候选；此命令不发布、不安装到普通用户目录，不启动普通IDEA或Junie。[本轮验收记录](../openspec/changes/doc-view-client-skill/verification-2026-11-13-release.md)保留失败轮次和测试边界。

## 正式发布边界

Skill正式发布还依赖`doc-view-mcp-tools`的发布范围取消与项目生命周期验收，以及正式包的Project/Global安装、更新、取消和故障恢复。完成本地archive构建、注入测试源验收或`skills/list`发现，均不能单独解除这些门槛。GitHub Release上传和版本发布须经独立发布授权及完整[发布检查清单](release-checklist.md)核对。

## 1.7.0 GitHub 预发布资产

[GitHub v1.7.0](https://github.com/liuzhihang/doc-view/releases/tag/v1.7.0) 保留插件 ZIP、Skill ZIP 和 manifest，绑定源码 commit `1612cdc3ac62323c04fca0301843a0a0be5e8f0c`。ZIP 正文与前述本地候选相同；GitHub 资产字节已回读。Release 目前标记为 prerelease 且非 Latest，生产下载器只读取稳定版，因此不会将它作为可安装的最新 Skill。manifest 指纹为 `sha256:400aacb3bc373117e01288d995be7969d3e962c2ace58880a4a1775752ec2a17`，SHA-256 为 `d1d1865697720f25c0069adb080fa6b889ed986dedbe0d4133ae0a63a070d17b`。IDEA 原生安装、更新及恢复状态见[1.7.0验收记录](release-1.7.0.md)和[1.7.1补丁记录](release-1.7.1.md)。

## 当前稳定版 1.7.1

[GitHub v1.7.1](https://github.com/liuzhihang/doc-view/releases/tag/v1.7.1) 已正式发布，tag commit 为 `0894d9e427827a49eb3e2142bc674c8a5941fc83`。Skill ZIP SHA-256 为 `f0f50211f7492167f99f391d0e740c4880bf69146d2989d6342b6267333955f9`，manifest SHA-256 为 `6292b59596c11394c00374da20c12e7c3db76e6e62511ce6deb22b21d7da9828`；manifest 绑定 contract version 1、指纹 `sha256:400aacb3bc373117e01288d995be7969d3e962c2ace58880a4a1775752ec2a17` 和插件范围 `[1.7.1, 2.0.0)`。官方附件均已下载回读；正文 ZIP 与旧版相同不代表 manifest 或发布身份相同。

1.7.0 两范围官方首次安装通过后，原生重启恢复发现按钮禁用问题；1.7.1 已修复并在最终插件 ZIP 中完成两范围恢复和官方 1.7.0→1.7.1 更新。当前项目、隔离全局 HOME 的实际 Codex 发现、进行中取消保留旧文件、再次更新成功、用户修改保护及范围隔离均通过。取消时确认操作处于 Working，未定位具体下载/暂存阶段；journal 为受控注入，子 JVM 强制退出恢复另有自动证据。未将首次安装、升级及不同包的证据混为同一检查。

Marketplace 已接收插件 1.7.1，仍待审核；Skill 发行资产已经可由生产安装器通过 GitHub 稳定 Release 获取。自动安装仍限 macOS Apple Silicon 与 `codex-cli 0.167.0-alpha.4.1`，其他 build/系统不承诺支持。详情及未覆盖项见[1.7.1 发布记录](release-1.7.1.md)。
