# Safe Output R2.5 Demo 前端设计文档

版本：v0.3
适用范围：0047-0053 R2.5/R3 Demo 前端与展示体验；R2.6 Dashboard starter 前端布局基线
关联 PRD：`docs/project/prd/safe-output-r25-prd.md`
实现校准补充：`docs/project/prd/safe-output-r25-supplemental-prd.md`
目标入口：R2.5 Demo 为 `safe-output-demo/src/main/resources/static/index.html`；R2.6 通用 Dashboard 为 `/safe-output/dashboard/index.html`

## 1. 设计目标

R2.5/R3 前端要把 Demo 从“功能验证控制台”升级为“真实业务系统接入 Safe Output 后的治理工作台”。页面既要能支撑本地验证，也要能在竞赛现场投屏演示时让评委快速理解：

1. Safe Output 不是单点工具，而是可以嵌入业务系统的输出侧脱敏 starter。
2. Response、Log、Manual、Report 四类能力来自同一套规则、策略和统计边界。
3. Demo 展示的是业务动作、接入方式和治理结果，不是孤立 API 按钮。
4. 报告、日志建议和页面展示不保存、不读取、不暴露敏感原文。

一句话定位：

> 一个真实业务系统的敏感数据治理工作台。

## 2. 设计原则

### 2.1 业务系统优先

R2.5 设计阶段默认首屏保留治理 Dashboard，用于集中展示实时聚合、历史报告、统计和风险摘要；R2.6 抽离后，Demo 默认入口调整为“工作台”，顶部“治理 Dashboard”跳转到 dashboard starter 提供的独立页面。业务菜单统一命名为“工作台”，承载总览、客户、订单、支付、工单和账户等业务页面。工作台总览直接承载接入说明内容。页面语言优先使用“客户、订单、工单、支付、账户、风险、治理建议”等业务词汇，底层 API 名称作为辅助信息出现。

### 2.2 投屏可读

竞赛现场通常会使用 1920x1080 或相近比例投屏，页面需要在 3-5 米外仍能辨识主指标、场景名称和状态。关键数字、主按钮和风险状态必须有足够字号与对比度。

### 2.3 可演示闭环

主演示页面应有明确动作，例如查看业务详情、查看 API ignore 明文、运行脱敏实验、导出当前报告。日志场景页不单独触发日志，而是只读展示业务工作台和脱敏实验室产生的真实 Log4j2 聚合结果。

### 2.4 轻量工程

继续使用 Spring Boot 静态资源方式，不引入 React、Vue、Vite、Webpack 或 npm 构建链。保留本地 `Chart.js`。通过拆分 CSS 和原生 JS 模块控制复杂度。

### 2.5 安全边界前置

前端可以展示虚构输入、脱敏结果、预设日志模板、聚合指标、脱敏 evidence 和 API ignore raw 面板中的 mock 明文字段；不得读取或展示原始日志文件、完整原始 response、真实敏感值或可反推出敏感原文的大段上下文。

## 3. 前端架构

### 3.1 静态资源结构

R2.5 Demo 静态资源已从单文件页面拆分为以下结构：

```text
safe-output-demo/src/main/resources/static/
  index.html
  vendor/
    chart.min.js
    fonts/
      fonts.css
  css/
    app.css
  js/
    app.js
    api.js
    views/
      workbench.js
      guide.js
      lab.js
      logs.js
      reports.js
    components/
      charts.js
      formatters.js
```

`index.html` 只保留壳层、导航和脚本引用。`guide.js` 不再作为主导航独立页使用，而是提供接入说明卡片渲染能力，由 `workbench.js` 在 `#workbench` 总览中复用；旧 `#guide` 和 `#workbench/integration` 路由都重定向到 `#workbench`。R2.6 后，通用治理 Dashboard 的静态资源迁移到 `safe-output-dashboard-spring-boot-starter/src/main/resources/safe-output-dashboard/`，Demo 顶部导航只跳转到 `/safe-output/dashboard/index.html`。

### 3.2 模块职责

