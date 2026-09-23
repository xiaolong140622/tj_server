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
          <view class="sub-row" v-else>
            <text class="sub">{{ maskedPhone || '未绑定手机号' }}</text>
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

    <!-- 资产卡 -->
    <view class="asset-card" hover-class="asset-card-hover" @click="onAssetClick">
      <view class="asset-row">
        <view class="asset-col" v-for="col in rewardCols" :key="col.label">
          <template v-if="rewardLoading && !reward">
            <view class="skel skel-value"></view>
            <view class="skel skel-label"></view>
          </template>
          <template v-else>
            <view class="asset-value-wrap">
              <text class="asset-symbol">¥</text>
              <text class="asset-value">{{ displayValue(col) }}</text>
            </view>
            <text class="asset-label">{{ col.label }}</text>
          </template>
        </view>
      </view>
      <view class="asset-foot" @click.stop="onBillClick">
        <text class="asset-foot-text">{{ rewardError ? '加载失败，点击重试' : '查看账单明细' }}</text>
        <view class="chevron"></view>
      </view>
    </view>

    <!-- 功能分组 -->
    <block v-for="group in menuGroups" :key="group.title">
      <text class="group-title">{{ group.title }}</text>
      <view class="menu-card">
        <view
          class="menu-item"
          v-for="item in group.items"
          :key="item.label"
          hover-class="menu-hover"
          @click="goPage(item.url)"
        >
          <view class="menu-icon" :class="item.bgClass" :style="{ backgroundImage: item.icon }"></view>
          <text class="menu-text">{{ item.label }}</text>
          <view class="chevron"></view>
        </view>
      </view>
    </block>

    <text class="page-version">苏分宝 v1.0.0</text>

    <!-- 完善资料半屏抽屉 -->
    <view class="drawer-mask" v-if="profileVisible" @click="closeProfile">
      <view class="drawer" @click.stop>
        <view class="drawer-head">
          <text class="drawer-title">完善个人资料</text>
          <view class="drawer-close" hover-class="drawer-close-hover" @click="closeProfile">
            <text class="drawer-close-icon">×</text>
          </view>
        </view>
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
          <text class="drawer-avatar-tip">点击更换头像</text>
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
  </view>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { useUserStore } from '../../store/user';
import { editUser } from '../../api/user';
import { get } from '../../utils/request';
import { formatMoney, maskPhone } from '../../utils/format';
import { BASE_URL, TOKEN_KEY } from '../../utils/constants';

const userStore = useUserStore();

/* ---------- 内联 SVG 线性图标（避免新增位图，emoji 仅作降级） ---------- */
const lineIcon = (stroke, inner) =>
  `url("data:image/svg+xml,${encodeURIComponent(
    `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='${stroke}' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'>${inner}</svg>`
  )}")`;
const camIcon = lineIcon('#FF6B35',
  "<rect x='3' y='7.5' width='18' height='12.5' rx='2'/><circle cx='12' cy='13.8' r='3.2'/><path d='M8.6 7.5 10.1 5h3.8l1.5 2.5'/>");

const menuGroups = [
  {
    title: '交易服务',
    items: [
      {
        label: '我的订单', url: '/pages/order/list', bgClass: 'icon-order',
        icon: lineIcon('#FF6B35', "<path d='M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z'/><path d='M14 3v5h5'/><path d='M9 13h6M9 17h4'/>"),
      },
      {
        label: '我的余额', url: '/pages/user/balance', bgClass: 'icon-balance',
        icon: lineIcon('#FA8C16', "<circle cx='8.5' cy='8.5' r='5.5'/><circle cx='15.5' cy='15.5' r='5.5'/>"),
      },
      {
        label: '账单明细', url: '/pages/user/bill', bgClass: 'icon-bill',
        icon: lineIcon('#1890FF', "<path d='M4 6h2M4 12h2M4 18h2M10 6h10M10 12h10M10 18h7'/>"),
      },
    ],
  },
  {
    title: '推广与服务',
    items: [
      {
        label: '推广中心', url: '/pages/user/spread', bgClass: 'icon-spread',
        icon: lineIcon('#07C160', "<circle cx='6' cy='12' r='2.5'/><circle cx='18' cy='6' r='2.5'/><circle cx='18' cy='18' r='2.5'/><path d='M8.2 10.9l7.6-3.8M8.2 13.1l7.6 3.8'/>"),
      },
      {
        label: '设置', url: '/pages/user/settings', bgClass: 'icon-settings',
        icon: lineIcon('#6B7280', "<path d='M4 6h4M14 6h6M4 12h8M18 12h2M4 18h2M12 18h8'/><circle cx='11' cy='6' r='2.3'/><circle cx='15.5' cy='12' r='2.3'/><circle cx='9' cy='18' r='2.3'/>"),
      },
    ],
  },
];

