# Domain Docs

This repo uses a single-context domain layout.

## Context

- Root context: `CONTEXT.md`
- Context map: none

Agents should read `CONTEXT.md` before creating issues, changing requirements, or implementing behavior that touches Safe Output domain terms.

## ADRs

No ADR directory exists yet. Create `docs/adr/` lazily only when a decision is hard to reverse, surprising without context, and the result of a real trade-off.

## Current Domain Terms

Use the language from `CONTEXT.md`, especially:

- 输出侧脱敏
- MaskType
- MaskScene
- Rule
- Ignore
- 歧义字段
- Regex fallback
- 接口风险统计
- 报告快照
