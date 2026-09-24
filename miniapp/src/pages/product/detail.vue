<template>
  <view class="page-detail">
    <!-- F-4（测试基线 v3）：加载失败走错误态+重试，禁止 mock 假数据回落 -->
    <view v-if="loadError" class="detail-error"><FailRetry @retry="loadDetail" /></view>
    <block v-else>
    <view class="detail-swiper-wrap">
      <swiper class="detail-swiper" autoplay circular :duration="500" @change="onSwiperChange">
        <swiper-item v-for="(img, idx) in images" :key="idx">
          <image :src="img" mode="aspectFill" class="detail-image" />
        </swiper-item>
      </swiper>
      <view class="swiper-counter" v-if="images.length > 1">
        <text class="swiper-counter-text">{{ currentIndex + 1 }}/{{ images.length }}</text>
      </view>
      <view class="platform-flag" :style="{ background: platformColor }">
        <text class="platform-flag-text">{{ platformName }}</text>
      </view>
    </view>

    <view class="detail-price-card">
      <view class="price-row">
        <text class="price-symbol">¥</text>
        <text class="price-main">{{ priceInt }}</text>
        <text class="price-decimal">.{{ priceDec }}</text>
        <text class="price-original" v-if="product.originalPrice">¥{{ formatMoney(product.originalPrice) }}</text>
      </view>
      <view class="price-badges">
        <view class="badge-coupon" v-if="product.couponAmount" @click.stop="receiveCoupon">
          <text class="badge-coupon-text">券 ¥{{ formatMoney(product.couponAmount) }}</text>
        </view>
        <view class="badge-shipping" v-if="product.freeShipping">
          <text class="badge-shipping-text">包邮</text>
        </view>
      </view>
      <view class="commission-row">
        <!-- TODO(B-2 挂起·管理 seq-338/350)：补贴三字段展示位（C-1 冻结稿字段名放行后接） -->
        <view class="commission-box">
          <text class="commission-label">{{ COPYWRITING.COMMISSION_LABEL }}</text>
          <text class="commission-value">¥{{ commissionAmount }}</text>
        </view>
      </view>
    </view>

    <view class="detail-title-card">
      <view class="title-tags">
        <text class="title-tag" v-if="product.shopType">{{ product.shopType }}</text>
      </view>
      <text class="detail-title">{{ product.title }}</text>
    </view>

    <view class="detail-shop-card" v-if="product.shopName">
      <view class="shop-info">
        <view class="shop-avatar-wrap">
          <text class="shop-avatar-text">{{ (product.shopName || '店')[0] }}</text>
        </view>
        <view class="shop-meta">
          <text class="shop-name">{{ product.shopName }}</text>
          <text class="shop-desc">优质店铺 · 品质保障</text>
        </view>
      </view>
    </view>

    <view class="detail-content-card">
      <view class="content-header">
        <view class="content-header-bar"></view>
        <text class="content-header-text">商品详情</text>
      </view>
      <rich-content v-if="product.detail" :content="product.detail" />
      <view class="content-placeholder" v-else>
        <text class="content-placeholder-text">暂无详情</text>
      </view>
    </view>
    </block>

    <view class="bottom-bar" v-if="!loadError">
      <view class="bottom-share" @click="onShare" hover-class="bottom-btn--hover">
        <text class="bottom-share-icon">&#x1F4E4;</text>
        <text class="bottom-share-text">{{ COPYWRITING.SHARE_BUTTON }}</text>
      </view>
      <view class="bottom-buy" @click="onBuy" hover-class="bottom-btn--hover">
        <text class="bottom-buy-text">立即购买</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue';
import { onLoad, onShareAppMessage } from '@dcloudio/uni-app';
import { getTbGoodsDetail, getTbGoodsWord, getJdGoodsDetail, getJdGoodsWord, getJdKuGoodsDetail, getPddGoodsDetail, getPddGoodsWord, getDyGoodsDetail, getDyGoodsWord } from '../../api/product';
import { formatMoney } from '../../utils/format';
import { COPYWRITING, PLATFORM } from '../../utils/constants';
import FailRetry from '../../components/FailRetry.vue';
import { mockProductDetail } from '../../mock/index';

const USE_MOCK = false;

const product = ref({});
const images = ref([]);
const platform = ref('tb');
const productId = ref('');
const currentIndex = ref(0);
const loadError = ref(false);

const platformInfo = computed(() => PLATFORM[platform.value.toUpperCase()] || PLATFORM.TB);
const platformName = computed(() => platformInfo.value.label);
const platformColor = computed(() => {
  const colors = { tb: '#ff4d4f', jd: '#e4393c', pdd: '#e02e24', dy: '#161823' };
  return colors[platform.value] || '#ff6b35';
});

