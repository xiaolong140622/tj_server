<template>
  <view class="page-poster">
    <view class="poster-preview">
      <view class="poster-card">
        <view class="poster-brand">
          <view class="brand-deco brand-deco-1"></view>
          <view class="brand-deco brand-deco-2"></view>
          <text class="poster-app-name">苏分宝</text>
          <text class="poster-slogan">好物精选 省钱利器</text>
        </view>
        <view class="poster-body">
          <text class="poster-invite">{{ COPYWRITING.SHARE_POSTER_TITLE }}</text>
          <view class="qrcode-wrap">
            <image class="poster-qrcode" :src="posterQrcode" mode="aspectFit" v-if="posterQrcode" />
            <view v-else class="poster-placeholder"><text>海报生成中...</text></view>
          </view>
          <text class="poster-guide">长按识别小程序码</text>
        </view>
        <text class="poster-tip">{{ COPYWRITING.REWARD_DISCLAIMER }}</text>
      </view>
    </view>

    <view class="poster-actions">
      <button class="btn-save" hover-class="btn-hover" @click="savePoster">保存到相册</button>
      <button class="btn-share" hover-class="btn-hover" open-type="share">分享给好友</button>
    </view>

    <canvas canvas-id="posterCanvas" id="posterCanvas" class="poster-canvas"
      :style="{ width: canvasW + 'px', height: canvasH + 'px' }" />
  </view>
</template>

<script setup>
import { ref } from 'vue';
import { onShow, onShareAppMessage } from '@dcloudio/uni-app';
import { getSpreadCode } from '../../api/share';
import { COPYWRITING } from '../../utils/constants';
import { mockSpreadCode } from '../../mock/index';

const USE_MOCK = false;

const posterQrcode = ref('');
const canvasW = 375;
const canvasH = 600;

onShow(async () => {
  let qrcode = '';
  if (USE_MOCK) {
    const data = mockSpreadCode();
    qrcode = data.qrcode;
  } else {
    try {
      const res = await getSpreadCode();
      const d = res.result || res.data;
      qrcode = d?.code || d?.qrcode || '';
    } catch (e) {
      const data = mockSpreadCode();
      qrcode = data.qrcode;
    }
  }
  posterQrcode.value = qrcode;
  drawPoster(qrcode);
});

onShareAppMessage(() => ({
  title: COPYWRITING.SHARE_POSTER_TITLE,
}));

const drawPoster = (qrcodeUrl) => {
  const ctx = uni.createCanvasContext('posterCanvas');
  const w = canvasW;
  const h = canvasH;

  ctx.setFillStyle('#ffffff');
  ctx.fillRect(0, 0, w, h);

  ctx.setFillStyle('#ff6b35');
  ctx.fillRect(0, 0, w, 80);
  ctx.setFontSize(22);
  ctx.setFillStyle('#ffffff');
  ctx.setTextAlign('center');
  ctx.fillText('苏分宝', w / 2, 35);
  ctx.setFontSize(13);
  ctx.setFillStyle('rgba(255,255,255,0.85)');
  ctx.fillText('好物精选 省钱利器', w / 2, 60);

  ctx.setFontSize(16);
  ctx.setFillStyle('#333333');
  ctx.fillText('邀请好友一起省', w / 2, 120);

  ctx.setFillStyle('#f8f8f8');
  ctx.fillRect(40, 150, w - 80, 300);
  ctx.setStrokeStyle('#eeeeee');
  ctx.setLineWidth(1);
  ctx.strokeRect(40, 150, w - 80, 300);

  ctx.setFontSize(13);
  ctx.setFillStyle('#666666');
  ctx.fillText('长按识别小程序码', w / 2, 480);
  ctx.fillText('立即开启省钱之旅', w / 2, 502);

  ctx.setFillStyle('#f0f0f0');
  ctx.fillRect(60, 530, w - 120, 40);
  ctx.setFontSize(11);
  ctx.setFillStyle('#999999');
  ctx.fillText('预估奖励金额仅供参考', w / 2, 548);
  ctx.fillText('实际以平台结算为准', w / 2, 564);

  ctx.draw(false, () => {
    if (qrcodeUrl) {
      ctx.drawImage(qrcodeUrl, (w - 160) / 2, 180, 160, 160);
      ctx.draw(true);
    }
  });
};

const savePoster = () => {
  uni.canvasToTempFilePath({
    canvasId: 'posterCanvas',
    success: (res) => {
      uni.saveImageToPhotosAlbum({
        filePath: res.tempFilePath,
        success: () => uni.showToast({ title: '已保存到相册', icon: 'success' }),
        fail: () => uni.showToast({ title: '保存失败', icon: 'none' }),
      });
    },
    fail: () => uni.showToast({ title: '海报生成失败', icon: 'none' }),
  });
};
</script>

<style scoped>
.page-poster { min-height: 100vh; background: #f5f5f5; display: flex; flex-direction: column; align-items: center; padding: 48rpx 32rpx 0; }

.poster-preview { width: 100%; display: flex; justify-content: center; margin-bottom: 48rpx; }
.poster-card {
  width: 560rpx;
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.1);
}
.poster-brand {
  position: relative;
  width: 100%;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8f65 100%);
  padding: 44rpx 0 36rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  overflow: hidden;
}
.brand-deco { position: absolute; border-radius: 50%; background: rgba(255, 255, 255, 0.1); }
.brand-deco-1 { width: 180rpx; height: 180rpx; top: -70rpx; right: -50rpx; }
.brand-deco-2 { width: 120rpx; height: 120rpx; bottom: -50rpx; left: -30rpx; }
.poster-app-name { font-size: 44rpx; color: #fff; font-weight: bold; }
.poster-slogan { font-size: 24rpx; color: rgba(255, 255, 255, 0.88); margin-top: 10rpx; }

.poster-body { display: flex; flex-direction: column; align-items: center; padding: 40rpx 0 24rpx; }
.poster-invite { font-size: 32rpx; color: #333; font-weight: bold; margin-bottom: 28rpx; }
.qrcode-wrap {
  width: 360rpx;
  height: 360rpx;
  border-radius: 20rpx;
  overflow: hidden;
  border: 2rpx solid #f5f5f5;
  background: #f8f8f8;
}
.poster-qrcode { width: 100%; height: 100%; }
.poster-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: #999; font-size: 24rpx; }
.poster-guide { font-size: 24rpx; color: #999; margin-top: 20rpx; }
.poster-tip { font-size: 22rpx; color: #bbb; padding: 20rpx 0 28rpx; }

.poster-actions { display: flex; gap: 24rpx; width: 100%; }
.btn-hover { opacity: 0.85; transform: scale(0.98); }
.btn-save {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  background: linear-gradient(135deg, #ff6b35 0%, #ff8f65 100%);
  color: #fff;
  border-radius: 44rpx;
  font-size: 30rpx;
  border: none;
  padding: 0;
  box-shadow: 0 6rpx 16rpx rgba(255, 107, 53, 0.35);
}
.btn-share {
  flex: 1;
  height: 88rpx;
  line-height: 84rpx;
  background: #fff;
  color: #ff6b35;
  border: 2rpx solid #ff6b35;
  border-radius: 44rpx;
  font-size: 30rpx;
  padding: 0;
}
.poster-canvas { position: fixed; left: -9999rpx; top: -9999rpx; }
</style>
