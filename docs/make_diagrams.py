# -*- coding: utf-8 -*-
"""生成实训报告示意图（窄版布局，保证 A4 打印可读）。"""
import os

OUT = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(OUT, "src")
os.makedirs(SRC, exist_ok=True)
FONT = "Microsoft YaHei, SimHei, sans-serif"


def esc(t):
    return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def box(x, y, w, h, title, fields=None, fill="#eef6f9", stroke="#1a5f7a",
        title_fill="#1a5f7a", title_color="#ffffff", fs=20, fs_title=22):
    parts = [f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="8" fill="{fill}" stroke="{stroke}" stroke-width="2"/>']
    parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="38" rx="8" fill="{title_fill}"/>')
    parts.append(f'<rect x="{x}" y="{y+20}" width="{w}" height="18" fill="{title_fill}"/>')
    parts.append(f'<text x="{x + w/2}" y="{y + 26}" font-size="{fs_title}" font-weight="bold" fill="{title_color}" text-anchor="middle" font-family="{FONT}">{esc(title)}</text>')
    if fields:
        for i, f in enumerate(fields):
            parts.append(f'<text x="{x + 10}" y="{y + 60 + i * 24}" font-size="{fs}" fill="#1e293b" font-family="{FONT}">{esc(f)}</text>')
    return "".join(parts)


def arrow(pts, color="#64748b", label=None, label2=None, width=2, dashed=False):
    d = " ".join(("M" if i == 0 else "L") + f" {p[0]} {p[1]}" for i, p in enumerate(pts))
    dash = ' stroke-dasharray="7 5"' if dashed else ""
    out = f'<path d="{d}" fill="none" stroke="{color}" stroke-width="{width}"{dash} marker-end="url(#arrow)"/>'
    if label:
        out += f'<text x="{pts[0][0] + 8}" y="{pts[0][1] - 8}" font-size="17" fill="{color}" font-family="{FONT}">{label}</text>'
    if label2:
        out += f'<text x="{pts[-1][0] - 20}" y="{pts[-1][1] - 8}" font-size="17" fill="{color}" font-family="{FONT}">{label2}</text>'
    return out


def page(name, w, h, body):
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">
<defs><marker id="arrow" markerWidth="10" markerHeight="8" refX="9" refY="4" orient="auto">
<path d="M0,0 L10,4 L0,8 z" fill="#64748b"/></marker></defs>
<rect width="100%" height="100%" fill="#ffffff"/>
{body}
</svg>'''
    html = f'<!DOCTYPE html><html><head><meta charset="UTF-8"><style>html,body{{margin:0;padding:0;background:#fff}}svg{{display:block}}</style></head><body>{svg}</body></html>'
    with open(os.path.join(SRC, name + ".html"), "w", encoding="utf-8") as f:
        f.write(html)


# ---------------- 功能模块图（卡片式） ----------------
W, H = 1180, 1120
b = [f'<text x="{W/2}" y="40" font-size="28" font-weight="bold" fill="#0f172a" text-anchor="middle" font-family="{FONT}">大学智能课程管理系统功能模块</text>']
cats = [
    ("登录认证", ["账号密码登录", "角色权限控制", "默认密码强制修改", "登录失败锁定"]),
    ("学生端", ["本人考勤查询", "课堂表现查询", "实验与作业查询", "平时/总成绩查询"]),
    ("教师端", ["考勤录入（单条/批量）", "课堂表现录入", "实验管理与记录", "作业管理与记录", "成绩汇总与导出"]),
    ("管理员端", ["班级管理", "教师管理", "学生管理", "课程管理", "评价规则", "操作日志"]),
    ("公共支撑", ["前后端分离 API", "Docker 部署", "数据备份与恢复"]),
]
for i, (cat, leaves) in enumerate(cats):
    cy = 80 + i * 200
    b.append(f'<rect x="40" y="{cy}" width="1100" height="46" rx="8" fill="#1a5f7a"/>')
    b.append(f'<text x="590" y="{cy+30}" font-size="22" font-weight="bold" fill="#fff" text-anchor="middle" font-family="{FONT}">{esc(cat)}</text>')
    paths, cells = [], []
    for j, leaf in enumerate(leaves):
        col = j % 3
        row = j // 3
        lx = 60 + col * 360
        ly = cy + 58 + row * 56
        paths.append(f'<path d="M {lx+170} {cy+46} L {lx+170} {ly}" stroke="#94a3b8" stroke-width="1.6"/>')
        cells.append(f'<rect x="{lx}" y="{ly}" width="340" height="48" rx="6" fill="#f8fafc" stroke="#94a3b8" stroke-width="1.5"/>')
        cells.append(f'<text x="{lx+170}" y="{ly+31}" font-size="19" fill="#1e293b" text-anchor="middle" font-family="{FONT}">{esc(leaf)}</text>')
    b.extend(paths)
    b.extend(cells)
page("modules", W, H, "".join(b))

# ---------------- ER 1 ----------------
W, H = 900, 1120
b = [f'<text x="{W/2}" y="40" font-size="26" font-weight="bold" fill="#0f172a" text-anchor="middle" font-family="{FONT}">基础数据 E-R 图</text>']
b.append(box(60, 80, 280, 140, "sys_role 角色", ["id  PK", "role_code", "role_name"], fs_title=20))
b.append(box(60, 320, 280, 200, "sys_user 用户", ["id  PK", "username", "password", "role_code  FK", "ref_id", "enabled"], fs_title=20))
b.append(box(60, 640, 280, 200, "clazz 班级", ["id  PK", "class_no", "class_name", "grade", "major"], fs_title=20))
b.append(box(60, 920, 280, 170, "student 学生", ["id  PK", "student_no", "name", "clazz_id  FK"], fs_title=20))
b.append(box(560, 80, 280, 160, "teacher 教师", ["id  PK", "teacher_no", "name", "title"], fs_title=20))
b.append(box(560, 320, 280, 200, "course 课程", ["id  PK", "course_no", "course_name", "credit", "teacher_id  FK"], fs_title=20))
b.append(box(560, 640, 280, 140, "course_class 选课", ["id  PK", "course_id  FK", "clazz_id  FK"], fs_title=20))
b.append(box(560, 920, 280, 180, "eval_rule 规则", ["id  PK", "course_id  FK", "四项权重", "两项占比"], fs_title=20))
b.append(arrow([(200, 220), (200, 320)], label="1", label2="N"))
b.append(arrow([(200, 520), (200, 640)], label="1", label2="N"))
b.append(arrow([(200, 840), (200, 920)], label="1", label2="N"))
b.append(arrow([(700, 240), (700, 320)], label="1", label2="N"))
b.append(arrow([(700, 520), (700, 640)], label="1", label2="N"))
b.append(arrow([(340, 740), (560, 710)], color="#16a34a", label="1", label2="N"))
b.append(arrow([(840, 430), (860, 430), (860, 1000), (840, 1000)], color="#d97706", label="1"))
page("er1", W, H, "".join(b))

# ---------------- ER 2（窄版两列） ----------------
W, H = 1250, 1480
b = [f'<text x="{W/2}" y="40" font-size="26" font-weight="bold" fill="#0f172a" text-anchor="middle" font-family="{FONT}">教学数据 E-R 图</text>']
b.append(box(40, 120, 300, 200, "student 学生", ["id  PK", "student_no", "name", "clazz_id", "phone"], fs_title=20))
b.append(box(40, 560, 300, 200, "course 课程", ["id  PK", "course_no", "course_name", "credit", "teacher_id"], fs_title=20))
rows = [
    ("attendance 考勤", 40, ["id PK", "student_id FK", "course_id FK", "attend_date", "status"]),
    ("performance 表现", 240, ["id PK", "student_id FK", "course_id FK", "record_date", "score"]),
    ("exam_score 期末", 440, ["id PK", "student_id FK", "course_id FK", "score", "exam_date"]),
    ("experiment 实验", 640, ["id PK", "course_id FK", "title", "full_score", "due_date"]),
    ("homework 作业", 840, ["id PK", "course_id FK", "title", "full_score", "due_date"]),
]
for title, y, fields in rows:
    b.append(box(620, y, 320, 176, title, fields, fs_title=20))
b.append(box(990, 640, 230, 176, "experiment_record", ["id PK", "experiment_id", "student_id", "completion", "score"], fs_title=17))
b.append(box(990, 840, 230, 176, "homework_record", ["id PK", "homework_id", "student_id", "status", "score"], fs_title=17))

# 课程 -> 教学记录（蓝色总线）
b.append(f'<path d="M 340 660 L 470 660 L 470 120" fill="none" stroke="#2563eb" stroke-width="2"/>')
for title, y, fields in rows:
    yy = y + 88
    b.append(f'<path d="M 470 {yy} L 620 {yy}" fill="none" stroke="#2563eb" stroke-width="2" marker-end="url(#arrow)"/>')
b.append(f'<text x="352" y="640" font-size="17" fill="#2563eb" font-family="{FONT}">1</text>')
b.append(f'<text x="596" y="112" font-size="17" fill="#2563eb" font-family="{FONT}">N</text>')
# 学生 -> 考勤/表现/期末（绿色总线）
b.append(f'<path d="M 340 220 L 400 220 L 400 640" fill="none" stroke="#16a34a" stroke-width="2"/>')
for y in (128, 328, 528):
    b.append(f'<path d="M 400 {y} L 620 {y}" fill="none" stroke="#16a34a" stroke-width="2" marker-end="url(#arrow)"/>')
b.append(f'<text x="352" y="200" font-size="17" fill="#16a34a" font-family="{FONT}">1</text>')
b.append(f'<text x="596" y="120" font-size="17" fill="#16a34a" font-family="{FONT}">N</text>')
# 实验/作业 -> 记录
b.append(arrow([(940, 728), (990, 728)], color="#d97706", label="1", label2="N"))
b.append(arrow([(940, 928), (990, 928)], color="#d97706", label="1", label2="N"))
# 学生 -> 实验记录/作业记录（底部总线）
b.append(f'<path d="M 190 760 L 190 1410 L 950 1410 L 950 750 L 990 750" fill="none" stroke="#16a34a" stroke-width="2" marker-end="url(#arrow)"/>')
b.append(f'<path d="M 950 950 L 990 950" fill="none" stroke="#16a34a" stroke-width="2" marker-end="url(#arrow)"/>')
b.append(f'<text x="203" y="1392" font-size="17" fill="#16a34a" font-family="{FONT}">1</text>')
page("er2", W, H, "".join(b))

# ---------------- 部署架构（竖向） ----------------
W, H = 1000, 1080
b = [f'<text x="{W/2}" y="44" font-size="28" font-weight="bold" fill="#0f172a" text-anchor="middle" font-family="{FONT}">系统部署架构</text>']
b.append(box(250, 70, 500, 110, "浏览器", ["学生 / 教师 / 管理员", "HTML + JavaScript（原生 SPA）"], fs=19, fs_title=22))
b.append(box(250, 240, 500, 140, "Nginx 容器（端口 8080）", ["静态资源：frontend/", "反向代理：/api → app:8080"], fs=19, fs_title=22))
b.append(box(250, 440, 500, 150, "Tomcat 容器（端口 1001）", ["JDK 17 + Tomcat 10.1", "ROOT.war 部署"], fs=19, fs_title=22))
b.append(box(250, 650, 500, 120, "MySQL 容器", ["MySQL 8.0 / course_mgmt / utf8mb4"], fs=19, fs_title=22))
b.append(box(180, 840, 640, 140, "Docker Engine（docker compose 编排）", ["cms-web  /  cms-app  /  cms-mysql"], fill="#f1f5f9", fs=19, fs_title=20))
b.append(arrow([(500, 180), (500, 240)]))
b.append(f'<text x="514" y="216" font-size="18" fill="#64748b" font-family="{FONT}">HTTP</text>')
b.append(arrow([(500, 380), (500, 440)]))
b.append(f'<text x="514" y="416" font-size="18" fill="#64748b" font-family="{FONT}">HTTP / JSON</text>')
b.append(arrow([(500, 590), (500, 650)]))
b.append(f'<text x="514" y="626" font-size="18" fill="#64748b" font-family="{FONT}">JDBC</text>')
page("arch", W, H, "".join(b))

print("diagrams regenerated")
