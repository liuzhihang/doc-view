# Spec Delta

## Purpose

定义从 Doc View 当前选中 Java HTTP 方法生成可编辑请求草稿的行为，包括端点与参数支持范围、请求语义、不可用反馈和文件生命周期，使用户能够在 IDEA HTTP Client 中继续编辑和主动执行，而不误用文档显示字段或覆盖既有请求。

## ADDED Requirements

### Requirement: HTTP 入口只处理支持的单方法端点

系统 SHALL 为 Spring Controller 和 Spring MVC 注解风格 Feign 中符合本规范的单个方法提供 HTTP 草稿生成。Dubbo、普通非 HTTP 接口、构造器和非方法节点 SHALL NOT 触发生成或目录展开。入口 SHALL 使用所属项目与选中方法，不依赖外层编辑器恰好打开的文件。轻量入口仅识别直接标准 mapping；不把仅有自定义组合或继承映射的方法列为可生成候选，后台校验仍 SHALL 明确拒绝这些映射。

#### Scenario: Spring 和 Feign 独立支持
- **WHEN** 用户分别选择 Spring Controller 和 Feign 中具有单静态路径、单明确 HTTP method、受支持参数的方法
- **THEN** 两种方法均能生成对应的请求草稿
- **AND** Feign 服务发现、配置地址及属性占位符不被当成可直接调用的真实 host

#### Scenario: 不适用的选择
- **WHEN** 用户选择 Dubbo 方法、普通接口、模块、类或空节点后通过菜单或快捷键触发 HTTP 动作
- **THEN** 不解析文档、不生成文件、不改变目录展开状态

#### Scenario: 外层编辑器属于另一个方法
- **WHEN** 目录选中方法 A，但当前编辑器显示方法 B 或没有编辑器
- **THEN** 请求只对应目录中的方法 A 及其所属项目

### Requirement: 端点映射具有唯一且可解释的结果

系统 SHALL 解析直接声明的 Spring 标准 mapping 注解及可解析的编译期字符串常量，按 Spring 的类/方法组合规则获得一个路径和一个明确 HTTP method。`path` 与 `value` 别名 SHALL 等价。多路径、多 method、无明确 method、动态表达式、矛盾别名、自定义组合/继承映射、非空 params/headers 条件及通配/正则路径 SHALL 被明确拒绝，不静默选择或忽略匹配条件。

#### Scenario: 类方法路径与别名
- **WHEN** 类路径为 `/api`，方法分别用 `@GetMapping(path="/users/{id}")` 或等价 `value` 声明
- **THEN** 两种写法均得到 `GET /api/users/{id}`，之后按路径参数规则替换占位符

#### Scenario: 编译期常量
- **WHEN** 单一路径由能静态解析的字符串常量或常量拼接声明
- **THEN** 请求使用解析出的实际路径，不包含 Java 常量名称

#### Scenario: Feign 静态路径前缀
- **WHEN** FeignClient 的 path 为可静态求值的单一路径前缀，方法声明受支持的标准 mapping
- **THEN** 前缀与类/方法路径一起组成请求路径；host 仍为用户配置的变量
- **AND** 动态或不安全的前缀明确拒绝，不静默丢弃前缀

#### Scenario: 路径声明中的分隔符
- **WHEN** 单静态路径包含尾斜杠
- **THEN** 草稿保留其末尾斜杠；路径声明含 CR/LF、空白或片段分隔符时拒绝生成，不能拆分请求行

#### Scenario: 映射不唯一或不明确
- **WHEN** 有效映射有多个路径、多个 method、未限定 method、`${...}` 动态值或不一致的 `path`/`value`
- **THEN** 用户收到具体不支持原因，且不创建部分请求

#### Scenario: 额外匹配条件
- **WHEN** mapping 声明 `params="mode=full"`、`headers="X-Version=2"`，或路径含通配符/正则变量
- **THEN** 系统明确报告该条件不在支持范围，不生成遗漏匹配条件的请求

### Requirement: 请求绑定名称与文档显示名称分离

