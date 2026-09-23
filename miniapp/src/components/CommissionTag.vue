<template>
  <view class="commission-tag">
    <text class="label">{{ COPYWRITING.COMMISSION_LABEL }}</text>
    <text class="value">¥{{ formatMoney(amount) }}</text>
  </view>
</template>

<script setup>
import { COPYWRITING } from '../utils/constants';
import { formatMoney } from '../utils/format';

defineProps({
  amount: { type: [Number, String], default: 0 },
});
</script>

<style scoped>
.commission-tag {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  background: linear-gradient(135deg, #fff7f0, #fff0e6);
  border: 1rpx solid rgba(255, 107, 53, 0.15);
  border-radius: 12rpx;
  padding: 8rpx 16rpx;
}
.label {
  font-size: 22rpx;
  color: #ff8f65;
}
.value {
  font-size: 28rpx;
  color: #ff6b35;
  font-weight: bold;
}
</style>
