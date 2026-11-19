import { logger } from './lib/logger';

export function renderApp(root: HTMLElement) {
  root.innerHTML = `
    <header><h1>入出庫</h1></header>
    <main>
      <table id="stock"><tbody></tbody></table>
      <button id="receive">入庫を登録</button>
      <button id="ship">出庫を登録</button>
    </main>`;
  root.querySelector('#receive')!.addEventListener('click', () => logger.info('ui.click receive'));
  root.querySelector('#ship')!.addEventListener('click', () => logger.info('ui.click ship'));
}
