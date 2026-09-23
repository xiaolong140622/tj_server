<template>
  <view class="page-settings">
    <!-- 资料组 -->
    <text class="group-title">账号资料</text>
    <view class="card">
      <button
        class="row row-btn"
        open-type="chooseAvatar"
        :disabled="avatarUploading"
        @chooseavatar="onChooseAvatar"
        @error="onChooseAvatarError"
      >
        <view class="row-icon icon-avatar" :style="{ backgroundImage: ICONS.user('#FF6B35') }"></view>
        <text class="row-label">头像</text>
        <view class="avatar-cell">
          <image class="row-avatar" :src="avatarDisplaySrc" mode="aspectFill" @error="onAvatarImgError" />
          <view class="avatar-mask" v-if="avatarUploading">
            <view class="spinner"></view>
          </view>
        </view>
        <view class="chevron"></view>
      </button>
      <view class="row-divider"></view>
      <view class="row" hover-class="row-hover" @click="openProfile">
        <view class="row-icon icon-nickname" :style="{ backgroundImage: ICONS.about('#FA8C16') }"></view>
        <text class="row-label">昵称</text>
        <text class="row-value" :class="{ 'row-value-weak': isDefaultNickname }">{{ nicknameRight }}</text>
        <view class="chevron"></view>
      </view>
      <view class="row-divider"></view>
      <!-- J5：未绑定时整行=微信手机号授权按钮（POST /wxapp/binding 解密绑定）；已绑定仅展示脱敏 -->
      <button
        v-if="!hasPhone && userStore.isLoggedIn"
        class="row row-btn"
        open-type="getPhoneNumber"
        :disabled="binding"
        @getphonenumber="onGetPhoneNumber"
      >
        <view class="row-icon icon-phone" :style="{ backgroundImage: ICONS.phone('#1890FF') }"></view>
        <text class="row-label">手机号</text>
        <text class="row-value row-value-weak">{{ binding ? '绑定中…' : '去绑定' }}</text>
        <view class="chevron"></view>
      </button>
      <view v-else class="row">
        <view class="row-icon icon-phone" :style="{ backgroundImage: ICONS.phone('#1890FF') }"></view>
        <text class="row-label">手机号</text>
        <text class="row-value" :class="{ 'row-value-weak': !hasPhone }">{{ hasPhone ? maskedPhone : '未绑定' }}</text>
      </view>
      <view class="row-divider"></view>
      <view class="row">
        <view class="row-icon icon-version" :style="{ backgroundImage: ICONS.settings('#6B7280') }"></view>
        <text class="row-label">版本号</text>
        <text class="row-value">v{{ version }}</text>
      </view>
    </view>

    <!-- 操作组 -->
    <text class="group-title">操作</text>
    <view class="card card-pad">
      <view
        class="refresh-btn"
        :class="{ 'refresh-btn-loading': refreshing }"
        @click="onRefresh"
      >
        <view class="row-icon icon-refresh-light" :style="{ backgroundImage: ICONS.refresh('#FFFFFF') }"></view>
        <text class="refresh-btn-text">{{ refreshing ? '刷新中…' : '刷新个人信息' }}</text>
      </view>
    </view>
    <view class="logout-card" v-if="userStore.isLoggedIn" hover-class="logout-hover" @click="onLogout">
      <text>退出登录</text>
    </view>

    <!-- 协议组 -->
    <text class="group-title">协议与关于</text>
    <view class="card">
      <view class="row" hover-class="row-hover" @click="goWebview('agreement')">
        <view class="row-icon icon-agreement" :style="{ backgroundImage: ICONS.agreement('#FF6B35') }"></view>
        <text class="row-label">用户协议</text>
        <view class="chevron"></view>
      </view>
      <view class="row-divider"></view>
      <view class="row" hover-class="row-hover" @click="goWebview('privacy')">
        <view class="row-icon icon-privacy" :style="{ backgroundImage: ICONS.privacy('#07C160') }"></view>
        <text class="row-label">隐私政策</text>
        <view class="chevron"></view>
      </view>
      <view class="row-divider"></view>
      <view class="row" hover-class="row-hover" @click="showAbout">
        <view class="row-icon icon-about" :style="{ backgroundImage: ICONS.about('#9B6BFF') }"></view>
        <text class="row-label">关于我们</text>
        <view class="chevron"></view>
      </view>
    </view>

    <!-- 低频操作置底：清除缓存 -->
    <view class="card card-solo">
      <view class="row" hover-class="row-hover" @click="clearCache">
        <view class="row-icon icon-cache" :style="{ backgroundImage: ICONS.cache('#4A90D9') }"></view>
        <text class="row-label">清除缓存</text>
        <text class="row-value">{{ cacheSize }}</text>
      </view>
    </view>

    <view class="settings-footer">
      <text class="footer-name">苏分宝</text>
      <text class="footer-tip">{{ COPYWRITING.REWARD_DISCLAIMER }}</text>
    </view>

    <!-- 与个人中心共用完善头像昵称抽屉 -->
    <ProfileDrawer v-model:visible="profileVisible" />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { useUserStore } from '../../store/user';
