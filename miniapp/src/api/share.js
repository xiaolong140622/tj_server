import { get, post } from '../utils/request';

export const getSpreadCode = (opts) => get('/spread/code', {}, opts);
export const getSpreadSummary = (params, opts) => get('/spread/summary', params, opts);
export const getSpreadBanner = () => get('/spread/banner');
export const getSpreadPeople = (params, opts) => post('/spread/people', params, opts);
export const getSpreadCommission = (type) => get(`/spread/commission/${type}`);
export const getSpreadOrder = (params) => post('/spread/order', params);
export const getSpreadHb = () => get('/spread/hb');
export const getShareConfig = () => get('/share');
