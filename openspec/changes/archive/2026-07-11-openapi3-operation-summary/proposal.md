## Why

Doc View 已声称支持 OpenAPI 3 `@Operation` 名称来源，但当前实现读取不存在的 `name` 属性，导致标准 `summary` 被忽略并错误回退到 Swagger 2、Javadoc 或方法名。Markdown 契约基线已经建立，现在可以用失败测试修复这个用户可见问题，并证明文档结构未被连带改变。

## What Changes

- 当 Swagger 3 名称来源开启时，使用非空的 `@Operation.summary` 作为接口名称。
- 明确并保持名称优先级：非空 `summary` → `@ApiOperation.value` → `@DocView.Name` → 方法首行注释 → Java 方法名。
- 对未声明、空字符串或仅空白的 `summary` 继续执行既有 fallback，不返回空名称。
- 保持 `@Operation.description`、DTO、Markdown 章节/模板变量、设置字段、上传 payload shape 和编辑写回不变。
- 新增 OpenAPI 3/Swagger 2 最小注解桩与 RED/GREEN 契约测试，并更新 changelog 与路线图切片说明。
- 本 change 不实现 `@Schema.requiredMode`、参数 `@Parameter`、组合/继承注解或完整 OAS 导入导出；这些行为分别进入后续 contract。

## Capabilities

### New Capabilities

- `openapi3-annotation-compatibility`: 定义 OpenAPI 3 注解映射到现有 `DocView` 模型时的兼容规则；本次只建立 `@Operation.summary` 名称契约。

### Modified Capabilities

无。

## Impact

- 生产改动限定在 `DocViewUtils.getName(PsiMethod)` 的 Swagger 3 分支，继续复用现有 Settings、PSI read action 和 fallback 链。
- 新增基于 IntelliJ Java light fixture 的方法元数据测试，不引入 Swagger/OpenAPI runtime 依赖。
- 对已使用非空 `@Operation.summary` 且启用 Swagger 3 名称来源的项目，接口名称会从错误 fallback 值改为 summary；该值会传播到 Markdown 内容、预览/目录、导出名称和平台标题，但输出结构不变。
- `CHANGELOG.md` 记录用户可见修复；`docs/roadmap.md` 将广泛的 OpenAPI 注解工作拆成可独立验证的后续切片。
