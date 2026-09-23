import { ref, computed, watch } from 'vue';
import { useUserStore } from '../store/user';
import { editUser } from '../api/user';
import { BASE_URL, TOKEN_KEY } from './constants';

// 模块级共享状态：个人中心 Hero、完善资料抽屉、设置页头像行共用同一换头像链路（无逻辑分叉）
const avatarPreview = ref('');
const avatarUploading = ref(false);
const avatarBroken = ref(false);

export function useAvatar() {
  const userStore = useUserStore();
  const displayUser = computed(() => userStore.userInfo || {});

  // QC-03 口径：avatar 为空才显示 logo 占位；任何有效 URL 直接加载，失败经 binderror 回落 logo
  const serverAvatar = computed(() => {
    const raw = displayUser.value.avatar || '';
    if (!raw) return '/static/logo.png';
    return raw.startsWith('/') ? BASE_URL + raw : raw;
  });
  const avatarDisplaySrc = computed(() => {
    if (avatarPreview.value) return avatarPreview.value;
    if (avatarBroken.value) return '/static/logo.png';
    return serverAvatar.value;
  });
  watch(serverAvatar, () => { avatarBroken.value = false; });

  const onAvatarImgError = () => { avatarBroken.value = true; };

  // chooseAvatar 临时文件在 Windows 开发者工具存在读取竞态（ENOENT，模拟器已知问题）：
  // 先复制到 USER_DATA_PATH 稳定路径（含延时重试）再上传；上传中锁重复点击
  const copyToStable = (src, ext) =>
    new Promise((resolve, reject) => {
      const base = typeof wx !== 'undefined' && wx.env ? wx.env.USER_DATA_PATH : '';
      if (!base) { reject(new Error('USER_DATA_PATH unavailable')); return; }
      const dest = `${base}/avatar_pick_${Date.now()}.${ext}`;
      let attemptsLeft = 2;
      const once = () => {
        uni.getFileSystemManager().copyFile({
          srcPath: src,
          destPath: dest,
          success: () => resolve(dest),
          fail: (err) => {
            if (attemptsLeft > 0) { attemptsLeft -= 1; setTimeout(once, 300); }
            else reject(err);
          },
        });
      };
      once();
    });

  const rollbackAvatar = (msg) => {
    avatarPreview.value = ''; // 失败回滚为原头像
    uni.showToast({ title: msg, icon: 'none' });
  };

  const uploadAvatar = (filePath) => {
    uni.uploadFile({
      url: BASE_URL + '/user/avatar/upload',
      filePath,
      name: 'file',
      header: { Authorization: `Bearer ${uni.getStorageSync(TOKEN_KEY)}` },
      success: async (res) => {
        let body = null;
        try { body = JSON.parse(res.data); } catch (e) { body = null; }
        const url = body && body.data ? body.data.url : '';
        if (res.statusCode !== 200 || !url) {
          rollbackAvatar((body && body.msg) || '头像上传失败，请确认后端服务后重试');
          return;
        }
        try {
          await editUser({ avatar: BASE_URL + url });
          await userStore.refreshUserInfo();
          avatarPreview.value = '';
          uni.showToast({ title: '头像已更新', icon: 'success' });
        } catch (e) {
          rollbackAvatar('头像保存失败，请重试');
        }
      },
      fail: () => rollbackAvatar('头像上传失败，请重试'),
      complete: () => { avatarUploading.value = false; },
    });
  };

  const onChooseAvatar = async (e) => {
    const tempPath = e.detail && e.detail.avatarUrl;
    if (!tempPath || avatarUploading.value) return;
    avatarBroken.value = false;
    avatarPreview.value = tempPath; // 本地秒换预览
    avatarUploading.value = true;
    const m = tempPath.match(/\.(\w+)(\?|$)/);
    const ext = m ? m[1] : 'png';
    try {
      const stable = await copyToStable(tempPath, ext);
      uploadAvatar(stable);
    } catch (err) {
      console.warn('avatar copy failed, upload temp file directly', err);
      uploadAvatar(tempPath);
    }
  };

  const onChooseAvatarError = (e) => {
    const msg = (e && e.detail && e.detail.errMsg) || '';
    if (msg.includes('cancel')) return; // 用户主动取消：静默处理，不弹错
    uni.showToast({ title: '未获取到微信头像（模拟器为已知问题，请真机重试）', icon: 'none' });
  };

  return {
    avatarUploading,
    avatarDisplaySrc,
    onChooseAvatar,
    onChooseAvatarError,
    onAvatarImgError,
  };
}
