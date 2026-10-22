# 验证记录

## Round 59 当前状态（2026-10-19）：放宽欢迎与更新页内容区

用户反馈 Round58 页面在宽屏中太窄。本轮生产变更仅为 `welcome.css` 两行：容器上限 1000 → 1800 CSS px，水平 padding 改为 `clamp(24px, 3vw, 64px)`。720px/600px 断点原留白、字号、行高、内容、路由及生命周期均保持。

证据目录 `/tmp/doc-view-reliability-20261018/round59/`：

- 修改前实际浏览器布局：2560px 视口、1000px 外框、左右各 32px padding，正文可用宽度 936px；`before-layout.json`。修改后相同视口为 1800px 外框、左右各 64px padding，正文可用宽度 1672px（约增加 79%），字号仍为 16px。
- 两页 × 深浅两种颜色 × 2560/1800/1200/720/600/320px，共 24 组浏览器布局读数均无页面水平溢出，600px 时左右各 24px 留白；`browser-layout.json`。该预览使用先前 HTML 文案配上当前 CSS，仅证明布局，未替代新包原生验收。
- `./gradlew buildPlugin` 成功：11 tasks 中 7 executed、4 up-to-date，见 `build.log`。本轮不改 Java/平台/API，未重复运行 207 项测试或 Verifier；Round58 的通过项有明确包归属，不计作本包重跑。
- 新包 ZIP SHA-256 `97f5b300a5637644e96a970a258b4759ff4266da97b628ff621f12b2b18e745c`；实际安装主 JAR SHA-256 `cb21f4d347a43190233e49687f730b4473d32d24ec1692c42eb351e1e2fea99c`。安装 JAR 中 CSS 与源码字节相同。
- IU-262.10968.63、隔离 profile、doc-demo 原生 Light + Compact 开：欢迎/更新两页宽屏显示明显放宽；两次 Split Right 后约 460px 窄分屏，正文、返回链接和按钮换行正常。截图 `native-welcome-wide-final.png`、`native-updates-wide.png`、`native-welcome-narrow.png`、`native-updates-narrow.png`。本轮显示分辨率 1920×993，与 Round58 的截图尺寸不同，因此不以跨轮截图像素直接计算增幅。原生深色及 Compact 四组未重复验；本轮深浅 CSS 布局来自上述浏览器测量。
- 已 Unsplit All、关闭测试欢迎标签并正常退出测试实例 PID 69856，恢复原应用配置；普通 IDEA PID 70011 已重新打开 doc-view。测试浏览器标签与 HTTP 预览服务已清理。
- `scope-final.json` 核对两个仓库 HEAD/index 不变，doc-demo 原有文件内容不变；没有提交、推送、发布或 archive/sync。完整键盘和 GitHub 新指南公开正文仍保持原有待办。

## Round 58 实现与验收（后续宽度调整见 Round 59）：HTML 欢迎与更新页已实现、验收并恢复环境

用户本轮明确“继续实现”。本轮把旧七章 Swing 手册替换为编辑器标签中的本地 HTML/CSS 页面，由 JCEF 渲染；完整使用指南和故障排查只在 GitHub 维护。没有继续扩展离线手册，也没有旧 JCEF/Swing 双轨回退。以下证据均属于本轮新包，不借用 Round 57 的旧页面通过项。

证据根目录：`/tmp/doc-view-reliability-20261018/round58/`。临时证据仅用于本地复核，未随插件打包或发布。Translation 的 `WebView` 源码使用 `JCEFHtmlPanel`，IDEA 更新页也通过 JCEF 的 HTML editor 呈现；HTML 是内容格式，JCEF 是底层渲染引擎，不决定页面是否位于编辑器标签。

### 实现与复现证据

- 新增欢迎、更新两个页面，使用随包 HTML/CSS、原生品牌 SVG、IDE 字体/主题参数；宽屏限制阅读宽度，窄屏自动换行，保留段落留白和可见焦点。更新页显示实际安装版本，并明确“开发预览 · 未发布变更”。
- 内部动作仅接受首页、更新、目录、模板设置、平台设置、更新提醒六种白名单值；外链仅接受指定 GitHub 页面，交给默认浏览器。设置帮助的七个入口分别链接对应 GitHub 指南，不 Apply/Reset 或保存草稿。
- 原生首轮暴露 JCEF `loadHTML` 初始请求实际为 `file:///jbcefbrowser/...#url=...`，并非提供的显示 URL；原请求限制会把它拦截成空白。修复允许平台初始非用户、非重定向装载，继续取消用户导航并经白名单分发；HTTP(S) 资源请求被拦截。
- 页面没有 JS 业务脚本、远端字体/图片/iframe，CSP 默认拒绝其他资源。无自动外网加载的结论来自源码、资源 contract、CSP、HTTP(S) 请求拦截和本地装载实测；**没有抓包，不声称整个 IDE 无网络流量**。
- 关闭使用 editor disposal、generation 和后台取消约束，主题监听与 JCEF 资源绑定编辑器生命周期。JCEF 不可用时只显示原因及 GitHub 入口，不另做 HTML renderer。
- RED 记录：`welcome-red.log`（3 个预期失败）、`lifecycle-red.log`（5 tests 中 3 个预期失败）、`settings-red-2.log`（原设置帮助路由断言失败）。`settings-red.log` 是早期测试代码编译问题，不作为有效行为 RED。最终修复均纳入完整 GREEN。

### 自动门禁与产物

