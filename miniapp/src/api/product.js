import { get } from '../utils/request';

// P0-2：各平台详情接口后端键名不同（tb/dy→goodsId，jd→goodsId+itemId，pdd→goodsSign）。
// 统一封装，非破坏性：入参可为标量 id，也可为对象（自动挑出后端需要的键，
// 并兼容 id/goodsId/itemId/goodsSign/skuId 等别名）。
const detailParams = (arg, keys) => {
  const out = {};
  if (arg && typeof arg === 'object') {
    keys.forEach((k) => { if (arg[k] !== undefined && arg[k] !== null && arg[k] !== '') out[k] = arg[k]; });
    if (out[keys[0]] === undefined || out[keys[0]] === null || out[keys[0]] === '') {
      const fallback = arg.id ?? arg.goodsId ?? arg.itemId ?? arg.goodsSign ?? arg.skuId ?? arg.itemid ?? arg.productId;
      if (fallback !== undefined && fallback !== null && fallback !== '') keys.forEach((k) => { if (out[k] === undefined) out[k] = fallback; });
    }
    return out;
  }
  keys.forEach((k) => { out[k] = arg; });
  return out;
};

export const getTbGoodsList = (params, opts) => get('/tao/goods/list', params, opts);
export const searchTbGoods = (params, opts) => get('/tao/goods/search', params, opts);
export const getTbGoodsDetail = (params, opts) => get('/tao/goods/detail', params, opts);
export const getTbGoodsWord = (params, opts) => get('/tao/goods/word', params, opts);
export const getTbCategory = (opts) => get('/tao/goods/category', {}, opts);
export const getTbSimilar = (params, opts) => get('/tao/goods/similar/list', params, opts);
export const getTbTljList = (params, opts) => get('/tao/tlj/goods/list', params, opts);

export const getJdGoodsDetail = (params, opts) => get('/jd/goods/detail', params, opts);
export const getJdGoodsWord = (params, opts) => get('/jd/goods/word', params, opts);
export const getJdRankList = (params, opts) => get('/jd/rank/list', params, opts);
// JD 商品搜索（JAVA seq-177 P1-4，好单库透传）：keyword/pageId/pageSize/sortName/sort，响应 {code,msg,data:[...]}
export const searchJdGoods = (params, opts) => get('/jd/goods/search', params, opts);

export const getPddGoodsList = (params, opts) => get('/pdd/goods/list', params, opts);
export const getPddCate = (opts) => get('/pdd/goods/cate', {}, opts);
export const getPddGoodsDetail = (params, opts) => get('/pdd/goods/detail', params, opts);
export const getPddGoodsWord = (params, opts) => get('/pdd/goods/word', params, opts);

export const searchDyGoods = (params, opts) => get('/dy/goods/search', params, opts);
export const getDyGoodsDetail = (params, opts) => get('/dy/goods/detail', params, opts);
export const getDyGoodsWord = (params, opts) => get('/dy/word', params, opts);

export const getPddNav = (opts) => get('/ku/pdd/nav', {}, opts);
export const getPddKuList = (params, opts) => get('/ku/pdd/list', params, opts);
export const getDyNav = (opts) => get('/ku/dy/nav', {}, opts);
export const getDyKuList = (params, opts) => get('/ku/dy/list', params, opts);
export const getPickCate = (opts) => get('/ku/pick/cate', {}, opts);
export const getPickList = (params, opts) => get('/ku/pick/list', params, opts);
export const getMiniList = (params, opts) => get('/ku/mini/list', params, opts);
export const getBanner = (opts) => get('/ku/banner', {}, opts);
// 首页轮播新通道（JAVA seq-177 P1-3，无鉴权）：data [{id,imageUrl,title,type,target}]
export const getHomeBanner = (opts) => get('/home/banner', {}, opts);
export const getNineCate = (opts) => get('/ku/nine/cate', {}, opts);
export const getNineList = (params, opts) => get('/ku/nine/list', params, opts);

export const getSearchHot = (opts) => get('/search/keyword', {}, opts);
export const getCategoryList = (opts) => get('/category', {}, opts);
