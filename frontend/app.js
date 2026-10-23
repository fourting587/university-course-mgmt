const state = { user: null, mustChange: false };
let flash = null;
const app = document.getElementById('app');

const fmt = (v) => {
  if (v === null || v === undefined || v === '') return '-';
  const s = String(v);
  return s.includes('T') ? s.replace('T', ' ').slice(0, 19) : s;
};
// 日期字段（DATE）在 Jackson 里会带 00:00:00，这里只显示年月日
const fmtDate = (v) => {
  if (v === null || v === undefined || v === '') return '-';
  const s = String(v);
  return s.includes('T00:00:00') ? s.slice(0, 10) : fmt(s);
};
const esc = (v) => String(v === null || v === undefined ? '' : v)
  .replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const roleLabel = (r) => ({ ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }[r] || r);

function shell(title, contentHtml) {
  const role = state.user.roleCode;
  const links = [
    ['#/dashboard', '工作台'],
    ['#/scores', '成绩汇总'],
    ['#/attendance', '考勤查询'],
    ['#/performance', '课堂表现'],
    ['#/experiments', '实验'],
    ['#/homework', '作业'],
    ['#/courses', '课程'],
  ];
  if (role === 'ADMIN') links.push(['#/students', '学生管理'], ['#/audit', '操作日志']);
  links.push(['#/password', '修改密码']);

  return `
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">课程管理系统</div>
      <nav>${links.map(([h, t]) => `<a href="${h}" class="${location.hash.startsWith(h) ? 'active' : ''}">${t}</a>`).join('')}</nav>
    </aside>
    <main class="main">
      <div class="topbar">
        <h1>${esc(title)}</h1>
        <div class="who">${esc(state.user.realName)}（${roleLabel(role)}）
          <button class="ghost" id="logoutBtn">退出</button>
        </div>
      </div>
      <div id="content">${contentHtml}</div>
    </main>
  </div>`;
}

function mount(title, contentHtml) {
  app.innerHTML = shell(title, contentHtml);
  document.getElementById('logoutBtn').onclick = logout;
}

async function logout() {
  try { await API.post('/auth/logout'); } catch (e) { /* 忽略 */ }
  state.user = null;
  state.mustChange = false;
  location.hash = '#/login';
  renderLogin();
}

function table(head, rows) {
  if (!rows.length) {
    return `<table class="table"><thead><tr>${head.map((h) => `<th>${h}</th>`).join('')}</tr></thead>
      <tbody><tr><td colspan="${head.length}" class="empty">暂无数据</td></tr></tbody></table>`;
  }
  return `<table class="table"><thead><tr>${head.map((h) => `<th>${h}</th>`).join('')}</tr></thead>
    <tbody>${rows.map((r) => `<tr>${r.map((c) => `<td>${c === null || c === undefined ? '' : c}</td>`).join('')}</tr>`).join('')}</tbody></table>`;
}

function pager(data) {
  if (!data || data.totalPages <= 1) return '';
  return `<div class="pager">
    <button class="ghost" id="prevBtn" ${data.page <= 1 ? 'disabled' : ''}>上一页</button>
    <span>第 ${data.page} / ${data.totalPages} 页，共 ${data.total} 条</span>
    <button class="ghost" id="nextBtn" ${data.page >= data.totalPages ? 'disabled' : ''}>下一页</button>
  </div>`;
}

function bindPager(data, go) {
  const prev = document.getElementById('prevBtn');
  const next = document.getElementById('nextBtn');
  if (prev) prev.onclick = () => go(data.page - 1);
  if (next) next.onclick = () => go(data.page + 1);
}

// ---------------- 登录 ----------------