执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`，`final-gates.log` 记录 BUILD SUCCESSFUL、18 tasks 全部实际执行；复用了 configuration cache，但测试并非缓存命中。`final-test-results/` 共 **43 suites / 207 tests，0 failures / errors / skipped**。`final-plugin-verifier/` 对 IU-261.22158.277 的结论为 **Compatible**。

相较旧七章手册的 215 tests，旧 Swing renderer 测试被移除并替换为当前 HTML 资源、路由、设置草稿和生命周期 contract；计数变化不表示沿用旧 renderer 通过证据。第一次完整测试有一个过时按钮定位 fixture 失败，修正定位后重新执行全部门禁；只有最终结果记为通过。

- ZIP：`build/distributions/Doc View-1.3.14.zip`；SHA-256 `de28d0110feed290cd7be220fbe5bbc34b76e01dc9a4bddc4f7248d4ffd8e6a0`。
- 主 JAR SHA-256：`e4767060319cc2eb8431544b7286c0565c3bed936861260d350c53a88e01c097`；隔离 profile 实际安装 JAR 与最终 ZIP 内 JAR 字节一致。
- 包内仅有 `welcome/welcome.html`、`welcome/updates.html`、`welcome/welcome.css` 三份欢迎资源，没有 `doc-view-help/` 手册资源；显式声明 `com.intellij.modules.jcef`，生产仍为 Java-only。
- 汇总：`final-gate-summary.json`、`package.json`。构建现有 API 警告不等于验证失败；未据此扩大无关代码修改。

### 原生 IDEA 验收

使用 IU-262.10968.63 / JBR 25，专用 doc-demo 和隔离 profile。菜单自动化历史问题不作为本轮 HTML 页面操作证据。

| 场景 | 实测结果与证据 |
| --- | --- |
| 首次打开 | 清除隔离 profile 的已见版本后启动，欢迎页自动打开，安装版本为 1.3.14；`first-welcome.png`。 |
| 内部动作 | 欢迎/更新/返回/品牌首页链接可达；目录打开 Doc View；模板和平台按钮进入 doc-demo 对应设置；提醒开关即时显示状态。 |
| GitHub 外链 | 实际点击指南目录、仓库、Issues、CHANGELOG，浏览器到达对应 URL，IDE 内页保留。新指南正文未推送，`templates.md` 仍为 GitHub 404；路由通过不等于远端正文可用。 |
| 设置草稿 | 在模板设置输入临时标记，点击“模板使用指南 (GitHub)”后浏览器路由正确；草稿仍在、Apply 仍启用，随后 Cancel；`template-help-draft-ax.txt`。其余六个帮助按钮的精确 URL 和草稿/持久化状态由自动测试覆盖，未声称七个均做原生点击。 |
| 四组主题 | 欢迎和更新两页分别核对 Islands Light/Dark × Compact 开/关，正文、按钮、链接清晰，切换后无旧颜色残留；`first-welcome.png`、`welcome-light-off.png`、`welcome-dark-{off,on}.png`、`updates-{light,dark}-{off,on}.png`。 |
| 窄宽布局 | 原生窗口拖动未实际缩窄，故不计证据；改用两次编辑器 Split Right 得到约 600 CSS px 的窄页，两页均正确换行且无裁切，随后 Unsplit All；`welcome-dark-quarter.png`、`updates-dark-quarter.png`。 |
| 基本焦点 | 正文点击后 Tab 到“查看完整变更记录”，有清晰蓝色焦点框；`focus-light.png`。完整纯键盘专项仍按用户要求暂时跳过。 |
| 多项目 | 用已有 DocViewHelpPeer 新窗口打开，相同版本不自动弹欢迎；手动打开后，模板动作进入 Settings–DocViewHelpPeer，显示 peer 原模板，未串到 doc-demo；取消设置并正常关闭 peer，doc-demo 保持；`peer-welcome.png`、`peer-template-ax.txt`。 |
| 关闭与重开 | Cmd+W 后回到 Java 文件；Find Action → Doc View Welcome 可重新打开；`reopened.png`。 |
| 升级入口 | 隔离 profile 模拟 1.3.13 → 1.3.14，出现原生更新通知；点击“查看更新”进入本版本更新页而非欢迎页；`upgrade-opens-updates.png`。 |
| 禁用提醒持久化 | 更新页关闭提醒后正常退出，XML 保存 false；再模拟升级启动，无 Doc View 更新通知或自动页面，退出后仍为 false 且已见版本更新；`disabled-before-restart.png`、`notification-disabled-persisted.xml`、`disabled-upgrade.png`、`disabled-upgrade-ax.txt`、`disabled-after-restart.xml`。 |
| 日志 | 当前新包验收时间区间未匹配新的 ERROR；`current-errors.txt`。不把旧 profile 的历史错误图标解释成本轮错误，也不声称整个历史日志无错误。 |

### 加载中关闭和释放

`WelcomeJcefLoadPauseJdi.java` 通过 localhost JDWP 只暂停后台加载线程（`DocViewWelcomeEditor$4.run` 第 132 行），其余 UI 线程仍可操作；只读取字段，不调用 IDE 函数改变对象状态。当前构建通过 `javap` 确认断点类和行号。原始证据 `loading-close-jdi.log`：

1. 加载暂停：request=2，generation=2，editorDisposed=false，browserWrapperDisposed=0，jcefClientWrapperDisposed=0，cefClientDisposed=false。
2. 原生 Cmd+W 关闭：editorDisposed=true，generation=3，browserWrapperDisposed=1，jcefClientWrapperDisposed=1，cefClientDisposed=true。
3. 恢复后台线程并 detach 后，标签没有被迟到回调重新打开；手动重开成功。截图 `closed-during-load.png`、`late-callback-still-closed.png`、`reopen-after-loading-close.png`。

该证据证明编辑器、JBCef browser/client wrapper 及该 CefClient 的 disposal 标记，以及迟到回调防护。原始 CefBrowser closing/closed 字段返回 unavailable，**没有测量底层 CEF 进程退出、堆引用或长期内存泄漏**，不得扩大为这些项目的通过结论。

### 环境恢复与剩余边界

- 测试设置恢复 Islands Light + Compact 开，隔离 profile 的 `DocViewApplicationSettings.xml` 恢复验收前原件；`laf.xml`、`ui.lnf.xml`、`ide.general.xml` 与基线字节一致。操作统计、recent-projects 等测试历史仍可变化，没有整目录覆盖恢复。所有测试 IDEA 通过正常退出关闭，没有强制 kill。
- 普通 IDEA PID 66890 已重新打开 `/Users/liuzhihang/workspace/doc-view`，窗口为 `doc-view – ai-context-assessment.md [doc-view]`；`ordinary-restored-ax.txt`、`ordinary-restored.png`。
- doc-demo 35 个验收前文件内容未变，两个仓库 HEAD 与 index entries 均保持；最终差异以 `scope-final.json` 为准。浏览器测试标签与本地预览服务已清理。
- 完整纯键盘验收按用户明确答复跳过，未记通过。GitHub 指南公开正文需获得推送授权后复核；当前 404 不通过“路由已打开”来掩盖。
- `openspec validate doc-view-welcome --strict` 与 `git diff --check` 均通过；当前 change 的 12 项实现任务已按上述验证范围勾选。完整键盘和公开文档的发布前条件保持独立列出。
- 本轮没有提交、推送、发布、sync 或 archive；普通额度仍可用，没有使用 Credits 或兑换重置。

## Round 57 历史状态（已由 Round 58 替代）

### 用户方向修订与当前范围

用户已明确暂时跳过完整键盘验收，并进一步澄清：使用指南、故障排查等只放 GitHub；编辑区应展示类似 Translation / IDE 更新页的 HTML 欢迎与更新说明。proposal/design/spec/tasks 已按“随包本地 HTML + JCEF，完整文档 GitHub”重写。**新页面尚未 apply，当前运行时仍是旧七章手册。** 以下 D 包通过结果是旧实现收尾证据，不是新页面完成状态。

### D 包：布局问题修复与自动门禁

C 包续验在约 600 logical px 打开模板教程后正文变空白，AX 仍有全文；宽度恢复后、切其他章节也未恢复。原因是阅读栏留白基于已被长代码撑大的正文宽度计算，反复布局产生正反馈。真实模板全文与 IDEA HTML Kit 的 RED 记录 preferred width 连续增长 `2539 → 3683 → 4827 → 5971 → 7115`。修复改为依据最近 JViewport 的可用宽度计算留白；保留长代码横向阅读，不以强制跟随视口裁切内容。

- 原始有效 RED：`layout-stability-red.log`。`layout-stability-harness-red.log` 是早期测试框架错误，不作为根因复现证据。
- GREEN：`layout-stability-green.log`、`layout-stability-lifecycle-green.log`；真实模板在连续布局、600/1400px 往返及切章后首标题保持可见，尺寸收敛。
- 完整命令 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`：18 tasks 实际执行；43 suites / **215 tests，0 failures / errors / skipped**；IU-261.22158.277 Verifier **Compatible**。
- 证据：`layout-final-gates.log`、`layout-final-test-xml/`、`layout-final-plugin-verifier/`、`layout-final-gate-summary.json`。
- D 包 ZIP SHA-256：`310167877107727e6447d2a803787c7a82c0b849a5b9efb008becce98707be49`；安装 JAR：`97c77bc610a753ae25447e1e18ceef7221799340a609458c911a58b62299010b`。`layout-final-package-manifest.json` 核对七份同源 Markdown，`layout-final-install-launch.json` 记录安装及 PID 55759。