const priceStr = computed(() => {
  const p = product.value?.price || 0;
  return typeof p === 'string' ? p : Number(p).toFixed(2);
});
const priceInt = computed(() => priceStr.value.split('.')[0]);
const priceDec = computed(() => priceStr.value.split('.')[1] || '00');

const commissionAmount = computed(() => {
  const c = Number(product.value.commission || product.value.estimateCommission || 0);
  return (c * 0.8).toFixed(2);
});

const onSwiperChange = (e) => {
  currentIndex.value = e.detail.current;
};

// F-11（管理 seq-299 定稿）：JD DTK 详情上游 403 → 兜底 ku 透传通道（/ku/jd/goods/detail?goodsId=加密ID）。
// ku 响应 {code,msg,data} 为透传 VO，字段映射到本页视图模型；兜底再失败则上抛走整页错误态。
const fetchJdDetail = async (params, opts) => {
  try {
    return await getJdGoodsDetail(params, opts);
  } catch (e) {
    const ku = await getJdKuGoodsDetail({ goodsId: params.id }, opts);
    const d = ku?.data || ku?.result || {};
    if (!d.title && !d.goodsId && !d.itemId) throw e;
    return {
      data: {
        ...d,
        price: d.startPrice,
        originalPrice: d.endPrice,
        couponAmount: d.coupon,
        commission: d.fee,
        // 8008 实测：img=主图，details=详情图数组；合并成 '|' 分隔串复用本页切图逻辑
        images: [d.img, ...(Array.isArray(d.details) ? d.details : [])].filter(Boolean).join('|'),
      },
    };
  }
};

const loadDetail = async () => {
  if (USE_MOCK) {
    const data = mockProductDetail(platform.value);
    product.value = data;
    images.value = data.images.split('|').filter(Boolean);
    return;
  }
  loadError.value = false;
  const params = { id: productId.value };
  const opts = { silent: true }; // 错误由本页 FailRetry 承载，不重复弹 toast
  try {
    let res;
    switch (platform.value) {
      case 'tb': res = await getTbGoodsDetail(params, opts); break;
      case 'jd': res = await fetchJdDetail(params, opts); break;
      case 'pdd': res = await getPddGoodsDetail(params, opts); break;
      case 'dy': res = await getDyGoodsDetail(params, opts); break;
    }
    const data = res?.result || res?.data || {};
    product.value = data;
    images.value = data.images || data.pics ? (data.images || data.pics).split('|').filter(Boolean) : (data.mainPic ? [data.mainPic] : []);
  } catch (e) {
    loadError.value = true; // F-4：错误态+点击重试（FailRetry），正式链路禁 mock 假数据
  }
};

const onBuy = async () => {
  if (USE_MOCK) {
    uni.showToast({ title: 'Mock模式：跳转模拟', icon: 'none' });
    return;
  }
  try {
    let res;
    const params = { id: productId.value };
    const opts = { silent: true }; // 本页统一「获取链接失败」提示，避免双 toast
    switch (platform.value) {
      case 'tb': res = await getTbGoodsWord(params, opts); break;
      case 'jd': res = await getJdGoodsWord(params, opts); break;
      case 'pdd': res = await getPddGoodsWord(params, opts); break;
      case 'dy': res = await getDyGoodsWord(params, opts); break;
    }
    const data = res?.result || res?.data || {};
    const url = data.clickUrl || data.url || data.shortUrl || '';
    if (url) {
      uni.navigateTo({ url: `/pages/webview/index?url=${encodeURIComponent(url)}` });
    } else {
      uni.showToast({ title: '获取链接失败', icon: 'none' });
    }
  } catch (e) {
    uni.showToast({ title: '获取链接失败', icon: 'none' });
  }
};

const onShare = () => {
  uni.showToast({ title: '请点击右上角分享', icon: 'none' });
};

const receiveCoupon = () => {
  uni.showToast({ title: '优惠券已领取', icon: 'success' });
};

onShareAppMessage(() => ({
  title: `${COPYWRITING.SHARE_CARD_TITLE}`,
  path: `/pages/product/detail?id=${productId.value}&platform=${platform.value}`,
}));

onLoad((opts) => {
  productId.value = opts.id || '';
  platform.value = opts.platform || 'tb';
  loadDetail();
});
</script>

<style scoped>
.page-detail {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: calc(130rpx + env(safe-area-inset-bottom));
}

.detail-swiper-wrap {
  position: relative;
}
.detail-swiper {
  height: 640rpx;
}
.detail-image {
  width: 100%;
  height: 100%;
}
.swiper-counter {
  position: absolute;
  bottom: 20rpx;
  right: 24rpx;
  background: rgba(0, 0, 0, 0.45);
  border-radius: 20rpx;
  padding: 6rpx 16rpx;
}
.swiper-counter-text {
  font-size: 22rpx;
  color: #fff;
}
.platform-flag {
  position: absolute;
  top: 24rpx;
  left: 0;
  padding: 8rpx 20rpx 8rpx 16rpx;
  border-radius: 0 20rpx 20rpx 0;
}
.platform-flag-text {
  font-size: 22rpx;
  color: #fff;
  font-weight: 600;
}

