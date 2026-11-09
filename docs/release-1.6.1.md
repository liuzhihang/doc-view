# Doc View 1.6.1 发布记录

状态：最终包、自动门禁和本次新增 UI 原生验收已完成；合并/tag/GitHub Release/Marketplace 上传待执行。发布授权已具备。

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
| 原生验收 | 官方 IU 263.7307.200 隔离 profile、同一最终 ZIP；深浅主题高亮、同面板反复切换及 Insert/Restore/Reset、设置保存取消与非法值、目录/说明/图标、列表选择、全部导出均通过 |
| ZIP | `Doc View-1.6.1.zip`；SHA-256 `086b14b853da7bd3643ce4a5b80d3a43fe8eed75711b8a4872ea5bf29cf32f9c` |
| JAR | `doc-view-1.6.1.jar`；SHA-256 `ad32191d3c641e38b76f1664e050dfd8731d3627c55e42eaf9988f20f9577571` |
| 包审计 | 单 JAR，413 entries/348 class，major 69；1.6.1、sinceBuild 263.7031.23、无 until、require-restart=true；无新增依赖/测试类 |

完整步骤、TDD、边界见 [OpenSpec 验证记录](../openspec/changes/archive/2026-11-09-polish-doc-view-1-6-1/verification.md)。未批量归档其他活动 changes。

## 发布跟踪

| 操作 | 状态 |
| --- | --- |
| develop→master | 待执行 |
| v1.6.1 tag | 待执行 |
| GitHub Release 与下载 SHA | 待执行 |
| Marketplace 上传及 owner/download 回读 | 待执行 |

## 剩余边界与回滚

没有未完成的本次新增 UI 门禁。既有完整 1.6.0 原生矩阵未全部重跑，包括 OS 凭据拒绝/失败重试、真实平台上传、HTTP Send、Schema 写回、完整性能取消和安装/卸载组合；不将旧例外或自动测试描述为这些原生场景通过。虚构烟测项目未配置 JDK，不宣称编译验证；完整 447 项自动测试仍覆盖既有功能。未来 build 未验证；重启生效，不承诺热卸载。类说明写入 Markdown/模板，JSON v1 schema 不新增字段。

如发布后出现阻断问题，保留当前版本与证据，选择 1.6.0 回退或发布新 patch；不覆盖 tag 或已公开 ZIP。Marketplace 接收与审核上架是不同状态，以上传后回读为准。
