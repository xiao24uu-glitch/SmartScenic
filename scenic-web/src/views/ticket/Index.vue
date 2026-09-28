<template>
  <div class="ticket-page">
    <!-- ====== 页面头部 ====== -->
    <div class="page-hero">
      <div class="section-header">
        <span class="section-tag">TICKETS</span>
        <h2 class="section-title">门票选购</h2>
        <p class="section-subtitle">选择适合您的票种，开启智慧之旅</p>
      </div>
    </div>

    <div class="content-wrapper">
      <!-- 暂停运营提示 -->
      <div class="suspend-banner" v-if="scenicStore.scenicStatus === 0">
        <div class="suspend-icon">⚠️</div>
        <div>
          <div class="suspend-title">景区暂停运营</div>
          <div class="suspend-desc">当前景区处于暂停运营状态，暂不支持购票，请稍后再试</div>
        </div>
      </div>

      <!-- 日期选择 -->
      <div class="section section-filter" v-observe>
        <div class="filter-bar">
          <span class="filter-label">游览日期</span>
          <el-date-picker v-model="visitDate" type="date" placeholder="请选择日期" :disabled-date="disabledDate" class="filter-picker" />
          <span class="filter-hint" v-if="!visitDate">请先选择游览日期</span>
        </div>
      </div>

      <!-- 票种列表 -->
      <div class="section" v-observe>
        <div class="tickets-grid">
          <div class="ticket-card" v-for="(ticket, i) in ticketTypes" :key="ticket.id"
               :class="{ disabled: !ticket.status || (ticket.dailyStock - ticket.soldCount) <= 0 }"
               :style="{ animationDelay: `${i * 0.1}s` }">
            <div class="ticket-badge" v-if="ticket.isGroup">
              <span>团体票</span>
            </div>
            <div class="ticket-badge sold-out" v-if="(ticket.dailyStock - ticket.soldCount) <= 0">
              <span>已售罄</span>
            </div>
            <h3 class="ticket-name">{{ ticket.name }}</h3>
            <div class="ticket-price">
              <span class="price-symbol">¥</span>
              <span class="price-num">{{ ticket.price }}</span>
            </div>
            <p class="ticket-desc">{{ ticket.description }}</p>
            <div class="ticket-action">
              <!-- 自定义数量选择器（需先选日期） -->
              <div v-if="!ticket.isGroup" class="qty-picker">
                <button class="qty-btn" :disabled="!visitDate || quantities[ticket.id] <= 0 || (ticket.dailyStock - ticket.soldCount) <= 0" @click="quantities[ticket.id] = Math.max(0, quantities[ticket.id] - 1)">−</button>
                <span class="qty-value">{{ quantities[ticket.id] || 0 }}</span>
                <button class="qty-btn" :disabled="!visitDate || quantities[ticket.id] >= 10 || (ticket.dailyStock - ticket.soldCount) <= 0" @click="quantities[ticket.id] = Math.min(10, (quantities[ticket.id] || 0) + 1)">+</button>
              </div>
              <button
                v-if="ticket.isGroup"
                class="ticket-btn"
                :disabled="scenicStore.scenicStatus === 0"
                @click="$router.push('/group-purchase')"
              >
                立即购买
              </button>
              <button
                v-else
                class="ticket-btn"
                :disabled="!ticket.status || (ticket.dailyStock - ticket.soldCount) <= 0 || !quantities[ticket.id] || scenicStore.scenicStatus === 0"
                @click="addToCart(ticket)"
              >
                加入购物车
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 购物车 -->
      <div class="section" v-if="cart.length" v-observe>
        <div class="cart-card">
          <div class="cart-header">
            <h3>购物车</h3>
            <span class="cart-count">{{ cart.length }} 种票型</span>
          </div>
          <div class="cart-table-wrap">
            <el-table :data="cart" style="width: 100%" class="cart-table">
              <el-table-column prop="name" label="票种" />
              <el-table-column prop="price" label="单价">
                <template #default="{ row }">¥{{ row.price }}</template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" />
              <el-table-column label="小计">
                <template #default="{ row }">¥{{ (row.price * row.quantity).toFixed(2) }}</template>
              </el-table-column>
              <el-table-column label="操作">
                <template #default="{ $index }">
                  <el-button type="danger" size="small" plain @click="removeFromCart($index)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div class="cart-footer">
            <div class="cart-footer-left">
              <div class="coupon-selector" v-if="availableCoupons.length > 0">
                <span class="coupon-label">优惠券：</span>
                <el-select v-model="selectedCouponId" placeholder="选择优惠券" size="small" clearable style="width: 220px;" @change="onCouponChange">
                  <el-option label="不使用优惠券" :value="null" />
                  <el-option
                    v-for="c in availableCoupons"
                    :key="c.userCouponId"
                    :label="c.desc + ' (-¥' + c.discountAmount + ')'"
                    :value="c.userCouponId"
                  />
                </el-select>
                <span v-if="discountAmount > 0" class="coupon-discount">-¥{{ discountAmount }}</span>
                <button class="coupon-cancel-btn" v-if="discountAmount > 0" @click="cancelCoupon">取消使用</button>
              </div>
              <button
                class="coupon-receive-btn"
                @click="couponDialogVisible = true"
              >
                领取优惠券
              </button>
            </div>
            <span class="cart-total">
              合计：<strong>¥{{ totalAmount.toFixed(2) }}</strong>
              <span v-if="discountAmount > 0" class="discount-badge">已省¥{{ discountAmount }}</span>
            </span>
            <button class="cart-submit-btn" :disabled="!visitDate || scenicStore.scenicStatus === 0" @click="handleCreateOrder" :class="{ loading: ordering }">
              <span v-if="ordering">提交中...</span>
              <span v-else>提交订单</span>
            </button>
          </div>
        </div>
      </div>
    </div>
    <!-- 领取优惠券弹窗（居中） -->
    <el-dialog v-model="couponDialogVisible" title="优惠券中心" width="520px" top="10vh" :close-on-click-modal="false">
      <div class="coupon-dialog-body">
        <!-- 可领取的优惠券 -->
        <div class="coupon-section-title">可领取</div>
        <div v-if="claimableTemplates.length === 0" class="empty-state">暂无可领取的优惠券</div>
        <div v-for="tpl in claimableTemplates" :key="tpl.id" class="coupon-tpl-card">
          <div class="coupon-tpl-left">
            <div class="coupon-tpl-name">{{ tpl.name }}</div>
            <div class="coupon-tpl-desc">
              {{ tpl.type === 1 ? '满' + tpl.threshold + '减' + tpl.discountValue : '满' + tpl.threshold + '打' + (tpl.discountValue * 100).toFixed(0) + '折' }}
              <span v-if="tpl.maxDiscount">，最高减{{ tpl.maxDiscount }}元</span>
            </div>
            <div class="coupon-tpl-valid">有效期{{ tpl.validDays }}天</div>
            <div v-if="tpl.maxOrderCount != null && tpl.maxOrderCount >= 0" class="coupon-tpl-new-user">
              🆕 仅限 ≤{{ tpl.maxOrderCount }}笔订单用户
            </div>
          </div>
          <button
            class="tpl-claim-btn"
            :disabled="claimingId === tpl.id || claimedIds.has(tpl.id)"
            @click="claimCoupon(tpl)"
          >
            <template v-if="claimedIds.has(tpl.id)">已领取</template>
            <template v-else-if="claimingId === tpl.id">领取中...</template>
            <template v-else>立即领取</template>
          </button>
        </div>

        <!-- 已领取的优惠券 -->
        <div class="coupon-section-title" style="margin-top: 24px;">我的优惠券</div>
        <div v-if="myCoupons.length === 0" class="empty-state">暂未领取优惠券</div>
        <div v-for="mc in myCoupons" :key="mc.id" class="coupon-tpl-card" :class="{ expired: mc.status === 2 }">
          <div class="coupon-tpl-left">
            <div class="coupon-tpl-name">{{ mc.couponName }}</div>
            <div class="coupon-tpl-desc">
              {{ mc.type === 1 ? '满' + mc.threshold + '减' + mc.discountValue : '满' + mc.threshold + '打' + (mc.discountValue * 100).toFixed(0) + '折' }}
              <span v-if="mc.maxDiscount">，最高减{{ mc.maxDiscount }}元</span>
            </div>
            <div class="coupon-tpl-valid">
              有效期至 {{ mc.validUntil }}
              <span class="coupon-status-tag" :class="mc.status === 0 ? 'unused' : mc.status === 1 ? 'used' : 'expired'">
                {{ mc.status === 0 ? '未使用' : mc.status === 1 ? '已使用' : '已过期' }}
              </span>
            </div>
          </div>
          <!-- 未使用：显示使用/已选中 + 删除 -->
          <div class="coupon-tpl-right-btns" v-if="mc.status === 0">
            <button
              v-if="selectedCouponId === mc.id"
              class="tpl-claim-btn my-coupon-selected-btn"
              disabled
            >
              已选中
            </button>
            <button
              v-else-if="!selectedCouponId"
              class="tpl-claim-btn my-coupon-use-btn"
              @click="handleUseMyCoupon(mc)"
            >
              使用
            </button>
            <button
              class="tpl-claim-btn my-coupon-del-btn"
              @click="handleDeleteMyCoupon(mc)"
            >
              删除
            </button>
          </div>
          <!-- 已使用：仅显示删除 -->
          <div class="coupon-tpl-right-btns" v-if="mc.status === 1">
            <button
              class="tpl-claim-btn my-coupon-del-btn"
              @click="handleDeleteMyCoupon(mc)"
            >
              删除
            </button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useScenicStore } from '../../stores/scenic'