参数位置 SHALL 由 Spring 绑定注解和有效媒体类型确定。线上参数名 SHALL 使用 Spring 注解的非空 `name`/`value`，缺省时使用 Java 参数名；相互矛盾的别名 SHALL 拒绝生成。`@Parameter.name` 与 `@Parameter.in` SHALL NOT 覆盖实际绑定名称或位置。现有文档展示名称和模板输出 SHALL 保持不变。

#### Scenario: 文档名与绑定名不同
- **WHEN** Java 参数为 `userId`，声明 `@RequestParam(name="id")` 和 `@Parameter(name="用户编号")`
- **THEN** 请求使用 query key `id`，不发送 `用户编号`
- **AND** 文档参数名称仍遵循既有文档 contract

#### Scenario: 绑定注解没有显式名称
- **WHEN** 参数 `keyword` 声明 `@RequestParam` 和 `@Parameter(name="search", in=HEADER)`
- **THEN** 请求仍使用实际绑定名称 `keyword`，位置为 query

### Requirement: 路径和查询参数正确编码

系统 SHALL 支持 String、boolean 与数值基本类型及对应包装类型的显式 `@PathVariable`、`@RequestParam`。每个路径占位符 SHALL 与一个路径参数匹配；路径参数不再写入 query。具体值 SHALL 按 UTF-8 对所在 URI 组件编码一次，query 的名称和值分别编码，HTTP Client 变量语法保留为可替换占位符。

#### Scenario: 路径和 query 特殊字符
- **WHEN** 路径为 `/users/{id}`，路径示例为 `42`，query `q` 示例为 `a&b 空格`
- **THEN** 路径为 `/users/42`，query 解码后只有一个名为 `q` 的值 `a&b 空格`
- **AND** `id` 不出现在 query 中，不添加请求 body

#### Scenario: 路径值含分隔字符
- **WHEN** 一个路径变量的具体值为 `a/b`
- **THEN** 变量值中的斜杠作为路径组件数据编码，不能额外形成路径段

#### Scenario: 路径绑定缺失或重复
- **WHEN** 路径占位符没有对应变量，或多个参数争用同一绑定位置与名称
- **THEN** 生成失败并说明冲突，不保留未解释的 Spring 花括号占位符

### Requirement: Header 使用实际名称和值

系统 SHALL 将受支持标量 `@RequestHeader` 写入请求头，大小写不敏感地检查同名冲突，不写入 URL 或 body。Header 值不进行 query 编码；名称或值包含 CR/LF 等不能安全表示为单个 header 的内容时 SHALL 拒绝生成。有 body 时 Content-Type SHALL 以有效媒体类型为准：显式匹配值合并为一行，无示例/缺省值时直接使用有效类型，冲突或无法确认匹配的变量值拒绝生成。

#### Scenario: 字符串和数值 Header
- **WHEN** 方法包含示例值为 `demo` 的 `X-Trace` 和示例值为 `2` 的数值 header `X-Version`
- **THEN** 草稿包含对应两个 header 行，URL 中不包含它们

#### Scenario: Content-Type 冲突
- **WHEN** 请求 body 是 JSON，而显式 Content-Type header 的具体值声明为表单类型
- **THEN** 系统提示媒体类型冲突，不静默覆盖其中一个声明

#### Scenario: Content-Type 匹配或未提供值
- **WHEN** JSON body 同时存在显式 Content-Type 绑定，其值为 `application/json`，或未提供 example/defaultValue
- **THEN** 请求都只包含一行 `Content-Type: application/json`，不额外生成该 header 的变量

#### Scenario: Header 值包含换行
- **WHEN** 示例 header 值包含换行及另一个 header 文本
- **THEN** 系统拒绝生成，不把示例扩展为额外 header 或请求

### Requirement: JSON 请求体与 Query 可以共存

对一个受现有类型解析支持的 `@RequestBody`，有效 consumes 缺省或为单一 `application/json` 时，系统 SHALL 生成有效 JSON body 与一个匹配的 Content-Type。路径、query 和 header SHALL 保留各自位置。方法级 consumes 覆盖类级声明；无 body 的请求 SHALL NOT 自动附加 Content-Type。

