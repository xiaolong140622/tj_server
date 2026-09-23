import { get, post } from '../utils/request';

export const getBalance = () => get('/user/balance');
export const getCommissionInfo = () => get('/commissionInfo');
export const getCommissionList = (params) => get('/commission', params);
export const getIntegralList = (params, opts) => get('/integral/list', params, opts);
export const getMenu = () => get('/menu/user');
export const editUser = (data, opts) => post('/user/edit', data, opts);
export const getSpreadCode = () => get('/user/spread');
export const getShareInfo = () => get('/user/share');
export const getBankList = () => get('/user/bank/list');
