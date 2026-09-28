<template>
  <div class="checker-dashboard">
    <!-- 欢迎区域 -->
    <div class="welcome-banner" :class="{ 'light-bg': isLightBg }" :style="{ background: welcomeBg }">
      <div class="welcome-left">
        <el-avatar :size="52" :src="userStore.userInfo?.avatarUrl || ''" :class="{ 'welcome-avatar': true, 'avatar-light': isLightBg }">
          {{ (userStore.userInfo?.realName || '检')[0] }}
        </el-avatar>
        <div class="welcome-text" :class="{ 'text-dark': isLightBg }">
          <div class="welcome-greeting">{{ greeting }}，{{ userStore.userInfo?.realName || '检票员' }} 👋</div>
          <div class="welcome-quote">{{ quote }}</div>
        </div>
      </div>
      <div class="welcome-date" :class="{ 'text-dark': isLightBg }">{{ currentDate }}</div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 24px;">
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color: #409EFF;">{{ stats.todayEntry }}</div>
          <div class="stat-label">今日入园</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color: #E6A23C;">{{ stats.todayExit }}</div>
          <div class="stat-label">今日出园</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" style="color: #67C23A;">{{ stats.inParkCount }}</div>
          <div class="stat-label">当前在园</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.todayFaceCheck }}</div>
          <div class="stat-label">核验次数</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.manualEntry }}</div>
          <div class="stat-label">人工入场</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.todayOrders }}</div>
          <div class="stat-label">有效订单</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 人脸核验检票 -->
    <el-card style="margin-bottom: 24px;">
      <template #header><h3>人脸核验检票</h3></template>
      <div class="face-check-area">
        <!-- 摄像头区域 -->
        <div class="camera-wrapper">
          <video
            ref="videoRef"
            autoplay
            playsinline
            :class="['camera-video', { active: cameraActive }]"
          />
          <div v-if="!cameraActive" class="camera-mask">
            <el-icon :size="48"><VideoCamera /></el-icon>
            <span>摄像头未开启</span>
          </div>
          <canvas ref="canvasRef" style="display:none;" />
        </div>

        <!-- 控制区域 -->
        <div class="control-panel">
          <el-form :model="faceForm" label-width="100px">
            <el-form-item label="闸机编号">
              <el-select v-model="faceForm.gateNo" placeholder="请选择闸机" style="width: 200px;">
                <el-option
                  v-for="g in gates"
                  :key="g.gateNo"
                  :label="g.gateNo + (g.name ? ' (' + g.name + ')' : '')"
                  :value="g.gateNo"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button v-if="!cameraActive" type="primary" @click="startCamera">
                <el-icon><VideoCamera /></el-icon> 开启摄像头
              </el-button>
              <el-button v-else type="danger" @click="stopCamera">
                <el-icon><SwitchButton /></el-icon> 关闭摄像头
              </el-button>
              <el-button
                type="success"
                :loading="verifying"
                @click="handleVerifyFace"
                :disabled="!cameraActive || !faceForm.gateNo"
              >
                开始核验
              </el-button>
            </el-form-item>
          </el-form>

          <!-- 核验结果 -->
          <el-alert
            v-if="verifyResult"
            :title="verifyResult.message"
            :type="verifyResult.success ? (verifyResult.action === 'EXIT' ? 'warning' : 'success') : 'error'"
            show-icon
            :closable="false"
            style="margin-top: 16px;"
          >
            <template v-if="verifyResult.success" #default>
              <p>游客: {{ verifyResult.userName }}</p>
              <p>相似度: {{ verifyResult.score.toFixed(2) }}%</p>
              <p v-if="verifyResult.action === 'EXIT'">出园时间: {{ verifyResult.exitTime }}</p>
              <p v-else>入园时间: {{ verifyResult.entryTime }}</p>
              <p>通道: {{ verifyResult.gateNo }}</p>
              <p v-if="verifyResult.captureImagePath">
                <el-image :src="verifyResult.captureImagePath" :preview-src-list="[verifyResult.captureImagePath]" preview-teleported style="width: 100px; height: 100px; object-fit: cover; border-radius: 4px; margin-top: 4px;" />
              </p>
            </template>
          </el-alert>
        </div>
      </div>
    </el-card>

    <!-- 人工验票入场 -->
    <el-card style="margin-bottom: 24px;">
      <template #header><h3>人工验票入场</h3></template>
      <el-form :inline="true" :model="manualForm">
        <el-form-item label="订单编号">
          <el-autocomplete
            v-model="manualForm.orderNo"
            :fetch-suggestions="searchOrders"
            placeholder="输入订单编号"
            style="width: 260px;"
            clearable
            @select="onOrderSelect"
            @keyup.enter="handleManualEntry"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" :loading="manualLoading" @click="handleManualEntry">人工入场</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 人工验票出场 -->
    <el-card style="margin-bottom: 24px;">
      <template #header><h3>人工验票出场</h3></template>
      <el-form :inline="true" :model="exitForm">
        <el-form-item label="订单编号">
          <el-input v-model="exitForm.orderNo" placeholder="输入要出园的订单编号" style="width: 260px;" clearable @keyup.enter="handleManualExit" />
        </el-form-item>
        <el-form-item>
          <el-button type="danger" :loading="exitLoading" @click="handleManualExit">人工出场</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 今日入园记录 -->
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <h3>今日入园记录</h3>
          <el-button size="small" @click="loadEntries">刷新</el-button>
        </div>
      </template>
      <el-table :data="entries" v-loading="entryLoading">
        <el-table-column prop="gateNo" label="闸机" width="80" />
        <el-table-column label="姓名" width="100">
          <template #default="{ row }">{{ row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column label="录入照片" width="100">
          <template #default="{ row }">
            <el-image v-if="row.enrollImagePath" :src="row.enrollImagePath" :preview-src-list="[row.enrollImagePath]" preview-teleported style="width: 60px; height: 60px; object-fit: cover; border-radius: 4px;" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="闸机抓拍" width="100">
          <template #default="{ row }">
            <el-image v-if="row.captureImagePath" :src="row.captureImagePath" :preview-src-list="[row.captureImagePath]" preview-teleported style="width: 60px; height: 60px; object-fit: cover; border-radius: 4px;" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="相似度" width="100">
          <template #default="{ row }">{{ row.compareScore != null ? row.compareScore.toFixed(2) + '%' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="entryTime" label="入园时间" width="180" />
        <el-table-column label="出园时间" width="180">
          <template #default="{ row }">{{ row.exitTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status ? 'success' : 'danger'">{{ row.status ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单号" width="180">
          <template #default="{ row }">{{ row.orderNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="票种" width="120">
          <template #default="{ row }">{{ row.ticketTypeName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="failReason" label="备注" />
      </el-table>
      <el-pagination
        v-model:current-page="entryPage"
        :page-size="entrySize"
        :total="entryTotal"
        layout="prev, pager, next"
        @current-change="loadEntries"
        style="margin-top: 16px; justify-content: center;"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { VideoCamera, SwitchButton } from '@element-plus/icons-vue'
