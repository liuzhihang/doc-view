# 设置说明

## 入口与作用范围

打开 **Settings / Preferences → Tools（工具）→ Doc View**。配置按使用目的分类：

- **Doc View**：常规行为，包括代码行标记和普通接口识别。
- **生成规则（Generation Rules）**：文档标题、接口名称、描述、必填注释和字段名称的来源。
- **模板与输出（Template and Output）**：模板编辑、预览结果和输出选项分开显示；变量目录可按需展开。
- **平台集成（Platform Integrations）**：平台入口和说明，下设 YApi、ShowDoc、YuQue。

以上配置都按当前项目保存；切换到另一个项目时，应在那个项目中单独检查。首页的“新版本更新时提醒我”是 IDE 全局偏好。普通设置使用 Apply 或 OK 后生效；Cancel 放弃本次尚未应用的修改。

设置页以原生 `?` 图标提供指南：悬停查看简短说明，再点击气泡里的链接，由默认浏览器打开 GitHub 上对应文章。阅读需要网络，打开指南不修改设置。

## 界面语言

当前开发版本的主要界面支持英语和简体中文，跟随 IDEA 的界面语言；在 IDEA 的 **Language and Region / 语言和区域** 修改语言后按提示重启。插件没有单独的语言开关，未提供翻译的语言回退英语。菜单、设置、通知和欢迎页的产品名、注解名、变量名等标识保持原样。

界面翻译不会翻译源码注释、业务名称、已有默认模板、用户保存的模板或生成的 Markdown/JSON。需要英文文档时，请自行编辑模板与源码说明；当前 GitHub 用户指南仍以中文维护。语言覆盖和实际验收范围见[本轮记录](../validation-2026-10-20.md)。

