# Spec Delta

## Purpose

为当前 IDEA 项目的 Markdown 与 JSON 文档库提供清晰的生成、搜索、打开和状态入口。让用户区分 PSI 接口目录、已落盘快照与显式写入，并在未知、冲突、取消或部分发布时看到准确结果。

## ADDED Requirements

### Requirement: 项目文档界面绑定当前项目的固定文档根

Doc View 的项目文档操作 SHALL 使用当前 IDEA 项目及其固定 `docs/doc-view/` 文档根，不提供任意目录导入、注册或跨项目共享根。

#### Scenario: 在一个项目窗口执行项目文档操作
- **WHEN** 用户从项目 A 的 Doc View 工具窗口打开、搜索或生成项目文档
- **THEN** 操作只读取或写入项目 A 的 `docs/doc-view/`
- **AND** 项目 B 的文档根不受影响

### Requirement: 主工具栏保留五个常用入口

Doc View 主工具栏 SHALL 保持刷新接口目录、单一导出下拉、上传、展开全部和折叠全部五个入口；设置和清缓存 SHALL 留在工具窗口原生 `…` 菜单，不新增项目文档顶层按钮。

#### Scenario: 查看工具窗口主操作
- **WHEN** 用户打开 Doc View 工具窗口
- **THEN** 主工具栏显示“刷新接口目录 / Refresh API Catalog”、Export 下拉、Upload、Expand 和 Collapse
- **AND** Settings 与 Clear Cache 可从原生 `…` 菜单到达

### Requirement: 导出下拉提供项目文档生成、打开和搜索入口

Export 下拉 SHALL 保留 Export Markdown，将现有项目文档导出入口命名为 Generate or Update Project Docs，并提供 Open Project Docs 与 Search Project Docs。生成和更新使用同一入口，不提供独立 Update 按钮。

#### Scenario: 查看项目文档操作
- **WHEN** 用户打开 Export 下拉
- **THEN** 用户可分别选择 Export Markdown、Generate or Update Project Docs、Open Project Docs 或 Search Project Docs
- **AND** 打开或搜索不会触发生成或写入

#### Scenario: 打开项目索引
- **WHEN** 用户选择 Open Project Docs 且当前项目已有可读的 `index.md`
- **THEN** IDEA 打开当前项目已落盘的 `docs/doc-view/index.md`
- **AND** 不打开方法级 Scratch 文档

#### Scenario: 打开尚未生成的项目文档
- **WHEN** 用户选择 Open Project Docs 但当前项目没有可读的文档索引
- **THEN** 工具窗口显示“尚未生成项目文档”及显式生成入口
- **AND** 不扫描源码、不创建目录或文件

### Requirement: 项目文档搜索使用落盘机器清单

Search Project Docs SHALL 从当前项目 `index.json` 的摘要执行查询，并按稳定接口身份返回结果；它 SHALL NOT 用 PSI 树搜索或解析 Markdown 代替文档库查询。选择结果 SHALL 打开对应已落盘 Markdown 详情页。

#### Scenario: 搜索并打开接口文档
- **WHEN** 用户搜索已生成的接口名或完整签名并选择一个结果
- **THEN** 搜索结果区分不同重载并显示来自同一次索引读取的文档库状态
- **AND** IDEA 打开匹配的 `apis/<stem>.md`，而不是新建 Scratch 文档

#### Scenario: 搜索尚未生成的文档库
- **WHEN** 用户选择 Search Project Docs 且当前项目没有受支持的 `index.json`
- **THEN** 工具窗口说明需要显式生成或当前格式不受支持
- **AND** 不从 Markdown 反解析，不隐式运行生成

#### Scenario: 查询文件损坏或身份哈希不匹配
- **WHEN** 搜索时发现索引、详情身份或内容哈希无法相互验证
- **THEN** 工具窗口显示不可查询及需重新生成的原因
- **AND** 不混合展示不同代的记录，不回退到 Markdown 或 PSI 搜索

