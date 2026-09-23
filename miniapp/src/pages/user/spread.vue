<template>
  <view class="page-spread">
    <view class="spread-hero">
      <view class="hero-deco hero-deco-1"></view>
      <view class="hero-deco hero-deco-2"></view>
      <text class="hero-title">{{ COPYWRITING.SHARE_POSTER_TITLE }}</text>
      <text class="hero-sub">好物分享 一起省钱</text>
    </view>

    <!-- A 型等分文字 tab：推广 / 好友 / 邀请 -->
    <view class="seg-tabs">
      <view
        v-for="t in segTabs"
        :key="t.value"
        class="seg-tab"
        :class="{ active: activeTab === t.value }"
        @click="switchTab(t.value)"
      >
        <text class="seg-tab-text">{{ t.label }}</text>
        <view class="seg-tab-indicator" v-if="activeTab === t.value"></view>
      </view>
    </view>

    <!-- 推广 Tab -->
    <block v-if="activeTab === 'spread'">
      <view class="stats-card">
        <template v-if="statsError">
          <view class="stats-error" @click="loadStats">
            <text class="stats-error-text">加载失败，点击重试</text>
          </view>
        </template>
        <template v-else-if="statsLoading && !stats">
          <view class="stat-item" v-for="i in 3" :key="i">
            <view class="skel skel-value"></view>
            <view class="skel skel-label"></view>
          </view>
        </template>
        <template v-else>
          <view class="stat-item">
            <text class="stat-value">{{ stats.peopleCount || 0 }}</text>
            <text class="stat-label">推广人数</text>
          </view>
          <view class="stat-divider"></view>
          <view class="stat-item">
            <text class="stat-value">{{ stats.orderCount || 0 }}</text>
            <text class="stat-label">推广订单</text>
          </view>
          <view class="stat-divider"></view>
          <view class="stat-item">
            <text class="stat-value">¥{{ formatAmount(stats.commission) }}</text>
            <text class="stat-label">{{ COPYWRITING.SPREAD_REWARD_LABEL }}</text>
          </view>
        </template>
      </view>

      <view class="step-card">
        <view class="step-item">
          <view class="step-num"><text>1</text></view>
          <text class="step-text">分享小程序</text>
        </view>
        <view class="step-line"></view>
        <view class="step-item">
          <view class="step-num"><text>2</text></view>
          <text class="step-text">好友下单</text>
        </view>
        <view class="step-line"></view>
        <view class="step-item">
          <view class="step-num"><text>3</text></view>
          <text class="step-text">获得{{ COPYWRITING.SPREAD_REWARD_LABEL }}</text>
        </view>
      </view>

      <view class="section-card">
        <view class="section-title">
          <view class="title-bar"></view>
          <text class="title-text">推广用户</text>
        </view>
        <FailRetry v-if="peopleError" @retry="loadPeople(true)" />
        <block v-else>
          <PeopleRow
            v-for="(p, idx) in peopleList"
            :key="p.uid || idx"
            :user="p"
            :rank="idx + 1"
          />
          <Loading :visible="peopleLoading && !peopleList.length" />
          <Empty v-if="!peopleLoading && !peopleList.length" text="暂无推广用户" />
        </block>
      </view>
    </block>

    <!-- 好友 Tab -->
    <block v-if="activeTab === 'friend'">
      <view class="section-card">
        <view class="section-title">
          <view class="title-bar"></view>
          <text class="title-text">我的好友</text>
        </view>
        <FailRetry v-if="friendError" @retry="loadFriends(true)" />
        <block v-else>
          <PeopleRow
            v-for="(p, idx) in friendList"
            :key="p.uid || idx"
            :user="p"
            :rank="idx + 1"
            :label="COPYWRITING.SPREAD_REWARD_LABEL"
          />
          <Loading :visible="friendLoading && !friendList.length" text="加载中..." />
          <Empty
            v-if="!friendLoading && !friendList.length"
            text="还没有好友加入"
            action-text="去邀请"
            @action="switchTab('invite')"
          />
          <view class="list-end" v-if="!friendHasMore && friendList.length">
            <view class="list-end-line"></view>
            <text class="list-end-text">已经到底了</text>
            <view class="list-end-line"></view>
          </view>
        </block>
      </view>
    </block>

    <!-- 邀请 Tab -->
    <block v-if="activeTab === 'invite'">
      <view class="invite-card">
        <view class="qrcode-box" @click="retryCode">
          <image v-if="inviteQr && !codeError" :src="inviteQr" class="qrcode-image" mode="aspectFit" show-menu-by-longpress />
          <view v-else class="qrcode-placeholder">
            <view class="ph-icon" :style="{ backgroundImage: imageIcon }"></view>
            <text class="ph-text">{{ codeError ? '二维码加载失败，点击刷新' : '加载中...' }}</text>
          </view>
        </view>
        <text class="qrcode-tip">长按保存或分享给好友</text>
        <view class="spread-actions">
          <button class="btn-action btn-poster" hover-class="btn-hover" @click="goPoster">保存海报</button>
          <button class="btn-action btn-copy" hover-class="btn-hover" @click="copyLink">复制邀请链接</button>
        </view>
      </view>
      <view class="disclaimer">{{ COPYWRITING.REWARD_DISCLAIMER }}</view>
    </block>
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onShow, onReachBottom } from '@dcloudio/uni-app';
import Loading from '../../components/Loading.vue';
import Empty from '../../components/Empty.vue';
import FailRetry from '../../components/FailRetry.vue';
import PeopleRow from '../../components/PeopleRow.vue';
import { getSpreadCode, getSpreadPeople, getSpreadSummary } from '../../api/share';
import { formatAmount } from '../../utils/format';
import { COPYWRITING } from '../../utils/constants';
import { ICONS } from '../../utils/icons';
import { useAppStore } from '../../store/app';

