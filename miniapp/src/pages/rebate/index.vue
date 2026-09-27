<template>
  <view class="page-rebate">
    <!-- A 引导条（UI 规格 §2；本期仅淘宝，平台点标只渲染「淘」） -->
    <view class="guide-bar">
      <view class="platform-dots">
        <view class="platform-dot platform-dot--tb"><text class="platform-dot-text">淘</text></view>
      </view>
      <text class="guide-text">复制商品链接 › 打开小程序 › 粘贴查返利</text>
    </view>

    <!-- B 输入卡 -->
    <view class="input-area">
      <RebateInput v-model="inputText" placeholder="粘贴淘宝口令或商品链接" :loading="querying" @query="onQuery" />
    </view>

    <!-- C 会话流（时间倒序：新卡插顶；触底加载更早） -->
    <scroll-view
      class="chat-scroll"
      scroll-y
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresher"
      @scrolltolower="onLoadMore"
    >
      <view class="chat-list">
        <view class="chat-group" v-for="g in groups" :key="g.id">
          <view class="swipe-wrap">
            <view class="swipe-del" @click="removeGroup(g)">
              <text class="swipe-del-text">删除</text>
            </view>
            <view class="swipe-content" :style="{ transform: `translateX(${offsets[g.id] || 0}px)` }"
              @touchstart="onTouchStart(g.id, $event)"
              @touchmove="onTouchMove(g.id, $event)"
              @touchend="onTouchEnd(g.id)"
            >
              <!-- 用户气泡（右） -->
              <view class="msg-row msg-row--user">
                <view class="bubble bubble--user"><text class="bubble-user-text">{{ g.input }}</text></view>
                <image class="avatar avatar--user" :src="userAvatar" mode="aspectFill" />
              </view>
              <!-- 机器人侧（左） -->
              <view class="msg-row msg-row--bot">
                <view class="avatar avatar--bot"><text class="avatar-bot-text">返</text></view>
                <view class="bot-col">
                  <!-- 查询中 -->
                  <view class="bubble bubble--bot" v-if="g.status === 'loading'">
                    <view class="typing-dot"></view>
                    <text class="bubble-bot-text">正在识别商品…</text>
                  </view>
                  <!-- 失败占位条：不落历史，可点击重试 -->
                  <view class="fail-bar" v-else-if="g.status === 'failed'" @click="retryGroup(g)">
                    <text class="fail-bar-text">{{ g.failText || '查询失败' }}</text>
                    <text class="fail-bar-action">点击重试</text>
                  </view>
                  <!-- 结果卡 -->
                  <template v-else-if="g.status === 'ok'">
                    <view class="result-card">
                      <view class="rc-head">
                        <view class="platform-dot platform-dot--tb rc-head-dot"><text class="platform-dot-text">淘</text></view>
                        <text class="rc-title">{{ g.product.title || '淘宝商品' }}</text>
                      </view>
                      <view class="rc-body">
                        <image class="rc-pic" :src="g.product.pic || PLACEHOLDER_PIC" mode="aspectFill" @error="onPicError(g)" />
                        <view class="rc-info">
                          <view class="rc-line">
                            <text class="rc-label">券后：</text>
                            <text class="rc-price-symbol">¥</text>
                            <text class="rc-price">{{ g.product.price || '--.--' }}</text>
                          </view>
                          <view class="rc-line">
                            <text class="rc-label rc-label--sub">优惠：</text>
                            <text class="rc-coupon" :class="{ 'rc-coupon--none': !hasCoupon(g) }">{{ hasCoupon(g) ? g.product.couponAmount + '元券' : '无券' }}</text>
                          </view>
                          <view class="rc-line">
                            <text class="rc-label">返现：</text>
                            <text class="rc-reward" v-if="g.product.reward">¥{{ g.product.reward }}</text>
                            <text class="rc-reward rc-reward--pending" v-else>奖励 待计算</text>
                          </view>
                        </view>
                      </view>
                      <!-- 返现副注（管理 seq-514 定稿口径） -->
                      <text class="rc-reward-note">最终到账以订单结算为准</text>
                      <view class="rc-divider"></view>
                      <view class="rc-foot">
                        <view class="rc-foot-btn" hover-class="rc-foot-btn--hover" @click="goDetail(g)">
                          <text class="rc-foot-detail">查看详情 ›</text>
                        </view>
                        <view class="rc-foot-sep"></view>
                        <view class="rc-foot-btn" hover-class="rc-foot-btn--hover" @click="onOrderRebate(g)">
                          <text class="rc-foot-order">下单返{{ g.product.reward ? ' ¥' + g.product.reward : '' }} ›</text>
                        </view>
                      </view>
                    </view>
                    <text class="order-hint">复制口令，打开淘宝下单</text>
                    <!-- 提示条 -->
                    <view class="bubble bubble--bot tip-bar">
                      <view class="tip-star" :style="{ backgroundImage: starIcon }"></view>
                      <text class="bubble-bot-text">付款时使用红包会导致订单失效！系统会自动抵扣，可取消。</text>
                    </view>
                    <view class="more-link" hover-class="more-link--hover" @click="goMoreSearch(g)">
                      <view class="more-link-icon" :style="{ backgroundImage: pointIcon }"></view>
                      <text class="more-link-text">查看更多搜索结果 ›</text>
                    </view>
                  </template>
                </view>
              </view>
            </view>
          </view>
        </view>

        <view class="load-more" v-if="loadingMore"><text class="load-more-text">加载中</text></view>
        <view class="load-end" v-else-if="groups.length && !hasMore"><text class="load-end-text">— 没有更早的查询了 —</text></view>
        <Empty v-if="!groups.length && !loadingMore" text="还没有查询记录，粘贴淘宝链接/口令试试" />
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
// 查返利独立页（产品 rebate-page-spec v1.0 + UI rebate-page-design-spec v1.0.1）
// 口令/链接 → parse → 结果卡（不自动跳商详，用户主动点「查看详情」才跳）；纯文字 → 路由回搜索页关键词搜索
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import RebateInput from '../../components/RebateInput.vue';
import Empty from '../../components/Empty.vue';
import { parseTbGoods, getTbGoodsDetail, getTbGoodsWord } from '../../api/product';
import { normalizeProduct } from '../../utils/product';
import { loadCommissionInfo } from '../../utils/commission';
import { addRebateRecord, removeRebateRecord, loadRebatePage, takePendingRebateInput, REBATE_PAGE_SIZE } from '../../utils/rebateHistory';
import { getUserInfo } from '../../utils/auth';
import { lineIcon } from '../../utils/icons';

