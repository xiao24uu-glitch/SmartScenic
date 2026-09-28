<template>
  <div class="order-page">
    <!-- ====== 页面头部 ====== -->
    <div class="page-hero">
      <div class="section-header">
        <span class="section-tag">ORDERS</span>
        <h2 class="section-title">我的订单</h2>
        <p class="section-subtitle">管理您的所有购票订单与退款记录</p>
      </div>
    </div>

    <div class="content-wrapper">
      <!-- 分类筛选 Tab -->
      <div class="section section-tabs" v-observe>
        <div class="tab-bar">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            :class="['tab-btn', { active: activeTab === tab.value }]"
            @click="switchTab(tab.value)"
          >
            <span class="tab-icon">{{ tab.icon }}</span>
            <span class="tab-label">{{ tab.label }}</span>
            <span v-if="tab.value === '' && total > 0" class="tab-count">{{ total }}</span>
          </button>
        </div>
      </div>

      <!-- 统一记录面板 -->
      <div class="section" v-observe>
        <div class="list-card">
          <div class="list-card-header">
            <h3>{{ panelTitle }}</h3>
            <span class="list-card-count" v-if="total > 0">共 {{ total }} 条记录</span>
          </div>
          <div class="list-card-body">
            <el-table :data="allRecords" style="width: 100%" v-loading="loading" class="modern-table unified-table">
              <!-- 类型标签 -->
              <el-table-column label="类型" width="95">
                <template #default="{ row }">
                  <span class="type-tag" :class="'type--' + row._rowType">{{ row._rowTypeLabel }}</span>
                </template>
              </el-table-column>
              <!-- 编号/名称 -->
              <el-table-column label="编号 / 名称" min-width="220">
                <template #default="{ row }">
                  <template v-if="row._rowType === 'refund'">
                    <span class="record-no">{{ row.refundNo }}</span>
                    <div class="record-sub" v-if="row.orderNo">{{ row.orderNo }}</div>
                  </template>
                  <template v-else-if="row._rowType === 'group'">
                    <span class="record-name">{{ row.groupName }}</span>
                    <div class="record-sub" v-if="row.contactName">{{ row.contactName }} · {{ row.contactPhone }}</div>
                  </template>
                  <template v-else>
                    <span class="record-no">{{ row.orderNo }}</span>
                    <div class="record-sub" v-if="row.scenicName">{{ row.scenicName }}</div>
                  </template>
                </template>
              </el-table-column>
              <!-- 日期 -->
              <el-table-column label="日期" width="125">
                <template #default="{ row }">
                  <span v-if="row._rowType === 'refund'">{{ row.createTime?.slice(0, 10) || '-' }}</span>
                  <span v-else>{{ row.visitDate || '-' }}</span>
                </template>
              </el-table-column>
              <!-- 金额 -->
              <el-table-column label="金额" width="105">
                <template #default="{ row }">
                  <span class="amount-text" :class="{ 'amount--refund': row._rowType === 'refund' }">¥{{ row._amount }}</span>
                  <span v-if="row._rowType === 'normal' && row.discountAmount > 0" class="amount-save">省¥{{ row.discountAmount }}</span>
                </template>
              </el-table-column>
              <!-- 状态 -->
              <el-table-column label="状态" width="170">
                <template #default="{ row }">
                  <el-tag :type="row._rowType === 'group' ? groupStatusType(row._status) : row._rowType === 'refund' ? refundStatusType(row._status) : statusType(row._status)" effect="plain" round>{{ row._statusText }}</el-tag>
                  <el-tag v-if="row._rowType === 'normal' && row.status === 1 && isVisitDateExpired(row)" type="info" effect="plain" round style="margin-left: 4px;">已过期</el-tag>
                </template>
              </el-table-column>
              <!-- 时间 -->
              <el-table-column label="时间" width="170">
                <template #default="{ row }">
                  <div>{{ row.createTime || '-' }}</div>
                  <div v-if="row._rowType === 'refund' && row.auditTime" class="record-sub">{{ row.auditTime }} 审核</div>
                </template>
              </el-table-column>
              <!-- 补充信息（团体人数 / 拒绝原因 / 退款原因 / 录入人脸） -->
              <el-table-column label="备注" min-width="110">
                <template #default="{ row }">
                  <template v-if="row._rowType === 'group'">
                    <span class="extra-info">{{ row.totalCount }}人</span>
                    <span class="extra-info" v-if="row.enteredCount > 0">入园 {{ row.enteredCount }}/{{ row.totalCount }}</span>
                    <span class="extra-info extra--rejected" v-if="row.status === 2 && row.auditRemark" :title="row.auditRemark">原因: {{ row.auditRemark }}</span>
                  </template>
                  <template v-else-if="row._rowType === 'refund'">
                    <span class="extra-info extra--reason" :title="row.reason">{{ row.reason || '-' }}</span>
                  </template>
                  <template v-else>
                    <span v-if="row.status === 1 && row.faceCount > 0" class="extra-info">{{ row.faceCount }}人已录入</span>
                    <span v-if="row.status === 5 && row.inParkCount > 0" class="extra-info extra--inpark">在园 {{ row.inParkCount }}人</span>
                  </template>
                </template>
              </el-table-column>
              <!-- 操作 -->
              <el-table-column label="操作" width="300" fixed="right">
                <template #default="{ row }">
                  <div class="action-btns">
                    <!-- 普通订单 -->
                    <template v-if="row._rowType === 'normal'">
                      <button class="action-btn action-btn--detail" @click="$router.push(`/orders/${row.orderNo}`)">详情</button>
                      <button v-if="row.status === 0" class="action-btn action-btn--pay" @click="$router.push(`/pay?orderNo=${row.orderNo}`)">去支付</button>
                      <button v-if="row.status === 0" class="action-btn action-btn--cancel" @click="handleCancel(row)">取消</button>
                      <button v-if="row.status === 1 && !isVisitDateExpired(row)" class="action-btn action-btn--face" @click="$router.push(`/pay?orderNo=${row.orderNo}`)">录入人脸</button>
                      <button v-if="row.status === 1" class="action-btn action-btn--refund" @click="handleRefund(row)">退款</button>
                      <button v-if="row.status !== 1 && row.status !== 5 && row.status !== 6" class="action-btn action-btn--delete" @click="handleDeleteOrder(row)">删除</button>
                      <button v-if="row.status === 1 && !isVisitDateExpired(row)" class="action-btn action-btn--edit" @click="openModifyDialog(row)">修改</button>
                    </template>
                    <!-- 团体订单 -->
                    <template v-else-if="row._rowType === 'group'">
                      <button class="action-btn action-btn--detail" @click="viewGroupDetail(row)">详情</button>
                      <button v-if="row.status === 1" class="action-btn action-btn--pay" @click="$router.push(`/pay?groupOrderId=${row.id}`)">去支付</button>
                      <button v-if="row.status !== 3" class="action-btn action-btn--delete" @click="handleDeleteGroup(row.id)">删除</button>
                      <button v-if="row.status === 0 || row.status === 1 || row.status === 4" class="action-btn action-btn--edit" @click="openGroupEditDialog(row)">修改</button>
                    </template>
                    <!-- 退款记录 -->
                    <template v-else-if="row._rowType === 'refund'">
                      <button v-if="row.orderNo" class="action-btn action-btn--detail" @click="$router.push(`/orders/${row.orderNo}`)">查看订单</button>
                    </template>
                  </div>
                </template>
              </el-table-column>
              <template #empty>
                <div class="empty-state">{{ emptyText }}</div>
              </template>
            </el-table>
            <!-- 统一分页 -->
            <div class="card-pagination" v-if="total > size">
              <el-pagination
                v-model:current-page="page"
                :page-size="size"
                :total="total"
                layout="prev, pager, next"
                @current-change="loadData"
                background
                small
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 修改普通订单日期弹窗 -->
    <el-dialog v-model="modifyVisible" title="申请修改订单" width="450px">
      <el-alert type="warning" :closable="false" style="margin-bottom: 16px;">
        修改提交后需要管理员审核，审核通过后生效。
      </el-alert>
      <el-form label-width="100px">
        <el-form-item label="订单编号">
          <span>{{ modifyingOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="当前日期">
          <span>{{ modifyingOrder?.visitDate }}</span>
        </el-form-item>
        <el-form-item label="新游览日期">
          <el-date-picker v-model="newVisitDate" type="date" placeholder="选择新日期" :disabled-date="disabledDate" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modifyVisible = false">取消</el-button>
        <el-button type="primary" @click="handleModify" :loading="modifying">提交修改</el-button>
      </template>
    </el-dialog>

    <!-- 编辑团体信息弹窗 -->
    <el-dialog v-model="groupEditVisible" title="修改团体信息" width="500px">
      <el-alert type="warning" :closable="false" style="margin-bottom: 16px;">
        修改后订单将变为「待审核」状态，需管理员重新审核通过。
      </el-alert>
      <el-form ref="groupEditFormRef" :model="groupEditForm" :rules="groupEditRules" label-width="100px">
        <el-form-item label="团体名称" prop="groupName">
          <el-input v-model="groupEditForm.groupName" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="groupEditForm.contactName" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="groupEditForm.contactPhone" />
        </el-form-item>
        <el-form-item label="游览日期" prop="visitDate">
          <el-date-picker v-model="groupEditForm.visitDate" type="date" style="width: 100%;" :disabled-date="disabledDateGroup" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupEditVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGroupEdit" :loading="groupEditing">保存</el-button>
      </template>
    </el-dialog>

    <!-- 团体详情弹窗 -->
    <el-dialog v-model="groupDetailVisible" title="团体详情" width="850px">
      <el-descriptions :column="2" border v-if="groupDetail">
        <el-descriptions-item label="团体名称">{{ groupDetail.groupName }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ groupDetail.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ groupDetail.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="游览日期">{{ groupDetail.visitDate }}</el-descriptions-item>
        <el-descriptions-item label="总人数">{{ groupDetail.totalCount }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ groupDetail.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="已入园">{{ groupDetail.enteredCount }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ groupDetail.statusText }}</el-descriptions-item>
      </el-descriptions>
      <h4 style="margin: 16px 0 8px;">成员列表</h4>
      <el-table :data="groupDetail?.members || []" size="small" max-height="300">
        <el-table-column prop="realName" label="姓名" width="90" />
        <el-table-column prop="idCard" label="身份证号" width="170" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column label="人脸状态" width="130">
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
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button
              v-if="row.faceStatus === 2 && groupDetail?.status === 3"
              size="small" type="warning" link
              :loading="reuploadingMemberId === row.id"
              @click="triggerReupload(row)"
            >重传</el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- 隐藏的文件选择器 -->
      <input ref="reuploadInput" type="file" accept="image/*" style="display: none;" @change="handleReuploadFile" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useScenicStore } from '../../stores/scenic'
import { getUserOrders, cancelOrder, applyRefund, deleteOrder, requestModifyOrder, getMyGroupOrders, getGroupOrderDetail, updateGroupOrder, deleteGroupOrder as apiDeleteGroupOrder, getRefundList, getConfig, reRegisterMemberFace } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const scenicStore = useScenicStore()

const tabs = [
  { label: '全部', value: '', icon: '' },
  { label: '普通订单', value: 'normal', icon: '' },
  { label: '团体订单', value: 'group', icon: '' },
  { label: '待支付/待审核', value: 'pending', icon: '' },
]

const activeTab = ref('')
const _normalOrders = ref([])
const _groupOrders = ref([])
const _refunds = ref([])
const _normalTotal = ref(0)
const _groupTotal = ref(0)
const _refundTotal = ref(0)
const loading = ref(false)
const page = ref(1)
const size = ref(10)

// 统一面板标题
const panelTitle = computed(() => {
  const map = { '': '全部记录', normal: '普通订单', group: '团体订单', pending: '待支付 / 待审核' }
  return map[activeTab.value] || '全部记录'
})

// 空状态文案
const emptyText = computed(() => {
  const map = { '': '暂无任何记录', normal: '暂无普通订单', group: '暂无团体订单', pending: '暂无待支付或待审核记录' }
  return map[activeTab.value] || '暂无记录'
})

// 总条数（根据当前 tab）
const total = computed(() => {
  if (activeTab.value === 'normal') return _normalTotal.value
  if (activeTab.value === 'group') return _groupTotal.value
  if (activeTab.value === 'pending') return _normalTotal.value + _refundTotal.value
  return _normalTotal.value + _groupTotal.value + _refundTotal.value
})

// 统一数据源（合并 + 标准化）
const allRecords = computed(() => {
  const result = []
  for (const r of _normalOrders.value) {
    result.push({ ...r, _rowType: 'normal', _rowTypeLabel: '普通订单', _amount: r.totalAmount || 0, _status: r.status, _statusText: r.statusText })
  }
  for (const r of _groupOrders.value) {
    result.push({ ...r, _rowType: 'group', _rowTypeLabel: '团体订单', _amount: r.totalAmount || 0, _status: r.status, _statusText: r.statusText })
  }
  for (const r of _refunds.value) {
    result.push({ ...r, _rowType: 'refund', _rowTypeLabel: '退款记录', _amount: r.refundAmount || 0, _status: r.status, _statusText: r.statusText })
  }
  return result
})

function switchTab(tab) {
  activeTab.value = tab
  page.value = 1
  loadData()
}

function statusType(status) {
  const map = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger', 4: 'warning', 5: 'success', 6: 'info' }
  return map[status] || 'info'
}

function groupStatusType(s) {
  const map = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info', 4: 'warning' }
  return map[s] || 'info'
}

function refundStatusType(s) {
  const map = { 0: 'warning', 1: 'success', 2: 'danger' }
  return map[s] || 'info'
}

function isVisitDateExpired(row) {
  if (!row.visitDate) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const visitDate = new Date(row.visitDate)
  visitDate.setHours(0, 0, 0, 0)
  return visitDate < today
}

async function loadData() {
  loading.value = true
  try {
    const batch = []
    // 加载普通订单
    if (activeTab.value === '' || activeTab.value === 'normal' || activeTab.value === 'pending') {
      const params = { page: page.value, size: size.value }
      if (activeTab.value === 'pending') params.status = 0
      batch.push(getUserOrders(params).then(res => {
        _normalOrders.value = res.records
        _normalTotal.value = res.total
      }))
    } else {
      _normalOrders.value = []
      _normalTotal.value = 0
    }
    // 加载团体订单
    if (activeTab.value === '' || activeTab.value === 'group') {
      batch.push(getMyGroupOrders({ page: page.value, size: size.value }).then(res => {
        _groupOrders.value = res.records
        _groupTotal.value = res.total
      }))
    } else {
      _groupOrders.value = []
      _groupTotal.value = 0
    }
    // 加载退款记录
    if (activeTab.value === '' || activeTab.value === 'pending') {
      batch.push(getRefundList({ page: 1, size: 50 }).then(res => {
        let list = res.records || []
        if (activeTab.value === 'pending') {
          list = list.filter(r => r.status === 0)
        }
        _refunds.value = list
        _refundTotal.value = list.length
      }))
    } else {
      _refunds.value = []
      _refundTotal.value = 0
    }
    await Promise.all(batch)
  } catch (e) {
    console.error('加载记录失败', e)
  } finally {
    loading.value = false
  }
}

// --- 普通订单操作 ---

const bookingDaysNormal = ref(7)
const bookingDaysGroup = ref(14)

function disabledDate(time) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const maxDate = new Date(today)
  maxDate.setDate(today.getDate() + bookingDaysNormal.value)
  return time.getTime() < today.getTime() || time.getTime() > maxDate.getTime()
}

function disabledDateGroup(time) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const maxDate = new Date(today)
  maxDate.setDate(today.getDate() + bookingDaysGroup.value)
  return time.getTime() < today.getTime() || time.getTime() > maxDate.getTime()
}

