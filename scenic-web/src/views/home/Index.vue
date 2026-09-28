<template>
  <div class="home-page">
    <!-- ====== Banner 轮播区 ====== -->
    <div
      class="banner"
      :class="{ 'banner-fallback': !hasBannerImages }"
      :style="currentBannerStyle"
    >
      <div class="banner-overlay" :class="{ 'banner-overlay--transparent': !hasBannerImages }">
        <div class="banner-content">
          <h1 class="banner-title">
            <span class="banner-title-line">探索</span>
            <span class="banner-title-name">{{ scenicStore.scenicName }}</span>
          </h1>
          <p class="banner-desc">{{ scenicStore.scenicDescription }}</p>
          <div class="banner-actions">
            <button class="banner-btn primary" @click="$router.push('/tickets')">
              立即购票
            </button>
            <button class="banner-btn outline" @click="$router.push('/ai-assistant')">
              AI 智能助手
            </button>
          </div>
        </div>
        <div class="slide-dots" v-if="scenicStore.bannerImages.length > 1">
          <span v-for="(_, i) in scenicStore.bannerImages" :key="i" :class="{ active: i === currentSlide }" @click="currentSlide = i"></span>
        </div>
        <div class="scroll-hint" @click="scrollToContent">
          <span>向下探索</span>
          <div class="scroll-arrow"></div>
        </div>
      </div>
    </div>

    <div class="content-wrapper" ref="contentRef">
      <!-- ====== 关键数据 ====== -->
      <div class="section section-stats" v-observe>
        <div class="stat-cards">
          <div class="stat-card" v-for="(stat, i) in stats" :key="i" :style="{ animationDelay: `${i * 0.1}s` }">
            <span class="stat-num">{{ stat.value }}</span>
            <span class="stat-label">{{ stat.label }}</span>
          </div>
        </div>
      </div>

      <!-- ====== 实时拥挤度 & 公告 ====== -->
      <div class="section section-crowd" v-observe>
        <div class="section-header">
          <span class="section-tag">LIVE</span>
          <h2 class="section-title">实时拥挤度 && 公告</h2>
          <p class="section-subtitle">合理规划游览时间，注意事项</p>
        </div>
        <div class="crowd-announce-row">
          <!-- 左侧：拥挤度热力图 -->
          <div class="crowd-card">
            <div class="crowd-header">
              <div class="crowd-gauge">
                <div class="crowd-stat-group">
                  <div class="crowd-stat">
                    <span class="crowd-num">{{ crowdData.currentInPark || 0 }} 人</span>
                    <span class="crowd-label">当前在园</span>
                  </div>
                  <div class="crowd-stat" v-if="crowdData.maxCapacity">
                    <span class="crowd-num">{{ crowdData.maxCapacity }} 人</span>
                    <span class="crowd-label">最大承载</span>
                  </div>
                </div>
              </div>
              <div class="crowd-suggestion">
                <el-icon :size="18"><InfoFilled /></el-icon>
                <span>{{ crowdData.suggestion || '数据加载中...' }}</span>
              </div>
            </div>
            <div class="crowd-heatmap-title">
              <span class="heatmap-label">时段拥挤度</span>
              <div class="heatmap-legend-inline">
                <span class="legend-dot level-1"></span>空闲
                <span class="legend-dot level-2"></span>适中
                <span class="legend-dot level-3"></span>较挤
                <span class="legend-dot level-4"></span>拥挤
              </div>
            </div>
            <div class="crowd-hours" v-if="crowdData.hourlyData?.length">
              <div class="hour-bar" v-for="item in crowdData.hourlyData" :key="item.hour"
                   :class="'level-' + item.level" :title="`${item.hour}:00 ${item.label}(${item.count}人)`">
                <span class="hour-count">{{ item.count }}</span>
                <div class="hour-bar-fill" :style="{ height: ((item.count / maxHourlyCount) * 100) + '%' }"></div>
                <span class="hour-label">{{ item.hour }}:00</span>
              </div>
            </div>
            <div class="crowd-hours-empty" v-else>
              <span>暂无时段数据</span>
            </div>
          </div>
          <!-- 右侧：公告列表 -->
          <div class="announce-panel">
            <div class="announce-panel-header">
              <span class="announce-panel-title">景区公告</span>
              <div class="announce-pager" v-if="announcements.length">
                <button class="pager-btn" :disabled="announcePage <= 0" @click="announcePage--">‹</button>
                <span class="pager-info">{{ announcePage + 1 }}/{{ totalAnnouncePages }}</span>
                <button class="pager-btn" :disabled="announcePage >= totalAnnouncePages - 1" @click="announcePage++">›</button>
              </div>
            </div>
            <div class="announce-list" v-if="pagedAnnouncements.length">
              <div v-for="a in pagedAnnouncements" :key="a.id" class="announce-item"
                   :class="'announce-type-' + (a.type || 1)" @click="handleAnnounceClick(a)">
                <div class="announce-item-head">
                  <span class="announce-dot"></span>
                  <span class="announce-tag">{{ typeLabel(a.type) }}</span>
                  <span class="announce-time">{{ formatDate(a.createTime) }}</span>
                </div>
                <span class="announce-text">{{ a.title }}</span>
              </div>
            </div>
            <div class="announce-empty" v-else>
              <el-icon :size="36"><InfoFilled /></el-icon>
              <span>暂无公告</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ====== 票种展示 ====== -->
      <div class="section" v-observe>
        <div class="section-header">
          <span class="section-tag">TICKETS</span>
          <h2 class="section-title">票种价格</h2>
          <p class="section-subtitle">选择最适合您的入园方式</p>
        </div>
        <div class="tickets-grid">
          <div class="ticket-card" v-for="(ticket, i) in ticketTypes" :key="ticket.id"
               :style="{ animationDelay: `${i * 0.1}s` }">
            <h3 class="ticket-name">{{ ticket.name }}</h3>
            <div class="ticket-price">
              <span class="price-symbol">¥</span>
              <span class="price-num">{{ ticket.price }}</span>
            </div>
            <p class="ticket-desc">{{ ticket.description }}</p>
            <button class="ticket-btn" @click="ticket.isGroup ? $router.push('/group-purchase') : $router.push('/tickets')">
              立即购买
            </button>
          </div>
        </div>
      </div>

      <!-- ====== 景区介绍 ====== -->
      <div class="section" v-observe>
        <div class="section-header">
          <span class="section-tag">ABOUT</span>
          <h2 class="section-title">景区服务</h2>
          <p class="section-subtitle">智慧科技，让每一次旅行更美好</p>
        </div>
        <div class="info-grid">
          <div class="info-card" v-for="(item, i) in infoCards" :key="item.title" :style="{ animationDelay: `${i * 0.1}s` }">
            <div class="info-icon-wrap" :style="{ background: `${item.color}15` }">
              <el-icon :size="28" :color="item.color"><component :is="item.icon" /></el-icon>
            </div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.desc }}</p>
          </div>
        </div>
      </div>

      <!-- ====== 景区景点 ====== -->
      <div class="section" v-if="spots.length" v-observe>
        <div class="section-header">
          <span class="section-tag">SPOTS</span>
          <h2 class="section-title">景区景点</h2>
          <p class="section-subtitle">发现每一个令人心动的角落</p>
        </div>
        <div class="spot-grid">
          <div class="spot-card" v-for="(spot, i) in spots" :key="spot.id"
               :style="{ animationDelay: `${i * 0.08}s` }" @click="handleSpotClick(spot)">
            <div class="spot-card-img" v-if="spot.imageUrl">
              <img :src="spot.imageUrl" :alt="spot.name" />
              <div class="spot-card-overlay">
                <span class="spot-view-btn">查看详情</span>
              </div>
            </div>
            <div class="spot-card-img spot-card-img-fallback" v-else>
              <el-icon :size="40" color="#ccc"><Location /></el-icon>
            </div>
            <div class="spot-card-body">
              <h3>{{ spot.name }}</h3>
              <p>{{ spot.description }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- ====== 设施展览 ====== -->
      <div class="section" v-if="facilities.length" v-observe>
        <div class="section-header">
          <span class="section-tag">FACILITIES</span>
          <h2 class="section-title">设施服务</h2>
          <p class="section-subtitle">完善的配套设施，畅享无忧旅程</p>
        </div>
        <div class="facility-grid">
          <div class="facility-card" v-for="(facility, i) in facilities" :key="facility.id"
               :style="{ animationDelay: `${i * 0.08}s` }" @click="handleFacilityClick(facility)">
            <div class="facility-card-img" v-if="facility.imageUrl">
              <img :src="facility.imageUrl" :alt="facility.name" />
              <div class="facility-card-overlay">
                <span class="facility-view-btn">查看详情</span>
              </div>
            </div>
            <div class="facility-card-img facility-card-img-fallback" v-else>
              <el-icon :size="40" color="#ccc"><Service /></el-icon>
            </div>
            <div class="facility-card-body">
              <div class="facility-title-row">
                <h3>{{ facility.name }}</h3>
                <span class="facility-type-chip" :style="{ background: `${facilityTypeStyle(facility.type).color}18`, color: facilityTypeStyle(facility.type).color }">
                  {{ facilityTypeStyle(facility.type).label }}
                </span>
              </div>
              <p>{{ facility.description }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- ====== CTA 行动号召 ====== -->
      <div class="section section-cta" v-observe>
        <div class="cta-card" :style="{ background: `linear-gradient(135deg, ${scenicStore.primaryColor}, ${scenicStore.primaryColor}dd)` }">
          <h2>准备好开启您的旅程了吗？</h2>
          <p>现在购票，即刻体验智慧景区带来的便捷与美好</p>
          <button class="banner-btn primary" @click="$router.push('/tickets')">
            立即购票
          </button>
        </div>
      </div>
    </div>

    <!-- ====== 景点详情弹窗 ====== -->
    <Teleport to="body">
      <transition name="modal-fade">
        <div class="modal-overlay" v-if="spotDialogVisible" @click.self="spotDialogVisible = false">
          <div class="modal-dialog">
            <button class="modal-close" @click="spotDialogVisible = false">✕</button>
            <div class="modal-hero" v-if="currentSpot?.imageUrl">
              <img :src="currentSpot.imageUrl" :alt="currentSpot.name" />
              <div class="modal-hero-mask">
                <h2>{{ currentSpot.name }}</h2>
              </div>
            </div>
            <div class="modal-hero modal-hero-fallback" v-else>
              <h2>{{ currentSpot?.name }}</h2>
            </div>
            <div class="modal-body" v-if="currentSpot">
              <div class="modal-section">
                <span class="modal-section-icon">📖</span>
                <span class="modal-section-label">景点简介</span>
              </div>
              <p class="modal-desc">{{ currentSpot.description || '暂无详细介绍' }}</p>
              <div class="modal-meta" v-if="currentSpot.longitude || currentSpot.latitude">
                <div class="modal-meta-item">
                  <el-icon :size="16" color="var(--accent)"><Location /></el-icon>
                  <div>
                    <span class="meta-label">经度</span>
                    <span class="meta-value">{{ currentSpot.longitude || '-' }}</span>
                  </div>
                </div>
                <div class="modal-meta-item">
                  <el-icon :size="16" color="#67C23A"><Location /></el-icon>
                  <div>
                    <span class="meta-label">纬度</span>
                    <span class="meta-value">{{ currentSpot.latitude || '-' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>

    <!-- ====== 设施详情弹窗 ====== -->
    <Teleport to="body">
      <transition name="modal-fade">
        <div class="modal-overlay" v-if="facilityDialogVisible" @click.self="facilityDialogVisible = false">
          <div class="modal-dialog">
            <button class="modal-close" @click="facilityDialogVisible = false">✕</button>
            <div class="modal-hero" v-if="currentFacility?.imageUrl">
              <img :src="currentFacility.imageUrl" :alt="currentFacility.name" />
              <div class="modal-hero-mask">
                <h2>{{ currentFacility.name }}</h2>
              </div>
            </div>
            <div class="modal-hero modal-hero-fallback" v-else>
              <h2>{{ currentFacility?.name }}</h2>
            </div>
            <div class="modal-body" v-if="currentFacility">
              <div class="modal-section">
                <span class="modal-section-icon">{{ facilityTypeStyle(currentFacility.type).emoji }}</span>
                <span class="modal-section-label">{{ facilityTypeStyle(currentFacility.type).label }}</span>
              </div>
              <p class="modal-desc">{{ currentFacility.description || '暂无详细介绍' }}</p>
              <div class="modal-meta" v-if="currentFacility.longitude || currentFacility.latitude">
                <div class="modal-meta-item">
                  <el-icon :size="16" color="var(--accent)"><Location /></el-icon>
                  <div>
                    <span class="meta-label">经度</span>
                    <span class="meta-value">{{ currentFacility.longitude || '-' }}</span>
                  </div>
                </div>
                <div class="modal-meta-item">
                  <el-icon :size="16" color="#67C23A"><Location /></el-icon>
                  <div>
                    <span class="meta-label">纬度</span>
                    <span class="meta-value">{{ currentFacility.latitude || '-' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>

    <!-- ====== 公告详情弹窗 ====== -->
    <Teleport to="body">
      <transition name="modal-fade">
        <div class="modal-overlay" v-if="announceDialogVisible" @click.self="announceDialogVisible = false">
          <div class="modal-dialog">
            <button class="modal-close" @click="announceDialogVisible = false">✕</button>
            <div class="modal-hero announce-hero" :class="'announce-hero-type-' + (currentAnnounce?.type || 1)">
              <span class="announce-hero-badge">{{ typeLabel(currentAnnounce?.type) }}</span>
              <h2>{{ currentAnnounce?.title }}</h2>
            </div>
            <div class="modal-body" v-if="currentAnnounce">
              <div class="modal-section">
                <span class="modal-section-icon">📢</span>
                <span class="modal-section-label">公告内容</span>
              </div>
              <p class="modal-desc">{{ currentAnnounce.content || '暂无详细内容' }}</p>
              <div class="modal-meta" v-if="currentAnnounce.createTime">
                <div class="modal-meta-item">
                  <el-icon :size="16" color="#409EFF"><Clock /></el-icon>
                  <div>
                    <span class="meta-label">发布时间</span>
                    <span class="meta-value">{{ currentAnnounce.createTime }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useScenicStore } from '../../stores/scenic'
import { getTicketTypes, getSpots, getFacilities, getAiStats, getCrowdHeatmap, getPublicAnnouncements } from '../../api'
import { Clock, Sunny, Service, Location, InfoFilled } from '@element-plus/icons-vue'

defineOptions({ name: 'Home' })

const scenicStore = useScenicStore()
const ticketTypes = ref([])
const spots = ref([])
const facilities = ref([])
const aiStats = ref({ todayCount: 0, totalCount: 0 })
const crowdData = ref({})
const announcements = ref([])
const announcePage = ref(0)
const announcePageSize = 5
const totalAnnouncePages = computed(() => Math.max(1, Math.ceil(announcements.value.length / announcePageSize)))
const pagedAnnouncements = computed(() => {
  const start = announcePage.value * announcePageSize
  return announcements.value.slice(start, start + announcePageSize)
})
const currentSlide = ref(0)
const spotDialogVisible = ref(false)
const currentSpot = ref(null)
const facilityDialogVisible = ref(false)
const currentFacility = ref(null)
const announceDialogVisible = ref(false)
const currentAnnounce = ref(null)
const contentRef = ref(null)
let slideTimer = null

const maxHourlyCount = computed(() => {
  const data = crowdData.value.hourlyData
  if (!data?.length) return 1
  return Math.max(...data.map(d => d.count || 0), 1)
})

const hasBannerImages = computed(() => scenicStore.bannerImages.length > 0)

const currentBannerStyle = computed(() => {
  if (hasBannerImages.value) {
    return { backgroundImage: `url(${scenicStore.bannerImages[currentSlide.value]})` }
  }
  return {
    background: `linear-gradient(135deg, ${scenicStore.primaryColor} 0%, ${scenicStore.primaryColor}dd 100%)`
  }
})

const infoCards = computed(() => [
  { title: '开放时间', desc: scenicStore.scenicOpenTime && scenicStore.scenicCloseTime ? `每日 ${scenicStore.scenicOpenTime.substring(0, 5)} - ${scenicStore.scenicCloseTime.substring(0, 5)}` : '请查看景区公告', icon: Clock, color: '#409EFF' },
  { title: '最佳季节', desc: '春季和秋季是最佳游览季节，气候宜人风光正好', icon: Sunny, color: '#67C23A' },
  { title: '优质服务', desc: 'AI智能助手全程陪伴，刷脸入园快速通行', icon: Service, color: '#E6A23C' },
])

const stats = computed(() => [
  { value: spots.value.length || '多', label: '个景点' },
  { value: ticketTypes.value.length || '多', label: '种票型' },
  { value: facilities.value.length || '多', label: '项设施' },
  { value: aiStats.value.totalCount != null ? `${aiStats.value.totalCount}次` : '--', label: 'AI服务' },
])

function handleSpotClick(spot) { currentSpot.value = spot; spotDialogVisible.value = true }
function handleFacilityClick(facility) { currentFacility.value = facility; facilityDialogVisible.value = true }
function handleAnnounceClick(a) { currentAnnounce.value = a; announceDialogVisible.value = true }

function scrollToContent() {
  contentRef.value?.scrollIntoView({ behavior: 'smooth' })
}

function facilityTypeStyle(type) {
  const map = {
    1: { label: '卫生间', emoji: '🚻', color: '#409EFF' },
    2: { label: '餐饮', emoji: '🍽️', color: '#F56C6C' },
    3: { label: '停车场', emoji: '🅿️', color: '#67C23A' },
    4: { label: '医疗', emoji: '🏥', color: '#E6A23C' },
    5: { label: '游客中心', emoji: 'ℹ️', color: '#909399' },
  }
  return map[type] || { label: '其他', emoji: '📍', color: '#909399' }
}

function startSlide() {
  if (scenicStore.bannerImages.length <= 1) return
  slideTimer = setInterval(() => {
    currentSlide.value = (currentSlide.value + 1) % scenicStore.bannerImages.length
  }, 5000)
}

function typeLabel(type) {
  const map = { 1: '一般', 2: '紧急', 3: '活动' }
  return map[type] || '公告'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const m = (d.getMonth() + 1).toString().padStart(2, '0')
  const day = d.getDate().toString().padStart(2, '0')
  return `${m}-${day}`
}

// 滚动入场动画
function setupObserver() {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible')
        observer.unobserve(entry.target)
      }
    })
  }, { threshold: 0.1, rootMargin: '0px 0px -40px 0px' })
  document.querySelectorAll('[v-observe]').forEach(el => observer.observe(el))
}

onMounted(async () => {
  try {
    const [tickets, spotsData, facilitiesData, aiData, crowd, annData] = await Promise.all([
      getTicketTypes(), getSpots(), getFacilities(), getAiStats(),
      getCrowdHeatmap(), getPublicAnnouncements(),
    ])
    ticketTypes.value = tickets
    spots.value = spotsData
    facilities.value = facilitiesData
    if (aiData) { aiStats.value = aiData }
    if (crowd) { crowdData.value = crowd }
    if (annData) { announcements.value = annData; announcePage.value = 0 }
  } catch (e) { console.error('加载数据失败', e) }
  startSlide()
  setTimeout(setupObserver, 200)
})

onBeforeUnmount(() => { if (slideTimer) clearInterval(slideTimer) })
</script>

<style scoped>
/* ====== CSS Variables ====== */
:root {
  --accent: #3b82f6;
  --radius: 16px;
  --shadow-sm: 0 1px 3px rgba(0,0,0,0.06);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.08);
  --shadow-lg: 0 12px 40px rgba(0,0,0,0.12);
  --transition: 0.35s cubic-bezier(0.22, 0.61, 0.36, 1);
}
.home-page { margin: -24px; }