const PLACEHOLDER_PIC = '';

const inputText = ref('');
const querying = ref(false);
const groups = ref([]);
const page = ref(0);
const hasMore = ref(true);
const loadingMore = ref(false);
const refreshing = ref(false);
const offsets = ref({});

const starIcon = lineIcon('#FF6B35', "<path d='M12 3.5l2.4 5.3 5.6.6-4.2 3.9 1.2 5.6L12 16.9l-5 2 1.2-5.6L4 9.4l5.6-.6z'/>");
const pointIcon = lineIcon('#FF6B35', "<path d='M4 12h11'/><path d='M11 7l5 5-5 5'/><circle cx='4.5' cy='12' r='1.2'/>");

const userAvatar = computed(() => {
  try { return getUserInfo()?.avatarUrl || '/static/logo.png'; } catch (e) { return '/static/logo.png'; }
});

const hasCoupon = (g) => Number(g.product.couponAmount) > 0;

// 输入判定（产品 §2）：链接/口令特征 → 淘宝查询链路；非淘平台链接 → toast 禁假查询；纯文字 → 搜索页
const LINK_TPWD_RE = /(https?:\/\/\S+)|([¥￥$]\S+[¥￥$])|淘口令/;
const NON_TB_RE = /jd\.com|pinduoduo|yangkeduo|douyin|iesdouyin|vip\.com|suning/i;

const onQuery = async (text) => {
  const raw = (text || '').trim();
  if (!raw || querying.value) return;
  if (!LINK_TPWD_RE.test(raw)) {
    // 纯文字 → 现关键词搜索（此路径不落查返利历史，产品 §1）
    uni.navigateTo({ url: `/pages/search/index?platform=tb&keyword=${encodeURIComponent(raw)}` });
    return;
  }
  if (NON_TB_RE.test(raw)) {
    uni.showToast({ title: '暂支持淘宝商品，其他平台陆续开放', icon: 'none' });
    return; // 禁假查询、禁生成气泡记录
  }
  const id = `rb${Date.now()}${Math.random().toString(36).slice(2, 6)}`;
  groups.value.unshift({ id, ts: Date.now(), input: raw, status: 'loading', product: null, failText: '' });
  const g = groups.value[0];
  inputText.value = '';
  await runQuery(g, raw);
};

