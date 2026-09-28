<template>
  <div class="profile-page">
    <!-- ====== 页面头部 ====== -->
    <div class="page-hero">
      <div class="section-header">
        <span class="section-tag">PROFILE</span>
        <h2 class="section-title">个人中心</h2>
        <p class="section-subtitle">管理您的账户信息与人脸数据</p>
      </div>
    </div>

    <div class="content-wrapper">
      <div class="section" v-observe>
        <div class="profile-layout">
          <!-- 左侧：用户卡片 -->
          <div class="profile-sidebar">
            <div class="user-card">
              <div class="user-card-cover" :style="{ background: `linear-gradient(135deg, ${scenicStore.primaryColor}, ${scenicStore.primaryColor}cc)` }"></div>
              <div class="user-card-body">
                <div class="user-avatar-wrap">
                  <img
                    v-if="userStore.userInfo?.avatarUrl"
                    :src="userStore.userInfo.avatarUrl"
                    class="user-avatar"
                    alt="avatar"
                  />
                  <div v-else class="user-avatar user-avatar--placeholder" :style="{ background: scenicStore.primaryColor }">
                    {{ (userStore.userInfo?.realName || userStore.userInfo?.username || 'U').charAt(0) }}
                  </div>
                </div>
                <h3 class="user-name">{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</h3>
                <span class="user-role" :style="{ color: scenicStore.primaryColor }">{{ userStore.userInfo?.roleName }}</span>
              </div>
              <div class="user-menu">
                <button
                  :class="['menu-item', { active: activeMenu === 'info' }]"
                  @click="activeMenu = 'info'"
                >
                  <span>基本信息</span>
                </button>
                <button
                  :class="['menu-item', { active: activeMenu === 'souvenir' }]"
                  @click="handleMenuSelect('souvenir')"
                >
                  <span>纪念票</span>
                </button>
                <button
                  :class="['menu-item', { active: activeMenu === 'travelogue' }]"
                  @click="handleMenuSelect('travelogue')"
                >
                  <span>AI游记</span>
                </button>
                <button
                  v-if="!isTourist"
                  :class="['menu-item', { active: activeMenu === 'face' }]"
                  @click="handleMenuSelect('face')"
                >
                  <span>人脸管理</span>
                </button>
              </div>
            </div>
          </div>

          <!-- 右侧：内容区 -->
          <div class="profile-main">
            <!-- 基本信息 -->
            <div v-if="activeMenu === 'info'" class="content-card">
              <div class="content-card-header">
                <h3>基本信息</h3>
              </div>
              <div class="content-card-body">
                <el-descriptions :column="1" border class="info-descriptions">
                  <el-descriptions-item label="用户名">{{ userStore.userInfo?.username }}</el-descriptions-item>
                  <el-descriptions-item label="真实姓名">{{ userStore.userInfo?.realName || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="手机号">{{ userStore.userInfo?.phone || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="邮箱">{{ userStore.userInfo?.email || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="角色">{{ userStore.userInfo?.roleName }}</el-descriptions-item>
                </el-descriptions>
                <div class="content-card-actions">
                  <button class="profile-btn profile-btn--primary" @click="openEditInfo">编辑资料</button>
                  <button class="profile-btn profile-btn--outline" @click="openChangePassword">修改密码</button>
                </div>
              </div>
            </div>

            <!-- 人脸管理 -->
            <div v-if="activeMenu === 'face'" class="content-card">
              <div class="content-card-header">
                <h3>人脸管理</h3>
                <el-tag v-if="faceData" type="success" effect="plain" round size="small">已录入</el-tag>
                <el-tag v-else type="info" effect="plain" round size="small">未录入</el-tag>
              </div>

              <!-- 未录入 -->
              <div v-if="!faceData && !registering" class="face-empty-state">
                <div class="face-empty-icon">✖</div>
                <p class="face-empty-title">您尚未录入内部通道人脸</p>
                <p class="face-empty-desc">内部通道人脸用于员工日常通行闸机，与购票人脸相互独立</p>
                <button class="profile-btn profile-btn--primary" @click="startRegister">现在录入</button>
              </div>

              <!-- 录入中 -->
              <div v-if="registering" class="face-register-area">
                <p class="face-register-tip">请对准摄像头，保持正面免冠，光线充足</p>
                <div class="camera-box">
                  <video ref="videoRef" autoplay playsinline class="camera-video" v-show="!capturedImage"></video>
                  <img v-if="capturedImage" :src="capturedImage" class="camera-video" />
                </div>
                <div class="camera-actions">
                  <button class="profile-btn profile-btn--primary" @click="capture" :disabled="capturedImage">
                    <el-icon :size="16"><Camera /></el-icon> 拍照
                  </button>
                  <button v-if="capturedImage" class="profile-btn profile-btn--outline" @click="resetCapture">重新拍照</button>
                  <button v-if="capturedImage" class="profile-btn profile-btn--success" @click="submitFace" :disabled="submitting">
                    {{ submitting ? '提交中...' : '提交录入' }}
                  </button>
                  <button class="profile-btn profile-btn--ghost" @click="cancelRegister">取消</button>
                </div>
              </div>

              <!-- 已录入 -->
              <div v-if="faceData && !registering" class="face-done-area">
                <div class="face-photo-wrap" v-if="faceData.faceImagePath">
                  <el-image :src="faceData.faceImagePath" :preview-src-list="[faceData.faceImagePath]" preview-teleported class="face-photo" fit="cover" />
                </div>
                <el-descriptions :column="1" border class="info-descriptions">
                  <el-descriptions-item label="人脸状态">
                    <el-tag type="success" effect="plain" round size="small">有效</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="类型">内部通道人脸（不过期）</el-descriptions-item>
                  <el-descriptions-item label="质量分">
                    {{ faceData.qualityScore != null ? faceData.qualityScore : '-' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="录入时间">{{ faceData.createTime }}</el-descriptions-item>
                </el-descriptions>
                <div class="content-card-actions">
                  <button class="profile-btn profile-btn--primary" @click="startRegister">重新录入</button>
                  <button class="profile-btn profile-btn--danger" @click="handleDeleteFace">删除人脸</button>
                </div>
              </div>
            </div>

            <!-- 纪念票 -->
            <div v-if="activeMenu === 'souvenir'" class="content-card">
              <div class="content-card-header">
                <h3>数字纪念票</h3>
                <span style="font-size:13px;color:#94a3b8;">珍藏您的游览回忆</span>
              </div>
              <div class="content-card-body" v-loading="orderLoading">
                <el-empty v-if="orders.length === 0 && !orderLoading" description="暂无已支付订单" />
                <div v-else class="order-list">
                  <div v-for="order in orders" :key="order.orderNo" class="order-item"
                       :class="{ 'order-item--active': viewingOrder === order.orderNo }">
                    <div class="order-item-info">
                      <span class="order-no">{{ order.orderNo }}</span>
                      <span class="order-date">{{ order.visitDate }}</span>
                      <span class="order-amount">¥{{ order.totalAmount }}</span>
                      <el-tag size="small" :type="order.status === 6 ? 'success' : order.status === 5 ? 'warning' : ''">
                        {{ order.status === 6 ? '已出园' : order.status === 5 ? '已入园' : '已支付' }}
                      </el-tag>
                    </div>
                    <el-button size="small" type="primary" @click="viewSouvenir(order.orderNo)">查看纪念票</el-button>
                  </div>
                </div>
              </div>
            </div>

            <!-- AI游记 -->
            <div v-if="activeMenu === 'travelogue'" class="content-card">
              <div class="content-card-header">
                <h3>AI 游记生成</h3>
                <span style="font-size:13px;color:#94a3b8;">DeepSeek大模型为您书写</span>
              </div>
              <div class="content-card-body" v-loading="tlLoading">
                <el-empty v-if="tlOrders.length === 0 && !tlLoading" description="暂无已完成游览的订单" />
                <div v-else class="order-list">
                  <div v-for="order in tlOrders" :key="order.orderNo" class="order-item">
                    <div class="order-item-info">
                      <span class="order-no">{{ order.orderNo }}</span>
                      <span class="order-date">{{ order.visitDate }}</span>
                      <span class="order-amount">¥{{ order.totalAmount }}</span>
                    </div>
                    <div class="order-item-actions">
                      <el-select v-model="travelogueStyle" size="small" style="width:100px;margin-right:8px">
                        <el-option value="literary" label="文艺风" />
                        <el-option value="humor" label="幽默风" />
                        <el-option value="simple" label="简洁风" />
                      </el-select>
                      <el-button size="small" type="primary" @click="generateTravelogue(order.orderNo)" :loading="tlGenerating">
                        {{ tlGenerating ? '生成中...' : '生成游记' }}
                      </el-button>
                    </div>
                  </div>
                </div>
                <!-- 游记结果 -->
                <div v-if="travelogueContent" class="travelogue-result">
                  <div class="travelogue-actions">
                    <el-button size="small" @click="copyTravelogue">复制游记</el-button>
                    <el-dropdown @command="downloadTravelogue" trigger="click" style="margin:0 4px">
                      <el-button size="small" type="primary">
                        下载游记<el-icon class="el-icon--right"><arrow-down /></el-icon>
                      </el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="word">Word</el-dropdown-item>
                          <el-dropdown-item command="txt">TXT</el-dropdown-item>
                          <el-dropdown-item command="image">PNG</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                    <el-button size="small" @click="travelogueContent = ''">关闭</el-button>
                  </div>
                  <div class="travelogue-content" ref="travelogueRef" v-html="renderMarkdown(travelogueContent)"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 数字纪念票弹窗 -->
    <Teleport to="body">
      <transition name="modal-fade">
        <div class="modal-overlay" v-if="souvenirVisible" @click.self="souvenirVisible = false">
          <div class="souvenir-dialog">
            <button class="souvenir-close" @click="souvenirVisible = false">✕</button>
            <div class="souvenir-ticket" ref="souvenirRef" v-if="souvenir">
              <div class="ticket-border">
                <div class="ticket-header">
                  <h2>{{ souvenir.scenicName }}</h2>
                  <span class="ticket-badge">数字纪念票</span>
                </div>
                <div class="ticket-body">
                  <div class="ticket-spot-imgs" v-if="souvenir.spotImages?.length">
                    <img v-for="(img, i) in souvenir.spotImages.slice(0, 2)" :key="i" :src="img" />
                  </div>
                  <div class="ticket-info">
                    <div class="info-row"><span>订单编号</span><strong>{{ souvenir.orderNo }}</strong></div>
                    <div class="info-row"><span>游览日期</span><strong>{{ souvenir.visitDate }}</strong></div>
                    <div class="info-row"><span>票种信息</span>
                      <strong><span v-for="(t, i) in souvenir.tickets" :key="i">{{ t.ticketName }}×{{ t.quantity }}{{ i < souvenir.tickets.length - 1 ? '、' : '' }}</span></strong>
                    </div>
                    <div class="info-row"><span>总金额</span><strong style="color:#E6A23C;">¥{{ souvenir.totalAmount }}</strong></div>
                    <div class="info-row"><span>入园时间</span><strong>{{ souvenir.entryTime || '未入园' }}</strong></div>
                    <div class="info-row" v-if="souvenir.gateName"><span>入园通道</span><strong>{{ souvenir.gateName }}</strong></div>
                  </div>
                </div>
                <div class="ticket-footer">
                  <span>{{ souvenir.statusDesc }}</span>
                  <span class="ticket-no">NO.{{ souvenir.orderNo }}</span>
                </div>
              </div>
            </div>
            <div class="souvenir-actions">
              <el-button type="primary" @click="downloadSouvenir">保存图片</el-button>
              <el-button @click="souvenirVisible = false">关闭</el-button>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>

    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="editInfoVisible" title="编辑个人资料" width="420px">
      <el-form ref="infoFormRef" :model="infoForm" label-width="80px">
        <el-form-item label="头像">
          <div class="avatar-edit">
            <el-upload
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleEditAvatarChange"
              accept="image/*"
            >
              <img v-if="infoForm.avatarUrl" :src="infoForm.avatarUrl" class="edit-avatar-preview" />
              <el-button v-else size="small" type="primary" plain>选择头像</el-button>
            </el-upload>
            <p v-if="avatarEditUploading" style="font-size:12px;color:#1a73e8;margin-top:4px;">上传中...</p>
          </div>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="infoForm.username" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="infoForm.realName" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="infoForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="infoForm.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editInfoVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveInfo">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="passwordVisible" title="修改密码" width="400px">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { useScenicStore } from '../../stores/scenic'
import { getMyFace, registerFace, deleteMyFace, updateProfile, changePassword, uploadAvatar, getUserOrders, getSouvenirTicket, aiTravelogue, downloadTravelogueFile } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Camera, ArrowDown } from '@element-plus/icons-vue'
import html2canvas from 'html2canvas'

defineOptions({ name: 'Profile' })

const router = useRouter()
const userStore = useUserStore()
const scenicStore = useScenicStore()
const primaryColor80 = computed(() => scenicStore.primaryColor + 'cc')
const activeMenu = ref('info')
const faceData = ref(null)
const registering = ref(false)
const submitting = ref(false)
const videoRef = ref(null)
const capturedImage = ref(null)
const imageBase64 = ref(null)
let stream = null

// 游客角色不显示人脸管理（游客只能在订单页面录入人脸）
const isTourist = computed(() => userStore.userInfo?.roleCode === 'TOURIST')

// 编辑资料
const editInfoVisible = ref(false)
const infoFormRef = ref(null)
const avatarEditUploading = ref(false)
const infoForm = reactive({ username: '', realName: '', phone: '', email: '', avatarUrl: '' })

function openEditInfo() {
  infoForm.username = userStore.userInfo?.username || ''
  infoForm.realName = userStore.userInfo?.realName || ''
  infoForm.phone = userStore.userInfo?.phone || ''
  infoForm.email = userStore.userInfo?.email || ''
  infoForm.avatarUrl = userStore.userInfo?.avatarUrl || ''
  editInfoVisible.value = true
}

async function handleEditAvatarChange(file) {
  if (!file.raw) return
  avatarEditUploading.value = true
  try {
    const res = await uploadAvatar(file.raw)
    infoForm.avatarUrl = res.url
    ElMessage.success('头像上传成功')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '头像上传失败')
  } finally {
    avatarEditUploading.value = false
  }
}

async function handleSaveInfo() {
  try {
    await updateProfile({
      username: infoForm.username,
      realName: infoForm.realName,
      phone: infoForm.phone,
      email: infoForm.email,
      avatarUrl: infoForm.avatarUrl,
    })
    // 更新Store（会同步sessionStorage）
    userStore.updateUserInfo({
      username: infoForm.username,
      realName: infoForm.realName,
      phone: infoForm.phone,
      email: infoForm.email,
      avatarUrl: infoForm.avatarUrl,
    })
    ElMessage.success('资料更新成功')
    editInfoVisible.value = false
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '更新失败')
  }
}

// 修改密码
const passwordVisible = ref(false)
const pwdFormRef = ref(null)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 6, message: '新密码至少6位', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次密码输入不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

function openChangePassword() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  passwordVisible.value = true
}

async function handleChangePassword() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordVisible.value = false
    setTimeout(() => {
      userStore.logout()
      router.push('/login')
    }, 1500)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '密码修改失败')
  }
}

function handleMenuSelect(index) {
  activeMenu.value = index
  if (index === 'face') {
    loadFaceData()
  } else if (index === 'souvenir') {
    loadOrders()
  } else if (index === 'travelogue') {
    loadTravelogueOrders()
  }
}

async function loadFaceData() {
  try {
    const res = await getMyFace()
    faceData.value = res || null
  } catch (e) {
    console.error('加载人脸数据失败', e)
  }
}

async function startRegister() {
  registering.value = true
  capturedImage.value = null
  imageBase64.value = null
  // 延迟启动摄像头，等待 DOM 渲染
  setTimeout(async () => {
    try {
      stream = await navigator.mediaDevices.getUserMedia({ video: true })
      if (videoRef.value) {
        videoRef.value.srcObject = stream
      }
    } catch (e) {
      console.warn('无法访问摄像头', e)
      ElMessage.warning('无法访问摄像头，请检查权限设置')
    }
  }, 100)
}

function cancelRegister() {
  stopCamera()
  registering.value = false
  capturedImage.value = null
  imageBase64.value = null
}

function stopCamera() {
  if (stream) {
    stream.getTracks().forEach(t => t.stop())
    stream = null
  }
}

function capture() {
  if (!videoRef.value) return
  const canvas = document.createElement('canvas')
  canvas.width = videoRef.value.videoWidth || 640
  canvas.height = videoRef.value.videoHeight || 480
  canvas.getContext('2d').drawImage(videoRef.value, 0, 0)
  capturedImage.value = canvas.toDataURL('image/jpeg', 0.9)
  imageBase64.value = capturedImage.value.split(',')[1]
}

function resetCapture() {
  capturedImage.value = null
  imageBase64.value = null
}

async function submitFace() {
  if (!imageBase64.value) {
    ElMessage.warning('请先拍照')
    return
  }
  submitting.value = true
  try {
    await registerFace({ imageBase64: imageBase64.value })
    ElMessage.success('人脸录入成功')
    stopCamera()
    registering.value = false
    await loadFaceData()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '人脸录入失败')
  } finally {
    submitting.value = false
  }
}

async function handleDeleteFace() {
  try {
    await ElMessageBox.confirm('确定要删除已录入的人脸吗？删除后需重新录入。', '确认删除', { type: 'warning' })
    await deleteMyFace()
    ElMessage.success('人脸已删除')
    faceData.value = null
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

// ==================== 纪念票 ====================
const orderLoading = ref(false)
const orders = ref([])
const souvenirVisible = ref(false)
const souvenir = ref(null)
const souvenirRef = ref(null)
const viewingOrder = ref(null)

async function loadOrders() {
  orderLoading.value = true
  try {
    const res = await getUserOrders({ page: 1, size: 50 })
    orders.value = (res.records || []).filter(o => [1, 5, 6].includes(o.status))
  } catch (e) { console.error(e) }
  finally { orderLoading.value = false }
}

async function viewSouvenir(orderNo) {
  viewingOrder.value = orderNo
  try {
    souvenir.value = await getSouvenirTicket(orderNo)
    souvenirVisible.value = true
    await nextTick()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '获取纪念票失败')
  }
}

async function downloadSouvenir() {
  if (!souvenirRef.value) return
  try {
    const canvas = await html2canvas(souvenirRef.value, { scale: 2, backgroundColor: '#f5f0e8', useCORS: true })
    const link = document.createElement('a')
    link.download = `纪念票_${souvenir.value.orderNo}.png`
    link.href = canvas.toDataURL('image/png')
    link.click()
    ElMessage.success('纪念票已保存')
  } catch (e) {
    ElMessage.error('保存失败，请重试')
  }
}

// ==================== AI游记 ====================
const tlLoading = ref(false)
const tlOrders = ref([])
const tlGenerating = ref(false)
const travelogueStyle = ref('literary')
const travelogueContent = ref('')
const travelogueRef = ref(null)
const tlDownloading = ref(false)

async function loadTravelogueOrders() {
  tlLoading.value = true
  try {
    const res = await getUserOrders({ page: 1, size: 50 })
    tlOrders.value = (res.records || []).filter(o => [5, 6].includes(o.status))
  } catch (e) { console.error(e) }
  finally { tlLoading.value = false }
}

async function generateTravelogue(orderNo) {
  travelogueContent.value = ''
  tlGenerating.value = true
  try {
    const response = await aiTravelogue({ orderNo, style: travelogueStyle.value })
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const events = buffer.split('\n\n')
      buffer = events.pop() || ''
      for (const event of events) {
        const dataLine = event.split('\n').find(l => l.startsWith('data:'))
        if (!dataLine) continue
        try {
          const data = JSON.parse(dataLine.slice(5).trim())
          if (data.type === 'text' && data.delta) {
            travelogueContent.value += data.delta
          } else if (data.type === 'done') {
            ElMessage.success('游记生成完毕！')
          } else if (data.type === 'error') {
            ElMessage.error(data.delta || '生成失败')
          }
        } catch (e) { /* parse error */ }
      }
    }
  } catch (e) {
    ElMessage.error('AI服务暂时不可用，请稍后重试')
  } finally {
    tlGenerating.value = false
  }
}

function copyTravelogue() {
  const text = travelogueContent.value.replace(/[#*`>]/g, '')
  navigator.clipboard.writeText(text).then(() => ElMessage.success('已复制到剪贴板'))
}

async function downloadTravelogue(format) {
  if (tlDownloading.value) return
  const dateStr = new Date().toISOString().slice(0, 10)
  const content = travelogueContent.value
  if (!content) return

  // TXT: 纯前端 — 去掉 markdown 符号
  if (format === 'txt') {
    const text = content.replace(/[#*`>_~]/g, '')
    const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
    triggerDownload(blob, `游记_${dateStr}.txt`)
    ElMessage.success('TXT 已下载')
    return
  }

  // Image: 纯前端 — html2canvas 截图
  if (format === 'image') {
    await downloadTravelogueImage(dateStr)
    return
  }

  // Word: 调用后端
  tlDownloading.value = true
  try {
    const response = await downloadTravelogueFile({ content, format })
    if (!response.ok) throw new Error(await response.text())
    const blob = await response.blob()
    triggerDownload(blob, `游记_${dateStr}.docx`)
    ElMessage.success('Word 已下载')
  } catch (e) {
    ElMessage.error('下载失败，请稍后重试')
  } finally {
    tlDownloading.value = false
  }
}

/** 图片下载 — html2canvas 截图游记内容区（临时解除高度限制以捕获完整内容） */
async function downloadTravelogueImage(dateStr) {
  if (!travelogueRef.value) return
  tlDownloading.value = true
  const el = travelogueRef.value
  const origMaxHeight = el.style.maxHeight
  const origOverflow = el.style.overflowY
  // 临时解除高度限制，捕获完整内容
  el.style.maxHeight = 'none'
  el.style.overflowY = 'visible'
  try {
    const canvas = await html2canvas(el, {
      scale: 2,
      backgroundColor: '#ffffff',
      useCORS: true,
    })
    canvas.toBlob(blob => {
      triggerDownload(blob, `游记_${dateStr}.png`)
      ElMessage.success('图片已保存')
      tlDownloading.value = false
    }, 'image/png')
  } catch (e) {
    ElMessage.error('图片生成失败')
    tlDownloading.value = false
  } finally {
    // 恢复原始样式
    el.style.maxHeight = origMaxHeight
    el.style.overflowY = origOverflow
  }
}

/** 通用触发下载 */
function triggerDownload(blob, filename) {
  const link = document.createElement('a')
  link.download = filename
  link.href = URL.createObjectURL(blob)
  link.click()
  URL.revokeObjectURL(link.href)
}

function renderMarkdown(md) {
  return md
    .replace(/### (.+)/g, '<h4>$1</h4>')
    .replace(/## (.+)/g, '<h3>$1</h3>')
    .replace(/# (.+)/g, '<h2>$1</h2>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/\n/g, '<br>')
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

onMounted(() => {
  if (activeMenu.value === 'face') {
    loadFaceData()
  }
  setTimeout(setupObserver, 200)
})

onUnmounted(() => {
  stopCamera()
})
</script>

<style scoped>
/* ====== CSS Variables ====== */
:root {
  --radius: 16px;
  --shadow-sm: 0 1px 3px rgba(0,0,0,0.06);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.08);
  --shadow-lg: 0 12px 40px rgba(0,0,0,0.12);
  --transition: 0.35s cubic-bezier(0.22, 0.61, 0.36, 1);
}
.profile-page { margin: -24px; }

/* ====== 页面头部 ====== */
.page-hero {
  padding: 60px 24px 50px;
  text-align: center;
}
.page-hero .section-tag { color: rgba(255,255,255,0.7); }
.page-hero .section-title { color: #fff; }
.page-hero .section-subtitle { color: rgba(255,255,255,0.8); }

.section-header { margin-bottom: 0; }
.section-tag {
  display: inline-block; font-size: 12px; font-weight: 700; letter-spacing: 3px;
  margin-bottom: 12px;
}
.section-title {
  font-size: clamp(28px, 4vw, 38px); font-weight: 800; margin: 0 0 12px;
  letter-spacing: -0.5px;
}
.section-subtitle { font-size: 16px; margin: 0; }

/* ====== 内容区 ====== */
.content-wrapper { background: transparent; padding: 0 24px 80px; }
.section { max-width: 1000px; margin: 0 auto; padding-top: 40px; }

/* 入场动画 */
[v-observe] { opacity: 0; transform: translateY(30px); transition: opacity 0.7s ease, transform 0.7s cubic-bezier(0.22, 0.61, 0.36, 1); }
[v-observe].visible { opacity: 1; transform: translateY(0); }

/* ====== 布局 ====== */
.profile-layout { display: flex; gap: 24px; }

/* ====== 左侧用户卡片 ====== */
.profile-sidebar { width: 280px; flex-shrink: 0; }
.user-card {
  background: rgba(255,255,255,0.85); backdrop-filter: blur(10px);
  border-radius: 20px; overflow: hidden;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition);
}
.user-card:hover { box-shadow: var(--shadow-md); }
.user-card-cover { height: 80px; }
.user-card-body {
  text-align: center; padding: 0 20px 20px;
  margin-top: -40px; position: relative;
}
.user-avatar-wrap {
  display: inline-block; margin-bottom: 12px;
}
.user-avatar {
  width: 80px; height: 80px; border-radius: 50%;
  border: 4px solid #fff; box-shadow: var(--shadow-md);
  object-fit: cover; display: block;
}
.user-avatar--placeholder {
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 28px; font-weight: 700;
}
.user-name { font-size: 18px; font-weight: 700; color: #1a1a2e; margin: 0 0 6px; }
.user-role { font-size: 13px; font-weight: 600; }

.user-menu { padding: 0 12px 16px; display: flex; flex-direction: column; gap: 4px; }
.menu-item {
  display: flex; align-items: center; gap: 10px;
  border: none; border-radius: 12px; padding: 12px 16px;
  background: transparent; color: #64748b;
  font-size: 14px; font-weight: 600; cursor: pointer;
  transition: all var(--transition); text-align: left; width: 100%;
}
.menu-item:hover { background: #f8fafc; color: #1a1a2e; }
.menu-item.active {
  background: v-bind('scenicStore.primaryColor'); color: #fff;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.menu-icon { font-size: 18px; }

/* ====== 右侧内容区 ====== */
.profile-main { flex: 1; min-width: 0; }
.content-card {
  background: rgba(255,255,255,0.85); backdrop-filter: blur(10px);
  border-radius: 20px; overflow: hidden;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition);
}
.content-card:hover { box-shadow: var(--shadow-md); }
.content-card-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 28px; border-bottom: 1px solid #f1f5f9;
}
.content-card-header h3 { font-size: 17px; font-weight: 700; color: #1a1a2e; margin: 0; }
.content-card-badge {
  font-size: 11px; font-weight: 600; color: #67C23A;
  background: #e8f8e8; padding: 3px 10px; border-radius: 10px;
}
.content-card-body { padding: 24px 28px; }
.content-card-actions { margin-top: 20px; display: flex; gap: 12px; flex-wrap: wrap; }

/* 描述列表 */
.info-descriptions {
  --el-descriptions-item-bordered-label-background: #f8fafc;
}

/* 通用按钮 */
.profile-btn {
  border: none; border-radius: 10px; padding: 10px 22px;
  font-size: 14px; font-weight: 600; cursor: pointer;
  transition: all var(--transition); display: inline-flex; align-items: center; gap: 6px;
}
.profile-btn--primary {
  background: v-bind('scenicStore.primaryColor'); color: #fff;
}
.profile-btn--primary:hover:not(:disabled) { filter: brightness(1.1); transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
.profile-btn--primary:disabled { opacity: 0.5; cursor: not-allowed; }
.profile-btn--outline {
  background: transparent; color: #606266; border: 1px solid #dcdfe6;
}
.profile-btn--outline:hover { border-color: v-bind('scenicStore.primaryColor'); color: v-bind('scenicStore.primaryColor'); }
.profile-btn--success {
  background: #67C23A; color: #fff;
}
.profile-btn--success:hover:not(:disabled) { filter: brightness(1.1); transform: translateY(-1px); }
.profile-btn--success:disabled { opacity: 0.5; cursor: not-allowed; }
.profile-btn--danger {
  background: transparent; color: #F56C6C; border: 1px solid #F56C6C;
}
.profile-btn--danger:hover { background: #F56C6C; color: #fff; }
.profile-btn--ghost {
  background: transparent; color: #909399;
}
.profile-btn--ghost:hover { color: #606266; background: #f5f5f5; }

/* ====== 人脸管理 ====== */
.face-empty-state { text-align: center; padding: 40px 20px; }
.face-empty-icon { font-size: 56px; margin-bottom: 12px; }
.face-empty-title { font-size: 16px; font-weight: 600; color: #1a1a2e; margin: 0 0 8px; }
.face-empty-desc { color: #94a3b8; font-size: 13px; margin: 0 0 24px; }

.face-register-area { padding: 24px 28px; text-align: center; }
.face-register-tip { color: #606266; font-size: 14px; margin: 0 0 20px; }
.camera-box {
  width: 100%; max-width: 400px; margin: 0 auto 20px;
  border-radius: 16px; overflow: hidden; background: #000;
  box-shadow: var(--shadow-md);
}
.camera-video { width: 100%; display: block; }
.camera-actions { display: flex; gap: 10px; justify-content: center; flex-wrap: wrap; }

.face-done-area { padding: 24px 28px; }
.face-photo-wrap { text-align: center; margin-bottom: 20px; }
.face-photo { width: 150px; height: 150px; object-fit: cover; border-radius: 12px; box-shadow: var(--shadow-md); }

/* 头像编辑 */
.avatar-edit { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.edit-avatar-preview {
  width: 60px; height: 60px; object-fit: cover;
  border-radius: 50%; cursor: pointer; border: 2px solid #e0e0e0;
}

/* ====== 响应式 ====== */
@media (max-width: 768px) {
  .profile-layout { flex-direction: column; }
  .profile-sidebar { width: 100%; }
  .content-card-body { padding: 16px; }
  .camera-actions { gap: 8px; }
  .profile-btn { padding: 8px 16px; font-size: 13px; }
}

/* ====== 纪念票 ====== */
.souvenir-dialog {
  background: transparent; border-radius: 24px; width: 480px; max-width: 90vw;
  display: flex; flex-direction: column; align-items: center;
  position: relative;
}
.souvenir-close {
  position: absolute; top: 8px; right: 8px; z-index: 20;
  width: 36px; height: 36px; border-radius: 50%;
  border: none; background: rgba(0,0,0,0.5); color: #fff;
  font-size: 16px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.3s;
}
.souvenir-close:hover { background: rgba(0,0,0,0.8); transform: rotate(90deg); }
.souvenir-ticket {
  width: 100%; background: #f5f0e8;
  border-radius: 12px; overflow: hidden;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}
.ticket-border {
  border: 2px dashed #c9a96e; border-radius: 10px; margin: 8px; padding: 0;
}
.ticket-header {
  background: linear-gradient(135deg, #1a1a2e, #2d3a4a);
  color: #fff; text-align: center; padding: 20px 24px; position: relative;
}
.ticket-header h2 { margin: 0; font-size: 22px; font-weight: 800; letter-spacing: 2px; }
.ticket-badge {
  position: absolute; top: 12px; right: 16px;
  font-size: 10px; background: #c9a96e; color: #fff;
  padding: 3px 10px; border-radius: 20px; letter-spacing: 1px;
}
.ticket-body { padding: 20px 24px; }
.ticket-spot-imgs {
  display: grid; grid-template-columns: 1fr 1fr; gap: 8px;
  margin-bottom: 16px;
}
.ticket-spot-imgs img {
  width: 100%; height: 100px; object-fit: cover; border-radius: 8px;
}
.ticket-info { display: flex; flex-direction: column; gap: 8px; }
.info-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 6px 0; border-bottom: 1px dashed #e0d5c0;
}
.info-row span { font-size: 13px; color: #8b7355; }
.info-row strong { font-size: 14px; color: #1a1a2e; }
.ticket-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 24px; background: #ece3d5; font-size: 12px; color: #8b7355;
  border-top: 1px solid #d5c9b0;
}
.ticket-no { font-family: monospace; letter-spacing: 1px; }
.souvenir-actions {
  display: flex; gap: 12px; margin-top: 16px; justify-content: center;
}

/* ====== 订单列表 ====== */
.order-list { display: flex; flex-direction: column; gap: 10px; }
.order-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 18px; background: #f8fafc; border-radius: 12px;
  transition: all 0.3s; gap: 12px;
}
.order-item:hover { background: #f1f5f9; }
.order-item--active { border: 1px solid var(--accent); background: #eff6ff; }
.order-item-info { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; }
.order-no { font-family: monospace; font-size: 13px; color: #1a1a2e; font-weight: 600; }
.order-date { font-size: 13px; color: #64748b; }
.order-amount { font-size: 14px; font-weight: 600; color: #E6A23C; }
.order-item-actions { display: flex; align-items: center; flex-shrink: 0; }

/* ====== AI游记 ====== */
.travelogue-result { margin-top: 20px; }
.travelogue-actions { display: flex; gap: 8px; margin-bottom: 16px; }
.travelogue-content {
  background: #fff; border: 1px solid #e8ecf1; border-radius: 12px;
  padding: 24px; max-height: 500px; overflow-y: auto;
  font-size: 15px; line-height: 2; color: #1a1a2e;
}
.travelogue-content :deep(h2) { font-size: 20px; margin-bottom: 16px; color: #1a1a2e; }
.travelogue-content :deep(h3) { font-size: 17px; margin: 16px 0 8px; color: #334155; }
.travelogue-content :deep(h4) { font-size: 15px; margin: 12px 0 6px; color: #475569; }
.travelogue-content :deep(strong) { color: #0f172a; }

/* 弹窗覆盖样式 */
.modal-fade-enter-active { animation: modalIn 0.3s ease; }
.modal-fade-leave-active { animation: modalIn 0.2s ease reverse; }
@keyframes modalIn { from { opacity: 0; } to { opacity: 1; } }
.modal-overlay {
  position: fixed; inset: 0; z-index: 2000;
  background: rgba(0,0,0,0.55); backdrop-filter: blur(6px);
  display: flex; align-items: center; justify-content: center; padding: 24px;
}
</style>
