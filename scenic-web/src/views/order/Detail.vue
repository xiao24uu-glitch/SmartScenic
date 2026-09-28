<template>
  <div class="order-detail-page">
    <!-- ====== 页面头部 ====== -->
    <div class="page-hero">
      <div class="section-header">
        <span class="section-tag">ORDER DETAIL</span>
        <h2 class="section-title">订单详情</h2>
        <p class="section-subtitle">查看订单详细信息与票种明细</p>
      </div>
    </div>

    <div class="content-wrapper">
      <div class="section" v-observe>
        <div class="detail-card" v-loading="loading">
          <div class="detail-card-header">
            <div class="order-no-row">
              <span class="order-no-label">订单编号</span>
              <span class="order-no-value">{{ order.orderNo }}</span>
            </div>
            <el-tag :type="statusType(order.status)" effect="plain" round size="large">{{ order.statusText }}</el-tag>
          </div>

          <div class="detail-card-body">
            <el-descriptions :column="2" border class="detail-descriptions">
              <el-descriptions-item label="景区">{{ order.scenicName }}</el-descriptions-item>
              <el-descriptions-item label="游览日期">{{ order.visitDate }}</el-descriptions-item>
              <el-descriptions-item label="总金额">
                <span class="amount-highlight">¥{{ order.totalAmount }}</span>
              </el-descriptions-item>
              <el-descriptions-item v-if="order.discountAmount > 0" label="优惠金额">
                <span class="amount-highlight" style="color: #67C23A;">-¥{{ order.discountAmount }}（{{ order.couponName || '优惠券' }}）</span>
              </el-descriptions-item>
              <el-descriptions-item label="实付金额">
                <span class="amount-highlight">¥{{ order.payAmount || 0 }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="支付方式">{{ payTypeText(order.payType) }}</el-descriptions-item>
              <el-descriptions-item label="支付时间">{{ order.payTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ order.createTime }}</el-descriptions-item>
            </el-descriptions>

            <!-- 票种明细 -->
            <h3 class="sub-heading">票种明细</h3>
            <el-table :data="order.items || []" style="width: 100%" class="modern-table">
              <el-table-column prop="ticketTypeName" label="票种" />
              <el-table-column prop="unitPrice" label="单价">
                <template #default="{ row }">
                  <span class="amount-text">¥{{ row.unitPrice }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" />
              <el-table-column label="小计">
                <template #default="{ row }">
                  <span class="amount-text">¥{{ row.subtotal }}</span>
                </template>
              </el-table-column>
            </el-table>

            <!-- 待支付操作区 -->
            <div v-if="order.status === 0" class="action-area">
              <div class="action-status">
                <span class="action-status-icon">⏳</span>
                <div>
                  <h4>订单待支付</h4>
                  <p>请尽快完成支付，以免订单过期</p>
                </div>
              </div>
              <div class="action-buttons">
                <button class="detail-btn detail-btn--primary" @click="goPay">去支付 ¥{{ order.totalAmount }}</button>
                <button class="detail-btn detail-btn--ghost" @click="$router.push('/orders')">返回订单列表</button>
              </div>
            </div>

            <!-- 已支付 -->
            <div v-else-if="order.status === 1" class="action-area action-area--done">
              <div class="action-status">
                <span class="action-status-icon">✅</span>
                <div>
                  <h4>订单已支付</h4>
                  <p>您可以在订单列表中进行退款或修改操作</p>
                </div>
              </div>
              <div class="action-buttons">
                <button class="detail-btn detail-btn--outline" @click="$router.push('/orders')">返回订单列表</button>
              </div>
            </div>

            <!-- 操作记录 -->
            <template v-if="order.status !== 0">
              <h3 class="sub-heading">操作记录</h3>
              <div class="timeline-box">
                <el-timeline>
                  <!-- 1. 订单创建 -->
                  <el-timeline-item :timestamp="order.createTime" placement="top" type="primary">
                    订单创建
                  </el-timeline-item>
                  <!-- 2. 使用优惠券 -->
                  <el-timeline-item
                    v-if="order.discountAmount > 0"
                    :timestamp="order.createTime"
                    placement="top"
                    type="info"
                  >
                    使用优惠券「{{ order.couponName || '优惠' }}」省 ¥{{ order.discountAmount }}
                  </el-timeline-item>
                  <!-- 3. 支付成功 -->
                  <el-timeline-item
                    v-if="order.payTime"
                    :timestamp="order.payTime"
                    placement="top"
                    type="success"
                  >
                    支付成功 — ¥{{ order.payAmount || order.totalAmount }}
                  </el-timeline-item>
                  <!-- 4. 人脸录入进度 -->
                  <el-timeline-item
                    v-if="order.faceCount > 0"
                    :timestamp="order.faceTime || ''"
                    placement="top"
                    type="primary"
                  >
                    已录入 {{ order.faceCount }} / {{ order.totalTickets }} 张人脸
                  </el-timeline-item>
                  <!-- 5. 申请修改游览日期 -->
                  <el-timeline-item
                    v-if="order.status === 4 && order.pendingVisitDate"
                    :timestamp="''"
                    placement="top"
                    type="warning"
                  >
                    申请修改游览日期为 {{ order.pendingVisitDate }}
                  </el-timeline-item>
                  <!-- 6. 游览日期已过期 -->
                  <el-timeline-item
                    v-if="order.status === 1 && isVisitDateExpired(order)"
                    :timestamp="order.visitDate"
                    placement="top"
                    type="warning"
                  >
                    游览日期已过（{{ order.visitDate }}），尚未入园
                  </el-timeline-item>
                  <!-- 7. 首批游客入园 -->
                  <el-timeline-item
                    v-if="order.entryCount > 0"
                    :timestamp="order.firstEntryTime || ''"
                    placement="top"
                    type="success"
                  >
                    {{ order.entryCount }} 人已入园
                    <span v-if="order.inParkCount > 0" style="color: #67C23A; margin-left: 6px;">（当前在园 {{ order.inParkCount }} 人）</span>
                  </el-timeline-item>
                  <!-- 8. 游客全部出园 -->
                  <el-timeline-item
                    v-if="order.status === 6 && order.lastExitTime"
                    :timestamp="order.lastExitTime"
                    placement="top"
                    type="info"
                  >
                    游客已全部出园
                  </el-timeline-item>
                  <!-- 9. 订单已取消 -->
                  <el-timeline-item v-if="order.status === 2" placement="top" type="danger">
                    订单已取消
                  </el-timeline-item>
                  <!-- 10. 订单已退款 -->
                  <el-timeline-item v-if="order.status === 3" placement="top" type="warning">
                    订单已退款
                  </el-timeline-item>
                </el-timeline>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useScenicStore } from '../../stores/scenic'
import { getOrderDetail } from '../../api'

const route = useRoute()
const router = useRouter()
const scenicStore = useScenicStore()
const order = ref({})
const loading = ref(false)

function statusType(status) {
  const map = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger', 4: 'warning', 5: 'success', 6: 'info' }
  return map[status] || 'info'
}

function payTypeText(type) {
  const map = { 0: '模拟支付', 1: '微信支付', 2: '支付宝' }
  return map[type] || '未知'
}

function goPay() {
  router.push(`/pay?orderNo=${order.value.orderNo}`)
}

/** 判断游览日期是否已过期 */
function isVisitDateExpired(row) {
  if (!row.visitDate) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const visitDate = new Date(row.visitDate)
  visitDate.setHours(0, 0, 0, 0)
  return visitDate < today
}

// 滚动入场动画
function setupObserver() {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible')
        observer.unobserve(entry.target)
      }
    })
  }, { threshold: 0.1, rootMargin: '0px 0px -40px 0px' })
  document.querySelectorAll('[v-observe]').forEach(el => observer.observe(el))
}

