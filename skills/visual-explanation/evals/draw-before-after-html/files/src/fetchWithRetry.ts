// PR #58: 503 のときだけ指数バックオフで最大 3 回リトライする (以前はリトライなしで即エラー)
export async function fetchWithRetry(url: string, max = 3): Promise<Response> {
  for (let attempt = 0; ; attempt++) {
    const res = await fetch(url);
    if (res.status !== 503 || attempt >= max) return res;
    await new Promise((r) => setTimeout(r, 200 * 2 ** attempt));
  }
}
