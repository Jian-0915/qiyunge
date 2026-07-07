const api = require('NeteaseCloudMusicApi');

const port = parseInt(process.env.PORT) || 3000;
const host = process.env.HOST || '127.0.0.1';

api.server.serveNcmApi({
  host: host,
  port: port,
  checkVersion: false
}).then(() => {
  console.log(`[NeteaseAPI] Server running at http://${host}:${port}`);
}).catch(err => {
  console.error('[NeteaseAPI] Failed to start:', err.message);
  process.exit(1);
});
