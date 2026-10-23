// ===================== 弹窗与表单基础 =====================

function todayStr() {
  const d = new Date();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return d.getFullYear() + '-' + mm + '-' + dd;
}

function openModal(title, bodyHtml, onSubmit) {
  const mask = document.createElement('div');
  mask.className = 'modal-mask';
  mask.innerHTML = `<div class="modal">
    <h2>${esc(title)}</h2>
    <form id="modalForm">
      ${bodyHtml}
      <div class="modal-error" id="modalError"></div>
      <div class="modal-actions">
        <button type="button" class="ghost" id="modalCancel">取消</button>
        <button type="submit">保存</button>
      </div>
    </form>
  </div>`;
  document.body.appendChild(mask);
  const form = mask.querySelector('#modalForm');
  const close = () => mask.remove();
  mask.querySelector('#modalCancel').onclick = close;
  mask.onclick = (e) => { if (e.target === mask) close(); };
  form.onsubmit = async (e) => {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(form).entries());
    try {
      await onSubmit(data, close, form);
    } catch (err) {
      document.getElementById('modalError').textContent = err.message || '保存失败';
    }
  };
  return { close, form, mask };
}

const fld = (label, html) => `<label class="fld"><span>${label}</span>${html}</label>`;
const inp = (name, type = 'text', value = '', attrs = '') =>
  `<input name="${name}" type="${type}" value="${esc(value === null || value === undefined ? '' : value)}" ${attrs}/>`;
const sel = (name, options, value, attrs = '') => `<select name="${name}" ${attrs}>${options.map((o) =>
  `<option value="${o.value}" ${String(o.value) === String(value === null || value === undefined ? '' : value) ? 'selected' : ''}>${esc(o.label)}</option>`).join('')}</select>`;
const area = (name, value = '') => `<textarea name="${name}">${esc(value === null || value === undefined ? '' : value)}</textarea>`;
const opts = (list, labelKey = 'name') => list.map((x) => ({ value: x.id, label: x[labelKey] }));

function removeEntity(url, message, refresh) {
  if (!confirm(message)) return;
  API.del(url).then(refresh).catch((e) => alert(e.message));
}

function bindActions(handlers) {
  document.querySelectorAll('[data-act]').forEach((btn) => {
    btn.onclick = () => {
      const fn = handlers[btn.dataset.act];
      if (fn) fn(btn.dataset.id);
    };
  });
}

async function refData() {
  const [courses, classes, teachers] = await Promise.all([
    API.get('/courses'), API.get('/classes'), API.get('/admin/teachers').catch(() => []),
  ]);
  return { courses, classes, teachers };
}

// ===================== 管理员：班级 =====================

async function renderClasses() {
  const list = await API.get('/classes');
  const rows = list.map((c) => [esc(c.classNo), esc(c.className), esc(c.grade), esc(c.major),
    `<span class="actions"><button data-act="edit" data-id="${c.id}">编辑</button>
     <button class="danger" data-act="del" data-id="${c.id}">删除</button></span>`]);
  mount('班级管理', `<div class="card">
    <div class="toolbar"><button id="addBtn">新增班级</button></div>
    ${table(['班级号', '班级名称', '年级', '专业', '操作'], rows)}
  </div>`);
  document.getElementById('addBtn').onclick = () => classForm();
  bindActions({
    edit: (id) => classForm(list.find((x) => String(x.id) === String(id))),
    del: (id) => removeEntity(`/api/admin/classes/${id}`, '确定删除该班级？', renderClasses),
  });
}

function classForm(item = {}) {
  openModal(item.id ? '编辑班级' : '新增班级',
    fld('班级号', inp('classNo', 'text', item.classNo)) +
    fld('班级名称', inp('className', 'text', item.className)) +
    fld('年级', inp('grade', 'text', item.grade)) +
    fld('专业', inp('major', 'text', item.major)),
    async (data, close) => {
      await API.post('/admin/classes', { ...data, id: item.id });
      close();
      renderClasses();
    });
}

// ===================== 管理员：教师 =====================

