## Why

用户报告生成文档时提示 org/apache/commons/collections/CollectionUtils。当前实现依赖 IDE 提供该类，插件未自行打包，存在运行时缺类风险。

## What Changes

- 移除 DocViewData 和 YApiServiceImpl 的 CollectionUtils 引用，保留 null/空集合语义。
- 补充依赖回归检查及空值测试，沿用 Markdown golden 验证输出。
- 插件补丁版本由 1.3.13 升至 1.3.14，更新 changelog 和版本摘要。
- 不改变 PSI、模板、UI、网络调用、平台基线，不发布 Marketplace。

## Capabilities

### New Capabilities
- `collection-independent-rendering`: 文档和 YApi 集合处理不依赖 Commons Collections。

### Modified Capabilities
无。

## Impact

仅两个生产 Java 类、测试、版本及变更文档。无新增依赖，验证 test/buildPlugin/verifyPlugin 及产物内容。
