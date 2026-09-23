const MOCK_BANNERS = [
  { id: 1, pic: 'https://picsum.photos/750/280?random=1', url: '' },
  { id: 2, pic: 'https://picsum.photos/750/280?random=2', url: '' },
  { id: 3, pic: 'https://picsum.photos/750/280?random=3', url: '' },
];

const MOCK_CATEGORIES = [
  { id: 1, name: '女装', pic: 'https://picsum.photos/80/80?random=10' },
  { id: 2, name: '男装', pic: 'https://picsum.photos/80/80?random=11' },
  { id: 3, name: '美妆', pic: 'https://picsum.photos/80/80?random=12' },
  { id: 4, name: '食品', pic: 'https://picsum.photos/80/80?random=13' },
  { id: 5, name: '数码', pic: 'https://picsum.photos/80/80?random=14' },
  { id: 6, name: '家居', pic: 'https://picsum.photos/80/80?random=15' },
  { id: 7, name: '母婴', pic: 'https://picsum.photos/80/80?random=16' },
  { id: 8, name: '运动', pic: 'https://picsum.photos/80/80?random=17' },
  { id: 9, name: '鞋包', pic: 'https://picsum.photos/80/80?random=18' },
  { id: 10, name: '更多', pic: 'https://picsum.photos/80/80?random=19' },
];

const makeProduct = (i, platform) => ({
  id: `${platform}_${1000 + i}`,
  itemId: `${platform}_${1000 + i}`,
  title: `${['热卖', '爆款', '推荐', '新品', '限时'][i % 5]}${['女装', '数码', '美妆', '食品', '家居'][i % 5]}商品${i + 1} 优质好物推荐`,
  mainPic: `https://picsum.photos/300/300?random=${20 + i}`,
  price: (Math.random() * 500 + 10).toFixed(2),
  originalPrice: (Math.random() * 800 + 100).toFixed(2),
  commission: (Math.random() * 50 + 5).toFixed(2),
  estimateCommission: (Math.random() * 50 + 5).toFixed(2),
  couponAmount: [5, 10, 15, 20, 30][i % 5],
  freeShipping: i % 2 === 0,
  shopType: ['天猫', '淘宝', '京东自营', '拼多多', '抖音'][i % 5],
  tags: [['包邮', '热卖'], ['新品', '推荐'], ['限时', '折扣'], ['爆款', '好评'], ['特价', ' clearance']][i % 5],
  volume: Math.floor(Math.random() * 10000),
  platform,
});

export const mockProducts = (platform = 'tb', count = 20) =>
  Array.from({ length: count }, (_, i) => makeProduct(i, platform));

export const mockBanners = () => MOCK_BANNERS;
export const mockCategories = () => MOCK_CATEGORIES;
export const mockHotWord = () => '夏季清凉好物';

export const mockProductDetail = (platform = 'tb') => ({
  id: `${platform}_9999`,
  title: '2026新款夏季清凉透气防晒衣 女薄款外套 防紫外线遮阳衫 百搭开衫',
  mainPic: 'https://picsum.photos/750/750?random=100',
  images: [
    'https://picsum.photos/750/750?random=100',
    'https://picsum.photos/750/750?random=101',
    'https://picsum.photos/750/750?random=102',
    'https://picsum.photos/750/750?random=103',
  ].join('|'),
  price: '89.90',
  originalPrice: '199.00',
  commission: '22.50',
  estimateCommission: '22.50',
  couponAmount: 20,
  freeShipping: true,
  shopType: '天猫',
  shopName: '清凉旗舰店',
  detail: '<div style="text-align:center"><img src="https://picsum.photos/750/1000?random=200" /><img src="https://picsum.photos/750/1000?random=201" /></div>',
  platform,
});

const ORDER_STATUSES = [1, 1, 1, 2, 2, 3, 3, 1, 2, 3];
const PLATFORMS = ['tb', 'jd', 'pdd', 'dy'];

export const mockOrders = (platform = 'tb', count = 20) =>
  Array.from({ length: count }, (_, i) => ({
    id: `ord_${platform}_${i}`,
    orderKey: `ord_${platform}_${i}`,
    title: `${['夏季', '冬季', '春季', '秋季'][i % 4]}${['女装', '数码', '食品', '美妆', '家居'][i % 5]}商品${i + 1}`,
    mainPic: `https://picsum.photos/200/200?random=${50 + i}`,
    pic: `https://picsum.photos/200/200?random=${50 + i}`,
    amount: (Math.random() * 500 + 20).toFixed(2),
    payPrice: (Math.random() * 500 + 20).toFixed(2),
    hb: (Math.random() * 50 + 2).toFixed(2),
    commission: (Math.random() * 50 + 2).toFixed(2),
    status: ORDER_STATUSES[i % ORDER_STATUSES.length],
    platform: PLATFORMS[i % PLATFORMS.length],
    orderTime: `2026-08-${String(20 + (i % 11)).padStart(2, '0')} 14:${String(10 + i).padStart(2, '0')}:00`,
    settleTime: i % 3 === 0 ? `2026-09-${String(5 + (i % 15)).padStart(2, '0')}` : '',
  }));

