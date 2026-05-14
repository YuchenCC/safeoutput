# safe-output-demo

`safe-output-demo` 是 Safe Output 的 Spring Boot 2.x 示例应用，用于端到端验证 response 脱敏、Log4j2 日志脱敏和报告导出。

## 职责

- 演示业务侧只引用 `safe-output-spring-boot-starter` 的接入方式。
- 提供 Bean、Map、List、嵌套对象和 API Ignore 响应脱敏样例。
- 通过 Log4j2 `%safeOutputMsg` 演示日志 key-value 与 regex fallback 脱敏。
- 提供主动脱敏验证接口，覆盖指定 type、对象规则和强扫描。
- 启用报告模块，并提供快照查看和手动导出接口。
- 提供静态页面 `safe-output-playground.html`，方便浏览器手动验证。

## 运行

在 `safe-output/` 根目录执行：

```sh
mvn -pl safe-output-demo -am spring-boot:run
```

启动后访问：

```text
http://localhost:8080/safe-output-playground.html
```

## 示例接口

- `GET /demo/bean`: Bean 字段脱敏。
- `GET /demo/map`: Map key 脱敏。
- `GET /demo/list`: List 元素脱敏。
- `GET /demo/nested`: 嵌套对象和银行卡字段脱敏。
- `GET /demo/ignored`: API Ignore 示例，返回明文但记录风险统计。
- `GET /demo/logs`: 输出一条带敏感值的日志。
- `GET /demo/report/snapshot`: 查看内存聚合快照。
- `GET /demo/report/export`: 手动导出本地 JSON 报告。
- `GET /demo/report/response-risk`: 查看 R2 Response 风险画像、性能画像和 ignored 接口。
- `GET /demo/report/log-suggestions`: 查看 R2 Log 规则建议和候选 YAML 配置片段。
- `POST /demo/mask/by-type`: 指定类型标签主动脱敏，Demo 包含自定义 `mobileM` 策略。
- `POST /demo/mask/object`: 按对象规则主动脱敏，演示 `realName`、`mobile` 命中且商品 `name` 不误脱敏。
- `POST /demo/mask/strong`: 对文本执行主动强扫描脱敏。

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

测试覆盖 response、日志和报告的端到端行为。