### D 包原生验收

测试环境为 IU-262.10968.63 / 专用 doc-demo / 隔离 profile。证据根目录 `/tmp/doc-view-reliability-20261018/round57/`。

| 检查 | 实际结果与证据 |
| --- | --- |
| 长模板、宽窄切换 | 约 600px 与宽窗模板正文可见，长代码未触发空白；`layout-final-dark-on-wide-template.png`、`layout-final-dark-on-narrow-template.png`、`layout-final-narrow-template-long-code-left.png`。 |
| Light/Dark × Compact 开/关 | 七章 × 四组共 28 张实际截图，全部逐张视觉核对：正文、标题、段落、列表、代码和链接可读，无主题残留或空白；`matrix-{dark,light}-{on,off}-{home,quick,settings,templates,integrations,troubleshooting,changelog}.png` 及 AX。 |
| 中文同页锚点 | 点击故障排查中的“模板 Reset 风险”后实际滚到目标区域；`layout-final-anchor-self.png`。 |
| 中文跨章节锚点 | 从故障排查“源码入口示例”进入快速开始的“支持的源码入口”；多行 Feign/Dubbo 代码可见；`layout-final-anchor-cross.png`。 |
| 跨章节返回 | 点击快速开始的故障排查链接回到目标页首；`layout-final-link-back.png`。 |
| 键盘滚动 | 正文聚焦后 Cmd+Home、PageDown、PageUp、Cmd+End 正常；`layout-final-keyboard-{home,pagedown,pageup}.png`、`layout-final-code-bottom.png`。 |
| 完整键盘 | Tab 观察到只回到章节下拉框、跳过按钮；尚未完成纯键盘动作/链接激活。用户明确“暂时跳过完整键盘验收”，不记通过、不改系统设置。 |
| 日志 | D 包启动至退出区间未匹配 ERROR 或 Doc View PluginException；`layout-final-log-audit.json`。这不等于整个旧 profile 历史日志无错误。 |