const appStore = useAppStore();
const imageIcon = ICONS.image('#B6BAC2');

const segTabs = [
  { value: 'spread', label: '推广' },
  { value: 'friend', label: '好友' },
  { value: 'invite', label: '邀请' },
];
const activeTab = ref('spread');

/* ---------- 推广统计（GET /spread/summary，契约由 JAVA 广播：peopleCount/orderCount/commission） ---------- */
const stats = ref(null);
const statsLoading = ref(false);
const statsError = ref(false);

const loadStats = async () => {
  statsLoading.value = true;
  try {
    const res = await getSpreadSummary({}, { silent: true });
    const d = res.result || res.data || {};
    stats.value = {
      peopleCount: d.peopleCount || 0,
      orderCount: d.orderCount || 0,
      commission: d.commission != null ? d.commission : 0,
    };
    statsError.value = false;
  } catch (e) {
    statsError.value = true; // 失败重试，禁止 mock 回落（PRD §0-2）
  } finally {
    statsLoading.value = false;
  }
};

/* ---------- 推广用户 / 好友列表（POST /spread/people 分页） ---------- */
const PEOPLE_PAGE = 10;

const peopleList = ref([]);
const peopleLoading = ref(false);
const peopleError = ref(false);

const loadPeople = async (reset = false) => {
  if (peopleLoading.value) return;
  if (reset) peopleList.value = [];
  peopleLoading.value = true;
  try {
    const res = await getSpreadPeople({ page: 1, limit: PEOPLE_PAGE });
    const d = res.result || res.data || {};
    peopleList.value = d.list || (Array.isArray(d) ? d : []);
    peopleError.value = false;
  } catch (e) {
    peopleError.value = true;
  } finally {
    peopleLoading.value = false;
  }
};

const friendList = ref([]);
const friendPage = ref(1);
const friendHasMore = ref(true);
const friendLoading = ref(false);
const friendError = ref(false);

const loadFriends = async (reset = false) => {
  if (friendLoading.value || (!reset && !friendHasMore.value)) return;
  if (reset) { friendPage.value = 1; friendHasMore.value = true; friendList.value = []; }
  friendLoading.value = true;
  try {
    const res = await getSpreadPeople({ page: friendPage.value, limit: PEOPLE_PAGE });
    const d = res.result || res.data || {};
    const list = d.list || (Array.isArray(d) ? d : []);
    if (list.length < PEOPLE_PAGE) friendHasMore.value = false;
    friendList.value = reset ? list : [...friendList.value, ...list];
    friendPage.value++;
    friendError.value = false;
  } catch (e) {
    friendError.value = true;
  } finally {
    friendLoading.value = false;
  }
};

onReachBottom(() => {
  if (activeTab.value === 'friend') loadFriends();
});

