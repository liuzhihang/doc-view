# 2026-11-07 full-17：Keymap、弹窗、菜单与资源续验

本轮继续执行用户授权的修复与隔离原生验收。保留 Junie，不改普通 IDEA profile，不提交、推送、发布或归档；正式 MCP 等待剩余原生及文档库验收。先前 full-14/16 证据保留，不能替代本包结果。

## 修复与自动检查

- full-16 会话日志中七个 Doc View 注册组缺少标题，Keymap 回退为内部 ID。新增 `PluginDescriptorLocalizationTest.testEveryRegisteredActionGroupHasLocalizedVisibleText` 后真实 RED：`Action group liuzhihang.doc must declare a readable English fallback`，1 项测试失败、进程 exit 1。
- 为七个组补 descriptor 英文 fallback 和中英文 `group.<id>.text`；所有十个注册组均有非空本地化标题。与 full-16 打包 descriptor 对比，忽略 group text 后 actions 结构完全一致；未改 ID、子项顺序、popup/compact 或启用 dormant 浮动入口。
- 针对性 GREEN：4 suites / 15 tests，零失败、错误、跳过。
- `test buildPlugin verifyPluginProjectConfiguration verifyPlugin --rerun-tasks --console=plain`：70 suites / 362 tests，零失败、错误、跳过；18 tasks 实际执行，1m53s，exit 0。
- Verifier 1.411 串行检查 IU `263.7031.23` 和正式 `263.7307.200`：均 `Compatible. 16 usages of experimental API`，两次实际 exit 0。production 没有 compatibility、deprecated、internal 或 override-only 问题；15 条 test-only deprecated/removal 和 Gradle 10 弃用提示仍保留。

## 包与实例身份

| 项目 | 本轮读回 |
| --- | --- |
| ZIP SHA-256 | `5aaf24485507f548dd000f4c392fcf7162acd7444711e97a23f61a233d0ce09e` |
| JAR SHA-256 | `b0791ad3c1f9c5b662e703f6d4a435891123750e619017af139c6299d913ec47` |
| ZIP / class | 724,949 bytes；一个生产 JAR；331 classes 全部 major 69 |
| 输入清单 | 229 个生产/构建输入 SHA，构建后零变化 |
| 原生目标 | 隔离 `IDEA Native Test.app`；product-info 为 `263.7307.200` |
| 进程 | PID `80916`；JBR `25.0.4.1`；无 javaagent |
| 路径 | `build/native-validation/2026-11-05/profile/` 下独立 config/system/plugins/log；虚构项目为同日 `fictional-project` |
| MCP / Junie | MCP UI 显示 Disabled；Junie 保留 |

本轮原始材料位于 ignored 的 `build/native-validation/2026-11-07/full17/`，包含 RED/GREEN、全量 XML、两版 Verifier、包清单、启动身份和 `native-results.json`。资源原始输出限制为 0600。

## 已观察到的原生结果

| 场景 | 实际结果与边界 |
| --- | --- |
| Keymap 中英界面 | 英语搜索 Doc View，API Catalog、Editor、Editor Context、Tool Window Maintenance 显示可读组名；Find Action 也显示 Preview Menu/Toolbar 和 Tool Window Toolbar 的可读组名。通过原生设置切换简体中文并重启后，组标题显示对应中文名称，Apply 保持 disabled，未改快捷键。 |
| 参数弹窗跨应用 | 修改描述但不写回；Pin 开与关分别切换 Finder，通过坐标点击切换列表/图标视图；返回 IDEA 草稿保留且可以继续编辑。源文件 SHA 保持 `ab9d766cf7e040391066154d5e1068093e3ff5896b93367f262b39532db65e91`。 |
| 预览跨应用 | 实际 JCEF 显示 findUser、GET、路径及表格；Pin 开与关时 Finder 坐标点击均有效，返回后内容保留。应用截图不能证明完整桌面的合成层级，视觉遮挡仍未确认。 |
| 参数 Copy JSON | 点击真实按钮，剪贴板粘贴到原生文件搜索框读回 displayName 空字符串、age 为 0；复制未写源码。 |
| 预览 Copy | 真实按钮复制后粘贴读回 Markdown 名称、GET、路径及参数。 |
| 中文参数写回与 Undo | “复制 JSON / 写回源码”文字及平台图标实际可见。修改描述为 `Full17 native "quote" \ path`、必填 false→true，点击写回源码并保存，实际得到正确 Java 转义及 `Schema.RequiredMode.REQUIRED`。写回文件 SHA `e574a781fa6b545349ea7d576ec77da32ac7ef24d8c32fb43b351c7eeebbdfcd`；一次原生 Undo + Save 恢复最初 SHA，其他字段保持原状。 |
| 设置子窗口 | 预览和参数 Settings 均打开 Doc View 设置；Cancel 返回源码，未修改选项。中文深色设置页的小说明字号、灰色层级已实际核对。 |
| 预览 Upload 菜单 | YApi / ShowDoc / YuQue 可见；Down 后 Escape 返回预览，未选平台、未上传。 |
| 预览 Export | 路径选择器 Cancel 返回源码；重开后实际导出 `full17/exports/findUser.md`，604 bytes，SHA `a6fd207dd31704213880452682912113bc1e98be47364c60ac2dba28044f7514`。 |
| 工具栏 Export | 四个入口可见：Export Markdown、Generate or Update Project Docs、Open Project Docs、Search Project Docs；Escape 取消，没有执行生成。 |
| 目录 Enter | 选择 findUser 方法后 Enter 实际打开该方法的 Markdown 编辑器；验收后关闭该编辑器。 |
| 工具窗口 `…` 与目录右键 | AX 点击、坐标点击、标题右击、树右击及 Shift+F10 均未取得可观察菜单。精确 release SDK 的 Options / PopupHandler 路径使用 heavyweight Swing JBPopupMenu，主窗口级 AX/截图可能漏掉独立弹层；插件接入静态检查未发现缺陷，日志也没有 PopupHandler/JBPopupMenu/ActionPopupMenu 相关失败 WARN。仍不能声称该菜单已原生通过，Shift+F10 亦不能独立证明鼠标右键路径失败。 |

