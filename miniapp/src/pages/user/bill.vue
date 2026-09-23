<template>
  <view class="page-bill">
    <!-- A 型筛选 tab（服务端 type 过滤） -->
    <view class="filter-tabs">
      <view
        v-for="t in tabs"
        :key="t.id"
        class="filter-tab"
        :class="{ active: activeTabId === t.id }"
        @click="switchTab(t)"
      >
        <text class="filter-tab-text">{{ t.label }}</text>
        <view class="filter-tab-indicator" v-if="activeTabId === t.id"></view>
      </view>
    </view>

    <FailRetry v-if="loadError && !groups.length" @retry="loadBills(true)" />

    <block v-else>
      <view class="bill-month" v-for="g in groups" :key="g.month">
        <text class="month-text">{{ g.month }}</text>
        <view class="bill-card">
          <view class="bill-item" v-for="item in g.items" :key="item.id">
            <view class="bill-icon" :class="item._pm === 1 ? 'icon-in' : 'icon-out'" :style="{ backgroundImage: item._icon }"></view>
            <view class="bill-left">
              <text class="bill-title">{{ item._label }}</text>
              <text class="bill-time">{{ item.createTime }}</text>
            </view>
            <text class="bill-amount" :class="item._pm === 1 ? 'positive' : 'negative'">
              {{ item._pm === 1 ? '+' : '-' }}¥{{ formatAmount(item.number) }}
            </text>
          </view>
        </view>
      </view>

      <!-- 首载/tab 切换骨架（子页面规范 §5） -->
      <view class="bill-card skel-card" v-if="loading && !list.length">
        <view class="bill-item" v-for="i in 4" :key="i">
          <view class="skel skel-icon"></view>
          <view class="bill-left">
            <view class="skel skel-title"></view>
            <view class="skel skel-time"></view>
          </view>
          <view class="skel skel-amount"></view>
        </view>
      </view>

      <Loading :visible="loading && list.length" text="加载中..." />
      <Empty
        v-if="!loading && !groups.length"
        text="这个月还没有账单"
        action-text="去首页逛逛"
        @action="goHome"
      />
      <view class="bill-end" v-if="!hasMore && list.length">
        <view class="end-line"></view>
        <text class="end-text">已经到底了</text>
        <view class="end-line"></view>
      </view>
    </block>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import Loading from '../../components/Loading.vue';
import Empty from '../../components/Empty.vue';
import FailRetry from '../../components/FailRetry.vue';
import { getIntegralList } from '../../api/user';
import { formatAmount } from '../../utils/format';
import { BILL_TYPE_LABELS, BILL_DEFAULT_LABEL, BILL_FILTER_TABS } from '../../utils/constants';
import { useAppStore } from '../../store/app';

const appStore = useAppStore();
import { ICONS } from '../../utils/icons';

const tabs = BILL_FILTER_TABS;
const PAGE = 20;

const activeTabId = ref('');
const list = ref([]);
const page = ref(1);
const hasMore = ref(true);
const loading = ref(false);
const loadError = ref(false);

/* 业务名兜底（PRD §0-3）：title 非空优先；否则按 type 枚举映射，未知→其他 */
const labelOf = (item) => {
  if (item.title) return item.title;
  return BILL_TYPE_LABELS[item.type] || BILL_DEFAULT_LABEL;
};

const TYPE_ICONS = {
  retail: ICONS.cart('#FF6B35'),
  brokerage: ICONS.gift('#FF6B35'),
  extract: ICONS.cash('#7A7F89'),
  recharge: ICONS.wallet('#FF6B35'),
  pay_product: ICONS.order('#7A7F89'),
  pay_product_refund: ICONS.refresh('#FF6B35'),
};
const DEFAULT_IN_ICON = ICONS.coin('#FF6B35');
const DEFAULT_OUT_ICON = ICONS.doc('#7A7F89');

// /integral/list 行 → 视图模型（pm: 1 收入 / 0 支出；金额字段为 number）
const decorate = (rows) =>
  rows.map((item) => {
    const pm = Number(item.pm) === 1 ? 1 : 0;
    const label = labelOf(item);
    return {
      ...item,
      _pm: pm,
      _label: label,
      _icon: TYPE_ICONS[item.type] || (pm === 1 ? DEFAULT_IN_ICON : DEFAULT_OUT_ICON),
    };
  });

// 月份分组头（子页面规范 §6）：按 createTime「YYYY年M月」分段，保持后端时序
const groups = computed(() => {
  const out = [];
  let cur = null;
  list.value.forEach((item) => {
    const t = (item.createTime || '').slice(0, 7);
    const month = t ? `${Number(t.slice(0, 4))}年${Number(t.slice(5, 7))}月` : '更早';
    if (!cur || cur.month !== month) {
      cur = { month, items: [] };
      out.push(cur);
    }
    cur.items.push(item);
  });
  return out;
});

