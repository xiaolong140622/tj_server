<template>
  <view class="page-index">
    <view class="header-bg">
      <view class="search-bar" hover-class="search-bar--hover" @click="goSearch">
        <view class="search-icon"></view>
        <text class="search-prefix">搜索</text>
        <text class="search-placeholder">{{ hotWord || '搜索商品名称，找同款更省钱' }}</text>
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

    <view class="category-grid" v-if="categoryList.length">
      <view class="category-item" v-for="cat in categoryList" :key="cat.id" @click="onCategoryClick(cat)" hover-class="category-item--hover">
        <view class="category-icon-wrap">
          <image :src="cat.pic" class="category-icon" mode="aspectFill" />
        </view>
        <text class="category-name">{{ cat.name }}</text>
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
        <FailRetry v-if="productError && !productList.length" text="商品加载失败" @retry="loadProducts(true)" />
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
import { getHomeBanner, getTbGoodsList, getPddGoodsList, getDyKuList, getJdRankList, getSearchHot, getCategoryList } from '../../api/product';
import { loadCommissionInfo } from '../../utils/commission';
import { mockBanners, mockCategories, mockHotWord, mockProducts } from '../../mock/index';

// 仅显式开发开关，默认关闭；线上禁止回落 mock
const USE_MOCK = false;

const currentPlatform = ref('tb');
const bannerList = ref([]);
const bannerFailed = ref({});
const bannerCurrent = ref(0);
const categoryList = ref([]);
const productList = ref([]);
const hotWord = ref('');
const loading = ref(false);
const productError = ref(false);
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

const loadCategory = async () => {
  if (USE_MOCK) { categoryList.value = mockCategories(); return; }
  try {
    const res = await getCategoryList({ silent: true });
    categoryList.value = res.result || res.data || [];
  } catch (e) {
    categoryList.value = [];
  }
};

const loadProducts = async (reset = false) => {
  if (loading.value) return;
  if (!reset && !hasMore.value) return;
  if (reset) { page.value = 1; hasMore.value = true; productList.value = []; productError.value = false; }
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
      case 'dy': res = await getDyKuList(params, { silent: true }); break;
      default: res = await getTbGoodsList(params, { silent: true }); break;
    }
    const d = res?.result ?? res?.data ?? [];
    const list = Array.isArray(d) ? d : (d.list || d.content || d.rows || []);
    if (list.length < 20) hasMore.value = false;
    productList.value = reset ? list : [...productList.value, ...list];
    page.value++;
  } catch (e) {
    if (!productList.value.length) {
      productError.value = true;
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
    const TAB_PAGES = ['/pages/index/index', '/pages/order/list', '/pages/user/spread', '/pages/user/index'];
    const path = target.split('?')[0];
    if (TAB_PAGES.includes(path)) uni.switchTab({ url: path });
    else uni.navigateTo({ url: target });
  } else {
    uni.navigateTo({ url: `/pages/webview/index?url=${encodeURIComponent(target)}` });
  }
};

const onCategoryClick = (cat) => {
  uni.navigateTo({ url: `/pages/search/index?platform=${currentPlatform.value}&keyword=${encodeURIComponent(cat.name || '')}` });
};

onReachBottom(() => loadProducts());
onPullDownRefresh(async () => {
  await Promise.all([loadBanner(), loadHotWord(), loadCategory(), loadProducts(true)]);
  uni.stopPullDownRefresh();
});

onMounted(() => {
  loadBanner();
  loadHotWord();
  loadCategory();
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
.search-bar {
  display: flex;
  align-items: center;
  height: 72rpx;
  padding: 0 28rpx;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 999rpx;
  gap: 12rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
}
.search-bar--hover { opacity: 0.9; }
.search-icon {
  width: 28rpx;
  height: 28rpx;
  flex-shrink: 0;
  background-image: url("data:image/svg+xml,%3Csvg%20xmlns%3D%27http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%27%20viewBox%3D%270%200%2024%2024%27%20fill%3D%27none%27%20stroke%3D%27%23B6BAC2%27%20stroke-width%3D%272%27%20stroke-linecap%3D%27round%27%20stroke-linejoin%3D%27round%27%3E%3Ccircle%20cx%3D%2711%27%20cy%3D%2711%27%20r%3D%277.5%27%2F%3E%3Cpath%20d%3D%27M21%2021l-4.3-4.3%27%2F%3E%3C%2Fsvg%3E");
  background-size: 100% 100%;
  background-repeat: no-repeat;
}
.search-prefix { font-size: 26rpx; color: #B6BAC2; flex-shrink: 0; }
.search-placeholder {
  font-size: 26rpx;
  color: #7A7F89;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

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

.category-grid {
  display: flex;
  flex-wrap: wrap;
  background: #fff;
  margin: 20rpx 24rpx 0;
  border-radius: 24rpx;
  padding: 24rpx 12rpx 8rpx;
  box-shadow: 0 8rpx 32rpx rgba(31, 33, 38, 0.06);
}
.category-item {
  width: 20%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 16rpx;
  transition: transform 0.15s ease;
}
.category-item--hover { transform: scale(0.92); }
.category-icon-wrap {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  overflow: hidden;
  background: #F7F8FA;
}
.category-icon { width: 100%; height: 100%; }
.category-name {
  font-size: 22rpx;
  color: #7A7F89;
  margin-top: 10rpx;
  max-width: 120rpx;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
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