| 模块 | 职责 |
|---|---|
| `app.js` | 初始化应用、绑定全局导航、处理 hash 路由和旧入口重定向。 |
| `api.js` | 封装所有 Demo HTTP 调用、错误处理和 JSON 解析。 |
| `views/*` | 各页面渲染、事件绑定和局部刷新。 |
| `guide.js` | 接入说明卡片、代码片段高亮和旧入口兼容渲染。 |
| `components/*` | 可复用 UI 片段，目前包含图表封装和耗时格式化。 |
| `app.css` | 颜色、字号、布局、组件、代码高亮和打印样式。 |

### 3.3 路由设计

R2.5 Demo 建议使用 hash 路由，保持静态部署简单：

| 路由 | 页面 | 对应 issue |
|---|---|---|
| `#dashboard` | 治理 Dashboard，包含实时数据 Tab、历史报告 Tab、报告文件中心与单报告视图 | 0051 / 0052 |
| `#workbench` | 工作台总览 | 0047 |
| `#workbench/{customers|orders|payments|tickets|accounts}` | 业务页面 | 0047 |
| `#lab` | 主动脱敏实验室 | 0049 |
| `#logs` | 日志场景与规则建议 | 0050 |

`#dashboard` 是 R2.5 Demo 内置 Dashboard 的历史默认路由。R2.6 抽离后，Demo 当前默认路由为 `#workbench`，通用治理 Dashboard 入口为 `/safe-output/dashboard/index.html`，由 dashboard starter 提供。旧 `#guide` 和 `#workbench/integration` 入口只做兼容跳转到 `#workbench`，主导航中不再出现独立“接入说明”菜单。

R2.6 Dashboard starter 使用独立 hash 路由或等效客户端路由，推荐主路径为：

| 路由 | 页面 |
|---|---|
| `#overview` | 实时概览 |
| `#risk` | 接口风险 |
| `#log-suggestions` | 日志建议 |
| `#reports` | 历史报告、单报告视图和上传报告临时查看 |
| `#lab` | 通用脱敏实验室 |

### 3.4 API 封装

前端页面不应直接散落 `fetch`。当前由 `api.js` 统一封装为通用方法：

```text
SafeOutputApi.get(path)
SafeOutputApi.post(path, body)
```

R2.5 Demo 当前主要调用包括：

```text
GET  /demo/integration-guide
GET  /demo/business/{customers|orders|payments|tickets|accounts}
GET  /demo/business/{domain}/{id}
GET  /demo/business/{domain}/{id}/raw
POST /demo/mask/by-type
POST /demo/mask/object
POST /demo/mask/strong
GET  /demo/logs/scenarios
GET  /demo/report/dashboard
GET  /demo/report/export
GET  /demo/report/files
GET  /demo/report/files/{name}/dashboard
```

接口调用统一经过 `SafeOutputApi`，页面层可以直接使用业务路径，但不得绕过统一错误处理和 JSON 解析。

R2.6 Dashboard starter 前端不得继续调用 `/demo/report/**` 或 `/demo/mask/**` 作为通用治理能力。它必须通过 `SafeOutputDashboardApi` 或等效统一 API 层调用 `{path-prefix}/api/...` POST 接口：

```text
POST /safe-output/dashboard/api/overview
POST /safe-output/dashboard/api/response-risk
POST /safe-output/dashboard/api/log-suggestions
POST /safe-output/dashboard/api/reports/list
POST /safe-output/dashboard/api/reports/view
POST /safe-output/dashboard/api/reports/upload
POST /safe-output/dashboard/api/lab/by-type
POST /safe-output/dashboard/api/lab/object
POST /safe-output/dashboard/api/lab/strong
```

### 3.5 状态管理

不引入复杂 store。使用一个轻量状态对象即可：

```text
AppState.currentRoute
DashboardState.activeTab
DashboardState.selectedReport
LogState.activeScenario
AppState.selectedReportName
```

状态只用于页面交互和局部刷新，不作为报告或统计的权威数据源。报告 JSON 和后端聚合接口仍是权威来源。

## 4. 信息架构

### 4.1 全局导航

