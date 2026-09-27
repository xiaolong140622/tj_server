<template>
  <view class="rebate-input-card">
    <textarea
      class="rebate-textarea"
      :value="modelValue"
      :placeholder="placeholder"
      placeholder-class="rebate-textarea-ph"
      :auto-height="true"
      :show-confirm-bar="false"
      :adjust-position="true"
      maxlength="500"
      @input="onInput"
    />
    <view class="rebate-actions">
      <view class="clear-action" hover-class="clear-action--hover" @click="onClear">
        <view class="clear-action-icon" :style="{ backgroundImage: trashIcon }"></view>
        <text class="clear-action-text">清空</text>
      </view>
      <view class="rebate-actions-right">
        <view class="paste-btn" hover-class="paste-btn--hover" @click="onPaste">
          <text class="paste-btn-text">粘贴</text>
        </view>
        <view
          class="go-btn"
          :class="{ 'go-btn--disabled': !canSubmit, 'go-btn--loading': loading }"
          hover-class="go-btn--hover"
          @click="onQuery"
        >
          <view class="btn-spinner" v-if="loading"></view>
          <text class="go-btn-text">{{ loading ? '查询中…' : '查返利' }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
// 查返利输入卡（UI 规格 rebate-page-design-spec §3）：textarea + 清空/粘贴/查返利三键，四键态与搜索页同源
import { computed } from 'vue';
import { lineIcon } from '../utils/icons';

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '粘贴淘宝口令或商品链接' },
  loading: { type: Boolean, default: false },
});
const emit = defineEmits(['update:modelValue', 'query']);

const trashIcon = lineIcon('#FF6B35',
  "<path d='M4 7h16M9.5 7V4.8h5V7M6.5 7l1 13h9l1-13'/><path d='M10.5 11v5.5M13.5 11v5.5'/>");

const canSubmit = computed(() => !!props.modelValue.trim());

const onInput = (e) => emit('update:modelValue', e.detail.value);

const onClear = () => emit('update:modelValue', '');

// 粘贴只写入输入框，不自动触发查询（防误触，与搜索页口径一致）
const onPaste = () => {
  uni.getClipboardData({
    success: (r) => {
      const text = (r.data || '').trim();
      if (!text) { uni.showToast({ title: '剪贴板暂无内容', icon: 'none' }); return; }
      emit('update:modelValue', text);
    },
    fail: () => uni.showToast({ title: '剪贴板暂无内容', icon: 'none' }),
  });
};

const onQuery = () => {
  const text = props.modelValue.trim();
  if (!text || props.loading) return;
  emit('query', text);
};
</script>

<style scoped>
.rebate-input-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(31, 33, 38, 0.06);
}
.rebate-textarea {
  width: 100%;
  box-sizing: border-box;
  min-height: 80rpx;
  max-height: 220rpx;
  background: #F7F8FA;
  border-radius: 16rpx;
  padding: 20rpx;
  font-size: 26rpx;
  line-height: 1.5;
  color: #1F2126;
}
.rebate-textarea-ph { color: #B6BAC2; font-size: 26rpx; }
.rebate-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20rpx;
}
.rebate-actions-right {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.clear-action {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 12rpx 16rpx;
  border-radius: 999rpx;
}
.clear-action--hover { background: #FFF7F0; }
.clear-action-icon {
  width: 28rpx;
  height: 28rpx;
  background-size: 100% 100%;
  background-repeat: no-repeat;
}
.clear-action-text { font-size: 26rpx; color: #FF6B35; }
.paste-btn {
  display: flex;
  align-items: center;
  padding: 12rpx 32rpx;
  border-radius: 999rpx;
  background: #fff;
  border: 1rpx solid rgba(255, 107, 53, 0.4);
}
.paste-btn--hover { background: #FFF7F0; }
.paste-btn-text { font-size: 26rpx; color: #FF6B35; font-weight: 500; }
.go-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  height: 64rpx;
  padding: 0 40rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #FF6B35, #FF8F65);
  box-shadow: 0 6rpx 16rpx rgba(255, 107, 53, 0.35);
  transition: transform 0.15s ease, opacity 0.15s ease;
}
.go-btn--hover { transform: scale(0.97); opacity: 0.9; }
.go-btn-text { font-size: 28rpx; color: #fff; font-weight: 600; }
.go-btn--disabled { background: #F3F4F6; box-shadow: none; }
.go-btn--disabled .go-btn-text { color: #B6BAC2; }
.go-btn--loading { pointer-events: none; }
.btn-spinner {
  width: 28rpx;
  height: 28rpx;
  border: 3rpx solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: rebate-btn-spin 0.7s linear infinite;
}
@keyframes rebate-btn-spin { to { transform: rotate(360deg); } }
</style>