.detail-price-card {
  background: #fff;
  margin: -30rpx 24rpx 0;
  border-radius: 24rpx;
  padding: 28rpx 28rpx 20rpx;
  position: relative;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
}
.price-row {
  display: flex;
  align-items: baseline;
  gap: 2rpx;
}
.price-symbol {
  font-size: 28rpx;
  color: #ff4d4f;
  font-weight: bold;
}
.price-main {
  font-size: 52rpx;
  color: #ff4d4f;
  font-weight: 800;
  line-height: 1;
}
.price-decimal {
  font-size: 28rpx;
  color: #ff4d4f;
  font-weight: bold;
}
.price-original {
  font-size: 24rpx;
  color: #ccc;
  text-decoration: line-through;
  margin-left: 16rpx;
}
.price-badges {
  display: flex;
  gap: 12rpx;
  margin-top: 16rpx;
}
.badge-coupon {
  background: linear-gradient(135deg, #fff7f0, #fff0e6);
  border: 1rpx solid rgba(255, 107, 53, 0.2);
  border-radius: 8rpx;
  padding: 6rpx 16rpx;
}
.badge-coupon-text {
  font-size: 22rpx;
  color: #ff6b35;
  font-weight: 500;
}
.badge-shipping {
  background: #f0faf0;
  border: 1rpx solid rgba(7, 193, 96, 0.2);
  border-radius: 8rpx;
  padding: 6rpx 16rpx;
}
.badge-shipping-text {
  font-size: 22rpx;
  color: #07c160;
}
.commission-row {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #f5f5f5;
}
.commission-box {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  background: linear-gradient(135deg, #fff7f0, #fff0e6);
  border-radius: 12rpx;
  padding: 10rpx 20rpx;
}
.commission-label {
  font-size: 24rpx;
  color: #ff8f65;
}
.commission-value {
  font-size: 32rpx;
  color: #ff6b35;
  font-weight: bold;
}

.detail-title-card {
  background: #fff;
  margin: 16rpx 24rpx 0;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
}
.title-tags {
  display: flex;
  gap: 8rpx;
  margin-bottom: 12rpx;
}
.title-tag {
  font-size: 20rpx;
  color: #ff6b35;
  background: rgba(255, 107, 53, 0.08);
  border-radius: 6rpx;
  padding: 4rpx 12rpx;
}
.detail-title {
  font-size: 30rpx;
  color: #333;
  line-height: 1.6;
  font-weight: 500;
}

.detail-shop-card {
  background: #fff;
  margin: 16rpx 24rpx 0;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
}
.shop-info {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.shop-avatar-wrap {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b35, #ff8f65);
  display: flex;
  align-items: center;
  justify-content: center;
}
.shop-avatar-text {
  font-size: 28rpx;
  color: #fff;
  font-weight: bold;
}
.shop-meta {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}
.shop-name {
  font-size: 28rpx;
  color: #333;
  font-weight: 600;
}
.shop-desc {
  font-size: 22rpx;
  color: #999;
}

.detail-content-card {
  background: #fff;
  margin: 16rpx 24rpx 0;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
}
.content-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 24rpx;
}
.content-header-bar {
  width: 6rpx;
  height: 28rpx;
  background: linear-gradient(180deg, #ff6b35, #ff8f65);
  border-radius: 3rpx;
}
.content-header-text {
  font-size: 28rpx;
  color: #333;
  font-weight: 600;
}
.content-placeholder {
  padding: 60rpx 0;
  text-align: center;
}
.content-placeholder-text {
  font-size: 26rpx;
  color: #ccc;
}

.detail-error { padding: 120rpx 24rpx 0; }

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  background: #fff;
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
  gap: 16rpx;
}
.bottom-share {
  flex: 1;
  height: 80rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fff7f0;
  border-radius: 40rpx;
  gap: 2rpx;
  transition: opacity 0.15s;
}
.bottom-share-icon {
  font-size: 24rpx;
}
.bottom-share-text {
  font-size: 22rpx;
  color: #ff6b35;
  font-weight: 500;
}
.bottom-buy {
  flex: 2;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff6b35, #ff8f65);
  border-radius: 40rpx;
  box-shadow: 0 8rpx 20rpx rgba(255, 107, 53, 0.3);
  transition: opacity 0.15s;
}
.bottom-buy-text {
  font-size: 30rpx;
  color: #fff;
  font-weight: 600;
}
.bottom-btn--hover {
  opacity: 0.85;
}
</style>
