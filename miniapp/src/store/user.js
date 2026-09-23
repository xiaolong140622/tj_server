import { defineStore } from 'pinia';
import { ref } from 'vue';
import { getUserInfo as fetchUserInfo } from '../api/auth';
import { setToken, getToken, removeToken, setUserInfo, getUserInfo as loadUserInfo, clearAuth } from '../utils/auth';
import { flushInviteCode } from '../utils/invite';

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken());
  const userInfo = ref(loadUserInfo());
  const isLoggedIn = ref(!!getToken());

  const setLoginInfo = (newToken, info) => {
    token.value = newToken;
    userInfo.value = info;
    isLoggedIn.value = true;
    setToken(newToken);
    setUserInfo(info);
    flushInviteCode(); // 登录即绑定扫码带入的邀请码（若有）
  };

  const refreshUserInfo = async () => {
    try {
      const res = await fetchUserInfo();
      userInfo.value = res.result || res.data;
      setUserInfo(userInfo.value);
    } catch (e) {
      console.error('refreshUserInfo failed', e);
    }
  };

  const doLogout = () => {
    token.value = '';
    userInfo.value = null;
    isLoggedIn.value = false;
    clearAuth();
  };

  return { token, userInfo, isLoggedIn, setLoginInfo, refreshUserInfo, doLogout };
});
