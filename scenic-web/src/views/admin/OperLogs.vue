<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>操作日志</h3>
          <div class="header-actions">
            <el-button v-if="selectedIds.length > 0" type="danger" @click="handleBatchDelete">
              批量删除 ({{ selectedIds.length }})
            </el-button>
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              :shortcuts="dateShortcuts"
              style="width: 280px;"
            />
            <el-button type="primary" @click="handleQuery">查询</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="logs"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        ref="tableRef">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="action" label="操作" width="120" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status ? 'success' : 'danger'">{{ row.status ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="180" />
      </el-table>
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadData"
        style="margin-top: 16px; justify-content: center;" />
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getOperLogs, deleteOperLogs } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const logs = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const tableRef = ref(null)
const selectedIds = ref([])
const dateRange = ref(null)

const dateShortcuts = [
  { text: '近三天', value: () => { const end = new Date(); const start = new Date(); start.setDate(start.getDate() - 2); return [start, end] } },
  { text: '近一周', value: () => { const end = new Date(); const start = new Date(); start.setDate(start.getDate() - 6); return [start, end] } },
  { text: '近一月', value: () => { const end = new Date(); const start = new Date(); start.setMonth(start.getMonth() - 1); return [start, end] } },
]

function buildParams() {
  const params = { page: page.value, size: size.value }
  if (dateRange.value && dateRange.value.length === 2) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }
  return params
}

function handleQuery() {
  page.value = 1
  loadData()
}

function resetFilter() {
  dateRange.value = null
  page.value = 1
  loadData()
}

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(r => r.id)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getOperLogs(buildParams())
    logs.value = res.records
    total.value = res.total
    tableRef.value?.clearSelection()
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 条操作日志吗？此操作不可恢复。`,
      '批量删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteOperLogs(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    page.value = 1
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '批量删除失败')
    }
  }
}

loadData()
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
</style>
