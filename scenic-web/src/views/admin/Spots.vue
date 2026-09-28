<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>景点管理</h3>
          <div class="header-actions">
            <el-button v-if="selectedIds.length > 0" type="danger" @click="handleBatchDelete">
              批量删除 ({{ selectedIds.length }})
            </el-button>
            <el-button type="primary" @click="openAdd">新增景点</el-button>
          </div>
        </div>
      </template>
      <p class="drag-tip">按住拖拽行可调整显示顺序，松开后自动保存</p>
      <el-table ref="tableRef" :data="spots" v-loading="loading" row-key="id" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column label="拖拽排序" width="100" align="center">
          <template #default>
            <span class="drag-handle"><i class="el-icon-rank"></i> ☰</span>
          </template>
        </el-table-column>
        <el-table-column label="景点图片" width="90">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              :key="row.id + '_' + row.imageUrl"
              :src="row.imageUrl"
              :preview-src-list="[row.imageUrl]"
              :initial-index="0"
              fit="cover"
              preview-teleported
              style="width: 50px; height: 50px; border-radius: 4px;"
            />
            <span v-else style="color: #c0c4cc; font-size: 12px;">暂无</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="景点名称" />
        <el-table-column prop="description" label="景点介绍" />
        <el-table-column label="坐标" width="170">
          <template #default="{ row }">
            <span v-if="row.longitude || row.latitude" class="coord-text">
              {{ row.longitude?.toFixed(6) }}, {{ row.latitude?.toFixed(6) }}
            </span>
            <span v-else style="color: #c0c4cc;">未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status ? 'success' : 'info'">{{ row.status ? '显示' : '隐藏' }}</el-tag>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑景点' : '新增景点'" width="560px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="介绍">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="景点图片">
          <div class="spot-image-upload">
            <div class="image-preview" v-if="form.imageUrl">
              <img :src="form.imageUrl" alt="景点图片" />
              <div class="image-mask">
                <span @click="triggerUpload">更换</span>
                <span @click="form.imageUrl = ''">删除</span>
              </div>
            </div>
            <div class="image-placeholder" v-else @click="triggerUpload">
              <el-icon :size="28"><Plus /></el-icon>
              <span>上传图片</span>
            </div>
            <input ref="fileInputRef" type="file" accept="image/*" style="display:none;" @change="handleFileChange" />
          </div>
        </el-form-item>
        <el-form-item label="经度">
          <el-input-number v-model="form.longitude" :precision="6" :min="-180" :max="180" style="width: 100%;" placeholder="如 116.397428" />
        </el-form-item>
        <el-form-item label="纬度">
          <el-input-number v-model="form.latitude" :precision="6" :min="-90" :max="90" style="width: 100%;" placeholder="如 39.90923" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" active-text="显示" inactive-text="隐藏" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import { getAdminSpots, saveSpot, deleteSpot, deleteSpots, updateSpotSort, uploadImage } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import Sortable from 'sortablejs'

const tableRef = ref(null)
const fileInputRef = ref(null)
const spots = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const selectedIds = ref([])
const form = reactive({
  name: '', description: '', sortOrder: 0, status: true,
  longitude: null, latitude: null, imageUrl: ''
})

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(row => row.id)
}

function triggerUpload() {
  fileInputRef.value?.click()
}

async function handleFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  try {
    saving.value = true
    const result = await uploadImage(file, 'spot')
    form.imageUrl = result.url
    ElMessage.success('图片上传成功')
  } catch (err) {
    ElMessage.error('图片上传失败')
    console.error(err)
  } finally {
    saving.value = false
    if (fileInputRef.value) fileInputRef.value.value = ''
  }
}

function initSortable() {
  nextTick(() => {
    const tbody = tableRef.value?.$el?.querySelector('.el-table__body-wrapper tbody')
    if (!tbody) return
    new Sortable(tbody, {
      handle: '.drag-handle',
      animation: 200,
      ghostClass: 'ghost-row',
      onEnd: async (evt) => {
        if (evt.oldIndex === evt.newIndex) return
        const item = spots.value.splice(evt.oldIndex, 1)[0]
        spots.value.splice(evt.newIndex, 0, item)
        const updates = spots.value.map((s, i) => ({ id: s.id, sortOrder: i }))
        await updateSpotSort(updates)
        loadSpots()
      },
    })
  })
}

async function loadSpots() {
  loading.value = true
  try { spots.value = await getAdminSpots(); initSortable() }
  catch (e) { console.error(e) }
  finally { loading.value = false }
}

function openAdd() {
  isEdit.value = false
  editId.value = null
  const nextSort = spots.value.length > 0 ? spots.value[spots.value.length - 1].sortOrder + 1 : 0
  Object.assign(form, {
    name: '', description: '', sortOrder: nextSort, status: true,
    longitude: null, latitude: null, imageUrl: ''
  })
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    name: row.name, description: row.description, sortOrder: row.sortOrder,
    status: !!row.status, longitude: row.longitude, latitude: row.latitude,
    imageUrl: row.imageUrl || ''
  })
  dialogVisible.value = true
}

async function handleSave() {
  try {
    saving.value = true
    const data = { ...form, status: form.status ? 1 : 0 }
    if (isEdit.value) data.id = editId.value
    await saveSpot(data)
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadSpots()
  } catch (e) { console.error(e) }
  finally { saving.value = false }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除景点「${row.name}」吗？此操作不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteSpot(row.id)
    ElMessage.success('删除成功')
    loadSpots()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 个景点吗？此操作不可恢复。`,
      '批量删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteSpots(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    loadSpots()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '批量删除失败')
    }
  }
}

loadSpots()
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
.drag-tip {
  color: #909399;
  font-size: 12px;
  margin-bottom: 8px;
}
.drag-handle {
  cursor: grab;
  font-size: 18px;
  color: #c0c4cc;
  user-select: none;
}
.drag-handle:hover { color: #409eff; }
.drag-handle:active { cursor: grabbing; }
::deep(.ghost-row) > td {
  opacity: 0.4;
  background-color: #ecf5ff !important;
}
.coord-text {
  font-family: monospace;
  font-size: 12px;
  color: #606266;
}

.spot-image-upload {
  display: flex;
  align-items: flex-start;
}
.image-preview {
  position: relative;
  width: 160px;
  height: 120px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
}
.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.image-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  opacity: 0;
  transition: opacity 0.2s;
}
.image-preview:hover .image-mask {
  opacity: 1;
}
.image-mask span {
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 10px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 4px;
}
.image-mask span:hover {
  background: rgba(255, 255, 255, 0.2);
}
.image-placeholder {
  width: 160px;
  height: 120px;
  border: 2px dashed #dcdfe6;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #c0c4cc;
  cursor: pointer;
  font-size: 13px;
  transition: border-color 0.2s;
}
.image-placeholder:hover {
  border-color: #409eff;
  color: #409eff;
}
</style>
