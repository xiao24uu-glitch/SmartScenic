<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>优惠券管理</h3>
          <div class="header-actions">
            <el-button v-if="selectedIds.length > 0" type="success" @click="handleBatchEnable">
              批量启用 ({{ selectedIds.length }})
            </el-button>
            <el-button v-if="selectedIds.length > 0" type="warning" @click="handleBatchDisable">
              批量停用 ({{ selectedIds.length }})
            </el-button>
            <el-button v-if="selectedIds.length > 0" type="danger" @click="handleBatchDelete">
              批量删除 ({{ selectedIds.length }})
            </el-button>
            <el-button type="primary" @click="openCreateDialog">
              <el-icon><Plus /></el-icon> 创建优惠券
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="couponList" v-loading="loading" @selection-change="handleSelectionChange" stripe class="modern-table">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'success' : 'warning'" size="small">
              {{ row.type === 1 ? '满减券' : '折扣券' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="门槛" width="90">
          <template #default="{ row }">¥{{ row.threshold }}</template>
        </el-table-column>
        <el-table-column label="优惠值" width="110">
          <template #default="{ row }">
            <template v-if="row.type === 1">减¥{{ row.discountValue }}</template>
            <template v-else>{{ (row.discountValue * 100).toFixed(0) }}折</template>
          </template>
        </el-table-column>
        <el-table-column label="最大优惠" width="90">
          <template #default="{ row }">
            <template v-if="row.maxDiscount">¥{{ row.maxDiscount }}</template>
            <template v-else>-</template>
          </template>
        </el-table-column>
        <el-table-column label="领取/总量" width="100">
          <template #default="{ row }">{{ row.receivedCount }}/{{ row.totalCount }}</template>
        </el-table-column>
        <el-table-column label="有效期" width="80">
          <template #default="{ row }">{{ row.validDays }}天</template>
        </el-table-column>
        <el-table-column label="新用户" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.maxOrderCount != null && row.maxOrderCount >= 0" type="info" size="small">
              ≤{{ row.maxOrderCount }}笔
            </el-tag>
            <template v-else>-</template>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="handleToggle(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">暂无优惠券</div>
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadList"
        background
        style="margin-top: 16px; justify-content: center;"
      />
    </el-card>

    <!-- 创建/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑优惠券' : '创建优惠券'"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="100px" ref="formRef">
        <el-form-item label="优惠券名称" required>
          <el-input v-model="form.name" placeholder="如：新用户立减10元" />
        </el-form-item>
        <el-form-item label="优惠类型" required>
          <el-radio-group v-model="form.type">
            <el-radio :label="1">满减券</el-radio>
            <el-radio :label="2">折扣券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="使用门槛">
          <el-input-number v-model="form.threshold" :min="0" :precision="2" :step="10" style="width: 100%;" />
          <span class="form-hint">满多少元可用，0表示无门槛</span>
        </el-form-item>
        <el-form-item :label="form.type === 1 ? '优惠金额' : '折扣率'" required>
          <template v-if="form.type === 1">
            <el-input-number v-model="form.discountValue" :min="0.01" :precision="2" :step="10" style="width: 100%;" />
            <span class="form-hint">直接减免的金额（元）</span>
          </template>
          <template v-else>
            <el-input-number v-model="discountPercent" :min="1" :max="99" :step="5" style="width: 100%;" @change="onDiscountPercentChange" />
            <span class="form-hint">折扣百分比（如 85 表示 8.5 折）</span>
          </template>
        </el-form-item>
        <el-form-item v-if="form.type === 2" label="最大优惠">
          <el-input-number v-model="form.maxDiscount" :min="0" :precision="2" :step="10" style="width: 100%;" />
          <span class="form-hint">折扣券最多优惠金额，留空不限制</span>
        </el-form-item>
        <el-form-item label="发行总量" required>
          <el-input-number v-model="form.totalCount" :min="1" :max="100000" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="每人限领">
          <el-input-number v-model="form.perUserLimit" :min="1" :max="99" style="width: 100%;" />
        </el-form-item>
        <el-form-item v-if="isEdit" label="当前领取量">
          <div style="display: flex; align-items: center; gap: 12px;">
            <span style="color: #303133; font-weight: bold;">{{ form.receivedCount ?? 0 }}</span>
            <el-button
              size="small"
              type="warning"
              :disabled="!form.receivedCount || form.receivedCount <= 0"
              @click="handleClearReceivedCount"
            >
              清空领取量
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="有效期(天)" required>
          <el-input-number v-model="form.validDays" :min="1" :max="365" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="新用户专属">
          <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
            <el-switch v-model="newUserEnabled" active-text="仅新用户可领" />
            <template v-if="newUserEnabled">
              <span style="color: #606266; font-size: 13px;">最多</span>
              <el-input-number v-model="form.maxOrderCount" :min="0" :max="99" style="width: 100px;" />
              <span style="color: #606266; font-size: 13px;">笔已支付订单</span>
            </template>
          </div>
          <span class="form-hint">开启后只有历史订单数≤设定值的用户才能领取</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">
          {{ isEdit ? '保存修改' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getAdminCoupons, createCoupon, updateCoupon, toggleCoupon, deleteCoupons, batchToggleCoupons, deleteCoupon, clearCouponReceivedCount } from '../../api'

const couponList = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const selectedIds = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const discountPercent = ref(85)

const form = ref({
  id: null,
  name: '',
  type: 1,
  threshold: 0,
  discountValue: 10,
  maxDiscount: null,
  totalCount: 100,
  perUserLimit: 1,
  validDays: 30,
  maxOrderCount: -1,
})

const newUserEnabled = ref(false)

// 关闭新用户开关时重置为不限
watch(newUserEnabled, (val) => {
  if (!val) form.value.maxOrderCount = -1
})

function resetForm() {
  form.value = {
    id: null, name: '', type: 1, threshold: 0, discountValue: 10,
    maxDiscount: null, totalCount: 100, perUserLimit: 1, validDays: 30,
    maxOrderCount: -1,
  }
  discountPercent.value = 85
  newUserEnabled.value = false
}

function onDiscountPercentChange(val) {
  form.value.discountValue = val / 100
}

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(row => row.id)
}

function openCreateDialog() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  form.value = { ...row }
  newUserEnabled.value = row.maxOrderCount != null && row.maxOrderCount >= 0
  if (row.type === 2) {
    discountPercent.value = Math.round(row.discountValue * 100)
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.name.trim()) { ElMessage.warning('请输入优惠券名称'); return }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateCoupon(form.value.id, form.value)
      ElMessage.success('优惠券已更新')
    } else {
      await createCoupon(form.value)
      ElMessage.success('优惠券已创建')
    }
    dialogVisible.value = false
    loadList()
  } catch (e) {
    console.error('保存优惠券失败', e)
  } finally {
    saving.value = false
  }
}