async function renderTeachers() {
  const list = await API.get('/admin/teachers');
  const rows = list.map((t) => [esc(t.teacherNo), esc(t.name), esc(t.gender), esc(t.title), esc(t.phone),
    `<span class="actions">
      <button data-act="edit" data-id="${t.id}">编辑</button>
      <button class="ghost" data-act="reset" data-id="${t.id}">重置密码</button>
      <button class="ghost" data-act="toggle" data-id="${t.id}">启用/禁用</button>
      <button class="danger" data-act="del" data-id="${t.id}">删除</button>
    </span>`]);
  mount('教师管理', `<div class="card">
    <div class="toolbar"><button id="addBtn">新增教师</button></div>
    ${table(['工号', '姓名', '性别', '职称', '电话', '操作'], rows)}
  </div>`);
  document.getElementById('addBtn').onclick = () => teacherForm();
  bindActions({
    edit: (id) => teacherForm(list.find((x) => String(x.id) === String(id))),
    reset: (id) => API.post(`/admin/teachers/${id}/reset`).then(() => alert('已重置为默认密码 123456')).catch((e) => alert(e.message)),
    toggle: (id) => API.post(`/admin/teachers/${id}/toggle`).then(renderTeachers).catch((e) => alert(e.message)),
    del: (id) => removeEntity(`/api/admin/teachers/${id}`, '确定删除该教师？名下还有课程时会被拒绝。', renderTeachers),
  });
}

function teacherForm(item = {}) {
  openModal(item.id ? '编辑教师' : '新增教师',
    fld('工号', inp('teacherNo', 'text', item.teacherNo)) +
    fld('姓名', inp('name', 'text', item.name)) +
    fld('性别', sel('gender', [{ value: '男', label: '男' }, { value: '女', label: '女' }], item.gender)) +
    fld('职称', inp('title', 'text', item.title)) +
    fld('电话', inp('phone', 'text', item.phone)) +
    fld('邮箱', inp('email', 'email', item.email)),
    async (data, close) => {
      await API.post('/admin/teachers', { ...data, id: item.id });
      close();
      renderTeachers();
    });
}

// ===================== 管理员：学生 =====================

async function renderStudents() {
  const [classes, data] = await Promise.all([
    API.get('/classes'),
    API.get(`/students?keyword=${encodeURIComponent(studentFilter.keyword)}&clazzId=${studentFilter.clazzId || ''}&page=${studentFilter.page}`),
  ]);
  const rows = data.rows.map((s) => [esc(s.studentNo), esc(s.name), esc(s.gender), esc(s.className), esc(s.phone),
    `<span class="actions">
      <button data-act="edit" data-id="${s.id}">编辑</button>
      <button class="ghost" data-act="reset" data-id="${s.id}">重置密码</button>
      <button class="danger" data-act="del" data-id="${s.id}">删除</button>
    </span>`]);
  mount('学生管理', `<div class="card">
    <div class="toolbar"><button id="addBtn">新增学生</button></div>
    <div class="filters">
      <label>关键字<input id="keyword" value="${esc(studentFilter.keyword)}" placeholder="学号/姓名"/></label>
      <label>班级<select id="clazzSel"><option value="">全部</option>${classes.map((c) =>
        `<option value="${c.id}" ${String(c.id) === String(studentFilter.clazzId) ? 'selected' : ''}>${esc(c.className)}</option>`).join('')}</select></label>
      <button id="searchBtn">查询</button>
    </div>
    ${table(['学号', '姓名', '性别', '班级', '电话', '操作'], rows)}
    ${pager(data)}
  </div>`);
  document.getElementById('addBtn').onclick = () => studentForm(null, classes);
  document.getElementById('searchBtn').onclick = () => {
    studentFilter.keyword = document.getElementById('keyword').value.trim();
    studentFilter.clazzId = document.getElementById('clazzSel').value;
    studentFilter.page = 1;
    renderStudents();
  };
  bindPager(data, (p) => { studentFilter.page = p; renderStudents(); });
  bindActions({
    edit: (id) => studentForm(data.rows.find((x) => String(x.id) === String(id)), classes),
    reset: (id) => API.post(`/admin/students/${id}/reset`).then(() => alert('已重置为默认密码 123456')).catch((e) => alert(e.message)),
    del: (id) => removeEntity(`/api/admin/students/${id}`, '确定删除该学生？其考勤、成绩等记录会一并删除。', renderStudents),
  });
}