const runQuery = async (g, raw) => {
  querying.value = true;
  g.status = 'loading';
  try {
    const res = await parseTbGoods({ content: raw }, { silent: true });
    const d = res?.data ?? res?.result ?? {};
    const goodsId = String(d.goodsId || d.itemId || d.numIid || '');
    if ((d.dataType && d.dataType !== 'goods') || !goodsId) {
      throw new Error('未识别到有效淘宝商品，请检查链接/口令');
    }
    let detail = {};
    try {
      const dres = await getTbGoodsDetail({ goodsId }, { silent: true });
      detail = dres?.result || dres?.data || {};
    } catch (e) {
      // R-2（JAVA 调研中）：详情接口对部分 parse id 报 400 → 卡片回落 parse 可得字段，价格缺失走空值口径
      detail = {};
    }
    const p = normalizeProduct('tb', { ...detail, itemId: detail.itemId || goodsId, goodsId: detail.goodsId || goodsId });
    const priceNum = Number(p.price);
    // 返现公式定稿（产品 seq-511，管理 seq-514 放行）：预估返现 = 券后价 ×(commissionRate/100)×(tbRebateScale/100)，
    // 不乘 tbTimes；任一因子缺值 → null →「奖励 待计算」，禁 0 冒充
    const info = await loadCommissionInfo();
    const scale = Number(info?.tbRebateScale);
    const rate = Number(p.commissionRate ?? detail.commissionRate);
    const reward = priceNum > 0 && rate > 0 && scale > 0
      ? ((priceNum * rate) / 100 * scale / 100).toFixed(2)
      : null;
    g.product = {
      id: String(p.id || goodsId),
      title: p.title || '',
      pic: p.mainPic || '',
      price: priceNum > 0 ? p.price : '',
      couponAmount: p.couponAmount || 0,
      reward: reward || '',
    };
    g.status = 'ok';
    // 成功才落历史（失败不落库，产品 §2/§4）
    addRebateRecord({ id: g.id, ts: g.ts, input: g.input, status: 'ok', product: g.product });
  } catch (e) {
    g.status = 'failed';
    g.failText = e?.message || '查询失败，请检查网络';
  } finally {
    querying.value = false;
  }
};

const retryGroup = (g) => {
  if (querying.value) return;
  runQuery(g, g.input);
};

const goDetail = (g) => {
  uni.navigateTo({ url: `/pages/product/detail?id=${encodeURIComponent(g.product.id)}&platform=tb` });
};

const goMoreSearch = (g) => {
  const kw = (g.product.title || '').slice(0, 12);
  if (!kw) return;
  uni.navigateTo({ url: `/pages/search/index?platform=tb&keyword=${encodeURIComponent(kw)}` });
};

// 下单返¥X：转链取口令 → 复制成功 toast（产品 §3），失败 toast，禁假成功
const onOrderRebate = async (g) => {
  try {
    const res = await getTbGoodsWord({ goodsId: g.product.id }, { silent: true });
    const d = res?.result || res?.data || {};
    const word = d.password || d.tbPwd || d.content || d.model || d.shortUrl || d.clickUrl || d.url || '';
    if (!word) throw new Error('empty');
    uni.setClipboardData({
      data: String(word),
      success: () => uni.showToast({ title: '口令已复制，打开淘宝下单', icon: 'none' }),
      fail: () => uni.showToast({ title: '暂时无法获取下单口令，请稍后重试', icon: 'none' }),
    });
  } catch (e) {
    uni.showToast({ title: '暂时无法获取下单口令，请稍后重试', icon: 'none' });
  }
};

const onPicError = (g) => { g.product.pic = ''; };

/* ---------- 历史分页（本地 20/页，触底加载更早） ---------- */
const loadFirstPage = () => {
  const { items } = loadRebatePage(0);
  groups.value = items;
  page.value = 0;
  hasMore.value = items.length >= REBATE_PAGE_SIZE;
};

