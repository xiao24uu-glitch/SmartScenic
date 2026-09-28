<template>
  <div class="config-page">
    <!-- 景区基本信息配置 -->
    <el-card style="margin-bottom: 24px;">
      <template #header>
        <div class="card-header-row">
          <h3>景区基本信息</h3>
          <el-button type="primary" size="small" :loading="savingScenic" @click="handleSaveScenic">保存基本信息</el-button>
        </div>
      </template>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 20px;">
        <template #title>
          修改景区信息后系统中所有的位置将同步更新 刷新页面即可看到效果
        </template>
      </el-alert>
      <el-form :model="scenicForm" label-width="100px" style="max-width: 600px;">
        <el-form-item label="景区名称">
          <el-input v-model="scenicForm.name" />
        </el-form-item>
        <el-form-item label="景区地址">
          <el-input v-model="scenicForm.address" />
        </el-form-item>
        <el-form-item label="景区介绍">
          <el-input v-model="scenicForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="开放时间">
          <el-time-picker
            v-model="scenicForm.openTime"
            value-format="HH:mm:ss"
            format="HH:mm"
            placeholder="选择开放时间"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="关闭时间">
          <el-time-picker
            v-model="scenicForm.closeTime"
            value-format="HH:mm:ss"
            format="HH:mm"
            placeholder="选择关闭时间"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="最大日承载量">
          <el-input-number v-model="scenicForm.maxCapacity" :min="0" :max="1000000" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="运营状态">
          <el-radio-group v-model="scenicForm.status">
            <el-radio :label="1">正常运营</el-radio>
            <el-radio :label="0">暂停运营</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="景区Logo">
          <div class="logo-upload-area">
            <div class="logo-preview" v-if="scenicForm.logoUrl">
              <img :src="scenicForm.logoUrl" alt="景区Logo" />
              <div class="logo-mask">
                <span @click="triggerLogoUpload">更换</span>
                <span @click="scenicForm.logoUrl = ''">删除</span>
              </div>
            </div>
            <div class="logo-placeholder" v-else @click="triggerLogoUpload">
              <el-icon :size="32"><Plus /></el-icon>
              <span>上传Logo</span>
            </div>
            <input ref="logoInputRef" type="file" accept="image/*" style="display: none;" @change="handleLogoUpload" />
            <div class="logo-tip">支持 JPG、PNG 格式，建议尺寸 200x200 以内</div>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 首页轮播图 -->
    <el-card style="margin-bottom: 24px;">
      <template #header>
        <div class="card-header-row">
          <h3>首页轮播图</h3>
          <el-button type="primary" size="small" :loading="savingBanner" @click="handleSaveBanner">保存轮播图</el-button>
        </div>
      </template>
      <div class="carousel-editor">
        <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px;">
          支持上传多张图片，将在首页顶部自动轮播展示
        </el-alert>
        <div class="carousel-list">
          <div v-for="(img, idx) in bannerList" :key="idx" class="carousel-item">
            <img :src="img" class="carousel-thumb" />
            <div class="carousel-actions">
              <span class="carousel-index">#{{ idx + 1 }}</span>
              <el-button v-if="idx > 0" size="small" circle @click="moveBanner(idx, -1)"><el-icon><ArrowUp /></el-icon></el-button>
              <el-button v-if="idx < bannerList.length - 1" size="small" circle @click="moveBanner(idx, 1)"><el-icon><ArrowDown /></el-icon></el-button>
              <el-button size="small" type="danger" circle @click="removeBanner(idx)"><el-icon><Delete /></el-icon></el-button>
            </div>
          </div>
          <div class="carousel-add" @click="triggerBannerUpload">
            <el-icon :size="28"><Plus /></el-icon>
            <span>添加图片</span>
          </div>
        </div>
        <input ref="bannerInputRef" type="file" accept="image/*" style="display: none;" @change="handleBannerUpload" />
      </div>
    </el-card>

    <!-- 页面背景图 -->
    <el-card style="margin-bottom: 24px;">
      <template #header>
        <div class="card-header-row">
          <h3>各页面背景图</h3>
          <el-button type="primary" size="small" :loading="savingBg" @click="handleSaveBg">保存背景图</el-button>
        </div>
      </template>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px;">
        为每个页面单独设置背景图，留空则使用默认白色背景
      </el-alert>
      <el-row :gutter="20">
        <el-col :span="8" v-for="page in pageBgs" :key="page.key">
          <div class="bg-item">
            <label>{{ page.label }}</label>
            <div class="bg-preview" @click="triggerBgUpload(page.key)">
              <img v-if="scenicForm[page.key]" :src="scenicForm[page.key]" />
              <div v-else class="bg-empty">
                <el-icon :size="20"><Plus /></el-icon>
                <span>点击上传</span>
              </div>
            </div>
            <div class="bg-actions" v-if="scenicForm[page.key]">
              <el-button size="small" @click="triggerBgUpload(page.key)">更换</el-button>
              <el-button size="small" type="danger" @click="scenicForm[page.key] = ''">删除</el-button>
            </div>
          </div>
        </el-col>
      </el-row>
      <input ref="bgInputRef" type="file" accept="image/*" style="display: none;" @change="handleBgUpload" />
    </el-card>

    <!-- 主题色配置 -->
    <el-card style="margin-bottom: 24px;">
      <template #header>
        <div class="card-header-row">
          <h3>主题色配置</h3>
          <el-button type="primary" size="small" :loading="savingTheme" @click="handleSaveTheme">保存主题色</el-button>
        </div>
      </template>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px;">
        全局主题色将应用到导航栏、侧边栏、按钮、链接、强调元素等所有页面（含管理端）
      </el-alert>
      <el-form label-width="100px" style="max-width: 600px;">
        <el-form-item label="选择主题色">
          <!-- 预选色 -->
          <div class="preset-colors">
            <div
              v-for="c in presetColors"
              :key="c"
              class="preset-dot"
              :class="{ active: scenicForm.primaryColor === c }"
              :style="{ background: c }"
              :title="c"
              @click="scenicForm.primaryColor = c"
            >
              <span v-if="scenicForm.primaryColor === c" class="preset-check">✓</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="自定义颜色">
          <div class="color-input-row">
            <span
              v-for="f in colorFormats"
              :key="f.value"
              class="format-tag"
              :class="{ active: colorFormat === f.value }"
              @click="colorFormat = f.value"
            >{{ f.label }}</span>
            <el-color-picker v-model="scenicForm.primaryColor" :color-format="colorFormat" show-alpha />
            <el-input
              v-model="scenicForm.primaryColor"
              :placeholder="formatPlaceholder"
              style="width: 220px; margin-left: 12px;"
              @blur="onColorInputBlur"
            />
          </div>
        </el-form-item>
        <el-form-item label="实时预览">
          <div class="live-preview">
            <!-- 模拟导航栏 -->
            <div class="lp-navbar" :style="{ background: scenicForm.primaryColor }">
              <span class="lp-logo">景区名称</span>
              <span class="lp-nav-item active">首页</span>
              <span class="lp-nav-item">门票选购</span>
              <span class="lp-nav-item">AI助手</span>
              <span class="lp-nav-item">我的订单</span>
              <span class="lp-nav-auth">登录</span>
            </div>
            <!-- 模拟标题与强调文字 -->
            <div class="lp-title" :style="{ color: scenicForm.primaryColor }">
              页面标题 / 强调文字
            </div>
            <!-- 按钮区 -->
            <div class="lp-section-label">按钮组件</div>
            <div class="lp-buttons">
              <span class="lp-btn-primary" :style="{ background: scenicForm.primaryColor }">主要按钮</span>
              <span class="lp-btn-default">默认按钮</span>
            </div>
            <!-- 卡片预览 -->
            <div class="lp-section-label">卡片组件</div>
            <div class="lp-card-row">
              <div class="lp-card" :style="{ borderTopColor: scenicForm.primaryColor }">
                <div class="lp-card-title">景区门票</div>
                <div class="lp-card-price" :style="{ color: scenicForm.primaryColor }">¥1000</div>
                <span class="lp-card-btn" :style="{ background: scenicForm.primaryColor }">立即购买</span>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 系统参数配置 -->
    <el-card>
      <template #header>
        <h3>系统参数配置</h3>
      </template>
      <el-table :data="configs" v-loading="loading">
        <el-table-column prop="configGroup" label="分组" width="120">
          <template #default="{ row }">
            <el-tag :type="groupType(row.configGroup)">{{ groupLabel(row.configGroup) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="configKey" label="配置键" width="220" />
        <el-table-column prop="configValue" label="配置值">
          <template #default="{ row }">
            <span>{{ row.isEncrypted ? '******' + (row.configValue || '').slice(-4) : row.configValue }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadConfigs"
        style="margin-top: 16px; justify-content: center;"
      />
    </el-card>

    <!-- 编辑配置对话框 -->
    <el-dialog v-model="editVisible" title="编辑配置" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="配置键">
          <el-input v-model="editForm.configKey" disabled />
        </el-form-item>
        <el-form-item label="配置说明">
          <el-input v-model="editForm.description" disabled />
        </el-form-item>
        <el-form-item label="配置值">
          <el-input v-model="editForm.configValue" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, ArrowUp, ArrowDown, Delete } from '@element-plus/icons-vue'
import { getAdminConfigs, updateConfig, getAdminScenic, updateAdminScenic, uploadImage } from '../../api'
import { ElMessage } from 'element-plus'

// 景区基本信息
const scenicForm = reactive({
  id: null,
  name: '',
  address: '',
  description: '',
  openTime: '',
  closeTime: '',
  maxCapacity: 50000,
  status: 1,
  logoUrl: '',
  bannerImages: '',
  homeBgImage: '', ticketsBgImage: '', aiBgImage: '', ordersBgImage: '', profileBgImage: '',
  loginBgImage: '', registerBgImage: '',
  primaryColor: '#1A1A1D',
})
const savingScenic = ref(false)
const savingBanner = ref(false)
const savingBg = ref(false)
const savingTheme = ref(false)
const logoInputRef = ref(null)
const bannerInputRef = ref(null)
const bgInputRef = ref(null)

// 轮播图列表
const bannerList = ref([])
// 页面背景图配置
const pageBgs = [
  { key: 'homeBgImage', label: '首页' },
  { key: 'ticketsBgImage', label: '门票选购' },
  { key: 'aiBgImage', label: 'AI助手' },
  { key: 'ordersBgImage', label: '我的订单' },
  { key: 'profileBgImage', label: '个人中心' },
  { key: 'loginBgImage', label: '登录页' },
  { key: 'registerBgImage', label: '注册页' },
]
// 当前正在编辑的背景图key
const currentBgKey = ref('')

// 颜色格式选择
const colorFormat = ref('hex')
const colorFormats = [
  { label: 'HEX', value: 'hex' },
  { label: 'RGB', value: 'rgb' },
  { label: 'HSL', value: 'hsl' },
]

// 输入框占位提示
const formatPlaceholder = computed(() => {
  const map = { hex: '#1a73e8', rgb: 'rgb(26,115,232)', hsl: 'hsl(215,80%,51%)' }
  return map[colorFormat.value] || '#1a73e8'
})

// 预选色列表
const presetColors = [
  '#1a73e8', '#0d47a1', '#409EFF', '#67C23A',
  '#E6A23C', '#F56C6C', '#e74c3c', '#9b59b6',
  '#e91e63', '#00bcd4', '#009688', '#4caf50',
  '#ff9800', '#795548', '#607d8b', '#2c3e50',
]

// 颜色输入框失焦时做基本校验
function onColorInputBlur() {
  const v = (scenicForm.primaryColor || '').trim()
  if (!v) { scenicForm.primaryColor = '#1a73e8'; return }
  // 允许任意 CSS 合法颜色值，只做简单格式检查
  if (!/^(#|rgb|rgba|hsl|hsla|[a-z])/i.test(v)) {
    ElMessage.warning('无效颜色格式，请使用 HEX/RGB/HSL')
    scenicForm.primaryColor = '#1a73e8'
  }
}

// 系统配置列表
const configs = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

// 编辑对话框
const editVisible = ref(false)
const editForm = reactive({
  configKey: '',
  configValue: '',
  description: '',
})

// 加载景区基本信息（从 scenic 表读取）
async function loadScenicInfo() {
  try {
    const data = await getAdminScenic()
    if (data) {
      scenicForm.id = data.id || null
      scenicForm.name = data.name || ''
      scenicForm.address = data.address || ''
      scenicForm.description = data.description || ''
      scenicForm.openTime = data.openTime || ''
      scenicForm.closeTime = data.closeTime || ''
      scenicForm.maxCapacity = data.maxCapacity ?? 50000
      scenicForm.status = data.status ?? 1
      scenicForm.logoUrl = (data.logoUrl && !data.logoUrl.includes('logo.svg')) ? data.logoUrl : ''
      scenicForm.bannerImages = data.bannerImages || ''
      scenicForm.homeBgImage = data.homeBgImage || ''
      scenicForm.ticketsBgImage = data.ticketsBgImage || ''
      scenicForm.aiBgImage = data.aiBgImage || ''
      scenicForm.ordersBgImage = data.ordersBgImage || ''
      scenicForm.profileBgImage = data.profileBgImage || ''
      scenicForm.loginBgImage = data.loginBgImage || ''
      scenicForm.registerBgImage = data.registerBgImage || ''
      scenicForm.primaryColor = data.primaryColor || '#1a73e8'
      // 解析轮播图
      try { bannerList.value = data.bannerImages ? JSON.parse(data.bannerImages) : [] } catch { bannerList.value = [] }
    }
  } catch (e) {
    console.error('加载景区信息失败', e)
  }
}

// 保存景区基本信息
async function handleSaveScenic() {
  if (!scenicForm.name.trim()) {
    ElMessage.warning('请输入景区名称')
    return
  }
  savingScenic.value = true
  try {
    await updateAdminScenic({
      id: scenicForm.id,
      name: scenicForm.name,
      address: scenicForm.address,
      description: scenicForm.description,
      openTime: scenicForm.openTime,
      closeTime: scenicForm.closeTime,
      maxCapacity: scenicForm.maxCapacity,
      status: scenicForm.status,
      logoUrl: scenicForm.logoUrl,
    })
    ElMessage.success('基本信息已保存')
  } catch (e) {
    console.error('保存失败', e)
    ElMessage.error('保存失败，请重试')
  } finally {
    savingScenic.value = false
  }
}

// 单独保存主题色
async function handleSaveTheme() {
  savingTheme.value = true
  try {
    await updateAdminScenic({
      id: scenicForm.id,
      primaryColor: scenicForm.primaryColor,
    })
    ElMessage.success('主题色已保存')
  } catch (e) {
    console.error('保存主题色失败', e)
    ElMessage.error('保存失败，请重试')
  } finally {
    savingTheme.value = false
  }
}

// 单独保存轮播图
async function handleSaveBanner() {
  savingBanner.value = true
  try {
    scenicForm.bannerImages = JSON.stringify(bannerList.value)
    await updateAdminScenic({
      id: scenicForm.id,
      bannerImages: scenicForm.bannerImages,
    })
    ElMessage.success('轮播图已保存')
  } catch (e) {
    console.error('保存轮播图失败', e)
    ElMessage.error('保存失败，请重试')
  } finally {
    savingBanner.value = false
  }
}

// 单独保存各页面背景图
async function handleSaveBg() {
  savingBg.value = true
  try {
    await updateAdminScenic({
      id: scenicForm.id,
      homeBgImage: scenicForm.homeBgImage,
      ticketsBgImage: scenicForm.ticketsBgImage,
      aiBgImage: scenicForm.aiBgImage,
      ordersBgImage: scenicForm.ordersBgImage,
      profileBgImage: scenicForm.profileBgImage,
      loginBgImage: scenicForm.loginBgImage,
      registerBgImage: scenicForm.registerBgImage,
    })
    ElMessage.success('背景图已保存')
  } catch (e) {
    console.error('保存背景图失败', e)
    ElMessage.error('保存失败，请重试')
  } finally {
    savingBg.value = false
  }
}

// ===== 轮播图管理 =====
function triggerBannerUpload() { bannerInputRef.value?.click() }

async function handleBannerUpload(event) {
  const file = event.target.files[0]
  if (!file) return
  try {
    const res = await uploadImage(file, 'banner')
    bannerList.value.push(res.url || res.data)
    ElMessage.success('轮播图已添加')
  } catch (e) { ElMessage.error('上传失败') }
  event.target.value = ''
}

function removeBanner(idx) { bannerList.value.splice(idx, 1) }

function moveBanner(idx, dir) {
  const arr = bannerList.value
  const target = idx + dir
  if (target < 0 || target >= arr.length) return
  ;[arr[idx], arr[target]] = [arr[target], arr[idx]]
}

// ===== 页面背景图 =====
function triggerBgUpload(key) {
  currentBgKey.value = key
  bgInputRef.value?.click()
}

async function handleBgUpload(event) {
  const file = event.target.files[0]
  if (!file) return
  try {
    const res = await uploadImage(file, 'background')
    scenicForm[currentBgKey.value] = res.url || res.data
    ElMessage.success('背景图已上传')
  } catch (e) { ElMessage.error('上传失败') }
  event.target.value = ''
}

// 触发Logo上传
function triggerLogoUpload() {
  logoInputRef.value?.click()
}

// 处理Logo上传（转为Base64存储）
function handleLogoUpload(event) {
  const file = event.target.files[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return
  }

  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过2MB')
    return
  }

  const reader = new FileReader()
  reader.onload = (e) => {
    scenicForm.logoUrl = e.target.result
  }
  reader.readAsDataURL(file)
  // 重置 input，允许重复选择同一文件
  event.target.value = ''
}

function groupType(group) {
  const map = { scenic: '', deepseek: 'warning', baidu: 'danger' }
  return map[group] || 'info'
}

function groupLabel(group) {
  const map = { scenic: '景区配置', deepseek: 'AI配置', baidu: '百度人脸' }
  return map[group] || group
}

async function loadConfigs() {
  loading.value = true
  try {
    const res = await getAdminConfigs({ page: page.value, size: size.value })
    // 景区基本信息（name/address/description/logo_url）已在页面上方独立表单维护，列表中不再展示
    const excludeKeys = ['scenic.name', 'scenic.address', 'scenic.description', 'scenic.open_time', 'scenic.logo_url']
    configs.value = (res.records || []).filter(item => !excludeKeys.includes(item.configKey))
    total.value = res.total
  } catch (e) {
    console.error('加载配置失败', e)
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  editForm.configKey = row.configKey
  editForm.configValue = row.configValue
  editForm.description = row.description
  editVisible.value = true
}

async function handleSave() {
  try {
    await updateConfig({
      configKey: editForm.configKey,
      configValue: editForm.configValue,
    })
    ElMessage.success('配置更新成功')
    editVisible.value = false
    loadConfigs()
  } catch (e) {
    console.error('更新配置失败', e)
  }
}

onMounted(() => {
  loadScenicInfo()
  loadConfigs()
})
</script>

<style scoped>
.config-page {
  max-width: 1200px;
  margin: 0 auto;
}

.card-header-row {
  display: flex; align-items: center; justify-content: space-between;
}
.card-header-row h3 { margin: 0; }

.logo-upload-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.logo-preview {
  position: relative;
  width: 120px;
  height: 120px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
}

.logo-preview img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.logo-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  opacity: 0;
  transition: opacity 0.2s;
}

.logo-preview:hover .logo-mask {
  opacity: 1;
}

.logo-mask span {
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.logo-mask span:hover {
  color: #409eff;
}

.logo-placeholder {
  width: 120px;
  height: 120px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  color: #909399;
  transition: border-color 0.2s;
}

.logo-placeholder:hover {
  border-color: #409eff;
  color: #409eff;
}

.logo-tip {
  font-size: 12px;
  color: #909399;
}

/* 轮播图编辑器 */
.carousel-list {
  display: flex; flex-wrap: wrap; gap: 16px;
}
.carousel-item {
  width: 200px; border: 1px solid #dcdfe6; border-radius: 6px; overflow: hidden;
  background: #fff;
}
.carousel-thumb {
  width: 100%; height: 100px; object-fit: cover; display: block;
}
.carousel-actions {
  display: flex; align-items: center; gap: 6px; padding: 8px;
}
.carousel-index {
  flex: 1; font-size: 12px; color: #909399;
}
.carousel-add {
  width: 200px; height: 148px; border: 2px dashed #dcdfe6; border-radius: 6px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  cursor: pointer; color: #909399; gap: 8px; transition: all 0.2s;
}
.carousel-add:hover { border-color: #409eff; color: #409eff; }

/* 背景图 */
.bg-item { margin-bottom: 16px; }
.bg-item label {
  display: block; font-size: 14px; font-weight: 500; margin-bottom: 8px; color: #333;
}
.bg-preview {
  width: 100%; height: 120px; border: 1px solid #dcdfe6; border-radius: 6px;
  overflow: hidden; cursor: pointer; background: #fafafa;
}
.bg-preview img { width: 100%; height: 100%; object-fit: cover; }
.bg-empty {
  width: 100%; height: 100%; display: flex; flex-direction: column;
  align-items: center; justify-content: center; color: #909399; gap: 4px;
}
.bg-actions { margin-top: 8px; display: flex; gap: 8px; }

/* 主题色 */
.preset-colors {
  display: flex; flex-wrap: wrap; gap: 8px;
}
.preset-dot {
  width: 28px; height: 28px; border-radius: 50%;
  cursor: pointer; border: 3px solid transparent;
  display: flex; align-items: center; justify-content: center;
  transition: transform 0.15s, border-color 0.15s;
  position: relative;
}
.preset-dot:hover { transform: scale(1.15); }
.preset-dot.active {
  border-color: #333; transform: scale(1.15);
}
.preset-check {
  color: #fff; font-size: 13px; font-weight: bold;
  text-shadow: 0 0 2px rgba(0,0,0,0.6);
}
.color-input-row {
  display: flex; align-items: center; gap: 10px;
}
.format-tag {
  padding: 2px 14px; border-radius: 4px; font-size: 13px;
  color: #606266; background: #f0f2f5; cursor: pointer;
  border: 1px solid transparent; transition: all 0.2s;
}
.format-tag:hover { color: var(--el-color-primary); border-color: var(--el-color-primary); }
.format-tag.active {
  color: #fff; background: var(--el-color-primary); border-color: var(--el-color-primary);
}

/* 实时预览 */
.live-preview {
  background: #f5f7fa; border-radius: 12px; padding: 16px;
  display: flex; flex-direction: column; gap: 12px;
  font-size: 13px; border: 1px solid #ebeef5;
}
/* 导航栏 */
.lp-navbar {
  height: 40px; border-radius: 6px; display: flex;
  align-items: center; gap: 14px; padding: 0 14px; color: #fff;
  font-size: 12px;
}
.lp-logo {
  font-weight: bold; margin-right: auto;
}
.lp-nav-item {
  font-size: 12px; opacity: 0.75; cursor: default;
}
.lp-nav-item.active {
  opacity: 1; font-weight: bold;
  border-bottom: 2px solid #ffd04b; padding-bottom: 2px;
}
.lp-nav-auth {
  padding: 3px 10px; border-radius: 4px; font-size: 11px;
  cursor: default; background: rgba(255,255,255,0.2);
}
.lp-nav-auth.outline {
  background: transparent; border: 1px solid rgba(255,255,255,0.6);
}
/* 标题 */
.lp-title {
  font-size: 16px; font-weight: 700; padding: 4px 0;
}
/* 区块标签 */
.lp-section-label {
  font-size: 11px; color: #909399; font-weight: 600;
  text-transform: uppercase; letter-spacing: 1px;
  border-bottom: 1px solid #ebeef5; padding-bottom: 4px;
}
/* 按钮组 */
.lp-buttons {
  display: flex; gap: 8px; flex-wrap: wrap;
}
.lp-btn-primary, .lp-btn-default, .lp-btn-success, .lp-btn-danger,
.lp-btn-outline, .lp-btn-round, .lp-btn-disabled, .lp-btn-large {
  padding: 6px 16px; border-radius: 6px; font-size: 12px; cursor: default;
  border: 1px solid transparent; white-space: nowrap;
}
.lp-btn-primary { color: #fff; }
.lp-btn-default {
  border-color: #dcdfe6; background: #fff; color: #606266;
}
.lp-btn-success { background: #67C23A; color: #fff; }
.lp-btn-danger { background: #F56C6C; color: #fff; }
.lp-btn-outline { background: transparent; border-style: solid; border-width: 1px; }
.lp-btn-round { color: #fff; border-radius: 20px; padding: 4px 20px; }
.lp-btn-disabled {
  background: #f0f0f0; color: #c0c4cc; border-color: #e4e7ed; cursor: not-allowed;
}
.lp-btn-large { color: #fff; padding: 10px 28px; font-size: 14px; border-radius: 10px; }
/* 链接与标签 */
.lp-links {
  display: flex; align-items: center; gap: 14px; flex-wrap: wrap;
  color: #333;
}
.lp-link-hover {
  cursor: default; text-decoration: underline;
}
.lp-tab-active {
  border-bottom: 2px solid; padding-bottom: 2px; font-weight: 600;
}
.lp-tag {
  padding: 2px 10px; border-radius: 4px; border: 1px solid;
  font-size: 11px; background: rgba(255,255,255,0.6);
}
/* 表单元素 */
.lp-form-row {
  display: flex; gap: 10px; flex-wrap: wrap;
}
.lp-input, .lp-select {
  height: 32px; border-radius: 6px; border: 1px solid #dcdfe6;
  background: #fff; display: flex; align-items: center; padding: 0 10px;
  font-size: 12px; color: #606266; gap: 6px;
}
.lp-input { width: 180px; }
.lp-input-icon { font-size: 12px; }
.lp-input-text { color: #c0c4cc; }
.lp-select { width: 120px; justify-content: space-between; cursor: default; }
.lp-select-arrow { color: #c0c4cc; font-size: 10px; }
/* 卡片 */
.lp-card-row {
  display: flex; gap: 12px; flex-wrap: wrap;
}
.lp-card {
  flex: 1; min-width: 160px;
  background: #fff; border-radius: 10px; padding: 14px;
  border-top: 3px solid #dcdfe6;
  display: flex; flex-direction: column; gap: 6px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.lp-card--light { background: #fafbfc; }
.lp-card-title { font-size: 13px; font-weight: 600; color: #303133; }
.lp-card-price { font-size: 22px; font-weight: 700; }
.lp-card-status { font-size: 13px; font-weight: 600; color: #67C23A; }
.lp-card-desc { font-size: 11px; color: #909399; }
.lp-card-btn {
  padding: 6px 0; border-radius: 6px; font-size: 12px; text-align: center;
  color: #fff; cursor: default; margin-top: 6px;
}
.lp-card-link { font-size: 12px; cursor: default; }
</style>
