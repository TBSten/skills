import { renderApp } from './app';
import { logger } from './lib/logger';

renderApp(document.getElementById('app')!);
logger.info('app.start');

if (import.meta.env.DEV) {
  // 開発ビルドだけ。本番では dead code elimination で消える
  import('./debug/DevPanel').then((m) => m.mountDevPanel());
}
