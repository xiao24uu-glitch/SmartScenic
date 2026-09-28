<template>
  <div
    class="sunrise-login"
    ref="sceneRef"
    @pointermove="onDrag"
    @pointerup="stopDrag"
    @pointerleave="stopDrag"
    @pointercancel="stopDrag"
  >
    <!-- ==================== 场景 ==================== -->
    <div class="scene">
      <!-- 星空层 -->
      <div class="stars-layer" :style="{ opacity: starsOpacity }">
        <svg viewBox="0 0 1200 500" class="stars-svg" preserveAspectRatio="xMidYMid slice">
          <circle
            v-for="s in starList"
            :key="s.id"
            :cx="s.x" :cy="s.y" :r="s.r"
            :fill="s.color"
            :style="{ animation: `star-twinkle ${s.dur}s ease-in-out ${s.delay}s infinite` }"
          />
        </svg>
      </div>

      <!-- 天空渐变 -->
      <div class="sky-layer" :style="skyStyle"></div>

      <!-- 太阳 -->
      <div
        class="sun-container"
        :style="sunContainerStyle"
        :class="{ dragging: isDragging }"
        @pointerdown.prevent="startDrag"
        @pointermove="onDrag"
        @pointerup="stopDrag"
        @pointercancel="stopDrag"
      >
        <div class="sun-aura" :style="sunAuraStyle"></div>
        <div class="sun-body" :style="sunBodyStyle">
          <div class="sun-highlight"></div>
        </div>
      </div>

      <!-- 飞鸟层 -->
      <div class="birds-layer" :style="{ opacity: birdsOpacity }">
        <svg viewBox="0 0 1000 200" class="birds-svg" preserveAspectRatio="none">
          <g v-for="b in birdList" :key="b.id" :style="b.style">
            <path :d="birdPath(b.wingSpan)" stroke="#2c1810" stroke-width="1.2" fill="none" stroke-linecap="round" stroke-linejoin="round" opacity="0.7"/>
          </g>
        </svg>
      </div>

      <!-- 远山层 -->
      <div class="mountains-layer">
        <svg viewBox="0 0 1440 320" preserveAspectRatio="none" class="mountains-svg">
          <path :d="mountainsFar" :fill="mountainFarColor" opacity="0.6"/>
          <path :d="mountainsMid" :fill="mountainMidColor" opacity="0.8"/>
          <path :d="mountainsNear" :fill="mountainNearColor"/>
        </svg>
      </div>

      <!-- 地面 -->
      <div class="ground-layer" :style="{ background: groundColor }"></div>
    </div>

    <!-- ==================== 登录表单 ==================== -->
    <transition name="form-rise">
      <div v-if="formVisible" class="form-overlay">
        <div class="login-card" :class="{ 'has-bg': hasLoginBg, 'has-logo': hasLogo }" :style="cardBgStyle">
          <!-- 头部：Logo + 景区名 -->
          <div class="card-header">
            <img v-if="scenicStore.scenicLogoUrl" :src="scenicStore.scenicLogoUrl" class="scenic-logo" alt="logo" />
            <h2 class="scenic-name" :style="{ color: hasLoginBg ? '#fff' : scenicStore.primaryColor || '#f5a623' }">
              {{ scenicStore.scenicName || '智慧景区' }}
            </h2>
            <p class="scenic-subtitle">天色破晓 · 美好旅程由此开始</p>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" clearable />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password clearable />
            </el-form-item>

            <!-- 验证码 -->
            <el-form-item prop="captcha">
              <div class="captcha-row">
                <el-input
                  v-model="form.captcha"
                  placeholder="请输入验证码"
                  :prefix-icon="Key"
                  clearable
                  maxlength="4"
                  class="captcha-input"
                />
                <div class="captcha-box" @click="refreshCaptcha" title="点击刷新验证码">
                  <canvas ref="captchaCanvas" width="120" height="40" class="captcha-canvas" />
                </div>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :loading="loading"
                @click="handleLogin"
                class="login-btn"
                :style="{ '--btn-color': scenicStore.primaryColor || '#f5a623', '--btn-color-dark': scenicStore.primaryColor || '#d4891a' }"
              >
                <span v-if="!loading">登 录</span>
              </el-button>
            </el-form-item>
          </el-form>

          <div class="login-footer">
            <span class="footer-label">还没有账号？</span>
            <el-link type="primary" @click="$router.push('/register')">立即注册</el-link>
          </div>
        </div>
      </div>
    </transition>

    <!-- ==================== 提示文字 ==================== -->
    <transition name="hint-fade">
      <div v-if="showHint" class="drag-hint">
        <span class="hint-label">向上拖动太阳，开启美好旅程</span>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useScenicStore } from '../stores/scenic'