import { verifyFace, manualEntry, manualExit, getCheckerEntries, getCheckerDashboard, getGates, getValidOrders } from '../../api'
import { useUserStore } from '../../stores/user'
import { useScenicStore } from '../../stores/scenic'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const scenicStore = useScenicStore()

// 欢迎横幅背景色：使用主题色，默认白色
const welcomeBg = computed(() => scenicStore.primaryColor || '#ffffff')
const isLightBg = computed(() => !scenicStore.primaryColor || scenicStore.primaryColor === '#ffffff')

// 根据时段返回问候语
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

// 励志语录池
const quotes = [
  '不积跬步，无以至千里；不积小流，无以成江海。',
  '成功不是将来才有的，而是从决定去做的那一刻起，持续累积而成。',
  '每一天都是一个新的开始，深呼吸，从头再来。',
  '世上无难事，只要肯登攀。',
  '业精于勤，荒于嬉；行成于思，毁于随。',
  '千里之行，始于足下。',
  '细节决定成败，态度决定高度。',
  '简单的事情重复做，重复的事情用心做。',
  '用心服务每一位游客，让景区因你而更美。',
  '今天的努力，是明天惊喜的铺垫。',
  '志当存高远，路自脚下行。',
  '宝剑锋从磨砺出，梅花香自苦寒来。',
  '人生在勤，不索何获。',
  '路漫漫其修远兮，吾将上下而求索。',
  '长风破浪会有时，直挂云帆济沧海。',
  '天行健，君子以自强不息。',
  '博观而约取，厚积而薄发。',
  '山重水复疑无路，柳暗花明又一村。',
  '纸上得来终觉浅，绝知此事要躬行。',
  '莫愁前路无知己，天下谁人不识君。',
  '千淘万漉虽辛苦，吹尽狂沙始到金。',
  '海阔凭鱼跃，天高任鸟飞。',
  '不要等待机会，而要创造机会。',
  '把每一件简单的事做好就是不简单。',
  '行动是治愈恐惧的良药，而犹豫将不断滋养恐惧。',
  '所有的胜利，与征服自己的胜利比起来，都是微不足道。',
  '卓越不是一种行为，而是一种习惯。',
  '最困难的时候，就是离成功不远了。',
  '只有不断找寻机会的人，才会及时把握机会。',
  '每一个认真付出的日子，都值得被铭记。',
  '你的努力，终将成就无可替代的自己。',
]

