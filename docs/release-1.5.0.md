# 1.5.0 发布记录

日期：2026-11-03

状态：1.5.0 已经 [PR #111](https://github.com/liuzhihang/doc-view/pull/111) 合入 master，[v1.5.0 tag / GitHub Release](https://github.com/liuzhihang/doc-view/releases/tag/v1.5.0) 已公开，GitHub 安装包与校验文件已下载核对，相关六条 Issue 已说明版本及使用条件并关闭。Marketplace 已成功接收更新 `1211221`，服务端包内容核对一致；公开及认证单条元数据均为 `approve=false / listed=false`，仍待审核、尚未公开上架，公开版本列表最新仍为 1.4.0。

## 本轮范围

本次发布交付 [`open-issue-backlog`](../openspec/changes/archive/2026-11-03-open-issue-backlog/verification.md) 的六项修复，完整用户变化见 [`CHANGELOG.md`](../CHANGELOG.md) 的 1.5.0 条目。发布元数据的一致性与操作步骤由 [`release-doc-view-1-5-0`](../openspec/changes/archive/2026-11-03-release-doc-view-1-5-0/proposal.md) 跟踪。

| Issue | 交付内容与使用条件 | 当前状态 |
| --- | --- | --- |
| #89 | 接口方法和实体字段的 `@since`、`@version`；已有自定义模板需自行插入新增变量 | 1.5.0 已交付；回读 closed / completed |
| #90 | Spring 路由数组取第一项，支持 `path` 与常量；模板丢失的既有生命周期修复已做回归 | 1.5.0 已交付；回读 closed / completed |
| #91 | Dubbo/Spring 参数名精确匹配、多行和空标签处理；原截图的基础场景已有实现 | 1.5.0 已交付；回读 closed / completed |
| #96 | Markdown 隐藏集合、Map 的合成中间层，保持真实字段和 JSON；不扩展此前未解析的任意嵌套容器类型 | 1.5.0 已交付；回读 closed / completed |
| #97 | 实体当前编辑值复制为普通或独立分表 Markdown，复制不写回源码 | 1.5.0 已交付；回读 closed / completed |
| #101 | 使用 IDEA 已关联的正确版本源码读取依赖字段注释；不从纯字节码恢复 Javadoc，不自动下载源码 | 1.5.0 已交付；已说明源码条件，回读 closed / completed |

代码修复提交为 `9a4ee80`，原生续验记录为 `7c05136`；发布前基线 develop/master 为 `7b2e0a8`。原工作区未提交的 Java 25/IU 263、项目文档库、MCP 等功能没有加入本轮范围。正式包继续使用 Java 21、IU 2026.1（since-build 261）及既有依赖；未设置最高 build 不代表所有未来版本均验证过。

`open-issue-backlog` 已同步 3 份长期规格并归档；归档中“不升级、不发布”的表述保留为当时开发阶段的历史边界。维护者后来明确授权本次交付，因此以当前发布 change 跟踪版本与发布操作，不改写早期证据。本次发布 change 的 11 项任务均已完成，2 项发布元数据 requirement 已同步至长期规格，change 已归档为 `2026-11-03-release-doc-view-1-5-0`；严格规格验证 9/9 通过。其他未完成的 active change 保持原状态。

发布前已重新 fetch 并读取远端：当时 develop/master 均为 `7b2e0a849fe0fe54df6df08792579a361b712428`，没有开放 PR，master 保持保护。该次核查时 GitHub 最新公开 Release 为 1.4.0；Marketplace 插件 `15305` 的 1.4.0 更新 `1200396` 已为 `approve=true / listed=true`，认证读取近期更新未发现待审版本。既有 GitHub 与 Marketplace 凭据可用，值未写入仓库或日志。历史 v1.4.10/v1.4.11 指向较早提交，保留不变；发布前 1.5.0 尚未占用。原工作区 575 个 tracked/non-ignored 文件已记录哈希基线。

## 最终包与验证

| 项目 | 结果 |
| --- | --- |
| 最终插件包 | `build/distributions/Doc View-1.5.0.zip`，543743 bytes |
| ZIP SHA-256 | `23b2f6c96496e660c011f68c904bbbb09ab7d177fed7fd26478395d5e1e9798a` |
| 插件 JAR SHA-256 | `ff2ef5d2ebd6a75a7559bc639d29ba3ef557c0e6b68ea5195e7497108487ec9c` |
| descriptor / change-notes / 双语摘要 | 读回为版本 1.5.0、since-build 261、未设 until-build；change-notes 为 2026-11-03 的六项 Issue 更新；四份欢迎/更新 HTML 含本轮摘要和版本占位符 |
| 全量自动门禁 | 55 suites / 267 tests，0 failures、0 errors、0 skipped；18 个任务全部实际执行，BUILD SUCCESSFUL in 1m 24s |
| Plugin Verifier | `IU-261.22158.277`：`Compatible` |
| 最终包原生烟测 | 官方完整 IU-261.22158.277、隔离 profile 安装同一最终包，完成下述版本/生成/复制/写回场景 |
| 制品清单与测试依赖排除 | ZIP 仅含目录和 `Doc View/lib/doc-view-1.5.0.jar`；JAR 312 条目，未检出测试源码/类或 JUnit 内容 |

正式版本命令为 `./gradlew --offline test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks --no-build-cache`。以上统计来自本次重新执行；不是缓存命中或前轮候选结果。Gradle 仍提示既有弃用特性，不能描述为零编译/构建警告；Plugin Verifier verdict 为 Compatible。

机器证据位于验收工作区的 `evidence/release-1.5.0-gates.log`、`release-1.5.0-test-summary.json`、`release-1.5.0-package.json` 与 `release-1.5.0-plugin-verifier/`。前轮 1.4.0 版本号开发候选的普通/分表复制结果保留在 backlog 验证记录，不混同为本次最终包证据。

### 同包原生验收

- 英文欢迎页实际显示 `Version 1.5.0` 和实体 Markdown、版本/源码注释、路由/字段表三项新亮点；双语四份资源另外从最终包读回核对。
- 实体复制：把 Required 改为 true，描述单元格仍处于编辑状态时点击 `Copy as Markdown`；成功提示和弹窗关闭后，实际粘贴得到 `|id|String|Y|Release 1.5.0 active cell||` 及嵌套字段。`evidence/release150-entity-copy.md` 保存粘贴结果，原实体源码哈希未变。
- Spring `save`：预览显示 `/primary/save`、POST、接口 since `2026-11-03` / version `1.5.0`，请求/响应字段版本分别为 `1.2` / `1.3`。
- 依赖源码：已关联 sources 的 `LibraryModel<String>` 显示 `data`、继承 `libraryId` 及嵌套 `name` 的中文注释；泛型类型和嵌套字段按预期输出。
- 集合层级：List / Map 的合成 wrapper 被隐藏，业务字段 `element` / `key` / `value` 仍保留。
- Dubbo `query`：四个参数齐全；`id` 多行说明为“编号第一行 编号第二行”，`identifier` 为“完整标识”，没有名称前缀串位；boolean 参数和版本信息正常。
- Schema 写回：独立 `EditEntity` 通过原生 Confirm 将 optional 从 NOT_REQUIRED 改为 REQUIRED，将 required 从 REQUIRED 改为 NOT_REQUIRED；描述 `QA "quoted" C:\sample` 的源码转义正确，`title`、`example`、`nullable`、`deprecated` 保留。修改后样例通过 Java 21 `javac` 编译和 11 项反射断言。本结果覆盖本样例的操作，不等同于所有 Schema 组合均经原生验证。
- 该实例日志为 0 条 ERROR，没有 Doc View 异常栈，JCEF sandbox 正常启用；正常 shutdown 后，日常 IDEA 已恢复至欢迎页。本轮没有为补验修改最终候选的运行时文件或重建制品。
- `evidence/release150-final-source-check.json` 回读确认：16 份虚构源码中仅授权写回的 `qa150/EditEntity.java` 改变，实体复制样例源码不变；原工作区的 575 份 tracked/non-ignored 文件无新增、删除或内容变化，HEAD 仍为 `7b2e0a849fe0fe54df6df08792579a361b712428`。

## 已知验证边界

1.4.0 发布记录中有明确未完成的原生场景，详见[原生验收范围](release-1.4.0.md#原生验收范围与未完成项)。本轮补验了适用的生成、复制与 Schema 写回样例；以下其他完整矩阵仍保留缺口，不因版本升级被标为通过。

| 范围 | 已有未完成项 | 本轮处理记录 |
| --- | --- | --- |
| Preview / JCEF | 空结果、失败恢复、取消/快速切换组合与反复开关后的资源基线 | 本轮未修改相关核心实现；保留既有缺口，不宣称完整矩阵通过 |
| OpenAPI Schema 写回 | 编辑、确认、源码读回及其他 Schema 属性保留的完整原生流程 | 本轮独立 EditEntity 已完成必填双向切换、描述转义、Confirm 与其他属性保留；此样例通过，其他组合不扩大宣称，既有 change 状态未改 |
| PasswordSafe 异常路径 | OS 授权拒绝、存储失败后的提示与重试 | 本轮未修改凭据存储，不重测 OS 异常路径，不标为通过 |
| 取消与性能 | 最终包自然耗时分布及可见取消终态的完整组合 | 本轮不作性能提升声明；复制任务的时序/关闭由回归测试覆盖，既有完整矩阵仍未完成 |
| Clear Cache | 最终包目录菜单及选中上下文的完整原生场景 | 本轮未修改 Clear Cache，不扩大原生验收结论，既有 change 不归档 |

本轮未执行三平台真实账号上传；六项 Issue 不涉及上传实现，基线曾有维护者排除真实上传的记录。上传自动回归不等于当前真实账号端到端通过。

## 发布与回读

| 操作 | 结果 |
| --- | --- |
| develop → master PR / 发布提交 | [PR #111](https://github.com/liuzhihang/doc-view/pull/111) 已合并；合并提交 `6a0c6df5d4324af18fefc7da1eaced8591093c2b` |
| v1.5.0 tag | 已创建；peeled SHA 为 `6a0c6df5d4324af18fefc7da1eaced8591093c2b`，与发布合并提交一致 |
| GitHub Release / ZIP / 校验文件 | [Release 已公开](https://github.com/liuzhihang/doc-view/releases/tag/v1.5.0)；下载 `Doc.View-1.5.0.zip` 为 543743 bytes，SHA-256 与上方最终 ZIP 相同，`SHA256SUMS` 核对一致 |
| Marketplace 更新 ID / 渠道 | `publishPlugin` 的 1 项任务实际执行成功；新更新 `1211221`，版本 1.5.0，默认渠道 `default` |
| Marketplace 审核 / 公开状态 | 公开和认证的单条 API 均返回 HTTP 200、`approve=false / listed=false`；已接收、待审核、尚未公开上架，公开版本列表最新仍为 1.4.0 |
| Marketplace 制品回读 | 认证下载 HTTP 200；外层 ZIP 经服务端重包，JAR payload 与最终 GitHub 包逐字节一致，内部 274 个文件无新增、删除或变化 |
| Issue #89 / #90 / #91 / #96 / #97 / #101 | 已逐项评论版本、使用条件及 Marketplace 待审状态，并关闭；回读均为 `state=closed / state_reason=completed`，当前开放 Issue 为 0 |
| 原工作区保留 / 测试环境恢复 | 575 份 tracked/non-ignored 文件与 HEAD 均保持基线；测试实例正常退出，日常 IDEA 欢迎页已恢复 |

发布操作若返回不确定结果，先读取已存在的 tag、Release 或 Marketplace 更新，确认实际状态后再处理，避免重复提交或覆盖制品。token 只通过既有安全本地配置提供，不写入仓库、日志和 Release 正文。

Marketplace 下载文件为 `Doc_View-1.5.0.zip`，549339 bytes，外层 ZIP SHA-256 为 `68e7c7f60d4ec84311900c83812c5ba61fdb9f90fff3733c7641641827f9760e`。服务端 ZIP 与上传 ZIP 的容器字节不同，但条目名称和内容完全一致；两者包含同一个 `Doc View/lib/doc-view-1.5.0.jar`，JAR SHA-256 均为上方 `ff2ef5d2...`。文件内容核对完成不代表 Marketplace 已审核或已列入公开版本列表。

渠道与服务端制品证据见验收工作区 `evidence/release150-marketplace-verification-summary.txt`；Issue 状态回读见 `evidence/release150-issues-readback.json`。六项修复已通过公开 GitHub Release 交付，Marketplace 的剩余状态为等待 JetBrains 审核。

## 回退

需要旧行为时可安装 1.4.0；不重写已公开 tag 或覆盖旧制品。保留现有自定义模板与配置备份，旧版本不保证识别新模板变量。本次新增依赖源码读取没有下载或迁移步骤，取消源码关联后按原有注解或空描述降级。
