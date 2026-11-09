# Doc View 1.6.1 发布记录

状态：`v1.6.1` 已打 tag，GitHub Release 已公开并下载校验；Marketplace 已接收（update `1215689`），当前待审核/上架。自动门禁、原生验收及文案修订后的补验/等价范围见下文。

## 交付内容

- 类与方法普通 Javadoc 默认首行标题，后续正文分别保留为类说明/接口描述；标题上限默认 80、可设 1–200 个 Unicode code points，注解、tag 和描述不截断。
- 生成规则新增方法仅首行和后续正文描述开关；`${DocView.classDesc}` 可用于自定义模板。已有自定义模板需主动加入该变量或恢复默认。
- 模板源码 Markdown/Velocity 高亮、变量目录强调；Preview 在同一内容区域切换渲染，插入/恢复默认/Reset 自动回源码。JSON/json 代码块随主题高亮。
- 弱化 popup 目录边框，General 灰色小字说明、GitHub Star 与平台清理图标，README 分层精简。
- 保留 1.6.0 列表类名快照修复，补齐 Export All 的简单类名快照，避免 EDT 直接读 PSI。

## 验证和制品

| 项目 | 实际结果 |
| --- | --- |
| 全量自动门禁 | 84 suites / 447 tests，失败/错误/跳过均 0；test、verifyPluginProjectConfiguration、buildPlugin、verifyPlugin 全部实际执行 |
| Plugin Verifier | IU 263.7031.23 与 263.7307.200 均 Compatible，各 16 项 experimental API；无 blocking finding |
| 原生验收 | 官方 IU 263.7307.200 隔离 profile；前一 SHA 完成深浅主题、同面板及 Insert/Restore/Reset、保存取消、样式、选择/导出全矩阵；最终 SHA 补验启动、目录/描述、列表选择与浅色 JSON。两包只差 change-notes 图标称谓，348 class 及其余资源相同；详细绑定见验证记录 |
| ZIP | `Doc View-1.6.1.zip`；SHA-256 `dfef1c63f72a4049d6f108ebdb23697a158e592a100c4b9ecf4168bfa433ddb3` |
| JAR | `doc-view-1.6.1.jar`；SHA-256 `db8e09f5b2bd59af6354267549d8ca2c6010690edc61583a45f11233a03bc24d` |
| 包审计 | 单 JAR，413 entries/348 class，major 69；1.6.1、sinceBuild 263.7031.23、无 until、require-restart=true；无新增依赖/测试类 |

完整步骤、TDD、边界见 [OpenSpec 验证记录](../openspec/changes/archive/2026-11-09-polish-doc-view-1-6-1/verification.md)。未批量归档其他活动 changes。

## 发布跟踪

| 操作 | 状态 |
| --- | --- |
| develop→master | [#117](https://github.com/liuzhihang/doc-view/pull/117)、[#119](https://github.com/liuzhihang/doc-view/pull/119) 合入 develop；[#118](https://github.com/liuzhihang/doc-view/pull/118) 合入 master |
| v1.6.1 tag | 指向发布提交 `255be07017c4b233eb27aca32beef9ce616abb90`；与冻结提交 `4baef2798674362b2b0e1b4020bddeaca37e5381` tree 相同，334 个构建/测试输入逐项匹配 |
| GitHub Release 与下载 SHA | [v1.6.1](https://github.com/liuzhihang/doc-view/releases/tag/v1.6.1) 正式非预发布；下载 `Doc.View-1.6.1.zip` 与 SHA256SUMS 校验通过 |
| Marketplace 上传及 owner/download 回读 | [Doc View](https://plugins.jetbrains.com/plugin/15305-doc-view) update `1215689`、1.6.1、since 263.7031.23、无 until；`approve=false` / `listed=false`，待审核上架；下载内部文件逐项匹配 |

## 剩余边界与回滚

没有未完成的本次新增 UI 门禁。既有完整 1.6.0 原生矩阵未全部重跑，包括 OS 凭据拒绝/失败重试、真实平台上传、HTTP Send、Schema 写回、完整性能取消和安装/卸载组合；不将旧例外或自动测试描述为这些原生场景通过。虚构烟测项目未配置 JDK，不宣称编译验证；完整 447 项自动测试仍覆盖既有功能。未来 build 未验证；重启生效，不承诺热卸载。类说明写入 Markdown/模板，JSON v1 schema 不新增字段。

如发布后出现阻断问题，保留当前版本与证据，选择 1.6.0 回退或发布新 patch；不覆盖 tag 或已公开 ZIP。Marketplace 接收与审核上架是不同状态，以上传后回读为准。

## 2026-11-09 发布回读

GitHub 下载 ZIP SHA 与上表最终包一致。Marketplace 外层 ZIP 被平台重压缩，SHA-256 为 `f705782a1eed1a0a8ce87655b4d56d3633bded621a211f5f9ce4e819e1b6461d`；内部 JAR SHA-256 为 `db8e09f5b2bd59af6354267549d8ca2c6010690edc61583a45f11233a03bc24d`，全部文件内容与最终制品一致。可下载不等于公开上架。

首次以 `--offline` 执行 Gradle 时整体成功但 publishPlugin 被明确跳过，回读也无该版本，未算上传成功。随后按原有在线 `publishPlugin` 实际执行成功（8s，13 actionable tasks，3 executed/10 up-to-date）；上传前后最终 ZIP SHA 未变。附带 patchChangelog 格式改写已保存本地诊断并恢复冻结源文件，没有进入发布记录提交。

发布证据位于本地任务的 `doc-view-1.6.1-evidence/`：release-commit-binding.json、github-release-readback.json、github-download/、marketplace-publish-result.json、marketplace-owner-readback.json、marketplace-artifact-readback.json。完整原生记录和文案修订等价范围见归档 verification.md；原始 IDE profile/日志不公开。
