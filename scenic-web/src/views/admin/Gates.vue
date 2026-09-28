<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>闸机管理</h3>
          <div class="header-actions">
            <el-button v-if="selectedIds.length > 0" type="danger" @click="handleBatchDelete">
              批量删除 ({{ selectedIds.length }})
            </el-button>
            <el-button type="primary" @click="openAdd">新增闸机</el-button>
          </div>
        </div>
      </template>
      <el-table :data="gates" v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="gateNo" label="闸机编号" width="120" />
        <el-table-column prop="name" label="名称/位置" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑闸机' : '新增闸机'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="闸机编号">
          <el-input v-model="form.gateNo" placeholder="如 A01" />
        </el-form-item>
        <el-form-item label="名称/位置">
          <el-input v-model="form.name" placeholder="如 东门主入口" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { getAdminGates, saveGate, deleteGate, deleteGates } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const gates = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const selectedIds = ref([])
const form = reactive({ gateNo: '', name: '', status: true })

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(row => row.id)
}

async function loadGates() {
  loading.value = true
  try { gates.value = await getAdminGates() }
  catch (e) { console.error(e) }
  finally { loading.value = false }
}

function openAdd() {
  isEdit.value = false
  editId.value = null
  Object.assign(form, { gateNo: '', name: '', status: true })
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, { gateNo: row.gateNo, name: row.name, status: row.status === 1 })
  dialogVisible.value = true
}

async function handleSave() {
  try {
    const data = { ...form, status: form.status ? 1 : 0 }
    if (isEdit.value) data.id = editId.value
    await saveGate(data)
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadGates()
  } catch (e) { console.error(e) }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除闸机「${row.name || row.gateNo}」吗？此操作不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteGate(row.id)
    ElMessage.success('删除成功')
    loadGates()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 个闸机吗？此操作不可恢复。`,
      '批量删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteGates(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    loadGates()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '批量删除失败')
    }
  }
}

loadGates()
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