import { ElMessage } from 'element-plus'
import { Key } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const scenicStore = useScenicStore()

// ==================== 太阳拖拽 ====================
const sceneRef = ref(null)
const isDragging = ref(false)
const hasInteracted = ref(false)

const SUN_MIN_TOP = 8
const SUN_MAX_TOP = 56
const SUN_FORM_TOP = 28
const sunTop = ref(SUN_MAX_TOP)
const dragStartY = ref(0)
const dragStartTop = ref(SUN_MAX_TOP)

const sunProgress = computed(() => {
  const val = (SUN_MAX_TOP - sunTop.value) / (SUN_MAX_TOP - SUN_MIN_TOP)
  return Math.max(0, Math.min(1, val))
})

const formVisible = computed(() => sunProgress.value > 0.55)
const showHint = computed(() => sunProgress.value < 0.15 && !hasInteracted.value)

// 登录卡片背景 & Logo
const hasLoginBg = computed(() => !!scenicStore.loginBgImage)
const hasLogo = computed(() => !!scenicStore.scenicLogoUrl)
const cardBgStyle = computed(() => {
  if (scenicStore.loginBgImage) {
    return {
      background: `linear-gradient(rgba(0,0,0,0.55), rgba(0,0,0,0.58)), url(${scenicStore.loginBgImage}) center/cover no-repeat`,
      color: '#e8e0d8',
    }
  }
  return {}
})

function startDrag(e) {
  isDragging.value = true
  dragStartY.value = e.clientY
  dragStartTop.value = sunTop.value
}

function onDrag(e) {
  if (!isDragging.value) return
  const scene = sceneRef.value
  if (!scene) return
  const rect = scene.getBoundingClientRect()
  const deltaY = e.clientY - dragStartY.value
  const deltaPercent = (deltaY / rect.height) * 100
  const newTop = dragStartTop.value + deltaPercent
  if (Math.abs(deltaPercent) > 3) hasInteracted.value = true
  sunTop.value = Math.max(SUN_MIN_TOP, Math.min(SUN_MAX_TOP, newTop))
}

function stopDrag() {
  isDragging.value = false
}

// ==================== 天空颜色计算 ====================
function lerpColor(a, b, t) {
  const ar = (a >> 16) & 0xff, ag = (a >> 8) & 0xff, ab = a & 0xff
  const br = (b >> 16) & 0xff, bg = (b >> 8) & 0xff, bb = b & 0xff
  const r = Math.round(ar + (br - ar) * t)
  const g = Math.round(ag + (bg - ag) * t)
  const blue = Math.round(ab + (bb - ab) * t)
  return `rgb(${r},${g},${blue})`
}

const skyStages = [
  { p: 0.0, colors: [0x0a0a2e, 0x131340, 0x1a1040, 0x2d1040] },
  { p: 0.25, colors: [0x1a1045, 0x2d1555, 0x4a2055, 0x6b3055] },
  { p: 0.45, colors: [0x3d2060, 0x6b3055, 0xb06050, 0xe89550] },
  { p: 0.60, colors: [0x5b3580, 0xa85578, 0xec9558, 0xfdcc58] },
  { p: 0.80, colors: [0x4a6aa0, 0x8ba0c8, 0xe8c880, 0xfdf0c8] },
  { p: 1.0, colors: [0x3a8fd4, 0x78c0e8, 0xd8f0f8, 0xf0f8ff] },
]

function getSkyColors(progress) {
  let i = 0
  while (i < skyStages.length - 1 && skyStages[i + 1].p < progress) i++
  if (i >= skyStages.length - 1) return skyStages[skyStages.length - 1].colors.map(c => lerpColor(c, c, 0))
  const s0 = skyStages[i], s1 = skyStages[i + 1]
  const range = s1.p - s0.p
  const t = range > 0 ? (progress - s0.p) / range : 0
  return s0.colors.map((c, idx) => lerpColor(c, s1.colors[idx], t))
}

