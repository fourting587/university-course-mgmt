# 大学智能课程管理系统

信息化系统开发与应用实训项目。技术栈：**Java 17 + Spring Boot 3.5 + Thymeleaf + MyBatis + MySQL 8**。

## 功能概览

| 角色 | 能力 |
|------|------|
| 学生 | 查询本人考勤、课堂表现、实验、作业；查看平时/总成绩 |
| 教师 | 所授课程数据录入（单条/批量考勤）、成绩与实验作业管理、成绩汇总 |
| 管理员 | 上述全部 + 学生/教师/班级/课程/评价规则 CRUD、重置密码/启停账号、操作日志 |

其他能力：

- 默认密码首次登录强制修改密码；改密后其他设备会话立即失效
- 连续 5 次登录失败锁定 10 分钟；POST 请求做同源校验（CSRF 防护）
- 成绩报表支持分页，并可导出 CSV（Excel 可直接打开）
- 关键写操作写入 `audit_log`，管理员可在「操作日志」查看

成绩计算：

- 平时成绩 = 考勤、表现、实验、作业按评价规则权重加权
- 总成绩 = 平时×平时占比 + 期末×期末占比
- 考勤折算：到课 100 / 迟到 80 / 请假 60 / 缺勤 0

## 环境依赖

- JDK 17
- Maven 3.9+
- MySQL 8.0

## 快速启动

```bash
cd university-course-mgmt
mvn spring-boot:run
```

浏览器打开：http://localhost:1001

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | 123456 |
| 教师 | T1001 | 123456 |
| 学生 | 202201001 | 123456 |

> 首次用默认密码 `123456` 登录成功后，系统会自动跳转到「修改密码」页面，必须设置新密码才能进入系统；之后请使用新密码登录。新密码长度不少于 6 位，且不能与默认密码相同。

## 数据库准备

1. 新建数据库：

```sql
CREATE DATABASE IF NOT EXISTS course_mgmt CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行初始化脚本：
   - `src/main/resources/db/schema-mysql.sql`（应用启动时会自动执行，幂等；新建库会带上外键约束）
   - `src/main/resources/db/data-mysql.sql`（**完整数据快照**：先 `TRUNCATE` 再写入 8 个班、240 名学生、16 教学周的考勤/表现/实验/作业/期末记录（约 6.5 万条）；只需执行一次，重复执行会重置为快照数据）
   - 已有旧库升级：执行一次 `src/main/resources/db/migration-fk.sql` 补外键（重复执行会提示约束已存在，可忽略）

3. 确认 `src/main/resources/application.yml` 中的 MySQL 连接参数正确：

```yaml
spring:
  profiles:
    active: mysql
  datasource:
    url: jdbc:mysql://localhost:3306/course_mgmt?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: 123456
```

## 部署

项目同时提供两套界面：

- **前后端分离版（推荐）**：`frontend/` 静态页面 + `/api/**` JSON 接口，前端 8080、后端 1001，详见 [FRONTEND.md](FRONTEND.md)
- **Thymeleaf 服务端渲染版（保留兼容）**：原有的 1001 端口页面

Docker Compose 会一次启动 **前端（8080）+ 后端（1001）+ MySQL**：

```bash
docker compose up -d --build
# 前端 http://localhost:8080
```

后端构建产物是可执行 WAR（`mvn clean package` → `target/*.war`）：

- Docker 方式：部署到 `tomcat:10.1-jdk17-temurin` 容器的 `webapps/ROOT.war`
- 手工方式：既可以 `java -jar target/*.war` 独立运行，也可以直接放进外部 Tomcat 的 `webapps/`

其他部署方式（systemd、Nginx 反代 + HTTPS、外部 Tomcat）见 [DEPLOY.md](DEPLOY.md)。

## 实训材料

- [docs/README.md](docs/README.md)：材料清单与重新生成方法
- `docs/信息化系统开发与应用实训报告-刘中天-20231026120.docx`：实训报告（38 页）
- `docs/测试用例.xlsx`：30 条系统测试用例
- `docs/ER图-基础数据.png`、`docs/ER图-教学数据.png`、`docs/功能模块图.png`：设计图

## 项目结构

```
src/main/java/com/university/cms/
  controller/   # 控制层
  service/      # 业务（成绩汇总）
  mapper/       # MyBatis 数据访问
  entity/       # 实体
  interceptor/  # 登录与权限
  config/       # MVC 配置
src/main/resources/
  db/           # MySQL 初始化脚本
  templates/    # Thymeleaf 页面
  static/       # 静态资源
frontend/       # 前后端分离版静态页面
docs/           # 实训报告、ER 图、功能模块图、测试用例
```

## 常见问题

- **端口占用**：修改 `server.port`
- **MySQL 连不上**：检查 MySQL 服务是否启动、数据库 `course_mgmt` 是否存在、账号密码与连接串是否正确
- **中文乱码**：IDE 与文件编码均设为 UTF-8

## 测试

```bash
mvn test
```

当前包含 21 个单元测试：密码加密与兼容、登录锁定、课程权限、登录拦截器（CSRF/角色/强制改密）、成绩加权计算与聚合查询次数。