import { getTicketTypes, createOrder, getAvailableCoupons, getAvailableTemplates, receiveCoupon, getMyCoupons, deleteMyCoupon, getConfig } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const scenicStore = useScenicStore()
const ticketTypes = ref([])
const visitDate = ref(null)
const quantities = ref({})
const cart = ref([])
const ordering = ref(false)
const availableCoupons = ref([])
const selectedCouponId = ref(null)
const discountAmount = ref(0)
const couponDialogVisible = ref(false)
const claimableTemplates = ref([])
const claimingId = ref(null)
const claimedIds = ref(new Set())
const myCoupons = ref([])

const totalAmount = computed(() => {
  return cart.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

// 当购物车金额变化时，重新加载可用优惠券
watch(totalAmount, async (newVal) => {
  if (newVal > 0) {
    await loadAvailableCoupons()
  } else {
    availableCoupons.value = []
    selectedCouponId.value = null
    discountAmount.value = 0
  }
})

// 领取弹窗打开时加载可领取模板和我的优惠券
watch(couponDialogVisible, async (open) => {
  if (open) {
    claimedIds.value = new Set()
    await Promise.all([loadClaimableTemplates(), loadMyCoupons()])
  }
})

async function loadClaimableTemplates() {
  try {
    claimableTemplates.value = await getAvailableTemplates() || []
  } catch (e) {
    console.error('加载可领取优惠券失败', e)
  }
}

async function claimCoupon(tpl) {
  claimingId.value = tpl.id
  try {
    await receiveCoupon(tpl.id)
    ElMessage.success('领取成功')
    claimedIds.value.add(tpl.id)
    await Promise.all([loadAvailableCoupons(), loadMyCoupons()])
    // 如果领取成功且有可用券，自动选中
    if (availableCoupons.value.length > 0 && !selectedCouponId.value) {
      selectedCouponId.value = availableCoupons.value[0].userCouponId
      discountAmount.value = availableCoupons.value[0].discountAmount
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '领取失败')
  } finally {
    claimingId.value = null
  }
}

async function loadMyCoupons() {
  try {
    const data = await getMyCoupons({ page: 1, size: 50 })
    myCoupons.value = data?.records || []
  } catch (e) {
    console.error('加载我的优惠券失败', e)
  }
}

async function handleDeleteMyCoupon(mc) {
  try {
    await ElMessageBox.confirm('确定要删除该优惠券吗？删除后不可恢复。', '删除确认', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
    })
    await deleteMyCoupon(mc.id)
    ElMessage.success('已删除')
    await Promise.all([loadMyCoupons(), loadAvailableCoupons()])
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '已删除')
    }
  }
}