/* ---------- 邀请二维码（JAVA seq-177：data {code, scene, page, qrBase64|null}；qrBase64=null 即上游生成失败→点击刷新，不回落 mock） ---------- */
const inviteQr = ref('');
const inviteCodeText = ref('');
const codeError = ref(false);

const loadCode = async () => {
  codeError.value = false;
  try {
    const res = await getSpreadCode({ silent: true });
    const d = res.result || res.data || {};
    inviteCodeText.value = d.code || '';
    if (d.qrBase64) {
      inviteQr.value = String(d.qrBase64).startsWith('data:') ? d.qrBase64 : `data:image/png;base64,${d.qrBase64}`;
    } else {
      inviteQr.value = '';
      codeError.value = true;
    }
  } catch (e) {
    inviteQr.value = '';
    codeError.value = true;
  }
};

const retryCode = () => {
  if (codeError.value) loadCode();
};

const switchTab = (v) => {
  if (activeTab.value === v) return;
  activeTab.value = v;
  if (v === 'friend' && !friendList.value.length && !friendError.value) loadFriends(true);
};

const goPoster = () => {
  uni.navigateTo({ url: '/pages/user/poster' });
};

const copyLink = () => {
  const code = inviteCodeText.value || '';
  if (!code) { uni.showToast({ title: '暂无邀请码', icon: 'none' }); return; }
  uni.setClipboardData({
    data: code,
    success: () => uni.showToast({ title: '邀请码已复制', icon: 'success' }),
  });
};

onShow(() => {
  // 入口（个人中心双入口卡）经全局状态定位 tab，消费即清空
  const intent = appStore.consumeSpreadTabIntent();
  if (intent === 'friend' || intent === 'invite') activeTab.value = intent;

  loadStats();
  loadPeople(true);
  loadCode();
  friendList.value = [];
  loadFriends(true);
});
</script>

