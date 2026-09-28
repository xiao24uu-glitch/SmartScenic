<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>订单管理</h3>
          <div class="header-actions">
            <el-button v-if="selectedIds.length > 0" type="danger" @click="handleBatchDelete">
              批量删除 ({{ selectedIds.length }})
            </el-button>
          </div>
        </div>
      </template>
      <el-form :inline="true" :model="filter">
        <el-form-item label="订单编号">
          <el-input v-model="filter.orderNo" placeholder="搜索订单编号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filter.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="待支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已取消" :value="2" />
            <el-option label="已退款" :value="3" />
            <el-option label="修改待审核" :value="4" />
            <el-option label="已入园" :value="5" />
            <el-option label="已出园" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadOrders">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="orders" v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="orderNo" label="订单编号" width="250" />
        <el-table-column prop="visitDate" label="游览日期" width="200">
          <template #default="{ row }">
            <span>{{ row.visitDate }}</span>
            <el-tag v-if="row.status === 4 && row.pendingVisitDate" size="small" type="warning" style="margin-left: 6px;">申请改为 {{ row.pendingVisitDate }}</el-tag>
            <el-tag v-if="row.status === 1 && isVisitDateExpired(row)" size="small" type="info" style="margin-left: 6px;">已过期</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="100">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="150">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 4">
              <el-button type="primary" size="small" @click="openDetail(row)">详情</el-button>
              <el-button type="success" size="small" @click="handleAudit(row, true)">通过</el-button>
              <el-button type="danger" size="small" @click="handleAudit(row, false)">拒绝</el-button>
            </template>
            <template v-else>
              <el-button type="primary" size="small" @click="openDetail(row)">详情</el-button>
              <el-button type="primary" size="small" @click="openEdit(row)">修改</el-button>
              <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev, pager, next" @current-change="loadOrders" style="margin-top: 16px; justify-content: center;" />
    </el-card>

    <!-- 修改订单对话框 -->
    <el-dialog v-model="editVisible" title="修改订单" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="订单编号">
          <el-input v-model="editForm.orderNo" disabled />
        </el-form-item>
        <el-form-item label="游览日期">
          <el-date-picker v-model="editForm.visitDate" type="date" placeholder="请选择日期" style="width: 100%" :disabled-date="disabledDate" />
        </el-form-item>
        <el-form-item label="订单金额">
          <el-input-number v-model="editForm.totalAmount" :precision="2" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="editForm.status" style="width: 100%">
            <el-option label="待支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已取消" :value="2" />
            <el-option label="已退款" :value="3" />
            <el-option label="修改待审核" :value="4" />
            <el-option label="已入园" :value="5" />
            <el-option label="已出园" :value="6" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="650px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单编号">{{ detailRow.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="statusType(detailRow.status)">{{ statusText(detailRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="景区名称">{{ detailRow.scenicName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="游览日期">{{ detailRow.visitDate }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">¥{{ detailRow.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="实付金额">{{ detailRow.payAmount ? '¥' + detailRow.payAmount : '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ payTypeLabel(detailRow.payType) }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ detailRow.payTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailRow.createTime }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ detailRow.userId }}</el-descriptions-item>
        <el-descriptions-item v-if="detailRow.pendingVisitDate" label="待审日期" :span="2">
          <el-tag type="warning">{{ detailRow.pendingVisitDate }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="detailRow.items && detailRow.items.length" style="margin-top: 20px;">
        <h4 style="margin-bottom: 12px;">票种明细</h4>
        <el-table :data="detailRow.items" border size="small">
          <el-table-column prop="ticketTypeName" label="票种" />
          <el-table-column prop="unitPrice" label="单价">
            <template #default="{ row }">¥{{ row.unitPrice }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="小计">
            <template #default="{ row }">¥{{ row.subtotal }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAdminOrders, updateAdminOrder, deleteAdminOrder, deleteAdminOrders, auditOrderModify, getConfig } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const orders = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const selectedIds = ref([])
const filter = reactive({ orderNo: '', status: null })
const bookingDaysNormal = ref(7)

// 编辑对话框
const editVisible = ref(false)
const editForm = reactive({
  id: null,
  orderNo: '',
  visitDate: '',
  totalAmount: 0,
  status: 0,
})

// 详情对话框
const detailVisible = ref(false)
const detailRow = ref({})

function statusType(s) {
  const map = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger', 4: 'warning', 5: 'success', 6: 'info' }
  return map[s] || 'info'
}

function statusText(s) {
  const map = { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已退款', 4: '修改待审核', 5: '已入园', 6: '已出园' }
  return map[s] || '未知'
}

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(row => row.id)
}

async function handleAudit(row, approved) {
  const action = approved ? '通过' : '拒绝'
  try {
    await ElMessageBox.confirm(`确定${action}该订单的修改申请？`, '审核确认', { type: 'warning' })
    await auditOrderModify(row.id, approved)
    ElMessage.success(`已${action}修改申请`)
    loadOrders()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function loadOrders() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filter.status !== null && filter.status !== '') params.status = filter.status
    if (filter.orderNo) params.orderNo = filter.orderNo
    const res = await getAdminOrders(params)
    orders.value = res.records
    total.value = res.total
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

function openEdit(row) {
  editForm.id = row.id
  editForm.orderNo = row.orderNo
  editForm.visitDate = row.visitDate
  editForm.totalAmount = row.totalAmount
  editForm.status = row.status
  editVisible.value = true
}

async function handleSave() {
  try {
    await updateAdminOrder(editForm.id, {
      visitDate: editForm.visitDate,
      totalAmount: editForm.totalAmount,
      status: editForm.status,
    })
    ElMessage.success('订单修改成功')
    editVisible.value = false
    loadOrders()
  } catch (e) {
    console.error('修改订单失败', e)
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除订单 ${row.orderNo} 吗？删除后不可恢复！`,
      '确认删除',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteAdminOrder(row.id)
    ElMessage.success('订单已删除')
    loadOrders()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除订单失败', e)
    }
  }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 个订单吗？此操作不可恢复。`,
      '批量删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteAdminOrders(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    loadOrders()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '批量删除失败')
    }
  }
}

function openDetail(row) {
  detailRow.value = row
  detailVisible.value = true
}

function payTypeLabel(payType) {
  if (payType === null || payType === undefined) return '-'
  const map = { 0: '模拟支付', 1: '微信支付', 2: '支付宝' }
  return map[payType] || '模拟支付'
}

// 判断订单游览日期是否已过期
function isVisitDateExpired(row) {
  if (!row.visitDate) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const visitDate = new Date(row.visitDate)
  visitDate.setHours(0, 0, 0, 0)
  return visitDate < today
}

function disabledDate(time) {
  const t = new Date(); t.setHours(0,0,0,0)
  const m = new Date(t); m.setDate(t.getDate() + bookingDaysNormal.value)
  return time.getTime() < t.getTime() || time.getTime() > m.getTime()
}

loadOrders()
onMounted(async () => {
  try {
    const cfg = await getConfig()
    if (cfg.bookingDaysNormal) bookingDaysNormal.value = cfg.bookingDaysNormal
  } catch (e) { /* use default */ }
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
