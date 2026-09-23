<script setup>
import { onLaunch } from '@dcloudio/uni-app';
import { useUserStore } from './store/user';
import { captureInviteScene, flushInviteCode } from './utils/invite';

onLaunch((options) => {
  captureInviteScene(options?.query); // 扫码进入：scene "c=<邀请码>" → 暂存待注册绑定
  const userStore = useUserStore();
  if (userStore.isLoggedIn) {
    userStore.refreshUserInfo();
    flushInviteCode();
  }
});
</script>

<style>
page {
  background-color: #F6F7F9;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 28rpx;
  color: #333;
}
</style>
