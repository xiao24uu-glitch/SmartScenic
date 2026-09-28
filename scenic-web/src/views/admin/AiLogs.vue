<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>AI对话记录</h3>
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
        :data="conversations"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        ref="tableRef">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="question" label="用户问题" show-overflow-tooltip />
        <el-table-column prop="answer" label="AI回答" show-overflow-tooltip />
        <el-table-column prop="intent" label="意图" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ intentLabel(row.intent) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tokensUsed" label="Tokens" width="80" />
        <el-table-column label="用户评价" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="feedbackTagType(row.feedback)">{{ feedbackLabel(row.feedback) }}</el-tag>
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
import { getAiConversations, deleteAiConversations } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const conversations = ref([])
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

function intentLabel(i) {
  const map = { consult: '咨询', buy_ticket: '购票', guide: '导览', other: '其他' }
  return map[i] || i
}

function feedbackLabel(v) {
  const map = { 0: '未评价', 1: '有用', 2: '无用' }
  return map[v] ?? '未知'
}

function feedbackTagType(v) {
  if (v === 1) return 'success'
  if (v === 2) return 'danger'
  return 'info'
}

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
    const res = await getAiConversations(buildParams())
    conversations.value = res.records
    total.value = res.total
    tableRef.value?.clearSelection()
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 条对话记录吗？此操作不可恢复。`,
      '批量删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteAiConversations(selectedIds.value)
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
