<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>退款审核</h3>
          <div class="header-actions">
            <el-button v-if="selectedIds.length > 0" type="danger" @click="handleBatchDelete">
              批量删除 ({{ selectedIds.length }})
            </el-button>
          </div>
        </div>
      </template>
      <el-table :data="refunds" v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="refundNo" label="退款编号" width="220" />
        <el-table-column prop="orderNo" label="订单编号" width="220" />
        <el-table-column label="退款金额" width="100">
          <template #default="{ row }">¥{{ row.refundAmount }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="退款原因" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="180" />
        <el-table-column prop="auditTime" label="审核时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button size="small" type="success" @click="handleAudit(row, true)">通过</el-button>
              <el-button size="small" type="danger" @click="handleAudit(row, false)">拒绝</el-button>
            </template>
            <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev, pager, next" @current-change="loadRefunds" style="margin-top: 16px; justify-content: center;" />
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getAdminRefunds, auditRefund, deleteAdminRefund, deleteAdminRefunds } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const refunds = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const selectedIds = ref([])

function statusType(s) {
  const map = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }
  return map[s] || 'info'
}

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(row => row.id)
}

async function loadRefunds() {
  loading.value = true
  try {
    const res = await getAdminRefunds({ page: page.value, size: size.value })
    refunds.value = res.records
    total.value = res.total
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function handleAudit(row, approved) {
  try {
    await auditRefund({ refundNo: row.refundNo, approved })
    ElMessage.success(approved ? '已通过' : '已拒绝')
    loadRefunds()
  } catch (e) { console.error(e) }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除退款记录 ${row.refundNo} 吗？此操作不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteAdminRefund(row.id)
    ElMessage.success('退款记录已删除')
    loadRefunds()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 条退款记录吗？此操作不可恢复。`,
      '批量删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteAdminRefunds(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    loadRefunds()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '批量删除失败')
    }
  }
}

loadRefunds()
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
