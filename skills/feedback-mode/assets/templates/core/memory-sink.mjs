/**
 * memory-sink.mjs — メモリの Sink（テスト・契約の確認・モックアップ用）。【追加】
 * 採番・競合・archived の数え方は file-sink と同じ規則（feedback-core.mjs の nextSeqFromIds）。
 */
import { feedbackDay, entryId, nextSeqFromIds, ID_PATTERN } from './feedback-core.mjs';

export function createMemorySink() {
  const entries = new Map(); // id → { text, state, createdAt }
  const attachments = new Map(); // ref → Uint8Array
  let attachSeq = 0;
  const info = (id) => {
    const e = entries.get(id);
    return { id, ref: `memory:${id}`, state: e.state, bytes: new TextEncoder().encode(e.text).length, createdAt: e.createdAt };
  };
  return {
    kind: 'memory',
    capabilities: { put: true, list: true, read: true, archive: true },
    entries,
    attachments,
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
      e.state = 'archived';
      if (note) e.text += `\n---\n対応メモ: ${note}\n`;
      return info(id);
    },
  };
}
