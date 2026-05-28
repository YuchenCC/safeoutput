# 部署登录记录

更新时间：2026-05-28

## 服务器

- 公网 IP：122.51.95.83
- SSH 用户：ubuntu
- SSH 端口：22
- 主机名：VM-0-2-ubuntu
- 系统内核：Linux 6.8.0-101-generic x86_64
- 系统类型：Ubuntu

## 首次登录验证

本地已完成首次 SSH 登录，并已将服务器主机指纹加入当前用户的 `~/.ssh/known_hosts`。

验证命令形式：

```bash
ssh ubuntu@122.51.95.83
```

登录后验证结果：

```text
hostname: VM-0-2-ubuntu
whoami: ubuntu
uname: Linux VM-0-2-ubuntu 6.8.0-101-generic #101-Ubuntu SMP PREEMPT_DYNAMIC Mon Feb 9 10:15:05 UTC 2026 x86_64
```

## SSH key 登录

已为本地机器生成并配置专用部署 key：

```text
私钥路径：~/.ssh/safeoutput_demo_ed25519
公钥路径：~/.ssh/safeoutput_demo_ed25519.pub
远程用户：ubuntu
远程 authorized_keys：/home/ubuntu/.ssh/authorized_keys
```

免密登录验证命令：

```bash
ssh -i ~/.ssh/safeoutput_demo_ed25519 -o IdentitiesOnly=yes ubuntu@122.51.95.83
```

已使用 `BatchMode=yes` 验证 key 登录成功，返回：

```text
key-login-ok
VM-0-2-ubuntu
ubuntu
```

## 安全说明

- 本文档不记录 SSH 密码。
- 后续建议关闭或限制密码登录。
- demo 部署前需要继续确认 Java 8、Maven 或上传 jar 的运行环境。