完整 38/38 链接与 12 处 fragment 的实际点击、受控加载关闭/切换属于此前 A 包，详情保留在后文；D 包只做上述最终回归，不冒充再次逐一执行。多项目、草稿、版本提醒也沿用有明确包归属的历史验证，不扩大为新 JCEF 页面证据。

已知非阻塞行为：超长 Velocity 宏行保留横向滚动，未改成软换行。模板宽页截图 `layout-final-code-bottom.png` 可读到完整长行；窄窗横向滚动动作未单独完成验收。主题颜色和留白已改善，但旧手册整体已被新产品方向替代，不继续为其增加浏览器布局功能。

### 环境已恢复

测试实例已恢复 Islands Light + Compact 开；Project 工具窗口已恢复原宽度并隐藏。随后通过 Cmd+Q / Exit 正常退出 PID 55759，恢复 `app-settings-original.xml`，未强制终止进程。普通 IDEA PID 58609 已打开 `/Users/liuzhihang/workspace/doc-view`，CUA 核对实际窗口为 `doc-view – ai-context-assessment.md [doc-view]`。证据：`ordinary-restoration.json`、`ordinary-restored-ax.txt`、`ordinary-restored.png`。

范围核对见 `scope-final-restored.json`：保留 doc-demo 的原有文件和配置、仓库 HEAD 及 index entries。未提交、推送、发布、sync 或 archive；普通额度仍可用，未使用 Credits 或重置权益。

### 旧计划与证据保留

方向修订前 proposal/design/spec/tasks/verification 原件保存于 `round57/before-welcome-direction/`。旧手册的离线资源、渲染、链接和生命周期任务已停止扩展；当时新的 HTML 页面尚未实施；后续实现以本文 Round 58 与当前 tasks.md 为准。下文为更早历史。

## Round 57 锁屏时的历史快照（2026-10-19；已被上方状态替代）

本节保留当时 A/B/C 候选包与锁屏状态，不代表当前结果。最新环境恢复、D 包验收和用户方向修订以本文开头为准。

### A 包：链接、锚点与加载生命周期已通过

A 包的 `final-package.json` 记录 ZIP SHA-256 **89a6fcda64ec5f1a488be236f94de7fc997ad6903d250095030f5afaf67d48d1**，JAR SHA-256 **14a997b285e7312a9eaae8157f926c8de2c27f716177b0c8756b0f31de06396b**。

- 38/38 个离线链接出现位置已实际点击：`native-links.json` 有 37 项，欢迎页至快速开始另由 `new-link-welcome-index-ax.txt` 和 `new-link-welcome-index.png` 记录。
- 12 处 fragment 链接（含中文锚点）均有实际滚动到目标的截图与 AX 记录（`anchor-*.png`、`anchor-*-ax.txt`）。`fragment-audit.json` 的 12/12 slug 静态匹配是补充证据，不替代原生滚动截图。
- 加载期间切换章节的原始 JDI 输出为 `loading-switch-jdi.log`：只暂停 `Backgroundable` 的加载线程；在暂停期间切到设置页后，读取到 `generation=6`、`currentPage=settings.md`；恢复旧线程后仍显示设置页。
- 加载期间关闭帮助的结果记在 `loading-close-observed.md`，并有对应关闭前后 AX/截图。此 Markdown 是对操作和 JDI 状态的转述，不是原始 JDI transcript；它记录关闭后 `disposed=true`、`generation=3`，随后恢复并 detach。该记录明确绑定 A 包 ZIP hash。

链接截图与 `loading-switch-jdi.log` 没有逐项内嵌 ZIP/JAR hash；包归属依赖本轮测试会话记录，只有关闭摘要明确记录 ZIP hash。不能将 A 的链接/锚点/JDI 结果自动算作 C 包复验。

### B 包：窄布局间距候选未通过

B 包 `reading-final-package.json` 记录 ZIP SHA-256 **b81019f8db5263bc6a331aa2269e64d74302e0aa4c3b625b444a12cf1f878e99**、JAR SHA-256 **1541327c4c6b315a13b153c913ce295c41fe84f5f4c1936e9ed2abe45154ae66**。`reading-dark-compact-home.png` 证明正文行宽已限制，但 Swing 未按预期处理 `em` 间距，段落和标题仍挤在一起；不能把该候选当作排版通过。随后通过原生 Project 工具窗口 Stretch 操作缩窄编辑器，`reading-600-editor-before-fix.png` 记录约 600 logical px 的修正前状态。较早的窗口拖动尝试未成功，不计为窄视口证据。

真实 Swing `modelToView2D` 布局回归先测得段间距为 0（`reading-spacing-red.log`），改用 Swing 可处理的 `px` 间距后通过（`reading-green.log`）。正文保持 IDE 字体和主题颜色，宽视口阅读栏限制为 900 logical px，窄视口自适应并保留两侧留白；标题、列表、代码块和表格分别增加间距。六份用户指南按任务整理为短步骤，仍由同一 Markdown 源生成内置 HTML。

