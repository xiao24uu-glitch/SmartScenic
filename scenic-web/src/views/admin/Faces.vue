<template>
  <div class="face-manage">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 标签1：人脸库浏览 -->
      <el-tab-pane label="人脸库浏览" name="list">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span><strong>人脸数据列表</strong></span>
              <div class="header-actions">
                <el-button v-if="selectedIds.length > 0" type="danger" @click="handleBatchDelete">
                  批量删除 ({{ selectedIds.length }})
                </el-button>
                <el-button type="danger" @click="handleCleanExpired">清理过期人脸</el-button>
              </div>
            </div>
          </template>
          <el-table :data="faces" v-loading="loading" stripe @selection-change="handleSelectionChange" ref="tableRef">
            <el-table-column type="selection" width="50" />
            <el-table-column prop="realName" label="真实姓名" width="100">
              <template #default="{ row }">{{ row.realName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="orderNo" label="关联订单" width="270">
              <template #default="{ row }">
                <span v-if="row.orderNo">{{ row.orderNo }} <el-tag v-if="row.isGroup === 1" size="small" type="warning" style="margin-left:6px">团体</el-tag></span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="人脸照片" width="100">
              <template #default="{ row }">
                <el-image v-if="row.faceImagePath" :src="row.faceImagePath" :preview-src-list="[row.faceImagePath]" preview-teleported style="width: 60px; height: 60px; object-fit: cover; border-radius: 4px;" />
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="qualityScore" label="质量分" width="90">
              <template #default="{ row }">
                {{ row.qualityScore != null ? row.qualityScore : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '有效' : '无效' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="expireTime" label="过期时间" width="170" />
            <el-table-column prop="createTime" label="创建时间" width="170" />
          </el-table>
          <el-pagination
            v-model:current-page="page" :page-size="size" :total="total"
            layout="prev, pager, next" @current-change="loadFaces"
            style="margin-top: 16px; justify-content: center;" />
        </el-card>
      </el-tab-pane>

      <!-- 标签2：人脸对比 -->
      <el-tab-pane label="人脸对比" name="compare">
        <el-card shadow="never">
          <template #header><strong>人脸对比 — 判断两张照片是否为同一个人</strong></template>
          <p style="color: #666; margin-bottom: 16px;">用于验证闸机抓拍的人脸与注册人脸是否为同一人</p>
          <el-row :gutter="24">
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>图片1（如：注册人脸）</template>
                <div class="compare-box" v-if="!compareImage1">
                  <input type="file" accept="image/*" @change="handleCompareUpload(1, $event)" ref="cmpFileInput1" style="display:none" />
                  <el-button @click="$refs.cmpFileInput1.click()">选择图片1</el-button>
                </div>
                <div v-else class="preview-area">
                  <img :src="compareImage1" class="preview-img-small" />
                  <el-button size="small" @click="compareImage1 = null; cmpBase64_1 = null; compareResult = null">重选</el-button>
                </div>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header>图片2（如：闸机抓拍）</template>
                <div class="compare-box" v-if="!compareImage2">
                  <input type="file" accept="image/*" @change="handleCompareUpload(2, $event)" ref="cmpFileInput2" style="display:none" />
                  <el-button @click="$refs.cmpFileInput2.click()">选择图片2</el-button>
                </div>
                <div v-else class="preview-area">
                  <img :src="compareImage2" class="preview-img-small" />
                  <el-button size="small" @click="compareImage2 = null; cmpBase64_2 = null; compareResult = null">重选</el-button>
                </div>
              </el-card>
            </el-col>
          </el-row>
          <el-button
            type="primary" @click="doCompare" :loading="comparing"
            :disabled="!cmpBase64_1 || !cmpBase64_2" style="margin-top: 16px; width: 100%;">
            开始对比
          </el-button>
          <div v-if="compareResult" class="result-panel">
            <el-divider />
            <el-alert
              :title="compareResult.samePerson ? '判定为同一个人' : '判定为不同人'"
              :type="compareResult.samePerson ? 'success' : 'warning'"
              :closable="false"
              show-icon>
              <template #default>
                <p><strong>相似度得分：</strong>{{ compareResult.score }} / 100</p>
                <p><strong>阈值：</strong>80（得分≥80 判定为同一个人）</p>
              </template>
            </el-alert>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFaceData, compareFaces, cleanExpiredFaces, deleteFaceData, deleteFaceDatas } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('list')

// ========== 人脸库浏览 ==========
const faces = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const selectedIds = ref([])
const tableRef = ref(null)

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(r => r.id)
}

async function loadFaces() {
  loading.value = true
  try {
    const res = await getFaceData({ page: page.value, size: size.value })
    faces.value = res.records
    total.value = res.total
    tableRef.value?.clearSelection()
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function handleCleanExpired() {
  try {
    await ElMessageBox.confirm('确定要清理所有过期的人脸数据吗？', '确认清理', { type: 'warning' })
    await cleanExpiredFaces()
    ElMessage.success('清理完成')
    loadFaces()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 条人脸数据吗？此操作不可恢复。`,
      '批量删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await deleteFaceDatas(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    loadFaces()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '批量删除失败')
    }
  }
}

// ========== 人脸对比 ==========
const compareImage1 = ref(null)
const compareImage2 = ref(null)
const cmpBase64_1 = ref(null)
const cmpBase64_2 = ref(null)
const comparing = ref(false)
const compareResult = ref(null)

function handleCompareUpload(idx, e) {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (ev) => {
    const base64 = ev.target.result
    if (idx === 1) {
      compareImage1.value = base64
      cmpBase64_1.value = base64.split(',')[1]
    } else {
      compareImage2.value = base64
      cmpBase64_2.value = base64.split(',')[1]
    }
  }
  reader.readAsDataURL(file)
}

async function doCompare() {
  if (!cmpBase64_1.value || !cmpBase64_2.value) return
  comparing.value = true
  try {
    const res = await compareFaces({
      image1: cmpBase64_1.value,
      image2: cmpBase64_2.value,
    })
    compareResult.value = res
    ElMessage.success('对比完成')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '人脸对比失败')
  } finally {
    comparing.value = false
  }
}

onMounted(() => {
  loadFaces()
})
</script>

<style scoped>
.face-manage {
  padding: 4px;
}
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
.compare-box {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  border: 2px dashed #ddd;
  border-radius: 8px;
}
.preview-area {
  text-align: center;
}
.preview-img-small {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  margin-bottom: 8px;
  display: block;
}
.result-panel {
  margin-top: 8px;
}
.result-panel p {
  margin: 4px 0;
}
</style>