R2.5 Demo 导航固定展示当前主路径：

1. 治理 Dashboard
2. 工作台
3. 脱敏实验室
4. 日志场景

“工作台”是一个导航分组，包含总览、客户档案、订单履约、支付核验、工单处理和账户安全。接入说明不再作为一级菜单或独立内页，而是显示在工作台总览。

R2.6 Dashboard starter 导航独立于 Demo 业务工作台，固定展示实时概览、接口风险、日志建议、历史报告和脱敏实验室，不展示客户、订单、支付、工单、账户或小眼睛明文查看入口。

### 4.2 业务工作台

目标：让评委第一眼看到“这是一个已接入 Safe Output 的业务系统”，并且能从同一组工作台菜单理解接入方式。

实际布局：

```text
业务模块页：
左侧：业务列表表格
右侧：业务详情和 API ignore 明文查看

工作台总览：
接入说明卡片网格
```

业务场景覆盖客户、订单、支付、工单和账户五类。每个业务模块页面展示：

- 业务列表字段和刷新按钮。
- 业务详情字段，敏感字段用 `sensitive` 样式标记。
- “查看”按钮调用 `/{id}/raw`，演示 API ignore 明文查看并进入风险统计。
- 模块说明文案解释规则来源和覆盖类型，包括 `MOBILE`、`ID_CARD`、`BANK_CARD`、`EMAIL`、`CHINESE_NAME`、`ADDRESS`、`PASSWORD`、`DEFAULT`。

业务详情由真实 Response 脱敏链路返回，raw 查看由 API ignore 返回明文；前端不手写脱敏结果。

### 4.3 接入方式说明

目标：把“为什么这样接入”讲清楚，而不是做静态长文档。该内容属于工作台总览，路由为 `#workbench`。

核心结构：

```text
业务场景 -> 接入方式 -> 示例接口 -> 字段 -> 规则来源 -> 输出效果
```

工作台总览接入说明当前聚焦业务页面真实用到的字段规则，必须覆盖：

- YAML 配置规则。
- 注解规则。
- 默认规则库。
- 字段级 ignore。
- 接口级 ignore。

Log4j2 `%safeOutputMsg` 和 `SafeOutputMaskService` 主动脱敏分别在“日志场景”和“脱敏实验室”中展示，不再混入工作台接入说明卡片。

每条说明展示业务字段、示例接口、片段来源、规则来源和代码片段。说明项内部不再展示“打开业务页”“触发场景”等跳转入口，避免与工作台主菜单重复。代码片段必须做轻量语法高亮：YAML 高亮配置 key 和脱敏类型，Java 高亮关键字、注解、类型和字符串。

### 4.4 主动脱敏实验室

目标：让接入方手动验证主动脱敏能力、幂等性和单轮耗时。

实际布局：

```text
三列面板：
按类型标签 / 业务对象 / 强文本扫描

每个面板：
输入控件 + 执行按钮 + Round 1 / Round 2 结果
```

必须展示：

- 按类型标签输入值并脱敏。
- Demo 业务对象主动脱敏。
- 非结构化文本强扫描。
- 第一次结果、第二次结果、幂等判断。
- 每轮 `elapsedNanos` 转换后的毫秒耗时。

实验室可以显示用户当前输入用于交互，但报告和统计仍不得保存敏感原文。

### 4.5 日志场景

目标：证明日志脱敏来自真实 Log4j2 `%safeOutputMsg`，并展示规则建议变化。

页面展示预设模板摘要，不读取原始日志文件，不提供日志页专用触发按钮。日志聚合来自业务工作台接口和脱敏实验室接口中的真实 Log4j2 logger。场景包括：

- JSON-like 日志。
- `key=value` 日志。
- regex fallback。

每个模板卡片展示：

- 场景名称。
- 预设日志形态摘要。
- 场景类别说明。
- regex fallback 场景标记“可收集脱敏信息”。

页面展示：

- LOG 脱敏计数。
- 日志规则建议列表。
- YAML 配置片段。
- LOW / MEDIUM / HIGH 置信度说明。

