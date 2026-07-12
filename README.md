# codex-gateway

这是 Codex 平台的网关项目。完成配置后，可以部署并使用自己的平台。

## 使用说明

### 1. 启动平台

确认上游 API 服务已经启动后，执行：

```bash
mvn spring-boot:run
```

网关默认运行在 `http://localhost:88`。也可以先打包，再运行 jar 文件：

```bash
mvn clean package
java -jar target/*.jar
```

首次启动时，如果启动工作目录没有 `invite-code.txt`，访问网关会自动进入初始化页面。登录管理员账号后，网关会从上游获取该账号的 key 并将它写入当前目录的 `invite-code.txt`，随后即可正常访问首页和注册页面。

## 配置说明

- `site.invite-code-file`：平台 key 文件名，默认是启动工作目录下的 `invite-code.txt`。

## 技术栈

- Spring Boot 2.7.x
- Spring Cloud Gateway
- Vue 2.x

技术QQ群：747172607

演示站点：
用户前台：https://javaee.cc
管理员后台：http://admin.javaee.cc

