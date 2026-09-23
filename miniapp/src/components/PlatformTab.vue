<template>
  <scroll-view scroll-x class="platform-chips" :show-scrollbar="false">
    <view class="chips-row">
      <view
        v-for="item in platforms"
        :key="item.value"
        class="chip"
        :class="{ active: current === item.value }"
        hover-class="chip-hover"
        @click="onChange(item.value)"
      >
        <view class="chip-dot" :style="{ background: dotColor(item.value) }"></view>
        <text class="chip-text">{{ item.label }}</text>
      </view>
    </view>
  </scroll-view>
</template>

<script setup>
import { PLATFORM_LIST } from '../utils/constants';

defineProps({
  current: { type: String, default: 'tb' },
});

const emit = defineEmits(['change']);
const platforms = PLATFORM_LIST;
const onChange = (value) => emit('change', value);

const DOT_COLORS = { tb: '#FF4D4F', jd: '#E4393C', pdd: '#E02E24', dy: '#161823' };
const dotColor = (v) => DOT_COLORS[v] || '#FF6B35';
</script>

<style scoped>
/* 子页面规范 §2 B 变体：胶囊筛选 chips（替代旧等分文字 tab，避免与状态 tab 双层堆叠） */
.platform-chips { width: 100%; background: #fff; white-space: nowrap; }
.chips-row { display: flex; align-items: center; height: 88rpx; padding: 0 24rpx; gap: 16rpx; }
.chip {
  display: flex; align-items: center; flex-shrink: 0;
  padding: 10rpx 28rpx; border-radius: 999rpx; background: #F5F6F7;
  transition: background-color 0.15s;
}
.chip.active { background: #FFF1EC; }
.chip-hover { opacity: 0.85; }
.chip-dot { width: 16rpx; height: 16rpx; border-radius: 50%; margin-right: 10rpx; }
.chip-text { font-size: 26rpx; color: #7A7F89; }
.chip.active .chip-text { color: #FF6B35; font-weight: 600; }
</style>
