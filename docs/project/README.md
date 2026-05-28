# Safe Output 项目文档入口

本目录保存项目级说明、需求和模块深挖文档。随组件源码发布给接入方的手册位于 `safe-output/doc/`。

| 路径 | 定位 |
|---|---|
| [safe-output-project-overview.md](safe-output-project-overview.md) | 当前代码结构、模块职责、调用链和设计边界总览。 |
| [safe-output-report-module-guide.md](safe-output-report-module-guide.md) | Report 模块数据来源、内存聚合、JSON 导出和 Demo 报告接口说明。 |
| [deployment-login-record.md](deployment-login-record.md) | Demo 服务器 SSH 首登、免密 key 路径和登录验证记录，不包含密码或私钥内容。 |
| [demo-deployment-runbook.md](demo-deployment-runbook.md) | Demo 部署流程基准文档，后续自动化部署应按该文档步骤实现。 |
| [prd/](prd/) | MVP、R2、R3 需求说明、需求变更记录和澄清记录。 |
| [java_data_desensitization_mvp_scope_and_wbs_v0.3.md](java_data_desensitization_mvp_scope_and_wbs_v0.3.md) | 早期 MVP 范围和 WBS 原始资料，作为历史参考。 |

接入方优先阅读 [safe-output/doc/component-integration-guide.md](../../safe-output/doc/component-integration-guide.md)；维护者优先阅读本目录的项目总览，再按模块跳转到具体文档。