/* ====== Banner ====== */
.banner {
  background-size: cover; background-position: center;
  width: 100%;
  min-height: 720px;
  border-radius: 0 0 32px 32px;
  overflow: hidden;
  position: relative;
  transition: background-image 1s ease;
}
.banner-overlay {
  position: absolute; inset: 0;
  background: linear-gradient(180deg, rgba(0,0,0,0.25) 0%, rgba(0,0,0,0.5) 100%);
  display: flex; flex-direction: column;
  align-items: center; justify-content: flex-start;
  padding: 56px 32px 28px;
}
.banner-overlay--transparent { background: transparent; }
.banner-fallback { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 420px; }
.banner-content { text-align: center; max-width: 900px; }
.banner-title { margin-bottom: 20px; }
.banner-title-line {
  display: block; color: rgba(255,255,255,0.7);
  font-size: 16px; font-weight: 400; letter-spacing: 4px;
  text-transform: uppercase; margin-bottom: 8px;
  animation: fadeInUp 0.8s ease both;
}
.banner-title-name {
  display: block; color: #fff;
  font-size: clamp(34px, 5.5vw, 54px); font-weight: 800; letter-spacing: 2px;
  text-shadow: 0 4px 20px rgba(0,0,0,0.3);
  animation: fadeInUp 0.8s 0.15s ease both;
}
.banner-desc {
  color: rgba(255,255,255,0.92); font-size: 17px; line-height: 1.9;
  max-width: 700px; margin: 0 auto 28px;
  letter-spacing: 0.4px;
  text-shadow: 0 1px 8px rgba(0,0,0,0.3);
  white-space: pre-line; word-break: break-word;
  text-align: justify;
  animation: fadeInUp 0.8s 0.3s ease both;
}
.banner-actions { display: flex; gap: 20px; justify-content: center; flex-wrap: wrap; margin-bottom: 36px; animation: fadeInUp 0.8s 0.45s ease both; }
.banner-btn {
  position: relative; border: none; border-radius: 50px;
  padding: 13px 36px; font-size: 16px; font-weight: 600;
  cursor: pointer; transition: all var(--transition); overflow: hidden;
}
.banner-btn.primary {
  background: #fff; color: #1a1a2e;
  box-shadow: 0 4px 20px rgba(255,255,255,0.25);
}
.banner-btn.primary:hover { transform: translateY(-2px); box-shadow: 0 8px 30px rgba(255,255,255,0.35); }
.banner-btn.outline {
  background: transparent; color: #fff;
  border: 2px solid rgba(255,255,255,0.5);
  backdrop-filter: blur(4px);
}
.banner-btn.outline:hover { border-color: #fff; background: rgba(255,255,255,0.1); }
.btn-arrow { margin-left: 6px; transition: transform 0.3s; display: inline-block; }
.banner-btn:hover .btn-arrow { transform: translateX(4px); }

/* 轮播指示点 */
.slide-dots { display: flex; gap: 14px; justify-content: center; margin-bottom: 20px; }
.slide-dots span {
  width: 10px; height: 10px; border-radius: 50%;
  background: rgba(255,255,255,0.4); cursor: pointer;
  transition: all 0.4s;
}
.slide-dots span.active { background: #fff; box-shadow: 0 0 12px rgba(255,255,255,0.5); width: 28px; border-radius: 5px; }

/* 滚动提示 */
.scroll-hint {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  cursor: pointer; opacity: 0.7; transition: opacity 0.3s;
}
.scroll-hint:hover { opacity: 1; }
.scroll-hint span { color: #fff; font-size: 12px; letter-spacing: 3px; }
.scroll-arrow {
  width: 18px; height: 18px; border-right: 2px solid #fff; border-bottom: 2px solid #fff;
  transform: rotate(45deg); animation: bounce 2s infinite;
}

/* ====== 内容区 ====== */
.content-wrapper { background: transparent; padding: 0 24px 80px; }
.section { max-width: 1200px; margin: 0 auto; padding-top: 80px; }

/* 入场动画 */
[v-observe] { opacity: 0; transform: translateY(30px); transition: opacity 0.7s ease, transform 0.7s cubic-bezier(0.22, 0.61, 0.36, 1); }
[v-observe].visible { opacity: 1; transform: translateY(0); }

/* ====== 分区标题 ====== */
.section-header { text-align: center; margin-bottom: 48px; }
.section-tag {
  display: inline-block; font-size: 12px; font-weight: 700; letter-spacing: 3px;
  color: v-bind('scenicStore.primaryColor'); margin-bottom: 12px;
}
.section-title {
  font-size: clamp(28px, 4vw, 38px); font-weight: 800; color: #1a1a2e; margin: 0 0 12px;
  letter-spacing: -0.5px;
}
.section-subtitle { color: #94a3b8; font-size: 16px; margin: 0; }

/* ====== 数据统计 ====== */
.section-stats { padding-top: 60px; }
.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px; }
.stat-card {
  background: #fff; border-radius: var(--radius);
  padding: 32px 20px; text-align: center;
  box-shadow: var(--shadow-sm); transition: all var(--transition);
  opacity: 0; animation: fadeInUp 0.6s ease forwards;
}
.stat-card:hover { transform: translateY(-4px); box-shadow: var(--shadow-md); }
.stat-num { display: block; font-size: 36px; font-weight: 800; color: #1a1a2e; margin-bottom: 6px; }
.stat-label { font-size: 14px; color: #94a3b8; }

/* ====== 实时拥挤度 & 公告并行布局 ====== */
.section-crowd { padding-top: 60px; }
.crowd-announce-row {
  display: flex; gap: 20px; align-items: flex-start;
}
.crowd-card {
  flex: 1; background: #fff; border-radius: 20px; padding: 28px;
  box-shadow: var(--shadow-sm);
}
.announce-panel {
  flex: 1; background: #fff; border-radius: 20px; padding: 20px;
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  min-width: 0;
}
.crowd-header { display: flex; align-items: center; justify-content: space-between; gap: 24px; flex-wrap: wrap; }
.crowd-gauge { display: flex; align-items: center; gap: 20px; }
.crowd-level {
  width: 90px; height: 90px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.5s;
}
.crowd-level.level-1 { background: #e8f5e9; }
.crowd-level.level-2 { background: #fff3e0; }
.crowd-level.level-3 { background: #ffe0b2; }
.crowd-level.level-4 { background: #ffcdd2; }
.crowd-level-text {
  font-size: 16px; font-weight: 800;
}
.crowd-level.level-1 .crowd-level-text { color: #2e7d32; }
.crowd-level.level-2 .crowd-level-text { color: #e65100; }
.crowd-level.level-3 .crowd-level-text { color: #bf360c; }
.crowd-level.level-4 .crowd-level-text { color: #c62828; }
.crowd-stat { text-align: center; }
.crowd-num { display: block; font-size: 24px; font-weight: 800; color: #1a1a2e; }
.crowd-unit { font-size: 13px; color: #94a3b8; margin-left: 4px; }
.crowd-label { display: block; font-size: 11px; color: #94a3b8; margin-top: 2px; }
.crowd-suggestion {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 16px; background: #f8fafc; border-radius: 12px;
  font-size: 13px; color: #64748b; flex: 1; min-width: 200px;
}
.crowd-suggestion .el-icon { color: #409EFF; flex-shrink: 0; }

.crowd-stat-group {
  display: flex; gap: 16px;
}
.crowd-heatmap-title {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 24px; margin-bottom: 12px;
  padding-bottom: 10px; border-bottom: 2px solid #f1f5f9;
}
.heatmap-label {
  font-size: 14px; font-weight: 700; color: #1a1a2e;
}
.heatmap-legend-inline {
  display: flex; align-items: center; gap: 4px;
  font-size: 11px; color: #94a3b8;
}
.legend-dot {
  width: 8px; height: 8px; border-radius: 2px; flex-shrink: 0;
  margin-right: 2px;
}
.legend-dot.level-1 { background: #a5d6a7; }
.legend-dot.level-2 { background: #ffcc80; }
.legend-dot.level-3 { background: #ff8a65; }
.legend-dot.level-4 { background: #ef5350; }

.crowd-hours {
  display: flex; gap: 6px; align-items: flex-end;
  height: 150px; padding-top: 4px;
}
.crowd-hours-empty {
  display: flex; align-items: center; justify-content: center;
  height: 100px; color: #c0c4cc; font-size: 13px;
}
.hour-bar {
  flex: 1; display: flex; flex-direction: column; align-items: center;
  justify-content: flex-end;
  height: 100%; position: relative;
}
.hour-bar-fill {
  width: 100%; max-width: 40px; border-radius: 6px 6px 0 0;
  transition: height 0.6s cubic-bezier(0.22, 0.61, 0.36, 1);
}
.hour-bar.level-1 .hour-bar-fill { background: #a5d6a7; }
.hour-bar.level-2 .hour-bar-fill { background: #ffcc80; }
.hour-bar.level-3 .hour-bar-fill { background: #ff8a65; }
.hour-bar.level-4 .hour-bar-fill { background: #ef5350; }
.hour-count {
  font-size: 12px; color: #1a1a2e; font-weight: 700;
  margin-bottom: 2px; white-space: nowrap;
  line-height: 1;
}
.hour-label {
  font-size: 11px; color: #94a3b8; font-weight: 600; margin-top: 4px;
  white-space: nowrap;
}

/* ====== 公告 ====== */
.announce-panel-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 14px; padding-bottom: 12px;
  border-bottom: 2px solid #f1f5f9;
}
.announce-panel-title {
  font-size: 16px; font-weight: 700; color: #1a1a2e;
}
.announce-pager {
  display: flex; align-items: center; gap: 6px;
}
.pager-btn {
  width: 24px; height: 24px; border-radius: 6px;
  border: 1px solid #e2e8f0; background: #fff;
  color: #475569; font-size: 14px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.pager-btn:hover:not(:disabled) { background: #409EFF; border-color: #409EFF; color: #fff; }
.pager-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.pager-info {
  font-size: 12px; color: #64748b; min-width: 40px; text-align: center;
}
.announce-empty {
  display: flex; flex-direction: column; align-items: center; gap: 12px;
  padding: 32px 0; color: #c0c4cc; font-size: 13px;
}
.announce-list {
  display: flex; flex-direction: column; gap: 10px;
}
.announce-item {
  padding: 12px 14px; border-radius: 12px;
  transition: all 0.25s; cursor: pointer;
  border: 1px solid transparent;
}
.announce-item:hover { transform: translateX(4px); border-color: #e2e8f0; }
.announce-item.announce-type-1 { background: #f0f7ff; border-left: 3px solid #409EFF; }
.announce-item.announce-type-2 { background: #fff0f0; border-left: 3px solid #F56C6C; }
.announce-item.announce-type-3 { background: #fff8e1; border-left: 3px solid #E6A23C; }
.announce-item-head {
  display: flex; align-items: center; gap: 6px; margin-bottom: 6px;
}
.announce-dot {
  width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0;
}
.announce-type-1 .announce-dot { background: #409EFF; }
.announce-type-2 .announce-dot { background: #F56C6C; animation: pulse 1.5s infinite; }
.announce-type-3 .announce-dot { background: #E6A23C; }
.announce-tag {
  font-size: 11px; padding: 1px 8px; border-radius: 4px; font-weight: 600;
}
.announce-type-1 .announce-tag { color: #409EFF; background: #d9ecff; }
.announce-type-2 .announce-tag { color: #F56C6C; background: #fde2e2; }
.announce-type-3 .announce-tag { color: #E6A23C; background: #faecd8; }
.announce-time {
  font-size: 11px; color: #94a3b8; margin-left: auto;
}
.announce-text {
  display: block; color: #334155; font-size: 13.5px; font-weight: 500;
  line-height: 1.5; overflow: hidden; text-overflow: ellipsis;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
}
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.3; } }

/* ====== 票种卡片 ====== */
.tickets-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px;
}
.ticket-card {
  background: #fff; border-radius: 20px; padding: 32px 24px;
  text-align: center; position: relative;
  box-shadow: var(--shadow-sm); transition: all var(--transition);
  border: 2px solid transparent;
  opacity: 0; animation: fadeInUp 0.6s ease forwards;
}
.ticket-card:hover { transform: translateY(-6px); box-shadow: var(--shadow-lg); }
.ticket-name { font-size: 20px; font-weight: 700; color: #1a1a2e; margin: 0 0 16px; }
.ticket-price { margin-bottom: 16px; }
.price-symbol { font-size: 24px; color: #E6A23C; font-weight: 600; vertical-align: top; }
.price-num { font-size: 44px; color: #E6A23C; font-weight: 800; line-height: 1; }
.ticket-desc { color: #94a3b8; font-size: 14px; line-height: 1.6; margin: 0 0 24px; }
.ticket-btn {
  width: 100%; border: none; border-radius: 12px; padding: 12px;
  background: v-bind('scenicStore.primaryColor'); color: #fff;
  font-size: 15px; font-weight: 600; cursor: pointer;
  transition: all var(--transition);
}
.ticket-btn:hover { filter: brightness(1.1); transform: scale(1.02); }

/* ====== 景区介绍 ====== */
.info-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 28px; }
.info-card {
  background: #fff; border-radius: 20px; padding: 36px 28px;
  text-align: center; box-shadow: var(--shadow-sm);
  transition: all var(--transition);
  opacity: 0; animation: fadeInUp 0.6s ease forwards;
}
.info-card:hover { transform: translateY(-6px); box-shadow: var(--shadow-lg); }
.info-icon-wrap {
  width: 64px; height: 64px; border-radius: 18px;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 20px;
}
.info-card h3 { font-size: 18px; font-weight: 700; color: #1a1a2e; margin: 0 0 10px; }
.info-card p { color: #64748b; font-size: 14px; line-height: 1.7; margin: 0; }

/* ====== 景点/设施网格 ====== */
.spot-grid, .facility-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 24px; }
.spot-card {
  background: #fff; border-radius: 20px; overflow: hidden;
  box-shadow: var(--shadow-sm); cursor: pointer;
  transition: all var(--transition);
  opacity: 0; animation: fadeInUp 0.6s ease forwards;
}
.spot-card:hover { transform: translateY(-6px); box-shadow: var(--shadow-lg); }
.spot-card-img {
  position: relative; height: 200px; overflow: hidden;
  background: #f1f5f9; display: flex; align-items: center; justify-content: center;
}
.spot-card-img img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.6s; }
.spot-card:hover .spot-card-img img { transform: scale(1.08); }
.spot-card-overlay {
  position: absolute; inset: 0;
  background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.35s;
}
.spot-card:hover .spot-card-overlay { opacity: 1; }
.spot-view-btn {
  color: #fff; border: 2px solid #fff; border-radius: 30px;
  padding: 8px 24px; font-size: 14px; font-weight: 500;
  backdrop-filter: blur(4px);
}
.spot-card-body { padding: 20px 24px; }
.spot-card-body h3 { font-size: 17px; font-weight: 700; color: #1a1a2e; margin: 0 0 8px; }
.spot-card-body p { color: #64748b; font-size: 13px; line-height: 1.6; margin: 0; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

/* ====== 设施卡片（与景点统一） ====== */
.facility-card {
  background: #fff; border-radius: 20px; overflow: hidden;
  box-shadow: var(--shadow-sm); cursor: pointer;
  transition: all var(--transition);
  opacity: 0; animation: fadeInUp 0.6s ease forwards;
}
.facility-card:hover { transform: translateY(-6px); box-shadow: var(--shadow-lg); }
.facility-card-img {
  position: relative; height: 200px; overflow: hidden;
  background: #f1f5f9; display: flex; align-items: center; justify-content: center;
}
.facility-card-img img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.6s; }
.facility-card:hover .facility-card-img img { transform: scale(1.08); }
.facility-card-overlay {
  position: absolute; inset: 0;
  background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.35s;
}
.facility-card:hover .facility-card-overlay { opacity: 1; }
.facility-view-btn {
  color: #fff; border: 2px solid #fff; border-radius: 30px;
  padding: 8px 24px; font-size: 14px; font-weight: 500;
  backdrop-filter: blur(4px);
}
.facility-card-body { padding: 18px 22px; }
.facility-title-row {
  display: flex; align-items: center; justify-content: space-between;
  gap: 8px; margin-bottom: 8px;
}
.facility-card-body h3 {
  font-size: 16px; font-weight: 700; color: #1a1a2e;
  margin: 0; flex: 1; min-width: 0;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.facility-type-chip {
  flex-shrink: 0; font-size: 11px; font-weight: 600;
  padding: 3px 10px; border-radius: 10px;
}
.facility-card-body p {
  color: #64748b; font-size: 13px; line-height: 1.6; margin: 0;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}

/* ====== CTA ====== */
.section-cta { padding-bottom: 40px; }
.cta-card {
  border-radius: 24px; padding: 60px 40px; text-align: center;
  color: #fff; box-shadow: 0 12px 40px rgba(0,0,0,0.15);
}
.cta-card h2 { font-size: 32px; font-weight: 800; margin: 0 0 12px; }
.cta-card p { font-size: 16px; opacity: 0.85; margin: 0 0 32px; }

/* ====== 详情弹窗 ====== */
.modal-fade-enter-active { animation: modalIn 0.3s ease; }
.modal-fade-leave-active { animation: modalIn 0.2s ease reverse; }
@keyframes modalIn { from { opacity: 0; } to { opacity: 1; } }
.modal-overlay {
  position: fixed; inset: 0; z-index: 2000;
  background: rgba(0,0,0,0.55); backdrop-filter: blur(6px);
  display: flex; align-items: center; justify-content: center; padding: 24px;
}
.modal-dialog {
  background: #fff; border-radius: 24px; width: 560px; max-width: 90vw;
  max-height: 85vh;
  box-shadow: 0 20px 60px rgba(0,0,0,0.25);
  animation: slideUp 0.4s cubic-bezier(0.22, 0.61, 0.36, 1);
  display: flex; flex-direction: column; position: relative;
}
@keyframes slideUp {
  from { opacity: 0; transform: translateY(40px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.modal-close {
  position: absolute; top: 14px; right: 14px; z-index: 20;
  width: 36px; height: 36px; border-radius: 50%;
  border: none; background: rgba(0,0,0,0.5); color: #fff;
  font-size: 16px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.3s; backdrop-filter: blur(4px);
}
.modal-close:hover { background: rgba(0,0,0,0.8); transform: rotate(90deg); }
.modal-hero { position: relative; width: 100%; height: 240px; overflow: hidden; flex-shrink: 0; border-radius: 24px 24px 0 0; }
.modal-hero img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.5s; }
.modal-dialog:hover .modal-hero img { transform: scale(1.04); }
.modal-hero-fallback { height: auto; background: linear-gradient(135deg, #f0f5ff, #e8f4ff); display: flex; align-items: flex-end; padding: 32px; }
.modal-hero-mask {
  position: absolute; inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.6), transparent 50%);
  display: flex; align-items: flex-end; padding: 28px;
}
.modal-hero-mask h2, .modal-hero-fallback h2 {
  margin: 0; font-size: 26px; font-weight: 800; color: #fff;
  letter-spacing: 1px; text-shadow: 0 2px 8px rgba(0,0,0,0.4);
}
.modal-hero-fallback h2 { color: #1a1a2e; text-shadow: none; }
/* 公告弹窗头部 */
.announce-hero {
  position: relative; width: 100%; height: auto;
  overflow: hidden; flex-shrink: 0; border-radius: 24px 24px 0 0;
  padding: 18px 24px;
  display: flex; flex-direction: column; justify-content: center;
}
.announce-hero-type-1 { background: linear-gradient(135deg, #409EFF, #337ecc); }
.announce-hero-type-2 { background: linear-gradient(135deg, #F56C6C, #d14343); }
.announce-hero-type-3 { background: linear-gradient(135deg, #E6A23C, #c87f2a); }
.announce-hero h2 {
  margin: 0; font-size: 22px; font-weight: 800; color: #fff;
  letter-spacing: 0.5px; text-shadow: 0 2px 8px rgba(0,0,0,0.25);
  line-height: 1.4;
}
.announce-hero-badge {
  display: inline-block; margin-bottom: 10px;
  font-size: 12px; padding: 3px 14px; border-radius: 20px;
  background: rgba(255,255,255,0.25); color: #fff;
  font-weight: 600; letter-spacing: 1px;
  backdrop-filter: blur(4px);
  width: fit-content;
}

.modal-body { padding: 24px 28px 28px; overflow-y: auto; flex: 1; min-height: 0; }
.modal-section { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; }
.modal-section-icon { font-size: 22px; }
.modal-section-label { font-size: 17px; font-weight: 700; color: #1a1a2e; }
.modal-desc { margin: 0 0 24px; font-size: 15px; color: #475569; line-height: 1.9; text-align: justify; }
.modal-meta { display: flex; gap: 16px; background: #f8fafc; border-radius: 14px; padding: 16px 20px; }
.modal-meta-item { flex: 1; display: flex; align-items: center; gap: 10px; }
.modal-meta-item > div { display: flex; flex-direction: column; }
.meta-label { font-size: 11px; color: #94a3b8; margin-bottom: 2px; }
.meta-value { font-size: 14px; color: #1a1a2e; font-weight: 600; }

/* ====== 动画关键帧 ====== */
@keyframes fadeInUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
@keyframes bounce { 0%,100% { transform: rotate(45deg) translateY(0); } 50% { transform: rotate(45deg) translateY(6px); } }

/* ====== 响应式 ====== */
@media (max-width: 900px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
  .tickets-grid { grid-template-columns: repeat(2, 1fr); }
  .info-grid, .spot-grid, .facility-grid { grid-template-columns: repeat(2, 1fr); }
  .crowd-announce-row { flex-direction: column; }
}
@media (max-width: 560px) {
  .stat-cards { grid-template-columns: 1fr 1fr; gap: 12px; }
  .tickets-grid { grid-template-columns: 1fr; }
  .info-grid, .spot-grid, .facility-grid { grid-template-columns: 1fr; }
  .banner-title-name { font-size: 32px; }
  .cta-card { padding: 40px 24px; }
  .cta-card h2 { font-size: 24px; }
}
</style>