async function handleToggle(row) {
  try {
    await toggleCoupon(row.id)
    ElMessage.success(`优惠券已${row.status === 1 ? '停用' : '启用'}`)
    loadList()
  } catch (e) {
    console.error('操作失败', e)
  }
}

async function handleClearReceivedCount() {
  try {
    await ElMessageBox.confirm(
      `确定要清空优惠券「${form.value.name}」的领取量吗？当前领取量: ${form.value.receivedCount}`,
      '清空领取量确认',
      { type: 'warning', confirmButtonText: '确定清空', cancelButtonText: '取消' }
    )
    await clearCouponReceivedCount(form.value.id)
    form.value.receivedCount = 0
    ElMessage.success('领取量已清空')
    loadList()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '清空领取量失败')
    }
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除优惠券「${row.name}」吗？此操作不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteCoupon(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '删除失败')
    }
  }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 张优惠券吗？此操作不可恢复。`,
      '批量删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteCoupons(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    loadList()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '批量删除失败')
    }
  }
}

async function handleBatchEnable() {
  try {
    await batchToggleCoupons(selectedIds.value, 1)
    ElMessage.success('批量启用成功')
    selectedIds.value = []
    loadList()
  } catch (e) {
    console.error('批量启用失败', e)
  }
}

async function handleBatchDisable() {
  try {
    await batchToggleCoupons(selectedIds.value, 0)
    ElMessage.success('批量停用成功')
    selectedIds.value = []
    loadList()
  } catch (e) {
    console.error('批量停用失败', e)
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await getAdminCoupons({ page: page.value, size: size.value })
    couponList.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.error('加载优惠券列表失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => loadList())
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-header h3 { margin: 0; font-size: 16px; color: #1a1a2e; }
.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}
.modern-table {
  --el-table-border-color: #f1f5f9;
  --el-table-header-bg-color: #f8fafc;
}
.empty-state { padding: 40px; text-align: center; color: #c0c4cc; font-size: 14px; }
.form-hint { font-size: 12px; color: #909399; margin-left: 8px; display: inline-block; }
</style>