/* ---------- 用户信息 ---------- */
const isLoggedIn = computed(() => userStore.isLoggedIn);
const displayUser = computed(() => userStore.userInfo || {});
const nickname = computed(() => displayUser.value?.nickname || '用户');
const isDefaultNickname = computed(
  () => !displayUser.value?.nickname || displayUser.value.nickname === '微信用户');
const maskedPhone = computed(() => maskPhone(displayUser.value?.phone || displayUser.value?.mobile || ''));

/* ---------- 头像：换头像链路与状态 ---------- */
const avatarPreview = ref('');
const avatarUploading = ref(false);
const avatarBroken = ref(false);

// QC-03 口径：avatar 为空才显示 logo 占位；任何有效 URL 直接加载，失败经 binderror 回落 logo
const serverAvatar = computed(() => {
  const raw = displayUser.value?.avatar || '';
  if (!raw) return '/static/logo.png';
  return raw.startsWith('/') ? BASE_URL + raw : raw;
});
const avatarDisplaySrc = computed(() => {
  if (avatarPreview.value) return avatarPreview.value;
  if (avatarBroken.value) return '/static/logo.png';
  return serverAvatar.value;
});
watch(serverAvatar, () => { avatarBroken.value = false; });

const onAvatarImgError = () => { avatarBroken.value = true; };

// chooseAvatar 临时文件在 Windows 开发者工具存在读取竞态（ENOENT，模拟器已知问题）：
// 先复制到 USER_DATA_PATH 稳定路径（含延时重试）再上传；上传中锁重复点击
const copyToStable = (src, ext) =>
  new Promise((resolve, reject) => {
    const base = typeof wx !== 'undefined' && wx.env ? wx.env.USER_DATA_PATH : '';
    if (!base) { reject(new Error('USER_DATA_PATH unavailable')); return; }
    const dest = `${base}/avatar_pick_${Date.now()}.${ext}`;
    let attemptsLeft = 2;
    const once = () => {
      uni.getFileSystemManager().copyFile({
        srcPath: src,
        destPath: dest,
        success: () => resolve(dest),
        fail: (err) => {
          if (attemptsLeft > 0) { attemptsLeft -= 1; setTimeout(once, 300); }
          else reject(err);
        },
      });
    };
    once();
  });

const rollbackAvatar = (msg) => {
  avatarPreview.value = ''; // 失败回滚为原头像
  uni.showToast({ title: msg, icon: 'none' });
};

const uploadAvatar = (filePath) => {
  uni.uploadFile({
    url: BASE_URL + '/user/avatar/upload',
    filePath,
    name: 'file',
    header: { Authorization: `Bearer ${uni.getStorageSync(TOKEN_KEY)}` },
    success: async (res) => {
      let body = null;
      try { body = JSON.parse(res.data); } catch (e) { body = null; }
      const url = body && body.data ? body.data.url : '';
      if (res.statusCode !== 200 || !url) {
        rollbackAvatar((body && body.msg) || '头像上传失败，请确认后端服务后重试');
        return;
      }
      try {
        await editUser({ avatar: BASE_URL + url });
        await userStore.refreshUserInfo();
        avatarPreview.value = '';
        uni.showToast({ title: '头像已更新', icon: 'success' });
      } catch (e) {
        rollbackAvatar('头像保存失败，请重试');
      }
    },
    fail: () => rollbackAvatar('头像上传失败，请重试'),
    complete: () => { avatarUploading.value = false; },
  });
};

const onChooseAvatar = async (e) => {
  const tempPath = e.detail && e.detail.avatarUrl;
  if (!tempPath || avatarUploading.value) return;
  avatarBroken.value = false;
  avatarPreview.value = tempPath; // 本地秒换预览
  avatarUploading.value = true;
  const m = tempPath.match(/\.(\w+)(\?|$)/);
  const ext = m ? m[1] : 'png';
  try {
    const stable = await copyToStable(tempPath, ext);
    uploadAvatar(stable);
  } catch (err) {
    console.warn('avatar copy failed, upload temp file directly', err);
    uploadAvatar(tempPath);
  }
};

