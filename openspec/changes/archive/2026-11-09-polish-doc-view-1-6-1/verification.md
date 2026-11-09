# 1.6.1 验证记录

## 范围与最终制品

日期：2026-11-09。从已发布 1.6.0 创建独立工作区，已纳入 origin/master `6fbc102` 的发布结果文档；未修改旧 develop 和其他发布 worktree。生产 Java 25、IU 263.7031.23、Gradle 9.5.0、IPGP 2.17.0 及依赖保持不变。

- ZIP `Doc View-1.6.1.zip` SHA-256：`dfef1c63f72a4049d6f108ebdb23697a158e592a100c4b9ecf4168bfa433ddb3`。
- JAR `doc-view-1.6.1.jar` SHA-256：`db8e09f5b2bd59af6354267549d8ca2c6010690edc61583a45f11233a03bc24d`。
- ZIP 仅插件目录/lib/单个 JAR；JAR 413 项、348 个 class 全为 major 69。无测试/fixture/PoC 类路径。
- descriptor 1.6.1、sinceBuild 263.7031.23、无 untilBuild、require-restart=true；依赖 platform/java/xml/markdown/jcef。description 与当前源文件逐字回读一致，change-notes 为 1.6.1。

## Contract 与 TDD

| Contract | RED 与最终结果 |
| --- | --- |
| 类/方法普通 Javadoc 首行、正文、Unicode 上限 | 新 PSI fixture 初次 6 项失败；新增设置测试后 7 项失败；实现后转绿。保留英文空格、空白段落、inline tags，排除 block tags；注解/显式 tag 不受上限影响。 |
| 设置与文档库 | 持久化、Apply/Reset/Cancel、非法输入原子性、三设置 fingerprint 回归通过。类正文独立 classDesc，默认模板及 Markdown 保存；改类正文会改变 source hash，不混入方法 desc；JSON v1 schema 不扩展。 |
| 同面板预览 | 初次新交互测试失败；实现后通过。追加 Preview→Insert/Restore/Reset 测试，先实际失败于“插入后仍显示预览”，修复后定向 5 项通过。 |
| JSON 高亮 | 平台默认 JSON/json fence 未着色的回归实际失败；共享渲染器修复后通过。原始 Markdown 未混入 HTML。 |
| PSI UI 读访问 | 1.6.0 的列表 qualifiedName 快照已存在，本次不虚称重现旧栈。新增全部导出用例实际触发 PSI 读访问 guard，改为后台 simpleName 快照后通过；选择回归继续通过。 |

第一次完整测试的旧预期失败只调整了本 contract 改变的 15 字符截断、全段描述、空格与 tab 数；新增 classDesc fixture 的无效 PSI 问题通过重取修改后的 class 修正，没有跳过测试。Verifier 曾发现新增 deprecated ReadAction.compute 调用，已改 computeBlocking 后重新跑最终完整门禁。

## 最终自动门禁

使用官方 IDEA JBR，实际运行（无旧结果缓存）：

```bash
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' ./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --offline --rerun-tasks --no-build-cache --no-parallel --console=plain
```

结果：**84 suites / 447 tests，0 failures / errors / skipped**；18 个任务全部执行，BUILD SUCCESSFUL，2m1s。

另以 Gradle 官方 `pluginVerification.ides.local(file('/Applications/IntelliJ IDEA.app'))` 指定已安装正式版，执行 `verifyPlugin -PpluginVerifierIdeVersions= -I <local-init.gradle> --offline --no-configuration-cache --rerun-tasks --no-build-cache --no-parallel --console=plain`，12 个任务执行，BUILD SUCCESSFUL，1m20s。该构建产物与上面 ZIP/JAR 哈希相同。

| Verifier 1.411 目标 | 结果 |
| --- | --- |
| IU 263.7031.23（最低 build） | Compatible；16 usages of experimental API；无 blocking/internal/new deprecated finding |
| IU 263.7307.200（官方已安装正式版） | Compatible；16 usages of experimental API；无 blocking/internal/new deprecated finding |

正式版远程 SDK archive 未缓存的下载尝试被取消，随后使用官方 local IDE 输入完成验证；不将取消的下载当成通过。最初 SSH fetch 网络拒绝已经正式 require_escalated 审批后解除并成功执行；不是待解决阻塞，也未改协议/代理绕过。

## 原生验收与文案修订补验