export const mockOrderDetail = (id = 'ord_tb_0', platform = 'tb') => ({
  id,
  orderKey: `TB${Date.now().toString().slice(-10)}`,
  title: '2026新款夏季清凉透气防晒衣 女薄款外套 防紫外线遮阳衫',
  mainPic: 'https://picsum.photos/200/200?random=50',
  pic: 'https://picsum.photos/200/200?random=50',
  amount: '129.90',
  payPrice: '109.90',
  commission: '18.50',
  hb: '14.80',
  status: 1,
  platform,
  orderTime: '2026-08-25 14:32:00',
  createTime: '2026-08-25 14:32:00',
  payTime: '2026-08-25 14:32:15',
  settleTime: '',
  unlockTime: '2026-09-10',
  shopName: '清凉旗舰店',
  couponAmount: 20,
  estimateCommission: '14.80',
  timeline: [
    { status: 1, label: '订单创建', time: '2026-08-25 14:32:00', done: true },
    { status: 2, label: '待结算', time: '预计2026-09-10', done: false },
    { status: 3, label: '已到账', time: '', done: false },
  ],
});

export const mockBalance = () => ({
  totalMoney: 2580.66,
  pendingMoney: 860.30,
  settledMoney: 1720.36,
  total: 2580.66,
  pending: 860.30,
  settled: 1720.36,
});

const BILL_TYPES = [
  { title: '自购奖励', mark: '自购奖励-淘宝订单' },
  { title: '分享奖励', mark: '分享奖励-用户***8' },
  { title: '订单奖励', mark: '订单奖励-京东订单' },
  { title: '奖励到账', mark: '奖励到账-余额' },
];

export const mockBills = (count = 20) =>
  Array.from({ length: count }, (_, i) => {
    const isWithdraw = i % 9 === 8;
    const type = BILL_TYPES[i % 4];
    return {
      id: `bill_${i}`,
      title: isWithdraw ? '余额转出' : type.title,
      mark: isWithdraw ? '余额转出-微信零钱' : type.mark,
      type: isWithdraw ? 'withdraw' : ['self', 'share', 'order', 'income'][i % 4],
      amount: isWithdraw
        ? -(Math.random() * 100 + 50).toFixed(2)
        : (Math.random() * 50 + 1).toFixed(2),
      createTime: `2026-08-${String(30 - (i % 28)).padStart(2, '0')} ${String(8 + (i % 14)).padStart(2, '0')}:${String(10 + i).padStart(2, '0')}:00`,
    };
  });

export const mockRecentIncome = () => [
  { id: 'in_1', title: '自购奖励到账', amount: '12.50', createTime: '2026-08-30 08:00:00' },
  { id: 'in_2', title: '分享奖励到账', amount: '8.60', createTime: '2026-08-29 08:00:00' },
  { id: 'in_3', title: '自购奖励到账', amount: '18.30', createTime: '2026-08-28 08:00:00' },
  { id: 'in_4', title: '分享奖励到账', amount: '22.40', createTime: '2026-08-27 08:00:00' },
  { id: 'in_5', title: '自购奖励到账', amount: '6.20', createTime: '2026-08-26 08:00:00' },
];

export const mockHotKeywords = () => [
  '夏季清凉好物', '防晒霜推荐', '数码好物', '家居收纳',
  '运动鞋特价', '美妆护肤', '零食大礼包', '手机壳',
  '连衣裙夏', '蓝牙耳机',
];

export const mockSpreadCode = () => ({
  code: 'https://sufenbao.com/spread/abc123',
  qrcode: 'https://picsum.photos/320/320?random=300',
});

export const mockSpreadStats = () => ({
  peopleCount: 36,
  orderCount: 128,
  commission: 1260.50,
});

export const mockSpreadPeople = () =>
  Array.from({ length: 8 }, (_, i) => ({
    uid: 1000 + i,
    nickname: `用户${String(8800 + i).slice(-4)}`,
    avatar: `https://picsum.photos/80/80?random=${400 + i}`,
    orderCount: Math.floor(Math.random() * 20),
    commission: (Math.random() * 200 + 5).toFixed(2),
  }));

export const mockUserInfo = () => ({
  uid: 10086,
  nickname: '苏分宝用户',
  avatar: 'https://picsum.photos/120/120?random=500',
  phone: '13812345678',
  mobile: '13812345678',
});
