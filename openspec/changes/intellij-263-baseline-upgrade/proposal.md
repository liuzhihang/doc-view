# Proposal

## Why

生产基线仍声明支持 IU 2026.1（build 261），而 IU `263.7031.23` 尚未完成生产包自动验证与原生回归。实际 Codex 客户端的取消互操作候选门槛已通过；用户于 2026-10-24 明确批准同步把生产 Java 源码、API 与字节码基线升级到 Java 25，以满足 IU 263 的平台要求。仍需用生产代码、精确 build 和最终插件包完成其余验证，不能把隔离 PoC 或客户端互操作结果当成生产支持范围。

## What Changes

- 修复参数表静态 renderer 保留最后渲染树及已关闭表单的问题，改为表格实例所有，不改变字段解析或编辑规则；独立复验参数功能与 GC，保留完整资源矩阵的未验边界。
- 2026-11-06 修复原生补验发现的弹窗内部点击误取消：公开 Swing 组件归属判断优先于 mouse-out 的错误屏幕坐标分类，预览和参数弹窗共用同一策略，保持外部点击及 Pin 行为。重建后用无诊断程序的最终包复验，不将输入工具的坐标异常推广为所有物理鼠标问题。
- 2026-11-05 补验正式版 `IU-263.7307.200`：该 build 作为额外精确验证目标，不用它替换旧 EAP 的证据或提前改变最低 build。修复现有 Spring XML/Dubbo DOM 功能缺少显式 bundled XML 依赖的兼容性问题；构建与 descriptor 同时声明 `com.intellij.xml`，不打包平台 XML JAR、不增加第三方库。

- 只有在实际 Codex 客户端主动 interrupt、IDEA 收到与协商协议匹配的取消信号、服务端任务完成取消与 cleanup、且同一 session 后续调用成功这条链全部有证据后，才开始运行时升级；官方 TypeScript SDK 可补充验证，但不能替代该门槛。
- 将目标限定为 IntelliJ IDEA Ultimate `IU-263.7031.23`，用精确 build 更新 Gradle 平台目标、Plugin Verifier 目标矩阵和生成的 `since-build`。
- 按用户明确批准，将生产 Java 源码、Java API 与 classfile 升级为 Java 25 / major 69，并使用 JDK 25 toolchain；不保留 Java 21 产物或旧 IDEA 兼容分支。
- 将构建期 IntelliJ Platform Gradle Plugin 升级到正式发布版 `2.17.0`。其官方 release notes 更新了 build 262 起的 Java 版本映射，源码将 build 262+ 映射至 Java 25；不修改或绕过平台检查逻辑。
- 用完整自动检查和目标 build 的原生功能回归验证新基线，并更新兼容性说明及变更记录。
- 明确不再支持低于 `263.7031.23` 的 IDEA build，不增加旧版反射分支、备用 API 或双轨兼容。
- 保留当前 HTTP/UI 尚未验收项为未完成；本 change 覆盖平台基线及验收中已定位的兼容性、输入与生命周期修复，不交付正式 MCP 查询工具，也不发布或推送。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `intellij-api-compatibility`：定义生产最低 IDEA build、精确验证目标及旧 build 的不支持边界。

## Impact

实现会影响 `gradle.properties` 中的平台、Verifier、Java 版本和最低 build，及 `build.gradle` 中的构建期 Gradle 插件版本，进而影响生成的 `plugin.xml`。IPGP 2.17.0 是构建工具升级，不新增运行时/生产依赖；还需要运行完整 tests、构建、项目配置检查、Plugin Verifier 和 IU 原生回归，并更新 IntelliJ 兼容性文档、README/插件介绍/用户指南/AGENTS 中的最低版本及 changelog。客户端取消准入已通过；Java 25 基线变更已获用户批准，但生产实现与其余验证仍未完成。

## 当前准入结果

截至 2026-10-24，官方 Codex `0.165.0-alpha.1` Apple Silicon app-server 候选已通过三轮真实模型工具调用和 `turn/interrupt` 验证：协商版本为 `2025-06-18`，三次均发送匹配原 request ID 的 `notifications/cancelled`，IDEA 探针对应任务各取消并 cleanup 一次，没有迟到的完成结果，同一 session 后续 echo 均成功。客户端取消准入 **3/3 passed**。用户同日明确批准 Java 25 源码/API/classfile 基线，允许生产基线升级继续；该变更尚未通过构建和兼容性验证。

常用桌面 Codex `0.163.0-alpha.11.2` 在 2026-10-23 的失败记录仍保留；`0.165.0-alpha.1` 是隔离 prerelease 候选，不代表已更新日常客户端。生产配置、Java 25 编译、自动测试、Plugin Verifier、最终包读回和 IU 263 原生回归仍未全部验收。详细客户端身份、摘要和协议证据见[验证记录](verification.md)与[取消调查](../../../docs/mcp-cancellation.md)。

## 2026-11-08：卸载图标缓存修复范围

维护者要求继续验证缓存清理调用时机。允许增加一个应用级公开 `DynamicPluginListener`，仅在 Doc View 自身卸载前同步调用公开 `IconLoader.clearCache()`，清理平台禁用图标缓存对插件 ClassLoader 的引用。不增加依赖、不更换图标、不延迟回调、不触碰 JNI 或 Internal API。此修复不等于解决 macOS Accessibility 的独立引用链；后者和完整原生矩阵仍须分别验收。
