<template>
  <view class="page-user">
    <!-- Hero 头部 -->
    <view class="hero">
      <view class="hero-deco deco-lg"></view>
      <view class="hero-deco deco-sm"></view>

      <view class="hero-user" v-if="isLoggedIn">
        <view class="avatar-wrap">
          <button
            class="avatar-btn"
            open-type="chooseAvatar"
            :disabled="avatarUploading"
            @chooseavatar="onChooseAvatar"
            @error="onChooseAvatarError"
          >
            <image class="avatar" :src="avatarDisplaySrc" mode="aspectFill" @error="onAvatarImgError" />
            <view class="avatar-mask" v-if="avatarUploading">
              <view class="spinner"></view>
            </view>
          </button>
          <view class="avatar-cam" :style="{ backgroundImage: camIcon }"></view>
        </view>
        <view class="user-meta">
          <text class="nickname">{{ nickname }}</text>
          <view class="sub-row" v-if="isDefaultNickname" @click="openProfile">
            <text class="sub-guide">点击完善头像昵称</text>
          </view>
          <view class="id-row" v-if="displayUid">
            <text class="id-text">苏分宝 ID: {{ displayUid }}</text>
            <view class="id-copy" hover-class="id-copy-hover" @click.stop="copyUid">
              <view class="id-copy-icon" :style="{ backgroundImage: copyIcon }"></view>
              <text class="id-copy-text">复制</text>
            </view>
          </view>
        </view>
        <view class="profile-entry" hover-class="profile-entry-hover" @click="openProfile">
          <text class="profile-entry-text">完善资料</text>
          <view class="chevron chevron-onprimary"></view>
        </view>
      </view>

      <view class="hero-user" v-else @click="goLogin">
        <view class="avatar-wrap">
          <image class="avatar avatar-guest" src="/static/logo.png" mode="aspectFill" />
          <view class="avatar-cam" :style="{ backgroundImage: camIcon }"></view>
        </view>
        <view class="user-meta">
          <text class="nickname">点击登录</text>
          <text class="sub sub-inline">登录后同步奖励与订单</text>
        </view>
        <view class="guest-cta">
          <text class="guest-cta-text">微信一键登录</text>
        </view>
      </view>
    </view>

    <!-- 资产主卡（深色记忆点） -->
    <view class="ink-card">
      <view class="ink-top">
        <view class="ink-balance">
          <text class="ink-label">可用余额(元)</text>
          <text class="ink-value">{{ availableText }}</text>
        </view>
        <view
          class="ink-withdraw"
          :class="{ 'ink-withdraw-disabled': isLoggedIn && !withdrawable }"
          hover-class="ink-withdraw-hover"
          @click="onWithdraw"
        >
          <text class="ink-withdraw-text">去提现</text>
          <view class="chevron chevron-onprimary"></view>
        </view>
      </view>
      <view class="ink-row" @click="onAssetClick">
        <view class="ink-col" v-for="col in rewardCols" :key="col.label">
          <template v-if="rewardLoading && !reward">
            <view class="skel-dark skel-value"></view>
            <view class="skel-dark skel-label"></view>
          </template>
          <template v-else>
            <text class="ink-col-value">{{ displayValue(col) }}</text>
            <text class="ink-col-label">{{ col.label }}</text>
          </template>
        </view>
      </view>
      <view class="ink-foot" @click="onBillClick">
        <text class="ink-foot-text">{{ rewardError ? '加载失败，点击重试' : '查看账单明细' }}</text>
        <view class="chevron chevron-onprimary"></view>
      </view>
    </view>

    <!-- 好友双入口卡（J4：带参跳现有 spread 页） -->
    <view class="friend-card">
      <view class="friend-cell" hover-class="friend-cell-hover" @click="goPage('/pages/user/spread?tab=friend')">
        <view class="friend-icon" :style="{ backgroundImage: ICONS.friends('#FF6B35') }"></view>
        <text class="friend-text">好友列表</text>
      </view>
      <view class="friend-divider"></view>
      <view class="friend-cell" hover-class="friend-cell-hover" @click="goPage('/pages/user/spread?tab=invite')">
        <view class="friend-icon" :style="{ backgroundImage: ICONS.invite('#FF6B35') }"></view>
        <text class="friend-text">邀请好友</text>
      </view>
    </view>

    <!-- 功能宫格 -->
    <view class="grid-card">
      <view
        class="grid-cell"
        v-for="item in menuItems"
        :key="item.label"
        hover-class="grid-cell-hover"
        @click="goPage(item.url)"
      >
        <view class="grid-icon" :class="item.bgClass" :style="{ backgroundImage: item.icon }"></view>
        <text class="grid-text">{{ item.label }}</text>
      </view>
    </view>

    <text class="page-version">苏分宝 v1.0.0</text>

    <!-- 完善头像昵称抽屉（与设置页共用组件） -->
    <ProfileDrawer v-model:visible="profileVisible" />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { useUserStore } from '../../store/user';
