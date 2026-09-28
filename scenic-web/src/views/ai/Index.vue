<template>
  <div class="ai-page">
    <!-- ====== 页面头部 ====== -->
    <div class="page-hero">
      <div class="section-header">
        <span class="section-tag">AI ASSISTANT</span>
        <h2 class="section-title">AI智能助手 · 小景</h2>
        <p class="section-subtitle">随时随地，为您解答景区相关问题</p>
      </div>
    </div>

    <div class="content-wrapper">
      <!-- 暂停运营提示 -->
      <div class="suspend-banner" v-if="scenicStore.scenicStatus === 0">
        <div class="suspend-icon">⚠️</div>
        <div>
          <div class="suspend-title">景区暂停运营</div>
          <div class="suspend-desc">当前景区处于暂停运营状态，AI智能助手暂时无法提供服务</div>
        </div>
      </div>

      <div class="chat-layout">
        <!-- ====== 左侧：聊天区域 ====== -->
        <div class="chat-container" :class="{ 'has-panel': sidePanelMode }">
          <div class="chat-messages" ref="chatBox">
            <!-- 欢迎界面 -->
            <div v-if="messages.length === 0" class="welcome">
              <div class="welcome-icon">👋</div>
              <h3>您好！我是{{ scenicStore.scenicName }}的智能助手小景</h3>
              <div class="welcome-cards">
                <div class="welcome-card" :class="{ disabled: scenicStore.scenicStatus === 0 }" :style="{ borderTopColor: scenicStore.primaryColor }" @click="scenicStore.scenicStatus !== 0 && sendQuickMsg('解答景区相关问题')">
                  <span class="welcome-card-icon">💬</span>
                  <span>解答景区相关问题</span>
                  <small>开放时间、票价、交通等</small>
                </div>
                <div class="welcome-card" :class="{ disabled: scenicStore.scenicStatus === 0 }" :style="{ borderTopColor: scenicStore.primaryColor }" @click="scenicStore.scenicStatus !== 0 && sendQuickMsg('我想购买门票')">
                  <span class="welcome-card-icon">🛒</span>
                  <span>辅助购票</span>
                  <small>推荐票种、引导下单</small>
                </div>
                <div class="welcome-card" :class="{ disabled: scenicStore.scenicStatus === 0 }" :style="{ borderTopColor: scenicStore.primaryColor }" @click="scenicStore.scenicStatus !== 0 && sendQuickMsg('景区有什么好玩的路线')">
                  <span class="welcome-card-icon">🗺️</span>
                  <span>景区导览</span>
                  <small>路线推荐、景点设施查询</small>
                </div>
              </div>
            </div>

            <!-- 消息列表 -->
            <div v-for="(msg, idx) in messages" :key="idx" :class="['message', msg.role]">
              <div class="msg-content" v-html="formatMessage(msg.content)"></div>
              <div class="msg-time">{{ msg.time }}</div>

              <!-- AI回复的快捷操作按钮（仅在没有侧面板时显示 "前往购票"） -->
              <div v-if="msg.role === 'assistant' && (msg.intent || purchaseState !== 'idle')" class="msg-actions">
                <!-- 只有侧面板没打开时，才显示"前往购票"兜底按钮 -->
                <button
                  v-if="(msg.intent === 'buy_ticket' || purchaseState !== 'idle') && !sidePanelMode"
                  class="msg-action-btn"
                  :style="{ background: scenicStore.primaryColor }"
                  @click="goToBuyTicket"
                >前往购票</button>
                <button
                  v-if="msg.intent === 'guide'"
                  class="msg-action-btn"
                  :style="{ background: '#67C23A' }"
                  @click="openMap"
                >查看景区地图</button>
              </div>

              <!-- 快捷回复按钮组（购票流程） -->
              <div v-if="msg.role === 'assistant' && quickReplies.length > 0 && idx === messages.length - 1" class="quick-replies">
                <button
                  v-for="(qr, qi) in quickReplies"
                  :key="qi"
                  class="quick-reply-btn"
                  :style="qr.style || {}"
                  @click="handleQuickReply(qr)"
                >{{ qr.label }}</button>
              </div>

              <!-- 反馈 -->
              <div v-if="msg.role === 'assistant' && msg.conversationId" class="msg-feedback">
                <span :class="['fb-btn', { active: msg.feedback === 1 }]" @click="submitFeedback(msg, 1)" title="有用">👍 有用</span>
                <span :class="['fb-btn', { active: msg.feedback === 2 }]" @click="submitFeedback(msg, 2)" title="无用">👎 无用</span>
              </div>
            </div>

            <!-- 流式输出 -->
            <div v-if="isStreaming" class="message assistant">
              <div class="msg-content streaming">{{ streamingText }}<span class="cursor">|</span></div>
            </div>
          </div>

          <!-- 输入区 -->
          <div class="chat-input">
            <div class="chat-input-inner">
              <input
                v-model="input"
                class="chat-input-field"
                :placeholder="scenicStore.scenicStatus === 0 ? '景区暂停运营，AI助手暂不可用' : '请输入您的问题...'"
                @keyup.enter="sendMessage"
                :disabled="isStreaming || scenicStore.scenicStatus === 0"
              />
              <button
                v-if="messages.length > 0"
                class="chat-clear-btn"
                @click="clearChat"
                title="清空对话"
              >清空</button>
              <button
                class="chat-send-btn"
                :disabled="!input.trim() || isStreaming || scenicStore.scenicStatus === 0"
                :class="{ loading: isStreaming }"
                @click="sendMessage"
              >
                <span v-if="isStreaming">···</span>
                <span v-else>发送</span>
              </button>
            </div>
          </div>
        </div>

        <!-- ====== 右侧：操作面板 ====== -->
        <transition name="panel-slide">
          <div v-if="sidePanelMode" class="side-panel" ref="sidePanel">
            <!-- 面板头部 -->
            <div class="panel-header">
              <h3 class="panel-title">{{ panelTitle }}</h3>
              <button class="panel-close" @click="closeSidePanel">✕</button>
            </div>

            <!-- 购物车模式 -->
            <div v-if="sidePanelMode === 'cart'" class="panel-body">
              <!-- 日期选择 -->
              <div class="panel-section">
                <div class="panel-label">游览日期</div>
                <el-date-picker
                  v-model="cart.visitDate"
                  type="date"
                  placeholder="请选择日期"
                  :disabled-date="disabledDate"
                  class="panel-date-picker"
                  @change="onCartDateChange"
                />
              </div>

              <!-- 票种选择（需先选日期） -->
              <div class="panel-section">
                <div class="panel-label">选择票种</div>
                <div v-if="!cart.visitDate" class="panel-date-hint">请先选择游览日期</div>
                <div class="panel-ticket-list" v-else>
                  <div
                    v-for="ticket in ticketTypes"
                    :key="ticket.id"
                    class="panel-ticket-item"
                    :class="{ selected: getCartItem(ticket.id), 'is-group': ticket.isGroup || ticket.name?.includes('团体'), disabled: !ticket.status || (ticket.dailyStock - ticket.soldCount) <= 0 }"
                    @click="toggleTicketInCart(ticket)"
                  >
                    <div class="panel-ticket-info">
                      <span class="panel-ticket-name">
                        {{ ticket.name }}
                        <span v-if="ticket.isGroup || ticket.name?.includes('团体')" class="panel-ticket-group-tag">团体</span>
                      </span>
                      <span class="panel-ticket-price">¥{{ ticket.price }}</span>
                    </div>
                    <div class="panel-ticket-qty" v-if="getCartItem(ticket.id)">
                      <button class="pqty-btn" @click.stop="adjustCartQty(ticket.id, -1)">−</button>
                      <span class="pqty-val">{{ getCartItem(ticket.id).quantity }}</span>
                      <button class="pqty-btn" @click.stop="adjustCartQty(ticket.id, 1)">+</button>
                    </div>
                    <span v-else class="panel-ticket-action">
                      <template v-if="ticket.isGroup || ticket.name?.includes('团体')">
                         选购
                      </template>
                      <template v-else>
                        选择
                      </template>
                    </span>
                    <span v-if="(ticket.dailyStock - ticket.soldCount) <= 0" class="panel-ticket-soldout">售罄</span>
                  </div>
                </div>
              </div>

              <!-- 购物车汇总 -->
              <div class="panel-section" v-if="cart.items.length > 0">
                <div class="panel-label">购物车汇总</div>
                <div class="panel-cart-summary">
                  <div class="panel-cart-row" v-if="cart.visitDate">
                    <span>游览日期</span>
                    <span>{{ formatCartDate(cart.visitDate) }}</span>
                  </div>
                  <div class="panel-cart-row" v-for="item in cart.items" :key="item.ticketTypeId">
                    <span>{{ item.name }} × {{ item.quantity }}</span>
                    <span>¥{{ (item.price * item.quantity).toFixed(2) }}</span>
                  </div>
                  <div class="panel-cart-divider"></div>
                  <div class="panel-cart-row total">
                    <span>合计</span>
                    <span>¥{{ cartTotal.toFixed(2) }}</span>
                  </div>
                </div>
                <button
                  class="panel-confirm-btn"
                  :disabled="!cart.visitDate || cart.items.length === 0"
                  @click="confirmCart"
                >
                  确认下单
                </button>
              </div>

              <!-- 空购物车提示 -->
              <div class="panel-empty" v-if="cart.items.length === 0">
                <span class="panel-empty-icon">🛒</span>
                <p>请选择游览日期和票种</p>
                <p class="panel-empty-hint">在对话框中告诉我您的需求，我会帮您一起选购</p>
              </div>
            </div>

            <!-- 支付模式 -->
            <div v-if="sidePanelMode === 'payment'" class="panel-body">
              <div class="panel-section">
                <div class="panel-label">订单详情</div>
                <div class="panel-cart-summary">
                  <div class="panel-cart-row">
                    <span>订单编号</span>
                    <span class="order-no-text">{{ cart.orderNo }}</span>
                  </div>
                  <div class="panel-cart-row">
                    <span>游览日期</span>
                    <span>{{ formatCartDate(cart.visitDate) }}</span>
                  </div>
                  <div class="panel-cart-row" v-for="item in cart.items" :key="item.ticketTypeId">
                    <span>{{ item.name }} × {{ item.quantity }}</span>
                    <span>¥{{ (item.price * item.quantity).toFixed(2) }}</span>
                  </div>
                  <div class="panel-cart-divider"></div>
                  <div class="panel-cart-row" v-if="cart.discountAmount > 0">
                    <span>订单金额</span>
                    <span>¥{{ cart.totalAmount.toFixed(2) }}</span>
                  </div>
                  <div class="panel-cart-row discount" v-if="cart.discountAmount > 0">
                    <span>优惠券「{{ cart.couponName }}」</span>
                    <span style="color: #67C23A;">-¥{{ cart.discountAmount.toFixed(2) }}</span>
                  </div>
                  <div class="panel-cart-row total">
                    <span>应付金额</span>
                    <span>¥{{ cartPayAmount.toFixed(2) }}</span>
                  </div>
                </div>

                <div class="panel-section-title">选择支付方式</div>
                <div class="pay-methods-compact">
                  <div class="pay-method-compact" :class="{ active: payMethod === 'mock' }" @click="payMethod = 'mock'">
                    <span class="pm-icon pm-icon--mock">
                      <svg viewBox="0 0 48 48" width="36" height="36" fill="none"><rect x="4" y="8" width="40" height="32" rx="4" stroke="currentColor" stroke-width="3" fill="none"/><line x1="4" y1="18" x2="44" y2="18" stroke="currentColor" stroke-width="3"/><circle cx="12" cy="28" r="3" fill="currentColor"/><circle cx="22" cy="28" r="3" fill="currentColor"/><rect x="28" y="24" width="12" height="8" rx="2" fill="currentColor"/><line x1="34" y1="24" x2="34" y2="32" stroke="#fff" stroke-width="1.5"/></svg>
                    </span>
                    <span>模拟支付</span>
                  </div>
                  <div class="pay-method-compact disabled" title="暂未开放" @click="ElMessage.info('微信支付暂未开放')">
                    <span class="pm-icon pm-icon--wechat">
                      <svg viewBox="0 0 48 48" width="36" height="36" fill="#07C160"><path d="M17.382 4.376C7.782 4.376 0 10.952 0 19.06c0 4.424 2.34 8.406 6.004 11.1.118.086.198.227.194.378-.003.084-.024.167-.06.243l-.78 2.96c-.038.14-.096.282-.096.426 0 .326.26.59.58.59.11 0 .22-.037.334-.108l3.806-2.228c.204-.12.45-.14.674-.056.333.126.675.225 1.025.296.453.208.875.4 1.526.51-.28-.6-.478-1.246-.58-1.92-1.714-5.156.314-9.944 3.864-12.892 3.406-2.83 7.764-3.96 11.706-3.676C27.512 7.874 20.258 4.376 17.382 4.376zM11.57 11.982c1.284 0 2.324 1.058 2.324 2.36 0 1.302-1.04 2.356-2.324 2.356-1.283 0-2.324-1.054-2.324-2.356 0-1.302 1.04-2.36 2.324-2.36zm11.626 0c1.284 0 2.324 1.058 2.324 2.36 0 1.302-1.04 2.356-2.324 2.356-1.283 0-2.324-1.054-2.324-2.356 0-1.302 1.04-2.36 2.324-2.36zm10.68 5.734c-3.594-.104-7.492 1.024-10.56 3.572-3.44 2.856-5.374 7.44-3.56 12.44 1.484 4.086 5.56 6.896 10.048 6.896.71 0 1.45-.074 2.194-.196l.116-.022c.37-.07.76.002 1.124.184l3.07 1.796c.07.04.136.07.206.07.22 0 .38-.18.38-.396 0-.08-.02-.16-.056-.236l-.01-.022-.628-2.386a.944.944 0 0 1 .34-1.048c2.688-2.032 4.252-4.86 4.252-7.924 0-5.746-4.934-10.478-10.916-10.728zM32.532 20.4c1 0 1.812.83 1.812 1.854 0 1.024-.812 1.854-1.812 1.854s-1.814-.83-1.814-1.854c0-1.024.814-1.854 1.814-1.854zm8.696 0c1 0 1.812.83 1.812 1.854 0 1.024-.812 1.854-1.812 1.854S39.414 23.278 39.414 22.254c0-1.024.814-1.854 1.814-1.854z"/></svg>
                    </span>
                    <span>微信支付</span>
                    <span class="pm-tag">暂未开放</span>
                  </div>
                  <div class="pay-method-compact disabled" title="暂未开放" @click="ElMessage.info('支付宝暂未开放')">
                    <span class="pm-icon pm-icon--alipay">
                      <svg viewBox="0 0 48 48" width="36" height="36" fill="#1677FF"><path d="M24 6C14.059 6 6 14.059 6 24s8.059 18 18 18 18-8.059 18-18S33.941 6 24 6zm8.4 22.2c-1.2 2-3.2 3.28-6.44 3.28h-1.04c-.82 0-1.48.6-1.58 1.36l-.24 1.94c-.1.76-.5 1.2-1.26 1.2h-2.2c-.72 0-1-.44-.84-1.22l1.22-7.72c.16-.92.76-1.22 1.5-1.22h4.8c4.06 0 6.02 2 6.02 2.38zm-8.44-9.1c-.84.26-1.36.84-1.58 1.58l-.3 1.8h3.94c2.04 0 3.02-1 3.02-2.04 0-.92-.78-1.34-2.8-1.34h-2.28z"/></svg>
                    </span>
                    <span>支付宝</span>
                    <span class="pm-tag">暂未开放</span>
                  </div>
                </div>

                <button class="panel-pay-btn" :disabled="paying" @click="handlePayInPanel">
                  <span v-if="paying">处理中...</span>
                  <span v-else>确认支付 ¥{{ cartPayAmount.toFixed(2) }}</span>
                </button>
              </div>
            </div>

            <!-- 支付完成/人脸录入提醒 -->
            <div v-if="sidePanelMode === 'paid'" class="panel-body">
              <h3 class="panel-success-title">支付成功！</h3>
              <p class="panel-success-text">订单编号：<code>{{ cart.orderNo }}</code></p>
              <div class="panel-alert">
                <span class="panel-alert-icon">⚡</span>
                <span>请记得为每张门票录入人脸信息，游览当天可刷脸入园！</span>
              </div>
              <button class="panel-face-btn" @click="goToPayPage">
                前往录入人脸
              </button>
              <button class="panel-face-btn secondary" @click="markFaceDone">
                稍后录入
              </button>
            </div>

            <!-- 全部完成 -->
            <div v-if="sidePanelMode === 'complete'" class="panel-body">
              <h3 class="panel-success-title">恭喜您完成所有步骤！</h3>
              <p class="panel-success-text">祝您在{{ scenicStore.scenicName }}游玩愉快！</p>
              <div class="panel-complete-summary">
                <div class="panel-cart-row">
                  <span>订单编号</span>
                  <span class="order-no-text">{{ cart.orderNo }}</span>
                </div>
                <div class="panel-cart-row">
                  <span>游览日期</span>
                  <span>{{ formatCartDate(cart.visitDate) }}</span>
                </div>
              </div>
              <div class="panel-complete-btns">
                <button class="panel-face-btn" @click="goToPayPage">查看订单详情</button>
                <button class="panel-face-btn secondary" @click="resetPurchase">开始新对话</button>
              </div>
            </div>

            <!-- 地图模式 -->
            <div v-if="sidePanelMode === 'map'" class="panel-body panel-map-body">
              <div v-if="!hasCoordinates" class="map-empty">
                <p>暂无景点坐标数据</p>
              </div>
              <div ref="mapPanelContainer" class="map-panel-container" v-show="hasCoordinates"></div>
            </div>
          </div>
        </transition>
      </div>
    </div>

    <!-- 景区地图弹窗（保留原有弹窗方式） -->
    <el-dialog v-model="mapDialogVisible" title="景区景点分布" width="800px" destroy-on-close>
      <div v-if="!hasCoordinates" class="map-empty">
        <p>暂无景点坐标数据</p>
      </div>
      <div ref="mapContainer" class="map-container" v-show="hasCoordinates"></div>
      <template #footer>
        <el-button @click="mapDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onActivated, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useScenicStore } from '../../stores/scenic'
