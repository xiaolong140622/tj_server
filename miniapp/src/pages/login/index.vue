<template>
  <view class="page-login">
    <view class="login-header">
      <view class="logo-wrap">
        <image class="logo" src="/static/logo.png" mode="aspectFit" />
      </view>
      <text class="app-name">苏分宝</text>
      <text class="app-desc">购物省钱 分享更省</text>
    </view>

    <view class="value-card">
      <text class="value-title">买东西，先省一笔</text>
      <view class="benefit-row">
        <view class="benefit-icon icon-platform"></view>
        <text class="benefit-text">覆盖淘宝/京东/拼多多等主流平台</text>
      </view>
      <view class="benefit-row">
        <view class="benefit-icon icon-receipt"></view>
        <text class="benefit-text">下单即计奖励，笔笔可查</text>
      </view>
      <view class="benefit-row">
        <view class="benefit-icon icon-friend"></view>
        <text class="benefit-text">好友下单，你也能省</text>
      </view>
    </view>

    <view class="login-body">
      <button
        class="btn-wx-login"
        :class="{ 'btn-wx-disabled': !agreed }"
        hover-class="btn-hover"
        :disabled="logging"
        @click="onWxLogin"
      >
        <view v-if="logging" class="btn-spinner"></view>
        <view v-else class="wx-icon">
          <text class="wx-icon-text">微</text>
        </view>
        <text>{{ logging ? '登录中…' : '微信一键登录' }}</text>
      </button>

      <view class="agree-row" @click="agreed = !agreed">
        <view class="agree-box" :class="{ 'agree-box-on': agreed }">
          <view v-if="agreed" class="agree-tick"></view>
        </view>
        <view class="agree-text">
          <text>我已阅读并同意</text>
          <text class="link" @click.stop="goAgreement">《用户协议》</text>
          <text>与</text>
          <text class="link" @click.stop="goPrivacy">《隐私政策》</text>
        </view>
      </view>
      <view class="login-disclaimer">
        <text>{{ COPYWRITING.REWARD_DISCLAIMER }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { useUserStore } from '../../store/user';
import { wxLogin } from '../../api/auth';
import { COPYWRITING } from '../../utils/constants';

const userStore = useUserStore();
const agreed = ref(false);
const logging = ref(false);

const onWxLogin = async () => {
  if (!agreed.value) {
    uni.showToast({ title: '请先阅读并同意协议', icon: 'none' });
    return;
  }
  if (logging.value) return;
  logging.value = true;
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
    uni.showToast({ title: '网络开小差了，请稍后再试', icon: 'none' });
  } finally {
    logging.value = false;
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
  background: linear-gradient(180deg, #FFF3EC 0%, #FFFFFF 42%);
  padding: 0 60rpx;
  overflow: hidden;
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 140rpx;
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
.app-name { font-size: 48rpx; font-weight: bold; color: #1F2126; letter-spacing: 4rpx; }
.app-desc { font-size: 26rpx; color: #7A7F89; margin-top: 14rpx; letter-spacing: 2rpx; }

.value-card {
  width: 100%;
  margin-top: 64rpx;
  background: #fff;
  border-radius: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(31, 33, 38, 0.06);
  padding: 40rpx 36rpx 12rpx;
  box-sizing: border-box;
}
.value-title { display: block; font-size: 40rpx; font-weight: bold; color: #1F2126; }
.benefit-row { display: flex; align-items: center; height: 72rpx; }
.benefit-icon {
  width: 36rpx; height: 36rpx; margin-right: 20rpx; flex-shrink: 0;
  background-repeat: no-repeat; background-position: center; background-size: 36rpx;
}
.icon-platform {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23FF6B35' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'%3E%3Crect x='3' y='4' width='18' height='14' rx='2'/%3E%3Cpath d='M8 21h8M12 18v3'/%3E%3C/svg%3E");
}
.icon-receipt {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23FF6B35' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M5 3h14v18l-2.5-1.5L14 21l-2-1.5L10 21l-2.5-1.5L5 21z'/%3E%3Cpath d='M9 8h6M9 12h6'/%3E%3C/svg%3E");
}
.icon-friend {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23FF6B35' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='9' cy='8' r='3.2'/%3E%3Cpath d='M3.5 19c.6-3 2.9-4.8 5.5-4.8s4.9 1.8 5.5 4.8'/%3E%3Ccircle cx='17' cy='9' r='2.6'/%3E%3Cpath d='M15.8 14.4c2.4.1 4.2 1.7 4.7 4.1'/%3E%3C/svg%3E");
}
.benefit-text { font-size: 27rpx; color: #7A7F89; }

.login-body { width: 100%; margin-top: 88rpx; }
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
  transition: background-color 0.15s, opacity 0.15s;
}
.btn-wx-disabled {
  background: #BFD9CB;
  box-shadow: none;
}
.btn-spinner {
  width: 34rpx; height: 34rpx;
  border: 4rpx solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  margin-right: 16rpx;
  animation: btn-spin 0.8s linear infinite;
}
@keyframes btn-spin { to { transform: rotate(360deg); } }
.wx-icon {
  width: 44rpx; height: 44rpx; border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  display: flex; align-items: center; justify-content: center;
  margin-right: 16rpx;
}
.wx-icon-text { font-size: 24rpx; color: #fff; font-weight: bold; }

.agree-row {
  display: flex;
  align-items: flex-start;
  margin-top: 44rpx;
  padding: 4rpx 8rpx;
}
.agree-box {
  width: 34rpx; height: 34rpx; flex-shrink: 0;
  border-radius: 50%;
  border: 2rpx solid #D5D8DD;
  margin-right: 14rpx;
  margin-top: 2rpx;
  display: flex; align-items: center; justify-content: center;
  box-sizing: border-box;
  transition: border-color 0.15s, background-color 0.15s;
}
.agree-box-on { background: #09c269; border-color: #09c269; }
.agree-tick {
  width: 16rpx; height: 8rpx;
  border-left: 3rpx solid #fff;
  border-bottom: 3rpx solid #fff;
  transform: rotate(-45deg) translateY(-2rpx);
}
.agree-text { display: flex; flex-wrap: wrap; font-size: 24rpx; color: #999; line-height: 40rpx; }
.link { color: #ff6b35; }
.login-disclaimer { text-align: center; margin-top: 20rpx; font-size: 20rpx; color: #ccc; }
</style>