不得展示完整原始 message。

### 4.6 可复用治理 Dashboard 与历史报告布局基线

目标：把 JSON 报告从“文件产物”升级为可浏览、可演示、可打印的治理报告，并区分当前进程实时数据与历史报告快照。本节最初服务于 R2.5 Demo 内置 Dashboard；R2.6 后作为 `safe-output-dashboard-spring-boot-starter` 前端页面的布局基线继续复用。

Dashboard 顶部展示：

- 刷新当前视图按钮。
- 打印历史报告按钮。
- “实时数据”和“历史报告”两个 Tab，或在 R2.6 starter 中拆分为左侧导航下的“实时概览”和“历史报告”页面。

实时数据 Tab 展示：

- 当前进程内存快照。
- 总脱敏次数、覆盖接口、脱敏类型、日志建议。
- 平均耗时、最大耗时、Ignore 接口、失败次数。
- 场景分布图、类型 Top 图、API 脱敏统计、日志规则建议、明文豁免接口和性能异常拆解。

历史报告 Tab 展示：

- 报告文件列表刷新或手动导出按钮；R2.6 starter 第一阶段不要求提供导出按钮，导出能力可以继续留在宿主应用或 Demo 兼容接口。
- 当前报告数量。
- 报告文件名。
- 文件大小。
- 修改时间。
- 选中报告后的单报告可视化详情。
- 上传 JSON 报告临时查看入口；上传结果必须复用单报告可视化模型，不展示 JSON 原文。

读取报告文件必须限制在 `safe-output.report.directory` 内，只允许 JSON 报告快照，拒绝路径穿越和非 JSON 文件。

R2.6 starter 前端复用 Demo 存量 `reports.js` 的布局结构时，必须替换接口层：实时概览使用 `POST /overview`，报告列表使用 `POST /reports/list`，报告查看使用 `POST /reports/view` 且文件名放在请求体，上传查看使用 `POST /reports/upload`。

### 4.7 单报告视图与打印

单报告详情展示：

- 总览指标。
- Response / Log / Manual 场景分布。
- 脱敏类型 Top 排名。
- 高风险接口。
- ignore 风险接口。
- 日志规则建议。
- 性能指标。

打印样式要求：

- 隐藏侧边导航、刷新按钮、导出按钮、Tab 控件和页面交互控件。
- 保留标题、报告文件名、生成时间和关键指标。
- 图表旁提供表格化数据，避免打印时图表不可读。
- 不包含原始敏感值、完整日志或完整 response。

## 5. 视觉风格

### 5.1 风格定位

采用“白底业务后台 + 治理实证卡片”的风格。它应该像真实业务系统的运营工作台，而不是深色大屏或营销页。

关键词：

- 可信。
- 清晰。
- 干净。
- 可扫描。
- 业务系统。
- 合规治理。
- 代码可读。

避免：

- 大面积紫蓝渐变。
- 粒子背景、光球、装饰性动态图。
- 复杂玻璃拟态。
- 过细灰字。
- 只堆图表的大屏模板。
- 深色指挥舱式背景。

### 5.2 色彩 token

建议基线：

```css
:root {
  --bg: #f7f8fb;
  --surface: #ffffff;
  --surface-2: #f1f5f9;
  --surface-3: #e8eef6;
  --line: #d9e1ea;
  --line-soft: #edf1f5;
  --text: #17202a;
  --muted: #687789;
  --blue: #2563eb;
  --teal: #0f9f8f;
  --green: #138a52;
  --amber: #b7791f;
  --red: #c2413a;
  --code-bg: #f8fafc;
}
```

颜色语义固定：

| 颜色 | 语义 |
|---|---|
| `--green` | 已脱敏、已保护、通过。 |
| `--amber` | 待治理、建议、ignore 风险。 |
| `--red` | 高风险、失败、异常。 |
| `--blue` | 主操作、业务链路、接口、说明。 |
| `--teal` | 日志、统计、辅助成功态。 |

### 5.3 字号与投屏规格

按 1920x1080 优先优化：

