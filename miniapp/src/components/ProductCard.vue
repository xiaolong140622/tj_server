<template>
  <view class="product-card" @click="onClick" hover-class="product-card--hover">
    <view class="product-image-wrap">
      <image class="product-image" :src="product.mainPic || product.pic" mode="aspectFill" lazy-load />
      <view class="platform-badge" :style="{ background: platformColor }">
        <text class="platform-badge-text">{{ platformLabel }}</text>
      </view>
    </view>
    <view class="product-info">
      <text class="product-title">{{ product.title }}</text>
      <view class="product-tags" v-if="tagList.length">
        <text class="tag" v-for="tag in tagList" :key="tag">{{ tag }}</text>
      </view>
      <view class="product-price-row">
        <text class="product-price-symbol">¥</text>
        <text class="product-price">{{ priceInt }}</text>
        <text class="product-price-decimal">.{{ priceDec }}</text>
        <text class="product-original-price" v-if="product.originalPrice">¥{{ formatPrice(product.originalPrice) }}</text>
      </view>
      <view class="product-bottom-row">
        <view class="product-commission" v-if="showCommission">
          <text class="commission-label">{{ COPYWRITING.COMMISSION_LABEL }}</text>
          <text class="commission-value">{{ rewardText ? `¥${rewardText}` : '待计算' }}</text>
        </view>
        <text class="product-volume" v-if="product.volume">{{ formatVolume(product.volume) }}人付款</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue';
import { COPYWRITING, PLATFORM } from '../utils/constants';
import { formatMoney } from '../utils/format';
import { estimateReward } from '../utils/commission';

const props = defineProps({
  product: { type: Object, required: true },
  showCommission: { type: Boolean, default: true },
});

const emit = defineEmits(['click']);

const formatPrice = (v) => formatMoney(v);

const priceStr = computed(() => {
  const p = props.product?.price || 0;
  return typeof p === 'string' ? p : Number(p).toFixed(2);
});

const priceInt = computed(() => priceStr.value.split('.')[0]);
const priceDec = computed(() => priceStr.value.split('.')[1] || '00');

const platformKey = computed(() => props.product?.platform || 'tb');
// 预估奖励 = 价格 × /commissionInfo 平台系数/100（JAVA seq-177 口径）；禁止前端写死系数；
// 系数未加载/字段缺失 → null → 显示「待计算」
const rewardText = computed(() => {
  const p = props.product;
  if (!p) return null;
  return estimateReward(p.price, platformKey.value);
});
const platformInfo = computed(() => PLATFORM[platformKey.value.toUpperCase()] || PLATFORM.TB);
const platformLabel = computed(() => platformInfo.value.label);
const platformColor = computed(() => {
  const colors = { tb: '#ff4d4f', jd: '#e4393c', pdd: '#e02e24', dy: '#161823' };
  return colors[platformKey.value] || '#ff6b35';
});

const tagList = computed(() => {
  const p = props.product;
  if (!p) return [];
  const tags = [];
  if (p.shopType) tags.push(p.shopType);
  if (p.freeShipping) tags.push('包邮');
  if (p.couponAmount > 0) tags.push(`券${formatMoney(p.couponAmount)}元`);
  return tags.slice(0, 3);
});

const formatVolume = (v) => {
  if (v >= 10000) return (v / 10000).toFixed(1) + '万';
  if (v >= 1000) return (v / 1000).toFixed(1) + 'k';
  return String(v);
};

const onClick = () => emit('click', props.product);
</script>

<style scoped>
.product-card {
  display: flex;
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
  margin-bottom: 16rpx;
  box-shadow: 0 8rpx 32rpx rgba(31, 33, 38, 0.06);
  transition: transform 0.15s ease;
}
.product-card--hover {
  transform: scale(0.98);
  opacity: 0.9;
}
.product-image-wrap {
  position: relative;
  flex-shrink: 0;
  width: 240rpx;
  height: 240rpx;
}
.product-image {
  width: 100%;
  height: 100%;
}
.platform-badge {
  position: absolute;
  top: 0;
  left: 0;
  padding: 4rpx 12rpx;
  border-radius: 16rpx 0 16rpx 0;
}
.platform-badge-text {
  font-size: 20rpx;
  color: #fff;
  font-weight: 500;
}
.product-info {
  flex: 1;
  padding: 16rpx 20rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}
.product-title {
  font-size: 26rpx;
  color: #333;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.product-tags {
  display: flex;
  gap: 8rpx;
  margin-top: 8rpx;
  flex-wrap: wrap;
}
.tag {
  font-size: 20rpx;
  color: #ff6b35;
  border: 1rpx solid rgba(255, 107, 53, 0.3);
  border-radius: 6rpx;
  padding: 2rpx 10rpx;
  background: rgba(255, 107, 53, 0.05);
}
.product-price-row {
  display: flex;
  align-items: baseline;
  gap: 4rpx;
  margin-top: 10rpx;
}
.product-price-symbol {
  font-size: 24rpx;
  color: #1F2126;
  font-weight: bold;
}
.product-price {
  font-size: 36rpx;
  color: #1F2126;
  font-weight: bold;
  line-height: 1;
}
.product-price-decimal {
  font-size: 24rpx;
  color: #1F2126;
  font-weight: bold;
}
.product-original-price {
  font-size: 22rpx;
  color: #bbb;
  text-decoration: line-through;
  margin-left: 8rpx;
}
.product-bottom-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8rpx;
}
.product-commission {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  background: linear-gradient(135deg, #FF6B35, #FF8F65);
  border-radius: 10rpx;
  padding: 6rpx 16rpx;
  box-shadow: 0 4rpx 12rpx rgba(255, 107, 53, 0.25);
}
.commission-label {
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.9);
}
.commission-value {
  font-size: 26rpx;
  color: #fff;
  font-weight: bold;
}
.product-volume {
  font-size: 20rpx;
  color: #999;
}
</style>
