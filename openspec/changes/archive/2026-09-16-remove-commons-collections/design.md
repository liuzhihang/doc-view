## Context

DocViewData 的 Markdown 组装及 YApiServiceImpl 的 payload 构造使用 Commons Collections 的 isEmpty/isNotEmpty。插件包不包含该库。用户报告 IDE build 262.8665.337 缺类，但尚无完整堆栈。

## Goals / Non-Goals

目标：消除该依赖，保持 null、空集合、非空集合行为，生成 1.3.14 安装包。
非目标：修改 PSI、DTO 结构、模板变量、UI 错误状态、线程、设置、上传协议或扩大正式平台矩阵。

## Decisions

直接用 null 判断及 List.isEmpty，不引入工具抽象或替代库。isEmpty 对应 null || empty，isNotEmpty 对应 nonnull && !empty。
先增加编译类依赖回归检查，证明旧实现失败；空值测试和既有 golden 证明语义稳定。构建后检查 descriptor 和 ZIP。

## Risks / Trade-offs

- null 判断丢失会导致 NPE：覆盖 null 和空列表。
- 精确用户 IDE 仍需验证：尝试命令行覆盖 Verifier 版本 262.8665.337，失败时记录原因，不夸大支持范围。
- 不调用真实上传平台，不使用用户私有业务样例；网络、无效 PSI 和 UI 降级维持原样。

## Migration Plan

无设置或数据迁移；生成本地补丁包供安装验证。尚未发布；回退补丁时原版本的缺类风险仍存在。