#### Scenario: JSON POST 同时包含 query 和 header
- **WHEN** POST 方法有一个 JSON DTO body、query `dryRun=true` 及 header `X-Trace: demo`
- **THEN** body 为完整可解析 JSON，query 留在 URL，header 独立成行，Content-Type 为 `application/json`

#### Scenario: 方法级媒体类型覆盖
- **WHEN** 类声明一个 consumes，方法明确覆盖为 `application/json` 且具有一个受支持 body
- **THEN** 使用方法级媒体类型，不把类级类型重复写入请求

#### Scenario: 只有类级媒体类型
- **WHEN** 类声明 form-urlencoded consumes，方法没有覆盖且满足受支持的表单条件
- **THEN** 请求继承类级媒体类型并生成表单 body，不按缺省 JSON 处理

#### Scenario: 无 body 的 GET
- **WHEN** GET 方法只有路径、query 或普通 header，没有显式 Content-Type header
- **THEN** 请求没有 body，也不自动添加 Content-Type

### Requirement: 显式表单参数写入请求体

有效 consumes 为单一 `application/x-www-form-urlencoded`、method 为 POST、没有 `@RequestBody`/`@RequestPart`，且数据参数都是显式标量 `@RequestParam` 时，系统 SHALL 将这些参数编码到表单 body，Content-Type 与之匹配。未声明表单类型的 `@RequestParam` SHALL 默认进入 query；无注解对象和 `@ModelAttribute` 不在首版表单支持范围。

#### Scenario: 显式编码表单
- **WHEN** POST 声明表单 consumes，参数 `name` 示例为 `Alice & Bob`，另有一个路径参数和一个 header
- **THEN** `name` 出现在表单 body，解码后仍为 `Alice & Bob`，不在 query 中重复
- **AND** 路径参数和 header 分别保持其位置，不输出 JSON Content-Type

#### Scenario: 未声明表单类型
- **WHEN** POST 只有显式 `@RequestParam` 且没有 consumes 或 body
- **THEN** 参数写入 query，不根据 POST 方法或现有文档分类臆测为 form body

#### Scenario: 表单字段全部可选且无值
- **WHEN** 受支持 POST 表单的字段全部 required=false，且没有 example/defaultValue
- **THEN** 字段全部省略，生成空表单 body 与匹配 Content-Type，不插入 null 或未知字段

### Requirement: 示例和缺省值有明确来源

受支持的 Path/Query/Form/Header 标量值 SHALL 优先使用非空 `@Parameter.example`，然后使用绑定注解提供的有效 defaultValue（含显式空字符串，不含框架未设置哨兵）。两者均缺失时，路径占位符及其他必需值 SHALL 使用可辨识的 HTTP Client 变量，可选 Query/Form/Header SHALL 省略；不得因 Java 空引用输出字面量 `null`。DTO、标量和集合 body 示例 SHALL 使用有界的既有类型默认示例，保证 JSON 语法有效；受支持元素类型的顶层数组 SHALL 生成可编辑的空数组 `[]`，不得因此接受文件/流等不支持元素。Content-Type 与认证 header 的专门规则优先。

#### Scenario: 默认值与示例优先级
- **WHEN** query 参数有 defaultValue `10`，又有非空 example `20`
- **THEN** 草稿值为 `20`；移除 example 后使用 `10`

#### Scenario: 必需和可选值都没有示例
- **WHEN** 必需 query `q` 和可选 query `note` 均没有 example/defaultValue
- **THEN** `q` 使用 HTTP Client 变量占位，`note` 被省略
- **AND** 不生成由空引用造成的 `q=null` 或 `note=null`

#### Scenario: 路径参数被标为可选
- **WHEN** 已选单一路径包含 `{id}`，对应 `@PathVariable(required=false)` 没有 example
- **THEN** 仍使用 HTTP Client 变量替换该路径段，不省略路径段或残留 Spring `{id}`

#### Scenario: 递归 body
- **WHEN** JSON DTO 含递归字段或嵌套泛型
- **THEN** body 示例沿用既有深度和循环限制，生成有限且语法有效的 JSON，不无限展开

