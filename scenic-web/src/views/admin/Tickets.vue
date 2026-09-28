<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>票种管理</h3>
          <div class="header-actions">
            <el-button v-if="selectedIds.length > 0" type="danger" @click="handleBatchDelete">
              批量删除 ({{ selectedIds.length }})
            </el-button>
            <el-button type="primary" @click="openAdd">新增票种</el-button>
          </div>
        </div>
      </template>
      <el-table :data="tickets" v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="name" label="票种名称" />
        <el-table-column label="价格">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="totalStock" label="总库存" />
        <el-table-column prop="dailyStock" label="每日库存" />
        <el-table-column prop="soldCount" label="已售" />
        <el-table-column label="团体票">
          <template #default="{ row }">
            <el-tag v-if="row.isGroup" type="warning" size="small">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status ? 'success' : 'danger'">{{ row.status ? '在售' : '停售' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑票种' : '新增票种'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="票种名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="总库存" prop="totalStock">
          <el-input-number v-model="form.totalStock" :min="0" />
        </el-form-item>
        <el-form-item label="每日库存" prop="dailyStock">
          <el-input-number v-model="form.dailyStock" :min="0" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
        <el-form-item label="团体票">
          <el-switch v-model="form.isGroup" />
          <span style="margin-left: 8px; color: #909399; font-size: 12px;">预约天数请在「系统配置」中统一设置</span>
        </el-form-item>
        <el-form-item v-if="form.isGroup" label="最少人数" prop="minGroupSize">
          <el-input-number v-model="form.minGroupSize" :min="1" :max="form.totalStock || 9999" placeholder="团体最少购买人数" />
          <span style="margin-left: 8px; color: #909399; font-size: 12px;">达到此人数才可按团体价购买</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" active-text="在售" inactive-text="停售" />
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
import { getAdminTicketTypes, createTicketType, updateTicketType, deleteTicketType, deleteTicketTypes } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const tickets = ref([])
const loading = ref(false)
const selectedIds = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)

const form = reactive({
  name: '', price: 0, totalStock: 0, dailyStock: 0,
  description: '', isGroup: false, minGroupSize: 10, status: true,
})

const rules = {
  name: [{ required: true, message: '请输入票种名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
}

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(row => row.id)
}

async function loadTickets() {
  loading.value = true
  try { tickets.value = await getAdminTicketTypes() }
  catch (e) { console.error(e) }
  finally { loading.value = false }
}

function openAdd() {
  isEdit.value = false
  editId.value = null
  Object.assign(form, { name: '', price: 0, totalStock: 0, dailyStock: 0, description: '', isGroup: false, minGroupSize: 10, status: true })
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    name: row.name, price: row.price, totalStock: row.totalStock,
    dailyStock: row.dailyStock, description: row.description,
    isGroup: !!row.isGroup, minGroupSize: row.minGroupSize || 10, status: !!row.status,
  })
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const data = {
    name: form.name, price: form.price, totalStock: form.totalStock,
    dailyStock: form.dailyStock, description: form.description,
    isGroup: form.isGroup ? 1 : 0,
    minGroupSize: form.isGroup ? form.minGroupSize : null,
    status: form.status ? 1 : 0,
  }

  try {
    if (isEdit.value) {
      await updateTicketType(editId.value, data)
    } else {
      await createTicketType(data)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadTickets()
  } catch (e) { console.error(e) }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除票种「${row.name}」吗？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteTicketType(row.id)
    ElMessage.success('删除成功')
    loadTickets()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 个票种吗？此操作不可恢复。`,
      '批量删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteTicketTypes(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    loadTickets()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '批量删除失败')
    }
  }
}

loadTickets()
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
