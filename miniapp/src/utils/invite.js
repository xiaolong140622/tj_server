import { editUser } from '../api/user';
import { getToken } from './auth';

// 邀请归因（JAVA seq-177）：扫码进入小程序时 scene 形如 "c=<邀请码>"；
// 注册/登录后走 POST /user/edit {code} 绑定邀请人。
const INVITE_KEY = 'invite_code';

export const captureInviteScene = (query) => {
  try {
    const scene = query && query.scene ? decodeURIComponent(query.scene) : '';
    const m = scene.match(/(?:^|[&;])c=([^&;]+)/);
    const code = (m && m[1]) || query?.c || query?.code || '';
    if (code && !uni.getStorageSync(INVITE_KEY)) uni.setStorageSync(INVITE_KEY, code);
  } catch (e) { /* ignore */ }
};

// 有 token 且有未绑定邀请码时提交一次；成功即清除，失败保留待下次冷启/登录重试
export const flushInviteCode = async () => {
  let code = '';
  try { code = uni.getStorageSync(INVITE_KEY); } catch (e) { return; }
  if (!code || !getToken()) return;
  try {
    await editUser({ code }, { silent: true });
    uni.removeStorageSync(INVITE_KEY);
  } catch (e) { /* 保留邀请码 */ }
};