function studentForm(item, classes) {
  openModal(item ? '编辑学生' : '新增学生',
    fld('学号', inp('studentNo', 'text', item && item.studentNo)) +
    fld('姓名', inp('name', 'text', item && item.name)) +
    fld('性别', sel('gender', [{ value: '男', label: '男' }, { value: '女', label: '女' }], item && item.gender)) +
    fld('班级', sel('clazzId', opts(classes, 'className'), item && item.clazzId)) +
    fld('电话', inp('phone', 'text', item && item.phone)) +
    fld('邮箱', inp('email', 'email', item && item.email)),
    async (data, close) => {
      await API.post('/admin/students', { ...data, id: item && item.id });
      close();
      renderStudents();
    });
}

// ===================== 管理员：课程 =====================

async function renderCourses() {
  if (state.user.roleCode !== 'ADMIN') {
    const list = await API.get('/courses');
    const rows = list.map((c) => [esc(c.courseNo), esc(c.courseName), c.credit, esc(c.semester), esc(c.teacherName)]);
    mount('课程', `<div class="card">${table(['课程号', '名称', '学分', '学期', '教师'], rows)}</div>`);
    return;
  }
  const [courses, classes, teachers] = await Promise.all([
    API.get('/admin/courses'), API.get('/classes'), API.get('/admin/teachers'),
  ]);
  const rows = courses.map((c) => [esc(c.courseNo), esc(c.courseName), c.credit, esc(c.semester), esc(c.teacherName),
    (c.clazzIds || []).map((id) => { const cl = classes.find((x) => x.id === id); return cl ? esc(cl.className) : id; }).join('、'),
    `<span class="actions"><button data-act="edit" data-id="${c.id}">编辑</button>
     <button class="danger" data-act="del" data-id="${c.id}">删除</button></span>`]);
  mount('课程管理', `<div class="card">
    <div class="toolbar"><button id="addBtn">新增课程</button></div>
    ${table(['课程号', '名称', '学分', '学期', '教师', '班级', '操作'], rows)}
  </div>`);
  document.getElementById('addBtn').onclick = () => courseForm(null, classes, teachers);
  bindActions({
    edit: (id) => courseForm(courses.find((x) => String(x.id) === String(id)), classes, teachers),
    del: (id) => removeEntity(`/api/admin/courses/${id}`, '确定删除该课程？其考勤、成绩、实验、作业会一并删除。', renderCourses),
  });
}

function courseForm(item, classes, teachers) {
  const clazzIds = (item && item.clazzIds) || [];
  const clazzChecks = classes.map((c) => `<label class="chk"><input type="checkbox" name="clazzIds" value="${c.id}" ${clazzIds.includes(c.id) ? 'checked' : ''}/> ${esc(c.className)}</label>`).join('');
  openModal(item ? '编辑课程' : '新增课程',
    fld('课程号', inp('courseNo', 'text', item && item.courseNo)) +
    fld('课程名称', inp('courseName', 'text', item && item.courseName)) +
    `<div class="grid2">
      ${fld('学分', inp('credit', 'number', item && item.credit, 'step="0.5" min="0.5" max="20"'))}
      ${fld('学期', inp('semester', 'text', item && item.semester, 'placeholder="2025-2026-1"'))}
    </div>` +
    fld('任课教师', sel('teacherId', opts(teachers), item && item.teacherId)) +
    `<div class="fld"><span>授课班级</span><div class="chk-group">${clazzChecks}</div></div>`,
    async (data, close, form) => {
      data.clazzIds = Array.from(form.querySelectorAll('input[name="clazzIds"]:checked')).map((x) => Number(x.value));
      await API.post('/admin/courses', { ...data, id: item && item.id, clazzIds: data.clazzIds });
      close();
      renderCourses();
    });
}

// ===================== 管理员：评价规则 =====================

