# Safe Output Demo 部署流程

更新时间：2026-05-28

本文档记录当前 demo 演示服务器的手工部署步骤，并作为后续自动化部署脚本的唯一流程依据。

## 目标

- 部署模块：`safe-output-demo`
- 部署方式：本地 Maven 打包，上传 Spring Boot 可执行 jar 到远程服务器
- 运行方式：远程 `systemd` 托管 Java 进程
- 访问入口：`http://122.51.95.83:8080/index.html`
- Dashboard 入口：`http://122.51.95.83:8080/safe-output/dashboard/index.html`

## 服务器信息

```text
DEPLOY_HOST=122.51.95.83
DEPLOY_USER=ubuntu
DEPLOY_PORT=22
SSH_KEY=~/.ssh/safeoutput_demo_ed25519
APP_NAME=safe-output-demo
APP_PORT=8080
REMOTE_APP_DIR=/opt/safe-output-demo
REMOTE_RELEASE_DIR=/opt/safe-output-demo/releases
REMOTE_CURRENT_JAR=/opt/safe-output-demo/safe-output-demo.jar
REMOTE_LOG_DIR=/var/log/safe-output-demo
REMOTE_REPORT_DIR=/opt/safe-output-demo/reports
SERVICE_NAME=safe-output-demo
```

免密登录验证：

```bash
ssh -i ~/.ssh/safeoutput_demo_ed25519 -o IdentitiesOnly=yes ubuntu@122.51.95.83 'hostname && whoami'
```

## 前置条件

本地：

- 已安装 JDK 8 或兼容本项目编译要求的 JDK。
- 已安装 Maven。
- 已配置 `~/.ssh/safeoutput_demo_ed25519`，并可免密登录远程 `ubuntu` 用户。

远程：

- Ubuntu 服务器。
- 安全组已放行 `TCP 8080`。
- 需要安装 Java 8 运行时。
- `ubuntu` 用户需要具备 `sudo` 权限，用于创建 `/opt`、`/var/log` 和 `systemd` 服务。
- 2026-05-28 部署结果：已安装 Eclipse Temurin 8 JRE，`java -version` 为 `openjdk version "1.8.0_492"`。

## 本地打包

在项目根目录执行：

```bash
cd safe-output
mvn -pl safe-output-demo -am -DskipTests package
```

产物位置：

```text
safe-output/safe-output-demo/target/safe-output-demo-*.jar
```

自动化脚本应排除 `original-*.jar`，只上传 Spring Boot repackage 后的可执行 jar。

## 远程初始化

首次部署前执行一次：

```bash
ssh -i ~/.ssh/safeoutput_demo_ed25519 -o IdentitiesOnly=yes ubuntu@122.51.95.83 '
  set -e
  sudo mkdir -p /opt/safe-output-demo/releases
  sudo mkdir -p /opt/safe-output-demo/reports
  sudo mkdir -p /var/log/safe-output-demo
  sudo chown -R ubuntu:ubuntu /opt/safe-output-demo /var/log/safe-output-demo
'
```

确认 Java：

```bash
ssh -i ~/.ssh/safeoutput_demo_ed25519 -o IdentitiesOnly=yes ubuntu@122.51.95.83 'java -version'
```

如果远程没有 Java 8，优先安装 Eclipse Temurin 8 JRE。安装步骤按 Adoptium 官方 Ubuntu/Debian apt 仓库说明整理：

```bash
ssh -i ~/.ssh/safeoutput_demo_ed25519 -o IdentitiesOnly=yes ubuntu@122.51.95.83 '
  set -e
  sudo apt update
  sudo apt install -y wget apt-transport-https gpg
  wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor | sudo tee /etc/apt/trusted.gpg.d/adoptium.gpg >/dev/null
  echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '\''/^VERSION_CODENAME/{print$2}'\'' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
  sudo apt update
  sudo apt install -y temurin-8-jre
  java -version
'
```

自动化脚本在安装前应检查 `java -version`，避免重复安装或误覆盖已有运行环境。

## 上传发布包

建议每次发布使用时间戳命名，保留可回滚版本：

```bash
RELEASE_ID=$(date +%Y%m%d%H%M%S)
LOCAL_JAR=$(find safe-output/safe-output-demo/target -maxdepth 1 -name "safe-output-demo-*.jar" ! -name "original-*.jar" | head -n 1)

scp -i ~/.ssh/safeoutput_demo_ed25519 "$LOCAL_JAR" \
  ubuntu@122.51.95.83:/opt/safe-output-demo/releases/safe-output-demo-${RELEASE_ID}.jar

ssh -i ~/.ssh/safeoutput_demo_ed25519 -o IdentitiesOnly=yes ubuntu@122.51.95.83 "
  ln -sfn /opt/safe-output-demo/releases/safe-output-demo-${RELEASE_ID}.jar /opt/safe-output-demo/safe-output-demo.jar
"
```

