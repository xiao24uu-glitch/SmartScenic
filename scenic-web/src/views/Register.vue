<template>
  <div class="sunrise-register">
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

      <!-- 太阳（静止在天顶） -->
      <div class="sun-container" :style="sunContainerStyle">
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

    <!-- ==================== 注册表单 ==================== -->
    <div class="form-overlay">
      <div class="register-card" :class="{ 'has-bg': hasRegisterBg, 'has-logo': hasLogo }" :style="cardBgStyle">
        <!-- 头部 -->
        <div class="card-header">
          <img v-if="scenicStore.scenicLogoUrl" :src="scenicStore.scenicLogoUrl" class="scenic-logo" alt="logo" />
          <h2 class="scenic-name" :style="{ color: hasRegisterBg ? '#fff' : scenicStore.primaryColor || '#f5a623' }">
            {{ scenicStore.scenicName || '智慧景区' }}
          </h2>
          <p class="scenic-subtitle">加入我们，开启美好旅程</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large">
          <!-- 第一行：头像 + 用户名 -->
          <div class="row-first">
            <div class="avatar-upload">
              <el-upload
                :auto-upload="false"
                :show-file-list="false"
                :on-change="handleAvatarChange"
                accept="image/*"
              >
                <img v-if="form.avatarUrl" :src="form.avatarUrl" class="avatar-preview" />
                <div v-else class="avatar-placeholder">
                  <el-icon :size="18"><Plus /></el-icon>
                </div>
              </el-upload>
            </div>
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" clearable />
            </el-form-item>
          </div>

          <!-- 第二行：密码 + 确认密码 -->
          <div class="row-grid">
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="密码（至少6位）" prefix-icon="Lock" show-password clearable />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" show-password clearable />
            </el-form-item>
          </div>

          <!-- 第三行：姓名 + 手机号 -->
          <div class="row-grid">
            <el-form-item prop="realName">
              <el-input v-model="form.realName" placeholder="真实姓名" prefix-icon="Edit" clearable />
            </el-form-item>
            <el-form-item prop="phone">
              <el-input v-model="form.phone" placeholder="手机号" prefix-icon="Phone" clearable />
            </el-form-item>
          </div>

          <!-- 邮箱 -->
          <el-form-item prop="email">
            <el-input v-model="form.email" placeholder="邮箱" prefix-icon="Message" clearable />
          </el-form-item>

          <!-- 注册按钮 -->
          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              @click="handleRegister"
              class="register-btn"
              :style="{ '--btn-color': scenicStore.primaryColor || '#f5a623', '--btn-color-dark': scenicStore.primaryColor || '#d4891a' }"
            >
              <span v-if="!loading">注 册</span>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="register-footer">
          <span class="footer-label">已有账号？</span>
          <el-link type="primary" @click="$router.push('/login')">去登录</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useScenicStore } from '../stores/scenic'
import { uploadAvatarTemp } from '../api'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const scenicStore = useScenicStore()

// ==================== 太阳（静止在天顶） ====================
const sunProgress = 1 // 固定：白天状态

// 卡片背景 & Logo
const hasRegisterBg = computed(() => !!scenicStore.registerBgImage)
const hasLogo = computed(() => !!scenicStore.scenicLogoUrl)
const cardBgStyle = computed(() => {
  if (scenicStore.registerBgImage) {
    return {
      background: `linear-gradient(rgba(0,0,0,0.55), rgba(0,0,0,0.58)), url(${scenicStore.registerBgImage}) center/cover no-repeat`,
      color: '#e8e0d8',
    }
  }
  return {}
})

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

const skyStyle = computed(() => {
  const colors = skyStages[skyStages.length - 1].colors
  return { background: `linear-gradient(180deg, ${colors.map(c => lerpColor(c, c, 0)).join(', ')})` }
})

const mountainFarColor  = computed(() => lerpColor(0x1a1030, 0x3d2860, sunProgress))
const mountainMidColor  = computed(() => lerpColor(0x110a20, 0x2a1850, sunProgress))
const mountainNearColor = computed(() => lerpColor(0x0a0518, 0x1a0e38, sunProgress))
const groundColor       = computed(() => lerpColor(0x080410, 0x140a28, sunProgress))

// 太阳样式（固定白天）
const sunContainerStyle = { top: '8%' }

const sunColor = lerpColor(0xffc040, 0xffe888, 1)
const sunGlowColor = lerpColor(0xffcd50, 0xfff0a8, 1)

const sunAuraStyle = computed(() => ({
  background: `radial-gradient(circle, ${sunGlowColor}88 0%, ${sunGlowColor}33 40%, ${sunGlowColor}08 70%, transparent 100%)`,
  opacity: 1,
}))

const sunBodyStyle = computed(() => ({
  background: `radial-gradient(circle at 40% 35%, ${lerpColor(0xffffff, 0xfff8e0, sunProgress)} 0%, ${sunColor} 60%, ${lerpColor(sunColor, sunGlowColor, 0.5)} 100%)`,
  boxShadow: `0 0 ${30 + sunProgress * 60}px ${15 + sunProgress * 20}px ${sunGlowColor}66,
              0 0 ${60 + sunProgress * 100}px ${30 + sunProgress * 30}px ${sunGlowColor}33`,
}))

// 星星
const starsOpacity = computed(() => 0) // 白天不显示
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
const birdsOpacity = computed(() => 1) // 白天一直显示
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

