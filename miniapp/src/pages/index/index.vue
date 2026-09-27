<template>
  <view class="page-index">
    <view class="header-bg">
      <!-- 用户 seq-482 第4点：首页伪搜索框加高为主视觉大卡（UI 规格 rebate-page-design-spec §5，≈260rpx）；点击进搜索页逻辑不变 -->
      <view class="home-search-card" hover-class="home-search-card--hover" @click="goSearch">
        <text class="home-search-title">找商品 · 查返利</text>
        <view class="home-search-display">
          <text class="home-search-ph">{{ hotWord || '粘贴商品链接/口令，或搜标题/关键词' }}</text>
        </view>
        <view class="home-search-actions">
          <view class="home-paste-btn" hover-class="home-paste-btn--hover" @click.stop="onHomePaste">
            <text class="home-paste-btn-text">粘贴</text>
          </view>
          <view class="home-go-btn" hover-class="home-go-btn--hover" @click.stop="goSearch">
            <text class="home-go-btn-text">查返利</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Banner：整组失败收起不占位；单帧失败显示浅橙占位 -->
    <view class="banner-wrap" v-if="bannerShown">
      <swiper
        class="banner-swiper"
        autoplay
        circular
        :interval="4000"
        :duration="500"
        :current="bannerCurrent"
        @change="onBannerChange"
      >
        <swiper-item v-for="(item, idx) in bannerList" :key="item.id || idx">
          <view
            class="banner-frame"
            :hover-class="isBannerClickable(item) ? 'banner-frame--hover' : 'none'"
            @click="onBannerClick(item)"
          >
            <image
              v-if="!bannerFailed[idx]"
              :src="item.imageUrl"
              mode="aspectFill"
              class="banner-image"
              :lazy-load="idx > 0"
              @error="onBannerImageError(idx)"
            />
            <view v-else class="banner-placeholder">
              <view class="banner-placeholder-icon"></view>
            </view>
          </view>
        </swiper-item>
      </swiper>
      <view class="banner-indicator" v-if="bannerList.length > 1">
        <view
          v-for="(item, idx) in bannerList"
          :key="idx"
          class="banner-dot"
          :class="{ 'banner-dot--active': bannerCurrent === idx }"
        ></view>
      </view>
    </view>

    <PlatformTab :current="currentPlatform" @change="onPlatformChange" />

    <view class="product-section">
      <view class="section-header">
        <text class="section-title">精选好物</text>
        <text class="section-subtitle">为你推荐</text>
      </view>
      <view class="product-list">
        <template v-if="!productError && !loading && productList.length">
          <ProductCard
            v-for="(item, idx) in productList"
            :key="item.id || idx"
            :product="item"
            @click="goDetail"
          />
        </template>
        <view class="skeleton-list" v-if="loading && !productList.length && !productError">
          <view class="skeleton-card" v-for="i in 4" :key="i">
            <view class="skeleton-image"></view>
            <view class="skeleton-info">
              <view class="skeleton-line skeleton-title"></view>
              <view class="skeleton-line skeleton-price"></view>
              <view class="skeleton-line skeleton-tag"></view>
            </view>
          </view>
        </view>
        <FailRetry v-if="productError && !productList.length" :text="productErrorText || '商品加载失败'" @retry="loadProducts(true)" />
        <Loading :visible="loading && productList.length > 0" text="加载更多..." />
        <Empty
          v-if="!loading && !productError && !productList.length"
          text="暂无商品"
          :action-text="currentPlatform !== 'tb' ? '换个平台看看' : ''"
          @action="onSwitchBackToTb"
        />
        <view class="load-end" v-if="!hasMore && productList.length && !productError">
          <text class="load-end-text">— 已经到底了 —</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { onReachBottom, onPullDownRefresh } from '@dcloudio/uni-app';
import PlatformTab from '../../components/PlatformTab.vue';
import ProductCard from '../../components/ProductCard.vue';
import Loading from '../../components/Loading.vue';
import Empty from '../../components/Empty.vue';
import FailRetry from '../../components/FailRetry.vue';
import { getHomeBanner, getTbGoodsList, getPddGoodsList, getDyKuList, getJdRankList, getSearchHot } from '../../api/product';
import { normalizeProductList } from '../../utils/product';
import { loadCommissionInfo } from '../../utils/commission';
import { mockBanners, mockHotWord, mockProducts } from '../../mock/index';

// 仅显式开发开关，默认关闭；线上禁止回落 mock
const USE_MOCK = false;

const currentPlatform = ref('tb');
const bannerList = ref([]);
const bannerFailed = ref({});
const bannerCurrent = ref(0);
const productList = ref([]);
const hotWord = ref('');
const loading = ref(false);
const productError = ref(false);
const productErrorText = ref('');
const page = ref(1);
const hasMore = ref(true);

