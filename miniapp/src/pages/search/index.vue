<template>
  <view class="page-search">
    <view class="search-header">
      <view class="header-row1">
        <view class="back-btn" @click="goBack">
          <view class="back-icon"></view>
        </view>
        <view class="search-input-wrap" :class="{ 'search-input-wrap--focus': inputFocus }">
          <view class="input-icon"></view>
          <input
            class="search-input"
            v-model="keyword"
            :placeholder="SEARCH_PLACEHOLDER"
            placeholder-class="search-input-ph"
            confirm-type="search"
            focus
            @focus="inputFocus = true"
            @blur="inputFocus = false"
            @confirm="onSearch"
          />
          <view class="clear-btn" v-if="keyword" @click="clearKeyword">
            <view class="clear-icon"></view>
          </view>
        </view>
      </view>
      <!-- S-2（UI 规格 shared/searchbox-redesign-spec-v1.md §1 方案A 双行式）：Row2 粘贴幽灵键 + 查返利主按钮 -->
      <view class="header-row2">
        <view class="paste-btn" hover-class="paste-btn--hover" @click="onPaste">
          <text class="paste-btn-text">粘贴</text>
        </view>
        <view
          class="search-go-btn"
          :class="{ 'search-go-btn--disabled': !canSearch, 'search-go-btn--loading': loading }"
          hover-class="search-go-btn--hover"
          @click="onSearch"
        >
          <view class="btn-spinner" v-if="loading"></view>
          <text class="search-go-btn-text">{{ loading ? '查询中…' : '查返利' }}</text>
        </view>
      </view>
    </view>

    <view class="search-body" v-if="!hasSearched">
      <view class="history-section" v-if="searchHistory.length">
        <view class="section-header">
          <text class="section-title">搜索历史</text>
          <text class="section-action" @click="clearHistory">清空</text>
        </view>
        <view class="history-tags">
          <text
            class="history-tag"
            v-for="(kw, idx) in searchHistory"
            :key="idx"
            @click="onKeywordClick(kw)"
          >{{ kw }}</text>
        </view>
      </view>

      <view class="hot-section" v-if="hotList.length">
        <view class="section-header">
          <text class="section-title">热门搜索</text>
        </view>
        <view class="hot-list">
          <view
            class="hot-item"
            v-for="(kw, idx) in hotList"
            :key="kw"
            @click="onKeywordClick(kw)"
            hover-class="hot-item--hover"
          >
            <view class="hot-rank" :class="{ 'hot-rank--top': idx < 3 }">
              <text class="hot-rank-text">{{ idx + 1 }}</text>
            </view>
            <text class="hot-keyword">{{ kw }}</text>
            <view class="hot-heat" v-if="idx < 3">
              <text class="hot-heat-text">HOT</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="search-result" v-if="hasSearched">
      <PlatformTab :current="currentPlatform" @change="onPlatformChange" />
      <view class="product-list">
        <view class="skeleton-list" v-if="loading && !productList.length && !searchError">
          <view class="skeleton-card" v-for="i in 3" :key="i">
            <view class="skeleton-image"></view>
            <view class="skeleton-info">
              <view class="skeleton-line skeleton-title"></view>
              <view class="skeleton-line skeleton-price"></view>
              <view class="skeleton-line skeleton-tag"></view>
            </view>
          </view>
        </view>
        <ProductCard
          v-for="(item, idx) in productList"
          :key="item.id || idx"
          :product="item"
          @click="goDetail"
        />
        <FailRetry v-if="searchError && !productList.length" :text="searchErrorText" @retry="doSearch(true)" />
        <Loading :visible="loading && productList.length > 0" text="加载中..." />
        <Empty
          v-if="!loading && !searchError && !productList.length"
          text="未找到相关商品"
        />
        <view class="load-end" v-if="!hasMore && productList.length && !searchError">
          <text class="load-end-text">— 已经到底了 —</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { onLoad, onReachBottom } from '@dcloudio/uni-app';