// ==================== 注册表单 ====================
const formRef = ref(null)
const loading = ref(false)
const avatarUploading = ref(false)
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  email: '',
  avatarUrl: '',
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次密码输入不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 3, max: 50, message: '用户名3-50个字符', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: validateConfirmPassword, trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
}

async function handleAvatarChange(file) {
  if (!file.raw) return
  avatarUploading.value = true
  try {
    const res = await uploadAvatarTemp(file.raw)
    form.avatarUrl = res.url
    ElMessage.success('头像上传成功')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.register({
      username: form.username,
      password: form.password,
      realName: form.realName,
      phone: form.phone,
      email: form.email,
      avatarUrl: form.avatarUrl,
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  generateStars()
  generateBirds()
})
</script>

<style scoped>
/* ============================================
   全局样式
   ============================================ */
.sunrise-register {
  width: 100vw;
  height: 100vh;
  position: relative;
  overflow: hidden;
  background: #0a0a2e;
  user-select: none;
  -webkit-user-select: none;
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
}

.stars-layer {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
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

.sun-aura {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  width: 240px; height: 240px;
  border-radius: 50%;
  pointer-events: none;
}

.sun-body {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  width: 56px; height: 56px;
  border-radius: 50%;
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

.ground-layer {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 20%;
  z-index: 1;
}

/* ============================================
   表单
   ============================================ */
.form-overlay {
  position: absolute;
  inset: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow-y: auto;
}

.register-card {
  width: 700px;
  height: 660px;
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

.scenic-name {
  font-size: 23px;
  font-weight: 700;
  margin: 0 0 6px;
  letter-spacing: 0.04em;
}

.scenic-subtitle {
  font-size: 13px;
  color: #999;
  margin: 0;
  font-weight: 400;
}

/* 第一行：头像 + 用户名 */
.row-first {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.row-first :deep(.el-form-item) {
  flex: 1;
  margin-bottom: 0 !important;
}

.row-first .avatar-upload {
  flex-shrink: 0;
}

/* 两栏行 */
.row-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

/* 表单内部 */

.register-card :deep(.el-form-item) {
  margin-bottom: 25px;
}

.register-card :deep(.el-input__wrapper) {
  border-radius: 12px !important;
  padding: 5px 15px !important;
  transition: all 0.3s ease !important;
  box-shadow: 0 0 0 1px rgba(0,0,0,0.06) !important;
  background: #f7f8fa !important;
}

.register-card :deep(.el-input__wrapper:hover) {
  border-color: transparent !important;
  box-shadow: 0 0 0 1px rgba(0,0,0,0.12) !important;
}

.register-card.has-bg :deep(.el-input__wrapper) {
  background: rgba(255,255,255,0.12) !important;
  box-shadow: 0 0 0 1px rgba(255,255,255,0.12) !important;
}

.register-card.has-bg :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(255,255,255,0.25) !important;
}

.register-card :deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 2px rgba(245, 166, 35, 0.25) !important;
  background: #fff !important;
}

.register-card.has-bg :deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 2px rgba(255,255,255,0.3) !important;
  background: rgba(255,255,255,0.15) !important;
}

.register-card :deep(.el-input__inner) {
  font-size: 15px !important;
  color: #333 !important;
}
.register-card.has-bg :deep(.el-input__inner) {
  color: #eee !important;
}

.register-card :deep(.el-input .el-input__prefix .el-icon) {
  color: #bbb !important;
}
.register-card.has-bg :deep(.el-input .el-input__prefix .el-icon) {
  color: rgba(232,224,216,0.5) !important;
}

/* 按钮 */
.register-btn {
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

.register-btn:hover {
  filter: brightness(1.06);
  transform: translateY(-1px);
}

.register-btn:active {
  transform: translateY(0);
}

/* 底部 */
.register-footer {
  text-align: center;
  margin-top: 20px;
}

.footer-label {
  color: #999;
  font-size: 14px;
}

/* 背景图模式文字调亮 */
.register-card.has-bg .scenic-subtitle {
  color: rgba(232, 224, 216, 0.55);
}
.register-card.has-bg .footer-label {
  color: rgba(232, 224, 216, 0.55);
}
.register-card.has-bg :deep(.el-link) {
  color: rgba(232, 224, 216, 0.85) !important;
}

/* 头像上传 */
.avatar-upload {
  display: inline-block;
  text-align: center;
}

.avatar-preview {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 50%;
  border: 2px solid rgba(0,0,0,0.08);
}

.avatar-placeholder {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0;
  color: #bbb;
  border: 2px dashed rgba(0,0,0,0.15);
  cursor: pointer;
  transition: all 0.3s ease;
  background: #f7f8fa;
}

.avatar-placeholder:hover {
  border-color: rgba(245,166,35,0.5);
  color: #999;
}

.avatar-placeholder p {
  margin: 4px 0 0;
  font-size: 11px;
  font-size: 13px;
}

.upload-tip {
  text-align: center;
  color: v-bind('scenicStore.primaryColor');
  font-size: 12px;
  margin-top: 4px;
}

.register-card.has-bg .avatar-placeholder {
  background: rgba(255,255,255,0.1);
  border-color: rgba(255,255,255,0.25);
  color: rgba(232,224,216,0.6);
}

/* ============================================
   响应式
   ============================================ */
@media (max-width: 768px) {
  .row-first {
    flex-direction: column;
    align-items: center;
    gap: 10px;
  }
  .row-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
  .register-card {
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
}

/* CSS 自定义属性 */
:root {
  --bird-y: 0%;
  --btn-color: #f5a623;
  --btn-color-dark: #e89a1f;
}
</style>
