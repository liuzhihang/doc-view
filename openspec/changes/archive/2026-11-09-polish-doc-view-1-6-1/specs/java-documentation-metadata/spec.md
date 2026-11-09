## ADDED Requirements

### Requirement: Javadoc 标题与正文分离
系统 SHALL 将类普通 Javadoc 的首个非空物理行作为标题；方法普通 Javadoc 名称默认同样只取首行。系统 SHALL 保留单词间空格与 Unicode 字符，排除 block tags；类描述与方法描述默认使用各自首行之后的普通正文，保留段落，不截断。Swagger、显式 DocView tag 和 Java 名称 fallback 优先级保持不变；参数、字段和版本 tag 语义保持不变。

#### Scenario: 多行注释与标签
- **WHEN** Spring、Feign 或 Dubbo 的类/方法注释包含标题、空白行、两行正文、@param 和 @return
- **THEN** 默认标题仅含首个非空行，方法描述包含完整后续正文与段落，不包含 block tags
- **AND** 类后续正文独立保留为 classDesc，默认模板以类说明展示，自定义模板可使用 ${DocView.classDesc}；不混入方法 desc

#### Scenario: 空注释与单行注释
- **WHEN** 注释只有标签、空白或只有一行正文
- **THEN** 空标题按既有来源 fallback，单行注释的方法描述为空，原参数/字段描述继续正常生成

### Requirement: 标题和描述生成设置
系统 SHALL 提供普通 Javadoc 标题字符上限（默认80，有效范围1–200，按 Unicode code point计数），作用于类和方法普通注释标题；方法“仅首行”默认开启、Javadoc后续正文描述来源默认开启。关闭仅首行时方法标题取普通正文合并行后再限长；关闭正文来源且没有更高优先级描述时输出空描述。限长 MUST NOT 作用于描述、注解、显式tag或Java名称。

#### Scenario: 设置保存与取消
- **WHEN** 修改三个新设置后 Apply 或 Reset，或取消设置窗口并重新打开
- **THEN** Apply 持久化且触发相关生成缓存失效；Reset 恢复已应用值；取消不更改持久化值
- **AND** 越界或非整数输入禁止 Apply 且说明有效范围，不部分保存同页字段

#### Scenario: Unicode 上限与描述保真
- **WHEN** 普通注释标题超出用户上限且包含补充平面字符，后续描述长于上限
- **THEN** 标题在完整 Unicode code point 边界截断、不附加额外字符，后续描述完整保留
