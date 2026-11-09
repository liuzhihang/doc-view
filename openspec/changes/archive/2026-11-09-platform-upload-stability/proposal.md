# Proposal

## Why

本地源码确认语雀 getDoc 依赖错误正文中的 `404`/`Not Found` 判断不存在，并吞掉其他 RuntimeException，可能把认证/服务错误转成创建请求。现有 HTTP helper 没有明确超时、未关闭每次创建的客户端，错误异常还包含原始响应正文；仅入口通用通知不足以证明请求及结果可靠。

## What Changes

- 以真实 HTTP status 区分语雀查无文档和请求失败；只有 404 允许进入创建路径。
- 明确连接/读取超时、资源关闭、不自动重试和不跟随重定向；保留既有 API 路径、认证位置及成功请求 payload。
- 严格检查三平台响应中的成功代码和后续必需数据；空、畸形或业务错误不得生成成功通知。
- 异常只携带固定平台/阶段及 status，不包含 token、请求 URL、payload 或响应正文；取消单独透传。
- 增加仅监听 loopback 的虚构服务测试，校验请求、成功/失败分类和次数；不连接真实账户或平台。

## Capabilities

### New Capabilities

- `platform-upload-results`：平台上传 HTTP 和响应判断、取消及敏感错误边界。

### Modified Capabilities

无。现有 payload 字段、Markdown 和凭据存储保持不变。

## Impact

影响 HttpUtils、三平台 facade 的响应处理及上传 service 的取消透传，以及用户集成/排错指南。不新增依赖、生产语言或平台版本。真实平台上传、系统凭据库和原生通知仍需有条件时单独验收。
