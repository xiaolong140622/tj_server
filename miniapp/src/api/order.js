import { get } from '../utils/request';

export const getTbOrders = (params, opts) => get('/tao/orders', params, opts);
export const getJdOrders = (params, opts) => get('/jd/orders', params, opts);
export const getPddOrders = (params, opts) => get('/pdd/orders', params, opts);
export const getDyOrders = (params, opts) => get('/dy/orders', params, opts);
export const getOrderData = () => get('/order/data');
export const hasUnlockOrder = () => get('/hasUnlockOrder');