<style scoped>
.page-spread { min-height: 100vh; background: #F6F7F9; padding-bottom: calc(40rpx + env(safe-area-inset-bottom)); }

/* Hero：v1.1 规格装饰圆 */
.spread-hero {
  position: relative; overflow: hidden;
  background: linear-gradient(160deg, #FF6B35 0%, #FF9A62 100%);
  padding: 48rpx 32rpx 56rpx;
  display: flex; flex-direction: column; align-items: center;
}
.hero-deco { position: absolute; border-radius: 50%; }
.hero-deco-1 { width: 320rpx; height: 320rpx; right: -80rpx; top: -120rpx; background: rgba(255, 255, 255, 0.10); }
.hero-deco-2 { width: 160rpx; height: 160rpx; left: -50rpx; bottom: -60rpx; background: rgba(255, 255, 255, 0.06); }
.hero-title { font-size: 40rpx; font-weight: bold; color: #fff; position: relative; }
.hero-sub { font-size: 24rpx; color: rgba(255, 255, 255, 0.85); margin-top: 10rpx; position: relative; }

/* A 型等分文字 tab（子页面规范 §2） */
.seg-tabs { display: flex; background: #fff; margin: -28rpx 24rpx 0; border-radius: 24rpx; box-shadow: 0 8rpx 32rpx rgba(31, 33, 38, 0.06); position: relative; z-index: 2; overflow: hidden; }
.seg-tab { flex: 1; display: flex; flex-direction: column; align-items: center; height: 88rpx; justify-content: center; position: relative; }
.seg-tab-text { font-size: 28rpx; color: #7A7F89; transition: all 0.25s ease; }
.seg-tab.active .seg-tab-text { color: #FF6B35; font-weight: 600; }
.seg-tab-indicator { position: absolute; bottom: 8rpx; width: 48rpx; height: 6rpx; border-radius: 3rpx; background: linear-gradient(90deg, #FF6B35, #FF8F65); }

/* 统计卡 */
.stats-card { display: flex; align-items: center; background: #fff; margin: 24rpx 24rpx 0; padding: 36rpx 0; border-radius: 24rpx; box-shadow: 0 8rpx 32rpx rgba(31, 33, 38, 0.06); }
.stat-item { flex: 1; text-align: center; display: flex; flex-direction: column; align-items: center; }
.stat-value { font-size: 36rpx; color: #FF6B35; font-weight: bold; }
.stat-label { font-size: 24rpx; color: #7A7F89; margin-top: 10rpx; }
.stat-divider { width: 1rpx; height: 56rpx; background: #F2F3F5; }
.stats-error { width: 100%; text-align: center; padding: 20rpx 0; }
.stats-error-text { font-size: 26rpx; color: #7A7F89; }

.skel { background: linear-gradient(90deg, #F0F1F3 25%, #F7F8FA 37%, #F0F1F3 63%); background-size: 400% 100%; animation: shimmer 1.2s ease infinite; border-radius: 8rpx; }
@keyframes shimmer { 0% { background-position: 100% 0; } 100% { background-position: 0 0; } }
.skel-value { width: 120rpx; height: 40rpx; }
.skel-label { width: 72rpx; height: 22rpx; margin-top: 14rpx; }

/* 步骤卡 */
.step-card { display: flex; align-items: center; background: #fff; margin: 24rpx 24rpx 0; padding: 28rpx 20rpx; border-radius: 24rpx; box-shadow: 0 8rpx 32rpx rgba(31, 33, 38, 0.06); }
.step-item { flex: 1; display: flex; flex-direction: column; align-items: center; }
.step-num { width: 52rpx; height: 52rpx; border-radius: 50%; background: linear-gradient(135deg, #FF6B35 0%, #FF8F65 100%); display: flex; align-items: center; justify-content: center; box-shadow: 0 4rpx 12rpx rgba(255, 107, 53, 0.3); }
.step-num text { font-size: 26rpx; color: #fff; font-weight: bold; }
.step-text { font-size: 22rpx; color: #7A7F89; margin-top: 12rpx; text-align: center; }
.step-line { width: 48rpx; height: 2rpx; background: linear-gradient(90deg, #FFD9C7, #FF8F65); margin-bottom: 30rpx; }

/* 列表卡 */
.section-card { margin: 24rpx 24rpx 0; background: #fff; border-radius: 24rpx; padding: 28rpx 24rpx; box-shadow: 0 8rpx 32rpx rgba(31, 33, 38, 0.06); }
.section-title { display: flex; align-items: center; margin-bottom: 12rpx; }
.title-bar { width: 8rpx; height: 28rpx; border-radius: 4rpx; background: linear-gradient(180deg, #FF6B35, #FF8F65); margin-right: 12rpx; }
.title-text { font-size: 30rpx; font-weight: bold; color: #1F2126; }
.list-end { display: flex; align-items: center; justify-content: center; padding: 24rpx 0 8rpx; }
.list-end-line { width: 80rpx; height: 1rpx; background: #E5E6EB; }
.list-end-text { font-size: 22rpx; color: #B6BAC2; margin: 0 20rpx; }

/* 邀请卡 */
.invite-card { background: #fff; margin: 24rpx 24rpx 0; border-radius: 24rpx; padding: 40rpx 32rpx 32rpx; display: flex; flex-direction: column; align-items: center; box-shadow: 0 8rpx 32rpx rgba(31, 33, 38, 0.06); }
.qrcode-box { width: 320rpx; height: 320rpx; border-radius: 20rpx; overflow: hidden; border: 2rpx solid #F2F3F5; }
.qrcode-image { width: 100%; height: 100%; }
.qrcode-placeholder { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; background: #FFF1EC; gap: 16rpx; }
.ph-icon { width: 56rpx; height: 56rpx; background-repeat: no-repeat; background-position: center; background-size: 44rpx; opacity: 0.6; }
.ph-text { font-size: 24rpx; color: #7A7F89; }
.qrcode-tip { font-size: 24rpx; color: #7A7F89; margin-top: 20rpx; }
.spread-actions { display: flex; gap: 24rpx; margin-top: 28rpx; width: 100%; }
.btn-action { flex: 1; height: 80rpx; line-height: 80rpx; font-size: 28rpx; border-radius: 40rpx; border: none; padding: 0; }
.btn-hover { opacity: 0.85; transform: scale(0.98); }
.btn-poster { color: #fff; background: linear-gradient(135deg, #FF6B35 0%, #FF8F65 100%); box-shadow: 0 6rpx 16rpx rgba(255, 107, 53, 0.35); }
.btn-copy { color: #FF6B35; background: #FFF7F0; border: 2rpx solid #FFD9C7; }

.disclaimer { text-align: center; margin-top: 32rpx; font-size: 22rpx; color: #B6BAC2; }
</style>
