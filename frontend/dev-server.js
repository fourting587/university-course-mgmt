/**
 * 本地开发用静态服务器 + /api 反向代理（无需 Docker、无需构建）。
 *
 * 用法：
 *   1) 先启动后端：java -jar target/course-management-system-1.0.0.jar --server.port=1001
 *   2) node frontend/dev-server.js
 *   3) 打开 http://localhost:8080
 */
const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = Number(process.env.PORT || 8080);
const API_HOST = process.env.API_HOST || '127.0.0.1';
const API_PORT = Number(process.env.API_PORT || 1001);
const ROOT = __dirname;

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
};

const server = http.createServer((req, res) => {
  if (req.url.startsWith('/api/')) {
    const proxyReq = http.request(
      {
        host: API_HOST,
        port: API_PORT,
        path: req.url,
        method: req.method,
        headers: { ...req.headers, host: `${API_HOST}:${API_PORT}` },
      },
      (proxyRes) => {
        res.writeHead(proxyRes.statusCode || 502, proxyRes.headers);
        proxyRes.pipe(res);
      }
    );
    proxyReq.on('error', () => {
      res.writeHead(502, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('API 不可用，请确认后端已启动在 ' + API_HOST + ':' + API_PORT);
    });
    req.pipe(proxyReq);
    return;
  }

  let file = req.url.split('?')[0];
  if (file === '/') file = '/index.html';
  const full = path.resolve(ROOT, '.' + file);
  if (!full.startsWith(ROOT) || !fs.existsSync(full) || !fs.statSync(full).isFile()) {
    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('Not found');
    return;
  }
  res.writeHead(200, { 'Content-Type': MIME[path.extname(full)] || 'application/octet-stream' });
  fs.createReadStream(full).pipe(res);
});

server.listen(PORT, () => {
  console.log(`前端已启动：http://localhost:${PORT}  (API -> ${API_HOST}:${API_PORT})`);
});