import PlatformTab from '../../components/PlatformTab.vue';
import ProductCard from '../../components/ProductCard.vue';
import Loading from '../../components/Loading.vue';
import Empty from '../../components/Empty.vue';
import FailRetry from '../../components/FailRetry.vue';
import { searchTbGoods, searchJdGoods, getPddGoodsList, searchDyGoods, getSearchHot, parseTbGoods } from '../../api/product';
import { normalizeProductList } from '../../utils/product';
import { mockHotKeywords } from '../../mock/index';

// 仅显式开发开关，默认关闭；线上禁止回落 mock
const USE_MOCK = false;
const HISTORY_KEY = 'search_history';
const MAX_HISTORY = 10;
const PAGE_SIZE = 20;
// S-2 提示语主稿（产品 seq-446 定稿；窄屏实测截断时回落短版「粘贴链接/口令，或搜关键词」，二级灰字方案作废）
const SEARCH_PLACEHOLDER = '粘贴商品链接/口令，或搜标题/关键词';
// 查返利三分支（规格 §3）：命中链接/口令特征 → parse；纯文字 → 现搜索；parse 不识别 → toast 禁假搜索
const LINK_TPWD_RE = /(https?:\/\/\S+)|([¥￥$]\S+[¥￥$])|淘口令/;
const isLinkOrTpwd = (kw) => LINK_TPWD_RE.test(kw);

const currentPlatform = ref('tb');
const keyword = ref('');
const hotList = ref([]);
const productList = ref([]);
const loading = ref(false);
const searchError = ref(false);
const searchErrorText = ref('搜索失败，请检查网络');
const hasSearched = ref(false);
const hasMore = ref(true);
const page = ref(1);
const inputFocus = ref(false);
const searchHistory = ref([]);

const loadHistory = () => {
  try {
    const data = uni.getStorageSync(HISTORY_KEY);
    if (data) searchHistory.value = JSON.parse(data);
  } catch (e) { searchHistory.value = []; }
};

const saveHistory = (kw) => {
  const list = searchHistory.value.filter((k) => k !== kw);
  list.unshift(kw);
  searchHistory.value = list.slice(0, MAX_HISTORY);
  try { uni.setStorageSync(HISTORY_KEY, JSON.stringify(searchHistory.value)); } catch (e) { /* ignore */ }
};

const clearHistory = () => {
  uni.showModal({
    title: '清空搜索历史',
    content: '确定清空全部搜索历史吗？',
    confirmText: '清空',
    confirmColor: '#FF6B35',
    success: (r) => {
      if (!r.confirm) return;
      searchHistory.value = [];
      try { uni.removeStorageSync(HISTORY_KEY); } catch (e) { /* ignore */ }
    },
  });
};

const loadHot = async () => {
  if (USE_MOCK) { hotList.value = mockHotKeywords(); return; }
  try {
    const res = await getSearchHot({ silent: true });
    const list = res.result || res.data || [];
    hotList.value = list.map((k) => k.name || k).filter(Boolean).slice(0, 10);
  } catch (e) {
    hotList.value = [];
  }
};

// JD 搜索已开放（JAVA seq-177 P1-4，/jd/goods/search 好单库透传，参数名 pageId/pageSize）
const searchByPlatform = (params, opts) => {
  switch (currentPlatform.value) {
    case 'tb': return searchTbGoods(params, opts);
    case 'jd': return searchJdGoods(params, opts);
    case 'pdd': return getPddGoodsList(params, opts);
    case 'dy': return searchDyGoods(params, opts);
    default: return searchTbGoods(params, opts);
  }
};

// 四平台搜索入参键名各异（8019 实测）：tb=keyWords、jd=keyword+pageId/pageSize（统一 VO）、
// pdd=word、dy=keyword；参数名错传时后端分别 500/空 list（S-1 顺带修复，同路径核查）
const buildSearchParams = (kw) => {
  switch (currentPlatform.value) {
    case 'tb': return { keyWords: kw, page: page.value, limit: PAGE_SIZE };
    case 'jd': return { keyword: kw, pageId: page.value, pageSize: PAGE_SIZE };
    case 'pdd': return { word: kw, page: page.value, limit: PAGE_SIZE };
    default: return { keyword: kw, page: page.value, limit: PAGE_SIZE };
  }
};