## systemd 服务

首次部署时创建服务文件：

```bash
ssh -i ~/.ssh/safeoutput_demo_ed25519 -o IdentitiesOnly=yes ubuntu@122.51.95.83 'sudo tee /etc/systemd/system/safe-output-demo.service >/dev/null' <<'EOF'
[Unit]
Description=Safe Output Demo
After=network.target

[Service]
User=ubuntu
WorkingDirectory=/opt/safe-output-demo
ExecStart=/usr/bin/java -Xms256m -Xmx768m -jar /opt/safe-output-demo/safe-output-demo.jar --server.port=8080 --safe-output.report.directory=/opt/safe-output-demo/reports
Restart=always
RestartSec=5
SuccessExitStatus=143
StandardOutput=append:/var/log/safe-output-demo/app.log
StandardError=append:/var/log/safe-output-demo/app.log

[Install]
WantedBy=multi-user.target
EOF
```

加载并启动：

```bash
ssh -i ~/.ssh/safeoutput_demo_ed25519 -o IdentitiesOnly=yes ubuntu@122.51.95.83 '
  sudo systemctl daemon-reload
  sudo systemctl enable safe-output-demo
  sudo systemctl restart safe-output-demo
  sudo systemctl status safe-output-demo --no-pager
'
```

后续发布只需要上传新 jar、更新软链接、重启服务。

## 健康检查

服务状态：

```bash
ssh -i ~/.ssh/safeoutput_demo_ed25519 -o IdentitiesOnly=yes ubuntu@122.51.95.83 \
  'systemctl is-active safe-output-demo && tail -n 80 /var/log/safe-output-demo/app.log'
```

HTTP 入口：

```bash
curl -I http://122.51.95.83:8080/index.html
curl -I http://122.51.95.83:8080/safe-output/dashboard/index.html
```

预期：

- `systemctl is-active` 返回 `active`。
- `index.html` 返回 `HTTP/1.1 200`。
- Dashboard 静态入口返回 `HTTP/1.1 200`。

## 回滚

查看远程历史发布包：

```bash
ssh -i ~/.ssh/safeoutput_demo_ed25519 -o IdentitiesOnly=yes ubuntu@122.51.95.83 \
  'ls -1t /opt/safe-output-demo/releases/*.jar'
```

切换到指定版本并重启：

```bash
ssh -i ~/.ssh/safeoutput_demo_ed25519 -o IdentitiesOnly=yes ubuntu@122.51.95.83 '
  ln -sfn /opt/safe-output-demo/releases/<target-release>.jar /opt/safe-output-demo/safe-output-demo.jar
  sudo systemctl restart safe-output-demo
  systemctl is-active safe-output-demo
'
```

## 自动化部署约束

- 自动化脚本必须先运行本地 Maven 打包，再上传 jar。
- 自动化脚本不得上传源码、报告敏感样本或本地临时文件。
- 自动化脚本不得把 SSH 密码、私钥内容写入项目文件。
- 自动化脚本应使用 `BatchMode=yes` 验证免密登录，避免部署过程中卡在密码输入。
- 自动化脚本应保留最近若干个 `releases/*.jar`，不要直接覆盖唯一 jar。
- 自动化脚本重启服务后必须执行 `systemctl is-active` 和 HTTP 健康检查。
- 如果健康检查失败，自动化脚本应输出 `systemctl status` 和最近日志，并保留失败版本用于排查。

## 当前部署状态

2026-05-28 已完成首次远程部署：

```text
release: /opt/safe-output-demo/releases/safe-output-demo-20260528162429.jar
current: /opt/safe-output-demo/safe-output-demo.jar -> releases/safe-output-demo-20260528162429.jar
service: safe-output-demo active
java: openjdk version "1.8.0_492"
local health: http://127.0.0.1:8080/index.html HTTP 200
local dashboard health: http://127.0.0.1:8080/safe-output/dashboard/index.html HTTP 200
public health: http://122.51.95.83:8080/index.html HTTP 200
public dashboard health: http://122.51.95.83:8080/safe-output/dashboard/index.html HTTP 200
```

服务端检查显示 Java 已监听 `*:8080`，`ufw` 为 inactive，`iptables` 默认 `INPUT ACCEPT`。首次公网访问超时已通过云厂商控制台放行 `TCP 8080` 解决。
