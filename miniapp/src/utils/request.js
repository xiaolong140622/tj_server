import { BASE_URL, TOKEN_KEY } from './constants';

const getToken = () => uni.getStorageSync(TOKEN_KEY) || '';

// 多个请求同时 401 时，避免重复弹提示与重复跳转登录页
let redirectingToLogin = false;
const handleUnauthorized = () => {
  uni.removeStorageSync(TOKEN_KEY);
  if (redirectingToLogin) return;
  redirectingToLogin = true;
  uni.showToast({ title: '请先登录', icon: 'none' });
  uni.redirectTo({
    url: '/pages/login/index',
    complete: () => { redirectingToLogin = false; },
  });
};

const request = (options) => {
  return new Promise((resolve, reject) => {
    // silent=true 时失败不弹 toast，用于有 mock 降级的调用（如首页 banner）
    const silent = options.silent === true;
    const token = getToken();
    const header = {
      'Content-Type': 'application/json',
      ...options.header,
    };
    if (token) {
      header['Authorization'] = `Bearer ${token}`;
    }

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header,
      success: (res) => {
        // 传输层 401
        if (res.statusCode === 401) {
          handleUnauthorized();
          reject(new Error('未授权'));
          return;
        }
        if (res.statusCode === 200) {
          const data = res.data;
          if (!data || typeof data !== 'object') {
            if (!silent) uni.showToast({ title: '数据异常', icon: 'none' });
            reject(res);
            return;
          }
          // 信封级 401：后端经 GlobalExceptionHandler 返回 HTTP200 + {status:401,success:false}
          // 必须清 token 并跳登录，且不能把含内网地址的原始 msg 回显给用户
          if (data.status === 401 || data.code === 401) {
            handleUnauthorized();
            reject(data);
            return;
          }
          if (
            data.success === true ||
            data.status === 200 ||
            data.code === 200 ||
            data.code === 0
          ) {
            resolve(data);
          } else {
            const msg = data.msg || data.message || '请求失败';
            if (!silent) uni.showToast({ title: msg, icon: 'none' });
            reject(data);
          }
        } else {
          if (!silent) uni.showToast({ title: '服务异常', icon: 'none' });
          reject(res);
        }
      },
      fail: (err) => {
        if (!silent) uni.showToast({ title: '网络异常', icon: 'none' });
        reject(err);
      },
    });
  });
};

export const get = (url, data, options = {}) => request({ url, method: 'GET', data, ...options });
export const post = (url, data, options = {}) => request({ url, method: 'POST', data, ...options });
export const put = (url, data, options = {}) => request({ url, method: 'PUT', data, ...options });
export const del = (url, data, options = {}) => request({ url, method: 'DELETE', data, ...options });

export default request;