const skyStyle = computed(() => {
  const colors = getSkyColors(sunProgress.value)
  return { background: `linear-gradient(180deg, ${colors[0]} 0%, ${colors[1]} 35%, ${colors[2]} 70%, ${colors[3]} 100%)` }
})

const mountainFarColor  = computed(() => lerpColor(0x1a1030, 0x3d2860, sunProgress.value))
const mountainMidColor  = computed(() => lerpColor(0x110a20, 0x2a1850, sunProgress.value))
const mountainNearColor = computed(() => lerpColor(0x0a0518, 0x1a0e38, sunProgress.value))
const groundColor       = computed(() => lerpColor(0x080410, 0x140a28, sunProgress.value))

// 太阳样式
const sunContainerStyle = computed(() => ({
  top: sunTop.value + '%',
  transition: isDragging.value ? 'none' : 'top 0.8s cubic-bezier(0.34, 1.56, 0.64, 1)',
  cursor: isDragging.value ? 'grabbing' : 'grab',
}))

const sunColor = computed(() => {
  const p = sunProgress.value
  if (p < 0.25) return lerpColor(0xff5c28, 0xff8828, p / 0.25)
  if (p < 0.55) return lerpColor(0xff8828, 0xffc040, (p - 0.25) / 0.3)
  return lerpColor(0xffc040, 0xffe888, (p - 0.55) / 0.45)
})

const sunGlowColor = computed(() => {
  const p = sunProgress.value
  if (p < 0.25) return lerpColor(0xff6630, 0xff9630, p / 0.25)
  if (p < 0.55) return lerpColor(0xff9630, 0xffcd50, (p - 0.25) / 0.3)
  return lerpColor(0xffcd50, 0xfff0a8, (p - 0.55) / 0.45)
})

const sunAuraStyle = computed(() => ({
  background: `radial-gradient(circle, ${sunGlowColor.value}88 0%, ${sunGlowColor.value}33 40%, ${sunGlowColor.value}08 70%, transparent 100%)`,
  opacity: 0.4 + sunProgress.value * 0.6,
}))

const sunBodyStyle = computed(() => ({
  background: `radial-gradient(circle at 40% 35%, ${lerpColor(0xffffff, 0xfff8e0, sunProgress.value)} 0%, ${sunColor.value} 60%, ${lerpColor(sunColor.value, sunGlowColor.value, 0.5)} 100%)`,
  boxShadow: `0 0 ${30 + sunProgress.value * 60}px ${15 + sunProgress.value * 20}px ${sunGlowColor.value}66,
              0 0 ${60 + sunProgress.value * 100}px ${30 + sunProgress.value * 30}px ${sunGlowColor.value}33`,
}))

// 星星
const starsOpacity = computed(() => Math.max(0, 1 - sunProgress.value * 2.5))
const starList = ref([])
function generateStars() {
  const stars = []
  for (let i = 0; i < 80; i++) {
    stars.push({
      id: i,
      x: Math.random() * 1200,
      y: Math.random() * 320,
      r: Math.random() * 1.6 + 0.4,
      color: Math.random() > 0.2 ? '#ffffff' : '#d4d4ff',
      dur: Math.random() * 3 + 2,
      delay: Math.random() * 5,
    })
  }
  starList.value = stars
}

// 飞鸟
const birdsOpacity = computed(() => {
  const p = sunProgress.value
  if (p < 0.5) return 0
  if (p < 0.65) return (p - 0.5) / 0.15
  return 1
})

const birdList = ref([])
function generateBirds() {
  const birds = []
  const durations = [8, 10, 12, 7, 9]
  const delays = [0, 2, 4, 1, 6]
  const ys = [15, 22, 18, 25, 12]
  for (let i = 0; i < 5; i++) {
    birds.push({
      id: i,
      wingSpan: 14 + Math.random() * 10,
      style: {
        animation: `bird-fly ${durations[i]}s linear ${delays[i]}s infinite`,
        '--bird-y': ys[i] + '%',
      },
    })
  }
  birdList.value = birds
}

function birdPath(span) {
  const hw = span / 2
  const h = span * 0.4
  return `M${-hw},0 Q${-hw/2},${-h} 0,0 Q${hw/2},${-h} ${hw},0`
}

