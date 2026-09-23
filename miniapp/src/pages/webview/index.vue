<template>
  <web-view :src="webUrl" v-if="webUrl" />
</template>

<script setup>
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';

const webUrl = ref('');

onLoad((opts) => {
  if (opts.url) {
    webUrl.value = decodeURIComponent(opts.url);
  } else if (opts.type === 'agreement') {
    webUrl.value = '/static/html/agreement.html';
  } else if (opts.type === 'privacy') {
    webUrl.value = '/static/html/privacy.html';
  }
});
</script>
