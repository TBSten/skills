#!/usr/bin/env node
/**
 * 検証用の静的ファイルサーバ。Playwright MCP は file:// を開けないため、
 * 生成した HTML はこれ経由で開く。python3 に依存しないよう node で持つ。
 *
 *   node serve.mjs <ディレクトリ> [ポート=8731]
 *
 * バックグラウンドで動かして、終わったら止める:
 *   nohup node serve.mjs <dir> 8731 >/dev/null 2>&1 &
 *   pkill -f "serve.mjs"
 */
import { createServer } from 'node:http';
import { readFile, stat, readdir } from 'node:fs/promises';
import { resolve, join, normalize, extname, sep } from 'node:path';

const root = resolve(process.argv[2] || '.');
const port = Number(process.argv[3] || 8731);

const TYPES = {
  '.html': 'text/html; charset=utf-8', '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8', '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml', '.png': 'image/png', '.jpg': 'image/jpeg', '.webp': 'image/webp'
};

const server = createServer(async (req, res) => {
  try {
    const url = decodeURIComponent((req.url || '/').split('?')[0]);
    const target = resolve(join(root, normalize(url)));
    /* ディレクトリトラバーサルを弾く */
    if (target !== root && !target.startsWith(root + sep)) {
      res.writeHead(403, { 'content-type': 'text/plain' }).end('forbidden');
      return;
    }
    const s = await stat(target);
    if (s.isDirectory()) {
      const index = join(target, 'index.html');
      try {
        const buf = await readFile(index);
        res.writeHead(200, { 'content-type': TYPES['.html'] }).end(buf);
        return;
      } catch {
        /* index.html が無ければ一覧を返す（生成物のファイル名を探す用） */
        const names = await readdir(target);
        const body = names.map(n => `<li><a href="${encodeURIComponent(n)}">${n}</a></li>`).join('');
        res.writeHead(200, { 'content-type': TYPES['.html'] }).end(`<meta charset="utf-8"><ul>${body}</ul>`);
        return;
      }
    }
    const buf = await readFile(target);
    res.writeHead(200, {
      'content-type': TYPES[extname(target)] || 'application/octet-stream',
      'cache-control': 'no-store'
    }).end(buf);
  } catch {
    res.writeHead(404, { 'content-type': 'text/plain' }).end('not found');
  }
});

/* ポートが埋まっているのに黙って死ぬと、別プロセスが配信している中身を
   自分の生成物だと思って検証してしまう。必ず落として気付かせる。 */
server.on('error', e => {
  if (e.code === 'EADDRINUSE') {
    console.error(`ポート ${port} は既に使われている。別のポートを指定するか、先に止める:`);
    console.error(`  lsof -nP -iTCP:${port} -sTCP:LISTEN`);
  } else {
    console.error(String(e));
  }
  process.exit(1);
});
server.listen(port, '127.0.0.1', () => {
  console.log(`http://127.0.0.1:${port}/  (root: ${root})`);
});
