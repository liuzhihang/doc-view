# Tasks

> 2026-10-19 Round 58：用户已授权继续实现。以下任务已针对新的 HTML 欢迎/更新页实现并验证，证据见 [verification](verification.md) 的 Round 58；旧七章手册记录仅作历史。完整键盘专项按用户答复暂时跳过，GitHub 新指南公开正文仍待推送后复核。

## 1. 内容与资源

- [x] 1.1 确定欢迎页与当前版本更新亮点文案、GitHub 文档和历史链接；将未发布构建标记纳入 contract 测试并取得 RED。
- [x] 1.2 制作低密度本地 HTML/CSS 欢迎和更新页面；无远端资源或 JS 业务脚本，宽窄布局与深浅主题均可预览。
- [x] 1.3 调整 processResources：打包首页/更新所需静态资源，停止打包完整五篇指南及旧章节资源；保留 GitHub 指南源文件，取得资源测试 GREEN。

## 2. IDEA 集成

- [x] 2.1 核对 261/262 公开 JCEF API，显式声明 bundled JCEF 依赖，保留 Java-only 与当前 IDE 基线。
- [x] 2.2 为动作白名单、GitHub 链接、项目绑定、页面复用、关闭与迟到回调新增 RED；替换旧 Swing 手册为 JCEF 页面，不保留 fallback renderer，取得 GREEN。
- [x] 2.3 将设置帮助改为对应 GitHub 链接，移除旧手册章节导航；验证帮助操作不会 Apply/Reset 或丢失草稿。
- [x] 2.4 接入 IDE 主题参数、原生设置/目录动作和更新提醒；复验首次、升级、同版、多项目及关闭提醒，更新页准确匹配安装版本。

## 3. 验证与文档

- [x] 3.1 执行 test、verifyPluginProjectConfiguration、buildPlugin、verifyPlugin；核对实际测试数量、JCEF 依赖和包内资源，记录产物 SHA。
- [x] 3.2 在 doc-demo 新包核对欢迎/更新页全部动作与 GitHub 路由、无自动外网加载；不以本地路径有效代替远端正文可用。
- [x] 3.3 在原生 IDEA 验证深浅 × Compact 四组、窄宽窗口、主题切换和基本焦点可见性；HTML 浏览器预览不能代替原生验收。
- [x] 3.4 在原生 IDEA 复验多项目、关闭重开、加载中关闭与释放；验收后恢复测试设置与普通 IDEA。
- [x] 3.5 同步 README、CHANGELOG、指南及验证记录，运行 OpenSpec 严格校验和 diff 检查；不得将旧手册通过项作为新页面证据。

## 4. 宽屏布局反馈

- [x] 4.1 记录 2560px 原宽度复现，放宽容器并保留窄窗、字号和段落间距。
- [x] 4.2 核对两页宽窄浏览器布局、buildPlugin 与新包原生 IDEA 宽窄烟测；更新证据并恢复普通 IDEA。

## 暂缓与发布前条件

- 完整纯键盘遍历/激活专项：用户明确暂时跳过，未验通过；旧手册中 Tab 跳过按钮的观察保留在 verification。
- GitHub 远端正文当前未发布，已知路径返回 404；获得提交/推送授权后再验证公开正文。本轮不得为此推送或发布。
- 不提交、不发布、不自动 sync/archive；不使用 Credits。