import { useUserStore } from '../../stores/user'
import { aiChat, submitAiFeedback, getSpots, getTicketTypes, createOrder, mockPay, getOrderDetail } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

defineOptions({ name: 'AiChat' })

const router = useRouter()
const scenicStore = useScenicStore()
const userStore = useUserStore()
const chatBox = ref(null)
const sidePanel = ref(null)
const input = ref('')
const messages = ref([])
const isStreaming = ref(false)
const streamingText = ref('')
const sessionId = ref('')

// ==================== 侧面板状态 ====================
// null | 'cart' | 'payment' | 'paid' | 'complete' | 'map'
const sidePanelMode = ref(null)
const purchaseState = ref('idle') // idle | buying | paying | paid | complete
const payMethod = ref('mock')
const paying = ref(false)

// ==================== 购物车数据 ====================
const cart = ref({
  visitDate: null,
  items: [], // [{ ticketTypeId, name, price, quantity }]
  orderNo: null,
  totalAmount: 0,
  discountAmount: 0,
  couponName: '',
})

const ticketTypes = ref([])
const multiSelectTickets = ref(new Map()) // 多选暂存 票种ID → 数量 (1~5)

const cartTotal = computed(() => {
  return cart.value.items.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

/** 最终应付金额（扣除优惠券后） */
const cartPayAmount = computed(() => {
  return Math.max(0, cart.value.totalAmount - cart.value.discountAmount)
})

const panelTitle = computed(() => {
  const titles = {
    cart: '购票助手',
    payment: '确认支付',
    paid: '支付成功',
    complete: '购票完成',
    map: '景区地图',
  }
  return titles[sidePanelMode.value] || '操作面板'
})

// ==================== 快捷回复 ====================
const quickReplies = computed(() => {
  if (!sidePanelMode.value) return []
  if (sidePanelMode.value === 'cart') {
    const replies = []
    if (!cart.value.visitDate) {
      replies.push({ label: '今天', action: 'setDateToday', style: { background: scenicStore.primaryColor, color: '#fff' } })
      replies.push({ label: '明天', action: 'setDateTomorrow', style: { background: scenicStore.primaryColor, color: '#fff' } })
    }
    // 日期已选但还没选票种时，显示可多选的票种快捷按钮（非团体票，点击切换数量 1→2→3→4→5→取消）
    if (cart.value.visitDate && cart.value.items.length === 0) {
      const nonGroupTickets = ticketTypes.value.filter(t => !t.isGroup && !t.name?.includes('团体') && t.status && (t.dailyStock - t.soldCount) > 0)
      nonGroupTickets.slice(0, 4).forEach(t => {
        const qty = multiSelectTickets.value.get(t.id) || 0
        const selected = qty > 0
        replies.push({
          label: selected ? `✓ ${t.name} ×${qty}` : t.name,
          action: 'toggleMultiTicket',
          ticketId: t.id,
          style: selected ? { background: scenicStore.primaryColor, color: '#fff' } : { background: '#f1f5f9', color: '#1a1a2e' }
        })
      })
      // 选好票种后显示确认按钮
      if (multiSelectTickets.value.size > 0) {
        replies.push({ label: '确认选票', action: 'confirmMultiSelection', style: { background: '#67C23A', color: '#fff' } })
      }
    }
    if (cart.value.visitDate && cart.value.items.length > 0) {
      // 仅单票种时显示数量快捷按钮
      if (cart.value.items.length === 1) {
        const qtyOptions = [1, 2, 3, 4, 5]
        qtyOptions.forEach(n => {
          replies.push({
            label: `${n} 张`,
            action: 'setQty',
            qty: n,
            style: cart.value.items[0].quantity === n
              ? { background: scenicStore.primaryColor, color: '#fff' }
              : { background: '#f1f5f9', color: '#1a1a2e' }
          })
        })
      }
      replies.push({ label: '确认下单', action: 'confirmCart', style: { background: '#67C23A', color: '#fff' } })
      replies.push({ label: '重新选择', action: 'resetCart', style: { background: '#909399', color: '#fff' } })
    }
    return replies
  }
  if (sidePanelMode.value === 'payment') {
    return [
      { label: '去支付', action: 'pay', style: { background: '#E6A23C', color: '#fff' } },
      { label: '修改订单', action: 'backToCart', style: { background: '#909399', color: '#fff' } },
    ]
  }
  if (sidePanelMode.value === 'paid') {
    return [
      { label: '录入人脸', action: 'face', style: { background: scenicStore.primaryColor, color: '#fff' } },
      { label: '稍后录入', action: 'skipFace', style: { background: '#909399', color: '#fff' } },
    ]
  }
  return []
})

// ==================== 地图相关 ====================
const mapDialogVisible = ref(false)
const mapContainer = ref(null)
const mapPanelContainer = ref(null)
const spots = ref([])
const hasCoordinates = ref(true)
let mapInstance = null
let mapPanelInstance = null

// ==================== 持久化 ====================
const storageKey = 'ai_messages_' + (userStore.userInfo?.userId || 'guest')

function saveMessages() {
  sessionStorage.setItem(storageKey, JSON.stringify(messages.value))
}
watch(messages, saveMessages, { deep: true })
watch(messages, () => { nextTick(() => scrollToBottom()) }, { deep: true })

onMounted(async () => {
  // 恢复消息
  const saved = sessionStorage.getItem(storageKey)
  if (saved) { try { messages.value = JSON.parse(saved) } catch (e) { /* ignore */ } }
  const savedSid = sessionStorage.getItem('ai_session_id')
  if (savedSid) sessionId.value = savedSid
  if (messages.value.length > 0) {
    nextTick(() => {
      requestAnimationFrame(() => {
        requestAnimationFrame(() => { scrollToBottom() })
      })
    })
  }
  // 加载票种
  try { ticketTypes.value = await getTicketTypes() } catch (e) { console.error('加载票种失败', e) }
  setTimeout(setupObserver, 200)
})

onActivated(() => { nextTick(() => scrollToBottom()) })

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

function stripGuidanceHints(content) {
  // 移除引导点击UI按钮的提示语（右侧面板已展示操作入口）
  let text = content
  // 1. 带💡的引导句：💡 后包含按钮/点击/前往/下方等UI引导关键词的整句
  text = text.replace(/💡[^。\n]*?(?:点击下方|点击右侧|前往购票|下方按钮|右侧按钮|查看地图|操作按钮)[^。\n]*[。]?/g, '')
  // 2. 不带💡的"请点击下方/右侧..."类引导
  text = text.replace(/请(?:您)?点击(?:下方|右侧|下方按钮|右侧按钮|下面)[^。\n]*[。]?/g, '')
  // 3. 移除尾部残留的💡emoji（如果引导句内容较特殊没被上面匹配到）
  text = text.replace(/💡[^。\n]*?点击[^。\n]*[。]?/g, '')
  text = text.replace(/💡[^。\n]*?按钮[^。\n]*[。]?/g, '')
  // 4. 清理多余空白行
  text = text.replace(/\n{2,}/g, '\n').trim()
  return text
}

function formatMessage(content) {
  // 右侧面板已打开时，过滤掉UI引导提示语
  let text = sidePanelMode.value ? stripGuidanceHints(content) : content
  return text
    .replace(/\n/g, '<br/>')
    .replace(/💡(.+?)((<br\/>)+|$)/g, '<span class="ai-hint">💡$1</span>$2')
}

function scrollToBottom() {
  if (chatBox.value) {
    chatBox.value.scrollTop = chatBox.value.scrollHeight
  }
}

// ==================== 发送消息 ====================
async function sendMessage() {
  const msg = input.value.trim()
  if (!msg || isStreaming.value) return

  // 检测用户在购票流程中的输入
  detectUserInput(msg)

  messages.value.push({ role: 'user', content: msg, time: new Date().toLocaleTimeString() })
  input.value = ''
  scrollToBottom()

  isStreaming.value = true
  streamingText.value = ''

  let fullText = ''
  let currentConversationId = null
  let currentIntent = null
  try {
    const response = await aiChat({ sessionId: sessionId.value, message: msg })
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const events = buffer.split('\n\n')
      buffer = events.pop() || ''
      for (const event of events) {
        const dataLine = event.split('\n').find(l => l.startsWith('data:'))
        if (!dataLine) continue
        try {
          const data = JSON.parse(dataLine.slice(5).trim())
          if (data.type === 'text' && data.delta) {
            streamingText.value += data.delta
            fullText += data.delta
            scrollToBottom()
          } else if (data.type === 'done') {
            if (data.sessionId) {
              sessionId.value = data.sessionId
              sessionStorage.setItem('ai_session_id', data.sessionId)
            }
            if (data.conversationId) currentConversationId = data.conversationId
            if (data.intent) currentIntent = data.intent
          } else if (data.type === 'error') {
            throw new Error(data.delta)
          }
        } catch (e) {
          if (!(e instanceof SyntaxError)) throw e
        }
      }
    }

    if (buffer) {
      const dataLine = buffer.split('\n').find(l => l.startsWith('data:'))
      if (dataLine) {
        try {
          const data = JSON.parse(dataLine.slice(5).trim())
          if (data.type === 'text' && data.delta) fullText += data.delta
        } catch (e) { /* ignore */ }
      }
    }

    messages.value.push({
      role: 'assistant', content: fullText, time: new Date().toLocaleTimeString(),
      conversationId: currentConversationId, intent: currentIntent, feedback: 0,
    })

    // 根据AI回复自动推进购票流程
    processAiResponse(fullText, currentIntent)
  } catch (e) {
    console.warn('AI对话流中断:', e.message || e)
    if (!fullText) {
      ElMessage.error('AI服务暂时不可用，请稍后重试')
      fullText = '抱歉，AI服务暂时不可用，请稍后重试。'
    }
    messages.value.push({
      role: 'assistant', content: fullText, time: new Date().toLocaleTimeString(),
      conversationId: null, intent: null, feedback: 0,
    })
  } finally {
    isStreaming.value = false
    streamingText.value = ''
    scrollToBottom()
  }

  // 滚动侧面板
  nextTick(() => {
    if (sidePanel.value) sidePanel.value.scrollTop = 0
  })
}

// ==================== 快捷消息 ====================
function sendQuickMsg(text) {
  input.value = text
  sendMessage()
}

// ==================== 快捷回复处理 ====================
function handleQuickReply(qr) {
  switch (qr.action) {
    case 'setDateToday':
      cart.value.visitDate = new Date()
      onCartDateChange()
      input.value = '今天'
      sendMessage()
      break
    case 'setDateTomorrow':
      const tomorrow = new Date()
      tomorrow.setDate(tomorrow.getDate() + 1)
      cart.value.visitDate = tomorrow
      onCartDateChange()
      input.value = '明天'
      sendMessage()
      break
    case 'selectTicket':  // 保留老版单选兼容
      if (qr.ticketId) {
        const ticket = ticketTypes.value.find(t => t.id === qr.ticketId)
        if (ticket) toggleTicketInCart(ticket)
        input.value = `我要买${ticket?.name || '门票'}`
        sendMessage()
      }
      break
    case 'toggleMultiTicket': {
      // 多选模式下循环切换数量：1→2→3→4→5→取消（删除）
      if (qr.ticketId) {
        const cur = multiSelectTickets.value.get(qr.ticketId) || 0
        const next = cur >= 5 ? 0 : cur + 1
        if (next === 0) {
          multiSelectTickets.value.delete(qr.ticketId)
        } else {
          multiSelectTickets.value.set(qr.ticketId, next)
        }
        // 触发响应式更新（Map 需要重新赋值）
        multiSelectTickets.value = new Map(multiSelectTickets.value)
      }
      break
    }
    case 'confirmMultiSelection': {
      // 多选确认：将暂存票种及其数量批量加入购物车
      if (multiSelectTickets.value.size === 0) break
      const parts = []
      multiSelectTickets.value.forEach((qty, tid) => {
        const ticket = ticketTypes.value.find(t => t.id === tid)
        if (ticket && ticket.status && (ticket.dailyStock - ticket.soldCount) > 0) {
          if (!getCartItem(tid)) {
            cart.value.items.push({ ticketTypeId: tid, name: ticket.name, price: ticket.price, quantity: qty })
          }
          parts.push(`${ticket.name}${qty}张`)
        }
      })
      multiSelectTickets.value = new Map()
      if (cart.value.items.length > 0) {
        input.value = `我要买${parts.join('、')}`
        sendMessage()
      }
      break
    }
    case 'setQty':
      if (qr.qty && cart.value.items.length > 0) {
        const lastItem = cart.value.items[cart.value.items.length - 1]
        lastItem.quantity = qr.qty
        input.value = `${qr.qty}张`
        sendMessage()
      }
      break
    case 'resetCart':
      cart.value = { visitDate: cart.value.visitDate, items: [], orderNo: null }
      multiSelectTickets.value = new Map()
      input.value = '我想重新选择票种'
      sendMessage()
      break
    case 'backToCart':
      purchaseState.value = 'buying'
      openSidePanel('cart')
      input.value = '我想修改订单'
      sendMessage()
      break
    case 'confirmCart':
      confirmCart()
      break
    case 'pay':
      handlePayInPanel()
      break
    case 'face':
      goToPayPage()
      break
    case 'skipFace':
      markFaceDone()
      break
    default:
      if (qr.label) {
        input.value = qr.label
        sendMessage()
      }
  }
}

// ==================== 用户输入检测 ====================
function detectUserInput(msg) {
  const lower = msg.toLowerCase()

  // 检测购票意图
  if (containsAny(msg, '买票', '购票', '买', '门票', '购买', '下单', '订票')) {
    if (purchaseState.value === 'idle') {
      startPurchase()
    }
  }

  // 在购票流程中检测日期输入
  if (purchaseState.value === 'buying' || purchaseState.value === 'idle') {
    const dateMatch = extractDate(msg)
    if (dateMatch && !cart.value.visitDate) {
      cart.value.visitDate = dateMatch
      if (sidePanelMode.value !== 'cart') {
        openSidePanel('cart')
      }
    }

    // 检测多票种输入（如 "成人票一张 儿童票2张"）
    let multiTicketMatched = false
    if (ticketTypes.value.length > 0) {
      for (const ticket of ticketTypes.value) {
        if (ticket.isGroup || ticket.name?.includes('团体')) continue
        if (!ticket.status || (ticket.dailyStock - ticket.soldCount) <= 0) continue
        const qty = extractTicketQty(msg, ticket.name)
        if (qty && qty > 0 && qty <= 10) {
          const existing = getCartItem(ticket.id)
          if (existing) {
            existing.quantity = qty
          } else {
            cart.value.items.push({
              ticketTypeId: ticket.id, name: ticket.name,
              price: ticket.price, quantity: qty,
            })
          }
          multiTicketMatched = true
        }
      }
    }
    // 兜底：没有匹配到具体票种名时，匹配纯数量（如 "3张" 或 "三张"）
    if (!multiTicketMatched) {
      const numMatch = msg.match(/(\d+|[一二两三四五六七八九十])\s*[张张个]/)
      if (numMatch && cart.value.items.length > 0) {
        const qty = parseInt(numMatch[1])
        const finalQty = (!isNaN(qty) && qty > 0) ? qty : chineseToNum(numMatch[1])
        if (finalQty && finalQty > 0 && finalQty <= 10) {
          cart.value.items[cart.value.items.length - 1].quantity = finalQty
        }
      }
    }
  }

  // 检测确认
  if (containsAny(msg, '确认', '是的', '对的', '没问题', '可以', '好的', '没错', '正确') && cart.value.items.length > 0 && cart.value.visitDate) {
    if (sidePanelMode.value === 'cart') {
      setTimeout(() => confirmCart(), 500)
    }
  }
}

// 解析中文数字为阿拉伯数字（一~十）
function chineseToNum(str) {
  const map = { '一': 1, '二': 2, '两': 2, '俩': 2, '三': 3, '四': 4, '五': 5, '六': 6, '七': 7, '八': 8, '九': 9, '十': 10 }
  return map[str] || null
}

// 从文本中提取票种名后的数量（支持 "1张" 和 "一张"）
function extractTicketQty(text, ticketName) {
  const escapedName = ticketName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const match = text.match(new RegExp(escapedName + '\\s*(\\d+|[一二两三四五六七八九十])\\s*[张张个]'))
  if (!match) return null
  const num = parseInt(match[1])
  return (!isNaN(num) && num > 0 && num <= 10) ? num : chineseToNum(match[1])
}

function extractDate(msg) {
  // 匹配 "今天"
  if (msg.includes('今天')) return new Date()
  // 匹配 "明天"
  if (msg.includes('明天')) {
    const d = new Date()
    d.setDate(d.getDate() + 1)
    return d
  }
  // 匹配 "后天"
  if (msg.includes('后天')) {
    const d = new Date()
    d.setDate(d.getDate() + 2)
    return d
  }
  // 匹配 YYYY-MM-DD 或 YYYY/MM/DD
  const match = msg.match(/(\d{4})[-\/](\d{1,2})[-\/](\d{1,2})/)
  if (match) {
    return new Date(parseInt(match[1]), parseInt(match[2]) - 1, parseInt(match[3]))
  }
  // 匹配 M月D日
  const match2 = msg.match(/(\d{1,2})月(\d{1,2})[日号]/)
  if (match2) {
    const now = new Date()
    return new Date(now.getFullYear(), parseInt(match2[1]) - 1, parseInt(match2[2]))
  }
  return null
}

function containsAny(msg, ...keywords) {
  for (const kw of keywords) {
    if (msg && msg.includes(kw)) return true
  }
  return false
}

// ==================== AI回复处理 ====================
function processAiResponse(text, intent) {
  if (intent === 'buy_ticket') {
    if (purchaseState.value === 'idle') {
      startPurchase()
    }
    // AI可能会引导购票，保持侧面板打开
    if (sidePanelMode.value !== 'cart' && sidePanelMode.value !== 'payment' && sidePanelMode.value !== 'paid' && sidePanelMode.value !== 'complete') {
      openSidePanel('cart')
    }
  }
  if (intent === 'guide') {
    openMap()
  }
}

// ==================== 购票流程 ====================
function startPurchase() {
  purchaseState.value = 'buying'
  // 重置购物车
  cart.value = { visitDate: null, items: [], orderNo: null }
  openSidePanel('cart')
}

function onCartDateChange() {
  // 日期变更时不做额外处理，购物车自动更新
}

function getCartItem(ticketId) {
  return cart.value.items.find(item => String(item.ticketTypeId) === String(ticketId))
}

function toggleTicketInCart(ticket) {
  // 团体票直接跳转到团体订单上传页面，不加入购物车
  if (ticket.isGroup || ticket.name?.includes('团体')) {
    ElMessage.info('团体票需要批量导入名单，请进行相关的操作')
    setTimeout(() => {
      router.push('/group-purchase')
    }, 300)
    return
  }

  if (!ticket.status || (ticket.dailyStock - ticket.soldCount) <= 0) return

  const existing = getCartItem(ticket.id)
  if (existing) {
    // 已经选了，增加数量
    existing.quantity = Math.min(10, existing.quantity + 1)
  } else {
    cart.value.items.push({
      ticketTypeId: ticket.id,
      name: ticket.name,
      price: ticket.price,
      quantity: 1,
    })
  }

  // 如果还没选日期，提示用户
  if (!cart.value.visitDate) {
    input.value = '我想选择游览日期'
  }
}

function adjustCartQty(ticketId, delta) {
  const item = getCartItem(ticketId)
  if (!item) return
  const newQty = item.quantity + delta
  if (newQty <= 0) {
    cart.value.items = cart.value.items.filter(i => i.ticketTypeId !== ticketId)
  } else {
    item.quantity = Math.min(10, newQty)
  }
}

async function confirmCart() {
  if (!cart.value.visitDate || cart.value.items.length === 0) {
    ElMessage.warning('请选择游览日期和票种')
    return
  }

  try {
    const orderData = {
      visitDate: formatCartDate(cart.value.visitDate),
      items: cart.value.items.map(item => ({
        ticketTypeId: item.ticketTypeId,
        quantity: item.quantity,
      })),
    }
    const order = await createOrder(orderData)
    cart.value.orderNo = order.orderNo
    cart.value.totalAmount = order.totalAmount || cartTotal.value
    cart.value.discountAmount = order.discountAmount || 0
    cart.value.couponName = order.couponName || ''
    cart.value.orderNo = order.orderNo
    purchaseState.value = 'paying'
    openSidePanel('payment')

    // 计算实际应付金额
    const finalAmount = (cart.value.totalAmount - cart.value.discountAmount).toFixed(2)
    const discountInfo = cart.value.discountAmount > 0
      ? `（已使用优惠券「${cart.value.couponName}」省 ¥${cart.value.discountAmount}）`
      : ''

    // 发送确认消息到AI
    const confirmMsg = `我已确认下单：${formatCartDate(cart.value.visitDate)}，${cart.value.items.map(i => i.name + '×' + i.quantity).join('、')}，合计¥${finalAmount}，订单号${order.orderNo}`
    messages.value.push({ role: 'user', content: confirmMsg, time: new Date().toLocaleTimeString() })
    messages.value.push({
      role: 'assistant',
      content: `订单已生成！<br/><br/>订单编号：<b>${order.orderNo}</b><br/>游览日期：${formatCartDate(cart.value.visitDate)}<br/>应付金额：<b>¥${finalAmount}</b>${discountInfo ? '<br/>' + discountInfo : ''}<br/><br/>请在右侧面板完成支付。`,
      time: new Date().toLocaleTimeString(),
      intent: 'buy_ticket',
      feedback: 0
    })
    scrollToBottom()
  } catch (e) {
    ElMessage.error('下单失败: ' + (e?.response?.data?.message || e.message || '未知错误'))
  }
}

async function handlePayInPanel() {
  if (!cart.value.orderNo) return

  // 弹出确认框
  const methodLabel = { mock: '模拟支付', wechat: '微信支付', alipay: '支付宝' }[payMethod.value] || '模拟支付'
  const discountTip = cart.value.discountAmount > 0
    ? `<br/>已优惠 ¥${cart.value.discountAmount.toFixed(2)}（${cart.value.couponName}）`
    : ''
  try {
    await ElMessageBox.confirm(
      `支付方式：${methodLabel}${discountTip}<br/>确认支付 ¥${cartPayAmount.value.toFixed(2)} 吗？`,
      '确认支付',
      {
        confirmButtonText: '确认支付',
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: true
      }
    )
  } catch {
    return // 用户取消
  }

  paying.value = true
  try {
    await mockPay({ orderNo: cart.value.orderNo, success: true })
    purchaseState.value = 'paid'
    openSidePanel('paid')

    // 发送支付成功消息
    messages.value.push({
      role: 'assistant',
      content: `支付成功！<br/><br/>您的订单 <b>${cart.value.orderNo}</b> 已支付完毕，金额：<b>¥${cartPayAmount.value.toFixed(2)}</b><br/><br/>⚡ <b>重要提醒：</b>请记得为每张门票录入人脸信息，游览当天可刷脸入园哦～`,
      time: new Date().toLocaleTimeString(),
      intent: 'buy_ticket',
      feedback: 0
    })
    scrollToBottom()
  } catch (e) {
    ElMessage.error('支付失败: ' + (e?.response?.data?.message || e.message || '未知错误'))
  } finally {
    paying.value = false
  }
}

function goToPayPage() {
  if (cart.value.orderNo) {
    router.push({ path: '/pay', query: { orderNo: cart.value.orderNo } })
  }
}

function markFaceDone() {
  purchaseState.value = 'complete'
  openSidePanel('complete')
  messages.value.push({
    role: 'assistant',
    content: `<b>恭喜您完成了所有步骤！</b><br/><br/>游览日期：${formatCartDate(cart.value.visitDate)}<br/>请按时前往${scenicStore.scenicName}，祝您游玩愉快！<br/><br/>提示：您可在「我的订单」中随时录入人脸信息。`,
    time: new Date().toLocaleTimeString(),
    intent: null,
    feedback: 0
  })
  scrollToBottom()
}

function resetPurchase() {
  purchaseState.value = 'idle'
  cart.value = { visitDate: null, items: [], orderNo: null, totalAmount: 0, discountAmount: 0, couponName: '' }
  sidePanelMode.value = null
}

// ==================== 侧面板控制 ====================
function openSidePanel(mode) {
  sidePanelMode.value = mode
  if (mode === 'map') {
    nextTick(() => initMapPanel())
  }
}

function closeSidePanel() {
  sidePanelMode.value = null
  // 不重置购买状态，只是折叠面板
}

// ==================== 地图 ====================
async function openMap() {
  if (spots.value.length === 0) {
    try { spots.value = await getSpots() } catch (e) { ElMessage.error('获取景点数据失败'); return }
  }
  openSidePanel('map')
}

function initMapPanel() {
  delete L.Icon.Default.prototype._getIconUrl
  L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  })

  if (mapPanelInstance) { mapPanelInstance.remove(); mapPanelInstance = null }

  const validSpots = spots.value.filter(s => s.longitude && s.latitude)
  if (validSpots.length === 0) { hasCoordinates.value = false; return }
  hasCoordinates.value = true

  const centerLat = validSpots.reduce((sum, s) => sum + Number(s.latitude), 0) / validSpots.length
  const centerLng = validSpots.reduce((sum, s) => sum + Number(s.longitude), 0) / validSpots.length

  nextTick(() => {
    if (!mapPanelContainer.value) return
    mapPanelInstance = L.map(mapPanelContainer.value).setView([centerLat, centerLng], 15)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors', maxZoom: 19,
    }).addTo(mapPanelInstance)

    validSpots.forEach(spot => {
      const marker = L.marker([Number(spot.latitude), Number(spot.longitude)]).addTo(mapPanelInstance)
      const popupContent = spot.imageUrl
        ? `<b>${spot.name}</b><br/>${spot.description || ''}<br/><img src="${spot.imageUrl}" style="max-width:200px;margin-top:6px;border-radius:4px;"/>`
        : `<b>${spot.name}</b><br/>${spot.description || ''}`
      marker.bindPopup(popupContent)
    })

    setTimeout(() => { mapPanelInstance?.invalidateSize() }, 200)
  })
}

