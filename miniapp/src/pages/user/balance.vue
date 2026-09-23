<template>
  <view class="page-balance">
    <!-- v2.1 深色资产卡（与个人中心资产卡同源） -->
    <view class="ink-card">
      <view class="ink-top">
        <view class="ink-balance">
          <text class="ink-label">可用余额(元)</text>
          <text class="ink-amount">{{ availableText }}</text>
        </view>
        <view class="ink-withdraw" :class="{ 'ink-withdraw-disabled': !withdrawable }" hover-class="ink-withdraw-hover" @click="onWithdraw">
          <text class="ink-withdraw-text">去提现 ›</text>
        </view>
      </view>
      <view class="ink-row">
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
    </view>

    <text class="balance-tip">{{ COPYWRITING.REWARD_DISCLAIMER }}</text>

    <!-- 近期到账（行型 1） -->
    <view class="recent-section">
      <view class="section-title-bar">
        <view class="title-deco"></view>
        <text class="section-title">近期到账</text>
        <text class="section-more" @click="goBill">全部账单 ›</text>
      </view>
      <view class="income-card">
        <FailRetry v-if="recentError" @retry="loadRecent" />
        <block v-else>
          <view class="income-item" v-for="item in recentList" :key="item.id">
            <view class="income-icon" :style="{ backgroundImage: incomeIcon }"></view>
            <view class="income-meta">
              <text class="income-title">{{ labelOf(item) }}</text>
              <text class="income-time">{{ item.createTime }}</text>
            </view>
            <text class="income-amount">+¥{{ formatAmount(item.number) }}</text>
          </view>
          <Loading :visible="recentLoading && !recentList.length" />
          <Empty
            v-if="!recentLoading && !recentList.length && !recentError"
            text="还没有到账记录"
            action-text="去推广"
            @action="goSpread"
          />
        </block>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import Loading from '../../components/Loading.vue';
import Empty from '../../components/Empty.vue';
import FailRetry from '../../components/FailRetry.vue';
import { getIntegralList } from '../../api/user';
import { get } from '../../utils/request';
import { formatMoney, formatAmount } from '../../utils/format';
import { COPYWRITING, BILL_TYPE_LABELS, BILL_DEFAULT_LABEL } from '../../utils/constants';
import { ICONS } from '../../utils/icons';
import { useUserStore } from '../../store/user';

const userStore = useUserStore();

const incomeIcon = ICONS.coin('#FF6B35');

/* ---------- 可用余额 + J1 提现阈值判定（与个人中心同口径） ---------- */
const nowMoney = computed(() => {
  const v = Number(userStore.userInfo?.nowMoney);
  return Number.isFinite(v) ? v : 0;
});
const availableText = computed(() => formatMoney(nowMoney.value));

const minPrice = ref(null);
const loadExtractMin = async () => {
  try {
    const res = await get('/extract/bank', {}, { silent: true });
    const d = res.result || res.data || {};
    const n = parseFloat(d.minPrice);
    minPrice.value = Number.isNaN(n) ? null : n;
  } catch (e) { /* 降级：仅按余额>0 判定 */ }
};
const withdrawable = computed(() => {
  if (nowMoney.value <= 0) return false;
  if (minPrice.value != null && nowMoney.value < minPrice.value) return false;
  return true;
});
const onWithdraw = () => {
  if (nowMoney.value <= 0) { uni.showToast({ title: '暂无可提现余额', icon: 'none' }); return; }
  if (minPrice.value != null && nowMoney.value < minPrice.value) {
    uni.showToast({ title: `满 ${minPrice.value} 元可提现`, icon: 'none' });
    return;
  }
  // 提现弹层未列本期 UI 规格：达标点击按 PRD §4 降级为提示，整体列 P2（管理排期定）
  uni.showModal({
    title: '余额提现',
    content: '提现功能正在开发中，敬请期待。',
    showCancel: false,
    confirmText: '知道了',
  });
};

/* ---------- 三值（与个人中心资产卡同源：GET /user/reward/summary） ---------- */
const reward = ref(null);
const rewardLoading = ref(false);
const rewardError = ref(false);

const rewardCols = computed(() => [
  { label: '待结算', value: reward.value ? reward.value.pending : null },
  { label: '已到账', value: reward.value ? reward.value.settled : null },
  { label: '累计奖励', value: reward.value ? reward.value.total : null },
]);
const displayValue = (col) => {
  if (rewardError.value) return formatMoney(0);
  if (col.value === null || col.value === undefined) return '--.--';
  return formatMoney(col.value);
};

