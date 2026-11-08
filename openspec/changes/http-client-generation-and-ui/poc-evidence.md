# HTTP Client 原生桥接 PoC 证据

## 结论

不采用 IDEA 原生请求生成路线，选用单一 Java 自生成实现。硬拒绝来自生命周期 gate：目标版原生 `generateRequestsInHttpEditor(List)` 返回 `void`，内部将 `launch(...)` 返回的 `Job` 在字节码偏移 67 丢弃；Doc View 无法等待完成、传递取消或在写入前检查源码/项目是否仍有效。A01–A12 和 L02–L06 的原生生成场景因此没有执行，不能解读为通过或语义失败。

另有两项边界需要保留：Endpoints Action 需要 Endpoints DataContext，不是可直接从任意 Doc View 方法调用的入口；Plugin Verifier 报告生成管理器的类、构造器和候选发布方法共 3 个 Experimental API 用法。Experimental 作为兼容性风险记录，不单独作为本次硬拒绝理由。

## 探针与隔离边界

- 新增 Java 探针仅位于 `tools/http-client-poc/`，不在生产 source set。`python3 tools/http-client-poc/run.py --help` 已实际执行并返回 0；README 分开说明构建、安装、启动和 UI 验收。
- 目标为 Gradle 缓存中的 IU 261.22158.277：`/Users/liuzhihang/.gradle/caches/9.5.0/transforms/232d456199c62cab0c9583ddb770291c/transformed/idea-2026.1-aarch64`。使用 Java 21.0.7 编译，classpath 含目标 SDK 与依赖插件 493 个 jar。
- 最终构建命令：

  ```sh
  python3 tools/http-client-poc/run.py \
    --ide-home /Users/liuzhihang/.gradle/caches/9.5.0/transforms/232d456199c62cab0c9583ddb770291c/transformed/idea-2026.1-aarch64 \
    --java-home /opt/homebrew/Cellar/openjdk@21/21.0.7/libexec/openjdk.jdk/Contents/Home \
    --output-dir /tmp/doc-view-http-client-poc-20261023-r3 \
    --verify
  ```