// ==================== 工具函数 ====================
function formatCartDate(d) {
  if (!d) return ''
  const date = d instanceof Date ? d : new Date(d)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function disabledDate(time) {
  return time.getTime() < Date.now() - 86400000
}

// ==================== 原有地图弹窗(保留兼容) ====================
function goToBuyTicket() {
  // 兜底方案：直接跳转到门票选购页面
  router.push('/tickets')
}

async function clearChat() {
  try {
    await ElMessageBox.confirm('确定要清空所有对话记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    messages.value = []
    purchaseState.value = 'idle'
    cart.value = { visitDate: null, items: [], orderNo: null }
    sidePanelMode.value = null
    input.value = ''
    sessionStorage.removeItem(storageKey)
    ElMessage.success('对话已清空')
  } catch (e) {
    // 用户取消，不做任何操作
  }
}

async function openMapDialog() {
  if (spots.value.length === 0) {
    try { spots.value = await getSpots() } catch (e) { ElMessage.error('获取景点数据失败'); return }
  }
  mapDialogVisible.value = true
  await nextTick()
  initDialogMap()
}

function initDialogMap() {
  delete L.Icon.Default.prototype._getIconUrl
  L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  })

  if (mapInstance) { mapInstance.remove(); mapInstance = null }

  const validSpots = spots.value.filter(s => s.longitude && s.latitude)
  if (validSpots.length === 0) { hasCoordinates.value = false; return }
  hasCoordinates.value = true

  const centerLat = validSpots.reduce((sum, s) => sum + Number(s.latitude), 0) / validSpots.length
  const centerLng = validSpots.reduce((sum, s) => sum + Number(s.longitude), 0) / validSpots.length

  mapInstance = L.map(mapContainer.value).setView([centerLat, centerLng], 15)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors', maxZoom: 19,
  }).addTo(mapInstance)

  validSpots.forEach(spot => {
    const marker = L.marker([Number(spot.latitude), Number(spot.longitude)]).addTo(mapInstance)
    const popupContent = spot.imageUrl
      ? `<b>${spot.name}</b><br/>${spot.description || ''}<br/><img src="${spot.imageUrl}" style="max-width:200px;margin-top:6px;border-radius:4px;"/>`
      : `<b>${spot.name}</b><br/>${spot.description || ''}`
    marker.bindPopup(popupContent)
  })

  setTimeout(() => { mapInstance?.invalidateSize() }, 200)
}

