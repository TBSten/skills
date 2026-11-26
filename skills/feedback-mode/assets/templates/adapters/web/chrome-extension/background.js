// background.js — ツールバーのボタン（または Alt+Shift+F）で、今のタブに content.js を入れる。
// activeTab なので、押したタブにだけ、その時だけ入る（どのサイトにも常に入る権限は持たない）。
chrome.action.onClicked.addListener(async (tab) => {
  if (!tab?.id) return;
  try {
    await chrome.scripting.executeScript({ target: { tabId: tab.id }, files: ['content.js'] });
  } catch (e) {
    // chrome:// や Chrome ウェブストアのページには入れない
    console.warn('feedback-mode: このページには入れません', e?.message ?? e);
  }
});