const doSearch = async (reset = false) => {
  if (loading.value) return;
  const kw = keyword.value.trim();
  if (!kw) return;
  if (reset) {
    page.value = 1;
    hasMore.value = true;
    productList.value = [];
    searchError.value = false;
  }
  if (!reset && !hasMore.value) return;
  hasSearched.value = true;
  loading.value = true;
  if (reset) saveHistory(kw);
  if (USE_MOCK) {
    await new Promise((r) => setTimeout(r, 400));
    return;
  }
  try {
    const params = buildSearchParams(kw);
    const res = await searchByPlatform(params, { silent: true });
    const d = res?.result ?? res?.data ?? [];
    const rawList = Array.isArray(d) ? d : (d.list || d.content || d.rows || []);
    if (rawList.length < PAGE_SIZE) hasMore.value = false;
    // S-1（管理 seq-437 根因定稿）：四平台原始 VO 键名各异（JD 统一 VO 为 img/startPrice），
    // ProductCard 只认归一后形状（mainPic/price）——必须过 normalizeProductList 再入列表，否则 ¥0.00+空图
    const list = normalizeProductList(currentPlatform.value, rawList);
    productList.value = reset ? list : [...productList.value, ...list];
    page.value++;
  } catch (e) {
    if (!productList.value.length) {
      // dy 上游官方政策下线（8019 实测 code=400）：给可行动文案而非「检查网络」，与首页口径一致
      searchErrorText.value = currentPlatform.value === 'dy'
        ? '抖音平台接口临时下线，切其他平台看看'
        : (e?.message || '搜索失败，请检查网络');
      searchError.value = true;
      // S-2 键态「失败」：按钮恢复可用 + toast 后端 message（规格 §1.3），结果区同步走 FailRetry
      uni.showToast({ title: searchErrorText.value, icon: 'none' });
    } else {
      hasMore.value = false;
      uni.showToast({ title: '加载更多失败', icon: 'none' });
    }
  } finally {
    loading.value = false;
  }
};

const canSearch = computed(() => !!keyword.value.trim());

// S-2 粘贴键：写入输入框但不自动触发搜索（防误触跳转，等用户按「查返利」确认）
const onPaste = () => {
  uni.getClipboardData({
    success: (r) => {
      const text = (r.data || '').trim();
      if (!text) { uni.showToast({ title: '剪贴板暂无内容', icon: 'none' }); return; }
      keyword.value = text;
    },
    fail: () => uni.showToast({ title: '剪贴板暂无内容', icon: 'none' }),
  });
};

// S-2 查返利三分支（产品 seq-446 定稿）：淘宝口令/链接 → parse 跳商详；纯文字 → 现搜索；
// parse 不识别 → toast，禁假搜索/mock 回落
const onSearch = async () => {
  const kw = keyword.value.trim();
  if (!kw || loading.value) return;
  if (!isLinkOrTpwd(kw)) { doSearch(true); return; }
  loading.value = true;
  try {
    const res = await parseTbGoods({ content: kw }, { silent: true });
    const d = res?.data ?? res?.result ?? {};
    const itemId = String(d.itemId || d.goodsId || '');
    if (d.dataType === 'goods' && itemId) {
      // 口令/链接不入搜索历史（历史词点击会重搜，长口令截断后必坏）
      uni.navigateTo({ url: `/pages/product/detail?id=${itemId}&platform=tb` });
    } else {
      uni.showToast({ title: '未识别到有效商品链接/口令', icon: 'none' });
    }
  } catch (e) {
    uni.showToast({ title: e?.message || '识别失败，请检查链接/口令', icon: 'none' });
  } finally {
    loading.value = false;
  }
};

const clearKeyword = () => {
  keyword.value = '';
  hasSearched.value = false;
  productList.value = [];
  searchError.value = false;
};

