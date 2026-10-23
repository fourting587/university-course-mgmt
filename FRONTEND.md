# 前后端分离说明

## 架构

```
浏览器
  │  http://localhost:8080
  ▼
┌─────────────────────────────┐
│ 静态前端（Nginx / dev-server）│
│  index.html + app.js + api.js │
└──────────────┬──────────────┘
               │  /api/** 反向代理
               ▼
┌─────────────────────────────┐
│ Spring Boot（1001）          │
│  /api/**  → JSON             │
│  Thymeleaf 旧界面（保留兼容） │
└──────────────┬──────────────┘
               ▼
            MySQL 8
```

- 前端是**零依赖静态页面**（原生 JS + hash 路由），不需要 Node 构建
- 登录态用 HttpSession + Cookie，前后端同源（Nginx 把 `/api` 反代到后端），不存在跨域 Cookie 问题
- 原来的 Thymeleaf 页面仍保留在 1001 端口，方便对比和逐步下线

## 目录

```
frontend/
  index.html       # 入口
  app.js           # 路由 + 页面渲染
  api.js           # fetch 封装
  style.css        # 样式
  dev-server.js    # 本地开发服务器（静态 + /api 代理）
  nginx.conf       # 生产环境 Nginx 配置
  Dockerfile       # 前端镜像
```

## 启动方式一：Docker Compose（推荐）

```bash
docker compose up -d --build
```

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:8080 |
| 后端 API | http://localhost:1001/api/... |
| MySQL | 仅容器网络内可访问 |

首次启动会自动建表并导入 240 人演示数据。

## 启动方式二：本地开发（不用 Docker）

```bash
# 1. 启动后端（IDEA 里跑 CmsApplication 也可以）
java -jar target/course-management-system-1.0.0.war --server.port=1001

# 2. 启动前端静态服务器（需要本机 Node）
node frontend/dev-server.js

# 3. 打开
http://localhost:8080
```

`dev-server.js` 会把 `/api/**` 转发到 `127.0.0.1:1001`，其余请求返回 `frontend/` 下的静态文件。

## 演示账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | admin | 123456 |
| 教师 | T1001 | 123456 |
| 学生 | 202201001 | 123456 |

使用默认密码登录后，前端会强制跳转到「修改密码」页。

## API 一览

所有接口返回 JSON，未登录返回 `401 {"error":"未登录"}`；默认密码未修改时返回 `403 {"error":"请先修改默认密码","code":"MUST_CHANGE"}`。

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/auth/login` | 登录 | 公开 |
| GET | `/api/auth/me` | 当前用户 | 登录 |
| POST | `/api/auth/logout` | 退出 | 登录 |
| POST | `/api/auth/password` | 修改密码 | 登录 |
| GET | `/api/dashboard` | 首页统计 | 登录 |
| GET | `/api/courses` | 课程列表（按角色过滤） | 登录 |
| GET | `/api/classes` | 班级列表 | 登录 |
| GET | `/api/students?keyword&clazzId&page` | 学生分页 | 管理员 |
| GET | `/api/attendance?courseId&clazzId&page` | 考勤分页 | 登录（教师/学生自动收窄） |
| GET | `/api/performance?courseId&page` | 课堂表现分页 | 登录 |
| GET | `/api/experiments?courseId&page` | 实验记录分页 | 登录 |
| GET | `/api/homework?courseId&page` | 作业记录分页 | 登录 |
| GET | `/api/scores?courseId&clazzId&type&page` | 成绩报表分页 | 登录（教师限本人课程） |
| GET | `/api/scores/export?courseId&type` | 导出 CSV | 同上 |
| GET | `/api/audit?page` | 操作日志 | 管理员 |

### 写接口（新前端已接入）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST / DELETE | `/api/admin/classes`、`/api/admin/classes/{id}` | 班级增改/删除 | 管理员 |
| POST / DELETE | `/api/admin/teachers`、`/{id}` | 教师增改/删除 | 管理员 |
| POST | `/api/admin/teachers/{id}/reset`、`/toggle` | 重置密码 / 启停 | 管理员 |
| POST / DELETE | `/api/admin/students`、`/{id}` | 学生增改/删除 | 管理员 |
| POST | `/api/admin/students/{id}/reset`、`/toggle` | 重置密码 / 启停 | 管理员 |
| POST / DELETE | `/api/admin/courses`、`/{id}` | 课程增改/删除（含授课班级） | 管理员 |
| POST / DELETE | `/api/admin/rules`、`/{id}` | 评价规则增改/删除 | 管理员 |
| GET | `/api/admin/teachers`、`/api/admin/courses`、`/api/admin/rules` | 管理端下拉/列表 | 管理员 |
| GET | `/api/courses/{id}/students` | 课程学生名单（录入用） | 教师（本人课程）/管理员 |
| POST / DELETE | `/api/attendance`、`/api/attendance/{id}` | 单条考勤 | 教师/管理员 |
| POST | `/api/attendance/batch` | 批量考勤 | 教师/管理员 |
| POST / DELETE | `/api/performance`、`/api/performance/{id}` | 课堂表现 | 教师/管理员 |
| POST / DELETE | `/api/experiments`、`/api/experiments/{id}` | 实验定义 | 教师/管理员 |
| POST / DELETE | `/api/experiments/records`、`/api/experiments/records/{id}` | 实验记录 | 教师/管理员 |
| POST / DELETE | `/api/homework`、`/api/homework/{id}` | 作业定义 | 教师/管理员 |
| POST / DELETE | `/api/homework/records`、`/api/homework/records/{id}` | 作业记录 | 教师/管理员 |
| GET | `/api/experiment-defs`、`/api/homework-defs` | 实验/作业下拉 | 教师/管理员 |
| POST / DELETE | `/api/exams`、`/api/exams/{id}` | 期末成绩（按学生+课程自动覆盖） | 教师/管理员 |
| GET | `/api/exams?courseId` | 某课程期末成绩 | 教师/管理员 |

## 与旧界面的关系

- 旧的 Thymeleaf 页面（`/login`、`/admin/**`、`/teach/**`）仍可用，适合过渡期
- 新前端只依赖 `/api/**`，后续可以安全地删除 Thymeleaf 模板
- 两套界面共用同一个数据库和同一套权限规则

## 常见问题

| 现象 | 原因 |
|------|------|
| 登录后立刻跳回登录页 | Cookie 没带上：确认通过 8080（Nginx）访问，而不是直接用 1001 打开前端文件 |
| 一直提示「请先修改默认密码」 | 默认密码必须先改，改完才能访问其他接口 |
| 502 Bad Gateway | 后端没起或没就绪，`docker compose logs app` 查看 |
| 前端空白 | 浏览器控制台看 `app.js` 是否加载、`/api/auth/me` 返回什么 |
