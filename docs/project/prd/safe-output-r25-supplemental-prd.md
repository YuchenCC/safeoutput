# Safe Output R2.5 Demo 实现校准补充 PRD

版本：v0.1 / R2.5 supplement
关联文档：`docs/project/prd/safe-output-r25-prd.md`、`docs/project/design/safe-output-r25-frontend-design.md`
适用范围：`safe-output-demo` 当前实际实现与 R2.5 文档口径校准

---

## 1. 文档目标

本文档记录 R2.5 Demo 在实现完成后的最终展示口径。若本补充 PRD 与 R2.5 原 PRD 或前端设计文档存在差异，以当前 `safe-output-demo` 实际编码和本文档为准。

本补充 PRD 不新增编码需求，只用于：

1. 固化当前 Demo 的真实页面结构、菜单、功能调用和展示边界。
2. 解释原 R2.5 设想中未按原形态落地的部分。
3. 避免后续开发把已决策的实现收敛误判为缺陷或遗漏。

---

## 2. 最终实现口径

### 2.1 前端主题

R2.5/R3 Demo 最终采用白底业务后台风格：

- 浅色侧边栏。
- 白色面板。
- 细边框。
- 蓝、青、绿为主的语义色。
- 浅底代码块。

后续 UI polish 应延续该风格，不恢复深色驾驶舱、大屏模板、粒子背景、光球或大面积紫蓝渐变。

### 2.2 布局与菜单

默认入口为 `#dashboard`，浏览器入口仍是：

```text
http://localhost:8080/index.html
```

主导航实际包含：

1. 治理 Dashboard。
2. 工作台分组。
3. 脱敏实验室。
4. 日志场景。

工作台分组实际包含：

- 总览：`#workbench`
- 客户档案：`#workbench/customers`
- 订单履约：`#workbench/orders`
- 支付核验：`#workbench/payments`
- 工单处理：`#workbench/tickets`
- 账户安全：`#workbench/accounts`

旧入口兼容策略：

- `#guide` 跳转到 `#workbench`。
- `#workbench/integration` 跳转到 `#workbench`。

接入说明不再是独立一级菜单，也不再是独立工作台内页；它是工作台总览内容。

### 2.3 工作台功能调用

业务工作台通过真实业务接口展示 Response 自动脱敏：

```text
GET /demo/business/{customers|orders|payments|tickets|accounts}
GET /demo/business/{domain}/{id}
GET /demo/business/{domain}/{id}/raw
```

列表和详情接口走正常 Response 脱敏链路。`/{id}/raw` 接口走 API ignore，用于“小眼睛查看明文”演示；该接口可以返回明文，但必须进入 Response 风险统计。

工作台页面不手写脱敏结果，不通过前端替换模拟脱敏效果。

### 2.4 接入说明展示

工作台总览读取：

```text
GET /demo/integration-guide
```

接入说明实际覆盖：

- 默认字段规则。
- YAML 配置规则。
- 字段注解。
- 字段级 ignore。
- API ignore。

Log4j2 `%safeOutputMsg` 不混入工作台接入说明卡片，它通过日志场景页展示聚合结果。`SafeOutputMaskService` 不混入工作台接入说明卡片，它通过脱敏实验室展示调用效果。

### 2.5 脱敏实验室

脱敏实验室实际提供三类入口：

```text
POST /demo/mask/by-type
POST /demo/mask/object
POST /demo/mask/strong
```

三类接口均固定连续执行两轮，不接收前端 `iterations` 输入。响应结构为数组，每轮包含：

```text
round
result
elapsedNanos
sameAsPrevious
```

前端将 `elapsedNanos` 转成毫秒展示，用于说明首次脱敏、二次脱敏稳定性和单轮耗时。

### 2.6 日志场景

日志场景页读取：

```text
GET /demo/logs/scenarios
```

该页面是只读聚合视图，不提供日志页专用触发按钮，也不提供 `/demo/logs/scenarios/{id}/trigger` 之类接口。