async function renderRules() {
  const [rules, courses] = await Promise.all([API.get('/admin/rules'), API.get('/courses')]);
  const rows = rules.map((r) => [esc(r.courseName), r.attendanceWeight, r.performanceWeight, r.experimentWeight,
    r.homeworkWeight, r.regularRatio, r.examRatio,
    `<span class="actions"><button data-act="edit" data-id="${r.id}">编辑</button>
     <button class="danger" data-act="del" data-id="${r.id}">删除</button></span>`]);
  mount('评价规则', `<div class="card">
    <div class="toolbar"><button id="addBtn">新增规则</button></div>
    ${table(['课程', '考勤%', '表现%', '实验%', '作业%', '平时占比%', '期末占比%', '操作'], rows)}
  </div>`);
  document.getElementById('addBtn').onclick = () => ruleForm(null, courses);
  bindActions({
    edit: (id) => ruleForm(rules.find((x) => String(x.id) === String(id)), courses),
    del: (id) => removeEntity(`/api/admin/rules/${id}`, '确定删除该评价规则？', renderRules),
  });
}

function ruleForm(item, courses) {
  const i = item || {};
  openModal(item ? '编辑评价规则' : '新增评价规则',
    fld('课程', sel('courseId', opts(courses, 'courseName'), i.courseId)) +
    `<div class="grid2">
      ${fld('考勤权重 %', inp('attendanceWeight', 'number', i.attendanceWeight ?? 20, 'min="0" max="100" step="0.5"'))}
      ${fld('表现权重 %', inp('performanceWeight', 'number', i.performanceWeight ?? 20, 'min="0" max="100" step="0.5"'))}
      ${fld('实验权重 %', inp('experimentWeight', 'number', i.experimentWeight ?? 30, 'min="0" max="100" step="0.5"'))}
      ${fld('作业权重 %', inp('homeworkWeight', 'number', i.homeworkWeight ?? 30, 'min="0" max="100" step="0.5"'))}
      ${fld('平时占比 %', inp('regularRatio', 'number', i.regularRatio ?? 40, 'min="0" max="100" step="0.5"'))}
      ${fld('期末占比 %', inp('examRatio', 'number', i.examRatio ?? 60, 'min="0" max="100" step="0.5"'))}
    </div>`,
    async (data, close) => {
      await API.post('/admin/rules', { ...data, id: item && item.id });
      close();
      renderRules();
    });
}

// ===================== 教学：考勤 =====================

function openAttendanceForm(courses) {
  const dialog = openModal('单条考勤录入',
    fld('课程', sel('courseId', opts(courses, 'courseName'))) +
    fld('学生', '<select name="studentId" id="studentSel"></select>') +
    fld('日期', inp('attendDate', 'date', todayStr())) +
    fld('状态', sel('status', [{ value: 'PRESENT', label: '到课' }, { value: 'LATE', label: '迟到' },
      { value: 'LEAVE', label: '请假' }, { value: 'ABSENT', label: '缺勤' }], 'PRESENT')) +
    fld('备注', inp('remark', 'text', '')),
    async (data, close) => {
      await API.post('/attendance', data);
      close();
      renderAttendance();
    });

  const courseSel = dialog.form.querySelector('[name="courseId"]');
  const studentSel = dialog.form.querySelector('#studentSel');
  const loadStudents = async () => {
    studentSel.innerHTML = '<option>加载中…</option>';
    const students = await API.get(`/courses/${courseSel.value}/students`);
    studentSel.innerHTML = students.map((s) => `<option value="${s.id}">${esc(s.studentNo)} ${esc(s.name)}</option>`).join('');
  };
  courseSel.onchange = loadStudents;
  loadStudents().catch((e) => { studentSel.innerHTML = `<option>${esc(e.message)}</option>`; });
}

