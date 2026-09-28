<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>用户管理</h3>
          <div class="header-actions">
            <el-button v-if="selectedIds.length > 0" type="danger" @click="handleBatchDelete">
              批量删除 ({{ selectedIds.length }})
            </el-button>
            <el-button type="primary" @click="openAdd">新增用户</el-button>
          </div>
        </div>
      </template>
      <el-table :data="users" v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="真实姓名" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column label="角色" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r.id" size="small" style="margin-right: 4px;" :type="r.roleLevel <= 2 ? 'danger' : 'primary'">
              {{ r.roleName }}
            </el-tag>
            <span v-if="!row.roles || row.roles.length === 0" style="color: #999;">未分配</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status ? 'success' : 'danger'">{{ row.status ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="warning" @click="openRoleAssign(row)">角色</el-button>
            <el-button size="small" :type="row.status ? 'danger' : 'success'" @click="toggleStatus(row)">
              {{ row.status ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev, pager, next" @current-change="loadUsers" style="margin-top: 16px; justify-content: center;" />
    </el-card>

    <!-- 新增/编辑用户 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="450px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码" :prop="isEdit ? null : 'password'">
          <el-input v-model="form.password" type="password" show-password :placeholder="isEdit ? '留空则不修改密码' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%;">
            <el-option v-for="r in allRoles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 角色分配 -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="400px">
      <el-form label-width="60px">
        <el-form-item label="用户">
          <span>{{ roleUser?.username }} ({{ roleUser?.realName }})</span>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="selectedRoles" multiple placeholder="请选择角色" style="width: 100%;">
            <el-option v-for="r in allRoles" :key="r.id" :label="r.roleName" :value="r.id"
              :disabled="userRoleLevel > 1 && r.roleLevel <= 2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRoleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { getUsers, createUser, updateUser, updateUserRoles, updateUserStatus, getRoles, deleteUsers } from '../../api'
import { useUserStore } from '../../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const userRoleLevel = computed(() => userStore.userInfo?.roleLevel || 0)

const users = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({ username: '', password: '', realName: '', phone: '', email: '', roleIds: [] })
const selectedIds = ref([])
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }],
}

// 角色分配
const roleDialogVisible = ref(false)
const roleUser = ref(null)
const selectedRoles = ref([])
const allRoles = ref([])

async function loadUsers() {
  loading.value = true
  try {
    const res = await getUsers({ page: page.value, size: size.value })
    users.value = res.records
    total.value = res.total
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function loadRoles() {
  try {
    allRoles.value = await getRoles()
  } catch (e) { console.error(e) }
}

function openAdd() {
  isEdit.value = false
  Object.assign(form, { username: '', password: '', realName: '', phone: '', email: '', roleIds: [] })
  dialogVisible.value = true
  loadRoles()
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    username: row.username,
    password: '',
    realName: row.realName,
    phone: row.phone,
    email: row.email,
    roleIds: [],
    id: row.id,
  })
  dialogVisible.value = true
}

function openRoleAssign(row) {
  roleUser.value = row
  selectedRoles.value = (row.roles || []).map(r => r.id)
  roleDialogVisible.value = true
  loadRoles()
}

async function handleSave() {
  if (isEdit.value) {
    try {
      const body = {
        username: form.username,
        realName: form.realName,
        phone: form.phone,
        email: form.email,
      }
      if (form.password && form.password.trim()) {
        body.password = form.password.trim()
      }
      await updateUser(form.id, body)
      ElMessage.success('更新成功')
      dialogVisible.value = false
      loadUsers()
    } catch (e) { console.error(e) }
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    await createUser({ ...form })
    ElMessage.success('创建成功')
    dialogVisible.value = false
    loadUsers()
  } catch (e) { console.error(e) }
}

async function handleRoleSave() {
  try {
    await updateUserRoles(roleUser.value.id, selectedRoles.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    loadUsers()
  } catch (e) { console.error(e) }
}

async function toggleStatus(row) {
  const newStatus = row.status ? 0 : 1
  try {
    await updateUserStatus(row.id, newStatus)
    ElMessage.success('状态更新成功')
    loadUsers()
  } catch (e) { console.error(e) }
}

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(row => row.id)
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 个用户吗？此操作不可恢复。`,
      '批量删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteUsers(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    loadUsers()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '批量删除失败')
    }
  }
}

loadUsers()
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