function renderLogin(message) {
  app.innerHTML = `
  <div class="login-wrap"><div class="login-card">
    <h1>大学智能课程管理系统</h1>
    ${message ? `<div class="alert alert-error">${esc(message)}</div>` : ''}
    <div id="loginError"></div>
    <form id="loginForm">
      <div class="field"><label>用户名</label><input name="username" required autofocus autocomplete="username"/></div>
      <div class="field"><label>密码</label><input name="password" type="password" required autocomplete="current-password"/></div>
      <button type="submit">登 录</button>
    </form>
    <p class="sub" style="margin-top:14px">演示账号：admin / T1001 / 202201001，初始密码 123456</p>
  </div></div>`;

  document.getElementById('loginForm').onsubmit = async (ev) => {
    ev.preventDefault();
    const form = new FormData(ev.target);
    try {
      const data = await API.post('/auth/login', {
        username: form.get('username'),
        password: form.get('password'),
      });
      state.user = data.user;
      state.mustChange = data.mustChangePassword;
      location.hash = state.mustChange ? '#/password' : '#/dashboard';
      router();
    } catch (e) {
      document.getElementById('loginError').innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`;
    }
  };
}

// ---------------- 工作台 ----------------

async function renderDashboard() {
  mount('工作台', '<div class="card">加载中…</div>');
  const data = await API.get('/dashboard');
  const items = [
    data.students !== undefined && ['学生', data.students],
    data.teachers !== undefined && ['教师', data.teachers],
    data.classes !== undefined && ['班级', data.classes],
    ['课程', data.courses],
    data.attendance !== undefined && ['考勤记录', data.attendance],
  ].filter(Boolean);

  mount('工作台', `
    ${flash ? `<div class="alert alert-ok">${esc(flash)}</div>` : ''}
    <div class="cards">${items.map(([t, n]) => `<div class="stat"><b>${n}</b><span>${t}</span></div>`).join('')}</div>
    <div class="card">
      <p style="margin:0;color:#64748b;font-size:13px;">
        本页面由独立前端通过 <code>/api</code> 获取 JSON 数据；左侧菜单可查询成绩、考勤、表现、实验与作业。
      </p>
    </div>`);
  flash = null;
}

// ---------------- 成绩 ----------------

const scoreFilter = { courseId: '', type: 'total', page: 1 };

async function renderScores() {
  const [courses, classes] = await Promise.all([API.get('/courses'), API.get('/classes')]);
  if (!courses.length) {
    mount('成绩汇总', '<div class="card">当前账号没有可查看的课程。</div>');
    return;
  }
  if (!scoreFilter.courseId) scoreFilter.courseId = courses[0].id;

  const data = await API.get(`/scores?courseId=${scoreFilter.courseId}&type=${scoreFilter.type}&page=${scoreFilter.page}`);
  const isTotal = scoreFilter.type === 'total';
  const head = ['学号', '姓名', '班级', '考勤', '表现', '实验', '作业', '平时', ...(isTotal ? ['期末', '总成绩'] : [])];
  const rows = data.rows.map((r) => [
    esc(r.studentNo), esc(r.studentName), esc(r.className),
    r.attendanceScore, r.performanceScore, r.experimentScore, r.homeworkScore, r.regularScore,
    ...(isTotal ? [r.examScore, r.totalScore] : []),
  ]);

  mount('成绩汇总', `
    <div class="card">
      <div class="filters">
        <label>课程<select id="courseSel">${courses.map((c) => `<option value="${c.id}" ${String(c.id) === String(scoreFilter.courseId) ? 'selected' : ''}>${esc(c.courseName)}</option>`).join('')}</select></label>
        <label>类型<select id="typeSel">
          <option value="total" ${isTotal ? 'selected' : ''}>总成绩</option>
          <option value="regular" ${!isTotal ? 'selected' : ''}>平时成绩</option>
        </select></label>
        <a class="button" href="/api/scores/export?courseId=${scoreFilter.courseId}&type=${scoreFilter.type}">导出 Excel（CSV）</a>
      </div>
      ${table(head, rows)}
      ${pager(data)}
    </div>`);

  document.getElementById('courseSel').onchange = (e) => {
    scoreFilter.courseId = e.target.value;
    scoreFilter.page = 1;
    renderScores();
  };
  document.getElementById('typeSel').onchange = (e) => {
    scoreFilter.type = e.target.value;
    scoreFilter.page = 1;
    renderScores();
  };
  bindPager(data, (p) => { scoreFilter.page = p; renderScores(); });
}

// ---------------- 通用列表页 ----------------

async function renderAttendance() {
  const [courses, classes] = await Promise.all([API.get('/courses'), API.get('/classes')]);
  const f = attendanceFilter;
  const data = await API.get(`/attendance?courseId=${f.courseId || ''}&clazzId=${f.clazzId || ''}&page=${f.page}`);
  const rows = data.rows.map((r) => [
    fmtDate(r.attendDate), esc(r.studentNo), esc(r.studentName), esc(r.className), esc(r.courseName), esc(r.status), esc(r.remark),
  ]);

  mount('考勤查询', `
    <div class="card">
      <div class="filters">
        <label>课程<select id="courseSel"><option value="">全部</option>${courses.map((c) => `<option value="${c.id}" ${String(c.id) === String(f.courseId) ? 'selected' : ''}>${esc(c.courseName)}</option>`).join('')}</select></label>
        <label>班级<select id="clazzSel"><option value="">全部</option>${classes.map((c) => `<option value="${c.id}" ${String(c.id) === String(f.clazzId) ? 'selected' : ''}>${esc(c.className)}</option>`).join('')}</select></label>
      </div>
      ${table(['日期', '学号', '姓名', '班级', '课程', '状态', '备注'], rows)}
      ${pager(data)}
    </div>`);
  document.getElementById('courseSel').onchange = (e) => { f.courseId = e.target.value; f.page = 1; renderAttendance(); };
  document.getElementById('clazzSel').onchange = (e) => { f.clazzId = e.target.value; f.page = 1; renderAttendance(); };
  bindPager(data, (p) => { f.page = p; renderAttendance(); });
}

const attendanceFilter = { courseId: '', clazzId: '', page: 1 };
const performanceFilter = { courseId: '', page: 1 };
const experimentFilter = { courseId: '', page: 1 };
const homeworkFilter = { courseId: '', page: 1 };
const studentFilter = { keyword: '', clazzId: '', page: 1 };
const auditPage = { page: 1 };

async function renderPerformance() {
  const courses = await API.get('/courses');
  const f = performanceFilter;
  const data = await API.get(`/performance?courseId=${f.courseId || ''}&page=${f.page}`);
  const rows = data.rows.map((r) => [fmtDate(r.recordDate), esc(r.studentNo), esc(r.studentName), esc(r.courseName), r.score, esc(r.comment)]);
  mount('课堂表现', `
    <div class="card">
      <div class="filters"><label>课程<select id="courseSel"><option value="">全部</option>${courses.map((c) => `<option value="${c.id}" ${String(c.id) === String(f.courseId) ? 'selected' : ''}>${esc(c.courseName)}</option>`).join('')}</select></label></div>
      ${table(['日期', '学号', '姓名', '课程', '得分', '评语'], rows)}
      ${pager(data)}
    </div>`);
  document.getElementById('courseSel').onchange = (e) => { f.courseId = e.target.value; f.page = 1; renderPerformance(); };
  bindPager(data, (p) => { f.page = p; renderPerformance(); });
}

async function renderExperiments() {
  const courses = await API.get('/courses');
  const f = experimentFilter;
  const data = await API.get(`/experiments?courseId=${f.courseId || ''}&page=${f.page}`);
  const rows = data.rows.map((r) => [esc(r.experimentTitle), esc(r.courseName), esc(r.studentNo), esc(r.studentName), r.completion, r.score, fmt(r.submitTime)]);
  mount('实验', `
    <div class="card">
      <div class="filters"><label>课程<select id="courseSel"><option value="">全部</option>${courses.map((c) => `<option value="${c.id}" ${String(c.id) === String(f.courseId) ? 'selected' : ''}>${esc(c.courseName)}</option>`).join('')}</select></label></div>
      ${table(['实验', '课程', '学号', '姓名', '完成度', '得分', '提交时间'], rows)}
      ${pager(data)}
    </div>`);
  document.getElementById('courseSel').onchange = (e) => { f.courseId = e.target.value; f.page = 1; renderExperiments(); };
  bindPager(data, (p) => { f.page = p; renderExperiments(); });
}

async function renderHomework() {
  const courses = await API.get('/courses');
  const f = homeworkFilter;
  const data = await API.get(`/homework?courseId=${f.courseId || ''}&page=${f.page}`);
  const rows = data.rows.map((r) => [esc(r.homeworkTitle), esc(r.courseName), esc(r.studentNo), esc(r.studentName), esc(r.status), r.score, fmt(r.submitTime)]);
  mount('作业', `
    <div class="card">
      <div class="filters"><label>课程<select id="courseSel"><option value="">全部</option>${courses.map((c) => `<option value="${c.id}" ${String(c.id) === String(f.courseId) ? 'selected' : ''}>${esc(c.courseName)}</option>`).join('')}</select></label></div>
      ${table(['作业', '课程', '学号', '姓名', '状态', '得分', '提交时间'], rows)}
      ${pager(data)}
    </div>`);
  document.getElementById('courseSel').onchange = (e) => { f.courseId = e.target.value; f.page = 1; renderHomework(); };
  bindPager(data, (p) => { f.page = p; renderHomework(); });
}

async function renderCourses() {
  const courses = await API.get('/courses');
  const rows = courses.map((c) => [esc(c.courseNo), esc(c.courseName), c.credit, esc(c.semester), esc(c.teacherName)]);
  mount('课程', `<div class="card">${table(['课程号', '名称', '学分', '学期', '教师'], rows)}</div>`);
}

async function renderStudents() {
  const classes = await API.get('/classes');
  const f = studentFilter;
  const data = await API.get(`/students?keyword=${encodeURIComponent(f.keyword)}&clazzId=${f.clazzId || ''}&page=${f.page}`);
  const rows = data.rows.map((s) => [esc(s.studentNo), esc(s.name), esc(s.gender), esc(s.className), esc(s.phone)]);
  mount('学生管理', `
    <div class="card">
      <div class="filters">
        <label>关键字<input id="keyword" value="${esc(f.keyword)}" placeholder="学号/姓名"/></label>
        <label>班级<select id="clazzSel"><option value="">全部</option>${classes.map((c) => `<option value="${c.id}" ${String(c.id) === String(f.clazzId) ? 'selected' : ''}>${esc(c.className)}</option>`).join('')}</select></label>
        <button id="searchBtn">查询</button>
      </div>
      ${table(['学号', '姓名', '性别', '班级', '电话'], rows)}
      ${pager(data)}
    </div>`);
  document.getElementById('searchBtn').onclick = () => {
    f.keyword = document.getElementById('keyword').value.trim();
    f.clazzId = document.getElementById('clazzSel').value;
    f.page = 1;
    renderStudents();
  };
  bindPager(data, (p) => { f.page = p; renderStudents(); });
}

async function renderAudit() {
  const data = await API.get(`/audit?page=${auditPage.page}`);
  const rows = data.rows.map((a) => [fmt(a.createdAt), esc(a.username), esc(a.action), esc(a.target), esc(a.detail), esc(a.ip)]);
  mount('操作日志', `<div class="card">${table(['时间', '操作人', '操作', '对象', '详情', 'IP'], rows)}${pager(data)}</div>`);
  bindPager(data, (p) => { auditPage.page = p; renderAudit(); });
}

// ---------------- 修改密码 ----------------

async function renderPassword() {
  mount('修改密码', `
    <div class="card" style="max-width:420px">
      ${state.mustChange ? '<div class="alert alert-error">你正在使用默认密码，请先设置新密码后再进入系统。</div>' : ''}
      <div id="pwdMsg"></div>
      <form id="pwdForm">
        <div class="field" style="margin-bottom:12px;display:flex;flex-direction:column;gap:4px;">
          <label style="font-size:12px;color:#64748b">新密码</label>
          <input name="newPassword" type="password" minlength="6" maxlength="50" required/>
        </div>
        <div class="field" style="margin-bottom:12px;display:flex;flex-direction:column;gap:4px;">
          <label style="font-size:12px;color:#64748b">确认新密码</label>
          <input name="confirmPassword" type="password" minlength="6" maxlength="50" required/>
        </div>
        <button type="submit">确认修改</button>
      </form>
    </div>`);

  document.getElementById('pwdForm').onsubmit = async (ev) => {
    ev.preventDefault();
    const form = new FormData(ev.target);
    try {
      await API.post('/auth/password', {
        newPassword: form.get('newPassword'),
        confirmPassword: form.get('confirmPassword'),
      });
      state.mustChange = false;
      flash = '密码修改成功，下次请使用新密码登录。';
      location.hash = '#/dashboard';
      renderDashboard();
    } catch (e) {
      document.getElementById('pwdMsg').innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`;
    }
  };
}