// /home/banner 契约（JAVA seq-177 P1-3）：{id,imageUrl,title,type,target}，type∈product|category|search|page|h5。
// 防御性兼容 admin 配置原始字段（picUrl/image/img、name、url）；缺省 type 按 target 推断（pages/ 开头=page，否则 h5）
const adaptBanner = (raw) => {
  if (!Array.isArray(raw)) return [];
  return raw
    .map((b) => {
      const imageUrl = b.imageUrl || b.picUrl || b.image || b.img || b.pic || '';
      const target = b.target || b.url || '';
      let type = b.type || '';
      if (!type && target) type = String(target).startsWith('pages/') ? 'page' : 'h5';
      return { id: b.id, imageUrl, title: b.title || b.name || '', type, target };
    })
    .filter((b) => b.imageUrl);
};

const bannerShown = computed(() => {
  const failedCount = Object.keys(bannerFailed.value).filter((k) => bannerFailed.value[k]).length;
  return bannerList.value.length > 0 && failedCount < bannerList.value.length;
});

const loadBanner = async () => {
  if (USE_MOCK) { bannerList.value = adaptBanner(mockBanners()); return; }
  try {
    const res = await getHomeBanner({ silent: true });
    bannerList.value = adaptBanner(res.result || res.data);
  } catch (e) {
    bannerList.value = []; // 整组失败 → banner 区收起不占位（首页规范 §2），禁止 picsum/mock 兜底
  }
};

const loadHotWord = async () => {
  if (USE_MOCK) { hotWord.value = mockHotWord(); return; }
  try {
    const res = await getSearchHot({ silent: true });
    const list = res.result || res.data || [];
    if (list.length) hotWord.value = list[0].name || list[0];
  } catch (e) {
    hotWord.value = '';
  }
};

const loadProducts = async (reset = false) => {
  if (loading.value) return;
  if (!reset && !hasMore.value) return;
  if (reset) { page.value = 1; hasMore.value = true; productList.value = []; productError.value = false; productErrorText.value = ''; }
  loading.value = true;
  if (USE_MOCK) {
    await new Promise((r) => setTimeout(r, 400));
    const mockList = mockProducts(currentPlatform.value, 20);
    productList.value = reset ? mockList : [...productList.value, ...mockList];
    if (mockList.length < 20) hasMore.value = false;
    page.value++;
    loading.value = false;
    return;
  }
  try {
    let res;
    const params = { page: page.value, limit: 20 };
    switch (currentPlatform.value) {
      case 'jd': res = await getJdRankList(params, { silent: true }); break;
      case 'pdd': res = await getPddGoodsList(params, { silent: true }); break;
      case 'dy': res = await getDyKuList({ pageId: page.value, pageSize: 20 }, { silent: true }); break;
      default: res = await getTbGoodsList(params, { silent: true }); break;
    }
    // 第三方透传通道（好单库等）失败时返回 {code:4xx/5xx,msg}，业务码非 0/200 视为失败
    if (res && typeof res.code === 'number' && res.code !== 0 && res.code !== 200) {
      throw new Error(res.msg || '商品加载失败');
    }
    const d = res?.result ?? res?.data ?? [];
    const rawList = Array.isArray(d) ? d : (d.list || d.content || d.rows || []);
    const list = normalizeProductList(currentPlatform.value, rawList);
    if (list.length < 20) hasMore.value = false;
    productList.value = reset ? list : [...productList.value, ...list];
    page.value++;
  } catch (e) {
    if (!productList.value.length) {
      productError.value = true;
      productErrorText.value = currentPlatform.value === 'dy'
        ? '抖音平台接口临时下线，切其他平台看看'
        : (e?.message || '商品加载失败');
    } else {
      hasMore.value = false;
      uni.showToast({ title: '加载更多失败', icon: 'none' });
    }
  } finally {
    loading.value = false;
  }
};

const onPlatformChange = (platform) => {
  if (platform === currentPlatform.value) return;
  currentPlatform.value = platform;
  loadProducts(true);
};
const onSwitchBackToTb = () => onPlatformChange('tb');

const goSearch = () => {
  const q = hotWord.value ? `&keyword=${encodeURIComponent(hotWord.value)}` : '';
  uni.navigateTo({ url: `/pages/search/index?platform=${currentPlatform.value}${q}` });
};
// 首页大卡粘贴键（UI 规格 §5）：读剪贴板回填搜索页输入框，不自动触发查询（auto=0）
const onHomePaste = () => {
  uni.getClipboardData({
    success: (r) => {
      const text = (r.data || '').trim();
      if (!text) { uni.showToast({ title: '剪贴板暂无内容', icon: 'none' }); return; }
      uni.navigateTo({ url: `/pages/search/index?platform=${currentPlatform.value}&keyword=${encodeURIComponent(text)}&auto=0` });
    },
    fail: () => uni.showToast({ title: '剪贴板暂无内容', icon: 'none' }),
  });
};
const goDetail = (product) => {
  const { id, itemId, goodsId } = product;
  const pid = id || itemId || goodsId;
  uni.navigateTo({ url: `/pages/product/detail?id=${pid}&platform=${currentPlatform.value}` });
};

const onBannerChange = (e) => { bannerCurrent.value = e.detail.current; };
const onBannerImageError = (idx) => { bannerFailed.value = { ...bannerFailed.value, [idx]: true }; };

const isBannerClickable = (item) => !!item.target;

