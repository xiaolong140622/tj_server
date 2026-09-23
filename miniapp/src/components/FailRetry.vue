<template>
  <view class="fail-retry">
    <view class="fail-icon" :style="{ backgroundImage: icon }"></view>
    <text class="fail-text">{{ text }}</text>
    <view class="fail-btn" hover-class="fail-btn-hover" @click="$emit('retry')">
      <text class="fail-btn-text">点击重试</text>
    </view>
  </view>
</template>

<script setup>
import { lineIcon } from '../utils/icons';

defineProps({
  text: { type: String, default: '加载失败，请检查网络' },
});
defineEmits(['retry']);

const icon = lineIcon('#B6BAC2',
  "<circle cx='12' cy='12' r='9'/><path d='M12 8v5'/><circle cx='12' cy='16.2' r='0.4' fill='#B6BAC2'/>");
</script>

<style scoped>
.fail-retry {
  display: flex; flex-direction: column; align-items: center;
  padding: 80rpx 0;
}
.fail-icon {
  width: 72rpx; height: 72rpx; border-radius: 50%;
  background-color: #F3F4F6; background-repeat: no-repeat; background-position: center; background-size: 40rpx;
}
.fail-text { font-size: 26rpx; color: #7A7F89; margin-top: 20rpx; }
.fail-btn {
  margin-top: 28rpx; padding: 14rpx 56rpx;
  border-radius: 999rpx; border: 2rpx solid #FF6B35;
}
.fail-btn-hover { background: #FFF1EC; }
.fail-btn-text { font-size: 26rpx; color: #FF6B35; font-weight: 600; }
</style>
