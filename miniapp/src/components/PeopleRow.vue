<template>
  <view class="people-row" hover-class="people-row-hover">
    <view class="rank-badge" :class="rank <= 3 ? 'rank-top' : 'rank-normal'">
      <text class="rank-text">{{ rank }}</text>
    </view>
    <image class="people-avatar" :src="avatarSrc" mode="aspectFill" @error="avatarBroken = true" />
    <view class="people-info">
      <text class="people-name">{{ user.nickname || '用户' }}</text>
      <text class="people-sub">下单{{ user.orderCount || 0 }}笔</text>
    </view>
    <view class="people-right">
      <text class="people-amount">+¥{{ formatAmount(user.commission) }}</text>
      <text class="people-label">{{ label }}</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { formatAmount } from '../utils/format';
import { BASE_URL } from '../utils/constants';

const props = defineProps({
  user: { type: Object, required: true },
  rank: { type: Number, default: 0 },
  label: { type: String, default: '推广奖励' },
});

// 行型 3（子页面规范 §3）：头像空值/坏图统一回落 logo，禁止破图
const avatarBroken = ref(false);
watch(() => props.user.avatar, () => { avatarBroken.value = false; });
const avatarSrc = computed(() => {
  const raw = props.user.avatar || '';
  if (!raw || avatarBroken.value) return '/static/logo.png';
  return raw.startsWith('/') ? BASE_URL + raw : raw;
});
</script>

<style scoped>
.people-row {
  display: flex; align-items: center;
  padding: 22rpx 0; border-bottom: 1rpx solid #F2F3F5;
}
.people-row:last-child { border-bottom: none; }
.people-row-hover { background: #FAFBFC; }
.rank-badge {
  width: 40rpx; height: 40rpx; border-radius: 12rpx; flex-shrink: 0; margin-right: 16rpx;
  display: flex; align-items: center; justify-content: center;
}
.rank-text { font-size: 22rpx; font-weight: bold; }
.rank-top { background: linear-gradient(135deg, #FF6B35 0%, #FF8F65 100%); box-shadow: 0 4rpx 10rpx rgba(255, 107, 53, 0.3); }
.rank-top .rank-text { color: #fff; }
.rank-normal { background: #EEE; }
.rank-normal .rank-text { color: #999; }
.people-avatar { width: 84rpx; height: 84rpx; border-radius: 50%; flex-shrink: 0; background: #F3F4F6; }
.people-info { margin-left: 20rpx; flex: 1; display: flex; flex-direction: column; min-width: 0; }
.people-name { font-size: 28rpx; color: #1F2126; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.people-sub { font-size: 22rpx; color: #B6BAC2; margin-top: 6rpx; }
.people-right { display: flex; flex-direction: column; align-items: flex-end; flex-shrink: 0; }
.people-amount { font-size: 28rpx; color: #FF6B35; font-weight: bold; }
.people-label { font-size: 20rpx; color: #B6BAC2; margin-top: 4rpx; }
</style>