| 元素 | 推荐字号 |
|---|---|
| 首屏主标题 | 28-36px |
| 关键指标数字 | 32-44px |
| 页面标题 | 24-30px |
| 卡片标题 | 16-20px |
| 表格正文 | 14-16px |
| 辅助说明 | 12-14px |
| 代码/YAML | 13-15px |

不要使用随 viewport 宽度线性缩放的字号。移动端可以通过断点调整布局，但字号不应不可控缩放。

### 5.4 布局密度

桌面投屏首屏必须在不滚动的情况下看到：

- 当前页面标题。
- 主演示区域。
- 至少 4 个关键指标。
- 主操作按钮。
- 当前场景状态。

页面可以有纵向滚动，但主叙事不能藏在第二屏。

### 5.5 组件规范

按钮：

- 主按钮用于触发当前页面核心动作；日志场景页没有触发按钮，只提供场景切换。
- 危险动作使用红色或琥珀色，但 Demo 中尽量避免真正危险动作。
- 按钮文字要短，现场投屏可读，例如“查看”“执行”“导出报告”。

卡片：

- 圆角控制在 6-8px。
- 用于业务对象、日志模板、报告文件和指标，不要把大页面 section 全部包成嵌套卡片。

Badge：

- 同时使用颜色和文字表达状态。
- 风险等级用 `LOW`、`MEDIUM`、`HIGH`、`CRITICAL` 时，要配中文解释或 tooltip。

代码块：

- 仅用于 YAML、接口路径、配置片段。
- 默认限制高度，避免占满投屏。
- 长内容提供展开或复制，但打印时应只保留关键片段。

图表：

- 优先使用横向条形图、排名表、指标卡。
- 少用复杂饼图和细线图。
- 图表颜色必须和风险语义一致。

### 5.6 动效规范

动效只服务演示反馈：

- 详情切换和报告选择应有明确选中态。
- 指标变化时数字可短暂高亮。
- 新报告导出后列表第一行高亮。

避免持续动画、背景粒子和大面积发光效果。动效不能影响图表可读性，也不能成为评审现场的注意力噪声。

## 6. Issue 执行映射

### 6.1 0047 业务工作台与模拟业务域

前端应完成：

- R2.5 历史设计中默认入口为 `#dashboard`；R2.6 抽离后 Demo 默认入口为 `#workbench`，工作台从主导航分组进入。
- 建立业务列表和业务详情双栏布局。
- 展示客户、订单、工单、支付或账户等业务场景。
- 业务详情展示脱敏后的字段，`查看` 按钮调用 raw 接口演示 API ignore 明文查看。
- 工作台总览展示接入说明卡片。

验收重点：

- 首屏像业务系统，不像工具集合。
- 不手写脱敏结果替代真实 Response 脱敏链路。

### 6.2 0048 工作台总览接入说明

前端应完成：

- 接入矩阵作为 `#workbench` 工作台总览内容。
- 每条说明关联业务场景、接入方式、示例接口、字段、规则来源、输出效果。
- 不展示跳转或触发入口，避免说明项和工作台菜单形成重复路径。
- 代码片段提供轻量高亮。
- `#guide` 和 `#workbench/integration` 兼容跳转到 `#workbench`。

验收重点：

- 不把类型标签、字段名和规则来源混淆。
- 页面不是静态长文档。

### 6.3 0049 主动脱敏实验室

前端应完成：

- 类型脱敏、对象脱敏、强文本扫描三类入口。
- 展示第一次结果、第二次结果、幂等判断。
- 展示每轮毫秒耗时。

验收重点：

- 所有主动脱敏都通过 `SafeOutputMaskService` 后端接口。
- 页面和报告不保存敏感原文。

### 6.4 0050 日志场景与规则建议

前端应完成：

- 日志模板卡片。
- 只读展示真实 Log4j2 logger 聚合结果。
- 展示 LOG 统计。
- 展示日志规则建议和 YAML 片段。
- 展示置信度说明。

验收重点：

- 不读取原始日志文件。
- 不展示完整原始 message。
- 不在 controller 手工 seed 建议。
- 不提供日志页专用触发接口。