const onLoadMore = async () => {
  if (!hasMore.value || loadingMore.value || !groups.value.length) return;
  loadingMore.value = true;
  const { items } = loadRebatePage(page.value + 1);
  if (items.length) {
    groups.value = [...groups.value, ...items];
    page.value++;
  }
  if (items.length < REBATE_PAGE_SIZE) hasMore.value = false;
  loadingMore.value = false;
};

const onRefresher = () => {
  refreshing.value = true;
  loadFirstPage(); // 下拉刷新 = 重读本地（产品 §4，无网络请求）
  setTimeout(() => { refreshing.value = false; }, 300);
};

/* ---------- 右滑单条删除（无批量清空入口） ---------- */
const DEL_PX = uni.upx2px(140);
const touchState = {};

const onTouchStart = (id, e) => {
  touchState[id] = { x: e.touches[0].clientX, y: e.touches[0].clientY, base: offsets.value[id] || 0 };
};
const onTouchMove = (id, e) => {
  const s = touchState[id];
  if (!s) return;
  const dx = e.touches[0].clientX - s.x;
  const dy = e.touches[0].clientY - s.y;
  if (Math.abs(dy) > Math.abs(dx)) return; // 纵向滚动优先
  const next = Math.max(-DEL_PX, Math.min(0, s.base + dx));
  offsets.value = { ...offsets.value, [id]: next };
};
const onTouchEnd = (id) => {
  const cur = offsets.value[id] || 0;
  offsets.value = { ...offsets.value, [id]: cur < -DEL_PX / 2 ? -DEL_PX : 0 };
};

const removeGroup = (g) => {
  removeRebateRecord(g.id);
  groups.value = groups.value.filter((x) => x.id !== g.id);
  const next = { ...offsets.value };
  delete next[g.id];
  offsets.value = next;
};

/* ---------- 搜索页口令路由交接（switchTab 不支持 query，storage 一次性传参） ---------- */
onShow(() => {
  const pending = takePendingRebateInput();
  if (pending) onQuery(pending);
});

loadFirstPage();
</script>

<style scoped>
.page-rebate {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #F6F7F9;
}

