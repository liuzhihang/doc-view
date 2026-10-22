# Tasks

## 1. HTTP 与查询失败复现

- [x] 1.1 新增只绑定127.0.0.1随机端口的 `PlatformUploadTransportTest`，使用虚构凭据/正文复现404文本匹配、401/500吞错、错误正文泄露、重定向及YApi特殊字符编码；运行 `./gradlew test --tests '*PlatformUploadTransportTest' --rerun-tasks`，保存修复前真实失败。
- [x] 1.2 以closeable客户端/响应、10秒连接及30秒读取超时、无自动重试/重定向和安全状态异常修复HttpUtils，语雀仅404返回null；同一targeted测试通过，检查请求次数与本机端口范围。

## 2. 响应契约与取消

- [x] 2.1 固化三平台成功payload/路由及成功代码、必需data响应；新增null/空/畸形/业务错误用例并修复facade，运行 `PlatformUploadTransportTest` 检查成功结果和安全失败，更新 `docs/user-guide/integrations.md` 的最终地址与超时说明。
- [x] 2.2 上传service透传ProcessCanceledException，HTTP前后检查取消；扩展 `DocViewUploadServiceTest` 和传输测试，验证预取消不请求、批量后续停止、无成功/普通错误通知；运行两个targeted测试并更新排错指南的远程结果未知边界。

## 3. 集成门禁

- [x] 3.1 更新CHANGELOG和本change verification，记录RED/GREEN、mock与真实平台区分；运行 `openspec validate platform-upload-stability --strict`、`git diff --check` 和全套 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`。

## 本轮排除的验证（非阻断）

原任务3.2：三平台真实上传、返回链接、真实服务错误与取消验收。2026-10-22 维护者明确要求“真实上传忽略”，故从本轮必需任务中移出；未执行，不标记为通过，也不再作为本轮归档/发布的阻断项。保留本机虚构HTTP服务、响应契约、取消和错误脱敏回归；系统凭据存储等独立验收不受本次排除影响。真实平台部署/版本差异仍未验证。