#### Scenario: 文件恢复后重新验证并清除对应错误
- **WHEN** 用户将先前被拒绝读取的受管理 Markdown 恢复为索引记录的精确字节，并再次打开该文件
- **THEN** 完整路径、raw-byte hash 与 UTF-8 验证成功后，工具窗口清除该文件对应的完整性错误并刷新可见状态
- **AND** 其它资源仍有完整性错误时继续显示错误；错误全部清除后显示原 freshness（例如 `Current for this session`），不因重新打开或恢复文件而生成文档或扫描 PSI

#### Scenario: 窄工具窗口中搜索并返回目录
- **WHEN** 用户在窄工具窗口打开项目文档搜索
- **THEN** 返回目录操作使用紧凑的平台返回图标，并保留本地化提示和可访问名称
- **AND** 搜索输入框仍可获得足够宽度以输入有意义的查询
- **AND** 当焦点在搜索卡片内（包括输入框、结果列表、返回按钮、“更多结果”和“打开 Markdown”）时，按 Escape 可返回接口目录，并把键盘焦点交还目录树；接口目录和其他视图的 Esc 行为不变

### Requirement: 文档库状态准确表达快照新鲜度

工具窗口 SHALL 区分未生成、生成中、current、stale、unknown、conflict 和 partial。current 仅表示本会话完成生成后尚未观察到源码、模板或设置变更，不表示实时一致；stale 表示本会话已观察到相关变更，重新打开项目且无法证明一致性时 SHALL 为 unknown。

#### Scenario: 生成后观察到变更
- **WHEN** 当前会话的生成/更新成功完成且尚未观察到相关变更
- **THEN** 工具窗口显示 current
- **AND** 后续观察到源码、模板或相关设置改变时改为 stale

#### Scenario: 重新打开已有文档库
- **WHEN** 项目重新打开且磁盘文档库存在，但本会话没有可验证的变更基线
- **THEN** 工具窗口显示 unknown
- **AND** 不因文件存在或修改时间显示 current

#### Scenario: 生成目标有用户修改冲突
- **WHEN** 生成/更新发现已管理页面或索引的所有权、内容哈希或路径冲突
- **THEN** 工具窗口显示 conflict 并指出冲突路径
- **AND** 保留冲突文件，不静默覆盖或删除旧页面

#### Scenario: 旧页面不再属于当前快照
- **WHEN** 当前源码快照不再包含磁盘上已有的接口页面
- **THEN** 更新后的索引不再列出该页面
- **AND** 原页面保留在磁盘，由用户决定是否删除或另行归档

### Requirement: 文档状态与进度使用非模态反馈

查询错误、缺失、未知、新鲜度、冲突和部分发布状态 SHALL 通过一个非模态内联状态区表达；生成过程 SHALL 使用 IDEA 原生可取消进度，不以多套模态对话框打断用户。

#### Scenario: 后台生成或更新
- **WHEN** 用户显式启动生成/更新
- **THEN** 原生进度显示扫描、校验和发布状态，并允许取消
- **AND** 工具窗口的内联状态区保留最终结果或原因

### Requirement: 取消遵守 JSON 索引提交边界

生成发布 SHALL 按文档库规定的文件顺序逐项检查取消，并以 `index.json` 成功提交作为本次索引发布边界。边界前取消 SHALL 停止后续发布；已有文件发布时报告路径及 partial 状态，无文件发布时保留原文档库状态。边界后 SHALL NOT 回滚已提交文件或将已完成发布显示为未完成。

#### Scenario: 发布开始前取消
- **WHEN** 用户在任何目标文件发布前取消生成
- **THEN** 后续发布停止，已有文档文件与原新鲜度状态保持不变
- **AND** 进度显示取消，不把本次任务标记为生成成功或 partial

#### Scenario: 索引提交前取消
- **WHEN** 用户在 `index.json` 成功发布前取消生成
- **THEN** 后续文件发布停止，已发布路径被报告且文档库显示 partial
- **AND** 不将旧索引与部分新文件报告为 current

#### Scenario: 索引提交后取消
- **WHEN** `index.json` 已成功发布后收到取消
- **THEN** 已提交文档不回滚
- **AND** 工具窗口保留已完成发布结果，不显示为未完成或部分发布

### Requirement: 清缓存只清理派生内存数据

工具窗口原生 `…` 菜单 SHALL 保留 Settings 与 Clear Cache。清理项目文档查询缓存时只可清除可重建的派生内存数据，不得删除或改写 Markdown、JSON 文档库，也不得仅因清缓存把 current 改为 stale。

