<template>
  <view class="page-order-list">
    <PlatformTab :current="currentPlatform" @change="onPlatformChange" />

    <view class="status-tabs">
      <view
        v-for="s in statusTabs"
        :key="s.value"
        class="status-tab"
        :class="{ active: currentStatus === s.value }"
        @click="onStatusChange(s.value)"
      >
        <text class="status-tab-text">{{ s.label }}</text>
        <view class="status-tab-indicator" v-if="currentStatus === s.value"></view>
      </view>
    </view>

    <scroll-view
      scroll-y
      class="order-scroll"
      @scrolltolower="loadMore"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onPullRefresh"
    >
      <view class="order-list" v-if="orderList.length">
        <view
          class="order-card"
          v-for="item in orderList"
          :key="item.id"
          @click="goDetail(item)"
          hover-class="order-card--hover"
        >
          <view class="order-card-header">
            <view class="order-platform-wrap">
              <view class="order-platform-dot" :style="{ background: getPlatformColor(item) }"></view>
              <text class="order-platform-text">{{ getPlatformLabel(item) }}</text>
            </view>
            <text class="order-status-text" :class="getStatusClass(item)">{{ getStatusLabel(item) }}</text>
          </view>
          <view class="order-card-body">
            <view class="order-img-wrap">
              <image
                v-if="itemPic(item) && !item._picError"
                class="order-img"
                :src="itemPic(item)"
                mode="aspectFill"
                lazy-load
                @error="item._picError = true"
              />
              <view v-else class="order-img-ph" :style="{ backgroundImage: imgIcon }"></view>
            </view>
            <view class="order-card-info">
              <text class="order-card-title">{{ itemTitle(item) }}</text>
              <view class="order-card-meta">
                <text class="order-card-amount">¥{{ formatMoney(item.amount || item.payPrice) }}</text>
                <text class="order-card-time">{{ formatOrderTime(item.orderTime || item.createTime) }}</text>
              </view>
              <view class="order-card-reward">
                <!-- B-2 补贴字段（冻结契约 jd-channel-contract-v1.md）：null 即隐藏，禁 0 填充 -->
                <view class="reward-subsidy" v-if="subsidyTags(item).length">
                  <text class="reward-subsidy-chip" v-for="t in subsidyTags(item)" :key="t.key">{{ t.label }}</text>
                </view>
                <text class="reward-label">{{ COPYWRITING.COMMISSION_LABEL }}</text>
                <text class="reward-value">¥{{ formatMoney(item.hb || item.commission) }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 首载/切换骨架（子页面规范 §5：tab 切换也算首载） -->
      <view class="order-skel-list" v-if="loading && !orderList.length && !loadError">
        <view class="order-skel-card" v-for="i in 3" :key="i">
          <view class="skel skel-head"></view>
          <view class="skel-body">
            <view class="skel skel-img"></view>
            <view class="skel-lines">
              <view class="skel skel-line1"></view>
              <view class="skel skel-line2"></view>
              <view class="skel skel-line3"></view>
            </view>
          </view>
        </view>
      </view>

      <FailRetry v-if="loadError && !orderList.length" @retry="loadOrders(true)" />

      <view class="load-end" v-if="!hasMore && orderList.length">
        <view class="load-end-line"></view>
        <text class="load-end-text">已经到底了</text>
        <view class="load-end-line"></view>
      </view>

      <Loading :visible="loading && orderList.length" text="加载中..." />
      <Empty
        v-if="!loading && !loadError && !orderList.length"
        :text="emptyText"
        :action-text="currentStatus === 0 ? '去首页逛逛' : ''"
        @action="goHome"
      />
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import PlatformTab from '../../components/PlatformTab.vue';
import Loading from '../../components/Loading.vue';
import Empty from '../../components/Empty.vue';
import FailRetry from '../../components/FailRetry.vue';
import { getTbOrders, getJdOrders, getPddOrders, getDyOrders } from '../../api/order';
import { PLATFORM_LIST, ORDER_STATUS, COPYWRITING } from '../../utils/constants';
import { formatMoney } from '../../utils/format';
import { normalizeJdOrder } from '../../utils/order';
import { subsidyTags } from '../../utils/subsidy';
import { ICONS } from '../../utils/icons';
import { mockOrders } from '../../mock/index';

const USE_MOCK = false; // 显式开发开关，默认关（PRD §0-2）；正式链路禁止 catch→mock

const currentPlatform = ref('tb');
const currentStatus = ref(0);
const orderList = ref([]);
const loading = ref(false);
const loadError = ref(false);
const refreshing = ref(false);
const page = ref(1);
const hasMore = ref(true);

const imgIcon = ICONS.image('#FF8F65');

// 三态：全部/待结算/已结算（JAVA seq-177：「已到账」属账单域提现出账，订单域无独立状态；产品定稿前按此执行）
const statusTabs = [
  { value: 0, label: '全部' },
  { value: ORDER_STATUS.PENDING.value, label: '待结算' },
  { value: ORDER_STATUS.SETTLED.value, label: '已结算' },
];

// 状态 → 各平台服务端筛选参数（JAVA seq-177 契约广播）
const STATUS_PARAMS = {
  tb: { 1: { tkStatus: 12 }, 2: { tkStatus: 3 } },
  jd: { 1: { validCode: 16 }, 2: { validCode: 17 } },
  pdd: { 1: { orderStatusIn: '0,1,2,3' }, 2: { orderStatus: 5 } },
  dy: { 1: { flowPoint: 'PAY_SUCC' }, 2: { flowPoint: 'CONFIRM' } },
};

const emptyText = computed(() =>
  currentStatus.value === ORDER_STATUS.PENDING.value ? '暂无待结算订单' : '还没有订单哦，去逛逛吧');
const goHome = () => uni.switchTab({ url: '/pages/index/index' });

const itemPic = (item) => item.pic || item.mainPic || '';
const itemTitle = (item) => item.title || item.orderTitle || '';

const PLATFORM_COLORS = { tb: '#ff4d4f', jd: '#e4393c', pdd: '#e02e24', dy: '#161823' };

const getPlatformLabel = (item) => {
  const p = PLATFORM_LIST.find(p => p.value === (item.platform || currentPlatform.value));
  return p ? p.label : '';
};

const getPlatformColor = (item) => {
  return PLATFORM_COLORS[item.platform || currentPlatform.value] || '#ff6b35';
};

// 行状态推导：后端行沿用平台原生字段（tkStatus/validCode/orderStatus/flowPoint），
// 按 JAVA seq-177 语义映射到本地三态；有归一化 status 字段时优先
const deriveStatus = (item) => {
  if (item.status === 1 || item.status === 2 || item.status === 3) return item.status;
  const p = item.platform || currentPlatform.value;
  if (p === 'tb') { const t = Number(item.tkStatus); if (t === 12) return 1; if (t === 3) return 2; }
  if (p === 'jd') { const v = Number(item.validCode); if (v === 16) return 1; if (v === 17) return 2; }
  if (p === 'pdd') { const s = Number(item.orderStatus); if (s === 5) return 2; if (s >= 0 && s <= 3) return 1; }
  if (p === 'dy') { const f = item.flowPoint; if (f === 'PAY_SUCC') return 1; if (f === 'CONFIRM') return 2; }
  return 0;
};

const getStatusLabel = (item) => {
  const v = deriveStatus(item);
  const s = Object.values(ORDER_STATUS).find((s) => s.value === v);
  return s ? s.label : '';
};

const getStatusClass = (item) => {
  const v = deriveStatus(item);
  if (v === ORDER_STATUS.COMPLETED.value) return 'status--completed';
  if (v === ORDER_STATUS.SETTLED.value) return 'status--settled';
  if (v === ORDER_STATUS.PENDING.value) return 'status--pending';
  return 'status--invalid';
};

const formatOrderTime = (t) => {
  if (!t) return '';
  return String(t).substring(0, 10);
};

// 后端分页包装统一适配：PageResult.content（订单四平台接口）与旧 list 双兼容
const pickRows = (res) => {
  const d = res?.result ?? res?.data ?? res ?? {};
  if (Array.isArray(d)) return d;
  return d.content || d.list || d.rows || [];
};

const mockFetch = async () => {
  await new Promise(r => setTimeout(r, 300));
  const list = mockOrders(currentPlatform.value, 20);
  const filtered = currentStatus.value ? list.filter(o => o.status === currentStatus.value) : list;
  orderList.value = [...orderList.value, ...filtered];
  if (filtered.length < 20) hasMore.value = false;
  page.value++;
};

const loadOrders = async (reset = false) => {
  if (loading.value) return;
  if (!reset && !hasMore.value) return;
  if (reset) { page.value = 1; hasMore.value = true; orderList.value = []; loadError.value = false; }
  loading.value = true;
  if (USE_MOCK) { await mockFetch(); loading.value = false; return; }
  try {
    const params = { page: page.value, limit: 20 };
    // 服务端状态筛选：按平台映射契约参数（JAVA seq-177）
    if (currentStatus.value) {
      const sp = STATUS_PARAMS[currentPlatform.value] && STATUS_PARAMS[currentPlatform.value][currentStatus.value];
      if (sp) Object.assign(params, sp);
    }
    let res;
    switch (currentPlatform.value) {
      case 'tb': res = await getTbOrders(params, { silent: true }); break;
      case 'jd': res = await getJdOrders(params, { silent: true }); break;
      case 'pdd': res = await getPddOrders(params, { silent: true }); break;
      case 'dy': res = await getDyOrders(params, { silent: true }); break;
      default: res = await getTbOrders(params, { silent: true });
    }
    let list = pickRows(res);
    // JD 行字段适配（MailvorJdOrderDto → 视图通用字段；utils/order.js，字段源自仓库已提交 DTO）
    if (currentPlatform.value === 'jd') list = list.map(normalizeJdOrder);
    if (list.length < 20) hasMore.value = false;
    orderList.value = reset ? list : [...orderList.value, ...list];
    page.value++;
    loadError.value = false;
  } catch (e) {
    if (reset) {
      loadError.value = true; // 失败整卡重试，禁止 mock 回落（PRD §0-2）
    } else {
      hasMore.value = false;
      uni.showToast({ title: '加载失败，下拉可重试', icon: 'none' });
    }
  } finally {
    loading.value = false;
  }
};

const loadMore = () => loadOrders();
const onPullRefresh = async () => {
  refreshing.value = true;
  await loadOrders(true);
  refreshing.value = false;
};
const onPlatformChange = (p) => { currentPlatform.value = p; loadOrders(true); };
const onStatusChange = (s) => { currentStatus.value = s; loadOrders(true); };
const goDetail = (item) => uni.navigateTo({ url: `/pages/order/detail?key=${item.orderKey || item.id}&platform=${item.platform || currentPlatform.value}` });

onShow(() => loadOrders(true));
</script>

<style scoped>
.page-order-list {
  min-height: 100vh;
  background: #F6F7F9;
}

/* A 型状态 tab（子页面规范 §2） */
.status-tabs {
  display: flex;
  background: #fff;
  padding: 0 12rpx;
  border-bottom: 1rpx solid #F2F3F5;
}
.status-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 0 16rpx;
  position: relative;
}
.status-tab-text {
  font-size: 28rpx;
  color: #7A7F89;
  transition: all 0.25s ease;
}
.status-tab.active .status-tab-text {
  color: #FF6B35;
  font-weight: 600;
}
.status-tab-indicator {
  position: absolute;
  bottom: 0;
  width: 48rpx;
  height: 6rpx;
  background: linear-gradient(90deg, #ff6b35, #ff8f65);
  border-radius: 3rpx;
}

.order-scroll {
  height: calc(100vh - 200rpx);
}

.order-list {
  padding: 16rpx 0;
}

.order-card {
  background: #fff;
  margin: 0 24rpx 16rpx;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 32rpx rgba(31, 33, 38, 0.06);
  transition: transform 0.15s ease;
}
.order-card--hover {
  transform: scale(0.98);
  opacity: 0.92;
}

.order-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18rpx 24rpx;
  border-bottom: 1rpx solid #F2F3F5;
}
.order-platform-wrap {
  display: flex;
  align-items: center;
  gap: 10rpx;
}
.order-platform-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
}
.order-platform-text {
  font-size: 24rpx;
  color: #1F2126;
  font-weight: 600;
}
/* 状态词色（行型 2）：待结算橙黄/已结算绿/已到账主色/失效灰，灰不再混淆「失效」 */
.order-status-text {
  font-size: 24rpx;
  font-weight: 500;
}
.status--pending { color: #FA8C16; }
.status--settled { color: #07C160; }
.status--completed { color: #FF6B35; }
.status--invalid { color: #B6BAC2; }

.order-card-body {
  display: flex;
  padding: 20rpx 24rpx;
  gap: 20rpx;
}
.order-img-wrap {
  width: 140rpx;
  height: 140rpx;
  border-radius: 16rpx;
  overflow: hidden;
  flex-shrink: 0;
}
.order-img {
  width: 100%;
  height: 100%;
}
/* 商品图为空/加载失败：浅橙占位+线性图标，不留白块 */
.order-img-ph {
  width: 100%;
  height: 100%;
  background-color: #FFF1EC;
  background-repeat: no-repeat;
  background-position: center;
  background-size: 56rpx;
}
.order-card-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}
.order-card-title {
  font-size: 26rpx;
  color: #1F2126;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.order-card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.order-card-amount {
  font-size: 30rpx;
  color: #1F2126;
  font-weight: 700;
}
.order-card-time {
  font-size: 22rpx;
  color: #B6BAC2;
}
.order-card-reward {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8rpx;
  background: linear-gradient(135deg, #fff7f0, #fff0e6);
  border-radius: 10rpx;
  padding: 8rpx 16rpx;
  align-self: flex-start;
}
.reward-subsidy {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  width: 100%;
}
.reward-subsidy-chip {
  font-size: 20rpx;
  color: #FF6B35;
  background: #FFF1EC;
  border: 1rpx solid rgba(255, 107, 53, 0.2);
  border-radius: 8rpx;
  padding: 2rpx 10rpx;
}
.reward-label {
  font-size: 22rpx;
  color: #ff8f65;
}
.reward-value {
  font-size: 26rpx;
  color: #ff6b35;
  font-weight: 700;
}

/* 首载/切 tab 骨架 */
.order-skel-list { padding: 16rpx 0; }
.order-skel-card {
  background: #fff; margin: 0 24rpx 16rpx; border-radius: 24rpx;
  padding: 20rpx 24rpx; box-shadow: 0 8rpx 32rpx rgba(31, 33, 38, 0.06);
}
.skel { background: linear-gradient(90deg, #F0F1F3 25%, #F7F8FA 37%, #F0F1F3 63%); background-size: 400% 100%; animation: shimmer 1.2s ease infinite; border-radius: 8rpx; }
@keyframes shimmer { 0% { background-position: 100% 0; } 100% { background-position: 0 0; } }
.skel-head { width: 40%; height: 26rpx; margin-bottom: 22rpx; }
.skel-body { display: flex; gap: 20rpx; }
.skel-img { width: 140rpx; height: 140rpx; border-radius: 16rpx; flex-shrink: 0; }
.skel-lines { flex: 1; display: flex; flex-direction: column; justify-content: space-between; padding: 6rpx 0; }
.skel-line1 { width: 86%; height: 26rpx; }
.skel-line2 { width: 46%; height: 30rpx; }
.skel-line3 { width: 36%; height: 34rpx; }

.load-end {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32rpx 48rpx 48rpx;
  gap: 16rpx;
}
.load-end-line {
  flex: 1;
  height: 1rpx;
  background: #E5E6EB;
}
.load-end-text {
  font-size: 22rpx;
  color: #B6BAC2;
  white-space: nowrap;
}
</style>
