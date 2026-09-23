import { TOKEN_KEY, USER_INFO_KEY } from './constants';

export const setToken = (token) => uni.setStorageSync(TOKEN_KEY, token);
export const getToken = () => uni.getStorageSync(TOKEN_KEY) || '';
export const removeToken = () => uni.removeStorageSync(TOKEN_KEY);
export const isLoggedIn = () => !!getToken();

export const setUserInfo = (info) => uni.setStorageSync(USER_INFO_KEY, JSON.stringify(info));
export const getUserInfo = () => {
  const raw = uni.getStorageSync(USER_INFO_KEY);
  return raw ? JSON.parse(raw) : null;
};
export const removeUserInfo = () => uni.removeStorageSync(USER_INFO_KEY);

export const clearAuth = () => {
  removeToken();
  removeUserInfo();
};
