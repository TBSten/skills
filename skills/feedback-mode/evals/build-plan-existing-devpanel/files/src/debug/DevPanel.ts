// 開発ビルド専用のデバッグパネル (main.ts から import.meta.env.DEV のときだけ動的 import)
type Tab = { id: string; label: string; render: (el: HTMLElement) => void };

const tabs: Tab[] = [
  { id: 'mock', label: 'API モック', render: (el) => { el.textContent = 'モック: off'; } },
  { id: 'flags', label: 'Feature flags', render: (el) => { el.textContent = 'newShipFlow: false'; } },
];

export function registerDevTab(tab: Tab) {
  tabs.push(tab);
}

export function mountDevPanel() {
  const panel = document.createElement('aside');
  panel.id = 'dev-panel';
  panel.style.cssText = 'position:fixed;right:8px;bottom:8px;background:#222;color:#fff;padding:8px';
  for (const t of tabs) {
    const b = document.createElement('button');
    b.textContent = t.label;
    const body = document.createElement('div');
    b.onclick = () => t.render(body);
    panel.append(b, body);
  }
  document.body.append(panel);
}
