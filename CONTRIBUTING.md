# 贡献指南 · Contributing

感谢关注 **Qualitest Helper**（IntelliJ 插件）。

| 仓库 | 用途 |
|------|------|
| [`qualitest`](https://github.com/qualitest-hq/qualitest) | 主平台 + Web |
| [`qualitest-demo`](https://github.com/qualitest-hq/qualitest-demo) | 接口靶场 / 推荐联调工程 |
| [`qualitest-intellij-plugin`](https://github.com/qualitest-hq/qualitest-intellij-plugin)（本仓） | IDEA 插件 |

参与本社区即表示同意 [`CODE_OF_CONDUCT.md`](./CODE_OF_CONDUCT.md)。  
安全漏洞请走 [`SECURITY.md`](./SECURITY.md)，不要开公开 Issue。

## 欢迎什么

扫描漏接口、上传体验、文案笔误、IDE 兼容问题等，开 Issue 或 PR 均可。半成品想法先 Issue 聊聊也行。

## 开发摘要

- 构建：`./gradlew buildPlugin`（产物在 `build/distributions/`）  
- 目标 IDE 与版本见 README / `gradle.properties`  
- 请勿提交本地绝对路径配置、密钥、`build/` 等产物

大改动建议先 Issue。维护者业余时间处理，**不承诺固定 SLA**。

本仓以 **[Apache License 2.0](./LICENSE)** 发布；贡献授权约定与主仓 [CONTRIBUTING](https://github.com/qualitest-hq/qualitest/blob/main/CONTRIBUTING.md) 相同。
