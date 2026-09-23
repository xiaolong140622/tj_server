<template>
  <view class="empty-state">
    <image class="empty-image" src="/static/empty.png" mode="aspectFit" />
    <text class="empty-text">{{ text || '暂无数据' }}</text>
    <view class="empty-action" v-if="actionText" hover-class="empty-action-hover" @click="$emit('action')">
      <text class="empty-action-text">{{ actionText }}</text>
    </view>
  </view>
</template>

<script setup>
defineProps({
  text: { type: String, default: '暂无数据' },
  actionText: { type: String, default: '' },
});
defineEmits(['action']);
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 0;
}
.empty-image {
  width: 200rpx;
  height: 200rpx;
  opacity: 0.5;
}
.empty-text {
  margin-top: 20rpx;
  font-size: 28rpx;
  color: #7A7F89;
}
.empty-action {
  margin-top: 32rpx;
  padding: 14rpx 56rpx;
  border-radius: 999rpx;
  border: 2rpx solid #FF6B35;
}
.empty-action-hover { background: #FFF1EC; }
.empty-action-text { font-size: 26rpx; color: #FF6B35; font-weight: 600; }
</style>