// ==================== 反馈 ====================
async function submitFeedback(msg, feedback) {
  if (!msg.conversationId || msg.feedback !== 0) return
  try {
    await submitAiFeedback({ conversationId: msg.conversationId, feedback })
    msg.feedback = feedback
    ElMessage.success(feedback === 1 ? '感谢您的肯定！' : '感谢您的反馈，我们会持续改进')
  } catch (e) {
    ElMessage.error('提交失败: ' + (e.message || '未知错误'))
  }
}
</script>

<style scoped>
:root { --transition: 0.35s cubic-bezier(0.22, 0.61, 0.36, 1); }
.ai-page { margin: -24px; }

/* ====== 页面头部 ====== */
.page-hero {
  padding: 72px 24px 56px; text-align: center;
}
.page-hero .section-header { text-align: center; margin-bottom: 0; }
.page-hero .section-tag {
  display: inline-block; font-size: 13px; font-weight: 700; letter-spacing: 3px;
  color: rgba(255,255,255,0.7); margin-bottom: 16px;
}
.page-hero .section-title {
  font-size: clamp(30px, 4vw, 40px); font-weight: 800; color: #fff;
  margin: 0 0 14px; letter-spacing: -0.5px;
}
.page-hero .section-subtitle { color: rgba(255,255,255,0.85); font-size: 17px; margin: 0; }

