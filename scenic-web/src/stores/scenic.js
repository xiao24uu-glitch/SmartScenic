import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getScenicInfo } from '../api'

export const useScenicStore = defineStore('scenic', () => {
  const scenicName = ref('')
  const scenicAddress = ref('')
  const scenicDescription = ref('')
  const scenicOpenTime = ref('')
  const scenicCloseTime = ref('')
  const scenicMaxCapacity = ref(null)
  const scenicStatus = ref(null)
  const scenicLogoUrl = ref('')
  // 轮播图
  const bannerImages = ref([])
  // 各页面背景图
  const homeBgImage = ref('')
  const ticketsBgImage = ref('')
  const aiBgImage = ref('')
  const ordersBgImage = ref('')
  const profileBgImage = ref('')
  const loginBgImage = ref('')
  const registerBgImage = ref('')
  // 主题色（导航栏、按钮、强调色统一使用）
  const primaryColor = ref('')
  const loaded = ref(false)

  async function loadScenicConfig() {
    try {
      const data = await getScenicInfo()
      if (data) {
        scenicName.value = data.name
        scenicAddress.value = data.address
        scenicDescription.value = data.description
        scenicOpenTime.value = data.openTime
        scenicCloseTime.value = data.closeTime
        scenicMaxCapacity.value = data.maxCapacity
        scenicStatus.value = data.status
        scenicLogoUrl.value = data.logoUrl
        // 轮播图
        try {
          bannerImages.value = data.bannerImages ? JSON.parse(data.bannerImages) : []
        } catch { bannerImages.value = [] }
        // 背景图
        homeBgImage.value = data.homeBgImage
        ticketsBgImage.value = data.ticketsBgImage
        aiBgImage.value = data.aiBgImage
        ordersBgImage.value = data.ordersBgImage
        profileBgImage.value = data.profileBgImage
        loginBgImage.value = data.loginBgImage
        registerBgImage.value = data.registerBgImage
        // 主题色
        primaryColor.value = data.primaryColor
      }
    } catch (e) {
      console.warn('加载景区配置失败，等待后端启动')
    } finally {
      loaded.value = true
    }
  }

  return {
    scenicName, scenicAddress, scenicDescription,
    scenicOpenTime, scenicCloseTime, scenicMaxCapacity,
    scenicStatus, scenicLogoUrl,
    bannerImages,
    homeBgImage, ticketsBgImage, aiBgImage, ordersBgImage, profileBgImage,
    loginBgImage, registerBgImage,
    primaryColor,
    loaded, loadScenicConfig,
  }
})