const onPlatformChange = (p) => {
  currentPlatform.value = p;
  if (keyword.value.trim()) doSearch(true);
};
const onKeywordClick = (kw) => { keyword.value = kw; doSearch(true); };
const goBack = () => uni.navigateBack();
const goDetail = (product) => {
  const pid = product.id || product.itemId || product.goodsId;
  uni.navigateTo({ url: `/pages/product/detail?id=${pid}&platform=${currentPlatform.value}` });
};

onLoad((opts) => {
  if (opts?.platform) currentPlatform.value = opts.platform;
  if (opts?.keyword) {
    keyword.value = decodeURIComponent(opts.keyword);
    doSearch(true);
  }
});
onReachBottom(() => { if (hasSearched.value) doSearch(false); });
onMounted(() => {
  loadHot();
  loadHistory();
});
</script>

<style scoped>
.page-search {
  min-height: 100vh;
  background: #F6F7F9;
}

.search-header {
  background: #fff;
  padding: 16rpx 20rpx 20rpx;
  position: sticky;
  top: 0;
  z-index: 10;
}
.header-row1 {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.header-row2 {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16rpx;
  height: 72rpx;
  margin-top: 8rpx;
}
.back-btn {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.back-icon {
  width: 32rpx;
  height: 32rpx;
  background-image: url("data:image/svg+xml,%3Csvg%20xmlns%3D%27http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%27%20viewBox%3D%270%200%2024%2024%27%20fill%3D%27none%27%20stroke%3D%27%231F2126%27%20stroke-width%3D%272%27%20stroke-linecap%3D%27round%27%20stroke-linejoin%3D%27round%27%3E%3Cpath%20d%3D%27M15%204l-8%208%208%208%27%2F%3E%3C%2Fsvg%3E");
  background-size: 100% 100%;
  background-repeat: no-repeat;
}
.search-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  background: #F3F4F6;
  border-radius: 999rpx;
  padding: 0 20rpx;
  height: 68rpx;
  gap: 10rpx;
  border: 1rpx solid transparent;
  transition: border-color 0.15s ease;
}
.search-input-wrap--focus { border-color: #FF6B35; }
.input-icon {
  width: 26rpx;
  height: 26rpx;
  flex-shrink: 0;
  background-image: url("data:image/svg+xml,%3Csvg%20xmlns%3D%27http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%27%20viewBox%3D%270%200%2024%2024%27%20fill%3D%27none%27%20stroke%3D%27%23B6BAC2%27%20stroke-width%3D%272%27%20stroke-linecap%3D%27round%27%20stroke-linejoin%3D%27round%27%3E%3Ccircle%20cx%3D%2711%27%20cy%3D%2711%27%20r%3D%277.5%27%2F%3E%3Cpath%20d%3D%27M21%2021l-4.3-4.3%27%2F%3E%3C%2Fsvg%3E");
  background-size: 100% 100%;
  background-repeat: no-repeat;
}
.search-input {
  flex: 1;
  font-size: 28rpx;
  color: #1F2126;
  height: 68rpx;
}
.search-input-ph { color: #B6BAC2; font-size: 24rpx; }
.clear-btn {
  width: 36rpx;
  height: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #B6BAC2;
  border-radius: 50%;
  flex-shrink: 0;
}
.clear-icon {
  width: 16rpx;
  height: 16rpx;
  background-image: url("data:image/svg+xml,%3Csvg%20xmlns%3D%27http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%27%20viewBox%3D%270%200%2024%2024%27%20fill%3D%27none%27%20stroke%3D%27%23fff%27%20stroke-width%3D%273%27%20stroke-linecap%3D%27round%27%3E%3Cpath%20d%3D%27M5%205l14%2014M19%205L5%2019%27%2F%3E%3C%2Fsvg%3E");
  background-size: 100% 100%;
  background-repeat: no-repeat;
}
.paste-btn {
  display: flex;
  align-items: center;
  padding: 12rpx 32rpx;
  border-radius: 999rpx;
  background: #fff;
  border: 1rpx solid rgba(255, 107, 53, 0.4);
}
.paste-btn--hover { background: #FFF7F0; }
.paste-btn-text {
  font-size: 26rpx;
  color: #FF6B35;
  font-weight: 500;
}
.search-go-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  height: 64rpx;
  padding: 0 40rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #FF6B35, #FF8F65);
  box-shadow: 0 6rpx 16rpx rgba(255, 107, 53, 0.35);
  transition: transform 0.15s ease, opacity 0.15s ease;
}
.search-go-btn--hover { transform: scale(0.97); opacity: 0.9; }
.search-go-btn-text {
  font-size: 28rpx;
  color: #fff;
  font-weight: 600;
}
.search-go-btn--disabled {
  background: #F3F4F6;
  box-shadow: none;
}
.search-go-btn--disabled .search-go-btn-text { color: #B6BAC2; }
.search-go-btn--loading { pointer-events: none; }
.btn-spinner {
  width: 28rpx;
  height: 28rpx;
  border: 3rpx solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: btn-spin 0.7s linear infinite;
}
.search-go-btn--disabled .btn-spinner { border-color: rgba(182, 186, 194, 0.4); border-top-color: #B6BAC2; }
@keyframes btn-spin { to { transform: rotate(360deg); } }

.search-body {
  background: #fff;
  margin-top: 16rpx;
  border-radius: 24rpx 24rpx 0 0;
  padding-bottom: 24rpx;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 28rpx 16rpx;
}
.section-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #1F2126;
}
.section-action {
  font-size: 24rpx;
  color: #7A7F89;
}

.history-section {
  padding-bottom: 16rpx;
}
.history-tags {
  display: flex;
  flex-wrap: wrap;
  padding: 0 28rpx;
  gap: 16rpx;
}
.history-tag {
  font-size: 24rpx;
  color: #1F2126;
  background: #F5F6F7;
  padding: 10rpx 24rpx;
  border-radius: 999rpx;
}

.hot-section {
  padding-bottom: 24rpx;
}
.hot-list {
  padding: 0 28rpx;
}
.hot-item {
  display: flex;
  align-items: center;
  padding: 18rpx 0;
  border-bottom: 1rpx solid #F2F3F5;
  transition: background 0.15s;
}
.hot-item:last-child { border-bottom: none; }
.hot-item--hover { background: #FAFBFC; }
.hot-rank {
  width: 40rpx;
  height: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8rpx;
  background: #F0F1F3;
  margin-right: 20rpx;
  flex-shrink: 0;
}
.hot-rank--top {
  background: linear-gradient(135deg, #FF6B35, #FF8F65);
}
.hot-rank-text {
  font-size: 22rpx;
  color: #7A7F89;
  font-weight: 600;
}
.hot-rank--top .hot-rank-text { color: #fff; }
.hot-keyword {
  flex: 1;
  font-size: 28rpx;
  color: #1F2126;
}
.hot-heat {
  background: rgba(255, 107, 53, 0.1);
  padding: 4rpx 10rpx;
  border-radius: 6rpx;
}
.hot-heat-text {
  font-size: 18rpx;
  color: #FF6B35;
  font-weight: 700;
}

.search-result { margin-top: 0; }
.product-list { padding: 16rpx 24rpx; }

.skeleton-list { display: flex; flex-direction: column; gap: 16rpx; }
.skeleton-card {
  display: flex;
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
}
.skeleton-image {
  width: 240rpx;
  height: 240rpx;
  background: linear-gradient(90deg, #F0F1F3 25%, #E8E9EC 50%, #F0F1F3 75%);
  background-size: 200% 100%;
  animation: shimmer 1.2s infinite;
}
.skeleton-info { flex: 1; padding: 20rpx; display: flex; flex-direction: column; gap: 16rpx; }
.skeleton-line {
  height: 28rpx;
  border-radius: 6rpx;
  background: linear-gradient(90deg, #F0F1F3 25%, #E8E9EC 50%, #F0F1F3 75%);
  background-size: 200% 100%;
  animation: shimmer 1.2s infinite;
}
.skeleton-title { width: 80%; }
.skeleton-price { width: 40%; height: 36rpx; }
.skeleton-tag { width: 60%; height: 24rpx; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.load-end { text-align: center; padding: 32rpx 0 48rpx; }
.load-end-text { font-size: 22rpx; color: #B6BAC2; }
</style>
