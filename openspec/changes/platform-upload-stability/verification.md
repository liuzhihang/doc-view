# 平台上传可靠性验证

## 范围与环境

本轮仅使用 JDK HttpServer 绑定 `127.0.0.1` 随机端口、虚构 Token/正文和测试替身。没有访问真实平台、读取用户账户或修改远程文档。目标 IU 261.22158.277，Java 21 源码目标；测试运行时 JBR 25（Gradle 管理的 IDE Runtime）。

## 修复前证据

2026-10-18 组合 targeted 命令实际执行 31 tests，其中本 change 的 `PlatformUploadTransportTest` 9 tests / 8 failures。失败覆盖语雀吞掉非404查询错误、空/无效data、YApi query截断、错误正文泄漏、自动重定向、缺少取消检查和ShowDoc假成功；原有正常payload用例通过。

报告保存于 `/tmp/doc-view-reliability-20261018/round11-TEST-com.liuzhihang.doc.view.contract.PlatformUploadTransportTest.xml`。

## 中间回归与夹具修正

第一次 HttpUtils/facade 修复后 targeted 执行10 tests / 1 failure；9项通过，包括实际停滞响应的30秒socket读取超时、成功payload和安全失败。唯一失败是取消夹具在 `runProcess` 启动前设置取消状态。已改为后台线程的进度作用域启动后、HTTP入口前取消，验证正在进行。

另一次组合命令在并发编辑导出模块时遇到中间状态的编译错误，未运行测试，不算回归结果。后续将等待共享编译单元稳定后统一重跑。

## 修复后证据

组合 targeted 命令实际执行41项，全部通过（round12）；其中 `PlatformUploadTransportTest` 11项、`DocViewUploadServiceTest` 5项，无失败/错误/跳过。报告为 `/tmp/doc-view-reliability-20261018/round12-TEST-*.xml`。验证了真实30秒socket超时、虚构服务成功payload/认证位置/UTF-8正文、语雀仅HTTP404查无文档、无效响应安全失败、query特殊字符、无重定向、正确作用域内取消不请求，以及三个平台service透传PCE、批量取消不处理下一篇且无错误通知、语雀查询异常不创建或更新。

随后补充连接被服务端直接断开时不得自动重试的用例。round13 组合 targeted 共执行62项，其中导出模块的3个磁盘路径 fixture 失败；本 change 的传输12项、上传service5项全部通过，无错误或跳过。报告保存为 `/tmp/doc-view-reliability-20261018/round13-TEST-*.xml`。新增用例观察到服务端断开后仅收到1次请求。

上面的取消夹具修正不会改变业务契约；不能把原始runProcess启动前取消的失败当作有效的runtime回归依据。

## 全量门禁

round21 执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`，35 个套件、180 项测试，0 failures/errors/skipped；包含传输12项和上传service5项。BUILD SUCCESSFUL（1m3s），实际 `verification-verdict.txt` 为 IU261.22158.277 Compatible。依赖报告仍包含 bundled plugin 的可选/跨平台模块解析提示，不称为零警告。XML、包审计保存于 `/tmp/doc-view-reliability-20261018/round21-fullgate-TEST-*.xml`、`round21-package-audit.json`。

安装包 SHA-256：`01e67f753ce9fa388c0dbb607fe940ddb9a371dc8570aa6347ad27a3c2c42020`。逐一检查 ZIP 内 JAR，无测试夹具、测试框架或 MCP PoC；7个离线文档资源与当前 canonical 文件逐字节一致。连接/连接池等待10秒配置经源码核对；真实停滞测试只证明30秒socket读取超时，不把它扩展为连接超时实测。

## 本轮排除：真实平台上传

此前因缺测试账户及原生环境锁屏，真实平台任务3.2保持未完成。2026-10-22 维护者进一步明确要求“真实上传忽略”，现将真实平台上传、返回链接及真实服务错误/取消验证从本轮必需任务移出，作为非阻断排除项记录，不勾选为验收通过。

本机虚构HTTP服务及上传服务自动回归仍保留；本次排除不覆盖OS凭据存储或其它独立原生场景。没有访问第三方账户、发起真实上传或修改远程文档。真实部署与服务版本差异仍无验收证据，不据mock成功宣称所有平台版本可用。

本次仅维护任务和文档，没有修改运行时代码、重跑Gradle、提交、推送、sync/archive或发布。真实上传不再阻断本轮发布评估，其它未完成项及发布授权要求保持。