#### Scenario: 清除项目文档派生缓存
- **WHEN** 当前文档库为 current 且用户从原生 `…` 菜单执行 Clear Cache
- **THEN** 磁盘 Markdown 与 JSON 文件保持不变，文档状态仍为 current
- **AND** 清缓存不会触发生成或修改源码

### Requirement: 入口名称和图标保持一致

同一项目文档动作在工具栏、Export 下拉、目录菜单、维护菜单和 Find Action 中 SHALL 使用一致的中英文名称、语义和平台图标；状态提示 SHALL 跟随界面语言，界面调整 SHALL NOT 重绘 Doc View 品牌 SVG 或节点图标。

#### Scenario: 使用中英文界面查看同一动作
- **WHEN** 用户切换 IDEA 英语或简体中文界面并查看同一项目文档操作
- **THEN** 名称与提示按界面语言表达相同语义，重复入口使用一致平台图标
- **AND** 现有品牌 SVG 与节点图标保持原样

### Requirement: 搜索结果解释命中并支持连续键盘浏览

文档搜索 SHALL 用主题支持的强调样式区分可定位的字面命中、普通文字与当前选中行；键盘用户 SHALL 能从输入框进入结果、打开文档并返回目录。交互不得隐式生成文件或更改查询与分页规则。

#### Scenario: 输入即搜与多词高亮
- **WHEN** 用户在原生搜索框输入或粘贴关键词
- **THEN** 短暂停顿后自动显示最新查询的第一页，无需点击搜索或按 Enter；连续编辑合并，新词输入后立即移除旧页及高亮，等待期间保留正在搜索提示，迟到的结果不能覆盖新词
- **AND** 清除输入后自动恢复未筛选首页；离开搜索或释放面板后取消待执行请求
- **AND** 在名称、HTTP 路径、签名、模块或描述中标出可直接定位的命中；仅在来源路径或标签解释命中时补充对应字段，保持原始文字，不解析其中的 HTML
- **AND** 不用颜色作为唯一线索；选中行保留主题选区与可辨认的命中样式，空查询不强调；命中片段与产生该页的查询一致
- **AND** 仅通过 Unicode 规范化等价而无法安全定位原文时，不伪造命中位置，不丢弃索引返回的条目

#### Scenario: 键盘浏览与窄窗
- **WHEN** 用户在搜索框中按向下键且存在结果
- **THEN** 进入已有选中项或第一条结果；Enter 打开当前 Markdown，Esc 返回接口目录，不触发生成
- **AND** 无结果时向下键不移动焦点；双击列表空白处不打开此前的选择
- **AND** 输入框显示简短的空值提示；返回按钮与状态条在窄窗中保留完整提示；搜索状态栏优先显示本次操作反馈和简短的快照新鲜度；文档库总数与完整的快照说明可从悬停提示读取。完整性错误和操作失败仍直接显示在搜索状态栏

#### Scenario: 首屏搜索未完成时按下向下键
- **WHEN** 用户在原生输入框按 ↓ 且当前查询的第一页仍在等待或加载
- **THEN** 当前查询有结果且用户未离开输入框时，结果到达后选择首条并进入列表，无需再按一次；本次按键不新增索引查询
- **AND** 已切换查询、离开输入框或搜索页、取消、失败或没有结果时，迟到的回调不得接管焦点

#### Scenario: 分页期间索引已更新
- **WHEN** 用户点“更多结果”时，已生成文档的快照已换代
- **THEN** 用当前关键词读取新快照的第一页，丢弃旧页，且不触发生成
- **AND** 同一快照正常分页时附加新条目；加载时焦点仍在“更多结果”上的用户可从新加载的首条继续键盘浏览

#### Scenario: 区分生成与导出
- **WHEN** 用户在工具窗口或 Markdown 导出菜单查看生成项目文档入口
- **THEN** 生成使用平台新建文件图标，导出 Markdown 保留平台导出图标；生成入口在状态区与菜单使用同一图标
- **AND** 打开目录索引或搜索条目使用平台 Markdown 图标，预览继续使用预览图标