onMounted(async () => {
  loading.value = true
  try {
    order.value = await getOrderDetail(route.params.orderNo)
  } catch (e) {
    console.error('加载订单详情失败', e)
  } finally {
    loading.value = false
  }
  setTimeout(setupObserver, 200)
})
</script>

<style scoped>
/* ====== CSS Variables ====== */
:root {
  --radius: 16px;
  --shadow-sm: 0 1px 3px rgba(0,0,0,0.06);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.08);
  --shadow-lg: 0 12px 40px rgba(0,0,0,0.12);
  --transition: 0.35s cubic-bezier(0.22, 0.61, 0.36, 1);
}
.order-detail-page { margin: -24px; }
/* ====== 页面头部 ====== */
.page-hero {
  padding: 60px 24px 50px;
  text-align: center;
}
.page-hero .section-tag { color: rgba(255,255,255,0.7); }
.page-hero .section-title { color: #fff; }
.page-hero .section-subtitle { color: rgba(255,255,255,0.8); }

.section-header { margin-bottom: 0; }
.section-tag {
  display: inline-block; font-size: 12px; font-weight: 700; letter-spacing: 3px;
  margin-bottom: 12px;
}
.section-title {
  font-size: clamp(28px, 4vw, 38px); font-weight: 800; margin: 0 0 12px;
  letter-spacing: -0.5px;
}
.section-subtitle { font-size: 16px; margin: 0; }

/* ====== 内容区 ====== */
.content-wrapper { background: transparent; padding: 0 24px 80px; }
.section { max-width: 900px; margin: 0 auto; padding-top: 40px; }

/* 入场动画 */
[v-observe] { opacity: 0; transform: translateY(30px); transition: opacity 0.7s ease, transform 0.7s cubic-bezier(0.22, 0.61, 0.36, 1); }
[v-observe].visible { opacity: 1; transform: translateY(0); }

/* ====== 详情卡片 ====== */
.detail-card {
  background: rgba(255,255,255,0.85); backdrop-filter: blur(10px);
  border-radius: 20px; overflow: hidden;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition);
}
.detail-card:hover { box-shadow: var(--shadow-md); }
.detail-card-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 28px; border-bottom: 1px solid #f1f5f9;
}
.order-no-row { display: flex; align-items: center; gap: 10px; }
.order-no-label { font-size: 14px; color: #94a3b8; font-weight: 500; }
.order-no-value { font-size: 15px; font-weight: 700; color: #1a1a2e; }

.detail-card-body { padding: 24px 28px; }
.sub-heading { font-size: 16px; font-weight: 700; color: #1a1a2e; margin: 28px 0 14px; }

.detail-descriptions {
  --el-descriptions-item-bordered-label-background: #f8fafc;
}
.amount-highlight { font-weight: 700; color: #E6A23C; font-size: 15px; }

/* 表格 */
.modern-table {
  --el-table-border-color: #f1f5f9;
  --el-table-header-bg-color: #f8fafc;
  --el-table-row-hover-bg-color: #f8fafc;
}
.amount-text { font-weight: 600; color: #E6A23C; }

/* ====== 操作区 ====== */
.action-area {
  margin-top: 28px; padding: 24px;
  background: #fefbf0; border-radius: 14px;
  border: 1px solid #fde8b5;
}
.action-area--done { background: #f0faf4; border-color: #c8e6c9; }
.action-status {
  display: flex; align-items: center; gap: 12px; margin-bottom: 20px;
}
.action-status-icon { font-size: 28px; }
.action-status h4 { margin: 0 0 4px; font-size: 16px; font-weight: 700; color: #1a1a2e; }
.action-status p { margin: 0; font-size: 13px; color: #94a3b8; }
.action-buttons { display: flex; gap: 12px; flex-wrap: wrap; }

.detail-btn {
  border: none; border-radius: 12px; padding: 12px 28px;
  font-size: 15px; font-weight: 600; cursor: pointer;
  transition: all var(--transition); display: inline-flex; align-items: center; gap: 6px;
}
.detail-btn--primary {
  background: v-bind('scenicStore.primaryColor'); color: #fff;
}
.detail-btn--primary:hover { filter: brightness(1.1); transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.15); }
.detail-btn--outline {
  background: transparent; color: v-bind('scenicStore.primaryColor');
  border: 2px solid v-bind('scenicStore.primaryColor');
}
.detail-btn--outline:hover { background: v-bind('scenicStore.primaryColor'); color: #fff; }
.detail-btn--ghost {
  background: transparent; color: #606266; border: 1px solid #dcdfe6;
}
.detail-btn--ghost:hover { border-color: #909399; background: #f5f5f5; }

/* ====== 操作记录时间轴 ====== */
.timeline-box {
  padding: 16px 8px;
  --el-timeline-node-size-normal: 14px;
}

/* ====== 响应式 ====== */
@media (max-width: 600px) {
  .detail-card-header { flex-direction: column; align-items: flex-start; gap: 10px; }
  .detail-card-body { padding: 16px; }
  .detail-btn { padding: 10px 20px; font-size: 14px; }
}
</style>
