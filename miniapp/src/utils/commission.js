import { ref } from 'vue';
import { get } from './request';

// 平台返佣系数（JAVA seq-177 已确认口径）：预估奖励 = 价格 × 系数/100；
// times 系列字段是会员倍数（现为 1.0），不用于奖励系数。
const SCALE_KEYS = {
  tb: 'tbRebateScale',
  jd: 'jdHbRebateScale',
  pdd: 'pddHbRebateScale',
  dy: 'dyHbRebateScale',
  vip: 'vipHbRebateScale',
};

const commissionInfo = ref(null);
let loading = null;

export const loadCommissionInfo = () => {
  if (commissionInfo.value) return Promise.resolve(commissionInfo.value);
  if (!loading) {
    loading = get('/commissionInfo', {}, { silent: true })
      .then((res) => {
        commissionInfo.value = res.result || res.data || {};
        return commissionInfo.value;
      })
      .catch(() => {
        loading = null; // 允许下次重试；取不到系数走「奖励 待计算」降级
        return null;
      });
  }
  return loading;
};

// 返回两位小数字符串；系数或价格不可用时返回 null（调用方显示「奖励 待计算」）
export const estimateReward = (price, platform) => {
  const info = commissionInfo.value;
  if (!info) return null;
  const scale = Number(info[SCALE_KEYS[platform] || SCALE_KEYS.tb]);
  const p = Number(price);
  if (!Number.isFinite(scale) || scale <= 0 || !Number.isFinite(p) || p <= 0) return null;
  return ((p * scale) / 100).toFixed(2);
};
