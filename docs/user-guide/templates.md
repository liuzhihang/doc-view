# Markdown 模板教程

[← 用户指南首页](index.md)

模板设置完整路径为 **Settings → Tools（工具）→ Doc View → 模板与输出（Template and Output）**；IDE 菜单译名可能不同。本文按“选择模板 → 插入变量 → 预览 → 保存”说明操作，并提供标题和参数表的修改示例。

修改前可先备份自己的 Spring/Dubbo 模板，方便比较或恢复。

## 保存、Reset 与取消

模板配置按最近一次保存状态处理：

- **Apply / OK**：保存当前两份模板和输出选项（独立实体、合并导出、两个前缀）。
- **Reset（IDE 设置界面显示 `Revert changes`）**：恢复最近一次已保存（Apply/OK）的模板和输出选项；它不是“恢复出厂默认”，也会丢弃当前尚未应用的编辑。
- **Cancel**：丢弃尚未应用的编辑。如果先 Apply，再继续编辑后按 Cancel，前一次 Apply 的内容仍保留，只有后续草稿被丢弃。

模板状态修复的原生保存与重启记录见[验证记录](../../openspec/changes/fix-template-settings-lifecycle/verification.md)。模板编辑、变量插入、预览和设置界面的检查记录见[配置验证记录](../../openspec/changes/doc-view-configuration/verification.md)。

## 选择哪份模板

当前代码以 Dubbo 为单独分支：Dubbo Service 使用 Dubbo 模板，其他 Doc View 类型走 Spring 模板；Spring Controller 和 Feign 风格接口因此使用 Spring 模板。进入 Template and Output 设置页后，分别编辑 Spring 和 Dubbo 标签页。生成文档时由插件按接口类型选择，用户不需要在生成时手动切换模板。

## 模板生效范围

基于 `DocViewData.markdownText()` 的预览、复制、导出以及 ShowDoc、YuQue 上传会使用当前接口对应的自定义模板。YApi 上传由 `YApiServiceImpl` 单独组装请求字段和返回结构，描述正文使用 `buildDesc()` 生成的固定 Markdown，不调用 `DocViewData.markdownText()`；改这里的模板不会改变 YApi 上传布局。

## 第一次修改模板

1. **选择模板**：在 **Template and Output → 模板编辑** 选择 Spring 或 Dubbo。默认只显示编辑区，为模板留出空间。
2. **插入变量**：先将光标放在目标位置，再点“显示模板变量”。按英文属性名或当前界面的名称/说明搜索；例如 `requestExample`、中文界面的“请求示例”或英文界面的“Request example”。选中变量后查看完整表达式和用途说明，再点“插入变量”。插入完整的 `${DocView.…}`；若编辑器有选区，所选内容会被替换。完成后可收起变量目录。
3. **预览结果**：点击“预览 Spring”或“预览 Dubbo”。只读编辑区显示 Markdown 源码；它使用插件内置的虚构“查询示例用户”数据，不读取项目源码、不联网，也不保存模板。
4. **核对实际接口**：检查预览和错误提示。点击“模板编辑”返回草稿；继续编辑后旧预览失效，需要重新预览。固定样例通过后，再用项目里的接口生成文档，确认真实输入也符合预期。
5. **保存或撤销**：点击 Apply / OK 保存；Cancel 丢弃未应用内容。需要回到最近保存值时使用 Revert changes。

页头说明旁的 `?` 提供模板指南链接。“恢复默认模板”与变量入口位于“模板编辑”的操作行，会把 **Spring 和 Dubbo 两份草稿** 都替换成随包模板，Apply 前不改变保存值。它与 Revert changes 不同。变量目录展开后可通过分隔条调整编辑器和目录的宽度；预览结果使用独立页签，不与编辑器争用高度。