async function handleCancel(row) {
  try {
    await ElMessageBox.confirm('确定取消该订单吗？', '提示', { type: 'warning' })
    await cancelOrder(row.orderNo)
    ElMessage.success('订单已取消')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function handleDeleteOrder(row) {
  try {
    await ElMessageBox.confirm('确定删除该订单吗？删除后无法恢复。', '警告', { type: 'error', confirmButtonText: '确认删除', confirmButtonClass: 'el-button--danger' })
    await deleteOrder(row.orderNo)
    ElMessage.success('订单已删除')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function handleRefund(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入退款原因', '申请退款', { type: 'warning' })
    await applyRefund({ orderNo: row.orderNo, reason: value })
    ElMessage.success('退款申请已提交')
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

// 修改订单
const modifyVisible = ref(false)
const modifying = ref(false)
const modifyingOrder = ref(null)
const newVisitDate = ref(null)

function openModifyDialog(row) {
  modifyingOrder.value = row
  newVisitDate.value = null
  modifyVisible.value = true
}

async function handleModify() {
  if (!newVisitDate.value) {
    ElMessage.warning('请选择新的游览日期')
    return
  }
  modifying.value = true
  try {
    const d = new Date(newVisitDate.value)
    const dateStr = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
    await requestModifyOrder(modifyingOrder.value.orderNo, dateStr)
    ElMessage.success('修改申请已提交，请等待管理员审核')
    modifyVisible.value = false
    loadData()
  } catch (e) {
    console.error(e)
  } finally {
    modifying.value = false
  }
}

// --- 团体订单操作 ---

const groupDetailVisible = ref(false)
const groupDetail = ref(null)
const reuploadInput = ref(null)
const reuploadingMemberId = ref(null)
let pendingReuploadMember = null

async function viewGroupDetail(row) {
  try {
    groupDetail.value = await getGroupOrderDetail(row.id)
    groupDetailVisible.value = true
  } catch (e) {
    console.error(e)
  }
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
      groupDetail.value = await getGroupOrderDetail(groupDetail.value.id)
      loadData()
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

async function handleDeleteGroup(id) {
  try {
    await ElMessageBox.confirm('确定删除该团体订单吗？', '删除确认', { type: 'warning' })
    await apiDeleteGroupOrder(id)
    ElMessage.success('团体订单已删除')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

// 编辑团体订单
const groupEditVisible = ref(false)
const groupEditing = ref(false)
const groupEditFormRef = ref(null)
const groupEditForm = reactive({ id: null, groupName: '', contactName: '', contactPhone: '', visitDate: null })
const groupEditRules = {
  groupName: [{ required: true, message: '请输入团体名称', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  visitDate: [{ required: true, message: '请选择游览日期', trigger: 'change' }],
}

function openGroupEditDialog(row) {
  groupEditForm.id = row.id
  groupEditForm.groupName = row.groupName
  groupEditForm.contactName = row.contactName
  groupEditForm.contactPhone = row.contactPhone
  groupEditForm.visitDate = row.visitDate ? new Date(row.visitDate) : null
  groupEditVisible.value = true
}

async function handleGroupEdit() {
  const valid = await groupEditFormRef.value.validate().catch(() => false)
  if (!valid) return

  groupEditing.value = true
  try {
    const formatDate = (d) => {
      if (!d) return ''
      return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
    }
    await updateGroupOrder(groupEditForm.id, {
      groupName: groupEditForm.groupName,
      contactName: groupEditForm.contactName,
      contactPhone: groupEditForm.contactPhone,
      visitDate: formatDate(groupEditForm.visitDate),
    })
    ElMessage.success('修改已提交，等待重新审核')
    groupEditVisible.value = false
    loadData()
  } catch (e) {
    console.error(e)
  } finally {
    groupEditing.value = false
  }
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
  loadData()
  setTimeout(setupObserver, 200)
  try {
    const cfg = await getConfig()
    if (cfg.bookingDaysNormal) bookingDaysNormal.value = cfg.bookingDaysNormal
    if (cfg.bookingDaysGroup) bookingDaysGroup.value = cfg.bookingDaysGroup
  } catch (e) { /* use defaults */ }
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
.order-page { margin: -24px; }

/* ====== 页面头部 ====== */
.page-hero {
  padding: 56px 24px 46px; text-align: center;
}
.page-hero .section-tag { color: rgba(255,255,255,0.7); }
.page-hero .section-title { color: #fff; }
.page-hero .section-subtitle { color: rgba(255,255,255,0.8); }

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

/* ====== 内容区 ====== */
.content-wrapper { background: transparent; padding: 0 24px 80px; }
.section { max-width: 1200px; margin: 0 auto; padding-top: 40px; }

/* 入场动画 */
[v-observe] { opacity: 0; transform: translateY(30px); transition: opacity 0.7s ease, transform 0.7s cubic-bezier(0.22, 0.61, 0.36, 1); }
[v-observe].visible { opacity: 1; transform: translateY(0); }

/* ====== Tab 切换栏 ====== */
.section-tabs { padding-top: 36px; }
.tab-bar {
  display: flex; gap: 8px; flex-wrap: wrap;
  background: rgba(255,255,255,0.85); backdrop-filter: blur(10px);
  border-radius: 18px; padding: 6px;
  box-shadow: var(--shadow-sm);
}
.tab-btn {
  flex: 1; min-width: 100px;
  border: none; border-radius: 14px; padding: 12px 20px;
  background: transparent; color: #64748b;
  font-size: 14px; font-weight: 600; cursor: pointer;
  transition: all var(--transition);
  display: flex; align-items: center; justify-content: center; gap: 8px;
}
.tab-btn:hover { color: #1a1a2e; background: rgba(0,0,0,0.04); }
.tab-btn.active {
  background: v-bind('scenicStore.primaryColor'); color: #fff;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}
.tab-icon { font-size: 16px; }
.tab-label { white-space: nowrap; }

/* ====== 列表卡片 ====== */
.list-card {
  background: rgba(255,255,255,0.85); backdrop-filter: blur(10px);
  border-radius: 20px; overflow: hidden;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition);
}
.list-card:hover { box-shadow: var(--shadow-md); }
.list-card-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 28px; border-bottom: 1px solid #f1f5f9;
}
.list-card-header h3 { font-size: 17px; font-weight: 700; color: #1a1a2e; margin: 0; }
.list-card-count { font-size: 13px; color: #94a3b8; }
.list-card-body { padding: 0; }

/* ====== 表格样式 ====== */
.modern-table {
  --el-table-border-color: #f1f5f9;
  --el-table-header-bg-color: #f8fafc;
  --el-table-row-hover-bg-color: #f8fafc;
}
.amount-text { font-weight: 700; color: #E6A23C; font-size: 15px; }
.amount--refund { color: #F56C6C; }

/* 操作按钮 */
.action-btns { display: flex; gap: 6px; flex-wrap: wrap; }
.action-btn {
  border: none; border-radius: 8px; padding: 5px 12px;
  font-size: 12px; font-weight: 600; cursor: pointer;
  transition: all 0.2s; white-space: nowrap;
}
.action-btn--detail { background: #e8f4fd; color: #409EFF; }
.action-btn--detail:hover { background: #d3eafc; }
.action-btn--pay { background: #e8f8e8; color: #67C23A; }
.action-btn--pay:hover { background: #d4f0d4; }
.action-btn--cancel { background: #fef0f0; color: #F56C6C; }
.action-btn--cancel:hover { background: #fde2e2; }
.action-btn--face { background: #ecf5ff; color: #409EFF; }
.action-btn--face:hover { background: #d9ecff; }
.action-btn--refund { background: #fdf6ec; color: #E6A23C; }
.action-btn--refund:hover { background: #faecd8; }
.action-btn--delete { background: #f5f5f5; color: #909399; }
.action-btn--delete:hover { background: #e9e9e9; }
.action-btn--edit { background: #f4f4f5; color: #606266; }
.action-btn--edit:hover { background: #e9e9eb; }
/* 空状态 */
.empty-state { padding: 40px 0; text-align: center; color: #94a3b8; font-size: 15px; }

/* ====== 统一表格 - 类型/编号/备注 ====== */
.type-tag {
  display: inline-block; font-size: 11px; font-weight: 700;
  padding: 3px 10px; border-radius: 8px;
}
.type--normal { background: #ecf5ff; color: #409EFF; }
.type--group  { background: #fdf6ec; color: #E6A23C; }
.type--refund  { background: #fef0f0; color: #F56C6C; }
.record-no, .record-name {
  font-weight: 600; color: #1a1a2e; font-size: 14px;
}
.record-sub {
  font-size: 12px; color: #94a3b8; margin-top: 2px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px;
}
.extra-info {
  display: inline-block; font-size: 12px; color: #606266;
  background: #f8fafc; padding: 2px 8px; border-radius: 6px; margin: 1px 2px;
}
.extra--reason {
  max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block;
}
.extra--inpark { background: #f0f9eb; color: #67C23A; }
.extra--rejected { background: #fef0f0; color: #F56C6C; max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: inline-block; }
.amount-save { display: block; font-size: 11px; color: #67C23A; }
.tab-count {
  background: rgba(255,255,255,0.4); font-size: 12px;
  padding: 1px 8px; border-radius: 10px; font-weight: 700;
}
.unified-table { --el-table-row-hover-bg-color: #f8fafc; }

/* 分页 */
.card-pagination {
  display: flex; justify-content: center; padding: 16px 0 8px;
  border-top: 1px solid #f1f5f9; margin-top: 0;
}

/* ====== 动画 ====== */
@keyframes fadeInUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

/* ====== 响应式 ====== */
@media (max-width: 768px) {
  .tab-btn { padding: 10px 14px; font-size: 13px; }
  .tab-icon { display: none; }
  .list-card-header { padding: 14px 16px; }
  .action-btns { gap: 4px; }
  .action-btn { padding: 4px 8px; font-size: 11px; }
}
</style>
