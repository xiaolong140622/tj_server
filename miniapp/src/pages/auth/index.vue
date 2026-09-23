<template>
  <view class="page-auth">
    <view class="auth-deco auth-deco-1"></view>
    <view class="auth-deco auth-deco-2"></view>

    <view class="auth-header">
      <view class="auth-icon-wrap">
        <text class="auth-icon-text">📱</text>
      </view>
      <text class="auth-title">绑定手机号</text>
      <text class="auth-desc">为了您的账户安全，请绑定手机号</text>
    </view>

    <view class="auth-form">
      <view class="field-box">
        <text class="field-prefix">+86</text>
        <view class="field-divider"></view>
        <input class="input-field" type="number" maxlength="11" v-model="phone" placeholder="请输入手机号" placeholder-class="input-placeholder" />
      </view>
      <view class="field-box">
        <input class="input-field sms-input" type="number" maxlength="6" v-model="smsCode" placeholder="请输入验证码" placeholder-class="input-placeholder" />
        <text class="sms-btn" :class="{ disabled: countdown > 0 }" @click="sendCode">
          {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
        </text>
      </view>
      <button class="btn-bind" :class="{ 'btn-disabled': !canBind }" hover-class="btn-hover" @click="onBind">确认绑定</button>
      <view class="auth-tip">
        <text>绑定后可用于账户安全验证与到账提醒</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue';
import { bindMobile, sendSmsCode } from '../../api/auth';
import { useUserStore } from '../../store/user';

const phone = ref('');
const smsCode = ref('');
const countdown = ref(0);
const userStore = useUserStore();
let timer = null;

const canBind = computed(() => phone.value.length === 11 && smsCode.value.length >= 4);

const sendCode = async () => {
  if (countdown.value > 0) return;
  if (!phone.value || phone.value.length !== 11) {
    uni.showToast({ title: '请输入正确手机号', icon: 'none' }); return;
  }
  try {
    await sendSmsCode(phone.value);
    uni.showToast({ title: '已发送', icon: 'success' });
    countdown.value = 60;
    timer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) clearInterval(timer);
    }, 1000);
  } catch (e) {
    uni.showToast({ title: '发送失败', icon: 'none' });
  }
};

const onBind = async () => {
  if (!phone.value || phone.value.length !== 11) {
    uni.showToast({ title: '请输入正确手机号', icon: 'none' }); return;
  }
  if (!smsCode.value || smsCode.value.length < 4) {
    uni.showToast({ title: '请输入验证码', icon: 'none' }); return;
  }
  try {
    await bindMobile({ phone: phone.value, code: smsCode.value });
    uni.showToast({ title: '绑定成功', icon: 'success' });
    await userStore.refreshUserInfo();
    setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 1000);
  } catch (e) {
    uni.showToast({ title: '绑定失败', icon: 'none' });
  }
};

onUnmounted(() => { if (timer) clearInterval(timer); });
</script>

<style scoped>
.page-auth { min-height: 100vh; background: #fff; padding: 0 48rpx; position: relative; overflow: hidden; }
.auth-deco { position: absolute; border-radius: 50%; }
.auth-deco-1 {
  width: 400rpx; height: 400rpx;
  top: -160rpx; right: -140rpx;
  background: radial-gradient(circle, rgba(255, 143, 101, 0.14) 0%, rgba(255, 143, 101, 0) 70%);
}
.auth-deco-2 {
  width: 320rpx; height: 320rpx;
  bottom: -100rpx; left: -120rpx;
  background: radial-gradient(circle, rgba(255, 107, 53, 0.1) 0%, rgba(255, 107, 53, 0) 70%);
}

.auth-header { padding: 100rpx 0 24rpx; display: flex; flex-direction: column; align-items: center; }
.auth-icon-wrap {
  width: 120rpx; height: 120rpx; border-radius: 36rpx;
  background: linear-gradient(135deg, #fff7f0 0%, #ffe9dc 100%);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(255, 107, 53, 0.16);
  margin-bottom: 32rpx;
}
.auth-icon-text { font-size: 56rpx; }
.auth-title { font-size: 40rpx; font-weight: bold; color: #333; }
.auth-desc { font-size: 26rpx; color: #999; margin-top: 14rpx; }

.auth-form { margin-top: 72rpx; position: relative; z-index: 2; }
.field-box {
  display: flex;
  align-items: center;
  height: 100rpx;
  background: #f7f7f7;
  border-radius: 20rpx;
  padding: 0 32rpx;
  margin-bottom: 28rpx;
}
.field-prefix { font-size: 30rpx; color: #333; font-weight: bold; }
.field-divider { width: 2rpx; height: 36rpx; background: #e5e5e5; margin: 0 24rpx; }
.input-field { flex: 1; height: 100rpx; font-size: 30rpx; color: #333; }
.input-placeholder { color: #bbb; }
.sms-input { flex: 1; }
.sms-btn {
  font-size: 26rpx;
  color: #ff6b35;
  white-space: nowrap;
  padding: 10rpx 24rpx;
  background: #fff7f0;
  border-radius: 30rpx;
  border: 2rpx solid #ffd9c7;
}
.sms-btn.disabled { color: #bbb; background: #f5f5f5; border-color: #eee; }

.btn-hover { opacity: 0.9; transform: scale(0.98); }
.btn-bind {
  margin-top: 40rpx;
  height: 92rpx;
  line-height: 92rpx;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8f65 100%);
  color: #fff;
  font-size: 32rpx;
  font-weight: bold;
  border-radius: 46rpx;
  border: none;
  box-shadow: 0 8rpx 24rpx rgba(255, 107, 53, 0.3);
}
.btn-disabled { opacity: 0.5; box-shadow: none; }

.auth-tip { text-align: center; margin-top: 32rpx; font-size: 22rpx; color: #bbb; }
</style>