“独立实体”、前缀符号和“合并导出”位于“输出选项”页签，纵向排列；含义见[设置说明](settings.md#文档设置与导出)。模板预览使用当前已保存的“独立实体”和前缀设置；调整后 Apply，再预览以核对参数表效果。“合并导出”只影响批量导出的文件组织，不改变单接口示例预览。

## 内置模板

下面两段是随插件提供的内置模板，可分别复制到对应标签页。Dubbo 模板不单列 Header 和 Param，而使用 Body 参数变量。

**Spring / Feign**

    **接口名称:**

    ${DocView.name}

    **接口描述:**

    ${DocView.desc}

    **请求路径:**

    ${DocView.path}

    **请求方式:**

    ${DocView.method}

    **请求参数:**

    - Header

    ${DocView.requestHeader}

    - Param

    ${DocView.requestParam}

    - Body

    ${DocView.requestBody}

    **请求示例:**

    ${DocView.requestExample}

    **返回参数:**

    ${DocView.responseParam}

    **返回示例:**

    ${DocView.responseExample}

**Dubbo**

    **接口名称:**

    ${DocView.name}

    **接口描述:**

    ${DocView.desc}

    **请求路径:**

    ${DocView.path}

    **请求方式:**

    ${DocView.method}

    **请求参数:**

    ${DocView.requestBody}

    **请求示例:**

    ${DocView.requestExample}

    **返回参数:**

    ${DocView.responseParam}

    **返回示例:**

    ${DocView.responseExample}

## 十个内置文本变量

模板上下文根对象名是 `DocView`。以下字段均为 `String`：前四个直接复制接口解析结果；后六个由插件预先生成 Markdown 或示例代码。

### 接口元数据

- `${DocView.name}`：接口名称；直接使用解析结果，不保证未提供时为空串。
- `${DocView.desc}`：接口描述；直接使用解析结果，不保证未提供时为空串。
- `${DocView.path}`：接口路径文本；Dubbo 下的含义取决于解析结果，不一定是 HTTP URL。
- `${DocView.method}`：接口方法文本；直接使用解析结果，不保证未提供时为空串。

### 已生成的文档片段

- `${DocView.requestHeader}`：Header Markdown 表格；没有 Header 时为空串。
- `${DocView.requestParam}`：Param Markdown 表格；没有 Param 时为空串。
- `${DocView.requestBody}`：Body 参数 Markdown；没有参数时为空串。独立实体布局受设置影响，默认递归包含子字段。
- `${DocView.requestExample}`：带 Form / JSON 代码围栏的请求示例。Form 或 Body 只有一项时只输出该项；两者都没有时为空串。不要再套代码围栏。
- `${DocView.responseParam}`：返回参数 Markdown；没有参数时为空串。默认递归包含子字段，布局受独立实体设置影响。
- `${DocView.responseExample}`：带 JSON 代码围栏的返回示例；源示例为空或全为空白时输出空串。不要重复套代码围栏。

“空串”表示 DTO 明确生成空字符串；`name`、`desc`、`path`、`method` 是直接拷贝的源字段，不能仅凭变量清单假定缺失值会被转换为空串。若希望某个可选段落完全不出现，可在模板里用 `#if` 条件包住标题和变量，并在虚构输入上先验证。

变量目录还提供 `${DocView.type}`，值为 `SPRING` 或 `DUBBO`，表示框架分类，不是 HTTP method。结构化列表需要用 `#foreach` 等指令遍历；直接插入列表表达式不会生成自定义表格。

## 结构化参数列表与字段

需要自己改表格列时，用结构化列表逐项渲染，而不是拆解已生成的 Markdown 字符串：

- `DocView.requestHeaderDataList`：请求 Header，没有层级子字段。
- `DocView.requestParamDataList`：请求 Param，当前为平铺列表。
- `DocView.requestBodyDataList`：请求 Body，可能包含 `childList`。
- `DocView.responseParamDataList`：返回参数，可能包含 `childList`。

每个元素是 `DocViewParamData`，常用字段如下：

- `name` (`String`)：参数名；`type` (`String`)：参数类型。
- `required` (`Boolean`)：是否必填；`example` (`String`)：示例值；`desc` (`String`)：描述。
- `since` / `version` (`String`)：起始版本和版本。
- `prefixSymbol1` / `prefixSymbol2` (`String`)：Body / 返回字段的层级前缀。
- `childList` (`List<DocViewParamData>`)：下一层字段。

不要把 `psiElement` 当作模板字段使用：它是内部 PSI 引用，不是稳定的文档字段。缺少参数时各列表为空列表，`#foreach` 不会生成数据行。

下面的循环展示一个简单的 Param 列表：

    #foreach($param in $DocView.requestParamDataList)
    - $!{param.name} | $!{param.type} | $!{param.example} | $!{param.desc}
    #end

Body 和返回列表可能嵌套。只遍历 `DocView.requestBodyDataList` 或 `DocView.responseParamDataList` 的最外层时，子字段不会自动展平；若需要保留所有层级，继续遍历 `childList`。也可以继续用内置的 `${DocView.requestBody}` / `${DocView.responseParam}` 字符串，插件已为这两项递归生成 Markdown。

下面是可选的递归宏示例：它把 Body 的每一级输出成同一张表的行，并在名称前保留插件构建的层级前缀。宏声明要放在调用之前。`TemplateGuideExamplesTest` 会从本页提取该代码，覆盖三层嵌套、必填/非必填和空列表；设置页手工检查范围见上方的验证记录。无需自定义列时，可以直接使用预渲染字符串。

    |参数名|类型|必填|示例|描述|
    |---|---|---|---|---|
    #macro(renderRows $items)
    #foreach($param in $items)
    |$!{param.prefixSymbol1}$!{param.prefixSymbol2}$!{param.name}|$!{param.type}|#if($param.required)是#else 否#end|$!{param.example}|$!{param.desc}|
    #renderRows($param.childList)
    #end
    #end
    #renderRows($DocView.requestBodyDataList)

Velocity 宏和指令语法可参考 [Apache Velocity VTL Reference](https://velocity.apache.org/engine/2.3/vtl-reference.html)。实际插件使用 IDE 提供的 Velocity 运行环境；此处以已执行的 IU 261 fixture 为依据，未声称其他 IDE 版本或任意自定义模板都已验证。

## 示例一：把接口名称改成 Markdown 标题

在模板中找到接口名称部分，将原来的名称标签和变量替换为：

    # ${DocView.name}

虚构输入：`name` 为“查询用户”。预期该处输出：

    # 查询用户

这只改变生成文档里的标题格式，不会改 Java 方法名。

## 示例二：删减 Param 表列

在 Spring 模板里，用下面内容替换原来的 Param 标题及 `${DocView.requestParam}`。例子保留参数名、类型、描述三列，删除默认表格中的必填和版本列：

    **请求参数（Param）:**

    |参数名|类型|描述|
    |---|---|---|
    #foreach($param in $DocView.requestParamDataList)
    |$!{param.name}|$!{param.type}|$!{param.desc}|
    #end

虚构输入：Param 列表只有一项，`name` 为 id、`type` 为 `String`、`desc` 为用户编号。预期表格行是：

    |id|String|用户编号|

该示例只渲染 Param 列表。若你要删减 Body 或返回参数表的列，可把列表换成 `DocView.requestBodyDataList` 或 `DocView.responseParamDataList`，并像上一节那样递归 `childList`；只遍历最外层会漏掉嵌套对象的子字段。标题、Param 表格与递归宏示例已通过同一个直接读取本页代码段的测试类验证；内置模板示例也与资源文件逐字核对。

## 输出为 `ERROR` 时

`VelocityUtils` 在 `evaluate()` 返回失败时会返回字面文本 `ERROR`；这个返回值本身不包含模板行号或原因。源码还配置了 Velocity 日志类别，但用户指南不能从 `ERROR` 文本推断具体错误，也不能保证其他异常都显示相同文本。

可以按以下有限步骤排查：

1. 如果要保留出错时的编辑草稿，先把当前模板全文另存为文本。`Reset` 会丢弃未应用草稿并回读最近一次已保存的模板；只有准备放弃这份草稿时才使用。`Cancel` 会丢弃未应用内容，已成功 Apply 的内容会保留。
2. 检查最近修改的 `#if` / `#foreach` / `#macro` 是否配有对应 `#end`，变量拼写是否来自本页列出的字段。
3. 若有可信备份，确认两份模板区分 Spring 与 Dubbo，再考虑恢复对应文本；内置默认模板可从本页复制，但这会覆盖自定义内容，操作前先保留原文。
4. 若找不到可信备份、页面文本与项目配置不一致或 ERROR 持续出现，保留可用文本并请维护者检查具体 IDE 日志和项目配置；尚未确认模板内容前不要 Apply/OK。

模板设置的示例预览单独显示错误；Velocity 提供行列位置时会包含对应位置。预览使用固定样例，不扫描当前项目，也不验证真实平台上传；要判断项目结果，应再用目标接口生成文档。
