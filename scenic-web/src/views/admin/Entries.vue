<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>检票记录</h3>
          <div class="header-actions">
            <el-button v-if="selectedIds.length > 0" type="danger" @click="handleBatchDelete">
              批量删除 ({{ selectedIds.length }})
            </el-button>
          </div>
        </div>
      </template>
      <el-table :data="entries" v-loading="loading" @selection-change="handleSelectionChange" ref="tableRef">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="gateNo" label="闸机" width="80" />
        <el-table-column prop="realName" label="姓名" width="100">
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
        <el-table-column label="订单号" width="220">
          <template #default="{ row }">{{ row.orderNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="票种" width="100">
          <template #default="{ row }">{{ row.ticketTypeName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="failReason" label="备注" />
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev, pager, next" @current-change="loadEntries" style="margin-top: 16px; justify-content: center;" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { getEntryLogs, getCheckerEntries, deleteEntryLog, deleteEntryLogs, deleteCheckerEntries } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const entries = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const selectedIds = ref([])
const tableRef = ref(null)

const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || 'null')
const isChecker = computed(() => userInfo?.roleCode === 'CHECKER')

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(row => row.id)
}

async function loadEntries() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    const res = isChecker.value
      ? await getCheckerEntries(params)
      : await getEntryLogs(params)
    entries.value = res.records || []
    total.value = res.total || 0
    tableRef.value?.clearSelection()
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 条检票记录吗？此操作不可恢复。`,
      '批量删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await (isChecker.value ? deleteCheckerEntries(selectedIds.value) : deleteEntryLogs(selectedIds.value))
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    loadEntries()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '批量删除失败')
    }
  }
}

loadEntries()
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