const loadBills = async (reset = false) => {
  if (loading.value || (!reset && !hasMore.value)) return;
  if (reset) { page.value = 1; hasMore.value = true; list.value = []; loadError.value = false; }
  loading.value = true;
  try {
    // 服务端分页+筛选（PRD §2）；category=now_money 为金额流水
    const params = { page: page.value, limit: PAGE, category: 'now_money' };
    const tab = tabs.find((t) => t.id === activeTabId.value);
    if (tab && tab.query) Object.assign(params, tab.query);
    const res = await getIntegralList(params, { silent: true });
    const d = res?.result ?? res?.data ?? [];
    const rows = Array.isArray(d) ? d : (d.list || d.content || []);
    if (rows.length < PAGE) hasMore.value = false;
    list.value = reset ? decorate(rows) : [...list.value, ...decorate(rows)];
    page.value++;
    loadError.value = false;
  } catch (e) {
    if (reset) {
      loadError.value = true; // 失败整卡重试，禁止 mock 回落
    } else {
      hasMore.value = false;
      uni.showToast({ title: '加载失败，点击重试', icon: 'none' });
    }
  } finally {
    loading.value = false;
  }
};

const switchTab = (tab) => {
  if (activeTabId.value === tab.id) return;
  activeTabId.value = tab.id;
  loadBills(true);
};

const goHome = () => uni.switchTab({ url: '/pages/index/index' });

onShow(() => {
  // 「已到账」等入口跳入时定位筛选（PRD v1.2 §1/B5）：消费即清空的全局意图态
  const intent = appStore.consumeBillFilterIntent();
  if (intent && tabs.some((t) => t.id === intent)) activeTabId.value = intent;
  loadBills(true);
});
</script>

<style scoped>
.page-bill { min-height: 100vh; background: #F6F7F9; display: flex; flex-direction: column; padding-bottom: calc(32rpx + env(safe-area-inset-bottom)); box-sizing: border-box; }

/* A 型等分文字 tab（§2） */
.filter-tabs { display: flex; background: #fff; position: sticky; top: 0; z-index: 10; box-shadow: 0 2rpx 8rpx rgba(31, 33, 38, 0.04); }
.filter-tab { flex: 1; display: flex; flex-direction: column; align-items: center; height: 88rpx; justify-content: center; position: relative; }
.filter-tab-text { font-size: 28rpx; color: #7A7F89; transition: all 0.25s ease; }
.filter-tab.active .filter-tab-text { color: #FF6B35; font-weight: 600; }
.filter-tab-indicator { position: absolute; bottom: 8rpx; width: 48rpx; height: 6rpx; border-radius: 3rpx; background: linear-gradient(90deg, #FF6B35, #FF8F65); }

/* 月份分组头（§6）：灰底上、缩进 */
.bill-month { margin-top: 8rpx; }
.month-text { display: block; font-size: 24rpx; color: #B6BAC2; margin: 20rpx 40rpx 8rpx; }

.bill-card { margin: 8rpx 24rpx 0; background: #fff; border-radius: 24rpx; overflow: hidden; box-shadow: 0 8rpx 32rpx rgba(31, 33, 38, 0.06); }

/* 行型 1（§3）：图标 + 业务名/时间 + 金额双色 */
.bill-item { display: flex; align-items: center; min-height: 108rpx; padding: 26rpx 28rpx; border-bottom: 1rpx solid #F2F3F5; box-sizing: border-box; }
.bill-item:last-child { border-bottom: none; }
.bill-icon { width: 64rpx; height: 64rpx; border-radius: 18rpx; background-repeat: no-repeat; background-position: center; background-size: 36rpx; margin-right: 20rpx; flex-shrink: 0; }
.icon-in { background-color: #FFF1EC; }
.icon-out { background-color: #F3F4F6; }
.bill-left { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.bill-title { font-size: 30rpx; color: #1F2126; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.bill-time { font-size: 22rpx; color: #B6BAC2; margin-top: 8rpx; }
.bill-amount { font-size: 32rpx; font-weight: bold; flex-shrink: 0; margin-left: 16rpx; }
.bill-amount.positive { color: #FF6B35; }
.bill-amount.negative { color: #1F2126; }

.skel { background: linear-gradient(90deg, #F0F1F3 25%, #F7F8FA 37%, #F0F1F3 63%); background-size: 400% 100%; animation: shimmer 1.2s ease infinite; border-radius: 8rpx; }
@keyframes shimmer { 0% { background-position: 100% 0; } 100% { background-position: 0 0; } }
.skel-icon { width: 64rpx; height: 64rpx; border-radius: 18rpx; margin-right: 20rpx; flex-shrink: 0; }
.skel-title { width: 200rpx; height: 28rpx; }
.skel-time { width: 140rpx; height: 20rpx; margin-top: 12rpx; }
.skel-amount { width: 110rpx; height: 32rpx; }

.bill-end { display: flex; align-items: center; justify-content: center; padding: 32rpx 0 40rpx; }
.end-line { width: 80rpx; height: 1rpx; background: #E5E6EB; }
.end-text { font-size: 22rpx; color: #B6BAC2; margin: 0 20rpx; }
</style>