### Requirement: HTTP 必需值遵循 Spring 绑定规则

HTTP 值是否必需 SHALL 取 Spring 绑定注解默认值和显式 required；RequestParam/RequestHeader 的有效 defaultValue 使绑定可选，但该值仍作为草稿示例使用。Doc View 文档必填设置、Validation 标记与 `@Parameter.required` SHALL NOT 改变传输规则。路径中存在的变量段仍必须替换，Content-Type 按 body 专门规则生成。

#### Scenario: 文档必填与线上绑定冲突
- **WHEN** Spring 声明 `required=false` 且没有 example/defaultValue，而文档配置或 `@Parameter.required` 标为必填
- **THEN** 可选 Query/Form/Header 不因文档标记被强行生成，文档本身仍遵循原有必填 contract

#### Scenario: 缺省必填与有效默认值
- **WHEN** 一个 RequestParam 未指定 required 且没有 defaultValue，另一个提供 defaultValue（包括显式空字符串）
- **THEN** 前者无 example 时使用变量，后者使用其实际默认值，框架未设置哨兵不出现在输出中

### Requirement: 不支持场景不得静默降为错误请求

multipart、文件/流（包含其数组或集合 body）、Map/Optional body、`@RequestPart`、Cookie/Map/集合/数组/Optional 形式的 HTTP 绑定、隐式复杂对象/ModelAttribute、多 body、多媒体类型、未知媒体类型、非空 consumes 却没有对应受支持 body，以及无法解析的绑定 SHALL 返回明确不支持原因。运行时未知的能力 SHALL NOT 被当成“无参数”或自动改成 JSON；不支持一个参数时 SHALL 不发布遗漏该参数的成功草稿。

#### Scenario: 上传接口或未知媒体类型
- **WHEN** 方法使用 multipart 文件参数、多个 body 或非支持 consumes
- **THEN** 提示具体不支持的形式，不创建或打开新请求

#### Scenario: 普通未绑定业务参数
- **WHEN** 方法包含无法确定传输位置的未注解业务参数
- **THEN** 系统拒绝猜测位置，不自动拼入 query

### Requirement: 草稿来自当前源码且只生成不执行

请求 SHALL 基于调用时已提交到 PSI 的当前编辑内容，而非旧导出 Markdown 或参数编辑弹窗中未确认的草稿。基础地址 SHALL 保留为 `{{host}}`。生成、取消与打开草稿 SHALL NOT 发起网络请求、写入环境/凭据配置或自动执行请求；实际运行由用户在 HTTP Client 中主动触发。

#### Scenario: 编辑但未保存源码
- **WHEN** 用户在 Java 编辑器修改端点路径，编辑内容已同步到 PSI 但尚未保存磁盘，然后生成请求
- **THEN** 草稿使用新路径，不读取磁盘旧源码或旧导出文档
- **AND** 不为生成请求强制保存整个项目

#### Scenario: 生成后等待编辑
- **WHEN** 请求草稿生成并在 HTTP Client 打开
- **THEN** host 仍是变量，用户可编辑和选择环境，服务端没有因生成动作收到请求

### Requirement: HTTP 生成可取消且不阻塞界面

端点解析、类型展开和请求构建 SHALL 在可取消后台工作中进行；action 更新 SHALL 不做全项目扫描、递归解析或请求生成。执行期间 SHALL 只处理选中方法所需范围，界面保持响应并提供取消入口。同一项目/方法已有生成任务时重复触发 SHALL 合并为该在途请求，不并发发布重复草稿。

#### Scenario: 大请求模型与重复点击
- **WHEN** 请求模型解析尚未完成，用户移动目录选择并再次触发同一方法
- **THEN** 界面可继续交互，原任务目标不随选择改变，同一方法最多发布一份该次草稿

#### Scenario: 用户取消
- **WHEN** 用户在发布开始前取消生成
- **THEN** 任务结束，不创建、改写或打开草稿，取消不显示为生成失败

### Requirement: 失效结果不得发布