### C 包：自动门禁通过，原生覆盖仍不完整

C 包 `reading-spacing-final-package.json` 记录 ZIP SHA-256 **6d5bf6d859d005acaac4143a41de800c8c8951273ef83c7ae63e1caf46c32955**、JAR SHA-256 **5c659542a1131e6aa44fcf23d67f74201e20fd84b7e21c9c71425c1c51cefce9**。`reading-spacing-final-tests/` 的 43 个 XML suites 汇总为 214 tests、0 failures、0 errors、0 skipped；`reading-spacing-final-gates.log` 记录全门禁成功，IU-261.22158.277 Plugin Verifier 为 Compatible。manifest 记录七篇随包 Markdown 均与源码同源，安装 profile 中 JAR 的现场 SHA-256 与 manifest 一致。

C 包目前只完成一项原生排版状态：`spacing-final-dark-compact-narrow-home.png` 与对应 AX 显示约 600 logical 宽的窄面板、Dark 主题、Compact 开启下首页排版成功。随后尝试切到“快速开始”时，IDEA 主机再次锁屏，未取得该章节的最终截图/AX；本次不计为章节通过。C 包仍待完成主题矩阵、其他章节最终回归、全量键盘焦点/导航，以及需要针对新包重跑的链接与锚点回归。

### 当前测试环境收尾状态

只读核对时，PID 49152（/Users/liuzhihang/workspace/doc-demo）仍运行；`reading-spacing-idea.pid` 记录该 PID，进程未监听先前 JDWP 端口 50257。普通 doc-view IDEA 窗口尚未恢复；应用级设置原件备份 `app-settings-original.xml` 仍待恢复。解锁后还需把隔离实例恢复为 Light＋Compact 开、还原 Project 工具窗口宽度并隐藏，再正常退出测试实例、恢复应用设置和普通 IDEA。IDEA 日志只记录插件版本 1.3.14，不记录运行时 JAR hash；配置目录 JAR 与 C manifest 匹配，包归属同时依据本轮安装和重启顺序，不能单靠日志确定。

最终锁屏范围核对 `scope-final-locked.json`：doc-demo 35 个既有文件内容、HEAD、index entries 均未变，两份原设置 XML 哈希相同；本仓库 HEAD 与 index entries 未变，改动集中于本项实现、测试和文档。没有提交、推送、发布、sync 或 archive。在线 GitHub 指南入口路由正确，但因本地文档尚未提交，远程返回 404；本轮未为此推送，发布前需确认远程正文可用。

## 以下为历史验收快照（非当前状态）

从这里开始的较早轮次记录保留其发生时的事实和未完成项；它们不代表当前覆盖度，也不覆盖上方 Round 57 最新状态。任务勾选仍以 `tasks.md` 为准。

## Round 57 中间阶段记录（2026-10-18；已由上方最新状态更新）

本轮先在 IU-262.10968.63 的隔离 profile、专用 `doc-demo` 重现帮助页问题：浏览器专用 Markdown 预览 HTML 被去除 head/script 后交给 Swing，Java 示例的换行/缩进丢失，且出现无功能的 `Copy to clipboard`；故障排查的中文同页锚点点击后仍停留页首。用户同时提供了对应截图。证据目录：`/tmp/doc-view-reliability-20261018/round57/`，包括 `before-fix-anchor-self.png`、`before-fix-anchor-final-ax.txt`、`link-welcome-index.png`。

### 修复与自动门禁

正文继续使用仓库 Markdown 同源打包；改用已有 bundled `org.intellij.markdown` GFM parser/HtmlGenerator 输出适合 Swing 的语义 HTML，代码块保留换行和缩进，标题补充 Swing `scrollToReference` 所需的命名锚点。没有新增依赖或第二套正文，没有引入 JCEF 回退。主题颜色、字体大小仍由 IDE 控制。

两项真实 Swing 渲染回归先分别因代码换行和中文命名锚点失败，再变为 GREEN；旧 RED XML 被后续 Gradle 覆盖，保留的命令/失败断言说明是 `renderer-red-note.md`，不得当作原始 XML。最新定向 GREEN XML 为 `renderer-green-WelcomeLifecycleTest.xml`、`renderer-green-WelcomeHelpTest.xml`（12 tests）。第一次整包 211 tests 通过，但 Verifier 因 `generateHtml(Function3)` 已弃用而失败；随后改用公开 `DefaultTagRenderer` 与 `generateHtml(TagRenderer)`。