真实日志采集来源于：

- 业务工作台接口。
- 脱敏实验室接口。

日志页展示三类场景：

- JSON-like。
- key=value。
- regex fallback。

regex fallback 场景展示漏脱敏补充提醒、日志规则建议和 YAML 配置候选。候选规则默认关闭，需要人工复核后采纳。

日志页不得读取原始日志文件，不得展示完整原始 message。

### 2.7 治理 Dashboard 与报告

治理 Dashboard 读取当前进程实时聚合：

```text
GET /demo/report/dashboard
```

历史报告能力收敛在 Dashboard 的“历史报告” Tab 中：

```text
GET /demo/report/export
GET /demo/report/files
GET /demo/report/files/{name}/dashboard
```

页面区分：

- 实时数据：当前进程内存聚合快照。
- 历史报告：已导出的 JSON 报告文件快照。

页面不直接展示 JSON 原文。实时和历史都通过指标、表格、图表、风险接口、Ignore 风险、日志规则建议和性能拆解展示。

报告读取必须限制在 `safe-output.report.directory` 内，只允许读取合法报告文件，不允许路径穿越或任意文件读取。

---

## 3. 与 R2.5 原始设想的差异说明

### 3.1 独立接入说明页被工作台总览替代

原始设想中的 `#workbench/integration` 独立页已收敛为 `#workbench` 总览。原因是当前导航更强调业务系统后台形态，减少说明页和业务菜单的重复路径。

### 3.2 日志触发按钮被真实业务日志聚合替代

原始设想中的日志场景触发按钮已取消。当前实现要求日志建议来自真实业务工作台和脱敏实验室产生的 Log4j2 输出，避免在 Demo controller 中手工 seed 日志建议。

### 3.3 报告中心被 Dashboard 历史报告 Tab 替代

原始设想中的独立报告中心已整合进治理 Dashboard。当前页面用实时数据和历史报告两个 Tab 同时承载当前进程快照和报告文件快照，减少一级菜单数量。

### 3.4 批量性能实验被固定两轮耗时展示替代

原始设想中的批量性能测试未作为独立前端能力保留。当前脱敏实验室固定执行两轮，展示每轮耗时和二次脱敏稳定性，重点服务演示闭环和幂等验证。

### 3.5 语义化 API 封装被通用 API 封装替代

原始设计中的工作台、日志触发等语义化 `SafeApi` 方法未落地。当前前端统一使用：

```text
SafeOutputApi.get(path)
SafeOutputApi.post(path, body)
```

该封装仍满足统一错误处理和 JSON 解析要求。

---

## 4. 验收口径

R2.5 Demo 文档和实现以以下验收口径为准：

1. 打开 `http://localhost:8080/index.html` 默认进入治理 Dashboard。
2. Dashboard 能展示实时数据和历史报告两个 Tab。
3. 工作台总览能展示接入说明卡片。
4. 工作台五个业务域能展示列表、详情和 API ignore 明文查看。
5. 脱敏实验室三类入口都能展示两轮结果、稳定性和毫秒耗时。
6. 日志场景页能只读展示 LOG 脱敏计数、三类日志场景、规则建议和 YAML 候选片段。
7. 历史报告能导出、列出、选择并渲染单报告 dashboard。
8. 页面、报告和日志建议不得保存或展示敏感原文、完整日志 message 或完整 response。

---

## 5. 后续约束

- 后续若要恢复日志页触发按钮，必须重新确认它不会绕过真实 Log4j2 `%safeOutputMsg`，且不得手工 seed 建议。
- 后续若要恢复独立接入说明页，应保留 `#workbench` 兼容入口，并避免和工作台菜单重复。
- 后续若要引入批量性能测试，应明确其统计边界，不把用户输入或敏感原文写入报告。
- 后续若改动页面路由、Demo 能力、运行入口或交接口径，需要同步更新 `.codex-memory/` 对应文档。
