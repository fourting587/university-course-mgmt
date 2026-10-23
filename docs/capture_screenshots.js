/**
 * 用 Chrome DevTools Protocol 抓取 6 张系统运行截图。
 * 前置：Chrome 以 --remote-debugging-port=9333 启动，tmpadmin 账号可用。
 */
const fs = require('fs');
const path = require('path');

const PORT = 9333;
const BASE = 'http://localhost:8080';
const OUT = path.join(__dirname, 'screenshots');
fs.mkdirSync(OUT, { recursive: true });

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function main() {
  const target = await (await fetch(`http://127.0.0.1:${PORT}/json/new?about:blank`, { method: 'PUT' })).json();
  const ws = new WebSocket(target.webSocketDebuggerUrl);
  let nextId = 0;
  const pending = new Map();
  ws.onmessage = (ev) => {
    const msg = JSON.parse(ev.data);
    if (msg.id && pending.has(msg.id)) {
      const p = pending.get(msg.id);
      pending.delete(msg.id);
      if (msg.error) p.reject(new Error(JSON.stringify(msg.error)));
      else p.resolve(msg.result);
    }
  };
  const send = (method, params = {}) => new Promise((resolve, reject) => {
    const id = ++nextId;
    pending.set(id, { resolve, reject });
    ws.send(JSON.stringify({ id, method, params }));
  });
  await new Promise((resolve, reject) => { ws.onopen = resolve; ws.onerror = reject; });

  await send('Page.enable');
  await send('Runtime.enable');
  await send('Emulation.setDeviceMetricsOverride', {
    width: 1440, height: 900, deviceScaleFactor: 1.5, mobile: false,
  });

  async function shot(name) {
    const { data } = await send('Page.captureScreenshot', { format: 'png', fromSurface: true });
    fs.writeFileSync(path.join(OUT, name), Buffer.from(data, 'base64'));
    console.log('saved', name);
  }

  async function nav(url, wait = 2600) {
    await send('Page.navigate', { url });
    await sleep(wait);
  }

  // 1. 登录页
  await nav(BASE + '/', 2800);
  await shot('login.png');

  // 登录（同源 fetch，写入 Cookie）
  const login = await send('Runtime.evaluate', {
    expression: `fetch('/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:'tmpadmin',password:'Test@123'})}).then(r=>r.text())`,
    awaitPromise: true,
    returnByValue: true,
  });
  console.log('login response:', login.result.value);

  // 重新加载，让 SPA 读取 /api/auth/me
  await send('Page.reload');
  await sleep(3600);
  await shot('dashboard.png');

  const pages = [
    ['attendance.png', '/#/attendance'],
    ['scores.png', '/#/scores'],
    ['students.png', '/#/students'],
    ['audit.png', '/#/audit'],
  ];
  for (const [name, hash] of pages) {
    await nav(BASE + hash, 2800);
    await shot(name);
  }

  ws.close();
}

main().catch((e) => { console.error(e); process.exit(1); });
