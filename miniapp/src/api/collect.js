import { post, get } from '../utils/request';

export const addCollect = (data) => post('/collect/add', data);
export const delCollect = (data) => post('/collect/del', data);
export const getCollectList = (params) => get('/collect', params);
export const addFootprint = (data) => post('/collect/addFoot', data);