function handleUseMyCoupon(mc) {
  if (totalAmount.value < mc.threshold) {
    ElMessage.warning(`满 ${mc.threshold} 元可用，当前订单金额 ¥${totalAmount.value.toFixed(2)}`)
    return
  }
  selectedCouponId.value = mc.id
  // 计算优惠金额
  if (mc.type === 1) {
    discountAmount.value = mc.discountValue
  } else {
    let d = totalAmount.value * mc.discountValue
    if (mc.maxDiscount && d > mc.maxDiscount) d = mc.maxDiscount
    discountAmount.value = d
  }
  couponDialogVisible.value = false
  ElMessage.success('已选择优惠券「' + mc.couponName + '」')
}

async function loadAvailableCoupons() {
  if (totalAmount.value <= 0) return
  try {
    const coupons = await getAvailableCoupons(totalAmount.value.toFixed(2))
    availableCoupons.value = coupons || []
    // 如果当前选中的券不在列表中，清除选择
    if (selectedCouponId.value && !coupons.find(c => c.userCouponId === selectedCouponId.value)) {
      selectedCouponId.value = null
      discountAmount.value = 0
    }
  } catch (e) {
    console.error('加载优惠券失败', e)
  }
}

function onCouponChange(val) {
  if (!val) {
    discountAmount.value = 0
    return
  }
  const coupon = availableCoupons.value.find(c => c.userCouponId === val)
  if (coupon) {
    discountAmount.value = coupon.discountAmount
  }
}

