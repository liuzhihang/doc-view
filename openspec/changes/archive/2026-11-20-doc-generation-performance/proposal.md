# Proposal

## Why

路线图进入性能评估阶段，但现有测试只验证 PSI 到 `DocView`/Markdown 的内容、扫描兼容性和 UI 生命周期，没有记录耗时、缓存保留量或取消延迟。当前已有 DTO schema 缓存和预览 Markdown 缓存；在没有可比较基线前继续增加缓存或重构，无法证明用户操作变快，也会增加失效与生命周期复杂度。

## What Changes

- 为固定的 Spring 与 Dubbo Java PSI 工作负载建立分阶段基线，分别测量整类 `DocView` 构建、Markdown 渲染、Tool Window 目录扫描及跨 PSI/settings/template 版本的 schema 缓存保留量；扫描单独报告，并区分 DTO schema 冷/热缓存及普通接口扫描开关。
- 在任何生产优化前保存运行环境、fixture 规模、缓存状态和重复测量分布；只对采样确认的热点做局部优化，不预设固定毫秒目标或声称未经验证的提升。
- 对预览生成与目录扫描期间的取消传播做可重复验证；若耗时路径存在未响应取消的循环，则在方法/字段/模块遍历边界传播取消，并丢弃未完成结果。若跨版本测量确认旧 schema 快照持续保留，则按当前 cache epoch 清理旧项并拒绝迟到的旧版本写入。
- 保持相同 PSI、设置和模板输入下的 `DocView` 与 Markdown 输出不变；不新增泛化缓存、运行时计时日志或基准依赖。

## Capabilities

### New Capabilities

- `generation-performance`: 定义生成性能基线、协作式取消和优化后的输出一致性要求。

### Modified Capabilities

无。

## Impact

- 主要待评估路径：`SpringDocViewServiceImpl` / `DubboDocViewServiceImpl`、`ParamPsiUtils`、`DtoSchemaCacheService`、`DocViewData` 与 `PreviewForm`。
- 现有缓存用于冷/热基线和旧 epoch 保留量测量；是否修改 key、复制策略或结构取决于测量结果，不引入通用 LRU。
- `DocViewWindowPanel` 的目录刷新作为独立索引扫描 workload 测量，不与文档生成耗时混在同一指标中；是否优化同样取决于该路径的实测热点。
- 使用现有 Java-only 和 IntelliJ 测试设施；不引入生产依赖、外部服务、遥测或新的平台 extension。
