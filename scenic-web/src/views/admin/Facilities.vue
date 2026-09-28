<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>设施管理</h3>
          <div class="header-actions">
            <el-button v-if="selectedIds.length > 0" type="danger" @click="handleBatchDelete">
              批量删除 ({{ selectedIds.length }})
            </el-button>
            <el-button type="primary" @click="openAdd">新增设施</el-button>
          </div>
        </div>
      </template>
      <el-table :data="facilities" v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ typeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column label="设施图片" width="90">
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
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="description" label="位置描述" />
        <el-table-column label="坐标" width="180">
          <template #default="{ row }">
            <span v-if="row.longitude || row.latitude" class="coord-text">
              {{ row.longitude?.toFixed(6) }}, {{ row.latitude?.toFixed(6) }}
            </span>
            <span v-else style="color: #c0c4cc;">未设置</span>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑设施' : '新增设施'" width="560px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="类型">
          <el-select v-model="form.type">
            <el-option :value="1" label="卫生间" />
            <el-option :value="2" label="餐饮" />
            <el-option :value="3" label="停车场" />
            <el-option :value="4" label="医疗" />
            <el-option :value="5" label="游客中心" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="位置描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="设施图片">
          <div class="facility-image-upload">
            <div class="image-preview" v-if="form.imageUrl">
              <img :src="form.imageUrl" alt="设施图片" />
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
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { getAdminFacilities, saveFacility, deleteFacility, deleteFacilities, uploadImage } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const fileInputRef = ref(null)
const facilities = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const selectedIds = ref([])
const form = reactive({ name: '', type: 1, description: '', longitude: null, latitude: null, imageUrl: '' })

function typeLabel(t) {
  const map = { 1: '卫生间', 2: '餐饮', 3: '停车场', 4: '医疗', 5: '游客中心' }
  return map[t] || '其他'
}

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
    const result = await uploadImage(file, 'facility')
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

async function loadFacilities() {
  loading.value = true
  try { facilities.value = await getAdminFacilities() }
  catch (e) { console.error(e) }
  finally { loading.value = false }
}

function openAdd() {
  isEdit.value = false; editId.value = null
  Object.assign(form, { name: '', type: 1, description: '', longitude: null, latitude: null, imageUrl: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true; editId.value = row.id
  Object.assign(form, {
    name: row.name, type: row.type, description: row.description,
    longitude: row.longitude, latitude: row.latitude,
    imageUrl: row.imageUrl || ''
  })
  dialogVisible.value = true
}

async function handleSave() {
  try {
    saving.value = true
    const data = { ...form }
    if (isEdit.value) data.id = editId.value
    await saveFacility(data)
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadFacilities()
  } catch (e) { console.error(e) }
  finally { saving.value = false }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除设施「${row.name}」吗？此操作不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteFacility(row.id)
    ElMessage.success('删除成功')
    loadFacilities()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 个设施吗？此操作不可恢复。`,
      '批量删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteFacilities(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    loadFacilities()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '批量删除失败')
    }
  }
}

loadFacilities()
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
.coord-text {
  font-family: monospace;
  font-size: 12px;
  color: #606266;
}

.facility-image-upload {
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
