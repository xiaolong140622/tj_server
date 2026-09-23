import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useAppStore = defineStore('app', () => {
  const currentPlatform = ref('tb');
  const bannerList = ref([]);
  const hotKeywords = ref([]);

  const setPlatform = (p) => { currentPlatform.value = p; };
  const setBanner = (list) => { bannerList.value = list; };
  const setHotKeywords = (list) => { hotKeywords.value = list; };

  // tabBar 页无法通过 URL 传参（switchTab 限制）：spread 三段 tab 定位用全局意图态，消费即清空
  const spreadTabIntent = ref('');
  const setSpreadTabIntent = (t) => { spreadTabIntent.value = t; };
  const consumeSpreadTabIntent = () => {
    const v = spreadTabIntent.value;
    spreadTabIntent.value = '';
    return v;
  };

  return {
    currentPlatform, bannerList, hotKeywords, spreadTabIntent,
    setPlatform, setBanner, setHotKeywords, setSpreadTabIntent, consumeSpreadTabIntent,
  };
});