async function openBatchAttendance(courses) {
  const dialog = openModal('批量考勤录入',
    `<div class="grid2">
      ${fld('课程', sel('courseId', opts(courses, 'courseName')))}
      ${fld('日期', inp('attendDate', 'date', todayStr()))}
    </div>
    <div id="batchList" class="batch-list">选择课程后加载学生…</div>`,
    async (data, close) => {
      const records = Array.from(dialog.form.querySelectorAll('[data-student]')).map((el) => ({
        studentId: Number(el.dataset.student),
        status: el.value,
      }));
      if (!records.length) throw new Error('没有学生可提交');
      const res = await API.post('/attendance/batch', { courseId: data.courseId, attendDate: data.attendDate, records });
      close();
      renderAttendance();
      setTimeout(() => alert(`已保存 ${res.saved} 条，跳过 ${res.skipped} 条`), 50);
    });

  const courseSel = dialog.form.querySelector('[name="courseId"]');
  const list = dialog.form.querySelector('#batchList');
  const load = async () => {
    list.innerHTML = '加载中…';
    const students = await API.get(`/courses/${courseSel.value}/students`);
    list.innerHTML = students.map((s) => `<div class="batch-row">
      <span>${esc(s.studentNo)} ${esc(s.name)}</span>
      <select data-student="${s.id}">
        <option value="PRESENT">到课</option><option value="LATE">迟到</option>
        <option value="LEAVE">请假</option><option value="ABSENT">缺勤</option>
      </select></div>`).join('') || '该课程没有学生';
  };
  courseSel.onchange = load;
  load().catch((e) => { list.textContent = e.message; });
}

// ===================== 教学：课堂表现 =====================

function openPerformanceForm(courses) {
  const dialog = openModal('课堂表现录入',
    fld('课程', sel('courseId', opts(courses, 'courseName'))) +
    fld('学生', '<select name="studentId" id="studentSel"></select>') +
    fld('日期', inp('recordDate', 'date', todayStr())) +
    fld('得分', inp('score', 'number', '', 'min="0" max="100" step="0.5"')) +
    fld('评语', inp('comment', 'text', '')),
    async (data, close) => {
      await API.post('/performance', data);
      close();
      renderPerformance();
    });

  const courseSel = dialog.form.querySelector('[name="courseId"]');
  const studentSel = dialog.form.querySelector('#studentSel');
  const load = async () => {
    const students = await API.get(`/courses/${courseSel.value}/students`);
    studentSel.innerHTML = students.map((s) => `<option value="${s.id}">${esc(s.studentNo)} ${esc(s.name)}</option>`).join('');
  };
  courseSel.onchange = load;
  load().catch((e) => { studentSel.innerHTML = `<option>${esc(e.message)}</option>`; });
}

// ===================== 教学：实验 =====================

function openExperimentForm(courses) {
  openModal('新建实验',
    fld('课程', sel('courseId', opts(courses, 'courseName'))) +
    fld('实验名称', inp('title', 'text', '')) +
    fld('满分', inp('fullScore', 'number', 100, 'min="1" max="1000" step="0.5"')) +
    fld('截止日期', inp('dueDate', 'date', '')),
    async (data, close) => {
      await API.post('/experiments', data);
      close();
      renderExperiments();
    });
}

async function openExperimentRecordForm(defs, courses) {
  const dialog = openModal('实验记录录入',
    fld('实验', sel('experimentId', defs.map((d) => ({ value: d.id, label: `${d.title}（${d.courseName}）` })))) +
    fld('学生', '<select name="studentId" id="studentSel"></select>') +
    `<div class="grid2">
      ${fld('完成度 %', inp('completion', 'number', 100, 'min="0" max="100" step="0.5"'))}
      ${fld('得分', inp('score', 'number', '', 'min="0" max="100" step="0.5"'))}
    </div>` +
    fld('备注', inp('remark', 'text', '')),
    async (data, close) => {
      await API.post('/experiments/records', data);
      close();
      renderExperiments();
    });

  const expSel = dialog.form.querySelector('[name="experimentId"]');
  const studentSel = dialog.form.querySelector('#studentSel');
  const load = async () => {
    const def = defs.find((d) => String(d.id) === String(expSel.value));
    if (!def) return;
    const students = await API.get(`/courses/${def.courseId}/students`);
    studentSel.innerHTML = students.map((s) => `<option value="${s.id}">${esc(s.studentNo)} ${esc(s.name)}</option>`).join('');
  };
  expSel.onchange = load;
  load().catch((e) => { studentSel.innerHTML = `<option>${esc(e.message)}</option>`; });
}

// ===================== 教学：作业 =====================