function cancelCoupon() {
  selectedCouponId.value = null
  discountAmount.value = 0
}

const bookingDaysNormal = ref(7)
function disabledDate(time) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const maxDate = new Date(today)
  maxDate.setDate(today.getDate() + bookingDaysNormal.value)
  return time.getTime() < today.getTime() || time.getTime() > maxDate.getTime()
}

function addToCart(ticket) {
  const qty = quantities.value[ticket.id]
  if (!qty || qty <= 0) return
  const existing = cart.value.find(item => item.id === ticket.id)
  if (existing) {
    existing.quantity = qty
  } else {
    cart.value.push({ id: ticket.id, name: ticket.name, price: ticket.price, quantity: qty })
  }
  quantities.value[ticket.id] = 0
  ElMessage.success('已加入购物车')
}

function removeFromCart(index) {
  cart.value.splice(index, 1)
}

const formatDate = (d) => {
  if (!d) return ''
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

async function handleCreateOrder() {
  if (!visitDate.value) {
    ElMessage.warning('请选择游览日期')
    return
  }
  ordering.value = true
  try {
    const orderData = {
      visitDate: formatDate(visitDate.value),
      items: cart.value.map(item => ({ ticketTypeId: item.id, quantity: item.quantity })),
    }
    if (selectedCouponId.value) {
      orderData.userCouponId = selectedCouponId.value
    }
    const order = await createOrder(orderData)
    ElMessage.success('订单已创建，状态：未支付，请前往支付')
    router.push({ path: '/pay', query: { orderNo: order.orderNo } })
  } catch (e) {
    console.error('下单失败', e)
  } finally {
    ordering.value = false
  }
}

onMounted(async () => {
  try {
    ticketTypes.value = await getTicketTypes()
    ticketTypes.value.forEach(t => { quantities.value[t.id] = 0 })
  } catch (e) {
    console.error('加载票种失败', e)
  }
  try {
    const cfg = await getConfig()
    if (cfg.bookingDaysNormal) bookingDaysNormal.value = cfg.bookingDaysNormal
  } catch (e) { /* use default 7 */ }
  setTimeout(setupObserver, 200)
})

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
.ticket-page { margin: -24px; }

/* ====== 页面头部 ====== */
.page-hero {
  padding: 56px 24px 46px; text-align: center;
}
.page-hero .section-tag { color: rgba(255,255,255,0.7); }
.page-hero .section-title { color: #fff; }
.page-hero .section-subtitle { color: rgba(255,255,255,0.8); }

/* ====== 内容区 ====== */
.content-wrapper { background: transparent; padding: 0 24px 80px; }
.section { max-width: 1200px; margin: 0 auto; padding-top: 60px; }

/* 入场动画 */
[v-observe] { opacity: 0; transform: translateY(30px); transition: opacity 0.7s ease, transform 0.7s cubic-bezier(0.22, 0.61, 0.36, 1); }
[v-observe].visible { opacity: 1; transform: translateY(0); }

/* 分区标题 */
.section-header { margin-bottom: 0; }
.section-tag {
  display: inline-block; font-size: 12px; font-weight: 700; letter-spacing: 3px;
  margin-bottom: 12px;
}
.section-title {
  font-size: clamp(28px, 4vw, 38px); font-weight: 800; color: #1a1a2e; margin: 0 0 12px;
  letter-spacing: -0.5px;
}
.section-subtitle { color: #94a3b8; font-size: 16px; margin: 0; }

/* ====== 日期筛选 ====== */
.section-filter { padding-top: 40px; }
.filter-bar {
  background: rgba(255,255,255,0.85); backdrop-filter: blur(10px);
  border-radius: 16px; padding: 20px 28px;
  display: flex; align-items: center; gap: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.filter-label { font-size: 15px; font-weight: 600; color: #1a1a2e; }
.filter-picker { width: 220px; }
.filter-hint { color: #94a3b8; font-size: 13px; }

/* ====== 票种卡片 ====== */
.tickets-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 24px; }
.ticket-card {
  background: rgba(255,255,255,0.85); backdrop-filter: blur(10px);
  border-radius: 20px; padding: 32px 24px; text-align: center; position: relative;
  box-shadow: var(--shadow-sm); transition: all var(--transition);
  opacity: 0; animation: fadeInUp 0.6s ease forwards;
}
.ticket-card:hover { transform: translateY(-6px); box-shadow: var(--shadow-lg); }
.ticket-card.disabled { opacity: 0.5; pointer-events: none; }
.ticket-badge {
  position: absolute; top: 14px; right: 14px;
  background: v-bind('scenicStore.primaryColor'); color: #fff;
  font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 10px;
}
.ticket-badge.sold-out {
  background: #e74c3c;
  top: 42px;
}
.ticket-name { font-size: 20px; font-weight: 700; color: #1a1a2e; margin: 0 0 16px; }
.ticket-price { margin-bottom: 16px; }
.price-symbol { font-size: 24px; color: v-bind('scenicStore.primaryColor'); font-weight: 600; vertical-align: top; }
.price-num { font-size: 44px; color: v-bind('scenicStore.primaryColor'); font-weight: 800; line-height: 1; }
.ticket-desc { color: #64748b; font-size: 14px; line-height: 1.6; margin: 0 0 16px; }
.ticket-meta { color: #94a3b8; font-size: 13px; margin-bottom: 20px; }
.ticket-action { display: flex; justify-content: center; align-items: center; gap: 12px; }

/* 自定义数量选择器 */
.qty-picker {
  display: flex; align-items: center; gap: 2px;
  background: #f1f5f9; border-radius: 12px; padding: 4px;
}
.qty-btn {
  width: 34px; height: 34px; border: none; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 600; cursor: pointer;
  background: #fff; color: #1a1a2e;
  transition: all 0.2s; line-height: 1;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.qty-btn:hover:not(:disabled) {
  background: v-bind('scenicStore.primaryColor'); color: #fff;
}
.qty-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.qty-value {
  min-width: 36px; text-align: center;
  font-size: 16px; font-weight: 700; color: #1a1a2e;
  user-select: none;
}

.ticket-btn {
  width: 100%; border: none; border-radius: 12px; padding: 12px 24px;
  background: v-bind('scenicStore.primaryColor'); color: #fff;
  font-size: 15px; font-weight: 600; cursor: pointer;
  transition: all var(--transition);
}
.ticket-btn:hover:not(:disabled) { filter: brightness(1.1); transform: scale(1.02); }
.ticket-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* ====== 购物车 ====== */
.cart-card {
  background: rgba(255,255,255,0.9); backdrop-filter: blur(10px);
  border-radius: 20px; overflow: hidden;
  box-shadow: var(--shadow-md);
}
.cart-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 28px; border-bottom: 1px solid #f1f5f9;
}
.cart-header h3 { font-size: 18px; font-weight: 700; color: #1a1a2e; margin: 0; }
.cart-count { font-size: 13px; color: #94a3b8; }
.cart-table-wrap { padding: 0; }
.cart-footer {
  display: flex; justify-content: flex-end; align-items: center;
  gap: 24px; padding: 20px 28px; border-top: 1px solid #f1f5f9;
}
.cart-footer-left { flex: 1; display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.coupon-selector { display: flex; align-items: center; gap: 8px; }
.coupon-label { font-size: 14px; color: #606266; font-weight: 500; }
.coupon-discount { font-size: 14px; color: #67C23A; font-weight: 700; }
.coupon-receive-btn {
  border: none; border-radius: 10px; padding: 8px 20px;
  background: v-bind('scenicStore.primaryColor'); color: #fff;
  font-size: 13px; font-weight: 600; cursor: pointer;
  transition: all 0.3s;
}
.coupon-receive-btn:hover { filter: brightness(1.15); transform: translateY(-1px); }
.coupon-cancel-btn {
  background: none; border: 1px solid #e2e8f0; border-radius: 6px;
  padding: 4px 10px; font-size: 12px; color: #f56c6c; cursor: pointer;
  transition: all 0.2s; margin-left: 8px;
}
.coupon-cancel-btn:hover { background: #fef0f0; border-color: #f56c6c; }
.discount-badge {
  font-size: 12px; color: #67C23A; background: #f0f9eb;
  padding: 2px 8px; border-radius: 10px; margin-left: 8px;
  font-weight: 600;
}
.cart-total { font-size: 18px; color: #1a1a2e; }
.cart-total strong { font-size: 22px; color: v-bind('scenicStore.primaryColor'); }
.cart-submit-btn {
  border: none; border-radius: 12px; padding: 14px 36px;
  background: v-bind('scenicStore.primaryColor'); color: #fff;
  font-size: 16px; font-weight: 600; cursor: pointer;
  transition: all var(--transition);
}
.cart-submit-btn:hover:not(:disabled) { filter: brightness(1.1); transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.15); }
.cart-submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.cart-submit-btn.loading { opacity: 0.7; }

/* ====== 动画 ====== */
@keyframes fadeInUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

/* ====== 响应式 ====== */
@media (max-width: 900px) { .tickets-grid { grid-template-columns: repeat(2, 1fr); } .filter-bar { flex-direction: column; align-items: flex-start; } }
@media (max-width: 560px) { .tickets-grid { grid-template-columns: 1fr; } }

/* ====== 领取优惠券弹窗 ====== */
.coupon-dialog-body { padding: 0 8px; max-height: 55vh; overflow-y: auto; }
.coupon-tpl-card {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px; margin-bottom: 12px;
  background: #f8fafc; border-radius: 12px; border: 1px solid #e2e8f0;
  transition: all 0.2s;
}
.coupon-tpl-card:hover { border-color: v-bind('scenicStore.primaryColor'); }
.coupon-tpl-left { flex: 1; min-width: 0; }
.coupon-tpl-name { font-size: 15px; font-weight: 600; color: #1a1a2e; margin-bottom: 4px; }
.coupon-tpl-desc { font-size: 13px; color: #606266; margin-bottom: 2px; }
.coupon-tpl-valid { font-size: 12px; color: #909399; }
.coupon-tpl-new-user { font-size: 12px; color: #E6A23C; margin-top: 2px; }
.tpl-claim-btn {
  flex-shrink: 0; margin-left: 16px;
  border: none; border-radius: 8px; padding: 8px 18px;
  background: v-bind('scenicStore.primaryColor'); color: #fff;
  font-size: 13px; font-weight: 600; cursor: pointer;
  transition: all 0.2s;
}
.tpl-claim-btn:hover:not(:disabled) { filter: brightness(1.1); }
.tpl-claim-btn:disabled { opacity: 0.5; cursor: not-allowed; background: #c0c4cc; }
.empty-state { text-align: center; padding: 40px 0; color: #909399; font-size: 14px; }
.coupon-section-title { font-size: 14px; font-weight: 700; color: #303133; margin-bottom: 10px; padding-left: 4px; }
.coupon-status-tag { font-size: 11px; padding: 1px 8px; border-radius: 8px; margin-left: 8px; font-weight: 600; }
.coupon-status-tag.unused { background: #e1f3d8; color: #67C23A; }
.coupon-status-tag.used { background: #f0f0f0; color: #909399; }
.coupon-status-tag.expired { background: #fef0f0; color: #f56c6c; }
.coupon-tpl-card.expired { opacity: 0.55; background: #f5f5f5; }
.coupon-tpl-right-btns { display: flex; gap: 8px; flex-shrink: 0; margin-left: 16px; }
.my-coupon-use-btn {
  background: v-bind('scenicStore.primaryColor') !important; color: #fff !important;
}
.my-coupon-use-btn:hover:not(:disabled) { filter: brightness(1.1); }
.my-coupon-selected-btn {
  background: #e8f5e9 !important; color: #67C23A !important; cursor: default !important;
}
.my-coupon-del-btn {
  background: #fff !important; color: #f56c6c !important;
  border: 1px solid #f56c6c !important;
}
/* 暂停运营横幅 */
.suspend-banner {
  display: flex; align-items: center; gap: 16px;
  background: linear-gradient(135deg, #fff5f5, #fef0f0);
  border: 1px solid #f56c6c; border-radius: 12px;
  padding: 20px 28px; margin-bottom: 24px;
}
.suspend-icon { font-size: 32px; }
.suspend-title { font-size: 16px; font-weight: 700; color: #f56c6c; margin-bottom: 4px; }
.suspend-desc { font-size: 13px; color: #909399; }
.my-coupon-del-btn:hover:not(:disabled) { background: #fef0f0 !important; filter: none !important; }
</style>
