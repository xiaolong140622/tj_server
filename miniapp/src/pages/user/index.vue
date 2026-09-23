<template>
  <view class="page-user">
    <view class="user-header" v-if="isLoggedIn">
      <button class="avatar-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
        <image class="avatar" :src="avatarSrc" mode="aspectFill" />
      </button>
      <view class="user-meta">
        <text class="nickname">{{ displayUser?.nickname || '用户' }}</text>
        <view class="phone-row">
          <text class="phone">{{ maskPhone(displayUser?.phone || displayUser?.mobile) }}</text>
          <text class="phone-badge">点击头像可换微信头像</text>
        </view>
      </view>
    </view>
    <view class="user-header login-prompt" v-else @click="goLogin">
      <view class="avatar-wrap">
        <image class="avatar" src="/static/logo.png" mode="aspectFill" />
      </view>
      <view class="user-meta">
        <text class="nickname">点击登录</text>
        <text class="login-tip">登录后即可查看奖励与订单</text>
      </view>
    </view>

    <view class="balance-card" v-if="isLoggedIn" @click="goPage('/pages/user/balance')">
      <view class="balance-item">
        <text class="balance-value">{{ formatMoney(balance.total) }}</text>
        <text class="balance-label">累计奖励</text>
      </view>
      <view class="balance-divider"></view>
      <view class="balance-item">
        <text class="balance-value">{{ formatMoney(balance.pending) }}</text>
        <text class="balance-label">待结算</text>
      </view>
      <view class="balance-divider"></view>
      <view class="balance-item">
        <text class="balance-value">{{ formatMoney(balance.settled) }}</text>
        <text class="balance-label">已到账</text>
      </view>
      <view class="chevron balance-chevron"></view>
    </view>

    <view class="menu-list">
      <view class="menu-item" hover-class="menu-hover" @click="goPage('/pages/order/list')">
        <view class="menu-icon icon-order">🧾</view>
        <text class="menu-text">我的订单</text>
        <view class="chevron"></view>
      </view>
      <view class="menu-item" hover-class="menu-hover" @click="goPage('/pages/user/balance')">
        <view class="menu-icon icon-balance">💰</view>
        <text class="menu-text">我的余额</text>
        <view class="chevron"></view>
      </view>
      <view class="menu-item" hover-class="menu-hover" @click="goPage('/pages/user/bill')">
        <view class="menu-icon icon-bill">📋</view>
        <text class="menu-text">账单明细</text>
        <view class="chevron"></view>
      </view>
      <view class="menu-item" hover-class="menu-hover" @click="goPage('/pages/user/spread')">
        <view class="menu-icon icon-spread">🤝</view>
        <text class="menu-text">推广中心</text>
        <view class="chevron"></view>
      </view>
      <view class="menu-item" hover-class="menu-hover" @click="goPage('/pages/user/settings')">
        <view class="menu-icon icon-settings">⚙️</view>
        <text class="menu-text">设置</text>
        <view class="chevron"></view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { useUserStore } from '../../store/user';
import { getBalance, editUser } from '../../api/user';
import { formatMoney, maskPhone } from '../../utils/format';
import { mockBalance, mockUserInfo } from '../../mock/index';
import { BASE_URL, TOKEN_KEY } from '../../utils/constants';

const USE_MOCK = false;

const userStore = useUserStore();
const balance = ref({ total: 0, pending: 0, settled: 0 });

const mockUser = computed(() => USE_MOCK ? mockUserInfo() : null);
const isLoggedIn = computed(() => USE_MOCK ? true : userStore.isLoggedIn);
const displayUser = computed(() => USE_MOCK ? mockUser.value : userStore.userInfo);

const avatarSrc = computed(() => {
  const raw = displayUser.value?.avatar || '';
  if (!raw || raw.includes('oss.mailvor.cn')) return '/static/logo.png';
  if (raw.startsWith('/')) return BASE_URL + raw;
  return raw;
});

const doUploadAvatar = (filePath) => {
  uni.uploadFile({
    url: BASE_URL + '/user/avatar/upload',
    filePath,
    name: 'file',
    header: { Authorization: `Bearer ${uni.getStorageSync(TOKEN_KEY)}` },
    success: async (res) => {
      try {
        let body;
        try { body = JSON.parse(res.data); } catch (parseErr) { body = null; }
        if (!body || res.statusCode === 404 || !body.data || !body.data.url) {
          uni.showToast({ title: body?.msg || '头像上传失败（请确认后端已重启）', icon: 'none' });
          return;
        }
        if (!body.success) {
          uni.showToast({ title: body.msg || '头像上传失败', icon: 'none' });
          return;
        }
        const url = body.data.url;
        await editUser({ avatar: BASE_URL + url });
        await userStore.refreshUserInfo();
        uni.showToast({ title: '头像已更新', icon: 'success' });
      } catch (err) {
        uni.showToast({ title: '头像更新失败', icon: 'none' });
      }
    },
    fail: (err) => {
      console.error('avatar upload fail', err);
      uni.showToast({ title: '头像上传失败', icon: 'none' });
    },
  });
};

