<template>
  <view class="page-order-detail">
    <view v-if="loadError" class="detail-error"><FailRetry @retry="retryLoad" /></view>
    <block v-else>
    <view class="status-hero" :class="'status-hero--' + statusTheme">
      <view class="status-icon-wrap">
        <text class="status-icon">{{ statusIcon }}</text>
      </view>
      <text class="status-label">{{ statusLabel }}</text>
      <text class="status-hint" v-if="orderInfo.status === 1">预计 {{ orderInfo.unlockTime || '—' }} 到账</text>
      <text class="status-hint" v-if="orderInfo.status === 3">奖励已转入余额</text>
    </view>

    <view class="product-card">
      <view class="product-img-wrap">
        <image class="product-img" :src="orderInfo.mainPic || orderInfo.pic" mode="aspectFill" />
        <view class="product-platform-flag" :style="{ background: platformColor }">
          <text class="product-platform-text">{{ platformLabel }}</text>
        </view>
      </view>
      <view class="product-info">
        <text class="product-title">{{ orderInfo.title }}</text>
        <view class="product-price-row">
          <view class="product-price-group">
            <text class="product-price-label">实付</text>
            <text class="product-price-symbol">¥</text>
            <text class="product-price-value">{{ priceInt }}</text>
            <text class="product-price-decimal">.{{ priceDec }}</text>
          </view>
          <text class="product-shop-name" v-if="orderInfo.shopName">{{ orderInfo.shopName }}</text>
        </view>
      </view>
    </view>

    <view class="reward-card">
      <view class="reward-row">
        <text class="reward-label">{{ COPYWRITING.COMMISSION_LABEL }}</text>
        <text class="reward-value">¥{{ formatMoney(orderInfo.hb || orderInfo.estimateCommission || orderInfo.commission) }}</text>
      </view>
      <view class="reward-note">
        <text class="reward-note-text">{{ COPYWRITING.ESTIMATED_PREFIX }}奖励金额仅供参考，实际以平台结算为准</text>
      </view>
    </view>

    <view class="info-card">
      <view class="info-card-header">
        <view class="info-card-bar"></view>
        <text class="info-card-title">订单信息</text>
      </view>
      <view class="info-rows">
        <view class="info-row">
          <text class="info-label">订单编号</text>
          <text class="info-value">{{ orderInfo.orderKey || orderInfo.id }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">订单平台</text>
          <view class="info-platform-tag" :style="{ color: platformColor, background: platformBg }">
            <text class="info-platform-text">{{ platformLabel }}</text>
          </view>
        </view>
        <view class="info-row">
          <text class="info-label">订单金额</text>
          <text class="info-value">¥{{ formatMoney(orderInfo.amount) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">实付金额</text>
          <text class="info-value info-value--bold">¥{{ formatMoney(orderInfo.payPrice) }}</text>
        </view>
        <view class="info-row" v-if="orderInfo.couponAmount">
          <text class="info-label">优惠券</text>
          <text class="info-value info-value--discount">-¥{{ formatMoney(orderInfo.couponAmount) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">下单时间</text>
          <text class="info-value">{{ orderInfo.orderTime || orderInfo.createTime || '—' }}</text>
        </view>
        <view class="info-row" v-if="orderInfo.settleTime">
          <text class="info-label">结算时间</text>
          <text class="info-value">{{ orderInfo.settleTime }}</text>
        </view>
        <view class="info-row" v-if="orderInfo.unlockTime">
          <text class="info-label">预计到账</text>
          <text class="info-value info-value--highlight">{{ orderInfo.unlockTime }}</text>
        </view>
      </view>
    </view>

    <view class="timeline-card" v-if="orderInfo.timeline && orderInfo.timeline.length">
      <view class="info-card-header">
        <view class="info-card-bar"></view>
        <text class="info-card-title">状态流转</text>
      </view>
      <view class="timeline">
        <view
          class="timeline-step"
          v-for="(item, idx) in orderInfo.timeline"
          :key="idx"
          :class="{ 'timeline-step--active': item.done, 'timeline-step--last': idx === orderInfo.timeline.length - 1 }"
        >
          <view class="timeline-rail">
            <view class="timeline-dot" :class="{ 'timeline-dot--active': item.done }">
              <text class="timeline-dot-num" v-if="item.done">{{ idx + 1 }}</text>
            </view>
            <view class="timeline-connector" v-if="idx < orderInfo.timeline.length - 1" :class="{ 'timeline-connector--active': item.done && orderInfo.timeline[idx + 1]?.done }"></view>
          </view>
          <view class="timeline-body">
            <text class="timeline-step-label" :class="{ 'timeline-step-label--active': item.done }">{{ item.label }}</text>
            <text class="timeline-step-time">{{ item.time || '—' }}</text>
          </view>
        </view>
      </view>
    </view>
    </block>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { formatMoney } from '../../utils/format';
import { normalizeJdOrder } from '../../utils/order';
import { getJdOrders } from '../../api/order';
import FailRetry from '../../components/FailRetry.vue';
import { COPYWRITING, PLATFORM_LIST, ORDER_STATUS } from '../../utils/constants';
import { mockOrderDetail } from '../../mock/index';

const USE_MOCK = false;

const orderInfo = ref({});
const loadError = ref(false);
const platformLabel = ref('');
const platformColor = ref('#ff6b35');
const platformBg = ref('rgba(255,107,53,0.08)');
const statusLabel = ref('');
const statusTheme = ref('pending');
const statusIcon = ref('...');

const PLATFORM_COLORS = { tb: '#ff4d4f', jd: '#e4393c', pdd: '#e02e24', dy: '#161823' };
const STATUS_ICONS = { 1: '⏳', 2: '✓', 3: '★' };

const priceStr = computed(() => {
  const p = orderInfo.value?.payPrice || orderInfo.value?.amount || 0;
  return typeof p === 'string' ? p : Number(p).toFixed(2);
});
const priceInt = computed(() => priceStr.value.split('.')[0]);
const priceDec = computed(() => priceStr.value.split('.')[1] || '00');

const queryParams = ref({});

const applyStatus = () => {
  const st = Object.values(ORDER_STATUS).find(s => s.value === orderInfo.value.status);
  statusLabel.value = st ? st.label : '';
  statusTheme.value = orderInfo.value.status === 3 ? 'completed' : orderInfo.value.status === 2 ? 'settled' : 'pending';
  statusIcon.value = STATUS_ICONS[orderInfo.value.status] || '⏳';
};

const loadDetail = async (opts) => {
  queryParams.value = opts || {};
  const platform = opts.platform || 'tb';
  const pInfo = PLATFORM_LIST.find(p => p.value === platform);
  platformLabel.value = pInfo ? pInfo.label : '';
  platformColor.value = PLATFORM_COLORS[platform] || '#ff6b35';
  platformBg.value = `rgba(${platform === 'dy' ? '22,24,35' : '255,107,53'},0.08)`;

  if (USE_MOCK) {
    orderInfo.value = mockOrderDetail(opts.id || opts.key || 'ord_tb_0', platform);
  } else if (platform === 'jd') {
    // C-3：JD 详情 = /jd/orders criteria.value 反查单条（blurry 含 id/orderId；F-7 信封已收口）
    loadError.value = false;
    const key = opts.key || opts.id || '';
    try {
      const res = await getJdOrders({ page: 1, limit: 1, value: key }, { silent: true });
      const rows = Array.isArray(res?.data) ? res.data : (Array.isArray(res?.result) ? res.result : []);
      const row = rows.find((r) => String(r.id) === String(key) || String(r.orderId) === String(key)) || rows[0];
      if (!row) {
        loadError.value = true; // 查无此行也走错误态，禁 mock 填充
      } else {
        orderInfo.value = normalizeJdOrder(row);
      }
    } catch (e) {
      loadError.value = true;
    }
  }
  applyStatus();
};

const retryLoad = () => loadDetail(queryParams.value);

onLoad((opts) => loadDetail(opts));
</script>

<style scoped>
.page-order-detail {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 60rpx;
}

.detail-error { padding: 120rpx 24rpx 0; }

.status-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 32rpx 40rpx;
  background: linear-gradient(160deg, #ff6b35, #ff8f65);
}
.status-hero--settled {
  background: linear-gradient(160deg, #00b894, #55efc4);
}
.status-hero--completed {
  background: linear-gradient(160deg, #ff6b35, #ffa06b);
}
.status-icon-wrap {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16rpx;
}
.status-icon {
  font-size: 36rpx;
  color: #fff;
}
.status-label {
  font-size: 36rpx;
  color: #fff;
  font-weight: 700;
}
.status-hint {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 8rpx;
}

.product-card {
  display: flex;
  background: #fff;
  margin: -20rpx 24rpx 0;
  border-radius: 20rpx;
  padding: 24rpx;
  gap: 20rpx;
  position: relative;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
}
.product-img-wrap {
  width: 160rpx;
  height: 160rpx;
  border-radius: 16rpx;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
  background: linear-gradient(135deg, #ffe0d0, #fff0e8);
}
.product-img {
  width: 100%;
  height: 100%;
}
.product-platform-flag {
  position: absolute;
  top: 0;
  left: 0;
  padding: 4rpx 12rpx;
  border-radius: 16rpx 0 12rpx 0;
}
.product-platform-text {
  font-size: 18rpx;
  color: #fff;
  font-weight: 500;
}
.product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}
.product-title {
  font-size: 26rpx;
  color: #333;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.product-price-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}
.product-price-group {
  display: flex;
  align-items: baseline;
  gap: 2rpx;
}
.product-price-label {
  font-size: 22rpx;
  color: #999;
  margin-right: 6rpx;
}
.product-price-symbol {
  font-size: 24rpx;
  color: #ff4d4f;
  font-weight: 700;
}
.product-price-value {
  font-size: 36rpx;
  color: #ff4d4f;
  font-weight: 800;
  line-height: 1;
}
.product-price-decimal {
  font-size: 24rpx;
  color: #ff4d4f;
  font-weight: 700;
}
.product-shop-name {
  font-size: 22rpx;
  color: #999;
  max-width: 200rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reward-card {
  background: linear-gradient(135deg, #fff7f0, #fff0e6);
  margin: 16rpx 24rpx 0;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
  border: 1rpx solid rgba(255, 107, 53, 0.1);
}
.reward-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.reward-label {
  font-size: 26rpx;
  color: #ff8f65;
  font-weight: 500;
}
.reward-value {
  font-size: 40rpx;
  color: #ff6b35;
  font-weight: 800;
}
.reward-note {
  margin-top: 12rpx;
  padding-top: 12rpx;
  border-top: 1rpx dashed rgba(255, 107, 53, 0.15);
}
.reward-note-text {
  font-size: 20rpx;
  color: #cca08a;
}

.info-card {
  background: #fff;
  margin: 16rpx 24rpx 0;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
}
.info-card-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 20rpx;
}
.info-card-bar {
  width: 6rpx;
  height: 28rpx;
  background: linear-gradient(180deg, #ff6b35, #ff8f65);
  border-radius: 3rpx;
}
.info-card-title {
  font-size: 28rpx;
  color: #333;
  font-weight: 600;
}
.info-rows {
  display: flex;
  flex-direction: column;
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f8f8f8;
}
.info-row:last-child {
  border-bottom: none;
}
.info-label {
  font-size: 26rpx;
  color: #999;
  flex-shrink: 0;
}
.info-value {
  font-size: 26rpx;
  color: #333;
  text-align: right;
  margin-left: 20rpx;
}
.info-value--bold {
  font-weight: 700;
  color: #333;
}
.info-value--discount {
  color: #00b894;
  font-weight: 500;
}
.info-value--highlight {
  color: #ff6b35;
  font-weight: 600;
}
.info-platform-tag {
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
}
.info-platform-text {
  font-size: 22rpx;
  font-weight: 600;
}

.timeline-card {
  background: #fff;
  margin: 16rpx 24rpx 0;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
}

.timeline {
  padding-left: 4rpx;
}
.timeline-step {
  display: flex;
  gap: 20rpx;
  position: relative;
}
.timeline-step--last {
  padding-bottom: 0;
}
.timeline-rail {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 32rpx;
}
.timeline-dot {
  width: 32rpx;
  height: 32rpx;
  border-radius: 50%;
  background: #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  z-index: 1;
}
.timeline-dot--active {
  background: linear-gradient(135deg, #ff6b35, #ff8f65);
}
.timeline-dot-num {
  font-size: 18rpx;
  color: #fff;
  font-weight: 700;
}
.timeline-connector {
  width: 2rpx;
  flex: 1;
  min-height: 40rpx;
  background: #e8e8e8;
}
.timeline-connector--active {
  background: linear-gradient(180deg, #ff6b35, #ff8f65);
}
.timeline-body {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4rpx 0 28rpx;
}
.timeline-step--last .timeline-body {
  padding-bottom: 0;
}
.timeline-step-label {
  font-size: 26rpx;
  color: #999;
}
.timeline-step-label--active {
  color: #333;
  font-weight: 600;
}
.timeline-step-time {
  font-size: 22rpx;
  color: #bbb;
}
</style>