const onChooseAvatarError = (e) => {
  const msg = (e && e.detail && e.detail.errMsg) || '';
  if (msg.includes('cancel')) return; // 用户主动取消：静默处理，不弹错
  uni.showToast({ title: '未获取到微信头像（模拟器为已知问题，请真机重试）', icon: 'none' });
};

/* ---------- 奖励资产（契约：GET /user/reward/summary，外层 ApiResult，前端 request.js 按 success/status 判定） ---------- */
const reward = ref(null);
const rewardLoading = ref(false);
const rewardError = ref(false);

const rewardCols = computed(() => [
  { label: '累计奖励', value: reward.value ? reward.value.total : null },
  { label: '待结算', value: reward.value ? reward.value.pending : null },
  { label: '已到账', value: reward.value ? reward.value.settled : null },
]);
const displayValue = (col) => {
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

/* ---------- 完善资料抽屉（改昵称 + 换头像双入口） ---------- */
const profileVisible = ref(false);
const nicknameDraft = ref('');
const nicknameSaving = ref(false);

const openProfile = () => {
  if (!isLoggedIn.value) { goLogin(); return; }
  const cur = displayUser.value?.nickname || '';
  nicknameDraft.value = cur === '微信用户' ? '' : cur;
  profileVisible.value = true;
};
const closeProfile = () => {
  if (nicknameSaving.value) return;
  profileVisible.value = false;
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
  if (name === (displayUser.value?.nickname || '')) { profileVisible.value = false; return; } // 无改动不发请求
  nicknameSaving.value = true;
  try {
    await editUser({ nickname: name });
    await userStore.refreshUserInfo();
    profileVisible.value = false;
    uni.showToast({ title: '已保存', icon: 'success' });
  } catch (e) {
    uni.showToast({ title: '保存失败，请重试', icon: 'none' }); // 失败保持抽屉打开
  } finally {
    nicknameSaving.value = false;
  }
};

/* ---------- 导航 ---------- */
const goLogin = () => uni.navigateTo({ url: '/pages/login/index' });
const goPage = (url) => {
  if (!isLoggedIn.value) { goLogin(); return; }
  uni.navigateTo({ url });
};
const onAssetClick = () => {
  if (!isLoggedIn.value) { goLogin(); return; }
  if (rewardError.value && !rewardLoading.value) { loadReward(); return; }
  goPage('/pages/user/balance');
};
const onBillClick = () => {
  if (!isLoggedIn.value) { goLogin(); return; }
  if (rewardError.value || rewardLoading.value) { loadReward(); return; }
  uni.navigateTo({ url: '/pages/user/bill' });
};

onShow(() => {
  if (!isLoggedIn.value) {
    reward.value = null;
    avatarPreview.value = '';
    return;
  }
  loadReward();
});
</script>

<style scoped>
.page-user {
  --primary: #FF6B35;
  --hero-gradient: linear-gradient(160deg, #FF6B35 0%, #FF9A62 100%);
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
.avatar-btn, .drawer-avatar-btn {
  padding: 0; margin: 0; line-height: 1; background: transparent; border: none;
  width: 128rpx; height: 128rpx; border-radius: 50%;
  position: relative; overflow: hidden;
}
.avatar-btn::after, .drawer-avatar-btn::after { border: none; }
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

/* ---------- 资产卡 ---------- */
.asset-card {
  margin: -56rpx 24rpx 0;
  background: var(--card-bg);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-float);
  position: relative; z-index: 1;
  overflow: hidden;
}
.asset-card-hover { background: #FFFAF7; }
.asset-row { display: flex; align-items: center; padding: 40rpx 0 36rpx; }
.asset-col { flex: 1; display: flex; flex-direction: column; align-items: center; position: relative; }
.asset-col + .asset-col::before {
  content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%);
  width: 1rpx; height: 56rpx; background: var(--divider);
}
.asset-value-wrap { display: flex; align-items: baseline; }
.asset-symbol { font-size: 26rpx; color: var(--primary); font-weight: bold; margin-right: 2rpx; }
.asset-value { font-size: 42rpx; color: var(--primary); font-weight: bold; }
.asset-label { font-size: 24rpx; color: var(--text-sub); margin-top: 10rpx; }

.skel {
  background: linear-gradient(90deg, #F0F1F3 25%, #F7F8FA 37%, #F0F1F3 63%);
  background-size: 400% 100%;
  animation: shimmer 1.2s ease infinite;
  border-radius: 8rpx;
}
@keyframes shimmer { 0% { background-position: 100% 0; } 100% { background-position: 0 0; } }
.skel-value { width: 120rpx; height: 40rpx; }
.skel-label { width: 72rpx; height: 22rpx; margin-top: 14rpx; }

.asset-foot {
  height: 72rpx; display: flex; align-items: center; justify-content: center;
  border-top: 1rpx solid var(--divider);
}
.asset-foot-text { font-size: 26rpx; color: var(--text-sub); margin-right: 8rpx; }

/* ---------- 功能分组 ---------- */
.group-title {
  display: block; font-size: 28rpx; color: var(--text-sub);
  margin: 32rpx 32rpx 0;
}
.menu-card {
  margin: 24rpx 24rpx 0;
  background: var(--card-bg);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.menu-item {
  position: relative;
  display: flex; align-items: center;
  height: 108rpx; padding: 0 32rpx;
  box-sizing: border-box;
  transition: background-color 0.15s;
}
.menu-item::before {
  content: ''; position: absolute; left: 120rpx; right: 0; top: 0;
  height: 1rpx; background: var(--divider);
}
.menu-item:first-child::before { display: none; }
.menu-hover { background: #FAFBFC; }
.menu-icon {
  width: 64rpx; height: 64rpx; border-radius: 18rpx; margin-right: 24rpx; flex-shrink: 0;
  background-repeat: no-repeat; background-position: center; background-size: 36rpx;
}
.icon-order { background-color: #FFF1EC; }
.icon-balance { background-color: #FFF7E6; }
.icon-bill { background-color: #EEF7FF; }
.icon-spread { background-color: #ECFFF5; }
.icon-settings { background-color: #F3F4F6; }
.menu-text { flex: 1; font-size: 30rpx; color: var(--text-main); }
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

/* ---------- 完善资料抽屉 ---------- */
.drawer-mask {
  position: fixed; left: 0; right: 0; top: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.4); z-index: 100;
  display: flex; align-items: flex-end;
}
.drawer {
  width: 100%;
  background: var(--card-bg);
  border-radius: 32rpx 32rpx 0 0;
  padding: 40rpx 32rpx calc(32rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  animation: drawer-up 0.25s ease;
}
@keyframes drawer-up { from { transform: translateY(100%); } to { transform: translateY(0); } }
.drawer-head { display: flex; align-items: center; justify-content: space-between; }
.drawer-title { font-size: 34rpx; font-weight: bold; color: var(--text-main); }
.drawer-close { padding: 8rpx 12rpx; }
.drawer-close-hover { opacity: 0.6; }
.drawer-close-icon { font-size: 40rpx; color: var(--text-weak); line-height: 1; }

.drawer-avatar-row { display: flex; align-items: center; height: 140rpx; margin-top: 16rpx; }
.drawer-avatar-btn { width: 96rpx; height: 96rpx; }
.drawer-avatar { width: 96rpx; height: 96rpx; border-radius: 50%; background: #F3F4F6; }
.drawer-avatar-tip { font-size: 26rpx; color: var(--text-sub); margin-left: 24rpx; }

.drawer-nick-row {
  display: flex; align-items: center; height: 100rpx;
  border-bottom: 1rpx solid var(--divider);
}
.drawer-nick-label { font-size: 28rpx; color: var(--text-main); flex-shrink: 0; margin-right: 24rpx; }
.drawer-nick-input { flex: 1; text-align: right; font-size: 28rpx; color: var(--text-main); }
.drawer-nick-placeholder { color: var(--text-weak); }

.drawer-save {
  margin-top: 48rpx; height: 88rpx; border-radius: 44rpx;
  background: linear-gradient(135deg, #FF6B35, #FF8F65);
  display: flex; align-items: center; justify-content: center;
}
.drawer-save-text { font-size: 32rpx; font-weight: bold; color: #fff; }
.drawer-save-disabled { background: #FFC9B3; }
.drawer-save-loading { opacity: 0.8; }
</style>
