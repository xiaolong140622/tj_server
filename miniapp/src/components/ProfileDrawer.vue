<template>
  <view class="drawer-mask" v-if="visible" @click="close">
    <view class="drawer" @click.stop>
      <view class="drawer-head">
        <text class="drawer-title">完善头像昵称</text>
        <view class="drawer-close" hover-class="drawer-close-hover" @click="close">
          <text class="drawer-close-icon">×</text>
        </view>
      </view>
      <text class="drawer-sub">点头像换微信头像，昵称支持一键带入微信昵称</text>
      <view class="drawer-avatar-row">
        <button
          class="drawer-avatar-btn"
          open-type="chooseAvatar"
          :disabled="avatarUploading"
          @chooseavatar="onChooseAvatar"
          @error="onChooseAvatarError"
        >
          <image class="drawer-avatar" :src="avatarDisplaySrc" mode="aspectFill" @error="onAvatarImgError" />
          <view class="avatar-mask" v-if="avatarUploading">
            <view class="spinner"></view>
          </view>
        </button>
        <text class="drawer-avatar-tip">点击头像可换微信头像</text>
      </view>
      <view class="drawer-nick-row">
        <text class="drawer-nick-label">昵称</text>
        <input
          class="drawer-nick-input"
          type="nickname"
          placeholder="点击输入昵称（支持使用微信昵称）"
          placeholder-class="drawer-nick-placeholder"
          :value="nicknameDraft"
          maxlength="20"
          @input="onNicknameInput"
          @blur="onNicknameInput"
          @change="onNicknameInput"
        />
      </view>
      <view
        class="drawer-save"
        :class="{ 'drawer-save-disabled': !canSave, 'drawer-save-loading': nicknameSaving }"
        @click="saveProfile"
      >
        <text class="drawer-save-text">{{ nicknameSaving ? '保存中…' : '保存' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useUserStore } from '../store/user';
import { editUser } from '../api/user';
import { useAvatar } from '../utils/useAvatar';

const props = defineProps({ visible: { type: Boolean, default: false } });
const emit = defineEmits(['update:visible']);

const userStore = useUserStore();
const { avatarUploading, avatarDisplaySrc, onChooseAvatar, onChooseAvatarError, onAvatarImgError } = useAvatar();

const nicknameDraft = ref('');
const nicknameSaving = ref(false);

// 打开时以当前昵称初始化草稿；默认昵称「微信用户」视为未完善，草稿置空
watch(() => props.visible, (v) => {
  if (!v) return;
  const cur = (userStore.userInfo && userStore.userInfo.nickname) || '';
  nicknameDraft.value = cur === '微信用户' ? '' : cur;
});

const close = () => {
  if (nicknameSaving.value) return;
  emit('update:visible', false);
};

const onNicknameInput = (e) => {
  let v = e && e.detail && typeof e.detail.value === 'string' ? e.detail.value : nicknameDraft.value;
  if (v.length > 20) {
    v = v.slice(0, 20);
    uni.showToast({ title: '昵称最长20个字符', icon: 'none' });
  }
  nicknameDraft.value = v;
};

// 空昵称置灰、保存中锁按钮
const canSave = computed(() => !nicknameSaving.value && nicknameDraft.value.trim().length > 0);

const saveProfile = async () => {
  if (nicknameSaving.value) return;
  const name = nicknameDraft.value.trim(); // 禁止首尾空格
  if (!name) return;
  const cur = (userStore.userInfo && userStore.userInfo.nickname) || '';
  if (name === cur) { emit('update:visible', false); return; } // 无改动不发请求
  nicknameSaving.value = true;
  try {
    await editUser({ nickname: name });
    await userStore.refreshUserInfo();
    emit('update:visible', false);
    uni.showToast({ title: '已保存', icon: 'success' });
  } catch (e) {
    uni.showToast({ title: '保存失败，请重试', icon: 'none' }); // 失败保持抽屉打开
  } finally {
    nicknameSaving.value = false;
  }
};
</script>

<style scoped>
.drawer-mask {
  position: fixed; left: 0; right: 0; top: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.4); z-index: 100;
  display: flex; align-items: flex-end;
}
.drawer {
  width: 100%;
  background: #FFFFFF;
  border-radius: 32rpx 32rpx 0 0;
  padding: 40rpx 32rpx calc(32rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  animation: drawer-up 0.25s ease;
}
@keyframes drawer-up { from { transform: translateY(100%); } to { transform: translateY(0); } }
.drawer-head { display: flex; align-items: center; justify-content: space-between; }
.drawer-title { font-size: 34rpx; font-weight: bold; color: #1F2126; }
.drawer-close { padding: 8rpx 12rpx; }
.drawer-close-hover { opacity: 0.6; }
.drawer-close-icon { font-size: 40rpx; color: #B6BAC2; line-height: 1; }
.drawer-sub { display: block; font-size: 24rpx; color: #7A7F89; margin-top: 10rpx; }

.drawer-avatar-row { display: flex; align-items: center; height: 140rpx; margin-top: 16rpx; }
.drawer-avatar-btn {
  padding: 0; margin: 0; line-height: 1; background: transparent; border: none;
  width: 96rpx; height: 96rpx; border-radius: 50%;
  position: relative; overflow: hidden;
}
.drawer-avatar-btn::after { border: none; }
.drawer-avatar { width: 96rpx; height: 96rpx; border-radius: 50%; background: #F3F4F6; }
.avatar-mask {
  position: absolute; left: 0; top: 0; right: 0; bottom: 0;
  border-radius: 50%; background: rgba(255, 255, 255, 0.4);
  display: flex; align-items: center; justify-content: center;
}
.spinner {
  width: 40rpx; height: 40rpx; border-radius: 50%;
  border: 5rpx solid rgba(255, 107, 53, 0.25); border-top-color: #FF6B35;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.drawer-avatar-tip { font-size: 26rpx; color: #7A7F89; margin-left: 24rpx; }

.drawer-nick-row {
  display: flex; align-items: center; height: 100rpx;
  border-bottom: 1rpx solid #F2F3F5;
}
.drawer-nick-label { font-size: 28rpx; color: #1F2126; flex-shrink: 0; margin-right: 24rpx; }
.drawer-nick-input { flex: 1; text-align: right; font-size: 28rpx; color: #1F2126; }
.drawer-nick-placeholder { color: #B6BAC2; }

.drawer-save {
  margin-top: 48rpx; height: 88rpx; border-radius: 44rpx;
  background: linear-gradient(135deg, #FF6B35, #FF8F65);
  display: flex; align-items: center; justify-content: center;
}
.drawer-save-text { font-size: 32rpx; font-weight: bold; color: #fff; }
.drawer-save-disabled { background: #FFC9B3; }
.drawer-save-loading { opacity: 0.8; }
</style>