import { COPYWRITING } from '../../utils/constants';
import { maskPhone } from '../../utils/format';
import { post } from '../../utils/request';
import { ICONS } from '../../utils/icons';
import { useAvatar } from '../../utils/useAvatar';
import ProfileDrawer from '../../components/ProfileDrawer.vue';

const userStore = useUserStore();
const { avatarUploading, avatarDisplaySrc, onChooseAvatar, onChooseAvatarError, onAvatarImgError } = useAvatar();

const version = ref('1.0.0');
const cacheSize = ref('0KB');
const refreshing = ref(false);
const profileVisible = ref(false);
const binding = ref(false);

const displayUser = computed(() => userStore.userInfo || {});
const isDefaultNickname = computed(
  () => !displayUser.value?.nickname || displayUser.value.nickname === '微信用户');
const nicknameRight = computed(() => (isDefaultNickname.value ? '去完善' : displayUser.value.nickname));
const maskedPhone = computed(() => maskPhone(displayUser.value?.phone || displayUser.value?.mobile || ''));
const hasPhone = computed(() => !!(displayUser.value?.phone || displayUser.value?.mobile));

const openProfile = () => { profileVisible.value = true; };

// J5：微信手机号授权 → /wxapp/binding（session_key 解密方案，登录时后端已写 Redis）
const onGetPhoneNumber = async (e) => {
  const detail = e && e.detail ? e.detail : {};
  if (detail.errMsg && detail.errMsg.indexOf('ok') === -1) {
    uni.showToast({ title: '已取消手机号绑定', icon: 'none' });
    return;
  }
  if (!detail.encryptedData || !detail.iv) {
    uni.showToast({ title: '未获取到手机号，请稍后再试', icon: 'none' });
    return;
  }
  if (binding.value) return;
  binding.value = true;
  try {
    await post('/wxapp/binding', { encryptedData: detail.encryptedData, iv: detail.iv });
    await userStore.refreshUserInfo();
    uni.showToast({ title: '手机号已绑定', icon: 'success' });
  } catch (err) {
    // request.js 已按后端 msg 弹提示
  } finally {
    binding.value = false;
  }
};

const calcCacheSize = () => {
  try {
    const res = uni.getStorageInfoSync();
    cacheSize.value = `${(res.currentSize || 0)}KB`;
  } catch (e) { cacheSize.value = '0KB'; }
};

onShow(() => {
  calcCacheSize();
  // #ifdef MP-WEIXIN
  const accountInfo = uni.getAccountInfoSync?.();
  if (accountInfo?.miniProgram?.envVersion) {
    version.value = accountInfo.miniProgram.version || '1.0.0';
  }
  // #endif
});

const onRefresh = async () => {
  if (refreshing.value) return;
  refreshing.value = true;
  try {
    await userStore.refreshUserInfo();
    uni.showToast({ title: '已刷新', icon: 'success' });
  } finally {
    refreshing.value = false;
  }
};

const goWebview = (type) => {
  uni.navigateTo({ url: `/pages/webview/index?type=${type}` });
};

const clearCache = () => {
  uni.showModal({
    title: '提示',
    content: '确定清除本地缓存？登录状态将保留',
    success: (res) => {
      if (!res.confirm) return;
      try {
        const token = uni.getStorageSync('token');
        const userInfo = uni.getStorageSync('user_info');
        uni.clearStorageSync();
        if (token) uni.setStorageSync('token', token);
        if (userInfo) uni.setStorageSync('user_info', userInfo);
        calcCacheSize();
        uni.showToast({ title: '已清除', icon: 'success' });
      } catch (e) {
        uni.showToast({ title: '清除失败', icon: 'none' });
      }
    },
  });
};