// 山体SVG路径
const mountainsFar = 'M0,160 L60,100 L130,140 L200,80 L280,130 L360,70 L440,120 L520,90 L600,130 L680,60 L760,110 L840,80 L920,120 L1000,70 L1080,130 L1160,85 L1240,140 L1320,100 L1440,150 L1440,320 L0,320 Z'
const mountainsMid = 'M0,210 L50,150 L120,190 L190,120 L280,170 L370,110 L460,165 L540,130 L620,175 L710,115 L800,160 L890,125 L980,170 L1070,135 L1170,180 L1250,145 L1340,185 L1440,155 L1440,320 L0,320 Z'
const mountainsNear = 'M0,260 L40,210 L90,240 L150,180 L230,230 L310,185 L390,225 L470,190 L550,230 L630,195 L720,225 L800,180 L890,220 L970,195 L1060,230 L1140,190 L1220,225 L1300,195 L1380,230 L1440,205 L1440,320 L0,320 Z'

// ==================== 前端 Canvas 验证码 ====================
const captchaCanvas = ref(null)
const captchaCode = ref('')
function drawCaptcha(code) {
  const canvas = captchaCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const w = canvas.width, h = canvas.height

  // 背景
  ctx.fillStyle = '#f0f2f5'
  ctx.fillRect(0, 0, w, h)

  // 干扰线
  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = `rgba(${Math.random()*150+100}, ${Math.random()*150+100}, ${Math.random()*150+100}, 0.35)`
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(Math.random() * w, Math.random() * h)
    ctx.lineTo(Math.random() * w, Math.random() * h)
    ctx.stroke()
  }

  // 干扰点
  for (let i = 0; i < 30; i++) {
    ctx.fillStyle = `rgba(${Math.random()*200}, ${Math.random()*200}, ${Math.random()*200}, 0.4)`
    ctx.beginPath()
    ctx.arc(Math.random() * w, Math.random() * h, 1, 0, Math.PI * 2)
    ctx.fill()
  }

  // 文字
  const chars = code.split('')
  const fontFamilies = ['Georgia', 'Times New Roman', 'Courier New', 'Verdana']
  chars.forEach((ch, i) => {
    ctx.font = `bold ${18 + Math.random()*6}px ${fontFamilies[Math.floor(Math.random()*fontFamilies.length)]}`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    // 随机颜色（深色系确保可读）
    ctx.fillStyle = `rgb(${Math.floor(Math.random()*80+30)}, ${Math.floor(Math.random()*80+30)}, ${Math.floor(Math.random()*80+30)})`
    ctx.save()
    ctx.translate(24 + i * 26, h / 2 + (Math.random()-0.5)*8)
    ctx.rotate((Math.random()-0.5)*0.4)
    ctx.fillText(ch, 0, 0)
    ctx.restore()
  })
}

function generateCaptchaCode(length = 4) {
  const pool = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789' // 去掉易混淆字符 O0I1l
  let code = ''
  for (let i = 0; i < length; i++) code += pool[Math.floor(Math.random() * pool.length)]
  return code
}

function refreshCaptcha() {
  form.captcha = ''
  captchaCode.value = generateCaptchaCode()
  nextTick(() => drawCaptcha(captchaCode.value))
}