**Revert changes**（部分页面称 Reset）恢复最近一次 Apply/OK 保存的设置；Cancel 丢弃未应用草稿，不撤销此前成功 Apply 的修改。模板修复的验证见[记录](../../openspec/changes/fix-template-settings-lifecycle/verification.md)；操作步骤见[模板教程](templates.md)，异常处理见[故障排查](troubleshooting.md#模板-reset-后自定义内容异常)。

## 阅读生成规则

生成规则页用浅分隔线和留白区分分组。组标题旁的问号提供来源优先级、固定回退及必填判断细节：来源优先级按编号逐条换行，回退另起一段，必填帮助按字段和直接参数分段。页面内的短灰字只提示操作结果，窗口变窄时自动换行，较短窗口可纵向滚动。各项仍通过 Apply / OK 保存，Revert / Cancel 规则不变。下文保留完整规则，便于查阅。

## 文档标题

按以下顺序尝试，遇到第一个可用值就停止。可取消勾选某个来源；顺序不变，也不会拼接多个来源。前四项默认开启，`DocView` 是固定兜底。

1. **`@DocView.Title`**：类 Javadoc 中的固定标签，优先级最高。
2. **类注释**：读取类 Javadoc 描述。
3. **类名**：读取 Java 简单类名。
4. **全类名**：读取带包名的类名。
5. **`DocView`**：前面没有可用值时显示的固定标题，不提供开关。

例如，类注释中的 `@DocView.Title 账户查询` 会优先作为标题；没有该标签时，才检查类注释和类名。

## 接口名

默认四个可配置来源均启用；关闭某项只会跳过它，不会改变顺序。最后的 Java 方法名是固定兜底。

1. **`@Operation`**：读取 Swagger 3 的 `summary`；空白值会继续向下查找。
2. **`@ApiOperation`**：读取 Swagger 2 的 `value`；空白、缺省或无法解析时继续查找。
3. **`@DocView.Name`**：读取方法 Javadoc 中的固定标签。
4. **方法注释**：读取方法 Javadoc 第一行；超过 15 个字符会截断。
5. **方法名**：最后兜底，不提供开关。

示例：

```java
package example.docs;

/** 账户接口 */
public interface AccountApi {
    /** 查询账户
     * @DocView.Name 查询账户详情
     */
    String findAccount(String accountId);
}
```

## 接口描述

来源顺序是 Swagger 3 `@Operation.description`、Swagger 2 `@ApiOperation.notes`、方法 Javadoc。两个 Swagger 来源默认开启并可分别关闭；注释是固定兜底。空白、缺省或无法解析的描述会继续向下查找；没有可用来源时结果为空字符串。

Swagger 名称和描述属性按 Java 字符串实际值读取，支持 IDEA 可求值的字符串常量。有效值中的空格、引号和转义文字会保留，不会把常量名当作正文。依赖或源码尚未解析时，先等待索引就绪，再生成并检查来源开关。

## 必填与字段名称

### 必填

必填分组的说明图标提供 Validation、Lombok、Swagger 的固定识别规则；这些固定规则不提供开关。实体字段和方法参数按不同顺序判断：

- **实体字段**：`@Schema(requiredMode = REQUIRED)` 表示必填，`NOT_REQUIRED` 表示非必填，二者优先级最高。未设置、为 `AUTO` 或无法解析时，依次检查已配置的 Validation/Lombok 注解、Swagger 2 `@ApiModelProperty(required = true)` 和启用的 `@DocView.Required`；没有依据时按非必填处理。
- **实体字段上的 `@DocView.Required`**：该 Javadoc 标签可配置且默认开启。关闭后只跳过此标签，不影响固定的注解判断。
- **直接方法参数与 String Header**：先查已配置的 Validation/Lombok 正向标记，再按 Spring 的 `defaultValue`、`required` 判断；Spring 没有结论时，读取显式的 `@Parameter.required`，最后按非必填处理。方法参数不读取 `@DocView.Required`。完整优先级见下节。

**注意：** `@Schema.required` 已废弃，本开发构建不再读取它；请改用 `requiredMode`。`@Nullable` 和 `@Schema(nullable = true)` 不决定必填标记；Swagger 2 的 `required = false` 也不会覆盖其他正向依据。字段上的 `requiredMode` 适用于请求/响应 DTO 及展开后的 DTO 字段。

```java
import io.swagger.v3.oas.annotations.media.Schema;

class AccountRequest {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "账户标识")
    String accountId;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "可选备注")
    String note;
}
```

在参数编辑器确认对已有 `@Schema` 字段的修改时，插件写入 `requiredMode`、更新描述、移除该字段显式的旧 `required` 成员，并保留其他未编辑成员。只调整必填状态时，原描述中的引号、反斜杠、换行和空白均保留；主动清空描述后，也不会重新填回 Javadoc 内容。

若本次修改中有字段的 Schema 类型没有 `requiredMode`，会提示升级 `swagger-annotations` 到 2.2.5 或更高版本，本次确认不会修改任何字段源码，编辑弹窗保留草稿；不自动迁移其他字段。

字段与直接方法参数的来源不同，调整字段上的 `@DocView.Required` 不会改变直接 `@RequestParam` 的判断。

### OpenAPI 3 方法参数说明

直接生成的参数支持 `@Parameter.name`、`description`、`example`、`required`；String 类型的 `@RequestHeader` 同样支持这些信息。

- **名称**：优先使用 Spring `@RequestParam`、`@PathVariable`、`@RequestHeader` 的非空 `name`/`value`，其次是非空 `@Parameter.name`，最后是 Java 参数名。Spring 两个别名填写冲突值时回退 Java 名，不再使用 OpenAPI 名称。
- **描述和示例**：非空 `@Parameter.description`/`example` 填入文档；空描述保留原有 Javadoc 回退。接受的文字保留首尾空白。示例也用于查询参数示例；它不会改变参数分类。出现非空 `examples`、`content` 或 `array` 复合元数据时，不采用单值 example，也不解析这些复合内容。
- **必填**：配置的正向标记优先。其次，`@RequestParam`/`@RequestHeader` 有有效 `defaultValue` 时为非必填；否则使用 Spring `required`（未写时为 true）。`@PathVariable` 使用其 `required`（未写时为 true）。无 Spring 结论才采用显式 `@Parameter.required`；其注解默认 false 不压过其他规则。

例如 `@RequestParam(name = "q", required = false) @Parameter(name = "ignored", description = "查询词", example = "alice", required = true)` 生成名称 `q`、描述“查询词”、示例 `alice`，仍为非必填；若另有已配置的 `@NotNull` 等正向标记，则必填。

这些规则不扩展为完整 OpenAPI 解析。对象参数展开后的字段、`@RequestBody`、方法级 `@Parameter`/`@Parameters`/`@Operation.parameters`、参数级 `@Schema` 和复合 schema 不由这项功能解析。字段规则仍见上节。

### 字段名称

`@JsonProperty` 默认开启。实体字段上的 Jackson `value` 非空时用作文档字段名，否则使用 Java 字段名。关闭后始终用 Java 名称。此选项只作用于实体字段，不替代 Spring 路径、查询等方法参数名称解析。

## 文档设置与导出

常用文档行为配置如下：

- **编辑器行标记（默认开）**：在 Doc View 根页控制 Java 编辑器 gutter 中的快捷标记，不控制工具窗口侧栏。
- **普通接口识别与扫描（默认关）**：在根页控制普通 Java 接口是否进入模块扫描和目录。它与行标记独立，可关闭图标但开启接口扫描；普通接口使用现有 Dubbo 文档路径，不保证任意接口都能完整解析。
- **独立实体（默认关）**：决定 Markdown 如何展示嵌套请求/响应实体。关闭时放在嵌套字段表格中，开启后拆成后续表格或段落；不会另生成实体文件。
- **合并导出（默认开）**：只影响批量 Markdown 导出。开启时把一个类的接口合并到 `<类名>.md`，关闭时每个接口分别写入 `<接口名>.md`。确认覆盖会替换目标内容，取消则保留原文件。它不影响单篇导出、平台上传或项目文档导出；后者详见[用户指南](index.md#项目内-api-文档与-ai-检索)。

## 前缀符号

默认“前缀符号 1”为空，“前缀符号 2”为 `-->`。它们用于关闭“独立实体”时，在嵌套参数表格中标记子字段层级。前缀 1 位于子字段前；前缀 2 会随嵌套层级重复累加。点击 Apply 时两个输入框都会执行 `trim()`，首尾空白无法保留。开启“独立实体”后，当前分表渲染路径不输出这两个前缀。

参数分表、前缀和合并导出在 **Template and Output → 输出选项** 页签，按行排列；常规行为与生成规则页面不重复显示这些字段。

## 固定行为与移除的无效选项

“隐藏左侧”没有实际作用，已从页面和当前设置状态移除。类标题的 `DocView`、接口名的“方法名”、描述的“方法注释”，以及 Validation/Lombok/Swagger 注解项改为说明文本。页面也没有可编辑的标签名、注解列表或参数类型排除列表。既有参数类型过滤仍生效；不需要直接改 IDE 配置 XML。

需要确认某个设置为何没有体现在文档中时，按[故障排查](troubleshooting.md#设置已修改但文档没变化)检查应用状态和重新生成步骤；三种平台的连接字段见[平台集成](integrations.md)。
