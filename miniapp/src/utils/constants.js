export const COPYWRITING = {
  COMMISSION_LABEL: '预估奖励',
  CASHOUT_LABEL: '到账',
  WITHDRAW_LABEL: '余额转出',
  SHARE_BUTTON: '分享给好友省更多',
  SHARE_POSTER_TITLE: '邀请好友一起省',
  SHARE_CARD_TITLE: '发现好物推荐给你',
  ESTIMATED_PREFIX: '预估',
  SPREAD_REWARD_LABEL: '推广奖励',
  REWARD_DISCLAIMER: '预估奖励金额仅供参考，实际以平台结算为准',
};

export const PLATFORM = {
  TB: { value: 'tb', label: '淘宝', icon: '/static/platform/tb.png' },
  JD: { value: 'jd', label: '京东', icon: '/static/platform/jd.png' },
  PDD: { value: 'pdd', label: '拼多多', icon: '/static/platform/pdd.png' },
  DY: { value: 'dy', label: '抖音', icon: '/static/platform/dy.png' },
};

export const PLATFORM_LIST = [
  PLATFORM.TB,
  PLATFORM.JD,
  PLATFORM.PDD,
  PLATFORM.DY,
];

export const ORDER_STATUS = {
  PENDING: { value: 1, label: '待结算' },
  SETTLED: { value: 2, label: '已结算' },
  COMPLETED: { value: 3, label: '已到账' },
};

// 账单业务名兜底映射（JAVA seq-177 契约广播）：title 非空优先展示后端 title，缺失按 type 映射。
// 库内返利奖励实际写 retail（自购+分享共用，sourceType 区分），无 self_purchase。
export const BILL_TYPE_LABELS = {
  retail: '返利奖励',
  brokerage: '分享奖励',
  extract: '提现出账',
  recharge: '充值',
  pay_product: '订单支付',
  pay_product_refund: '订单退款',
  system_add: '系统增加',
  system_sub: '系统扣减',
  deduction: '扣减',
  gain: '获得',
  sign: '签到奖励',
  order: '订单奖励',
  upgrade: '升级奖励',
};
export const BILL_DEFAULT_LABEL = '其他';

// 账单筛选 Tab（JAVA seq-177 口径）：query 直接并入 /integral/list 参数
export const BILL_FILTER_TABS = [
  { id: '', label: '全部', query: {} },
  { id: 'self', label: '自购奖励', query: { type: 'retail', sourceType: 'self' } },
  { id: 'share', label: '分享奖励', query: { type: 'retail', sourceType: 'share' } },
  { id: 'extract', label: '提现出账', query: { type: 'extract' } },
];

// 联调环境（本机后端 App 服务）。提审上线前需替换为已备案的 https 域名。
export const BASE_URL = 'http://192.168.10.201:8008/api';

export const TOKEN_KEY = 'token';
export const USER_INFO_KEY = 'user_info';