### 6.5 0051 Dashboard 历史报告与单报告视图

前端应完成：

- R2.5 Demo 内置 Dashboard 保留实时数据和历史报告 Tab 的布局经验。
- R2.6 Dashboard starter 复用该布局经验，但入口迁移到 `/safe-output/dashboard/index.html`，API 迁移到 `{path-prefix}/api/...` POST。
- R2.5 Demo 内置 Dashboard 保留报告导出按钮；R2.6 Dashboard starter 第一阶段可只提供报告列表刷新和上传查看，导出能力由宿主应用或 Demo 兼容接口提供。
- 报告文件列表。
- 单报告可视化页面。
- 报告 dashboard 图表与表格。
- 上传报告临时查看入口，上传结果复用单报告可视化页面。

验收重点：

- JSON 报告是权威数据源。
- 文件读取限制在报告目录。
- 页面不展示敏感原文。

### 6.6 0052 可打印报告与 Demo 打磨

前端应完成：

- 打印入口。
- `@media print` 样式。
- 导航、首屏、页面文案和视觉统一。
- 1920x1080 桌面宽度人工检查。

验收重点：

- 打印版包含总览、场景分布、类型 Top、高风险接口、ignore 风险、日志建议和性能指标。
- 不引入后端 PDF 依赖。

### 6.7 0053 回归与交接

前端应完成：

- 清理旧主路径或过时页面残留。
- 保留兼容入口时说明用途。
- 同步 `.codex-memory` 中 Demo 能力和后续交接信息。

验收重点：

- 新主路径完整可演示。
- 浏览器入口仍为 `http://localhost:8080/index.html`。

## 7. 验收清单

### 7.1 人工演示验收

1. 启动 Demo 后打开 `http://localhost:8080/index.html`，默认进入业务工作台总览；点击顶部“治理 Dashboard”进入 `/safe-output/dashboard/index.html`。
2. 在 1920x1080 视口下，Dashboard 首屏能看到治理摘要、报告操作和主指标；工作台首屏能看到总览卡片。
3. 触发业务接口后，Response 统计或风险摘要有可见变化。
4. 接入说明位于工作台总览，说明项不展示跳转入口，代码片段有高亮。
5. 主动脱敏实验室能展示两次脱敏结果、幂等判断和耗时。
6. 访问业务工作台或运行脱敏实验室后，日志场景页能只读展示 LOG 统计或日志建议。
7. Dashboard 历史报告页面可以列出、打开和临时上传报告；Demo 兼容路径仍可导出报告。
8. 单报告页面可浏览器打印，打印预览无明显遮挡、重叠或敏感原文。

### 7.2 技术验收

1. 不新增前端构建链。
2. 不新增后端 PDF 生成依赖。
3. 不读取原始日志文件。
4. 不展示完整原始日志 message；API ignore 明文查看只在业务详情 raw 面板中演示，并进入风险统计。
5. 前端调用通过统一 API 层组织。
6. 受影响 Demo 集成测试通过。

## 8. 非目标

R2.5 前端不做以下事项：

- 不建设完整权限系统、用户系统或多租户控制台。
- 不实现真实生产数据源。
- 不实现规则建议自动采纳。
- 不引入数据库持久化。
- 不引入服务端 PDF 渲染。
- 不做 R3 日志长度策略页面。
- 不把 Demo 扩展成通用低代码治理平台。

## 9. 后续实现建议

建议执行顺序：

1. 0047 先建立业务工作台和静态 SPA 骨架。
2. 0048 复用业务场景元数据生成接入说明。
3. 0049 独立实现实验室页面和主动脱敏 API。
4. 0050 独立实现日志模板只读聚合和建议展示。
5. 0051 实现 Dashboard 历史报告和单报告视图。
6. 0052 做投屏视觉统一、打印样式和文案收口。
7. 0053 做旧入口清理、人工验收和项目记忆更新。

实现过程中如果改变 Demo 能力、运行入口、测试命令、核心演示路径或交接信息，需要同步更新 `.codex-memory/` 对应文档。
