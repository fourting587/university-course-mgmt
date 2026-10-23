# 部署指南

## 0. 项目形态

项目现在有两种部署形态：

| 形态 | 说明 | 端口 |
|------|------|------|
| **前后端分离版（推荐）** | `frontend/` 静态页面（原生 JS）+ `/api/**` JSON 接口 | 前端 8080 / 后端 1001 |
| Thymeleaf 单体版（保留兼容） | 页面随 JAR 一起打包、服务端渲染 | 1001 |

前后端分离的目录、接口清单和本地开发方式见 [FRONTEND.md](FRONTEND.md)。  
下面第 2 节讲单体 JAR 部署，第 3 节讲 Compose 一键部署（会同时起前端和后端）。

---

## 1. 环境要求

| 组件 | 版本 |
|------|------|
| JDK | 17（必须，不能用 8 或 21+ 编译） |
| MySQL | 8.0 |
| Docker | 可选，方式二需要 |

---

## 2. 方式一：JAR 部署（最简单）

### 2.1 构建

```bash
mvn clean package -DskipTests
# 产物：target/course-management-system-1.0.0.jar
```

> 必须用 JDK 17 构建。IDE 如果配了更高版本 JDK，先把 IDE 的 JDK 固定到 17，否则会打出 class version 69 的包。

### 2.2 准备数据库

```sql
CREATE DATABASE IF NOT EXISTS course_mgmt
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后依次执行：

1. `src/main/resources/db/schema-mysql.sql`（建表 + 外键）
2. `src/main/resources/db/data-mysql.sql`（240 人演示数据，只需一次）

旧库升级补外键：执行一次 `src/main/resources/db/migration-fk.sql`。

### 2.3 运行

```bash
java -jar target/course-management-system-1.0.0.jar \
  --server.port=1001 \
  --spring.datasource.url="jdbc:mysql://127.0.0.1:3306/course_mgmt?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false" \
  --spring.datasource.username=root \
  --spring.datasource.password=你的密码
```

访问 `http://服务器IP:1001`。

### 2.4 后台常驻（Linux systemd）

`/etc/systemd/system/cms.service`：

```ini
[Unit]
Description=University Course Management System
After=network.target mysql.service

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/cms
Environment=JAVA_OPTS=-Xms256m -Xmx512m
Environment=SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/course_mgmt?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
Environment=SPRING_DATASOURCE_USERNAME=cms
Environment=SPRING_DATASOURCE_PASSWORD=换成强密码
ExecStart=/usr/bin/java -jar /opt/cms/course-management-system-1.0.0.jar --server.port=1001
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now cms
sudo systemctl status cms
```

Windows 服务器可以用 `nssm` 把 JAR 注册成服务，或直接 `start /b java -jar ...`。

---

## 3. 方式二：Docker Compose（推荐）

仓库已自带 `Dockerfile`、`frontend/Dockerfile` 和 `docker-compose.yml`，一条命令起 **前端 + 后端 + MySQL**：

```bash
docker compose up -d --build
```

服务与端口：

| 服务 | 容器 | 端口 |
|------|------|------|
| 前端（Nginx 静态页 + /api 反代） | `cms-web` | **8080** |
| 后端 Spring Boot | `cms-app` | 1001 |
| MySQL 8.0 | `cms-mysql` | 仅容器网络 |

首次启动会自动：

1. 创建 `course_mgmt` 数据库
2. 执行 `schema-mysql.sql` 建表
3. 执行 `data-mysql.sql` 导入演示数据
4. 启动应用并等待 MySQL 健康检查通过
5. 启动前端，`/api` 自动反代到后端

访问 `http://服务器IP:8080`（前后端分离版），或 `http://服务器IP:1001`（Thymeleaf 旧版）。

常用命令：

```bash
docker compose logs -f app      # 看应用日志
docker compose ps               # 查看状态
docker compose down             # 停止（保留数据）
docker compose down -v          # 停止并删除数据卷（会清空数据库）
docker compose up -d --build    # 改代码后重新构建
```

> ⚠️ `/docker-entrypoint-initdb.d` 里的脚本**只在数据卷为空时执行一次**。  
> 如果改了 SQL 想重新初始化，先 `docker compose down -v` 再启动。

数据持久化在 `mysql-data` 卷里，删容器不会丢数据。

---

## 4. 方式三：Nginx 反向代理 + HTTPS

应用监听 1001，前面挂 Nginx：

```nginx
server {
    listen 80;
    server_name cms.example.com;

    location / {
        proxy_pass http://127.0.0.1:1001;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

申请证书：

```bash
sudo certbot --nginx -d cms.example.com
```

> 应用里改密、登录都走 Session，反向代理不需要额外配置 WebSocket。

---

## 5. 配置项（环境变量）

Spring Boot 会自动读取环境变量覆盖 `application.yml`：

| 环境变量 | 说明 | 默认 |
|----------|------|------|
| `SERVER_PORT` | 应用端口 | 1001 |
| `SPRING_DATASOURCE_URL` | JDBC 连接串 | localhost:3306/course_mgmt |
| `SPRING_DATASOURCE_USERNAME` | 数据库账号 | root |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 | 123456 |
| `SPRING_PROFILES_ACTIVE` | 激活配置 | mysql |
| `SPRING_SQL_INIT_MODE` | 启动时是否执行建表脚本 | always |
| `JAVA_OPTS` | JVM 参数（Docker 镜像使用） | -Xms256m -Xmx512m |

---

## 6. 上线检查清单

- [ ] **改数据库密码**，不要用默认 `123456`；建议建独立账号，不要用 root 跑应用
- [ ] **首次登录后修改 admin 密码**（系统会强制要求）
- [ ] **关闭 SQL 日志**：生产环境把 `mybatis.configuration.log-impl` 去掉，`logging.level` 调成 `info`
- [ ] **关掉自动建表**（可选）：`SPRING_SQL_INIT_MODE=never`，避免启动时重复执行 schema
- [ ] **配置定时备份**：`mysqldump course_mgmt > backup_$(date +%F).sql`
- [ ] **确认时区**：容器和 JVM 都设 `Asia/Shanghai`
- [ ] **检查 JDK 版本**：`java -version` 必须是 17

---

## 7. 常见问题

| 现象 | 原因 / 解决 |
|------|-------------|
| `UnsupportedClassVersionError ... 69.0` | 用 JDK 25 编译了。改用 JDK 17 `mvn clean package` |
| 应用启动报 `Communications link failure` | MySQL 没起 / 连接串主机写错；Docker 里主机名是 `mysql` 不是 `localhost` |
| 页面 404 但登录页正常 | 检查 Nginx `proxy_pass` 是否带尾斜杠导致路径错位 |
| 数据没导入 | `docker-entrypoint-initdb.d` 只在空卷执行；`docker compose down -v` 后重来 |
| 中文乱码 | 数据库、连接串、容器三处都要 utf8mb4；JVM 默认 UTF-8 |
| 端口被占用 | 改 `SERVER_PORT` 或 compose 的端口映射 |