/* ====== 双栏布局 ====== */
.content-wrapper { background: transparent; padding: 0 24px 80px; }
.chat-layout {
  max-width: 1300px; margin: 0 auto;
  display: flex; gap: 20px; align-items: flex-start;
  padding-top: 40px;
}

/* ====== 聊天容器（左侧） ====== */
.chat-container {
  flex: 1; min-width: 0;
  background: rgba(255,255,255,0.85); backdrop-filter: blur(10px);
  border-radius: 20px; overflow: hidden;
  box-shadow: 0 4px 24px rgba(0,0,0,0.08);
  display: flex; flex-direction: column;
  height: calc(100vh - 260px); min-height: 500px;
  transition: all 0.3s ease;
}
.chat-container.has-panel {
  flex: 1;
}

/* ====== 侧面板（右侧） ====== */
.side-panel {
  flex: 0 0 380px; width: 380px;
  background: rgba(255,255,255,0.92); backdrop-filter: blur(12px);
  border-radius: 20px; overflow: hidden;
  box-shadow: 0 4px 24px rgba(0,0,0,0.1);
  display: flex; flex-direction: column;
  height: calc(100vh - 260px); min-height: 500px;
  max-height: calc(100vh - 260px);
}
.panel-slide-enter-active { transition: all 0.35s cubic-bezier(0.22, 0.61, 0.36, 1); }
.panel-slide-leave-active { transition: all 0.25s ease-in; }
.panel-slide-enter-from { opacity: 0; transform: translateX(30px); }
.panel-slide-leave-to { opacity: 0; transform: translateX(30px); }