const showAbout = () => {
  uni.showModal({
    title: '关于苏分宝',
    content: '苏分宝是一款购物省钱工具，汇集多平台好物推荐。购物可获得预估奖励，分享给好友还能一起省更多。\n预估奖励金额仅供参考，实际以平台结算为准。',
    showCancel: false,
    confirmText: '知道了',
  });
};

const onLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确定退出登录？',
    confirmColor: '#ff6b35',
    success: (res) => {
      if (res.confirm) {
        userStore.doLogout();
        uni.reLaunch({ url: '/pages/login/index' });
      }
    },
  });
};
</script>

<style scoped>
.page-settings {
  --primary: #FF6B35;
  --page-bg: #F6F7F9;
  --card-bg: #FFFFFF;
  --text-main: #1F2126;
  --text-sub: #7A7F89;
  --text-weak: #B6BAC2;
  --divider: #F2F3F5;
  --radius-card: 24rpx;
  --shadow-card: 0 8rpx 32rpx rgba(31, 33, 38, 0.06);
  min-height: 100vh;
  background: var(--page-bg);
  padding: 0 24rpx calc(40rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.group-title {
  display: block; font-size: 28rpx; color: var(--text-sub);
  margin: 32rpx 8rpx 16rpx;
}
.card {
  background: var(--card-bg);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.card-pad { padding: 24rpx; }
.card-solo { margin-top: 24rpx; }

.row {
  position: relative;
  display: flex; align-items: center;
  height: 108rpx; padding: 0 32rpx;
  box-sizing: border-box;
  transition: background-color 0.15s;
}
.row-hover { background: #FAFBFC; }
.row-divider { height: 1rpx; background: var(--divider); margin-left: 120rpx; }
.row-btn {
  width: 100%;
  padding: 0 32rpx; margin: 0; line-height: 1;
  background: transparent; border: none;
  border-radius: 0;
}
.row-btn::after { border: none; }
.row-icon {
  width: 56rpx; height: 56rpx; border-radius: 16rpx; margin-right: 24rpx; flex-shrink: 0;
  background-color: #F7F8FA;
  background-repeat: no-repeat; background-position: center; background-size: 32rpx;
}
.icon-refresh-light { background-color: transparent; margin-right: 12rpx; }
.row-label { flex: 1; font-size: 30rpx; color: var(--text-main); text-align: left; }
.row-value { font-size: 28rpx; color: var(--text-sub); margin-right: 12rpx; max-width: 320rpx; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.row-value-weak { color: var(--text-weak); }
.chevron {
  width: 14rpx; height: 14rpx; flex-shrink: 0;
  border-top: 3rpx solid var(--text-weak); border-right: 3rpx solid var(--text-weak);
  transform: rotate(45deg);
}

.avatar-cell { position: relative; width: 88rpx; height: 88rpx; margin-right: 12rpx; flex-shrink: 0; }
.row-avatar {
  width: 88rpx; height: 88rpx; border-radius: 50%;
  background: #F3F4F6; box-sizing: border-box;
}
.avatar-mask {
  position: absolute; left: 0; top: 0; right: 0; bottom: 0;
  border-radius: 50%; background: rgba(255, 255, 255, 0.4);
  display: flex; align-items: center; justify-content: center;
}
.spinner {
  width: 32rpx; height: 32rpx; border-radius: 50%;
  border: 4rpx solid rgba(255, 107, 53, 0.25); border-top-color: var(--primary);
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.refresh-btn {
  height: 88rpx; border-radius: 44rpx;
  background: linear-gradient(135deg, #FF6B35, #FF8F65);
  display: flex; align-items: center; justify-content: center;
  transition: opacity 0.15s;
}
.refresh-btn-loading { opacity: 0.7; }
.refresh-btn-text { font-size: 30rpx; font-weight: bold; color: #fff; }

.logout-card {
  margin-top: 24rpx;
  background: var(--card-bg);
  border-radius: var(--radius-card);
  padding: 30rpx 0;
  text-align: center;
  font-size: 30rpx;
  color: #FF4D4F;
  box-shadow: var(--shadow-card);
}
.logout-hover { opacity: 0.8; }

.settings-footer { display: flex; flex-direction: column; align-items: center; margin-top: 64rpx; }
.footer-name { font-size: 24rpx; color: #BBB; }
.footer-tip { font-size: 20rpx; color: #CCC; margin-top: 10rpx; }
</style>