- Java 编译 exit 0；独立探针 ZIP 为 `/tmp/doc-view-http-client-poc-20261023-r3/doc-view-http-client-poc.zip`，大小 4,305 bytes，SHA-256 `1964b24ee5934be96c1a7061f4f9399d48007889f020705d31465a60e4da7ff6`。编译日志、完整命令和产物清单见 `/tmp/doc-view-http-client-poc-20261023-r3/compile.log` 与 `build-report.json`。
- 缓存中的 Plugin Verifier 1.410 对 IU 261.22158.277 实际排定并完成 1/1 检查，结果 Compatible；脚本还检查有效描述符、非零且全部完成的排定数，以及 Deprecated/Internal/override-only API 结果。此包为**探针包**，不是主工程包或最终发布包。报告见 `/tmp/doc-view-http-client-poc-20261023-r3/verify.log` 和 `/tmp/doc-view-http-client-poc-20261023-r3/verifier-reports/IU-261.22158.277/report.html`。
- Verifier 报告 3 个 Experimental API 用法，均来自 `HttpRequestGenerationManager` 的类、构造器与 `generateRequestsInHttpEditor(List)`。探针另一个 `Application.runReadAction(Computable)` 用法未触发 Deprecated API 报告；无 Internal、Deprecated 或 override-only 违规。
- 首次启动尝试使用 Gradle transformed SDK 中的 `MacOS/idea`，进程以退出码 137 结束且未创建 `idea.log`；这只说明该缓存 SDK 的直接启动受阻。
- 随后从 JetBrains 官方下载 `https://download.jetbrains.com/idea/idea-2026.1-aarch64.dmg`，用其 `.sha256` 官方校验值核对完成：文件大小 1,483,012,780 bytes，SHA-256 `90f2aeca3fd0173102b3d2734ad863602bb17146c317fe2d88b0618fd47ea600`。DMG 只读挂载在 `/tmp/doc-view-http-client-poc-20261023-r3/official-mount`；包内 `/tmp/doc-view-http-client-poc-20261023-r3/official-mount/IntelliJ IDEA.app/Contents` 的 `product-info.json` 为 IU 2026.1 build `261.22158.277`，`codesign --verify --deep --strict` 通过。
- 以这个完整官方 app 重建 probe，Java 编译 exit 0，Plugin Verifier 1.410 实际排定并完成 1/1 检查，Compatible 且无 Deprecated/Internal/override-only 报告；3 个 Experimental 用法仍来自生成管理器。包位于 `/tmp/doc-view-http-client-poc-20261023-official/doc-view-http-client-poc.zip`，大小 4,305 bytes，SHA-256 `1964b24ee5934be96c1a7061f4f9399d48007889f020705d31465a60e4da7ff6`。命令、编译日志、报告和 verifier 输出见该目录的 `build-report.json`、`compile.log`、`verify.log` 与 `verifier-reports/IU-261.22158.277/report.html`。
- ZIP 已手动安装到 `/tmp/doc-view-http-client-poc-20261023-official/profile/plugins/doc-view-http-client-poc`，并仅用该 profile 启动官方 app。IDE 进程创建了 `/tmp/doc-view-http-client-poc-20261023-official/profile/log/idea.log`，但启动日志记载 `Disabled plugins: JetBrains Ultimate (261.22158.277)`；自动生成的 `profile/config/disabled_plugins.txt` 仅含 `com.intellij.modules.ultimate`。日志因此报告 Spring Web 未加载，探针因依赖 `com.intellij.spring.mvc` 也未加载。root 对隔离 261 UI 的只读检查也看到 PoC 与 Spring Web 为 Disabled、Ultimate 已禁用，而 HTTP Client、Endpoints、Java、Markdown 为 Enabled；没有启用 Ultimate 试用/许可或修改禁用配置。故官方 app 启动有日志，但探针**加载仍未验证**；这是插件启用条件未满足，不是原生 API gate 的拒绝依据。没有自动接受许可/安全条款，也没有操作普通 IDEA 262 或旧隔离 profile。裸 launcher 还输出 `sandbox_extension_issue_file_to_process ... Operation not permitted`，精简结果见 `/tmp/doc-view-http-client-poc-20261023-official/native-startup-summary.txt`，启动日志与 profile 日志见 `launch.stdout.log` 和 `profile/log/idea.log`。
- 探针不调用生成 API，不发送网络请求。由于后续候选评估在生命周期 gate 硬拒绝后停止，没有打开 fixture 执行原生生成，也没有保存原生生成文件。

## 目标 SDK 调用链证据

SDK 插件描述符确认：Spring Web 为 `com.intellij.spring.mvc`（`spring-mvc-impl/lib/spring-mvc-impl.jar`），HTTP Client 为 `com.jetbrains.restClient`（`restClient/lib/restClient.jar`），Java 为 `com.intellij.java`，Microservices JVM 为 `com.intellij.microservices.jvm`。探针只声明依赖 platform、Java module、Spring MVC、Microservices JVM 和 HTTP Client；Verifier 对该 descriptor 检查通过。

Spring gutter 的调用路径：

1. `SpringWebOpenInHttpClientLineMarkerProvider` 对 `PsiMethod` 调用 `UrlPathContextUtilsKt.findUrlPathContext(Project, PsiMethod)`。
2. 对得到的 `UrlPathContext` 调用 `OpenInHttpClientLineMarkerBuilder.Companion.fromUrlPathContext(...)`，再由 builder 提供 `navigationHandler()`。
3. builder 的导航处理最终建立 `HttpRequestGenerationManager(Project)` 并调用 `generateRequestsInHttpEditor(List)`。

前两项在目标 class 文件中为 public JVM 方法，所查方法没有 Internal 标记；第三项虽为 public，但 Plugin Verifier 将 manager 类、构造器和方法报告为 Experimental。完整 bytecode 文件见 `/tmp/doc-view-http-client-poc-20261023-r3/evidence/spring-url-context.javap.txt`、`spring-gutter-provider.javap.txt`、`native-builder-companion.javap.txt`、`native-builder.javap.txt`、`spring-navigation-closure-1.javap.txt`、`spring-navigation-closure-requests.javap.txt` 和 `generation-manager.javap.txt`。