最终执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`：18 tasks 全部实际执行，43 suites、211 tests，0 failures/errors/skipped；构建成功，IU-261.22158.277 Verifier `Compatible`，无 deprecated/experimental/internal API 报告。报告保存在 `final-tests/`、`final-verifier/` 和 `final-gates.log`。

最终 ZIP SHA-256：`89a6fcda64ec5f1a488be236f94de7fc997ad6903d250095030f5afaf67d48d1`。实际安装 JAR SHA-256：`14a997b285e7312a9eaae8157f926c8de2c27f716177b0c8756b0f31de06396b`。7 篇随包 Markdown 与仓库源字节一致；`final-package.json` 记录对应产物。

### 已完成的原生交互

在修复前的 Round52 包上，本轮已补齐以下状态交互；渲染修复没有修改这些状态路径：

- 同 profile 第二项目使用本轮创建的 `round57/DocViewHelpPeer`。B 首次打开不重复自动显示首页；手动 Help 为该项目创建自己的标签，重复打开复用；B 关闭帮助不关闭 A。A、B 章节独立。
- B 的模板显示 `HELP_PEER_B`；输入未保存标记后打开模板教程、返回设置，草稿和 Apply 状态保持；Cancel 后文件未变。A 仍显示原模板，无 B 标记。证据：`peer-*.png`、`project-a-*.png`。
- A 常规、Generation Rules、Platform Integrations 帮助入口打开正确章节。ShowDoc、YApi 地址及语雀用户名输入虚构未保存草稿后打开指南再返回，草稿保留；最后 Cancel，未 Apply/上传，两个现有项目设置 XML 哈希不变。证据：`settings-*-help*`、`showdoc-*`、`yapi-*`、`yuque-*`。
- 同版本启动无重复提示。把隔离应用级版本夹具置为 1.3.13 后启动，只有更新通知、没有自动打开帮助；点击“查看更新”进入更新记录。再在原生 UI 取消“新版本更新时提醒我”，退出并重置旧版本夹具，重启没有通知，手动 Help 仍可打开。证据：`startup-same-version*`、`upgrade-*`、`disabled-upgrade-*`。

在最终修复包上，使用空应用级版本夹具确认首次自动打开首页；在 Islands Dark / Compact 开检查了首页、快速开始表格及多行 Java/Feign/Dubbo 代码，换行和缩进恢复，浏览器复制伪控件消失。证据：`new-first-welcome.png`、`new-code-java-dark.png`。

最终包已实际点击 **13/38** 处离线链接：欢迎页全部 6 处；快速开始导航表 4 处、步骤中的平台集成 1 处、Dubbo 示例后的故障排查 1 处、末尾故障排查 1 处。目标章节均正确。`native-links.json` 记录其中 12 处的来源/目标/点击，首页快速开始的另一处证据为 `new-link-welcome-index-ax.txt`。38 处清单在 `help-links.json`；其中 12 处 fragment 的静态目标匹配通过，见 `fragment-audit.json`，不能当作原生滚动验证。

### 锁屏中断与明确未完成项

继续点击快速开始导出表中的“设置说明”时，工具返回 `The Mac is locked and automatic unlock could not unlock it.`。该次点击未计入通过。剩余 25 处链接、全部修复后中文锚点实际滚动、完整 Light/Dark × Compact 开/关矩阵、加载期间关闭/切换、完整键盘焦点与公开外链仍待补。JDI helper 只准备了代码，没有连接或产生生命周期验收结果。

锁屏时测试 IDEA PID 44569 仍在运行，普通 doc-view IDEA 已在开始时正常退出、尚未恢复。隔离应用级版本夹具和主题仍需收尾恢复；原始应用设置备份是 `app-settings-original.xml`。不能把此状态记作恢复完成。测试使用的 JDWP 仅监听 loopback 50257，尚无 helper 连接；解锁后完成场景并正常退出测试实例、恢复普通窗口。

截至锁屏，`doc-demo` 35 个原有 tracked/untracked 文件内容、两个 DocView 设置文件和 Git index entries 均与本轮基线一致；Doc View 仓库 HEAD 和 index entries 未变。范围核对见 `scope-before-lock.json`。没有提交、推送、发布、sync 或 archive。


## 锁屏期间的剩余风险核对（2026-10-18；历史快照，已由上方最新状态更新）

原生工具再次返回 Mac 已锁屏且自动解锁失败，等待手动解锁，未继续 UI 操作。静态复核和既有 `ConfigurationExperienceTest.testOpeningEveryHelpEntryPreservesDraftsAndSavedSettings` 证明四个主要帮助入口不会调用 Apply/Reset，且草稿、修改状态和已保存值保持不变；平台子页仅有帮助入口测试及源码依据，不能扩大该断言范围。`FileEditorManager.openFile(file, true)` 是否在 Settings 模态对话框仍打开时让帮助正文实际可见、可聚焦，需要原生验收；task 3.2.e.d 保持未完成。没有为绕开此验证改动生产实现。

## IU262 原生局部验收

原生 profile build：`IU-262.10968.63`；界面显示 Doc View `1.3.14`。已实际操作并通过：首次打开显示首页与七章选择器；Find Action 搜索 `Doc View Help` 后按 Return 打开 Doc View 编辑器，显示版本和随包离线内容；章节选择器按 Down 3 次再按 Return 打开模板教程，辅助文本包含完整 Markdown 示例正文；首页的 Template Settings 按钮打开 Tools 设置页。证据文件：`/tmp/doc-view-reliability-20261018/native-acceptance-progress.md`、`/tmp/doc-view-reliability-20261018/native-help-reopen-ax.txt`、`/tmp/doc-view-reliability-20261018/native-help-template-chapter-ax.txt`。后一个辅助文本对应成功打开的模板教程章节。

此前的核对只覆盖模板教程这一章和入口。Round 29 后，帮助章节正文和一个内部入口的覆盖增加，详见下节；Round 33 又验证了同一项目内的重复打开和关闭重开标签行为。这些结果仍不代表全部内部链接、设置草稿保留、跨项目标签或章节往返都已验证。

仍待执行的原生步骤：在同一 profile 打开项目 B 并核对首次自动打开行为与 A/B 项目设置隔离；除首页“快速开始”外的内部链接、设置指南往返不丢草稿、模板/平台设置说明链接不隐式 Apply、关闭加载任务、完整主题矩阵和完整键盘导航；验证升级提醒与关闭提醒设置。单项目重复打开与关闭重开已在 Round 33 覆盖。已执行一次 IslandsDark → IslandsLight → IslandsDark 切换，见下节。源码生成复验如需项目，应使用专用 `/Users/liuzhihang/workspace/doc-demo` 并保留其已有修改。真实平台账号和上传不属于首页验收。

## Round 33 单项目帮助标签复用

在 IU262 的单个 `doc-demo` 项目中，通过 Find Action 打开 `Doc View Help`，再次执行同一 Action 后界面仍只有一个 Doc View 首页标签。随后按 Cmd+W 关闭首页，IDE 返回到之前打开的 `UserFacade` 双参数 Markdown 页面；再次执行 Find Action 后，Doc View 首页重新打开并显示版本 `1.3.14`。本轮验证的是同一项目内的重复打开及关闭后重开，不覆盖第二项目的自动打开、跨项目标签行为或 A/B 设置隔离。

证据：`/tmp/doc-view-reliability-20261018/native-round33-help-reuse.png`、`/tmp/doc-view-reliability-20261018/native-round33-help-closed-ax.txt`、`/tmp/doc-view-reliability-20261018/native-round33-help-reopened-ax.txt`。

## Round 29 章节正文与入口核对

在 IU262 `doc-demo` profile 中逐项查看章节选择器的实际 Value 与正文：快速开始（`native-help-chapter-1-ax.txt`，正文为“Doc View 用户指南”及快速开始内容）、设置说明（`native-help-chapter-2-ax.txt`）、平台集成（`native-help-chapter-4-ax.txt`）、故障排查（`native-help-chapter-5-ax.txt`）和更新记录（`native-help-chapter-6-ax.txt`）。五份正文分别与对应随包页面内容相符；此前的首页与模板教程正文证据见本节前文。因此七篇页面均已有正文显示核对，但不表示其全部链接或交互都已验证。

另从首页实际点击“快速开始”内部链接，章节选择器转到快速开始并显示其正文，见 `/tmp/doc-view-reliability-20261018/native-help-internal-link-ax.txt`。实际点击首页“平台设置…”按钮后，Settings 打开 Tools → Doc View → Platform Integrations；页面显示三个平台入口，Apply 禁用，见 `/tmp/doc-view-reliability-20261018/native-help-platform-settings-ax.txt`。这只核对该入口和空闲页面状态，不证明其他链接、设置草稿往返或项目隔离。

## IU262 浅色主题单项核对

实际使用 Ctrl+grave 将 IslandsDark 切换为 IslandsLight；目视检查快速开始正文及 API 树中的代码主文字、灰色业务次文字仍可读，随后恢复 IslandsDark。证据截图：`/tmp/doc-view-reliability-20261018/native-help-light-theme.png`。这只核对一次浅色主题显示与恢复，不代表完整主题矩阵、键盘焦点或所有章节渲染均已验证。

日期：2026-10-18。基线：`develop` / `00f1f944c7b22cc03219f06324d177fecf9e848f`。本项在用户授权的路线图 1～7 范围内实施，保留已有工作区改动，不提交、推送、发布、sync 或 archive。

## 较早阶段状态（最新结果见 Round 57）

proposal、design、specs、tasks 已完成且 `openspec validate doc-view-welcome --strict` 通过。随包资源、原生 FileEditor、手动 Help 入口与应用级首次/更新策略已实现，9 项专项测试及整批 96 项测试通过；平台 API 修正后的整批自动门禁已通过（见末节）。IU262 原生首页入口、版本、七章选择器、七篇正文、一个首页内部链接、同一项目内重复打开及关闭重开、Template Settings 与 Platform Integrations 入口，以及一次浅色主题切换已部分核验；多项目、其余链接/设置往返、完整主题矩阵与升级提醒仍待补。

## 自动验证记录

- 首次资源 RED：`WelcomeHelpTest` 2 项全部失败，原因是缺少随包指南与更新记录资源。原始 XML：`/tmp/doc-view-reliability-20261018/round3-TEST-com.liuzhihang.doc.view.contract.WelcomeHelpTest.xml`。
- 新增链接与生命周期 RED：`./gradlew test --tests '*Welcome*Test' --tests '*ApiTreeNavigationTest' --tests '*ApiTreeRefreshTest'` 实际执行 16 项，3 项失败。WelcomeHelp 3 项中 1 项失败（缺少受控链接解析器），WelcomeLifecycle 2 项均失败（缺少版本决策函数与自有 FileEditorProvider）；目录树 11 项通过。不能把该混合命令写成整体成功。
- 第二次 RED 原始 XML 为同一临时证据目录中的 `round4-TEST-...WelcomeHelpTest.xml` 与 `round4-TEST-...WelcomeLifecycleTest.xml`。后续增加了非法资源读取、标签复用和销毁后回调验证，最终数量以 GREEN 报告为准。
- API 静态核对：目标 261 的 `ShowSettingsUtil` 提供公开 Predicate 重载，用 Configurable ID 精确选择平台设置；没有把 ID 误传为显示名。正文使用公开 `HTMLEditorKitBuilder`，未采用标记 Experimental 的 `JBHtmlPane`，未增加 JCEF、语言或依赖。
- 专项 GREEN：`./gradlew test --tests 'com.liuzhihang.doc.view.contract.ConfigurationExperienceTest' --tests '*Welcome*Test' --rerun-tasks`，实际 11 项中只有配置页的 3 项预期 RED 失败。WelcomeHelp 4 项、WelcomeLifecycle 4 项均为 0 failures/errors/skipped；完整编译通过。XML 已独立备份至同一证据目录的 `round5-TEST-...WelcomeHelpTest.xml`、`round5-TEST-...WelcomeLifecycleTest.xml`。测试覆盖资源字节一致、非法读取/链接、版本决策、provider 范围、真实 editor 释放、FileEditorManager 同项目复用与关闭重开、陈旧和关闭后错误回调。该混合命令整体退出失败，不作为整批 GREEN。

## 待验收范围

1. 整批全量回归、构建与 IU 261 Verifier 已完成，见末节。
2. 同 profile 多项目的首次打开与 A/B 项目设置隔离、升级通知和关闭提醒策略。单项目重复打开/关闭重开已由 Round 33 原生验收。
3. 除 Round 46 已验证的 Template & Output 帮助往返保留草稿外，其余内部链接、平台设置帮助往返、完整主题矩阵、完整键盘操作及关闭加载任务。
4. 已核验子项对应的最终 ZIP/加载 jar 与日志归档；当前文件记录的证据范围仅限上述实际打开页面。

本记录只证明已执行步骤；读取源码、fixture、261 Verifier 与 262 原生操作分别记录，不相互替代。

## 较早的原生环境阻碍记录

2026-10-18 的早期尝试中，Computer Use 返回 `The Mac is locked and automatic unlock could not unlock it.`；当时未操作普通 IDEA，也没有把那次尝试记作验收。之后在 IU262 隔离 profile 中完成了上面的部分原生核对，因此锁屏信息仅是历史记录，不再代表当前所有原生步骤均未执行。

本批检查 README、路线图和六篇用户指南 Markdown 的本地链接，发现首页更新记录相对链接仅在打包目录有效。现已改为仓库内也有效的 `../../CHANGELOG.md`，离线解析器仍映射内置更新页；68 条本地文件链接全部存在。新增在线链接标记与该路径映射测试已在 round7 执行通过；后续扩大检查至 README 和 docs 中 141 条本地文件链接，均存在。

## 本批全量回归与平台检查

- `./gradlew test --rerun-tasks`（round7）：96 tests，0 failures、0 errors、0 skipped；WelcomeHelp 5 项、WelcomeLifecycle 4 项均通过。原始 XML 独立保留为 `/tmp/doc-view-reliability-20261018/round7-TEST-*.xml`。
- 上轮 round6 的首页失败来自 light fixture 的 FileEditorManager 返回通用文本 editor，不能据它反射首页私有字段。现由真实 provider 单独创建并验证首页的初始章节/单次加载，fixture manager 仅断言虚拟文件的复用与关闭重开；它不替代原生 provider 选择或标签行为验收。
- 首次 `verifyPluginProjectConfiguration buildPlugin verifyPlugin`：前两项通过，Verifier 因 3 处 deprecated API 失败，其中本页使用 `ReadAction.compute`。已改用当前项目公开 Java API `Application.runReadAction(Computable)`，保留后台 PSI read action；复跑结果另记。

## 第 1～7 项整批自动门禁通过

2026-10-18 执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`，18 个任务实际执行并成功；96 tests，0 failures/errors/skipped。本项相关用例全部通过。IU-261.22158.277 Verifier 为 Compatible，无 deprecated、experimental 或 internal API 使用报告。

