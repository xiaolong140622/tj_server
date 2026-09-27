// 查返利历史（产品 rebate-page-spec §4）：本期本地留存，按 userId 隔离，最近 200 条上限，本地分页 20/页
import { getUserInfo } from './auth';

const KEY_PREFIX = 'rebate_history_v1';
const MAX_ENTRIES = 200;
const PAGE_SIZE = 20;
const PENDING_KEY = 'rebate_pending_input';

export const REBATE_PAGE_SIZE = PAGE_SIZE;

const historyKey = () => {
  let uid = '';
  try { uid = String(getUserInfo()?.uid || ''); } catch (e) { /* ignore */ }
  return `${KEY_PREFIX}_${uid || 'anon'}`;
};

const readAll = () => {
  try {
    const raw = uni.getStorageSync(historyKey());
    const list = raw ? JSON.parse(raw) : [];
    return Array.isArray(list) ? list : [];
  } catch (e) { return []; }
};

const writeAll = (list) => {
  try { uni.setStorageSync(historyKey(), JSON.stringify(list.slice(0, MAX_ENTRIES))); } catch (e) { /* ignore */ }
};

// 新条目插顶（时间倒序），超 200 丢最旧；失败查询不得调用本方法（不落历史）
export const addRebateRecord = (entry) => {
  const list = readAll().filter((e) => e && e.id !== entry.id);
  list.unshift(entry);
  writeAll(list);
};

export const removeRebateRecord = (id) => {
  writeAll(readAll().filter((e) => e.id !== id));
};

// pageIndex 从 0 起：每页 20 条更早记录
export const loadRebatePage = (pageIndex = 0) => {
  const all = readAll();
  const start = pageIndex * PAGE_SIZE;
  return { items: all.slice(start, start + PAGE_SIZE), total: all.length };
};

// 搜索页 → 查返利页自动查询传参（switchTab 不支持 query，走 storage 一次性交接）
export const setPendingRebateInput = (text) => {
  try { uni.setStorageSync(PENDING_KEY, String(text || '')); } catch (e) { /* ignore */ }
};

export const takePendingRebateInput = () => {
  let v = '';
  try {
    v = uni.getStorageSync(PENDING_KEY) || '';
    if (v) uni.removeStorageSync(PENDING_KEY);
  } catch (e) { /* ignore */ }
  return v;
};
