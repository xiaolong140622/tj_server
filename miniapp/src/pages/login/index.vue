<template>
  <view class="page-login">
    <view class="login-deco login-deco-1"></view>
    <view class="login-deco login-deco-2"></view>
    <view class="login-deco login-deco-3"></view>

    <view class="login-header">
      <view class="logo-wrap">
        <image class="logo" src="/static/logo.png" mode="aspectFit" />
      </view>
      <text class="app-name">苏分宝</text>
      <text class="app-desc">购物省钱 分享更省</text>
    </view>

    <view class="feature-row">
      <view class="feature-item">
        <text class="feature-dot"></text>
        <text class="feature-text">多平台好物</text>
      </view>
      <view class="feature-item">
        <text class="feature-dot"></text>
        <text class="feature-text">下单有预估奖励</text>
      </view>
      <view class="feature-item">
        <text class="feature-dot"></text>
        <text class="feature-text">好友一起省</text>
      </view>
    </view>

    <view class="login-body">
      <button class="btn-wx-login" hover-class="btn-hover" @click="onWxLogin">
        <view class="wx-icon">
          <text class="wx-icon-text">微</text>
        </view>
        <text>微信一键登录</text>
      </button>

      <view class="login-tips">
        <text>登录即表示同意</text>
        <text class="link" @click.stop="goAgreement">《用户协议》</text>
        <text>和</text>
        <text class="link" @click.stop="goPrivacy">《隐私政策》</text>
      </view>
      <view class="login-disclaimer">
        <text>{{ COPYWRITING.REWARD_DISCLAIMER }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { useUserStore } from '../../store/user';
import { wxLogin } from '../../api/auth';
import { COPYWRITING } from '../../utils/constants';

const userStore = useUserStore();

const onWxLogin = async () => {
  try {
    const loginResult = await uni.login({ provider: 'weixin' });
    // uni.login 的 Promise 结果在不同版本下可能是 [err, res] 或直接 res，做兼容
    const loginRes = Array.isArray(loginResult) ? (loginResult[1] || loginResult[0]) : loginResult;
    if (!loginRes || !loginRes.code) {
      uni.showToast({ title: '登录失败', icon: 'none' });
      return;
    }
    const res = await wxLogin(loginRes.code);
    const data = res.result || res.data || {};
    if (data.token) {
      userStore.setLoginInfo(data.token, data.userInfo || {});
      // 服务器暂无短信通道，验证码绑定环节暂时关闭，直接进入首页
      uni.switchTab({ url: '/pages/index/index' });
    } else {
      uni.showToast({ title: '登录失败', icon: 'none' });
    }
  } catch (e) {
    console.error('wxLogin failed', e);
    uni.showToast({ title: '登录失败', icon: 'none' });
  }
};

const goAgreement = () => uni.navigateTo({ url: '/pages/webview/index?type=agreement' });
const goPrivacy = () => uni.navigateTo({ url: '/pages/webview/index?type=privacy' });
</script>

<style scoped>
.page-login {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #fff;
  padding: 0 60rpx;
  position: relative;
  overflow: hidden;
}
.login-deco { position: absolute; border-radius: 50%; }
.login-deco-1 {
  width: 480rpx; height: 480rpx;
  top: -180rpx; right: -160rpx;
  background: radial-gradient(circle, rgba(255, 143, 101, 0.18) 0%, rgba(255, 143, 101, 0) 70%);
}
.login-deco-2 {
  width: 400rpx; height: 400rpx;
  bottom: -120rpx; left: -140rpx;
  background: radial-gradient(circle, rgba(255, 107, 53, 0.12) 0%, rgba(255, 107, 53, 0) 70%);
}
.login-deco-3 {
  width: 200rpx; height: 200rpx;
  top: 320rpx; left: -80rpx;
  background: radial-gradient(circle, rgba(7, 193, 96, 0.08) 0%, rgba(7, 193, 96, 0) 70%);
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 190rpx;
}
.logo-wrap {
  width: 176rpx; height: 176rpx;
  border-radius: 44rpx;
  background: linear-gradient(135deg, #fff7f0 0%, #ffe9dc 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12rpx 32rpx rgba(255, 107, 53, 0.2);
  margin-bottom: 32rpx;
}
.logo { width: 120rpx; height: 120rpx; }
.app-name { font-size: 48rpx; font-weight: bold; color: #333; letter-spacing: 4rpx; }
.app-desc { font-size: 26rpx; color: #999; margin-top: 14rpx; letter-spacing: 2rpx; }

.feature-row {
  display: flex;
  align-items: center;
  gap: 32rpx;
  margin-top: 56rpx;
}
.feature-item { display: flex; align-items: center; }
.feature-dot {
  width: 10rpx; height: 10rpx; border-radius: 50%;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8f65 100%);
  margin-right: 10rpx;
}
.feature-text { font-size: 24rpx; color: #666; }

.login-body { width: 100%; margin-top: 100rpx; }
.btn-hover { opacity: 0.9; transform: scale(0.98); }
.btn-wx-login {
  width: 100%;
  height: 92rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #09c269 0%, #07a356 100%);
  color: #fff;
  font-size: 32rpx;
  font-weight: bold;
  border-radius: 46rpx;
  border: none;
  box-shadow: 0 8rpx 24rpx rgba(7, 193, 96, 0.3);
  padding: 0;
}
.wx-icon {
  width: 44rpx; height: 44rpx; border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  display: flex; align-items: center; justify-content: center;
  margin-right: 16rpx;
}
.wx-icon-text { font-size: 24rpx; color: #fff; font-weight: bold; }

.login-tips {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  margin-top: 48rpx;
  font-size: 24rpx;
  color: #999;
}
.link { color: #ff6b35; }
.login-disclaimer { text-align: center; margin-top: 20rpx; font-size: 20rpx; color: #ccc; }
</style>