import { useAppStore } from '../../store/app';
import { get } from '../../utils/request';
import { formatMoney } from '../../utils/format';
import { camIcon, copyIcon, ICONS } from '../../utils/icons';
import { useAvatar } from '../../utils/useAvatar';
import ProfileDrawer from '../../components/ProfileDrawer.vue';

const userStore = useUserStore();
const appStore = useAppStore();
const { avatarUploading, avatarDisplaySrc, onChooseAvatar, onChooseAvatarError, onAvatarImgError } = useAvatar();

/* ---------- 用户信息 ---------- */
const isLoggedIn = computed(() => userStore.isLoggedIn);
const displayUser = computed(() => userStore.userInfo || {});
const nickname = computed(() => displayUser.value?.nickname || '用户');
const isDefaultNickname = computed(
  () => !displayUser.value?.nickname || displayUser.value.nickname === '微信用户');
const displayUid = computed(() => (isLoggedIn.value ? displayUser.value?.uid || '' : ''));

const copyUid = () => {
  uni.setClipboardData({
    data: String(displayUid.value),
    success: () => uni.showToast({ title: 'ID 已复制', icon: 'success' }),
  });
};

/* ---------- 可用余额与提现（J1 口径：金额=now_money；阈值=GET /extract/bank minPrice） ---------- */
const availableText = computed(() => {
  if (!isLoggedIn.value) return '--.--';
  return formatMoney(displayUser.value?.nowMoney ?? 0);
});
const minPrice = ref(null);
const loadExtractMin = async () => {
  if (!isLoggedIn.value) return;
  try {
    const res = await get('/extract/bank', {}, { silent: true });
    const d = res.result || res.data || {};
    const n = parseFloat(d.minPrice);
    minPrice.value = Number.isNaN(n) ? null : n;
  } catch (e) { /* 取不到阈值时仅按余额>0 判定 */ }
};
const balanceNum = computed(() => {
  const n = parseFloat(displayUser.value?.nowMoney ?? 0);
  return Number.isNaN(n) ? 0 : n;
});
const withdrawable = computed(() => {
  if (balanceNum.value <= 0) return false;
  if (minPrice.value != null && balanceNum.value < minPrice.value) return false;
  return true;
});
const onWithdraw = () => {
  if (!isLoggedIn.value) { goLogin(); return; }
  if (balanceNum.value <= 0) {
    uni.showToast({ title: '暂无可提现余额', icon: 'none' });
    return;
  }
  if (minPrice.value != null && balanceNum.value < minPrice.value) {
    uni.showToast({ title: `满 ${minPrice.value} 元可提现`, icon: 'none' });
    return;
  }
  uni.navigateTo({ url: '/pages/user/balance' });
};

/* ---------- 奖励三值（契约：GET /user/reward/summary；失败 0 + 点击重试，QC-01） ---------- */
const reward = ref(null);
const rewardLoading = ref(false);
const rewardError = ref(false);

const rewardCols = computed(() => [
  { label: '待结算', value: reward.value ? reward.value.pending : null },
  { label: '已到账', value: reward.value ? reward.value.settled : null },
  { label: '累计奖励', value: reward.value ? reward.value.total : null },
]);
const displayValue = (col) => {
  if (!isLoggedIn.value) return '--.--';
  if (rewardError.value) return formatMoney(0); // 失败显示 0 + 重试引导；正式链路禁止假数据
  if (col.value === null || col.value === undefined) return '--.--';
  return formatMoney(col.value);
};