// ==================== 登录表单 ====================
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '', captcha: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value) callback(new Error('请输入验证码'))
        else if (value.toUpperCase() !== captchaCode.value) callback(new Error('验证码不正确'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

async function handleLogin() {
  if (loading.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    const redirect = sessionStorage.getItem('redirect')
    if (redirect) {
      sessionStorage.removeItem('redirect')
      router.push(redirect)
    } else {
      const staffRoles = ['ADMIN', 'MANAGER', 'CHECKER']
      if (staffRoles.includes(userStore.userInfo?.roleCode)) {
        router.push('/admin')
      } else {
        router.push('/')
      }
    }
  } catch (e) {
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

// ==================== 生命周期 ====================
let captchaTimer = null

onMounted(() => {
  generateStars()
  generateBirds()
})

// 监听表单出现后立即绘制验证码
watch(formVisible, (visible) => {
  if (visible) {
    // 需要 setTimeout 确保 v-if 渲染完成后 canvas 在 DOM 中
    setTimeout(() => refreshCaptcha(), 50)
    // 每60秒自动刷新验证码
    if (captchaTimer) clearInterval(captchaTimer)
    captchaTimer = setInterval(() => refreshCaptcha(), 60000)
  } else {
    if (captchaTimer) {
      clearInterval(captchaTimer)
      captchaTimer = null
    }
  }
})

onUnmounted(() => {
  isDragging.value = false
  if (captchaTimer) clearInterval(captchaTimer)
})
</script>

<style scoped>
/* ============================================
   全局样式
   ============================================ */
.sunrise-login {
  width: 100vw;
  height: 100vh;
  position: relative;
  overflow: hidden;
  background: #0a0a2e;
  user-select: none;
  -webkit-user-select: none;
  touch-action: none;
}

/* ============================================
   场景层
   ============================================ */
.scene {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.sky-layer {
  position: absolute;
  inset: 0;
  transition: background 0.8s ease;
}

.stars-layer {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  transition: opacity 0.6s ease;
}
.stars-svg {
  width: 100%;
  height: 70%;
}

@keyframes star-twinkle {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}

.sun-container {
  position: absolute;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 3;
  width: 100px;
  height: 100px;
}
.sun-container.dragging { z-index: 5; }

.sun-aura {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  width: 240px; height: 240px;
  border-radius: 50%;
  transition: opacity 0.8s ease;
  pointer-events: none;
}

.sun-body {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  width: 56px; height: 56px;
  border-radius: 50%;
  transition: background 0.8s ease, box-shadow 0.8s ease;
}

.sun-highlight {
  position: absolute;
  top: 18%; left: 25%;
  width: 14px; height: 14px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255,255,255,0.9) 0%, rgba(255,255,255,0) 70%);
}

.birds-layer {
  position: absolute;
  top: 10%;
  left: 0;
  width: 100%;
  height: 30%;
  z-index: 2;
  pointer-events: none;
  transition: opacity 0.8s ease;
}
.birds-svg {
  width: 100%;
  height: 100%;
}

@keyframes bird-fly {
  0% { transform: translate(110%, var(--bird-y)) scaleX(1); }
  49% { transform: translate(-10%, var(--bird-y)) scaleX(1); }
  51% { transform: translate(-10%, var(--bird-y)) scaleX(-1); }
  100% { transform: translate(110%, var(--bird-y)) scaleX(-1); }
}

.mountains-layer {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 55%;
  z-index: 2;
  pointer-events: none;
}
.mountains-svg { width: 100%; height: 100%; }
.mountains-svg path { transition: fill 0.8s ease; }

.ground-layer {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 20%;
  z-index: 1;
  transition: background 0.8s ease;
}

/* ============================================
   登录表单 — 更宽更精致
   ============================================ */
.form-overlay {
  position: absolute;
  inset: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-card {
  width: 700px;
  height: 600px;
  max-width: 100%;
  min-height: 200px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 72px 64px 64px;
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.4),
    0 12px 48px rgba(0, 0, 0, 0.12),
    0 0 80px -10px rgba(245, 166, 35, 0.08);
  transition: box-shadow 0.8s ease, color 0.3s;
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.scenic-logo {
  width: 72px;
  height: 72px;
  object-fit: contain;
  border-radius: 16px;
  margin-bottom: 14px;
}
.login-card.has-logo .scenic-logo {
  margin-bottom: 14px;
}

.scenic-name {
  font-size: 23px;
  font-weight: 700;
  margin: 0 0 6px;
  letter-spacing: 0.04em;
  transition: color 0.3s;
}

.scenic-subtitle {
  font-size: 13px;
  color: #999;
  margin: 0;
  font-weight: 400;
}

/* 表单内部 */
.login-card :deep(.el-form-item) {
  margin-bottom: 30px;
}

.login-card :deep(.el-input__wrapper) {
  border-radius: 12px !important;
  padding: 5px 15px !important;
  transition: all 0.3s ease !important;
  box-shadow: 0 0 0 1px rgba(0,0,0,0.06) !important;
  background: #f7f8fa !important;
}

.login-card :deep(.el-input__wrapper:hover) {
  border-color: transparent !important;
  box-shadow: 0 0 0 1px rgba(0,0,0,0.12) !important;
}

.login-card.has-bg :deep(.el-input__wrapper) {
  background: rgba(255,255,255,0.12) !important;
  box-shadow: 0 0 0 1px rgba(255,255,255,0.12) !important;
}

.login-card.has-bg :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(255,255,255,0.25) !important;
}

.login-card :deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 2px rgba(245, 166, 35, 0.25) !important;
  background: #fff !important;
}

.login-card.has-bg :deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 2px rgba(255,255,255,0.3) !important;
  background: rgba(255,255,255,0.15) !important;
}

.login-card :deep(.el-input__inner) {
  font-size: 15px !important;
  color: #333 !important;
}
.login-card.has-bg :deep(.el-input__inner) {
  color: #eee !important;
}

.login-card :deep(.el-input .el-input__prefix .el-icon) {
  color: #bbb !important;
}
.login-card.has-bg :deep(.el-input .el-input__prefix .el-icon) {
  color: rgba(232,224,216,0.5) !important;
}

/* 验证码行 */
.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;
}
.captcha-input {
  flex: 1;
}
.captcha-box {
  width: 120px;
  height: 42px;
  border-radius: 12px;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f7f8fa;
  border: 1px solid rgba(0,0,0,0.06);
  flex-shrink: 0;
  transition: border-color 0.2s;
}
.captcha-box:hover {
  border-color: rgba(245, 166, 35, 0.4);
}
.login-card.has-bg .captcha-box {
  background: rgba(255,255,255,0.1);
  border-color: rgba(255,255,255,0.12);
}
.captcha-canvas {
  width: 100%;
  height: 100%;
  display: block;
}
/* 按钮 */
.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.12em;
  border-radius: 12px !important;
  margin-top: 4px;
  background: linear-gradient(135deg, var(--btn-color, #f5a623), var(--btn-color-dark, #e89a1f)) !important;
  border: none !important;
  transition: all 0.3s ease !important;
}

.login-btn:hover {
  filter: brightness(1.06);
  transform: translateY(-1px);
}

.login-btn:active {
  transform: translateY(0);
}

/* 底部 */
.login-footer {
  text-align: center;
  margin-top: 20px;
}

.footer-label {
  color: #999;
  font-size: 14px;
}

/* ============================================
   动画
   ============================================ */
.form-rise-enter-active {
  animation: formRiseIn 0.7s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}
.form-rise-leave-active {
  animation: formRiseIn 0.4s ease-in reverse both;
}

@keyframes formRiseIn {
  0% {
    opacity: 0;
    transform: translateY(50px) scale(0.94);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 背景图模式文字调亮 */
.login-card.has-bg .scenic-subtitle {
  color: rgba(232, 224, 216, 0.55);
}
.login-card.has-bg .footer-label {
  color: rgba(232, 224, 216, 0.55);
}
.login-card.has-bg :deep(.el-link) {
  color: rgba(232, 224, 216, 0.85) !important;
}

/* ============================================
   拖拽提示
   ============================================ */
.drag-hint {
  position: absolute;
  bottom: 35%;
  left: 50%;
  transform: translateX(-50%);
  z-index: 8;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  pointer-events: none;
}

.hint-label {
  color: rgba(255, 255, 255, 0.85);
  font-size: 18px;
  letter-spacing: 0.08em;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.6);
  animation: hintPulse 2s ease-in-out infinite;
}

@keyframes hintPulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

.hint-fade-enter-active { animation: hintIn 0.5s ease; }
.hint-fade-leave-active { animation: hintIn 0.3s ease reverse; }
@keyframes hintIn { from { opacity: 0; } to { opacity: 1; } }

/* ============================================
   响应式
   ============================================ */
@media (max-width: 520px) {
  .login-card {
    padding: 32px 26px 26px;
    border-radius: 20px;
    width: auto;
    min-width: 300px;
  }
  .scenic-logo {
    width: 56px;
    height: 56px;
  }
  .scenic-name { font-size: 20px; }
  .sun-container { width: 70px; height: 70px; }
  .sun-body { width: 40px; height: 40px; }
  .sun-aura { width: 180px; height: 180px; }
  .drag-hint { bottom: 30%; }
  .captcha-box { width: 100px; height: 38px; }
}

/* CSS 自定义属性声明 */
:root {
  --bird-y: 0%;
  --btn-color: #f5a623;
  --btn-color-dark: #e89a1f;
}
</style>