const onChooseAvatar = (e) => {
  const tempPath = e.detail?.avatarUrl;
  if (!tempPath) return;
  // chooseAvatar 返回的是模拟器/真机的临时文件，模拟器下该路径存在竞态（可能已被清理导致 ENOENT），
  // 先用 FileSystemManager 复制到 USER_DATA_PATH 的稳定路径再上传。
  try {
    const fs = uni.getFileSystemManager();
    const extMatch = tempPath.match(/\.(\w+)$/);
    const ext = extMatch ? extMatch[1] : 'png';
    const stablePath = `${wx.env.USER_DATA_PATH}/avatar_pick_${Date.now()}.${ext}`;
    fs.copyFile({
      srcPath: tempPath,
      destPath: stablePath,
      success: () => doUploadAvatar(stablePath),
      fail: (err) => {
        console.error('copy avatar failed', err);
        uni.showToast({ title: '微信头像获取失败，请重试', icon: 'none' });
      },
    });
  } catch (err) {
    console.error('avatar copy exception', err);
    doUploadAvatar(tempPath);
  }
};

const loadBalance = async () => {
  if (USE_MOCK) {
    const d = mockBalance();
    balance.value = { total: d.total, pending: d.pending, settled: d.settled };
    return;
  }
  try {
    const res = await getBalance();
    const data = res.result || res.data;
    balance.value = {
      total: data?.totalMoney || data?.total || 0,
      pending: data?.pendingMoney || data?.pending || 0,
      settled: data?.settledMoney || data?.settled || 0,
    };
  } catch (e) {
    const d = mockBalance();
    balance.value = { total: d.total, pending: d.pending, settled: d.settled };
  }
};

const goLogin = () => uni.navigateTo({ url: '/pages/login/index' });
const goPage = (url) => {
  if (!isLoggedIn.value) { goLogin(); return; }
  uni.navigateTo({ url });
};

onShow(() => {
  if (isLoggedIn.value) loadBalance();
});
</script>

<style scoped>
.page-user { min-height: 100vh; background: #f5f5f5; padding-bottom: 40rpx; }

.user-header {
  display: flex;
  align-items: center;
  padding: 60rpx 32rpx 70rpx;
  background: linear-gradient(135deg, #ff6b35, #ff8f65);
  position: relative;
  overflow: hidden;
}
.user-header::after {
  content: '';
  position: absolute;
  right: -60rpx;
  top: -60rpx;
  width: 240rpx;
  height: 240rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}
.avatar-wrap { position: relative; }
.avatar-btn {
  padding: 0;
  margin: 0;
  line-height: 1;
  background: transparent;
  border: none;
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
}
.avatar-btn::after { border: none; }
.avatar {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.15);
  background: #fff;
}
.user-meta { margin-left: 28rpx; flex: 1; }
.nickname { font-size: 38rpx; color: #fff; font-weight: bold; display: block; }
.phone-row { display: flex; align-items: center; margin-top: 12rpx; }
.phone { font-size: 26rpx; color: rgba(255, 255, 255, 0.85); }
.phone-badge {
  font-size: 20rpx;
  color: #ff6b35;
  background: #fff;
  border-radius: 20rpx;
  padding: 2rpx 14rpx;
  margin-left: 12rpx;
}
.login-tip { font-size: 24rpx; color: rgba(255, 255, 255, 0.85); margin-top: 10rpx; display: block; }

.balance-card {
  display: flex;
  align-items: center;
  margin: -46rpx 24rpx 0;
  background: #fff;
  border-radius: 20rpx;
  padding: 36rpx 0;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 1;
}
.balance-item { flex: 1; text-align: center; }
.balance-value { font-size: 38rpx; color: #ff6b35; font-weight: bold; display: block; }
.balance-label { font-size: 24rpx; color: #999; margin-top: 10rpx; display: block; }
.balance-divider { width: 1rpx; height: 56rpx; background: #f0f0f0; }
.balance-go { color: #ccc; font-size: 28rpx; padding: 0 24rpx; }

.menu-list {
  margin: 24rpx;
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}
.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid #f5f5f5;
}
.menu-item:last-child { border-bottom: none; }
.menu-hover { background: #fafafa; }
.menu-icon {
  width: 60rpx;
  height: 60rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  margin-right: 24rpx;
}
.icon-order { background: linear-gradient(135deg, #fff1ec, #ffe2d5); }
.icon-balance { background: linear-gradient(135deg, #fff7e6, #ffedc7); }
.icon-bill { background: linear-gradient(135deg, #eef7ff, #dcecff); }
.icon-spread { background: linear-gradient(135deg, #ecfff5, #d6f8e8); }
.icon-settings { background: linear-gradient(135deg, #f3f4f6, #e8eaef); }
.menu-text { flex: 1; font-size: 30rpx; color: #333; }
.chevron {
  width: 16rpx;
  height: 16rpx;
  border-top: 3rpx solid #ccc;
  border-right: 3rpx solid #ccc;
  transform: rotate(45deg);
  flex-shrink: 0;
}
.balance-chevron { margin: 0 28rpx 0 8rpx; }
</style>
