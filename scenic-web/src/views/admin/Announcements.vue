<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>公告管理</h3>
          <el-button type="primary" @click="openDialog(null)">发布公告</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="公告标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeMap[row.type]?.tag || 'info'" size="small">
              {{ typeMap[row.type]?.label || '一般公告' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isTop" label="置顶" width="70">
          <template #default="{ row }">
            <el-tag :type="row.isTop ? 'danger' : 'info'" size="small">
              {{ row.isTop ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除该公告吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑公告' : '发布公告'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="公告类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option :value="1" label="一般公告" />
            <el-option :value="2" label="紧急通知" />
            <el-option :value="3" label="活动公告" />
          </el-select>
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="是否置顶">
          <el-switch v-model="form.isTop" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="显示状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="显示" inactive-text="隐藏" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAdminAnnouncements, createAnnouncement, updateAnnouncement, deleteAnnouncement } from '../../api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  title: '',
  content: '',
  type: 1,
  isTop: 0,
  status: 1,
})

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择公告类型', trigger: 'change' }],
}

const typeMap = {
  1: { label: '一般公告', tag: 'info' },
  2: { label: '紧急通知', tag: 'danger' },
  3: { label: '活动公告', tag: 'warning' },
}

async function loadData() {
  loading.value = true
  try {
    const res = await getAdminAnnouncements({ page: currentPage.value, size: pageSize.value })
    tableData.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  if (row) {
    editingId.value = row.id
    form.title = row.title
    form.content = row.content || ''
    form.type = row.type || 1
    form.isTop = row.isTop || 0
    form.status = row.status || 1
  } else {
    editingId.value = null
    form.title = ''
    form.content = ''
    form.type = 1
    form.isTop = 0
    form.status = 1
  }
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editingId.value) {
      await updateAnnouncement(editingId.value, { ...form })
      ElMessage.success('公告更新成功')
    } else {
      await createAnnouncement({ ...form })
      ElMessage.success('公告发布成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(id) {
  try {
    await deleteAnnouncement(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-header h3 { margin: 0; font-size: 16px; }
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