跨应用交互通过不等于完整视觉遮挡通过；应用切换没有自动写回或丢失草稿。HTTP/Dubbo 原生菜单限制、两项目 Clear Cache、中英/深浅/Compact/窄窗完整矩阵仍单独待验。

## 资源对照

使用同一 PID、无 agent、保留 Junie，先取得未打开 Doc View 的 baseline，再记录真实操作和三轮参数 Escape / JCEF 预览 Escape。cycle 1 额外切换 Markdown/HTML，cycle 2、3 直接使用保留的 HTML 模式；均实际观察到完成渲染才关闭。工具窗口打开 Markdown 这一额外操作已记录，采样前关闭其编辑器，不能将本轮当作完全无其他活动的微基准。

基线、打开态、cycle 1/3 关闭后与最终 quiet 的 live histogram 分开保存；cycle 2 仅采进程/FD。先采 RSS/FD，再执行 histogram；未额外执行 `GC.run`，NMT 未启用。JBCefApp/CefApp 为共享长寿对象，不要求归零。关闭初期仍有最后一次表单和 JCEF 对象，需以 quiet 及必要的 retained-path 诊断区分延迟释放和持续保留。

第三轮关闭约 149.8 秒后取得 quiet 样本：ParamDocEditorForm、ParamTreeTableView、PreviewForm、MarkdownJCEFHtmlPanel、RemoteBrowser、JBCefClient、CefClient 均为 0；JBCefApp / CefApp 各 1，为本次首次渲染建立的共享对象。此前 cycle 1/3 的最后一次对象未归零属于本轮观察到的延迟释放，不定性为持续泄漏。

| 样本 | IDEA RSS（KiB） | IDEA FD | 可归属 CEF 子进程数 |
| --- | ---: | ---: | ---: |
| 使用前 baseline | 3,182,304 | 527 | 未采子进程基线 |
| 打开态 | 3,575,376 | 521 | 5 |
| cycle 1 关闭后约 14 秒 | 3,573,232 | 519 | 4 |
| cycle 2 关闭后 | 3,583,024 | 519 | 4 |
| cycle 3 关闭后约 30 秒 | 3,584,912 | 519 | 4 |
| quiet | 3,577,952 | 517 | 4 |

CEF 各 PID 的 FD 没有逐轮增长；RSS 不要求回到未初始化 JCEF 的启动值。quiet Thread.print 的 EDT 为 WAITING，没有执行中的 `at com.liuzhihang.doc.view...` 栈帧；仍打开的工具窗口存在受管 Invoker scope 名称，该 scope 名称不是运行中的预览任务。

上述有限三轮 Java/进程资源回归通过，不证明 CEF C++ allocator/native peer、所有 listener 或长期 soak/取消/关闭项目/卸载矩阵。没有因初期非零直接做 heap dump，也没有把本轮样本转换为整项 R2 或其他 lifecycle change 完成。原始样本时间、class counts、线程和边界保存在 `full17/gc/summary.json`；退出后的子进程回收另行读回。

## 日志、恢复与剩余门槛

- 英语 PID 80916、中文 PID 82510、恢复英语 PID 82665 的三个会话分别为 INFO/WARN `893/456`、`670/443`、`317/6`；均无带时间戳的 ERROR/FATAL、Doc View 空组标题消息或上述 popup 失败 WARN。平台其他 warning 保留，不宣称整个 IDEA 零警告。
- 英语资源会话在原生语言重启时正常退出；原 PID 80916 及四个 CEF 子进程 81313/81316/81317/81319 均已消失。最终恢复英语后的实例也通过原生 Exit 正常退出，三个 IDEA PID 均不再存在。
- Junie `air-plugin` 的 277 个 JAR 与本机正式安装逐一 hash 一致。MCP 一直 Disabled。没有访问真实上传平台或调用 Junie 云端会话。
- 英语恢复通过实际界面读回；区域 AMERICAS 未变、主题保持 Islands Dark。Finder 恢复原分栏视图并关闭临时窗口；虚构源码全部 hash 恢复，229 个构建输入与已验包仍一致。
- HTTP/UI task 5.10 完成，累计 31/40；5.8 的完整桌面视觉遮挡、5.9 全矩阵继续未完成。基线 R2/4.2 未勾选。可见菜单展开、完整参数/异常/取消矩阵、CEF native peer/listener/性能及文档库剩余门槛未通过，正式 MCP 不推进。

下一次原生续验优先取得能覆盖独立 popup window / 完整桌面的观测能力，补 `…`、目录上下文和跨应用遮挡，再处理对应剩余矩阵；不为观测盲区改动已接通的生产菜单。