const loadReward = async () => {
  if (!isLoggedIn.value) { reward.value = null; rewardError.value = false; return; }
  rewardLoading.value = true;
  try {
    const res = await get('/user/reward/summary', {}, { silent: true });
    const d = res.result || res.data || {};
    reward.value = {
      total: d.totalReward || 0,
      pending: d.pendingReward || 0,
      settled: d.settledReward || 0,
    };
    rewardError.value = false;
  } catch (e) {
    rewardError.value = true;
  } finally {
    rewardLoading.value = false;
  }
};

/* ---------- 好友双入口卡（J4） ---------- */

/* ---------- 功能宫格（J6 注销未实现不进格；地址页待产品确认设计稿） ---------- */
const menuItems = [
  {
    label: '我的订单', url: '/pages/order/list', bgClass: 'icon-order',
    icon: ICONS.order('#FF6B35'),
  },
  {
    label: '账单明细', url: '/pages/user/bill', bgClass: 'icon-bill',
    icon: ICONS.bill('#1890FF'),
  },
  {
    label: '推广中心', url: '/pages/user/spread', bgClass: 'icon-spread',
    icon: ICONS.spread('#07C160'),
  },
  {
    label: '设置', url: '/pages/user/settings', bgClass: 'icon-settings',
    icon: ICONS.settings('#6B7280'),
  },
];

/* ---------- 完善资料抽屉 ---------- */
const profileVisible = ref(false);
const openProfile = () => {
  if (!isLoggedIn.value) { goLogin(); return; }
  profileVisible.value = true;
};

/* ---------- 导航 ---------- */
const goLogin = () => uni.navigateTo({ url: '/pages/login/index' });
// tabBar 页 navigateTo 必静默失败：统一 switchTab；spread 三段 tab 用全局意图传参（switchTab 不支持 query）
const TAB_PAGES = ['/pages/index/index', '/pages/order/list', '/pages/user/spread', '/pages/user/index'];
const goPage = (url) => {
  if (!isLoggedIn.value) { goLogin(); return; }
  const [path, query] = url.split('?');
  if (path === '/pages/user/spread' && query) {
    const pair = query.split('&').find((s) => s.startsWith('tab='));
    if (pair) appStore.setSpreadTabIntent(pair.slice(4));
  }
  if (TAB_PAGES.includes(path)) { uni.switchTab({ url: path }); return; }
  uni.navigateTo({ url });
};
const onAssetClick = () => {
  if (!isLoggedIn.value) { goLogin(); return; }
  if (rewardError.value && !rewardLoading.value) { loadReward(); }
};
const onBillClick = () => {
  if (!isLoggedIn.value) { goLogin(); return; }
  if (rewardError.value || rewardLoading.value) { loadReward(); return; }
  uni.navigateTo({ url: '/pages/user/bill' });
};

onShow(() => {
  if (!isLoggedIn.value) {
    reward.value = null;
    return;
  }
  loadReward();
  loadExtractMin();
});
</script>

