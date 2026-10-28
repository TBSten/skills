/**
 * memory-sink.mjs — メモリの Sink（テスト・契約の確認・モックアップ用）。【追加】
 * 採番・競合・archived の数え方は file-sink と同じ規則（feedback-core.mjs の nextSeqFromIds）。
 */
import { feedbackDay, entryId, nextSeqFromIds, ID_PATTERN, safeDocPath } from './feedback-core.mjs';

/** @returns {import('./feedback-core.mjs').Sink & { entries: Map<string, {text: string, state: 'open'|'archived', createdAt: string}>, attachments: Map<string, Uint8Array>, docs: Map<string, string> }} */
export function createMemorySink() {
  const entries = new Map(); // id → { text, state, createdAt }
  const attachments = new Map(); // ref → Uint8Array
  const docs = new Map(); // 正規形の相対パス → 文書（writeDoc / readDoc）
  let attachSeq = 0;
  const info = (id) => {
    const e = entries.get(id);
    return { id, ref: `memory:${id}`, state: e.state, bytes: new TextEncoder().encode(e.text).length, createdAt: e.createdAt };
  };
  return {
    kind: 'memory',
    capabilities: { put: true, list: true, read: true, archive: true, doc: true },
    entries,
    attachments,
    docs,
    async putAttachments(files, { now = new Date() } = {}) {
      const dir = `memory:attach/${++attachSeq}`;
      const refs = {};
      for (const f of files) {
        refs[f.name] = `${dir}/${f.name}`;
        attachments.set(refs[f.name], f.data);
      }
      return { refs, dir, error: null };
    },
    async putEntry({ text, now = new Date() }) {
      const day = feedbackDay(now);
      const id = entryId(day, nextSeqFromIds([...entries.keys()], day));
      entries.set(id, { text, state: 'open', createdAt: now.toISOString() });
      return { ...info(id), attempts: 1 };
    },
    async list({ state = 'all' } = {}) {
      return [...entries.keys()].filter((id) => state === 'all' || entries.get(id).state === state).sort().map(info);
    },
    async read(id) {
      if (!ID_PATTERN.test(id) || !entries.has(id)) throw new Error(`見つかりません: ${id}`);
      return { ...info(id), text: entries.get(id).text };
    },
    async archive(id, { note } = {}) {
      if (!ID_PATTERN.test(id) || !entries.has(id)) throw new Error(`見つかりません: ${id}`);
      const e = entries.get(id);
      if (e.state === 'archived') return info(id); // file-sink と同じく、2 回目は何もしない（対応メモを二重にしない）
      e.state = 'archived';
      if (note) e.text += `\n---\n対応メモ: ${note}\n`;
      return info(id);
    },
    async writeDoc(relPath, text) {
      docs.set(safeDocPath(relPath), text);
    },
    async readDoc(relPath) {
      return docs.get(safeDocPath(relPath)) ?? null;
    },
  };
}
