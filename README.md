# Codex Gateway：开箱即用的 Codex 代理中转站

[![GitHub stars](https://img.shields.io/github/stars/javaeecc/codex?style=flat-square)](https://github.com/javaeecc/codex)
[![Gitee stars](https://gitee.com/baihongyu_1/codex-gateway/badge/star.svg?theme=gv)](https://gitee.com/baihongyu_1/codex-gateway)
[![Java](https://img.shields.io/badge/Java-8%2B-orange?style=flat-square)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-brightgreen?style=flat-square)](https://spring.io/projects/spring-boot)

一个开箱即用的 **Codex 代理中转站 / Codex API 中转服务**，基于 Spring Boot、Spring WebFlux 和 Spring Cloud Gateway 构建。

部署者只需要准备上游服务并启动项目，就可以快速拥有自己的 Codex 代理网站，不需要学习复杂的网关配置。项目已经集成用户端和后台管理端，适合个人部署、团队共享、API 中转和多用户运营。

[GitHub](https://github.com/javaeecc/codex) · [Gitee](https://gitee.com/baihongyu_1/codex-gateway) · [演示站点](https://javaee.cc) · [管理后台](http://admin.javaee.cc)

> 如果这个项目帮助你快速搭建了 Codex 代理中转站，欢迎在 [GitHub](https://github.com/javaeecc/codex) 或 [Gitee](https://gitee.com/baihongyu_1/codex-gateway) 点一个 Star。你的 Star 会帮助更多正在寻找 Codex 代理、Codex 中转和 AI API 中转方案的人发现这个项目。

## 为什么选择这个项目

- **开箱即用**：启动后就是完整的 Codex 代理网站，普通使用者不需要了解 Spring Cloud Gateway 或 API 转发规则。
- **完整用户端**：登录、注册、API Key 管理、Token 使用记录和套餐管理均已集成。
- **完整管理后台**：网站设置、用户管理、用户 API Key 管理、套餐管理和 Token 计费倍率设置均已集成。
- **Codex API 中转**：统一转发 Codex API 请求，方便接入客户端、脚本和第三方工具。
- **多用户运营**：每个用户可以独立管理自己的 API Key、使用量和套餐，适合搭建共享型或商业化代理站。
- **自动初始化**：首次启动自动引导管理员登录并生成平台配置，不需要手工修改复杂配置文件。
- **轻量部署**：支持 Maven、jar 和内置 JRE 的 Windows 发布包，下载后即可启动。

## 已集成功能

### 用户端功能

- 用户注册、登录和验证码验证
- 用户个人信息和账户状态
- API Key 创建、查看、管理和状态控制
- Token 使用记录和用量查询
- 余额、额度和消费信息查看
- 套餐列表、套餐购买或兑换流程
- Codex API 接入信息展示

### 管理后台功能

- 网站标题、关键词、描述、Logo 和客服信息设置
- 管理员登录、管理员注册和平台 Key 管理
- 用户列表、用户状态和用户信息管理
- 管理用户的 API Key，并支持状态控制
- Token 使用记录和用量统计
- 套餐模板和用户套餐管理
- Token 价格倍率 / 计费倍率设置
- 充值套餐、兑换码和账户额度管理
- 管理后台仪表盘和运营数据查看

## 适用场景

- 搭建自己的 Codex 代理网站
- 搭建 Codex API 中转站和 AI API 代理服务
- 给团队成员提供统一的 Codex API 接入地址
- 搭建多用户 API Key 管理和 Token 计费平台
- 搭建带套餐、额度和用量统计的 AI API 运营平台
- 学习 Spring Cloud Gateway API 网关和代理中转项目

## 搜索关键词

Codex 代理、Codex 代理中转站、Codex 中转站、Codex API 中转、Codex API 代理、AI API 中转、AI API 代理、OpenAI 兼容 API 网关、API Key 管理、Token 使用记录、Token 倍率、套餐管理、多用户 API 平台、Spring Cloud Gateway。

## 系统结构

```text
浏览器 / AI 客户端
        │
        ▼
Codex 代理中转站
  ├── 公共首页、登录、注册、初始化页面
  ├── 用户端：API Key、Token 使用记录、套餐管理
  ├── 管理后台：网站、用户、Key、套餐、倍率管理
  ├── /api/**  ───────────────► 上游 API 服务
  ├── /v1/**   ───────────────► 上游 API 服务的 /api/v1/**
  └── /dashboard/** ──────────► 控制台前端服务
```

## 快速开始

### 环境要求

- Java 8 或更高版本
- Maven 3.8+
- 可访问的上游 Codex API 服务

### 方式一：Maven 启动

```bash
mvn spring-boot:run
```

默认访问地址：<http://localhost:88>

### 方式二：打包运行

```bash
mvn clean package
java -jar target/codex-gateway.jar
```

指定端口启动：

```bash
java -jar target/codex-gateway.jar --server.port=80
```

### 方式三：Windows 发布包

如果使用包含 JRE 的发布压缩包，解压后直接运行：

```text
start.bat
```

发布包会优先使用目录内的 JRE，不依赖系统环境变量中的 Java。端口 80 在 Windows 下可能需要使用管理员身份运行。

## 首次初始化

网关会实时读取启动工作目录中的 `invite-code.txt`。

首次启动或该文件被删除时，访问网关会自动跳转到初始化页面：

1. 输入管理员邮箱、密码和验证码。
2. 网关调用上游管理员认证接口获取平台 Key。
3. Key 自动写入当前目录的 `invite-code.txt`。
4. 初始化完成后即可访问首页、注册页面和控制台。

`invite-code.txt` 是敏感配置文件，已经加入 `.gitignore`，不要提交到公开仓库。

## 配置说明

配置文件：`src/main/resources/application.yml`

```yaml
server:
  port: 88

codex:
  gateway:
    upstream-base-url: http://api.javaee.cc
    dashboard-base-url: http://api.javaee.cc
    site:
      invite-code-file: invite-code.txt
```

| 配置项 | 说明 |
| --- | --- |
| `server.port` | 网关监听端口，默认 `88` |
| `codex.gateway.upstream-base-url` | 上游 API 服务地址 |
| `codex.gateway.dashboard-base-url` | 控制台前端服务地址 |
| `codex.gateway.site.invite-code-file` | 平台 Key 文件名，默认是当前工作目录下的 `invite-code.txt` |

## 路由说明

| 路径 | 作用 |
| --- | --- |
| `/` | 公共首页 |
| `/login` | 用户登录页面 |
| `/register` | 用户注册页面 |
| `/initialize` | 首次启动初始化页面 |
| `/api/**` | 转发到上游 API 服务 |
| `/v1/**` | 自动转发到上游 `/api/v1/**` |
| `/dashboard/**` | 转发到控制台前端 |

## 开发与构建

```bash
# 运行测试
mvn test

# 构建可执行 jar
mvn clean package
```

主要技术栈：

- Spring Boot 2.7.x
- Spring Cloud Gateway 2021.x
- Spring WebFlux
- Thymeleaf
- Java 8+

## 安全建议

- 不要把 `invite-code.txt`、管理员密码或生产日志提交到 Git 仓库。
- 生产环境建议使用 HTTPS，并通过反向代理隐藏上游服务地址。
- 端口 80/443 建议由 Nginx、Caddy 或其他反向代理负责监听。
- 部署前请修改默认的上游服务地址和管理后台配置。

## 交流与反馈

- 技术交流群：`747172607`
- GitHub Issues：<https://github.com/javaeecc/codex/issues>
- Gitee Issues：<https://gitee.com/baihongyu_1/codex-gateway/issues>

欢迎提交 Issue、改进文档、补充部署方案或提交 Pull Request。

## English

Codex Gateway is an open-source, ready-to-use **Codex proxy relay station** and **Codex API gateway**. Start the project to deploy your own Codex proxy website without complicated gateway configuration.

It includes user registration and login, API key management, token usage records, package management, website settings, user management, user API key administration and token pricing multipliers.

Useful search terms: **Codex proxy**, **Codex relay**, **Codex API proxy**, **Codex API gateway**, **AI API relay**, **OpenAI compatible API gateway**, **API key management**, **token usage tracking**, **multi-user API platform**.