Endpoints 路径由 `com.jetbrains.restClient` 的 plugin descriptor 将 `HttpGenerateRequestFromEndpointsAction` 注册到 `EndpointsActions.ContextMenu`。Action 读取 `EndpointsProvider.URL_TARGET_INFO`，组装请求后也调用同一个 `HttpRequestGenerationManager.generateRequestsInHttpEditor(List)`。这说明 Spring gutter 与 Endpoints Action 共用有问题的 fire-and-forget 发布口；Endpoints Action 的输入上下文来自 Endpoints 面板/Action DataContext，不能直接等同于 Doc View 当前选择的 `PsiMethod`。原始 descriptor 与反编译输出在 `/tmp/doc-view-http-client-poc-20261023-r3/evidence/restClient-plugin.xml`、`endpoints-action.javap.txt`。

`generateRequestsInGivenFile(...)` 单独带有 `org.jetbrains.annotations.ApiStatus.Internal` 标记（`generation-manager.javap.txt` 对应注解区），不作为桥接方案或替代入口。

### 生命周期硬拒绝的精确证据

`generation-manager.javap.txt` 中 `generateRequestsInHttpEditor(List)` 声明为 `public final void`。该方法在偏移 11–44 获取 HTTP Client coroutine service 的 EDT scope，偏移 64 执行 `kotlinx.coroutines.BuildersKt.launch$default(...)`，其返回值类型为 `kotlinx.coroutines.Job`；紧接偏移 67 执行 `pop`，偏移 68 返回。公开调用方无法取得 Job，也没有 completion/failure/cancel handle。Spring builder 的导航闭包与 Endpoints Action 都调用此方法。因此不能满足设计 gate 4 的受控完成、取消、源码变化及项目关闭保护。

## 五项准入 gate

| Gate | 结果 | 证据与边界 |
| --- | --- | --- |
| 1. 第三方 API、兼容性和依赖 | 条件满足；Experimental 风险保留 | helper/builder 方法为 public 且无 Internal/override-only/Deprecated 报告；目标 Plugin Verifier 结果 Compatible。manager 类、构造器和发布方法出现 3 条 Experimental 报告，记录为未来兼容性风险。带 Internal 的 `generateRequestsInGivenFile` 已排除。 |
| 2. Doc View 方法入口、Spring 与 Feign 可达性 | 部分证实，未准入 | Spring helper 的输入是 `Project,PsiMethod`；Endpoints Action 依赖 `URL_TARGET_INFO` DataContext。原生 Feign 从 Doc View 当前 PSI 选择直达的能力没有验证。因 gate 4 已失败，不继续构造该桥接入口。 |
| 3. A01–A12 请求语义 | 未运行（上游硬拒绝后不适用） | 未执行 Spring/Feign 原生生成、编码、参数位置、required/default、consumes/form/header 或不支持输入用例；没有把打开 HTTP Client 或编译探针算作语义通过。 |
| 4. 后台完成、取消、源码与项目生命周期 | **硬拒绝** | 公开生成 API fire-and-forget 并丢弃 Job；不能由 Doc View 控制完成、取消或发布前校验。字节码偏移见上节及 `generation-manager.javap.txt`。 |
| 5. 重复生成、已保存/未保存内容保护、禁止自动执行 | 未运行（上游硬拒绝后不适用） | 未调用 generator，未修改 HTTP 文件/Scratch/用户草稿，未执行请求；因此保留行为没有 native 运行时证据。 |

## 最终路线与后续未运行项

任一硬 gate 失败即停止该候选；本次 gate 4 已提供硬证据，故唯一推荐/决策为 Java 自生成，不做 native runtime fallback 或双模式。原生 A01–A12、L02–L06 以及 fixture 上的 Spring/Feign 交互生成均不适用且未运行。官方完整 IDEA 261 app 已启动到日志初始化阶段，但目标 Ultimate 模块被自动禁用，探针因此没有加载；未通过更改许可、禁用项或安全设置来绕过此状态。该结果不改变 gate 4 硬拒绝和唯一路线决策。