<style scoped>
.page-user {
  --primary: #FF6B35;
  --hero-gradient: linear-gradient(160deg, #FF6B35 0%, #FF9A62 100%);
  --ink-gradient: linear-gradient(150deg, #2B211E 0%, #1F1B1A 100%);
  --gold: #FFD9C2;
  --page-bg: #F6F7F9;
  --card-bg: #FFFFFF;
  --text-main: #1F2126;
  --text-sub: #7A7F89;
  --text-weak: #B6BAC2;
  --divider: #F2F3F5;
  --radius-card: 24rpx;
  --shadow-card: 0 8rpx 32rpx rgba(31, 33, 38, 0.06);
  --shadow-float: 0 12rpx 40rpx rgba(255, 107, 53, 0.18);
  min-height: 100vh;
  background: var(--page-bg);
  padding-bottom: calc(32rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

/* ---------- Hero ---------- */
.hero {
  position: relative;
  overflow: hidden;
  padding: 70rpx 32rpx 96rpx;
  background: var(--hero-gradient);
}
.hero-deco { position: absolute; border-radius: 50%; }
.deco-lg { width: 320rpx; height: 320rpx; right: -80rpx; top: -100rpx; background: rgba(255, 255, 255, 0.10); }
.deco-sm { width: 160rpx; height: 160rpx; left: -50rpx; bottom: -60rpx; background: rgba(255, 255, 255, 0.06); }
.hero-user { display: flex; align-items: center; position: relative; }

.avatar-wrap { position: relative; flex-shrink: 0; }
.avatar-btn {
  padding: 0; margin: 0; line-height: 1; background: transparent; border: none;
  width: 128rpx; height: 128rpx; border-radius: 50%;
  position: relative; overflow: hidden;
}
.avatar-btn::after { border: none; }
.avatar {
  width: 128rpx; height: 128rpx; border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.65);
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.12);
  background: #fff; box-sizing: border-box;
}
.avatar-guest { width: 112rpx; height: 112rpx; }
.avatar-mask {
  position: absolute; left: 0; top: 0; right: 0; bottom: 0;
  border-radius: 50%; background: rgba(255, 255, 255, 0.4);
  display: flex; align-items: center; justify-content: center;
}
.spinner {
  width: 40rpx; height: 40rpx; border-radius: 50%;
  border: 5rpx solid rgba(255, 107, 53, 0.25); border-top-color: var(--primary);
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.avatar-cam {
  position: absolute; right: -2rpx; bottom: 6rpx;
  width: 36rpx; height: 36rpx; border-radius: 50%;
  background-color: #fff; background-repeat: no-repeat; background-position: center; background-size: 22rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.15);
}

.user-meta { margin-left: 28rpx; flex: 1; min-width: 0; }
.nickname {
  font-size: 40rpx; color: #fff; font-weight: bold; display: block;
  max-width: 360rpx; overflow: hidden; white-space: nowrap; text-overflow: ellipsis;
}
.sub-row { margin-top: 12rpx; }
.sub { font-size: 26rpx; color: rgba(255, 255, 255, 0.85); }
.sub-inline { display: block; margin-top: 12rpx; }
.sub-guide { font-size: 26rpx; color: rgba(255, 255, 255, 0.95); text-decoration: underline; }

.id-row { display: flex; align-items: center; margin-top: 12rpx; }
.id-text { font-size: 24rpx; color: rgba(255, 255, 255, 0.85); }
.id-copy {
  display: flex; align-items: center;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 999rpx; padding: 4rpx 16rpx; margin-left: 14rpx;
  transition: background-color 0.15s;
}
.id-copy-hover { background: rgba(255, 255, 255, 0.32); }
.id-copy-icon {
  width: 22rpx; height: 22rpx; margin-right: 6rpx;
  background-repeat: no-repeat; background-position: center; background-size: 22rpx;
}
.id-copy-text { font-size: 22rpx; color: #fff; }

.profile-entry {
  display: flex; align-items: center;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 999rpx; padding: 8rpx 20rpx; flex-shrink: 0;
}
.profile-entry-hover { background: rgba(255, 255, 255, 0.3); }
.profile-entry-text { font-size: 24rpx; color: #fff; margin-right: 6rpx; }

.guest-cta {
  background: #fff; border-radius: 999rpx; padding: 12rpx 32rpx; flex-shrink: 0;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.1);
}
.guest-cta-text { font-size: 28rpx; font-weight: bold; color: var(--primary); }

/* ---------- 资产主卡（深色） ---------- */
.ink-card {
  margin: -56rpx 24rpx 0;
  background: var(--ink-gradient);
  border: 1rpx solid rgba(255, 107, 53, 0.35);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-float);
  position: relative; z-index: 1;
  overflow: hidden;
}
.ink-top {
  display: flex; align-items: center; justify-content: space-between;
  padding: 36rpx 32rpx 8rpx;
}
.ink-balance { display: flex; flex-direction: column; }
.ink-label { font-size: 26rpx; color: rgba(255, 255, 255, 0.65); }
.ink-value { font-size: 56rpx; font-weight: bold; color: var(--gold); margin-top: 8rpx; line-height: 1.1; }
.ink-withdraw {
  display: flex; align-items: center;
  background: linear-gradient(135deg, #FF6B35, #FF8F65);
  border-radius: 999rpx; padding: 14rpx 28rpx;
  box-shadow: 0 6rpx 20rpx rgba(255, 107, 53, 0.35);
  transition: opacity 0.15s;
}
.ink-withdraw-hover { opacity: 0.85; }
.ink-withdraw-disabled { background: rgba(255, 255, 255, 0.25); box-shadow: none; }
.ink-withdraw-text { font-size: 26rpx; font-weight: bold; color: #fff; margin-right: 6rpx; }

.ink-row { display: flex; align-items: center; padding: 24rpx 0 28rpx; }
.ink-col { flex: 1; display: flex; flex-direction: column; align-items: center; position: relative; }
.ink-col + .ink-col::before {
  content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%);
  width: 1rpx; height: 56rpx; background: rgba(255, 255, 255, 0.12);
}
.ink-col-value { font-size: 32rpx; font-weight: bold; color: rgba(255, 255, 255, 0.88); }
.ink-col-label { font-size: 24rpx; color: rgba(255, 255, 255, 0.65); margin-top: 8rpx; }

.skel-dark {
  background: linear-gradient(90deg, rgba(255,255,255,0.12) 25%, rgba(255,255,255,0.20) 37%, rgba(255,255,255,0.12) 63%);
  background-size: 400% 100%;
  animation: shimmer 1.2s ease infinite;
  border-radius: 8rpx;
}
@keyframes shimmer { 0% { background-position: 100% 0; } 100% { background-position: 0 0; } }
.skel-value { width: 110rpx; height: 32rpx; }
.skel-label { width: 72rpx; height: 22rpx; margin-top: 14rpx; }

.ink-foot {
  height: 72rpx; display: flex; align-items: center; justify-content: center;
  border-top: 1rpx solid rgba(255, 255, 255, 0.10);
}
.ink-foot-text { font-size: 26rpx; color: rgba(255, 255, 255, 0.65); margin-right: 8rpx; }

/* ---------- 好友双入口卡 ---------- */
.friend-card {
  margin: 24rpx 24rpx 0;
  background: var(--card-bg);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  display: flex; align-items: center;
  height: 150rpx;
}
.friend-cell {
  flex: 1; height: 100%;
  display: flex; align-items: center; justify-content: center;
  transition: background-color 0.15s;
}
.friend-cell-hover { background: #FAFBFC; }
.friend-icon {
  width: 48rpx; height: 48rpx; margin-right: 16rpx; flex-shrink: 0;
  background-repeat: no-repeat; background-position: center; background-size: 48rpx;
}
.friend-text { font-size: 28rpx; color: var(--text-main); }
.friend-divider { width: 1rpx; height: 64rpx; background: var(--divider); }

/* ---------- 功能宫格 ---------- */
.grid-card {
  margin: 24rpx 24rpx 0;
  background: var(--card-bg);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  display: flex;
  flex-wrap: wrap;
  padding: 12rpx 0;
}
.grid-cell {
  width: 25%;
  height: 150rpx;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  transition: transform 0.15s;
}
.grid-cell-hover { transform: scale(0.97); }
.grid-cell-hover .grid-icon { filter: brightness(0.94); }
.grid-icon {
  width: 88rpx; height: 88rpx; border-radius: 24rpx;
  background-repeat: no-repeat; background-position: center; background-size: 56rpx;
}
.icon-order { background-color: #FFF1EC; }
.icon-balance { background-color: #FFF7E6; }
.icon-bill { background-color: #EEF7FF; }
.icon-spread { background-color: #ECFFF5; }
.icon-settings { background-color: #F3F4F6; }
.grid-text { font-size: 26rpx; color: var(--text-main); margin-top: 12rpx; }

.chevron {
  width: 14rpx; height: 14rpx; flex-shrink: 0;
  border-top: 3rpx solid var(--text-weak); border-right: 3rpx solid var(--text-weak);
  transform: rotate(45deg);
}
.chevron-onprimary { border-top-color: rgba(255, 255, 255, 0.9); border-right-color: rgba(255, 255, 255, 0.9); }

.page-version {
  display: block; text-align: center;
  font-size: 26rpx; color: #BBBBBB;
  padding-top: 48rpx;
}
</style>
