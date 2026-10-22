# Template Settings Lifecycle

## Purpose

定义项目级 Spring 与 Dubbo 模板在设置页打开、编辑、重置、提交、取消和重新打开时的可观察行为，保护已有自定义文本并确保模板编辑资源随设置页关闭而释放，使用户仅在明确应用修改后改变后续生成结果。

## ADDED Requirements

### Requirement: 打开模板设置不改变已保存模板

系统 SHALL 在打开模板设置页时显示当前项目最近保存的 Spring 和 Dubbo 模板，MUST NOT 因打开页面、切换模板标签或初始化设置而覆盖已保存内容。未编辑时页面 SHALL 处于未修改状态。

#### Scenario: 打开已有自定义模板
- **WHEN** 当前项目保存了不同于内置默认值的 Spring 与 Dubbo 模板并打开模板设置页
- **THEN** 两个编辑器分别显示对应的已保存文本
- **AND** 项目设置保持逐字符一致，页面不报告未保存修改

#### Scenario: 新项目首次打开
- **WHEN** 项目没有自定义模板且首次打开模板设置页
- **THEN** 编辑器显示现有内置默认模板
- **AND** 打开页面不产生额外模板修改

### Requirement: 编辑与重置只影响未应用草稿

系统 SHALL 将模板编辑保留为设置页草稿；平台 Reset SHALL 放弃未应用草稿并重新显示最近保存的文本，MUST NOT 将项目设置恢复为内置默认模板。重复 Reset SHALL 保持相同结果。

#### Scenario: 编辑后尚未应用
- **WHEN** 用户修改一个或两个模板且尚未执行 Apply/OK
- **THEN** 编辑器显示草稿，页面报告存在修改
- **AND** 项目保存状态及后续新生成文档使用的模板仍为上次保存值

#### Scenario: 编辑后执行 Reset
- **WHEN** 用户修改草稿后执行平台 Reset
- **THEN** 两个编辑器恢复到项目最近保存的模板，页面恢复未修改状态
- **AND** 项目设置保持不变，重复 Reset 不产生额外修改

### Requirement: 仅应用操作提交模板修改

系统 SHALL 在用户成功执行 Apply 或 OK 时保存当前两个模板草稿，并使后续新生成文档使用这些值。未编辑的模板 SHALL 保持原文；保存后页面 SHALL 不再报告模板修改。文本不得因本修复被裁剪、重排或格式化。

#### Scenario: 仅修改 Spring 模板后 Apply
- **WHEN** 用户仅修改 Spring 模板并执行 Apply
- **THEN** Spring 保存为编辑后的原文，Dubbo 保留原保存值
- **AND** 页面处于未修改状态，后续新生成的 Spring 文档使用新模板

#### Scenario: 仅修改 Dubbo 模板后 OK
- **WHEN** 用户仅修改 Dubbo 模板并执行 OK
- **THEN** Dubbo 保存为编辑后的原文，Spring 保留原保存值
- **AND** 再次打开时显示保存结果，后续新生成的 Dubbo 文档使用新模板

#### Scenario: 两种模板同时保存
- **WHEN** 用户修改两种模板并 Apply，文本包含中文、空行、空格或 Velocity 表达式
- **THEN** 两种保存值分别与其编辑器文本逐字符一致
- **AND** 再次 Apply 或在无修改时 OK 不进一步改变文本

### Requirement: 取消仅丢弃未应用修改

系统 SHALL 在用户 Cancel 或以不保存方式关闭设置时丢弃尚未应用的模板草稿，MUST NOT 写入默认模板或撤销最近一次成功 Apply。再次打开页面 SHALL 使用最近已保存的状态。

#### Scenario: 打开后直接取消
- **WHEN** 用户打开已有自定义模板设置后不编辑并 Cancel
- **THEN** 两种项目模板保持不变
- **AND** 再次打开和后续新生成文档使用原保存模板

#### Scenario: 编辑后取消
- **WHEN** 用户编辑模板但未 Apply，随后 Cancel
- **THEN** 草稿被丢弃，两种项目模板保持上次保存值
- **AND** 再次打开不显示已丢弃草稿

#### Scenario: Apply 后继续编辑再取消
- **WHEN** 用户将模板从 A 改为 B 并成功 Apply，再改为 C 后 Cancel
- **THEN** 项目保存值和再次打开的编辑器值均为 B
- **AND** 不恢复为 A、C 或内置默认值

### Requirement: 当前模板状态持久化且默认输出不变

系统 SHALL 沿用当前版本的项目级模板配置格式，并在 IDE 正常保存、关闭和重新打开项目后保留最近应用的值；未应用草稿 MUST NOT 进入保存结果。项目之间的模板修改 MUST NOT 相互污染。该 change 不修改默认模板内容、Velocity 变量或存储标识，也不要求旧格式迁移、跨版本兼容或降级读取。

#### Scenario: 当前版本格式保存并重启
- **WHEN** 用户应用模板 B，随后编辑 C 并 Cancel，再由 IDE 正常保存、关闭并重新打开项目
- **THEN** 重新打开的模板为 B，生成新文档也使用 B
- **AND** C 和因设置页生命周期产生的默认值不会覆盖 B

#### Scenario: 当前版本状态序列化往返
- **WHEN** 当前版本的项目模板状态经现有 XML 序列化与 loadState 往返
- **THEN** 最近应用的 Spring/Dubbo 值被保留，未应用草稿不进入保存状态
- **AND** 此场景不要求跨版本迁移或保证旧插件版本读取该配置

#### Scenario: 默认输出保持不变
- **WHEN** 项目沿用内置模板且输入 Java 与生成设置未变化
- **THEN** 默认生成结果保持既有 Markdown contract
- **AND** 本修复不改变 Spring/Feign/Dubbo 解析、复制、导出或上传格式

#### Scenario: 两个项目的模板独立
- **WHEN** 用户在项目 A 应用自定义模板并在项目 B 打开模板页
- **THEN** 项目 B 显示其自己的模板
- **AND** 项目 A 的草稿、应用和取消不修改项目 B 的模板

### Requirement: 模板页可安全重复打开和关闭

系统 SHALL 在模板设置页关闭后释放该页创建的编辑资源，MUST NOT 因关闭而应用草稿。重复打开/关闭或重复清理 SHALL 不遗留本页编辑器、不影响其他编辑器，并使再次打开的页面正常加载已保存模板。

#### Scenario: 关闭后再次打开
- **WHEN** 用户连续打开和关闭模板页，关闭过程包含 Apply/OK 与 Cancel
- **THEN** 每次关闭后该页的编辑资源均被释放
- **AND** 其他编辑器不受影响，再次打开正常显示最近保存值

#### Scenario: 重复清理或未创建页面
- **WHEN** 设置框架对已关闭的模板页重复清理，或清理尚未创建 UI 的配置页
- **THEN** 不抛出重复释放或空引用错误
- **AND** 项目模板与其他编辑器保持不变
