<template>
  <div class="group-page">
    <!-- ====== 页面头部 ====== -->
    <div class="page-hero">
      <div class="section-header">
        <span class="section-tag">GROUP</span>
        <h2 class="section-title">团体票购买</h2>
        <p class="section-subtitle">下载模板批量导入，高效管理团队出行</p>
      </div>
    </div>

    <div class="content-wrapper">
      <!-- 暂停运营提示 -->
      <div class="suspend-banner" v-if="scenicStore.scenicStatus === 0">
        <div class="suspend-icon">⚠️</div>
        <div>
          <div class="suspend-title">景区暂停运营</div>
          <div class="suspend-desc">当前景区处于暂停运营状态，暂不支持团体购票，请稍后再试</div>
        </div>
      </div>

      <!-- 步骤提示 -->
      <div class="section section-steps" v-observe>
        <div class="steps-card">
          <el-steps :active="step" finish-status="success" align-center>
            <el-step title="下载模板" />
            <el-step title="填写信息" />
            <el-step title="上传名单" />
            <el-step title="完成" />
          </el-steps>
        </div>
      </div>

      <!-- 步骤1：下载模板 -->
      <div v-if="step === 0" class="section" v-observe>
        <div class="step-card step-illustration">
          <div class="step-icon-wrap">
            <el-icon :size="56"><Document /></el-icon>
          </div>
          <h3>下载团体成员名单模板</h3>
          <p class="step-desc">请下载Excel模板，填写团体成员信息（姓名、身份证号、手机号、人脸照片）后上传</p>
          <button class="hero-btn" @click="downloadTpl" :disabled="downloading || scenicStore.scenicStatus === 0">
            <el-icon><Download /></el-icon>
            <span>{{ downloading ? '下载中...' : '下载Excel模板' }}</span>
          </button>
          <div class="step-notice">
            ⚠️ 团体名单需包含至少<strong>{{ minGroupSize }}名</strong>成员，请确保上传的Excel文件中填写了足够的人数
          </div>
          <div class="step-actions">
            <button class="next-btn" @click="step = 1">我已填好 下一步</button>
          </div>
        </div>
      </div>

      <!-- 步骤2：填写基本信息 -->
      <div v-if="step === 1" class="section" v-observe>
        <div class="step-card">
          <h3 class="form-title">填写团体信息</h3>
          <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" class="group-form">
            <el-form-item label="团体名称" prop="groupName">
              <el-input v-model="form.groupName" placeholder="请输入团体名称（如：XX公司团建）" size="large" />
            </el-form-item>
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="form.contactName" placeholder="请输入联系人姓名" size="large" />
            </el-form-item>
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系人手机号" size="large" />
            </el-form-item>
            <el-form-item label="游览日期" prop="visitDate">
              <el-date-picker v-model="form.visitDate" type="date" placeholder="选择游览日期" :disabled-date="disabledDate" size="large" style="width: 100%;" />
            </el-form-item>
          </el-form>
          <div class="step-actions">
            <button class="ghost-btn" @click="step = 0">上一步</button>
            <button class="next-btn" :disabled="scenicStore.scenicStatus === 0" @click="goToStep2">下一步</button>
          </div>
        </div>
      </div>

      <!-- 步骤3：上传名单 -->
      <div v-if="step === 2" class="section" v-observe>
        <div class="step-card">
          <h3 class="form-title">上传团体名单</h3>
          <p class="step-desc">请确保已使用正确的模板格式填写了所有必填项（序号、姓名、身份证号、手机号），空行会被自动跳过，团体名单需至少{{ minGroupSize }}人。</p>
          <div class="upload-area">
            <el-upload
              :auto-upload="false"
              :limit="1"
              accept=".xlsx,.xls"
              :on-change="handleFileChange"
              :on-remove="() => selectedFile = null"
              drag
            >
              <el-icon class="upload-icon"><Document /></el-icon>
              <div class="upload-text">
                <span>点击或拖拽文件到此区域</span>
                <span class="upload-hint">仅支持 .xlsx / .xls 格式</span>
              </div>
            </el-upload>
          </div>
          <div class="step-notice">
            ⚠️ 请确保上传的Excel文件中填写了足够的人数，否则会导致审核失败
          </div>
          <div class="step-actions">
            <button class="ghost-btn" @click="step = 1">上一步</button>
            <button class="next-btn" @click="handleImport" :disabled="importing || scenicStore.scenicStatus === 0">
              {{ importing ? '提交中...' : '提交导入' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 步骤4：完成 -->
      <div v-if="step === 3" class="section" v-observe>
        <div class="step-card step-illustration">
          <div class="step-icon-wrap step-icon-wrap--success">
            <el-icon :size="56"><CircleCheck /></el-icon>
          </div>
          <h3 class="success-title">团体名单提交成功！</h3>
          <p class="step-desc">您的团体票订单已创建，状态为<strong>待审核</strong></p>
          <p class="step-hint">管理员审核通过后，您可以在「我的订单」中完成支付，支付成功后系统将自动上传人脸信息。</p>
          <button class="hero-btn" @click="$router.push('/orders')">查看我的订单</button>
        </div>
      </div>

      <!-- 底部返回 -->
      <div class="section section-back" v-observe>
        <button class="ghost-btn" @click="$router.push('/tickets')">返回门票选购</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useScenicStore } from '../../stores/scenic'
import { downloadTemplate, importGroup, getConfig, getTicketTypes } from '../../api'
import { ElMessage } from 'element-plus'
import { Document, Download, CircleCheck } from '@element-plus/icons-vue'

const scenicStore = useScenicStore()

const step = ref(0)
const formRef = ref(null)
const downloading = ref(false)
const importing = ref(false)
const selectedFile = ref(null)

const form = reactive({
  groupName: '',
  contactName: '',
  contactPhone: '',
  visitDate: null,
})

const formRules = {
  groupName: [{ required: true, message: '请输入团体名称', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  visitDate: [{ required: true, message: '请选择游览日期', trigger: 'change' }],
}

const maxBookingDays = ref(14)
const minGroupSize = ref(10)

function disabledDate(time) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const maxDate = new Date(today)
  maxDate.setDate(today.getDate() + maxBookingDays.value)
  return time.getTime() < today.getTime() || time.getTime() > maxDate.getTime()
}

function goToStep2() {
  formRef.value.validate(valid => {
    if (valid) step.value = 2
  })
}

async function downloadTpl() {
  downloading.value = true
  try {
    const blob = await downloadTemplate()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'group_member_template.xlsx'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('模板下载成功')
  } catch (e) {
    console.error(e)
  } finally {
    downloading.value = false
  }
}

function handleFileChange(file) {
  selectedFile.value = file.raw
}

async function handleImport() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择Excel文件')
    return
  }
  importing.value = true
  try {
    const fd = new FormData()
    fd.append('file', selectedFile.value)
    fd.append('groupName', form.groupName)
    fd.append('contactName', form.contactName)
    fd.append('contactPhone', form.contactPhone)
    fd.append('visitDate', formatDate(form.visitDate))
    await importGroup(fd)
    step.value = 3
  } catch (e) {
    console.error(e)
  } finally {
    importing.value = false
  }
}

