<template>
  <router-view />
</template>

<script setup>
import { onMounted, watch } from 'vue'
import { useScenicStore } from './stores/scenic'

const scenicStore = useScenicStore()

// 将主题色同步到 Element Plus CSS 变量，使所有 el-button type="primary" 等组件跟随主题色
function syncThemeToElementPlus(color) {
  if (!color) return
  const root = document.documentElement
  root.style.setProperty('--el-color-primary', color)
  // 生成浅色变体（light-3, light-5, light-7, light-8, light-9 用于 hover/active/disabled 等状态）
  root.style.setProperty('--el-color-primary-light-3', mixColors(color, '#ffffff', 0.3))
  root.style.setProperty('--el-color-primary-light-5', mixColors(color, '#ffffff', 0.5))
  root.style.setProperty('--el-color-primary-light-7', mixColors(color, '#ffffff', 0.7))
  root.style.setProperty('--el-color-primary-light-8', mixColors(color, '#ffffff', 0.8))
  root.style.setProperty('--el-color-primary-light-9', mixColors(color, '#ffffff', 0.9))
  root.style.setProperty('--el-color-primary-dark-2', mixColors(color, '#000000', 0.2))
}

// 简单的颜色混合函数
function mixColors(hex, mix, ratio) {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  const mr = mix === '#ffffff' ? 255 : 0
  const mg = mix === '#ffffff' ? 255 : 0
  const mb = mix === '#ffffff' ? 255 : 0
  const nr = Math.round(r * (1 - ratio) + mr * ratio)
  const ng = Math.round(g * (1 - ratio) + mg * ratio)
  const nb = Math.round(b * (1 - ratio) + mb * ratio)
  return `#${nr.toString(16).padStart(2, '0')}${ng.toString(16).padStart(2, '0')}${nb.toString(16).padStart(2, '0')}`
}

// 监听主题色变化
watch(() => scenicStore.primaryColor, (newColor) => {
  syncThemeToElementPlus(newColor)
}, { immediate: true })

onMounted(async () => {
  await scenicStore.loadScenicConfig()
  syncThemeToElementPlus(scenicStore.primaryColor)
  document.title = scenicStore.scenicName + '门票销售及入场系统'
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
  background-color: #f5f7fa;
}

#app {
  min-height: 100vh;
}

/* 修复 el-image 预览器被表格/卡片遮住的问题 */
.el-image-viewer__wrapper {
  z-index: 9999 !important;
}
</style>