function openHomeworkForm(courses) {
  openModal('新建作业',
    fld('课程', sel('courseId', opts(courses, 'courseName'))) +
    fld('作业名称', inp('title', 'text', '')) +
    fld('满分', inp('fullScore', 'number', 100, 'min="1" max="1000" step="0.5"')) +
    fld('截止日期', inp('dueDate', 'date', '')),
    async (data, close) => {
      await API.post('/homework', data);
      close();
      renderHomework();
    });
}

async function openHomeworkRecordForm(defs, courses) {
  const dialog = openModal('作业记录录入',
    fld('作业', sel('homeworkId', defs.map((d) => ({ value: d.id, label: `${d.title}（${d.courseName}）` })))) +
    fld('学生', '<select name="studentId" id="studentSel"></select>') +
    `<div class="grid2">
      ${fld('得分', inp('score', 'number', '', 'min="0" max="100" step="0.5"'))}
      ${fld('状态', sel('status', [{ value: 'SUBMITTED', label: '已提交' }, { value: 'GRADED', label: '已批改' }, { value: 'PENDING', label: '未交' }], 'GRADED'))}
    </div>` +
    fld('备注', inp('remark', 'text', '')),
    async (data, close) => {
      await API.post('/homework/records', data);
      close();
      renderHomework();
    });

  const hwSel = dialog.form.querySelector('[name="homeworkId"]');
  const studentSel = dialog.form.querySelector('#studentSel');
  const load = async () => {
    const def = defs.find((d) => String(d.id) === String(hwSel.value));
    if (!def) return;
    const students = await API.get(`/courses/${def.courseId}/students`);
    studentSel.innerHTML = students.map((s) => `<option value="${s.id}">${esc(s.studentNo)} ${esc(s.name)}</option>`).join('');
  };
  hwSel.onchange = load;
  load().catch((e) => { studentSel.innerHTML = `<option>${esc(e.message)}</option>`; });
}

const examFilter = { courseId: '' };

// ===================== 教学：期末成绩 =====================

async function renderExam() {
  const courses = await API.get('/courses');
  if (!courses.length) {
    mount('期末成绩录入', '<div class="card">当前账号没有可录入的课程。</div>');
    return;
  }
  if (!examFilter.courseId) examFilter.courseId = courses[0].id;
  const [scores, exams] = await Promise.all([
    API.get(`/scores?courseId=${examFilter.courseId}&type=total&page=1&size=500`),
    API.get(`/exams?courseId=${examFilter.courseId}`),
  ]);
  const examMap = {};
  exams.forEach((e) => { examMap[e.studentId] = e; });

  const rows = scores.rows.map((r) => [esc(r.studentNo), esc(r.studentName), esc(r.className),
    r.examScore === null || r.examScore === undefined ? '<span class="muted">未录入</span>' : r.examScore,
    examMap[r.studentId] ? fmtDate(examMap[r.studentId].examDate) : '-',
    `<button data-act="edit" data-id="${r.studentId}">录入</button>`]);

  mount('期末成绩录入', `<div class="card">
    <div class="filters"><label>课程<select id="courseSel">${courses.map((c) =>
      `<option value="${c.id}" ${String(c.id) === String(examFilter.courseId) ? 'selected' : ''}>${esc(c.courseName)}</option>`).join('')}</select></label></div>
    ${table(['学号', '姓名', '班级', '期末成绩', '考试日期', '操作'], rows)}
  </div>`);
  document.getElementById('courseSel').onchange = (e) => { examFilter.courseId = e.target.value; renderExam(); };
  bindActions({
    edit: (studentId) => {
      const row = scores.rows.find((x) => String(x.studentId) === String(studentId));
      examForm(examFilter.courseId, row, examMap[studentId]);
    },
  });
}

function examForm(courseId, row, existing) {
  openModal(`期末成绩 · ${row.studentName}`,
    fld('学号', `<input value="${esc(row.studentNo)}" disabled/>`) +
    fld('期末成绩', inp('score', 'number', existing ? existing.score : '', 'min="0" max="100" step="0.5"')) +
    fld('考试日期', inp('examDate', 'date', existing && existing.examDate ? String(existing.examDate).slice(0, 10) : '')),
    async (data, close) => {
      await API.post('/exams', { ...data, studentId: row.studentId, courseId });
      close();
      renderExam();
    });
}