// type: product / category / search / page / h5（无 type 的旧帧带 url → webview）
const onBannerClick = (item) => {
  if (!isBannerClickable(item)) return;
  const t = item.type || 'h5';
  const target = item.target;
  if (t === 'product') {
    uni.navigateTo({ url: `/pages/product/detail?id=${encodeURIComponent(target)}&platform=${currentPlatform.value}` });
  } else if (t === 'search' || t === 'category') {
    uni.navigateTo({ url: `/pages/search/index?platform=${currentPlatform.value}&keyword=${encodeURIComponent(target)}` });
  } else if (t === 'page') {
    const TAB_PAGES = ['/pages/index/index', '/pages/rebate/index', '/pages/user/spread', '/pages/user/index'];
    const path = target.split('?')[0];
    if (TAB_PAGES.includes(path)) uni.switchTab({ url: path });
    else uni.navigateTo({ url: target });
  } else {
    uni.navigateTo({ url: `/pages/webview/index?url=${encodeURIComponent(target)}` });
  }
};

onReachBottom(() => loadProducts());
onPullDownRefresh(async () => {
  await Promise.all([loadBanner(), loadHotWord(), loadProducts(true)]);
  uni.stopPullDownRefresh();
});

onMounted(() => {
  loadBanner();
  loadHotWord();
  loadProducts(true);
  loadCommissionInfo();
});
</script>

<style scoped>
.page-index {
  min-height: 100vh;
  background: #F6F7F9;
}

.header-bg {
  background: linear-gradient(180deg, #FF6B35, #FF9A62);
  padding: 20rpx 24rpx 28rpx;
}
/* 首页主视觉大卡（UI 规格 rebate-page-design-spec §5）：≈260rpx，整卡可点进搜索页 */
.home-search-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 12rpx 32rpx rgba(0, 0, 0, 0.10);
}
.home-search-card--hover { opacity: 0.95; }
.home-search-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #1F2126;
}
.home-search-display {
  margin-top: 20rpx;
  padding: 18rpx 4rpx;
  border-bottom: 1rpx solid #F2F3F5;
}
.home-search-ph {
  font-size: 30rpx;
  color: #B6BAC2;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  display: block;
}
.home-search-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16rpx;
  margin-top: 24rpx;
}
.home-paste-btn {
  display: flex;
  align-items: center;
  padding: 12rpx 32rpx;
  border-radius: 999rpx;
  background: #fff;
  border: 1rpx solid rgba(255, 107, 53, 0.4);
}
.home-paste-btn--hover { background: #FFF7F0; }
.home-paste-btn-text { font-size: 26rpx; color: #FF6B35; font-weight: 500; }
.home-go-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 64rpx;
  padding: 0 40rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #FF6B35, #FF8F65);
  box-shadow: 0 6rpx 16rpx rgba(255, 107, 53, 0.35);
}
.home-go-btn--hover { transform: scale(0.97); opacity: 0.9; }
.home-go-btn-text { font-size: 28rpx; color: #fff; font-weight: 600; }

.banner-wrap {
  position: relative;
  margin: 20rpx 24rpx 0;
}
.banner-swiper {
  height: 280rpx;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 32rpx rgba(31, 33, 38, 0.06);
}
.banner-frame { width: 100%; height: 100%; transition: transform 0.15s ease; }
.banner-frame--hover { transform: scale(0.99); }
.banner-image { width: 100%; height: 100%; }
.banner-placeholder {
  width: 100%;
  height: 100%;
  background: #FFF1EC;
  display: flex;
  align-items: center;
  justify-content: center;
}
.banner-placeholder-icon {
  width: 56rpx;
  height: 56rpx;
  background-image: url("data:image/svg+xml,%3Csvg%20xmlns%3D%27http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%27%20viewBox%3D%270%200%2024%2024%27%20fill%3D%27none%27%20stroke%3D%27%23FF8F65%27%20stroke-width%3D%272%27%20stroke-linecap%3D%27round%27%20stroke-linejoin%3D%27round%27%3E%3Crect%20x%3D%273%27%20y%3D%275%27%20width%3D%2718%27%20height%3D%2714%27%20rx%3D%272%27%2F%3E%3Ccircle%20cx%3D%278.5%27%20cy%3D%2710%27%20r%3D%271.5%27%2F%3E%3Cpath%20d%3D%27M21%2015l-5-5L5%2019%27%2F%3E%3C%2Fsvg%3E");
  background-size: 100% 100%;
  background-repeat: no-repeat;
}
.banner-indicator {
  position: absolute;
  right: 16rpx;
  bottom: 14rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.banner-dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 4rpx;
  background: rgba(0, 0, 0, 0.15);
  transition: width 0.25s ease, background 0.25s ease;
}
.banner-dot--active {
  width: 24rpx;
  background: #FF6B35;
}

.product-section { margin-top: 8rpx; }
.section-header {
  display: flex;
  align-items: baseline;
  padding: 20rpx 28rpx 12rpx;
  gap: 12rpx;
}
.section-title { font-size: 32rpx; color: #1F2126; font-weight: 700; }
.section-subtitle { font-size: 22rpx; color: #B6BAC2; }

.product-list { padding: 0 24rpx; }

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
