<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>团体票管理</h3>
          <div class="header-actions">
            <el-button v-if="selectedIds.length > 0" type="danger" @click="handleBatchDelete">
              批量删除 ({{ selectedIds.length }})
            </el-button>
            <el-button @click="downloadTpl">下载Excel模板</el-button>
            <el-button type="primary" @click="importVisible = true">上传团体名单</el-button>
          </div>
        </div>
      </template>
      <el-table :data="groups" v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="groupName" label="团体名称" width="100" />
        <el-table-column prop="contactName" label="联系人" />
        <el-table-column prop="contactPhone" label="联系电话" width="120" />
        <el-table-column prop="visitDate" label="游览日期" width="120" />
        <el-table-column prop="totalCount" label="人数" width="80" />
        <el-table-column label="金额" width="80">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column label="入园" width="80">
          <template #default="{ row }">{{ row.enteredCount }}/{{ row.totalCount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
            <el-button size="small" type="warning" @click="openEditDialog(row)" v-if="row.status !== 3 && row.status !== 2">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)" v-if="row.status !== 3 && row.status !== 2">删除</el-button>
            <el-button v-if="row.status === 0 || row.status === 4" size="small" type="success" @click="handleAudit(row, true)">通过</el-button>
            <el-button v-if="row.status === 0 || row.status === 4" size="small" type="danger" @click="handleAudit(row, false)">拒绝</el-button>
            <el-button v-if="row.status === 1" size="small" type="primary" @click="handlePay(row)">支付</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev, pager, next" @current-change="loadGroups" style="margin-top: 16px; justify-content: center;" />
    </el-card>

    <!-- 导入弹窗 -->
    <el-dialog v-model="importVisible" title="上传团体名单" width="500px">
      <el-form ref="importFormRef" :model="importForm" :rules="importRules" label-width="100px">
        <el-form-item label="团体名称" prop="groupName">
          <el-input v-model="importForm.groupName" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="importForm.contactName" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="importForm.contactPhone" />
        </el-form-item>
        <el-form-item label="游览日期" prop="visitDate">
          <el-date-picker v-model="importForm.visitDate" type="date" style="width: 100%;" :disabled-date="disablePastDate" placeholder="请选择日期" />
        </el-form-item>
        <el-form-item label="Excel文件" prop="file">
          <el-upload :auto-upload="false" :limit="1" accept=".xlsx,.xls" :on-change="handleFileChange">
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div style="font-size: 12px; color: #999; margin-top: 4px; display: flex; align-items: center; gap: 4px;">
                <span>支持 .xlsx / .xls 格式</span>
                <el-tag size="small" type="warning" effect="plain">团体名单至少{{ minGroupSize }}人</el-tag>
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImport" :loading="importing">导入</el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑团体信息" width="500px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="团体名称" prop="groupName">
          <el-input v-model="editForm.groupName" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="editForm.contactName" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="editForm.contactPhone" />
        </el-form-item>
        <el-form-item label="游览日期" prop="visitDate">
          <el-date-picker v-model="editForm.visitDate" type="date" style="width: 100%;" :disabled-date="disablePastDate" placeholder="请选择日期" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEdit" :loading="editing">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="团体详情" width="800px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="团体名称">{{ detail.groupName }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="游览日期">{{ detail.visitDate }}</el-descriptions-item>
        <el-descriptions-item label="总人数">{{ detail.totalCount }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ detail.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="已入园">{{ detail.enteredCount }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.status === 2 && detail.auditRemark" label="拒绝理由" :span="2">{{ detail.auditRemark }}</el-descriptions-item>
      </el-descriptions>
      <h4 style="margin: 16px 0;">成员列表</h4>
      <el-table :data="detail?.members || []" size="small">
        <el-table-column prop="realName" label="姓名" width="90" />
        <el-table-column prop="idCard" label="身份证号" width="170" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column label="人脸状态" width="140">
          <template #default="{ row }">
            <div v-if="row.faceStatus === 1" style="display: flex; align-items: center; gap: 6px;">
              <el-image v-if="row.faceImagePath" :src="row.faceImagePath" :preview-src-list="[row.faceImagePath]" style="width: 36px; height: 36px; border-radius: 4px;" fit="cover" preview-teleported />
              <el-tag size="small" type="success">已录入</el-tag>
            </div>
            <el-tooltip v-else-if="row.faceStatus === 2" :content="row.failReason || '未知错误'" placement="top">
              <div style="display: flex; align-items: center; gap: 6px;">
                <el-image v-if="row.faceImagePath" :src="row.faceImagePath" :preview-src-list="[row.faceImagePath]" style="width: 36px; height: 36px; border-radius: 4px;" fit="cover" preview-teleported />
                <el-tag size="small" type="danger">录入失败</el-tag>
              </div>
            </el-tooltip>
            <el-tag v-else size="small" type="warning">待录入</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="入园" width="80">
          <template #default="{ row }">
            <el-tag :type="row.entryStatus ? 'success' : 'info'" size="small">{{ row.entryStatus ? '已入园' : '未入园' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <div style="display: flex; gap: 4px; align-items: center;">
              <el-button
                v-if="row.faceStatus === 2 && detail?.status === 3"
                size="small" type="warning" link
                @click="triggerReupload(row)"
                :loading="reuploadingMemberId === row.id"
              >重传</el-button>
              <el-popconfirm
                v-if="detail?.status !== 3 && row.entryStatus !== 1"
                title="确定删除该成员？"
                @confirm="deleteMember(row.id)"
                confirm-button-text="确认"
                cancel-button-text="取消"
              >
                <template #reference>
                  <el-button size="small" type="danger" link>删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <!-- 隐藏的文件选择器用于重传照片 -->
      <input
        ref="reuploadInput"
        type="file" accept="image/*" style="display: none;"
        @change="handleReuploadFile"
      />
    </el-dialog>

    <!-- 人脸录入进度弹窗 -->
    <el-dialog v-model="progressVisible" title="人脸录入进度" width="460px" :close-on-click-modal="false" :show-close="progressData?.status === 'COMPLETED' || progressData?.status === 'FAILED' || progressData?.status === 'NOT_STARTED'">
      <div v-if="progressData" style="display: flex; flex-direction: column; align-items: center; gap: 16px;">
        <el-icon v-if="progressData.status === 'RUNNING'" class="is-loading" :size="48" color="#409eff">
          <Loading />
        </el-icon>
        <el-icon v-else-if="progressData.status === 'COMPLETED'" :size="48" color="#67c23a">
          <CircleCheckFilled />
        </el-icon>
        <el-icon v-else-if="progressData.status === 'FAILED'" :size="48" color="#f56c6c">
          <CircleCloseFilled />
        </el-icon>

        <div style="font-size: 15px; font-weight: 500; text-align: center;">
          <template v-if="progressData.status === 'RUNNING'">
            正在为 <span style="color: #409eff;">{{ progressData.currentName || '...' }}</span> 录入人脸
          </template>
          <template v-else-if="progressData.status === 'COMPLETED'">
            录入完成！成功 {{ progressData.successCount }} 人<template v-if="progressData.failedCount > 0">，失败 {{ progressData.failedCount }} 人</template>
          </template>
          <template v-else-if="progressData.status === 'FAILED'">
            录入失败：{{ progressData.errorMessage || '未知错误' }}
          </template>
          <template v-else>
            等待开始...
          </template>
        </div>

        <div style="width: 100%;">
          <el-progress
            v-if="progressData.totalMembers > 0"
            :percentage="progressData.totalMembers > 0 ? Math.round(progressData.completedCount / progressData.totalMembers * 100) : 0"
            :status="progressData.status === 'FAILED' ? 'exception' : progressData.status === 'COMPLETED' ? 'success' : undefined"
            :stroke-width="18"
            :text-inside="true"
          />
        </div>

        <div style="display: flex; gap: 24px; font-size: 13px; color: #606266;">
          <span>总人数：<b>{{ progressData.totalMembers }}</b></span>
          <span>已完成：<b>{{ progressData.completedCount }}</b></span>
          <span style="color: #67c23a;">成功：<b>{{ progressData.successCount }}</b></span>
          <span v-if="progressData.failedCount > 0" style="color: #f56c6c;">失败：<b>{{ progressData.failedCount }}</b></span>
        </div>

        <div v-if="progressData.status === 'RUNNING'" style="font-size: 12px; color: #909399;">
          请耐心等待，录入过程大约需要几秒到几十秒
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import {
  getGroupOrders, getGroupOrderDetail, downloadTemplate, importGroup,
  auditGroupOrder, confirmGroupPay, updateGroupOrder, deleteGroupOrder, deleteGroupOrders, deleteGroupMember,
  getConfig, getTicketTypes, reRegisterMemberFace, getGroupFaceProgress
} from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue'

const groups = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const selectedIds = ref([])
const importVisible = ref(false)
const editVisible = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const reuploadInput = ref(null)
const reuploadingMemberId = ref(null)
let pendingReuploadMember = null
const importing = ref(false)
const editing = ref(false)
const importFormRef = ref(null)
const editFormRef = ref(null)

// ========== 人脸注册进度 ==========
const progressVisible = ref(false)
const progressData = ref(null)
let progressTimer = null
let progressGroupOrderId = null
const importForm = reactive({ groupName: '', contactName: '', contactPhone: '', visitDate: null })
const editForm = reactive({ id: null, groupName: '', contactName: '', contactPhone: '', visitDate: null })
const importRules = {
  groupName: [{ required: true, message: '请输入团体名称', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  visitDate: [{ required: true, message: '请选择游览日期', trigger: 'change' }],
}
const editRules = {
  groupName: [{ required: true, message: '请输入团体名称', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  visitDate: [{ required: true, message: '请选择游览日期', trigger: 'change' }],
}
let selectedFile = null

const bookingDaysGroup = ref(14)
const minGroupSize = ref(10)

function disablePastDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const maxDate = new Date(today)
  maxDate.setDate(today.getDate() + bookingDaysGroup.value)
  return date < today || date > maxDate
}

function statusType(s) {
  const map = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }
  return map[s] || 'info'
}

function statusText(s) {
  const map = { 0: '待审核', 1: '已通过', 2: '已拒绝', 3: '已支付', 4: '修改待审核' }
  return map[s] || '未知'
}

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(row => row.id)
}

async function loadGroups() {
  loading.value = true
  try {
    const res = await getGroupOrders({ page: page.value, size: size.value })
    groups.value = res.records
    total.value = res.total
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function downloadTpl() {
  try {
    const blob = await downloadTemplate()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'group_member_template.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) { console.error(e) }
}

function handleFileChange(file) {
  selectedFile = file.raw
}

async function handleImport() {
  if (!selectedFile) {
    ElMessage.warning('请选择Excel文件')
    return
  }
  const valid = await importFormRef.value.validate().catch(() => false)
  if (!valid) return

  importing.value = true
  try {
    const fd = new FormData()
    fd.append('file', selectedFile)
    fd.append('groupName', importForm.groupName)
    fd.append('contactName', importForm.contactName)
    fd.append('contactPhone', importForm.contactPhone)
    const formatDate = (d) => {
      if (!d) return ''
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    }
    fd.append('visitDate', formatDate(importForm.visitDate))
    await importGroup(fd)
    ElMessage.success('导入成功')
    importVisible.value = false
    loadGroups()
  } catch (e) { console.error(e) }
  finally { importing.value = false }
}

function openEditDialog(row) {
  editForm.id = row.id
  editForm.groupName = row.groupName
  editForm.contactName = row.contactName
  editForm.contactPhone = row.contactPhone
  // row.visitDate 是字符串（如 "2026-06-30"），需要转为 Date 对象
  editForm.visitDate = row.visitDate ? new Date(row.visitDate) : null
  editVisible.value = true
}

async function handleEdit() {
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return

  editing.value = true
  try {
    const formatDate = (d) => {
      if (!d) return ''
      const date = d instanceof Date ? d : new Date(d)
      if (isNaN(date.getTime())) return ''
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    }
    await updateGroupOrder(editForm.id, {
      groupName: editForm.groupName,
      contactName: editForm.contactName,
      contactPhone: editForm.contactPhone,
      visitDate: formatDate(editForm.visitDate),
    })
    ElMessage.success('修改成功')
    editVisible.value = false
    loadGroups()
  } catch (e) {
    // request.js 拦截器已处理错误提示，此处仅兜底
    console.error('编辑团体信息失败:', e)
  } finally { editing.value = false }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除团体订单「${row.groupName}」吗？此操作不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteGroupOrder(row.id)
    ElMessage.success('删除成功')
    loadGroups()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 个团体订单吗？此操作不可恢复。`,
      '批量删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteGroupOrders(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    loadGroups()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '批量删除失败')
    }
  }
}

async function deleteMember(memberId) {
  try {
    await deleteGroupMember(memberId)
    ElMessage.success('成员已删除')
    if (detail.value) {
      detail.value = await getGroupOrderDetail(detail.value.id)
    }
    loadGroups()
  } catch (e) { console.error(e) }
}

function triggerReupload(member) {
  pendingReuploadMember = member
  reuploadInput.value.value = ''
  reuploadInput.value.click()
}

async function handleReuploadFile(e) {
  const file = e.target.files?.[0]
  if (!file || !pendingReuploadMember) return
  if (file.size > 4 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过4MB')
    return
  }
  const reader = new FileReader()
  reader.onload = async () => {
    const base64 = reader.result.split(',')[1]
    reuploadingMemberId.value = pendingReuploadMember.id
    try {
      await reRegisterMemberFace({ memberId: String(pendingReuploadMember.id), imageBase64: base64 })
      ElMessage.success('人脸重新录入成功')
      if (detail.value) {
        detail.value = await getGroupOrderDetail(detail.value.id)
      }
      loadGroups()
    } catch (e) {
      const msg = e?.response?.data?.message || '重录失败'
      ElMessage.error(msg)
    } finally {
      reuploadingMemberId.value = null
      pendingReuploadMember = null
    }
  }
  reader.readAsDataURL(file)
}

async function viewDetail(row) {
  try {
    detail.value = await getGroupOrderDetail(row.id)
    detailVisible.value = true
  } catch (e) { console.error(e) }
}

async function handleAudit(row, approved) {
  if (!approved) {
    // 拒绝时弹出输入框，填写拒绝理由（可选）
    try {
      const { value } = await ElMessageBox.prompt('请输入拒绝理由（可选）', '拒绝团体订单', {
        confirmButtonText: '确认拒绝',
        cancelButtonText: '取消',
        type: 'warning',
        inputType: 'textarea',
        inputPlaceholder: '请填写拒绝原因...'
      })
      await auditGroupOrder({ groupOrderId: row.id, approved: false, remark: value || '' })
      ElMessage.success('已拒绝')
      loadGroups()
    } catch (e) {
      if (e !== 'cancel' && e !== 'close') console.error(e)
    }
    return
  }
  // 通过
  try {
    await auditGroupOrder({ groupOrderId: row.id, approved: true })
    ElMessage.success('已通过')
    loadGroups()
  } catch (e) { console.error(e) }
}

async function handlePay(row) {
  try {
    await ElMessageBox.confirm(
      `确认为团体「${row.groupName}」代付订单（${row.totalCount}人，合计 ¥${row.totalAmount}）？`,
      '确认代付',
      { confirmButtonText: '确认代付', cancelButtonText: '取消', type: 'warning' }
    )
    await confirmGroupPay(row.id)
    ElMessage.success('支付成功，正在后台录入人脸...')

    // 打开进度弹窗，轮询人脸注册进度
    progressGroupOrderId = row.id
    progressData.value = {
      status: 'RUNNING',
      totalMembers: row.totalCount,
      completedCount: 0,
      successCount: 0,
      failedCount: 0,
      currentName: '正在准备...'
    }
    progressVisible.value = true
    startProgressPolling()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

function startProgressPolling() {
  clearProgressPolling()
  progressTimer = setInterval(async () => {
    if (!progressGroupOrderId) {
      clearProgressPolling()
      return
    }
    try {
      const res = await getGroupFaceProgress(progressGroupOrderId)
      progressData.value = res
      if (res.status === 'COMPLETED' || res.status === 'FAILED' || res.status === 'NOT_STARTED') {
        // 等待1秒后自动关闭，让用户看到最终结果
        setTimeout(() => {
          progressVisible.value = false
          progressGroupOrderId = null
          loadGroups()
          if (res.status === 'COMPLETED') {
            const failedMsg = res.failedCount > 0 ? `（${res.failedCount}人失败）` : ''
            ElMessage.success(`人脸录入完成：成功${res.successCount}人${failedMsg}`)
          } else if (res.status === 'FAILED') {
            ElMessage.error(res.errorMessage || '人脸录入失败')
          }
        }, 1500)
        clearProgressPolling()
      }
    } catch (e) {
      // 轮询异常忽略
    }
  }, 1000)
}

function clearProgressPolling() {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
}

loadGroups()
onMounted(async () => {
  try {
    const cfg = await getConfig()
    if (cfg.bookingDaysGroup) bookingDaysGroup.value = cfg.bookingDaysGroup
  } catch (e) { /* use default */ }
  try {
    const types = await getTicketTypes()
    const group = types.find(t => t.isGroup)
    if (group && group.minGroupSize) minGroupSize.value = group.minGroupSize
  } catch (e) { /* use default 10 */ }
})
onBeforeUnmount(() => {
  clearProgressPolling()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>