// 当日随机选取一条语录
const quote = computed(() => quotes[new Date().getDate() % quotes.length])

// 当前日期
const currentDate = computed(() => {
  const now = new Date()
  const weekMap = ['日', '一', '二', '三', '四', '五', '六']
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 星期${weekMap[now.getDay()]}`
})

// 统计
const stats = reactive({ todayEntry: 0, todayExit: 0, inParkCount: 0, todayFaceCheck: 0, manualEntry: 0, todayOrders: 0 })

// 闸机列表
const gates = ref([])

// 人脸核验
const faceForm = reactive({ gateNo: '', imageBase64: '' })
const verifying = ref(false)
const verifyResult = ref(null)
const videoRef = ref(null)
const canvasRef = ref(null)
const stream = ref(null)
const cameraActive = ref(false)

// 人工验票
const manualForm = reactive({ orderNo: '', selectedOrder: null })
const manualLoading = ref(false)

// 人工出场
const exitForm = reactive({ orderNo: '' })
const exitLoading = ref(false)

// 自动补全：根据输入模糊搜索今日有效订单
async function searchOrders(queryString, cb) {
  if (!queryString || queryString.trim().length === 0) {
    cb([])
    return
  }
  try {
    const list = await getValidOrders({ orderNo: queryString.trim() })
    const suggestions = (list || []).map(o => ({
      value: o.orderNo,
      order: o,
      // 下拉列表中显示的文本
      label: `${o.orderNo} — 景区${o.scenicId || ''} ¥${o.payAmount || 0}`
    }))
    cb(suggestions)
  } catch (e) {
    cb([])
  }
}

function onOrderSelect(item) {
  manualForm.selectedOrder = item.order
}

// 入园记录
const entries = ref([])
const entryLoading = ref(false)
const entryPage = ref(1)
const entrySize = ref(15)
const entryTotal = ref(0)

async function loadGates() {
  try {
    const list = await getGates()
    gates.value = list
    if (list.length > 0 && !faceForm.gateNo) {
      faceForm.gateNo = list[0].gateNo
    }
  } catch (e) { console.error(e) }
}

async function startCamera() {
  // 依次尝试多种摄像头配置，提高兼容性
  const constraintList = [
    { video: { width: { ideal: 640 }, height: { ideal: 480 } } },
    { video: { width: { ideal: 1280 }, height: { ideal: 720 } } },
    { video: { facingMode: 'user' } },
    { video: true },
  ]
  for (const constraints of constraintList) {
    try {
      const s = await navigator.mediaDevices.getUserMedia(constraints)
      stream.value = s
      if (videoRef.value) {
        videoRef.value.srcObject = s
        // 等待视频元数据加载完成（确保 videoWidth/videoHeight 可用）
        await new Promise((resolve, reject) => {
          const timeout = setTimeout(() => reject(new Error('视频加载超时')), 5000)
          videoRef.value.onloadedmetadata = () => {
            clearTimeout(timeout)
            resolve()
          }
        })
        if (videoRef.value.videoWidth === 0 || videoRef.value.videoHeight === 0) {
          throw new Error('视频分辨率异常')
        }
      }
      cameraActive.value = true
      console.log('摄像头已启动，分辨率:', videoRef.value.videoWidth, '×', videoRef.value.videoHeight)
      return
    } catch (e) {
      console.warn('摄像头配置尝试失败:', constraints, e.message)
    }
  }
  ElMessage.error('无法启动摄像头，请检查摄像头权限和设备')
}

function stopCamera() {
  if (stream.value) {
    stream.value.getTracks().forEach(t => t.stop())
    stream.value = null
  }
  if (videoRef.value) {
    videoRef.value.srcObject = null
  }
  cameraActive.value = false
}

function captureFrame() {
  const video = videoRef.value
  const canvas = canvasRef.value
  if (!video || !canvas) return ''
  const vw = video.videoWidth, vh = video.videoHeight
  if (!vw || !vh) {
    console.error('视频未就绪: videoWidth/videoHeight 为 0')
    return ''
  }
  // 使用视频原始分辨率，不做裁剪变形
  canvas.width = vw
  canvas.height = vh
  const ctx = canvas.getContext('2d')
  ctx.drawImage(video, 0, 0, vw, vh)
  // 获取 base64（去掉前缀 data:image/jpeg;base64,）
  const dataUrl = canvas.toDataURL('image/jpeg', 0.9)
  const base64 = dataUrl.split(',')[1]
  if (!base64) {
    console.error('canvas.toDataURL 返回异常')
    return ''
  }
  console.log('抓拍成功，Base64 长度:', base64.length)
  return base64.trim()
}

async function handleVerifyFace() {
  if (!cameraActive.value) {
    ElMessage.warning('请先开启摄像头')
    return
  }
  if (!faceForm.gateNo) {
    ElMessage.warning('请选择闸机')
    return
  }
  const imageBase64 = captureFrame()
  if (!imageBase64) {
    ElMessage.error('抓拍失败，请确认摄像头已正常工作后重试')
    return
  }
  verifying.value = true
  verifyResult.value = null
  try {
    const res = await verifyFace({
      imageBase64,
      gateNo: faceForm.gateNo,
    })
    verifyResult.value = res
    if (res.success) {
      if (res.action === 'EXIT') {
        ElMessage.success('出园成功，欢迎再次光临！')
      } else {
        ElMessage.success('核验通过，欢迎入园！')
      }
      loadDashboard()
      loadEntries()
    } else {
      // 根据不同失败原因给出不同提示
      const msg = res.message || '核验失败'
      if (msg.includes('未检测到人脸')) {
        ElMessage.warning('未检测到人脸，请正对摄像头后重试')
      } else if (msg.includes('未找到匹配')) {
        ElMessage.warning('人脸未录入系统，请先注册人脸')
      } else if (msg.includes('未找到今日有效订单') || msg.includes('订单')) {
        ElMessage.warning('人脸验证成功，但您尚未购买今日门票')
      } else if (msg.includes('已入园')) {
        ElMessage.warning('该游客今日已入园')
      } else {
        ElMessage.warning(msg)
      }
    }
  } catch (e) {
    console.error('核验失败', e)
    verifyResult.value = { success: false, message: e.message || '核验服务异常，请稍后重试' }
    ElMessage.error(e.message || '核验服务异常')
  } finally {
    verifying.value = false
  }
}

async function handleManualEntry() {
  const orderNo = manualForm.orderNo.trim()
  if (!orderNo) {
    ElMessage.warning('请输入订单编号')
    return
  }
  // 弹出入场确认提示
  try {
    await ElMessageBox.confirm(
      `确认对订单【${orderNo}】执行人工入场操作？`,
      '人工入场确认',
      { confirmButtonText: '确认入场', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return // 用户取消
  }
  manualLoading.value = true
  try {
    await manualEntry({ orderNo })
    ElMessage.success('人工入场成功')
    manualForm.orderNo = ''
    manualForm.selectedOrder = null
    loadDashboard()
    loadEntries()
  } catch (e) {
    console.error('人工入场失败', e)
    ElMessage.error(e.message || '操作失败，请检查订单号')
  } finally {
    manualLoading.value = false
  }
}

async function handleManualExit() {
  const orderNo = exitForm.orderNo.trim()
  if (!orderNo) {
    ElMessage.warning('请输入订单编号')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认对订单【${orderNo}】执行人工出场？该订单所有在园人员将全部出园。`,
      '人工出场确认',
      { confirmButtonText: '确认出场', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return // 用户取消
  }
  exitLoading.value = true
  try {
    await manualExit({ orderNo })
    ElMessage.success('人工出场成功')
    exitForm.orderNo = ''
    loadDashboard()
    loadEntries()
  } catch (e) {
    console.error('人工出场失败', e)
    ElMessage.error(e.message || '操作失败，请检查订单号')
  } finally {
    exitLoading.value = false
  }
}

async function loadDashboard() {
  try {
    const res = await getCheckerDashboard()
    Object.assign(stats, res)
  } catch (e) { console.error('加载统计失败', e) }
}

async function loadEntries() {
  entryLoading.value = true
  try {
    const res = await getCheckerEntries({ page: entryPage.value, size: entrySize.value })
    entries.value = res.records
    entryTotal.value = res.total
    stats.todayEntry = res.todayTotal || 0
    stats.todayFaceCheck = res.total || 0
  } catch (e) { console.error('加载入园记录失败', e) }
  finally { entryLoading.value = false }
}

onMounted(() => {
  loadDashboard()
  loadEntries()
  loadGates()
})

onBeforeUnmount(() => {
  stopCamera()
})
</script>

<style scoped>
.checker-dashboard { max-width: 1400px; margin: 0 auto; }

.welcome-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 12px;
  padding: 24px 32px;
  margin-bottom: 24px;
  color: #fff;
  transition: background 0.3s;
}