const formatDate = (d) => {
  if (!d) return ''
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

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
  setTimeout(setupObserver, 200)
  try {
    const cfg = await getConfig()
    if (cfg.bookingDaysGroup) maxBookingDays.value = cfg.bookingDaysGroup
  } catch (e) { /* keep default 14 */ }
  try {
    const types = await getTicketTypes()
    const group = types.find(t => t.isGroup)
    if (group && group.minGroupSize) minGroupSize.value = group.minGroupSize
  } catch (e) { /* keep default 10 */ }
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
.group-page { margin: -24px; }

/* ====== 页面头部 ====== */
.page-hero {
  padding: 56px 24px 46px; text-align: center;
}
.page-hero .section-tag { color: rgba(255,255,255,0.7); }
.page-hero .section-title { color: #fff; }
.page-hero .section-subtitle { color: rgba(255,255,255,0.8); }

/* ====== 内容区 ====== */
.content-wrapper { padding: 0 24px 80px; }
.section { max-width: 860px; margin: 0 auto; padding-top: 60px; }

/* 入场动画 */
[v-observe] { opacity: 0; transform: translateY(30px); transition: opacity 0.7s ease, transform 0.7s cubic-bezier(0.22, 0.61, 0.36, 1); }
[v-observe].visible { opacity: 1; transform: translateY(0); }

/* 分区标题 */
.section-header { margin-bottom: 0; }
.section-tag {
  display: inline-block; font-size: 12px; font-weight: 700; letter-spacing: 3px;
  margin-bottom: 12px;
}
.section-title {
  font-size: clamp(28px, 4vw, 38px); font-weight: 800; color: #1a1a2e; margin: 0 0 12px;
  letter-spacing: -0.5px;
}
.section-subtitle { color: #94a3b8; font-size: 16px; margin: 0; }

/* ====== 步骤条卡片 ====== */
.section-steps { padding-top: 40px; }
.steps-card {
  background: rgba(255,255,255,0.85); backdrop-filter: blur(10px);
  border-radius: 20px; padding: 32px 28px;
  box-shadow: var(--shadow-sm);
}

/* ====== 步骤内容卡片 ====== */
.step-card {
  background: rgba(255,255,255,0.85); backdrop-filter: blur(10px);
  border-radius: 20px; padding: 40px 32px;
  box-shadow: var(--shadow-sm); transition: all var(--transition);
}
.step-card:hover { box-shadow: var(--shadow-md); }

/* 插图型步骤 */
.step-illustration { text-align: center; }
.step-illustration h3 {
  font-size: 20px; font-weight: 700; color: #1a1a2e; margin: 0 0 12px;
}
.step-illustration .success-title {
  font-size: 20px; font-weight: 700; color: #67C23A; margin: 0 0 12px;
}
.step-icon-wrap {
  width: 88px; height: 88px; margin: 0 auto 24px;
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #ecf5ff, #d9ecff); color: #409EFF;
}
.step-icon-wrap--success {
  background: linear-gradient(135deg, #f0f9eb, #e1f3d8); color: #67C23A;
}
.step-desc { color: #64748b; font-size: 14px; line-height: 1.7; margin: 0 0 28px; }
.step-hint { color: #94a3b8; font-size: 13px; margin: 0 0 28px; }
.step-notice {
  background: #fef8e8; border: 1px solid #faecd8; border-radius: 12px;
  padding: 16px 24px; font-size: 13px; color: #b88230; text-align: center;
  margin: 28px 0;
}
.step-actions {
  display: flex; justify-content: center; gap: 12px; flex-wrap: wrap;
  margin-top: 28px;
}

/* ====== 表单 ====== */
.form-title {
  font-size: 18px; font-weight: 700; color: #1a1a2e; margin: 0 0 28px;
}
.group-form {
  max-width: 520px;
}
.group-form :deep(.el-form-item__label) {
  font-weight: 600; color: #1a1a2e;
}
.group-form :deep(.el-input__wrapper) {
  border-radius: 12px; box-shadow: 0 0 0 1px #e4e7ed inset;
}
.group-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c0c4cc inset;
}
.group-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px v-bind('scenicStore.primaryColor') inset;
}

/* ====== 上传区域 ====== */
.upload-area {
  margin-bottom: 24px;
}
.upload-area :deep(.el-upload-dragger) {
  border-radius: 16px; border: 2px dashed #d9d9d9;
  padding: 40px 20px; background: #fafafa;
  transition: all 0.3s;
}
.upload-area :deep(.el-upload-dragger:hover) {
  border-color: v-bind('scenicStore.primaryColor');
  background: rgba(255,255,255,0.6);
}
.upload-icon { font-size: 48px; color: #c0c4cc; margin-bottom: 16px; }
.upload-text {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
}
.upload-text span { font-size: 15px; color: #606266; }
.upload-hint { font-size: 12px !important; color: #c0c4cc !important; }

/* ====== 按钮 ====== */
.hero-btn {
  display: inline-flex; align-items: center; gap: 8px;
  border: none; border-radius: 12px; padding: 14px 32px; margin-bottom: 8px;
  background: v-bind('scenicStore.primaryColor'); color: #fff;
  font-size: 16px; font-weight: 600; cursor: pointer;
  transition: all var(--transition);
}
.hero-btn:hover:not(:disabled) {
  filter: brightness(1.1); transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0,0,0,0.15);
}
.hero-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.next-btn {
  border: none; border-radius: 12px; padding: 12px 28px;
  background: v-bind('scenicStore.primaryColor'); color: #fff;
  font-size: 15px; font-weight: 600; cursor: pointer;
  transition: all var(--transition);
}
.next-btn:hover:not(:disabled) {
  filter: brightness(1.1); transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}
.next-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.ghost-btn {
  border: 2px solid #e4e7ed; border-radius: 12px; padding: 12px 28px;
  background: #fff; color: #606266;
  font-size: 15px; font-weight: 600; cursor: pointer;
  transition: all var(--transition);
}
.ghost-btn:hover {
  border-color: v-bind('scenicStore.primaryColor');
  color: v-bind('scenicStore.primaryColor');
}

.section-back { text-align: center; padding-top: 40px; padding-bottom: 40px; }
/* 暂停运营横幅 */
.suspend-banner {
  display: flex; align-items: center; gap: 16px;
  background: linear-gradient(135deg, #fff5f5, #fef0f0);
  border: 1px solid #f56c6c; border-radius: 12px;
  padding: 20px 28px; margin-bottom: 24px;
}
.suspend-icon { font-size: 32px; }
.suspend-title { font-size: 16px; font-weight: 700; color: #f56c6c; margin-bottom: 4px; }
.suspend-desc { font-size: 13px; color: #909399; }
</style>