原始 XML `/tmp/doc-view-reliability-20261018/round8-TEST-*.xml`；报告备份 `verifier-1-7-green/`。该阶段 ZIP SHA-256 为 `b9e1365161403069ec251311b7403ab8f27caaff943ebd2bd2efae2f91997b1d`，副本 `Doc-View-1-7-green.zip`。包仅含生产 jar；descriptor 为 1.3.14 / since-build 261，Java/Markdown 依赖正确，无测试框架/测试 class，7 份帮助 Markdown 与仓库源字节一致。

Round 8 所在阶段的原生验收因 Mac 锁定而跳过；这条记录仅对应当时的阶段。后来完成的 IU262 局部原生验收见本文件前文。本 SHA 仅对应 Round 8 包。

原生环境复查（2026-10-18T03:34:26+08:00）时仍曾返回锁屏提示；该记录早于后续成功的 IU262 隔离 profile 局部操作，不覆盖前述已完成项。

## Round 46 从模板设置打开帮助并返回草稿

Round 46 在 IU 262 隔离 profile 安装 Round 42 插件 JAR（SHA-256：`993fdc650c5b2f0a4a4aa0c45d292c8130051c1938df4df36f63b1263019a2fc`）。在 `Template & Output` 的模板编辑器输入未保存标记 `QA_HELP_DRAFT`，点击“打开模板使用指南”后，帮助编辑器实际显示模板教程章节；切回 Settings 后，编辑器仍包含 `QA_HELP_DRAFT`，Apply 仍可用。点击 Revert 后回到已保存模板，Apply 和 Revert changes 均禁用。证据：`/tmp/doc-view-reliability-20261018/native-round46-help-from-draft-ax.txt`、`native-round46-help-return-draft-ax.txt`、`native-round46-help-revert-ax.txt`。

这证明模板设置页的该帮助入口可见且往返没有丢弃草稿，也没有替用户 Apply；不覆盖 Platform Integrations 子页的帮助入口、其他内部链接、完整键盘流程或其他设置页草稿。