// ---------------- 路由 ----------------

const routes = {
  '/dashboard': renderDashboard,
  '/scores': renderScores,
  '/attendance': renderAttendance,
  '/performance': renderPerformance,
  '/experiments': renderExperiments,
  '/homework': renderHomework,
  '/courses': renderCourses,
  '/students': renderStudents,
  '/audit': renderAudit,
  '/password': renderPassword,
};

async function router() {
  const path = (location.hash.replace(/^#/, '') || '/dashboard').split('?')[0];
  if (!state.user) {
    renderLogin();
    return;
  }
  if (state.mustChange && path !== '/password') {
    location.hash = '#/password';
    return;
  }
  const view = routes[path] || renderDashboard;
  try {
    await view();
  } catch (e) {
    if (e.status === 401) {
      state.user = null;
      renderLogin('登录已过期，请重新登录');
      return;
    }
    if (e.code === 'MUST_CHANGE') {
      state.mustChange = true;
      location.hash = '#/password';
      return;
    }
    const box = document.getElementById('content');
    const html = `<div class="alert alert-error">${esc(e.message)}</div>`;
    if (box) box.innerHTML = html;
    else app.innerHTML = html;
  }
}

window.addEventListener('hashchange', router);

(async function boot() {
  try {
    const me = await API.get('/auth/me');
    state.user = me.user;
    state.mustChange = me.mustChangePassword;
  } catch (e) {
    state.user = null;
  }
  if (!location.hash) {
    location.hash = state.user ? (state.mustChange ? '#/password' : '#/dashboard') : '#/login';
  }
  router();
})();
