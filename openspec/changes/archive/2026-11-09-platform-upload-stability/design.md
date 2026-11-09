# Design

## Context

见 proposal。HttpUtils 是三平台唯一 HTTP helper，当前 GET 的 HTTP 错误抛出带正文的 RuntimeException，POST/PUT 抛出带正文的 Exception；YuQue facade 对 RuntimeException 做文本匹配并吞掉其他错误。YApi/ShowDoc 依赖成功代码但未完整校验 data；三上传 service 的 `catch(Exception)` 吞掉 ProcessCanceledException。

## Goals / Non-Goals

目标是可靠地区分“已有文档、不存在、上传成功、请求/业务失败、用户取消”，保持成功 payload 与端点，不改变凭据存储。只通过本机虚构服务验证。非目标：完整平台协议升级、自动重试、队列、断点续传、批量事务、模板/PSI 重构或真实账户测试。

## Decisions

1. 沿用已有 Apache HTTP API，单次请求使用 try-with-resources 关闭 CloseableHttpClient 和 CloseableHttpResponse。设置 connect/connection-request 10秒、socket read 30秒；关闭自动重试和重定向。只接受 2xx，状态异常仅保留数值，不保留 URL、请求/响应实体或带私有内容的 cause；IO/URI/协议错误固定提示。没有新增设置项。
2. 新增窄的 HttpStatusException（status code），YuQue getDoc 只对 status=404 返回null，其他错误原样抛出；200空正文、null/畸形JSON、缺少有效 data.id 都是失败。create/update 还需非空 data.slug，才能构造成功链接。404正文可以是任何内容，不能靠英文词判断。重定向不携带凭据重发。
3. YApi要求显式 errcode=0；getCatMenu的data为列表（可以空），每个目录有有效id；addCat需有效data.id；save无需为未使用字段虚构限制。ShowDoc要求error_code=0和有效data.item_id/page_id。JSON异常统一转换为固定平台响应错误，不把原文或Gson异常堆栈作为cause传播。依旧接受平台原来使用的数字或数字字符串成功code，不增加旧版协议分支。
4. YApi getCatMenu使用已有URIBuilder参数编码保留token完整值，认证位置仍为query。正常POST/PUT的JSON字段和YuQue X-Auth-Token位置不变；虚构值验证序列化与路由。
5. helper在请求开始和读取完成后checkCanceled；三上传service先catch ProcessCanceledException再catch Exception，让用户取消沿既有后台任务静默结束，不把取消显示为上传错误。同步网络进行中不承诺立即abort，最长等待受上述超时约束。project关闭和批量间检查沿现有任务边界，未知远程结果不自动重试。
6. 通过JDK自带HttpServer仅绑定127.0.0.1随机端口，用队列指定假响应并记录本次请求。测试不能读取项目账户/PasswordSafe或向外部域名发送请求。成功、401/403/404/429/500、HTTP302、业务错误、空/畸形JSON分别检查；错误中放置虚构敏感sentinel并断言异常不包含它。以调用次数证明无自动重试/重定向，不用真实服务访问代替mock证据。

## Risks / Trade-offs

- [30秒读取超时可能中断慢服务] → 返回固定失败，用户自行核对远程结果后再触发上传；不无限等待、不自动重试。
- [使用301/302跳转的地址现在失败] → 文档要求填最终API地址；避免带认证信息自动跳到另一主机，不增加兼容开关。
- [某部署返回缺少ID的“成功”] → 本插件无法构造可验证链接，明确失败；真实平台差异留实际账户验收，不猜成功。
- [同步HTTP尚在等待时用户取消] → 下个检查边界不再发布成功或后续请求，当前socket等待有上限；不宣称立即终止远程操作。

## Migration Plan

不迁移设置、token或旧文档，普通插件升级应用新错误语义。需要重定向的用户改填最终地址；旧行为由用户选择旧版插件。不提交、发布或改动账户。