官方 IU 263.7307.200 的独立应用副本及 config/system/plugins/log。下表完整原生矩阵实际安装的是文案修订前 ZIP `086b14b853da7bd3643ce4a5b80d3a43fe8eed75711b8a4872ea5bf29cf32f9c`，不冒充当前最终哈希。使用虚构 sample Java/Spring stubs；未使用真实业务源码、账号或 endpoint。通过 CUA 实际观察窗口与截图，非仅依赖加载日志。

| 场景 | 实际结果 |
| --- | --- |
| Islands Dark / Light | 两种主题均实际切换；源码 Markdown、Velocity `${DocView.name}` 与 #if/#end 有可区分颜色。 |
| JSON 显示 | 深色 json、浅色 JSON 模板预览均看到 key/string/number/boolean 颜色；真实接口 popup 的 JSON 返回块也可见颜色。 |
| 同面板预览 | 编辑/渲染多次切换保持当前区域与草稿，Spring/Dubbo 切换输出对应固定示例。 |
| 预览中的编辑动作 | Insert 返回源码并在保留的光标位置插入变量；恢复默认与 Revert changes/Reset 返回源码且内容正确。变量目录表达式强调、说明灰色。 |
| 设置持久化 | 80→24、关闭两选项 Apply；改 5 与开选项后 Cancel，重开仍为 24/关闭。输入 0 的 OK 被明确拒绝且未部分保存；Reset 回到 24/关闭；最终恢复 80/开启。 |
| 目录与描述 | 类/方法目录只显示首行；popup 的类说明与接口描述分别显示后续英文及中文段落，无 block tags；轻量分隔未形成重复粗线。 |
| 列表选择与全部导出 | GET→POST→GET 选择正确；Export All 出现 Markdown exported successfully，生成 UsersController.md 包含两方法和独立类说明，未混入 HTML。 |
| General 与图标 | 灰色小字帮助、GitHub 图标+Star 可见；清理动作使用平台 GC 图标，在 Find Action 实际观察并执行，导出文件仍在。 |
| 生命周期与日志 | 设置多次关闭/重开；测试实例正常 Exit。日志 0 Read-access assertion、0 ERROR 行。 |

截图保存在本会话 CUA 工具结果中。机器可读证据及测试 XML、Verifier 报告、包审计、原生 summary 位于本机 `doc-view-1.6.1-evidence/`（工作任务目录），不将完整 IDE 日志或 profile 打入仓库/公开 artifact。

macOS accessibility 在读取隐藏 JTree 时曾有 IllegalComponentStateException，栈为 JTree/CAccessibility，未包含 Doc View；独立窗口可继续操作，最终上述场景已完成。官方应用同 bundle 的窗口定位先前产生干扰，改用官方副本独立路径后完成；旧 launcher 关闭日志不代表最终包验收失败。

## 最终文案修订与等价范围

最后将 changelog 的“平台扫帚图标”校准为“平台清理图标”，对应当前主题实际显示的 GC 清理桶图标。重新执行全部自动门禁：84 suites / 447 tests 全绿，两个 Verifier 均 Compatible；最终哈希见本页首节。对前后 JAR 逐条比较：只有 META-INF/plugin.xml 不同，且 XML 精确差异仅该 change-notes 字词；348 个 class 与其他全部资源逐字节相同。

最终 ZIP 又安装到相同隔离官方 IDEA，实际观察启动后的可见窗口、首行目录、分离的类/方法描述、GET→POST 列表选择及浅色 JSON key/string/number/boolean 高亮，并正常退出。最终实例日志无 Read-access assertion/ERROR。没有重跑整套深浅主题/保存取消/全部导出矩阵；这些场景沿用上述前一 SHA 的实测，并以字节等价范围连接，不能表述为新 SHA 全部重新人工执行。证据：wording-equivalence.json、native-followup.json、test-summary-final.json、最终两份门禁日志。

## 边界与回滚

本轮新增/修改 UI 场景已完成，不沿用 1.6.0 原生例外冒充本轮通过。虚构项目未配置 JDK，原生检查覆盖 PSI/预览/UI，不宣称编译该工程。完整继承的凭据拒绝/写入失败、真实平台上传、HTTP Send、Schema 写回、性能取消、全语言/窗口/生命周期组合未重新做原生矩阵；现有相关自动回归在 447 项中执行。未来 IDE build 未验证；仍有既有 16 项 experimental API。类正文只增加 DTO/模板与 Markdown 路径，JSON v1 不新增独立字段。字段/参数普通注释的旧空格处理不在本轮改动范围。

正式发布状态及链接见 [发布记录](../../../../docs/release-1.6.1.md)。如发现阻断问题，保留日志/包，停止追加发布，回退选择 1.6.0 或另发 patch；不覆写已有 tag/ZIP。