发布前 SHALL 再次确认所属项目、方法、请求代次和源码状态仍有效。项目关闭、源码失效或生成期间相关源码变化时 SHALL 丢弃结果并在仍可交互时提示重新生成；不得把过期内容发布为当前请求。生成或写入失败 SHALL 明确反馈且不打开部分草稿。

#### Scenario: 项目关闭或源码被删除
- **WHEN** 后台任务完成前项目关闭，或选中方法已删除
- **THEN** 不创建或打开草稿，不回调已释放的 UI

#### Scenario: 生成期间修改源码
- **WHEN** 请求依据的源码在构建后、发布前发生变化
- **THEN** 丢弃旧结果，提示需要从当前源码重新生成，不自动发送或无界重试

#### Scenario: 写入失败
- **WHEN** HTTP 草稿无法完整创建或写入
- **THEN** 用户收到脱敏失败提示，旧草稿内容不变，不打开残缺请求

#### Scenario: 请求超过平台可编辑文件大小
- **WHEN** 包含来源注释的完整请求按 UTF-8 编码后超过目标 IDE 的完整内容加载上限
- **THEN** 系统在后台明确提示请求过大，不创建或打开只读大文件，不截断请求，也不修改已有草稿
- **AND** 字符数不能代替 UTF-8 字节数；仅超过代码洞察阈值但仍可完整编辑的请求不因此拒绝

### Requirement: 请求草稿隔离身份并保护用户编辑

不同项目、模块、源文件、类及完整方法签名 SHALL 对应可区分的请求来源，业务标题和行号 SHALL NOT 作为唯一身份。每次完成后的再次生成 SHALL 新建一个独立草稿或追加独立请求块，并聚焦新结果；原有已保存和未保存的请求内容 SHALL 保持原样，不自动迁移、清理或覆盖旧业务标题草稿。

#### Scenario: 同名重载与不同项目
- **WHEN** 相同业务标题的重载方法或两个项目中的同名方法分别生成请求
- **THEN** 新结果互不覆盖，能够区分各自项目和方法来源

#### Scenario: 用户编辑后再次生成
- **WHEN** 用户在旧请求中修改 URL/header/body，未保存就再次生成同一方法
- **THEN** 旧编辑内容完整保留，新生成结果成为独立可编辑请求

### Requirement: 运行环境与生成路线明确

HTTP 功能 SHALL 使用已验证目标 IDE 上的一条生产生成路线，不向用户暴露原生/自制模式切换，也不在失败时隐式改用另一套生成器。必要平台能力缺失、索引未就绪或方法不支持时 SHALL 明确显示不可用状态或原因；不得调用未支持的内部 API 绕过平台边界。

#### Scenario: 所需平台组件不可用
- **WHEN** 目标环境缺少所需 HTTP Client/原生生成能力或处于索引未就绪状态
- **THEN** HTTP 入口不可执行或报告明确原因，不生成降级的伪正确请求

#### Scenario: 原生生成遇到无法处理的输入
- **WHEN** 已选生产路线处理当前方法失败
- **THEN** 显示失败原因，不静默调用另一套生成器，不把失败当成已成功打开

### Requirement: 文档生成与凭据边界保持稳定

HTTP 改造 SHALL 保持 Spring/Feign/Dubbo 的 Markdown、默认/用户模板、上传 payload、参数编辑写回和文档文件命名不变。运行日志、测试及示例 SHALL 不包含真实凭据或私有源码；认证类 header SHALL 使用变量占位，生成动作不读取平台上传凭据或 HTTP Client 私有环境值。

#### Scenario: 同一源码生成文档和 HTTP
- **WHEN** 用户分别在变更前后生成同一源码的 Markdown、平台 payload 和 HTTP 草稿
- **THEN** 文档和 payload 保持既有 contract，只有 HTTP 输出按本规范修正

#### Scenario: 认证 header
- **WHEN** 方法包含大小写不敏感的 Authorization、Proxy-Authorization、X-Api-Key、Api-Key 或 ApiKey header，且按值选择规则需要输出该 header
- **THEN** 草稿使用变量，不从平台设置或私有环境读取真实值，也不把源代码中的凭据示例写入日志
