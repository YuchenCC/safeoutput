# safe-output-demo

`safe-output-demo` 是 Safe Output 的 Spring Boot 2.x 示例应用，用于端到端验证 response 脱敏、Log4j2 日志脱敏和报告导出。

## 职责

- 演示业务侧只引用 `safe-output-spring-boot-starter` 的接入方式。
- 提供 Bean、Map、List、嵌套对象和 API Ignore 响应脱敏样例。
- 通过业务工作台和脱敏实验室接口产生真实 Log4j2 日志，由 `%safeOutputMsg` 演示
  JSON-like、key-value 与 regex fallback 脱敏。
- 提供主动脱敏验证接口，覆盖指定 type、对象规则和强扫描；实验室接口固定执行两轮并返回逐轮结果数组。
- 启用报告模块，并提供快照查看和手动导出接口。
- 提供 SPA 控制台 `index.html`，包含 Dashboard、风险画像、规则发现、脱敏实验室和接入指南页面。

## 运行

在 `safe-output/` 根目录执行：

```sh
mvn -pl safe-output-demo -am spring-boot:run
```

启动后访问：

```text
http://localhost:8080/index.html
```

## 示例接口

- `GET /demo/bean`: Bean 字段脱敏。
- `GET /demo/map`: Map key 脱敏。
- `GET /demo/list`: List 元素脱敏。
- `GET /demo/nested`: 嵌套对象和银行卡字段脱敏。
- `GET /demo/ignored`: API Ignore 示例，返回明文但记录风险统计。
- `GET /demo/logs/scenarios`: 查看日志脱敏方式、当前 LOG 脱敏计数、规则建议和 YAML 片段。
- `GET /demo/report/snapshot`: 查看内存聚合快照。
- `GET /demo/report/export`: 手动导出本地 JSON 报告。
- `GET /demo/report/response-risk`: 查看 R2 Response 风险画像、性能画像和 ignored 接口。
- `GET /demo/report/log-suggestions`: 查看 R2 Log 规则建议和候选 YAML 配置片段；先操作业务工作台或脱敏实验室可产生真实 fallback 线索。
- `GET /demo/report/dashboard`: 聚合统计接口，返回脱敏总量、场景分布、高风险接口数、配置建议数、类型分布和场景趋势，供前端 Dashboard 使用。
- `POST /demo/mask/by-type`: 指定类型标签主动脱敏，Demo 包含自定义 `mobileM` 策略；固定返回两轮 `{round,result,elapsedNanos,sameAsPrevious}` 结果数组。
- `POST /demo/mask/object`: 按对象规则主动脱敏，支持传入 `realName`、`mobile`、`name` 表单字段，演示姓名/手机号命中且商品 `name` 不误脱敏；固定返回两轮结果数组。
- `POST /demo/mask/strong`: 对文本执行主动强扫描脱敏；固定返回两轮结果数组。

## 配置

示例配置位于 `src/main/resources/application.yml`：

- `safe-output.ignore.keys`: 演示字段级 Ignore。
- `safe-output.ignore.apis`: 演示接口级 Ignore。
- `safe-output.report`: 启用报告快照并输出到 `target/safe-output-demo-reports`。

Log4j2 pattern 位于 `src/main/resources/log4j2.xml`，使用 `%safeOutputMsg` 执行日志脱敏。

## 本模块验证

在 `safe-output/` 根目录执行：

```sh
mvn -pl safe-output-demo -am test
```

测试覆盖 response、日志、报告、风险画像、规则建议和主动脱敏的端到端行为。

## 前端控制台

`src/main/resources/static/index.html` 是基于 Chart.js 的单页应用，采用白底业务后台风格：

| 视图 | 路由 | 说明 |
|------|------|------|
| Dashboard | `#dashboard` | 实时风险摘要、场景/类型图表、报告导出、报告文件列表和单报告明细 |
| 工作台 | `#workbench` | 接入配置说明，以及客户、订单、支付、工单、账户业务菜单 |
| 脱敏实验室 | `#lab` | 三种主动脱敏交互验证（By-Type / Object / Strong），固定展示两轮结果、单轮耗时和二次脱敏稳定性 |
| 日志场景 | `#logs` | 只读展示日志脱敏计数、三类日志脱敏方式、漏脱敏补充提醒和 YAML 配置建议 |

Chart.js 库位于 `src/main/resources/static/vendor/chart.min.js`。