const loadReward = async () => {
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

/* ---------- 近期到账：真实流水（/integral/list 前 10 条收入类） ---------- */
const recentList = ref([]);
const recentLoading = ref(false);
const recentError = ref(false);

const labelOf = (item) => (item.title ? item.title : BILL_TYPE_LABELS[item.type] || BILL_DEFAULT_LABEL);

const loadRecent = async () => {
  recentLoading.value = true;
  try {
    const res = await getIntegralList({ page: 1, limit: 10, category: 'now_money' }, { silent: true });
    const d = res?.result ?? res?.data ?? [];
    const rows = Array.isArray(d) ? d : (d.list || d.content || []);
    recentList.value = rows.filter((r) => Number(r.pm) === 1);
    recentError.value = false;
  } catch (e) {
    recentError.value = true; // 禁止 mock 回落
  } finally {
    recentLoading.value = false;
  }
};

const goBill = () => uni.navigateTo({ url: '/pages/user/bill' });
const goSpread = () => uni.switchTab({ url: '/pages/user/spread' });

onShow(() => {
  loadReward();
  loadExtractMin();
  loadRecent();
});
</script>

<style scoped>
.page-balance { min-height: 100vh; background: #F6F7F9; padding: 24rpx 24rpx calc(40rpx + env(safe-area-inset-bottom)); box-sizing: border-box; }

/* 深色资产卡（v2.1 同源） */
.ink-card {
  background: linear-gradient(150deg, #2B211E 0%, #1F1B1A 100%);
  border: 1rpx solid rgba(255, 107, 53, 0.35);
  border-radius: 24rpx;
  padding: 36rpx 32rpx 30rpx;
  box-shadow: 0 12rpx 40rpx rgba(31, 33, 38, 0.25);
}
.ink-top { display: flex; align-items: flex-end; justify-content: space-between; }
.ink-balance { display: flex; flex-direction: column; }
.ink-label { font-size: 26rpx; color: rgba(255, 255, 255, 0.65); }
.ink-amount { font-size: 64rpx; color: #FFD9C2; font-weight: bold; margin-top: 8rpx; line-height: 1.1; }
.ink-withdraw {
  background: linear-gradient(135deg, #FF6B35 0%, #FF8F65 100%);
  border-radius: 999rpx; padding: 14rpx 32rpx; flex-shrink: 0;
}
.ink-withdraw-hover { opacity: 0.85; }
.ink-withdraw-disabled { background: rgba(255, 255, 255, 0.25); }
.ink-withdraw-text { font-size: 26rpx; color: #fff; font-weight: 600; }
.ink-row { display: flex; align-items: center; margin-top: 32rpx; padding-top: 28rpx; border-top: 1rpx solid rgba(255, 255, 255, 0.08); }
.ink-col { flex: 1; display: flex; flex-direction: column; align-items: center; position: relative; }
.ink-col + .ink-col::before { content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%); width: 1rpx; height: 52rpx; background: rgba(255, 255, 255, 0.1); }
.ink-col-value { font-size: 32rpx; color: rgba(255, 255, 255, 0.92); font-weight: bold; }
.ink-col-label { font-size: 22rpx; color: rgba(255, 255, 255, 0.6); margin-top: 8rpx; }
.skel-dark { background: rgba(255, 255, 255, 0.12); border-radius: 8rpx; }
.skel-value { width: 110rpx; height: 32rpx; }
.skel-label { width: 72rpx; height: 20rpx; margin-top: 12rpx; }

.balance-tip { display: block; text-align: center; font-size: 20rpx; color: #B6BAC2; margin-top: 20rpx; }

.recent-section { margin-top: 32rpx; }
.section-title-bar { display: flex; align-items: center; margin-bottom: 20rpx; }
.title-deco { width: 8rpx; height: 28rpx; border-radius: 4rpx; background: linear-gradient(180deg, #FF6B35, #FF8F65); margin-right: 12rpx; }
.section-title { flex: 1; font-size: 30rpx; font-weight: 600; color: #1F2126; }
.section-more { font-size: 24rpx; color: #7A7F89; }

.income-card { background: #fff; border-radius: 24rpx; overflow: hidden; box-shadow: 0 8rpx 32rpx rgba(31, 33, 38, 0.06); }
.income-item { display: flex; align-items: center; min-height: 108rpx; padding: 26rpx 28rpx; border-bottom: 1rpx solid #F2F3F5; box-sizing: border-box; }
.income-item:last-child { border-bottom: none; }
.income-icon { width: 64rpx; height: 64rpx; border-radius: 18rpx; background-color: #FFF1EC; background-repeat: no-repeat; background-position: center; background-size: 36rpx; margin-right: 20rpx; flex-shrink: 0; }
.income-meta { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.income-title { font-size: 30rpx; color: #1F2126; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.income-time { font-size: 22rpx; color: #B6BAC2; margin-top: 8rpx; }
.income-amount { font-size: 32rpx; color: #FF6B35; font-weight: bold; flex-shrink: 0; }
</style>
