// 商品字段适配层（P0-4）
// 四大平台返回结构各异，且后端 VO 用 @JSONField 别名（fastjson）与 Java 字段名（Jackson）
// 两套命名都可能到达前端，这里对每个语义字段做多候选防御式取值，统一成
// ProductCard / 商品详情页需要的形状：{id,title,mainPic,price,originalPrice,volume,commission,platform}
import { formatMoney } from './format';

// 从候选键里取第一个有值的字段
const pick = (obj, keys) => {
  if (!obj) return undefined;
  for (let i = 0; i < keys.length; i++) {
    const v = obj[keys[i]];
    if (v !== undefined && v !== null && v !== '') return v;
  }
  return undefined;
};

const toNumber = (v) => {
  if (v === undefined || v === null || v === '') return 0;
  const n = typeof v === 'string' ? parseFloat(v) : Number(v);
  return Number.isFinite(n) ? n : 0;
};

// 各平台"正确 id"字段优先级：PDD 详情必须用 goodsSign，JD 用 goodsId/itemId
// 注意 goodsSign 与 goodsId 在 PDD VO 里是同一个值（@JSONField 别名），两种命名都列上
const ID_KEYS = {
  tb: ['goodsId', 'itemId', 'numIid', 'id'],
  jd: ['goodsId', 'itemId', 'skuId', 'wareId', 'id'],
  pdd: ['goodsSign', 'goodsId', 'id'],
  dy: ['goodsId', 'itemid', 'itemId', 'productId', 'id'],
};

const TITLE_KEYS = ['title', 'goodsDesc', 'skuName', 'itemtitle', 'dtitle', 'goodsName', 'wareName', 'name'];
const PIC_KEYS = ['mainPic', 'pic', 'img', 'imageUrl', 'goodsImageUrl', 'item_pic', 'goodsThumbnailUrl', 'picUrl', 'whiteImg', 'whiteImage', 'cover', 'image'];
// 现价/券后价：优先各平台专属现价键，最后才回落到通用 price
const PRICE_KEYS = ['actualPrice', 'endPrice', 'end_price', 'minGroupPrice', 'lowestCouponPrice', 'zkFinalPrice', 'salePrice', 'price'];
// 原价：dy 的 fastjson 原价键恰好是 price，放在最后兜底
const ORIGINAL_PRICE_KEYS = ['originalPrice', 'startPrice', 'minNormalPrice', 'lowestPrice', 'reservePrice', 'originPrice', 'price'];
const VOLUME_KEYS = ['monthSales', 'sales', 'salesTip', 'inOrderCount30Days', 'volume', 'soldCount', 'totalSales', 'dailySales'];
const COMMISSION_KEYS = ['commission', 'fee', 'estimateCommission', 'commissionAmount', 'couponCommission', 'dymoney', 'preCommission'];
const RATE_KEYS = ['commissionRate', 'feeRatio', 'promotionRate', 'commissionShare', 'dyrates', 'tkRate'];
const COUPON_KEYS = ['couponPrice', 'couponAmount', 'coupon', 'couponDiscount', 'coupon_price', 'quanPrice'];
const SHOP_NAME_KEYS = ['shopName', 'mallName', 'shop_name', 'sellerNickName', 'teamName'];

// 归一化单个商品
export const normalizeProduct = (platform, raw) => {
  if (!raw || typeof raw !== 'object') return raw;
  const p = String(platform || raw.platform || 'tb').toLowerCase();

  const idKeys = ID_KEYS[p] || ID_KEYS.tb;
  const idVal = pick(raw, idKeys) ?? pick(raw, ['id', 'goodsId', 'itemId', 'goodsSign', 'skuId', 'itemid', 'productId']);
  const id = idVal === undefined ? '' : String(idVal);

  const title = pick(raw, TITLE_KEYS) || '';
  const mainPic = pick(raw, PIC_KEYS) || '';

  const price = toNumber(pick(raw, PRICE_KEYS));
  let originalPrice = toNumber(pick(raw, ORIGINAL_PRICE_KEYS));
  // 原价不大于现价时无意义（划线价异常），隐藏
  if (originalPrice <= price) originalPrice = 0;

  const volumeRaw = pick(raw, VOLUME_KEYS);
  const volume = typeof volumeRaw === 'string' && /[^\d.]/.test(volumeRaw)
    ? volumeRaw
    : toNumber(volumeRaw);

  // 预估奖励：后端直接给金额则用，否则按 现价 × 佣金比例 估算
  let commission = toNumber(pick(raw, COMMISSION_KEYS));
  if (!commission) {
    const rate = toNumber(pick(raw, RATE_KEYS));
    if (rate > 0 && price > 0) commission = rate > 1 ? (price * rate) / 100 : price * rate;
  }

  const couponAmount = toNumber(pick(raw, COUPON_KEYS));
  const shopName = pick(raw, SHOP_NAME_KEYS) || '';

  // 店铺类型标签：tb 用数字映射，jd 用 owner，其余回落到店铺名
  let shopType = pick(raw, ['shopTypeName']) || '';
  if (!shopType) {
    const st = pick(raw, ['shopType']);
    if (p === 'tb' || p === 'tm') {
      shopType = st === 1 || st === '1' ? '天猫' : (st === 0 || st === '0' ? '淘宝' : '');
    } else if (p === 'jd') {
      const owner = pick(raw, ['owner']);
      shopType = owner === 'g' ? '京东自营' : (owner === 'p' ? 'POP' : '');
    }
  }

  const freeShipping = raw.freeShipping === true || raw.freeShipping === 1 || raw.freeShipping === '1'
    || toNumber(pick(raw, ['shipPercent'])) === 100;

  return {
    ...raw, // 保留原始字段（详情页仍需 imgs/detailPics/gallery 等）
    id,
    platform: p,
    title,
    mainPic,
    price: formatMoney(price),
    originalPrice: originalPrice ? formatMoney(originalPrice) : '',
    volume,
    commission: formatMoney(commission),
    estimateCommission: formatMoney(commission),
    couponAmount,
    shopName,
    shopType,
    freeShipping: !!freeShipping,
    // 供详情页按平台映射后端参数键名（P0-2）：三者统一给平台正确 id 值
    goodsId: id,
    itemId: p === 'jd' ? id : pick(raw, ['itemId', 'itemid']) || id,
    goodsSign: p === 'pdd' ? id : pick(raw, ['goodsSign']) || '',
  };
};

// 归一化商品列表
export const normalizeProductList = (platform, list) =>
  Array.isArray(list) ? list.map((item) => normalizeProduct(platform, item)) : [];

export default normalizeProduct;