/* A 引导条 */
.guide-bar {
  display: flex;
  align-items: center;
  gap: 12rpx;
  height: 64rpx;
  background: #FFF7F0;
  padding: 0 24rpx;
  flex-shrink: 0;
}
.platform-dots { display: flex; align-items: center; }
.platform-dot {
  width: 28rpx;
  height: 28rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: -6rpx;
}
.platform-dot:first-child { margin-left: 0; }
.platform-dot--tb { background: #FF5000; }
.platform-dot-text { font-size: 18rpx; color: #fff; line-height: 1; }
.guide-text { font-size: 24rpx; color: #FF6B35; font-weight: 500; }

/* B 输入卡 */
.input-area { padding: 16rpx 24rpx 8rpx; flex-shrink: 0; }

/* C 会话流 */
.chat-scroll { flex: 1; min-height: 0; }
.chat-list { padding: 16rpx 24rpx calc(32rpx + env(safe-area-inset-bottom)); }

.chat-group { margin-bottom: 28rpx; }
.swipe-wrap { position: relative; overflow: hidden; border-radius: 16rpx; }
.swipe-del {
  position: absolute;
  top: 0; right: 0; bottom: 0;
  width: 140rpx;
  background: #FF4D4F;
  display: flex;
  align-items: center;
  justify-content: center;
}
.swipe-del-text { font-size: 26rpx; color: #fff; font-weight: 600; }
.swipe-content { position: relative; background: #F6F7F9; transition: transform 0.2s ease; }

.msg-row { display: flex; align-items: flex-start; margin-bottom: 16rpx; }
.msg-row--user { justify-content: flex-end; }
.msg-row--bot { justify-content: flex-start; }

.avatar { width: 56rpx; height: 56rpx; flex-shrink: 0; }
.avatar--user { border-radius: 50%; margin-left: 20rpx; }
.avatar--bot {
  border-radius: 12rpx;
  background: linear-gradient(135deg, #FF6B35, #FF8F65);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
}
.avatar-bot-text { font-size: 26rpx; color: #fff; font-weight: 700; }

.bubble { max-width: 78%; padding: 18rpx 22rpx; }
.bubble--user {
  background: #95EC69;
  border-radius: 16rpx;
  border-top-right-radius: 4rpx;
  margin-left: 20rpx;
}
.bubble-user-text { font-size: 28rpx; color: #1D1D1D; line-height: 1.5; word-break: break-all; }
.bubble--bot {
  background: #fff;
  border-radius: 16rpx;
  border-top-left-radius: 4rpx;
}
.bubble-bot-text { font-size: 26rpx; color: #1F2126; line-height: 1.5; }

.bot-col { max-width: 78%; display: flex; flex-direction: column; align-items: flex-start; gap: 12rpx; }

.typing-dot {
  width: 24rpx; height: 24rpx;
  border: 3rpx solid rgba(255, 107, 53, 0.25);
  border-top-color: #FF6B35;
  border-radius: 50%;
  animation: rb-spin 0.7s linear infinite;
  display: inline-block;
  margin-right: 8rpx;
}
@keyframes rb-spin { to { transform: rotate(360deg); } }

/* 失败占位条 */
.fail-bar {
  width: 100%;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  background: #FFF3F3;
  border: 1rpx solid rgba(255, 77, 79, 0.25);
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
}
.fail-bar-text { font-size: 26rpx; color: #7A7F89; flex: 1; }
.fail-bar-action { font-size: 26rpx; color: #FF6B35; font-weight: 600; flex-shrink: 0; }

/* 结果卡 */
.result-card {
  width: 100%;
  box-sizing: border-box;
  background: #fff;
  border-radius: 16rpx;
  border-top-left-radius: 4rpx;
  padding: 20rpx;
}
.rc-head { display: flex; align-items: flex-start; gap: 10rpx; margin-bottom: 16rpx; }
.rc-head-dot { flex-shrink: 0; margin-top: 4rpx; }
.rc-title {
  font-size: 28rpx; color: #1F2126; font-weight: 500; line-height: 1.4;
  display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden;
}
.rc-body { display: flex; gap: 20rpx; }
.rc-pic { width: 160rpx; height: 160rpx; border-radius: 12rpx; background: #F7F8FA; flex-shrink: 0; }
.rc-info { flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 12rpx; min-width: 0; }
.rc-line { display: flex; align-items: baseline; }
.rc-label { font-size: 26rpx; color: #7A7F89; }
.rc-label--sub { color: #7A7F89; }
.rc-price-symbol { font-size: 24rpx; color: #FF6B35; font-weight: 600; }
.rc-price { font-size: 30rpx; color: #FF6B35; font-weight: 700; }
.rc-coupon { font-size: 26rpx; color: #7A7F89; }
.rc-coupon--none { color: #B6BAC2; }
.rc-reward { font-size: 30rpx; color: #1F2126; font-weight: 600; }
.rc-reward--pending { font-size: 26rpx; color: #B6BAC2; font-weight: 400; }
.rc-reward-note { display: block; font-size: 22rpx; color: #B6BAC2; margin-top: 12rpx; }
.rc-divider { height: 1rpx; background: #F2F3F5; margin: 16rpx 0 4rpx; }
.rc-foot { display: flex; align-items: center; }
.rc-foot-btn { flex: 1; display: flex; align-items: center; justify-content: center; padding: 16rpx 0; border-radius: 12rpx; }
.rc-foot-btn--hover { background: #FAFBFC; }
.rc-foot-detail { font-size: 28rpx; color: #1F2126; font-weight: 500; }
.rc-foot-order { font-size: 28rpx; color: #FF6B35; font-weight: 600; }
.rc-foot-sep { width: 1rpx; height: 40rpx; background: #F2F3F5; }

.order-hint { font-size: 22rpx; color: #B6BAC2; align-self: center; }

.tip-bar { display: flex; align-items: flex-start; gap: 8rpx; }
.tip-star { width: 28rpx; height: 28rpx; flex-shrink: 0; margin-top: 4rpx; background-size: 100% 100%; background-repeat: no-repeat; }

.more-link {
  display: flex;
  align-items: center;
  gap: 8rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 16rpx 22rpx;
}
.more-link--hover { background: #FAFBFC; }
.more-link-icon { width: 28rpx; height: 28rpx; background-size: 100% 100%; background-repeat: no-repeat; }
.more-link-text { font-size: 28rpx; color: #FF6B35; }

.load-more, .load-end { display: flex; justify-content: center; padding: 24rpx 0; }
.load-more-text { font-size: 24rpx; color: #7A7F89; }
.load-end-text { font-size: 22rpx; color: #B6BAC2; }
</style>
