import { post, get } from '../utils/request';

export const wxLogin = (code) => post('/wxapp/login', { code });
export const bindMobile = (data) => post('/user/bind/mobile', data);
export const sendSmsCode = (phone) => post('/register/verify', { phone });
export const logout = () => post('/auth/logout');
export const getUserInfo = () => get('/userinfo');