.panel-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 20px; border-bottom: 1px solid #f1f5f9;
  flex-shrink: 0;
}
.panel-title { font-size: 16px; font-weight: 700; color: #1a1a2e; margin: 0; }
.panel-close {
  width: 30px; height: 30px; border: none; border-radius: 8px;
  background: #f1f5f9; color: #94a3b8; font-size: 14px;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.panel-close:hover { background: #fee2e2; color: #ef4444; }

.panel-body {
  flex: 1; overflow-y: auto; padding: 16px 20px;
}

/* ====== 面板通用样式 ====== */
.panel-section { margin-bottom: 20px; }
.panel-label { font-size: 14px; font-weight: 600; color: #1a1a2e; margin-bottom: 10px; }
.panel-date-picker { width: 100%; }

/* ====== 票种选择 ====== */
.panel-ticket-list { display: flex; flex-direction: column; gap: 8px; }
.panel-ticket-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 14px; border: 1.5px solid #e2e8f0; border-radius: 12px;
  cursor: pointer; transition: all 0.2s; position: relative; background: #fff;
}
.panel-ticket-item:hover:not(.disabled) { border-color: v-bind('scenicStore.primaryColor'); }
.panel-ticket-item.selected {
  border-color: v-bind('scenicStore.primaryColor');
  background: rgba(26,115,232,0.04);
}
.panel-ticket-item.disabled { opacity: 0.45; cursor: not-allowed; }
.panel-ticket-info { display: flex; flex-direction: column; gap: 2px; }
.panel-ticket-name { font-size: 14px; font-weight: 600; color: #1a1a2e; }
.panel-ticket-price { font-size: 13px; color: #E6A23C; font-weight: 700; }
.panel-ticket-add {
  font-size: 12px; color: v-bind('scenicStore.primaryColor');
  padding: 4px 12px; border: 1px solid v-bind('scenicStore.primaryColor');
  border-radius: 8px; font-weight: 500;
}
.panel-ticket-action {
  font-size: 12px; padding: 4px 12px; border-radius: 8px; font-weight: 500;
  color: v-bind('scenicStore.primaryColor');
  border: 1px solid v-bind('scenicStore.primaryColor');
}
.panel-ticket-item.is-group .panel-ticket-action {
  color: #92400e; border-color: #f59e0b; background: #fffbeb;
}
.panel-ticket-group-tag {
  display: inline-block; font-size: 10px; background: #fef3c7; color: #92400e;
  padding: 1px 6px; border-radius: 4px; margin-left: 4px; font-weight: 600;
  vertical-align: middle;
}
.panel-ticket-item.is-group {
  border-color: #fcd34d;
}
.panel-ticket-soldout {
  position: absolute; right: 8px; top: 50%; transform: translateY(-50%);
  font-size: 11px; background: #fef0f0; color: #f56c6c;
  padding: 2px 8px; border-radius: 8px; font-weight: 600;
}
.panel-date-hint {
  text-align: center; color: #909399; font-size: 13px;
  padding: 20px 0; background: #f9fafb; border-radius: 10px;
}
.panel-ticket-qty {
  display: flex; align-items: center; gap: 8px;
}
.pqty-btn {
  width: 28px; height: 28px; border: 1.5px solid #e2e8f0; border-radius: 8px;
  background: #fff; font-size: 16px; font-weight: 600; color: #1a1a2e;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.2s; line-height: 1;
}
.pqty-btn:hover { border-color: v-bind('scenicStore.primaryColor'); color: v-bind('scenicStore.primaryColor'); }
.pqty-val { font-size: 15px; font-weight: 700; color: #1a1a2e; min-width: 20px; text-align: center; }

/* ====== 购物车汇总 ====== */
.panel-cart-summary {
  background: #f8fafc; border-radius: 12px; padding: 14px;
}
.panel-cart-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 6px 0; font-size: 13px; color: #475569;
}
.panel-cart-row.total { font-size: 15px; font-weight: 700; color: #1a1a2e; padding-top: 8px; }
.panel-cart-divider { height: 1px; background: #e2e8f0; margin: 6px 0; }
.order-no-text {
  font-family: 'SF Mono', monospace; font-size: 12px;
  color: v-bind('scenicStore.primaryColor');
  background: rgba(26,115,232,0.08); padding: 2px 8px; border-radius: 4px;
}

/* ====== 确认按钮 ====== */
.panel-confirm-btn {
  width: 100%; border: none; border-radius: 12px; padding: 14px;
  margin-top: 16px;
  background: v-bind('scenicStore.primaryColor'); color: #fff;
  font-size: 15px; font-weight: 700; cursor: pointer;
  transition: all 0.3s;
}
.panel-confirm-btn:hover:not(:disabled) { filter: brightness(1.1); transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
.panel-confirm-btn:disabled { opacity: 0.45; cursor: not-allowed; }

/* ====== 支付面板 ====== */
.panel-section-title { font-size: 14px; font-weight: 600; color: #1a1a2e; margin: 16px 0 10px; }
.pay-methods-compact { display: flex; gap: 10px; }
.pay-method-compact {
  flex: 1; display: flex; flex-direction: column; align-items: center; gap: 6px;
  padding: 16px 12px; border: 2px solid #e2e8f0; border-radius: 12px;
  cursor: pointer; transition: all 0.2s; background: #fff;
}
.pay-method-compact:hover { border-color: v-bind('scenicStore.primaryColor'); }
.pay-method-compact.active {
  border-color: v-bind('scenicStore.primaryColor');
  background: rgba(26,115,232,0.04);
}
.pay-method-compact.disabled {
  opacity: 0.4; cursor: not-allowed;
}
.pay-method-compact.disabled:hover { border-color: #e2e8f0; }
.pm-icon {
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.pm-icon :deep(svg) { display: block; }
.pm-icon--mock { color: v-bind('scenicStore.primaryColor'); }
.pm-tag {
  font-size: 11px !important; color: #c0c4cc !important;
  background: #f5f7fa; padding: 2px 6px; border-radius: 4px;
}
.pay-method-compact span:last-child { font-size: 13px; font-weight: 500; color: #475569; }

.panel-pay-btn {
  width: 100%; border: none; border-radius: 12px; padding: 16px;
  margin-top: 20px;
  background: linear-gradient(135deg, #E6A23C, #F56C6C); color: #fff;
  font-size: 16px; font-weight: 700; cursor: pointer;
  transition: all 0.3s;
}
.panel-pay-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(230,162,60,0.3); }
.panel-pay-btn:disabled { opacity: 0.6; cursor: not-allowed; }

/* ====== 支付成功/完成面板 ====== */
.panel-success-icon { font-size: 48px; text-align: center; margin: 16px 0 8px; }
.panel-success-title { font-size: 20px; font-weight: 700; color: #1a1a2e; text-align: center; margin: 0 0 8px; }
.panel-success-text { font-size: 14px; color: #64748b; text-align: center; margin: 0 0 16px; }
.panel-success-text code {
  background: #f1f5f9; padding: 2px 8px; border-radius: 4px;
  color: v-bind('scenicStore.primaryColor'); font-family: monospace;
}

.panel-alert {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 12px 14px; background: #fef3c7; border-radius: 10px;
  margin-bottom: 16px; font-size: 13px; color: #92400e; line-height: 1.5;
}
.panel-alert-icon { font-size: 18px; flex-shrink: 0; }

.panel-face-btn {
  width: 100%; border: none; border-radius: 12px; padding: 13px;
  margin-bottom: 8px;
  background: v-bind('scenicStore.primaryColor'); color: #fff;
  font-size: 14px; font-weight: 600; cursor: pointer;
  transition: all 0.3s;
}
.panel-face-btn:hover { filter: brightness(1.1); transform: translateY(-1px); }
.panel-face-btn.secondary {
  background: #f1f5f9; color: #475569;
}
.panel-face-btn.secondary:hover { background: #e2e8f0; }

.panel-complete-summary {
  background: #f8fafc; border-radius: 12px; padding: 14px; margin: 12px 0;
}
.panel-complete-btns { display: flex; gap: 8px; margin-top: 12px; }
.panel-complete-btns .panel-face-btn { flex: 1; }

/* ====== 空状态 ====== */
.panel-empty { text-align: center; padding: 40px 20px; }
.panel-empty-icon { font-size: 40px; margin-bottom: 12px; display: block; }
.panel-empty p { margin: 4px 0; color: #64748b; font-size: 14px; }
.panel-empty-hint { font-size: 12px !important; color: #94a3b8 !important; }

/* ====== 地图 ====== */
.panel-map-body { padding: 0 !important; }
.map-panel-container { width: 100%; height: 100%; }
.map-empty {
  height: 100%; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  color: #909399;
}
.map-empty-icon { font-size: 48px; margin-bottom: 12px; }

/* ====== 欢迎区 ====== */
.welcome { text-align: center; padding: 48px 20px; }
.welcome-icon { font-size: 52px; margin-bottom: 16px; }
.welcome h3 { font-size: 20px; color: #1a1a2e; margin: 0 0 32px; font-weight: 700; }
.welcome-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; max-width: 600px; margin: 0 auto; }
.welcome-card {
  background: #fff; border-radius: 14px; padding: 20px 14px;
  border-top: 3px solid; box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  cursor: pointer; transition: all 0.2s;
}
.welcome-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.welcome-card.disabled { opacity: 0.4; cursor: not-allowed; pointer-events: none; }
.welcome-card-icon { font-size: 24px; }
.welcome-card span { font-size: 14px; font-weight: 600; color: #1a1a2e; }
.welcome-card small { font-size: 11px; color: #94a3b8; }

/* ====== 消息区域 ====== */
.chat-messages {
  flex: 1; overflow-y: auto; padding: 24px;
  background: rgba(248,250,252,0.5);
}
.message { margin-bottom: 20px; }
.message.user { text-align: right; }
.message.assistant { text-align: left; }
.msg-content {
  display: inline-block; max-width: 72%; padding: 14px 18px; border-radius: 16px;
  font-size: 14px; line-height: 1.7; word-break: break-word;
}
.message.user .msg-content {
  background: v-bind('scenicStore.primaryColor'); color: #fff; border-bottom-right-radius: 6px;
}
.message.assistant .msg-content {
  background: #fff; color: #333; box-shadow: 0 1px 3px rgba(0,0,0,0.06); border-bottom-left-radius: 6px;
}
.msg-time { font-size: 11px; color: #94a3b8; margin-top: 4px; }
.streaming .cursor { animation: blink 1s infinite; }
@keyframes blink { 0%,50% { opacity: 1; } 51%,100% { opacity: 0; } }

.ai-hint {
  display: inline-block; background: rgba(26,115,232,0.08);
  border-left: 3px solid v-bind('scenicStore.primaryColor');
  padding: 6px 12px; margin-top: 8px; border-radius: 0 6px 6px 0;
  font-size: 14px; color: v-bind('scenicStore.primaryColor');
}

/* ====== 快捷操作按钮 ====== */
.msg-actions { margin-top: 10px; display: flex; gap: 10px; flex-wrap: wrap; }
.msg-action-btn {
  border: none; border-radius: 10px; padding: 8px 18px;
  color: #fff; font-size: 13px; font-weight: 600; cursor: pointer;
  transition: all 0.2s;
}
.msg-action-btn:hover { filter: brightness(1.1); transform: scale(1.02); }

/* ====== 快捷回复按钮组 ====== */
.quick-replies { margin-top: 10px; display: flex; gap: 8px; flex-wrap: wrap; }
.quick-reply-btn {
  border: none; border-radius: 20px; padding: 8px 18px;
  font-size: 13px; font-weight: 600; cursor: pointer;
  transition: all 0.2s; background: #f1f5f9; color: #475569;
}
.quick-reply-btn:hover { filter: brightness(1.05); transform: translateY(-1px); }

/* ====== 反馈 ====== */
.msg-feedback { margin-top: 6px; display: flex; gap: 12px; }
.fb-btn {
  cursor: pointer; font-size: 13px; color: #94a3b8; padding: 2px 8px;
  border-radius: 4px; transition: all 0.2s; user-select: none;
}
.fb-btn:hover { color: v-bind('scenicStore.primaryColor'); background: rgba(26,115,232,0.08); }
.fb-btn.active { color: v-bind('scenicStore.primaryColor'); font-weight: 600; background: rgba(26,115,232,0.08); cursor: default; }

/* ====== 输入框 ====== */
.chat-input { padding: 16px 24px; border-top: 1px solid #f1f5f9; }
.chat-input-inner { display: flex; gap: 12px; }
.chat-input-field {
  flex: 1; border: 2px solid #e2e8f0; border-radius: 14px; padding: 12px 18px;
  font-size: 15px; outline: none; transition: border-color 0.2s; background: #fff;
}
.chat-input-field:focus { border-color: v-bind('scenicStore.primaryColor'); }
.chat-input-field:disabled { background: #f8fafc; }
.chat-send-btn {
  border: none; border-radius: 14px; padding: 12px 28px;
  background: v-bind('scenicStore.primaryColor'); color: #fff;
  font-size: 15px; font-weight: 600; cursor: pointer;
  transition: all 0.2s; white-space: nowrap;
}
.chat-send-btn:hover:not(:disabled) { filter: brightness(1.1); }
.chat-send-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.chat-send-btn.loading { opacity: 0.7; }

.chat-clear-btn {
  height: 36px; align-self: center; flex-shrink: 0;
  border: 1.5px solid #e2e8f0; border-radius: 10px; padding: 0 14px;
  background: #f8fafc; color: #94a3b8; font-size: 13px; font-weight: 500;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.2s; white-space: nowrap;
}
.chat-clear-btn:hover { border-color: #f56c6c; color: #f56c6c; background: #fef2f2; }

/* ====== 地图弹窗 ====== */
.map-container { width: 100%; height: 450px; border-radius: 6px; overflow: hidden; }

/* ====== 响应式 ====== */
@media (max-width: 900px) {
  .chat-layout { flex-direction: column; }
  .side-panel {
    flex: 0 0 auto; width: 100%; height: auto;
    max-height: 400px; min-height: auto;
  }
  .chat-container { height: calc(100vh - 400px); min-height: 400px; }
  .chat-container.has-panel { height: 400px; min-height: 300px; }
}
@media (max-width: 600px) {
  .welcome-cards { grid-template-columns: 1fr; }
  .msg-content { max-width: 90%; }
}

/* 暂停运营横幅 */
.suspend-banner {
  display: flex; align-items: center; gap: 16px;
  background: linear-gradient(135deg, #fff5f5, #fef0f0);
  border: 1px solid #f56c6c; border-radius: 12px;
  padding: 20px 28px; margin-bottom: 24px; max-width: 1300px; margin-left: auto; margin-right: auto;
}
.suspend-icon { font-size: 32px; }
.suspend-title { font-size: 16px; font-weight: 700; color: #f56c6c; margin-bottom: 4px; }
.suspend-desc { font-size: 13px; color: #909399; }
</style>
