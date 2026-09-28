<template>
  <div class="face-page">
    <h2 class="page-title">人脸录入</h2>
    <el-card>
      <el-steps :active="step" align-center style="margin-bottom: 32px;">
        <el-step title="拍照/上传" />
        <el-step title="质量检测" />
        <el-step title="注册完成" />
      </el-steps>

      <div v-if="step === 0" class="step-content">
        <p class="tip">请使用摄像头拍照或上传一张正面免冠照片用于入园人脸识别</p>
        <div v-if="orderNo" class="order-info">
          <el-alert type="info" :closable="false">
            关联订单：{{ orderNo }}
          </el-alert>
        </div>
        <div class="camera-area">
          <video ref="videoRef" autoplay playsinline class="camera-view" v-show="!capturedImage"></video>
          <img v-if="capturedImage" :src="capturedImage" class="camera-view" />
        </div>
        <div class="actions">
          <el-button type="primary" @click="capture" :disabled="capturedImage">拍照</el-button>
          <el-upload :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="handleUpload">
            <el-button>本地上传</el-button>
          </el-upload>
          <el-button v-if="capturedImage" @click="resetCapture">重新拍照</el-button>
          <el-button v-if="capturedImage" type="success" @click="submitFace">提交录入</el-button>
        </div>
      </div>

      <div v-if="step === 1" class="step-content">
        <el-icon :size="60" class="loading-icon"><Loading /></el-icon>
        <p>正在进行人脸质量检测，请稍候...</p>
      </div>

      <div v-if="step === 2" class="step-content">
        <el-icon :size="60" color="#67C23A"><CircleCheckFilled /></el-icon>
        <h3>人脸录入成功！</h3>
        <p>您可以在游览当日刷脸入园</p>
        <el-button v-if="orderNo" type="primary" @click="$router.push('/orders')">查看订单</el-button>
        <el-button v-else type="primary" @click="$router.push('/profile')">返回个人中心</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { registerFace } from '../../api'
import { ElMessage } from 'element-plus'
import { Loading, CircleCheckFilled } from '@element-plus/icons-vue'

const route = useRoute()
const step = ref(0)
const videoRef = ref(null)
const capturedImage = ref(null)
const imageBase64 = ref(null)
const orderNo = ref(route.query.orderNo || '')
let stream = null

onMounted(async () => {
  try {
    stream = await navigator.mediaDevices.getUserMedia({ video: true })
    if (videoRef.value) {
      videoRef.value.srcObject = stream
    }
  } catch (e) {
    console.warn('无法访问摄像头，请使用本地上传', e)
  }
})

onUnmounted(() => {
  if (stream) {
    stream.getTracks().forEach(t => t.stop())
  }
})

function capture() {
  if (!videoRef.value) return
  const canvas = document.createElement('canvas')
  canvas.width = videoRef.value.videoWidth
  canvas.height = videoRef.value.videoHeight
  canvas.getContext('2d').drawImage(videoRef.value, 0, 0)
  capturedImage.value = canvas.toDataURL('image/jpeg')
  imageBase64.value = canvas.toDataURL('image/jpeg').split(',')[1]
}

function handleUpload(file) {
  const reader = new FileReader()
  reader.onload = (e) => {
    capturedImage.value = e.target.result
    imageBase64.value = e.target.result.split(',')[1]
  }
  reader.readAsDataURL(file.raw)
}

function resetCapture() {
  capturedImage.value = null
  imageBase64.value = null
}

async function submitFace() {
  if (!imageBase64.value) {
    ElMessage.warning('请先拍照或上传照片')
    return
  }

  step.value = 1

  try {
    const payload = { imageBase64: imageBase64.value }
    if (orderNo.value) {
      payload.orderNo = orderNo.value
    }

    await registerFace(payload)
    step.value = 2
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '人脸录入失败，请重试')
    step.value = 0
  }
}
</script>

<style scoped>
.face-page {
  max-width: 700px;
  margin: 0 auto;
}

.page-title {
  font-size: 24px;
  margin-bottom: 24px;
  color: #333;
}

.step-content {
  text-align: center;
  padding: 40px 20px;
}

.tip {
  color: #666;
  margin-bottom: 16px;
}

.order-info {
  max-width: 400px;
  margin: 0 auto 16px;
}

.camera-area {
  width: 100%;
  max-width: 400px;
  margin: 0 auto 24px;
  border-radius: 12px;
  overflow: hidden;
  background-color: #000;
}

.camera-view {
  width: 100%;
  display: block;
}

.actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.loading-icon {
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

h3 {
  margin: 16px 0 8px;
  color: #333;
}
</style>