.welcome-banner.light-bg {
  color: #333;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.welcome-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.welcome-avatar {
  flex-shrink: 0;
  border: 3px solid rgba(255,255,255,0.5);
}

.welcome-avatar.avatar-light {
  border-color: #e4e7ed;
}

.welcome-greeting {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 6px;
}

.welcome-quote {
  font-size: 14px;
  opacity: 0.85;
  max-width: 500px;
}

.welcome-date {
  font-size: 14px;
  opacity: 0.75;
  white-space: nowrap;
}

.text-dark .welcome-quote,
.text-dark.welcome-date {
  opacity: 0.7;
}

.stat-card { text-align: center; }
.stat-value { font-size: 32px; font-weight: 700; color: #409EFF; }
.stat-label { font-size: 14px; color: #909399; margin-top: 8px; }

.face-check-area {
  display: flex;
  gap: 32px;
  align-items: flex-start;
  flex-wrap: wrap;
}
.camera-wrapper {
  position: relative;
  width: 400px;
  height: 300px;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}
.camera-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: none;
}
.camera-video.active {
  display: block;
}
.camera-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  background: #f5f7fa;
}
.camera-mask span {
  margin-top: 8px;
  font-size: 14px;
}
.control-panel {
  flex: 1;
  min-width: 280px;
}
</style>
