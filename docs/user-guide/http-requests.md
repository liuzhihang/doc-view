# 生成 HTTP 请求

本文描述 1.6.0 的 HTTP 生成与菜单操作。插件生成可编辑的 `.http` 草稿；实际编辑、环境变量、执行和响应查看由 IDEA HTTP Client 提供。生成动作本身不发送请求。

## 入口与使用

1. 在 Doc View 目录选中一个 Spring Controller 或 Spring MVC 注解风格的 Feign 方法。
2. 右键选择 **生成 HTTP 请求 / Generate HTTP Request**。请求在后台生成，可在 IDE 后台任务中取消。
3. 在新打开的草稿中配置 `{{host}}` 和缺失的变量，或直接编辑草稿中的值；确认目标和参数后，再主动执行请求。

执行请求还需当前 IDEA 的 HTTP Client 执行能力可用；若编辑器提示激活 IDE 许可，草稿仍可编辑，但必须先按 IDE 提示解决许可状态才能发送。

生成器不读取 Feign 服务发现、`server.port`、HTTP Client 私有环境或平台上传凭据。它使用目录中选中方法所属 Java 文件的当前源码（包含尚未保存到磁盘的编辑），不依赖活动编辑器的焦点，也不读取先前导出的 Markdown。

## 支持范围

| 输入 | 生成规则 |
| --- | --- |
| 标准 Spring mapping | 单静态路径、单明确 HTTP method；支持类/方法路径组合、`path`/`value` 及编译期字符串常量 |
| `@PathVariable` | 按路径段编码并替换占位符，不重复添加到 query |
| `@RequestParam` | 默认放入 query；明确 POST + form-urlencoded 时放入表单 body |
| `@RequestHeader` | 写入请求头，包括字符串、boolean、数值基本类型及对应包装类型 |
| 一个 `@RequestBody` | DTO/标量/集合使用现有有界类型解析生成 JSON；受支持元素类型的顶层数组生成可编辑的 `[]`；允许 query/header 同时存在 |
| JSON 或 URL 编码表单 | 输出一个匹配的 Content-Type；方法级 consumes 覆盖类级声明 |

Path/Query/Form/Header 首版只支持 String、boolean、数值基本类型及对应包装类型。线上绑定名使用 Spring 的 `name`/`value`，未声明时用 Java 参数名；`@Parameter.name/in` 只影响原有文档规则，不覆盖 HTTP 绑定。

标量值优先使用非空 `@Parameter.example`，其次为 Spring 绑定的有效 defaultValue（包括显式空字符串）。仍无值时，必需参数生成变量，可选 Query/Form/Header 省略；路径中已有的变量段始终替换，即使声明了 `required=false`。HTTP 必需性遵循 Spring 的 required/defaultValue，不受文档必填设置影响。缺失示例不会生成字面量 `null`。

例如，`/users/{id}` 的路径示例 `42`、query 示例 `a&b 空格`、header 示例 `demo` 会形成各自独立的部分：

```http
GET {{host}}/users/42?q=a%26b%20%E7%A9%BA%E6%A0%BC
X-Trace: demo
```

显式表单参数位于 header 后的 body，特殊字符进行表单编码，不再放入 URL：

```http
POST {{host}}/users
Content-Type: application/x-www-form-urlencoded

name=Alice+%26+Bob
```

无 body 时不自动添加 Content-Type。显式 Content-Type 与 body 匹配时合并为一行，没有值时使用有效媒体类型；冲突或无法确认匹配的变量会使生成失败。Header 名称和值中的换行等不安全字符会被拒绝。

Authorization、Proxy-Authorization、X-Api-Key、Api-Key、ApiKey（大小写不敏感）在需要输出时始终使用变量，不复制源码中的凭据示例。

## 明确不支持的情况

- Dubbo、普通接口、构造器、类/模块节点不提供 HTTP 生成入口；原有文档功能仍可用。
- 多路径、多 method、未限定 method、动态路径、自定义组合或继承映射、矛盾别名、通配/正则路径、非空 mapping params/headers 条件。
- Map、集合、数组、Optional 形式的 HTTP 标量绑定，Cookie、未绑定业务对象、ModelAttribute、文件/流、RequestPart、multipart。
- 多个 body、多种或未知媒体类型，声明 consumes 却没有对应受支持的 body，无法解析的请求类型；Map、Optional、文件/流 body，以及含不支持元素的数组/集合 body。

没有直接标准 mapping 的自定义组合或继承端点不提供生成入口；后台生成器也会拒绝这类输入。其余进入生成流程的不支持输入会得到明确提示，不生成遗漏参数的部分请求。索引中或 HTTP Client 组件不可用时，入口禁用并显示原因。生成失败也不会切换到另一套生成器。

请求大小按包含来源注释的完整 UTF-8 内容检查 IDEA 的完整内容加载上限。超过上限时，后台会明确提示请求过大，不创建或打开只读的大型 HTTP 文件、不截断请求，已有草稿保持不变。仅超过代码洞察阈值但仍可完整编辑的请求仍允许生成。

## 取消、再次生成与文件保护

同一方法的在途点击合并为一次任务；完成后再次生成会打开独立的新草稿。已有草稿（包括未保存的手工修改）保持原样，不迁移或清理历史请求。来源标记区分项目和完整方法签名，同名重载互不覆盖。

需要取消时，打开 IDEA 的 **Stop Background Processes / 停止后台进程**，选择正在进行的 **Generating HTTP request / 正在生成 HTTP 请求**。本次 macOS 默认键位为 `Shift+Command+F2`；自定义 Keymap 以 Find Action 中显示的快捷键为准。任务已经完成时不会出现在停止列表中。

发布开始前取消不会生成文件；解析期间源码改变、方法失效或项目关闭会使旧结果失效。需要时从当前源码重新触发。生成或写入失败不会打开不完整请求；已成功交付的草稿不因随后取消而回滚。

## 工具窗口操作

主工具栏保留 **刷新、导出下拉、上传、展开全部、折叠全部**。**设置** 和 **清缓存** 位于工具窗口的原生 **…** 菜单；清缓存只清所属项目的 schema 和文档查询派生内存缓存，保留 HTTP 草稿、手写 Scratch、未保存编辑和导出文档。

目录原来的 **Open** 改为 **打开 Markdown 文档 / Open Markdown Document**；源码导航继续使用 IDEA 的导航动作。目录获得焦点后可用 Speed Search 搜索方法名、完整 Java 签名和完整方法说明；说明的搜索范围不受灰色副标题长度限制。类、模块和空节点上的文档/HTTP 动作隐藏，程序化触发也不再展开目录。

相关：[使用指南](index.md)、[设置](settings.md)、[问题排查](troubleshooting.md)；实施与未覆盖验收见 [HTTP change 验证记录](../../openspec/changes/http-client-generation-and-ui/verification.md)。
