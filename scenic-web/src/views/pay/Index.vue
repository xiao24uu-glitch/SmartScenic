<template>
  <div class="pay-page">
    <!-- ====== 页面头部 ====== -->
    <div class="page-hero" :class="'hero--' + heroStatusClass">
      <div class="hero-content">
        <div class="hero-icon-wrap">
          <span class="hero-emoji">{{ heroEmoji }}</span>
        </div>
        <h2 class="hero-title">{{ statusTitle }}</h2>
        <p class="hero-subtitle">{{ statusSubTitle }}</p>
        <div class="hero-actions" v-if="(isGroupOrder && groupOrder.status === 1) || (!isGroupOrder && (order.status === 0 || order.status === 1))">
          <button v-if="(isGroupOrder && groupOrder.status === 1) || (!isGroupOrder && order.status === 0)" class="hero-btn hero-btn--pay" @click="handlePay" :disabled="paying">
            <span class="btn-inner">{{ paying ? '处理中...' : '确认支付' }}</span>
          </button>
          <button v-if="!isGroupOrder && order.status === 0" class="hero-btn hero-btn--ghost" @click="handleCancel">
            取消订单
          </button>
          <button v-if="!isGroupOrder && order.status === 1" class="hero-btn hero-btn--refund" @click="handleApplyRefund">
            申请退款
          </button>
        </div>
      </div>
    </div>

    <div class="content-wrapper">
      <!-- 订单详情卡片 -->
      <div class="section" v-observe>
        <div class="detail-card">
          <div class="card-header">
            <h3>订单详情</h3>
            <span class="card-status" :class="'status--' + statusTagType">{{ statusText }}</span>
          </div>
          <div class="card-body">
            <div class="info-grid">
              <!-- 普通订单信息 -->
              <template v-if="!isGroupOrder">
                <div class="info-item">
                  <span class="info-label">订单编号</span>
                  <span class="info-value">
                    <code class="order-no">{{ order.orderNo }}</code>
                    <button class="copy-btn" @click="copyOrderNo">复制</button>
                  </span>
                </div>
                <div class="info-item">
                  <span class="info-label">景区名称</span>
                  <span class="info-value">{{ order.scenicName || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">游览日期</span>
                  <span class="info-value">{{ order.visitDate }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">创建时间</span>
                  <span class="info-value">{{ order.createTime }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">订单金额</span>
                  <span class="info-value price">¥{{ order.totalAmount }}</span>
                </div>
                <div class="info-item" v-if="order.discountAmount > 0">
                  <span class="info-label">优惠金额</span>
                  <div class="info-value" style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                    <span class="info-value price" style="color: #67C23A;">-¥{{ order.discountAmount }}</span>
                    <span v-if="order.couponName" class="coupon-used-name">{{ order.couponName }}</span>
                    <button v-if="order.status === 0" class="coupon-cancel-btn" @click="handleCancelCoupon">取消使用</button>
                  </div>
                </div>
                <div class="info-item">
                  <span class="info-label">{{ order.payAmount ? '实付金额' : '应付金额' }}</span>
                  <span class="info-value price">¥{{ (order.payAmount || (order.totalAmount - (order.discountAmount || 0))).toFixed(2) }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">支付方式</span>
                  <span class="info-value">{{ payTypeText }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">支付时间</span>
                  <span class="info-value">{{ order.payTime || '-' }}</span>
                </div>
              </template>
              <!-- 团体订单信息 -->
              <template v-else>
                <div class="info-item">
                  <span class="info-label">订单类型</span>
                  <span class="info-value"><el-tag size="small" type="warning">团体订单</el-tag></span>
                </div>
                <div class="info-item">
                  <span class="info-label">团体名称</span>
                  <span class="info-value">{{ groupOrder.groupName }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">联系人</span>
                  <span class="info-value">{{ groupOrder.contactName }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">联系电话</span>
                  <span class="info-value">{{ groupOrder.contactPhone }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">游览日期</span>
                  <span class="info-value">{{ groupOrder.visitDate }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">总人数</span>
                  <span class="info-value">{{ groupOrder.totalCount }}人</span>
                </div>
                <div class="info-item">
                  <span class="info-label">订单金额</span>
                  <span class="info-value price">¥{{ groupOrder.totalAmount }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">入园进度</span>
                  <span class="info-value">{{ groupOrder.enteredCount }}/{{ groupOrder.totalCount }}人</span>
                </div>
              </template>
            </div>

            <!-- 票种明细 -->
            <div class="items-section" v-if="!isGroupOrder && order.items && order.items.length">
              <h4>票种明细</h4>
              <el-table :data="order.items" class="modern-table">
                <el-table-column prop="ticketTypeName" label="票种" />
                <el-table-column label="单价" width="100">
                  <template #default="{ row }">¥{{ row.unitPrice }}</template>
                </el-table-column>
                <el-table-column prop="quantity" label="数量" width="80" />
                <el-table-column label="小计" width="110">
                  <template #default="{ row }">¥{{ (row.unitPrice * row.quantity).toFixed(2) }}</template>
                </el-table-column>
              </el-table>
            </div>

            <!-- 支付方式选择（待支付时） -->
            <div class="pay-section" v-if="(!isGroupOrder && order.status === 0) || (isGroupOrder && groupOrder.status === 1)">
              <div class="section-title">选择支付方式</div>
              <div class="pay-methods">
                <div class="pay-method" :class="{ active: payMethod === 'mock' }" @click="payMethod = 'mock'">
                  <span class="pay-icon pay-icon--mock">
                    <svg viewBox="0 0 48 48" width="48" height="48" fill="none"><rect x="4" y="8" width="40" height="32" rx="4" stroke="currentColor" stroke-width="3" fill="none"/><line x1="4" y1="18" x2="44" y2="18" stroke="currentColor" stroke-width="3"/><circle cx="12" cy="28" r="3" fill="currentColor"/><circle cx="22" cy="28" r="3" fill="currentColor"/><rect x="28" y="24" width="12" height="8" rx="2" fill="currentColor"/><line x1="34" y1="24" x2="34" y2="32" stroke="#fff" stroke-width="1.5"/></svg>
                  </span>
                  <span class="pay-name">模拟支付</span>
                </div>
                <div class="pay-method disabled" @click="ElMessage.info('微信支付暂未开放')">
                  <span class="pay-icon pay-icon--wechat">
                    <svg viewBox="0 0 48 48" width="48" height="48" fill="#07C160"><path d="M17.382 4.376C7.782 4.376 0 10.952 0 19.06c0 4.424 2.34 8.406 6.004 11.1.118.086.198.227.194.378-.003.084-.024.167-.06.243l-.78 2.96c-.038.14-.096.282-.096.426 0 .326.26.59.58.59.11 0 .22-.037.334-.108l3.806-2.228c.204-.12.45-.14.674-.056.333.126.675.225 1.025.296.453.208.875.4 1.526.51-.28-.6-.478-1.246-.58-1.92-1.714-5.156.314-9.944 3.864-12.892 3.406-2.83 7.764-3.96 11.706-3.676C27.512 7.874 20.258 4.376 17.382 4.376zM11.57 11.982c1.284 0 2.324 1.058 2.324 2.36 0 1.302-1.04 2.356-2.324 2.356-1.283 0-2.324-1.054-2.324-2.356 0-1.302 1.04-2.36 2.324-2.36zm11.626 0c1.284 0 2.324 1.058 2.324 2.36 0 1.302-1.04 2.356-2.324 2.356-1.283 0-2.324-1.054-2.324-2.356 0-1.302 1.04-2.36 2.324-2.36zm10.68 5.734c-3.594-.104-7.492 1.024-10.56 3.572-3.44 2.856-5.374 7.44-3.56 12.44 1.484 4.086 5.56 6.896 10.048 6.896.71 0 1.45-.074 2.194-.196l.116-.022c.37-.07.76.002 1.124.184l3.07 1.796c.07.04.136.07.206.07.22 0 .38-.18.38-.396 0-.08-.02-.16-.056-.236l-.01-.022-.628-2.386a.944.944 0 0 1 .34-1.048c2.688-2.032 4.252-4.86 4.252-7.924 0-5.746-4.934-10.478-10.916-10.728zM32.532 20.4c1 0 1.812.83 1.812 1.854 0 1.024-.812 1.854-1.812 1.854s-1.814-.83-1.814-1.854c0-1.024.814-1.854 1.814-1.854zm8.696 0c1 0 1.812.83 1.812 1.854 0 1.024-.812 1.854-1.812 1.854S39.414 23.278 39.414 22.254c0-1.024.814-1.854 1.814-1.854z"/></svg>
                  </span>
                  <span class="pay-name">微信支付</span>
                </div>
                <div class="pay-method disabled" @click="ElMessage.info('支付宝暂未开放')">
                  <span class="pay-icon pay-icon--alipay">
                    <svg viewBox="0 0 48 48" width="48" height="48" fill="#1677FF"><path d="M24 6C14.059 6 6 14.059 6 24s8.059 18 18 18 18-8.059 18-18S33.941 6 24 6zm8.4 22.2c-1.2 2-3.2 3.28-6.44 3.28h-1.04c-.82 0-1.48.6-1.58 1.36l-.24 1.94c-.1.76-.5 1.2-1.26 1.2h-2.2c-.72 0-1-.44-.84-1.22l1.22-7.72c.16-.92.76-1.22 1.5-1.22h4.8c4.06 0 6.02 2 6.02 2.38zm-8.44-9.1c-.84.26-1.36.84-1.58 1.58l-.3 1.8h3.94c2.04 0 3.02-1 3.02-2.04 0-.92-.78-1.34-2.8-1.34h-2.28z"/></svg>
                  </span>
                  <span class="pay-name">支付宝</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 人脸录入卡片（已支付时，仅普通订单） -->
      <div class="section" v-if="!isGroupOrder && order.status === 1" v-observe>
        <div class="detail-card face-card">
          <div class="card-header">
            <h3>票务人脸录入</h3>
            <span class="card-status" :class="'status--' + faceProgressType">{{ faceProgressText }}</span>
          </div>
          <div class="card-body">
            <p class="face-tip">请为每张门票填写使用者信息并录入人脸（共需录入 <strong>{{ totalTickets }}</strong> 张）</p>

            <div class="ticket-face-list">
              <div
                v-for="(ticket, idx) in ticketList"
                :key="idx"
                class="ticket-face-item"
                :class="{ 'has-face': ticket.face }"
              >
                <div class="ticket-info">
                  <span class="ticket-name">{{ ticket.ticketTypeName }}</span>
                  <span class="ticket-price">¥{{ ticket.unitPrice }}</span>
                  <template v-if="ticket.face">
                    <div class="ticket-user-info">
                      <span class="user-tag" v-if="ticket.face.realName">{{ ticket.face.realName }}</span>
                      <span class="user-tag" v-if="ticket.face.phone">{{ ticket.face.phone }}</span>
                    </div>
                  </template>
                  <template v-else>
                    <div class="ticket-user-form">
                      <el-input
                        v-model="ticket.realName"
                        placeholder="真实姓名"
                        class="required-input"
                        style="min-width: 120px; max-width: 180px; flex: 1;"
                      />
                      <el-input
                        v-model="ticket.phone"
                        placeholder="手机号"
                        :class="{ 'required-input': isAdultTicket(ticket.ticketTypeName) }"
                        style="min-width: 140px; max-width: 200px; flex: 1;"
                      />
                    </div>
                  </template>
                </div>
                <div class="ticket-face-action">
                  <template v-if="ticket.face">
                    <el-image
                      :src="ticket.face.faceImagePath"
                      style="width: 56px; height: 56px; object-fit: cover; border-radius: 10px;"
                      :preview-src-list="[ticket.face.faceImagePath]"
                      preview-teleported
                    />
                    <span class="face-tag face-tag--ok">已录入</span>
                    <button class="face-re-btn" @click="openFaceDialog(idx)">重新录入</button>
                  </template>
                  <template v-else>
                    <div class="face-placeholder">
                      <span class="face-ph-icon">✖</span>
                      <span class="face-ph-text">未录入</span>
                    </div>
                    <button class="face-btn" @click="openFaceDialog(idx)">录入人脸</button>
                  </template>
                </div>
              </div>
            </div>

            <div class="face-complete" v-if="orderFaces.length >= totalTickets">
              ✅ 全部人脸录入完成！游览当日可刷脸入园。
            </div>
          </div>
        </div>
      </div>

      <!-- 操作记录 -->
      <div class="section" v-if="order.status !== 0" v-observe>
        <div class="detail-card">
          <div class="card-header">
            <h3>操作记录</h3>
          </div>
          <div class="card-body">
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
        </div>
      </div>

      <!-- 底部操作 -->
      <div class="section bottom-section" v-observe>
        <div class="bottom-actions">
          <button v-if="!isGroupOrder && order.status !== 1" class="coupon-receive-btn" @click="couponDialogVisible = true">领取优惠券</button>
          <button class="bottom-btn bottom-btn--outline" @click="router.push('/orders')">返回订单列表</button>
          <button class="bottom-btn bottom-btn--outline" @click="router.push('/tickets')">继续购票</button>
          <button class="bottom-btn bottom-btn--primary" @click="router.push('/')">返回首页</button>
        </div>
      </div>
    </div>

    <!-- 人脸录入弹窗 -->
    <el-dialog
      v-model="faceDialogVisible"
      title="人脸录入"
      width="560px"
      :close-on-click-modal="false"
      :close-on-press-escape="!dialogSubmitting"
      :show-close="!dialogSubmitting"
      @closed="closeFaceDialog"
    >
      <el-steps :active="activeStep" finish-status="success" simple style="margin-bottom: 24px;">
        <el-step title="拍照/上传" />
        <el-step title="质量检测" />
        <el-step title="注册完成" />
      </el-steps>

      <div v-if="activeStep === 0" class="face-dialog-content">
        <p class="dialog-tip">请使用摄像头拍照或上传一张正面免冠照片用于入园人脸识别</p>
        <div class="dialog-order-info">关联订单：{{ order.orderNo }}</div>

        <div v-if="cameraActive" class="face-preview-box camera-box">
          <video ref="videoRef" autoplay playsinline class="camera-video" />
        </div>
        <div v-else class="face-preview-box">
          <img v-if="dialogFaceImageBase64" :src="'data:image/jpeg;base64,' + dialogFaceImageBase64" class="face-preview-img" />
          <div v-else class="face-preview-placeholder">
            <span style="font-size: 48px;">📷</span>
            <span>请拍照或上传照片</span>
          </div>
        </div>

        <div v-if="cameraActive" class="dialog-actions">
          <el-button type="success" size="large" @click="capturePhoto" :disabled="dialogSubmitting">拍照</el-button>
          <el-button size="large" @click="stopCamera" :disabled="dialogSubmitting">取消</el-button>
        </div>
        <div v-else-if="dialogFaceImageBase64" class="dialog-actions">
          <el-button type="success" size="large" @click="submitDialogFace" :loading="dialogSubmitting">确认提交</el-button>
          <el-button size="large" @click="dialogFaceImageBase64 = ''" :disabled="dialogSubmitting">重新选择</el-button>
        </div>
        <div v-else class="dialog-actions">
          <input type="file" accept="image/*" @change="handleDialogFile" ref="dialogFileRef" style="display: none;" />
          <el-button type="primary" size="large" @click="startCamera" :disabled="dialogSubmitting">拍照</el-button>
          <el-button size="large" @click="$refs.dialogFileRef.click()" :disabled="dialogSubmitting">本地上传</el-button>
        </div>
      </div>

      <div v-if="activeStep === 1" class="face-dialog-content center">
        <el-icon class="is-loading" :size="48" color="#409eff"><Loading /></el-icon>
        <p style="margin-top: 16px; color: #606266;">正在质量检测并注册人脸，请稍候...</p>
      </div>

      <div v-if="activeStep === 2" class="face-dialog-content center">
        <span style="font-size: 48px;">✅</span>
        <p style="margin-top: 16px; color: #67c23a; font-size: 16px; font-weight: bold;">人脸录入成功！</p>
        <p style="color: #909399; margin-top: 8px;">{{ currentTicketIndex !== null ? ticketList[currentTicketIndex]?.ticketTypeName : '' }} 人脸已绑定</p>
        <el-button type="primary" style="margin-top: 24px;" @click="faceDialogVisible = false">完成</el-button>
      </div>
    </el-dialog>

    <!-- 领取优惠券弹窗 -->
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
          <!-- 未使用 -->
          <div class="coupon-tpl-right-btns" v-if="mc.status === 0">
            <button
              v-if="order.couponId === mc.id"
              class="tpl-claim-btn my-coupon-used-btn"
              disabled
            >
              已使用
            </button>
            <button
              v-else-if="!order.discountAmount"
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
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useScenicStore } from '../../stores/scenic'
import { getOrderDetail, mockPay, cancelOrder, applyRefund, getOrderFaces, registerFace, removeOrderCoupon } from '../../api'
import { getGroupOrderDetail, confirmGroupPay } from '../../api'
import { getAvailableTemplates, receiveCoupon, getMyCoupons, deleteMyCoupon, useOrderCoupon } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const scenicStore = useScenicStore()

const order = ref({
  orderNo: '',
  scenicName: '',
  visitDate: '',
  createTime: '',
  totalAmount: 0,
  payAmount: null,
  payType: null,
  payTime: null,
  status: 0,
  items: [],
})

// 团体订单相关
const isGroupOrder = ref(false)
const groupOrderId = ref(null)
const groupOrder = ref({
  groupName: '',
  contactName: '',
  contactPhone: '',
  visitDate: '',
  totalCount: 0,
  totalAmount: 0,
  status: 0,
  enteredCount: 0,
})

const payMethod = ref('mock')
const paying = ref(false)
const orderFaces = ref([])
const faceDialogVisible = ref(false)
const currentTicketIndex = ref(null)
const activeStep = ref(0)
const dialogFaceImageBase64 = ref('')
const dialogSubmitting = ref(false)
const dialogFileRef = ref(null)
const videoRef = ref(null)
const cameraActive = ref(false)
const videoStream = ref(null)

// 优惠券领取
const couponDialogVisible = ref(false)
const claimableTemplates = ref([])
const claimingId = ref(null)
const claimedIds = ref(new Set())
const myCoupons = ref([])

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
    await loadMyCoupons()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '删除失败')
    }
  }
}

async function handleUseMyCoupon(mc) {
  try {
    await ElMessageBox.confirm(`确定使用优惠券「${mc.couponName}」吗？`, '使用优惠券', {
      confirmButtonText: '确定', cancelButtonText: '再想想', type: 'success',
    })
    order.value = await useOrderCoupon(order.value.orderNo, mc.id)
    ElMessage.success('优惠券已应用')
    couponDialogVisible.value = false
    await loadMyCoupons()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '使用失败')
    }
  }
}

async function claimCoupon(tpl) {
  claimingId.value = tpl.id
  try {
    await receiveCoupon(tpl.id)
    ElMessage.success('领取成功')
    claimedIds.value.add(tpl.id)
    await loadMyCoupons()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '领取失败')
  } finally {
    claimingId.value = null
  }
}

async function handleCancelCoupon() {
  try {
    await ElMessageBox.confirm('确定要取消使用该优惠券吗？取消后订单将恢复原价。', '取消使用优惠券', {
      confirmButtonText: '确定', cancelButtonText: '再想想', type: 'warning',
    })
    await removeOrderCoupon(order.value.orderNo)
    ElMessage.success('已取消优惠券使用')
    await loadOrder()
  } catch (e) {
    if (e !== 'cancel') console.error('取消优惠券失败', e)
  }
}

const statusTitle = computed(() => {
  if (isGroupOrder.value) {
    const map = { 0: '待审核', 1: '订单待支付', 2: '已拒绝', 3: '支付成功', 4: '修改待审核' }
    return map[groupOrder.value.status] || '未知状态'
  }
  const map = { 0: '订单待支付', 1: '支付成功', 2: '订单已取消', 3: '订单已退款', 4: '部分退款' }
  return map[order.value.status] || '未知状态'
})

const statusSubTitle = computed(() => {
  if (isGroupOrder.value) {
    if (groupOrder.value.status === 1) return `请尽快完成支付 · 团体「${groupOrder.value.groupName}」`
    if (groupOrder.value.status === 3) return `支付金额 ¥${groupOrder.value.totalAmount}`
    return ''
  }
  if (order.value.status === 0) return `请尽快完成支付 · 订单编号：${order.value.orderNo}`
  if (order.value.status === 1) return `支付金额 ¥${(order.value.payAmount || (order.value.totalAmount - (order.value.discountAmount || 0))).toFixed(2)}`
  return ''
})

const heroEmoji = computed(() => {
  if (isGroupOrder.value) {
    const map = { 0: '📋', 1: '⏳', 2: '❌', 3: '✅', 4: '⚠️' }
    return map[groupOrder.value.status] || '📋'
  }
  const map = { 0: '⏳', 1: '✅', 2: '❌', 3: '↩️', 4: '⚠️' }
  return map[order.value.status] || '📋'
})

const heroStatusClass = computed(() => {
  if (isGroupOrder.value) {
    const map = { 0: 'info', 1: 'warning', 2: 'danger', 3: 'success', 4: 'warning' }
    return map[groupOrder.value.status] || 'info'
  }
  const map = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger', 4: 'warning' }
  return map[order.value.status] || 'info'
})

// 主题色透明变体（解决 v-bind 不支持 SCSS #{} 插值的问题）
const primaryColor80 = computed(() => scenicStore.primaryColor + 'cc')   // 80% opacity
const primaryColor04 = computed(() => scenicStore.primaryColor + '0a')   // ~4% opacity

const statusText = computed(() => {
  if (isGroupOrder.value) {
    const map = { 0: '待审核', 1: '已通过（待支付）', 2: '已拒绝', 3: '已支付', 4: '修改待审核' }
    return map[groupOrder.value.status] || '未知'
  }
  const map = { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已退款', 4: '部分退款' }
  return map[order.value.status] || '未知'
})

const statusTagType = computed(() => {
  if (isGroupOrder.value) {
    const map = { 0: 'warning', 1: 'info', 2: 'danger', 3: 'success', 4: 'warning' }
    return map[groupOrder.value.status] || 'info'
  }
  const map = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger', 4: 'warning' }
  return map[order.value.status] || 'info'
})

const payTypeText = computed(() => {
  if (isGroupOrder.value) return groupOrder.value.status === 3 ? '模拟支付' : '-'
  if (order.value.payType === null || order.value.payType === undefined) return '-'
  const map = { 0: '模拟支付', 1: '微信支付', 2: '支付宝' }
  return map[order.value.payType] || '模拟支付'
})

const totalTickets = computed(() => {
  if (!order.value.items) return 0
  return order.value.items.reduce((sum, item) => sum + (item.quantity || 0), 0)
})

const faceProgressText = computed(() => {
  return `${orderFaces.value.length} / ${totalTickets.value} 张已录入`
})

const faceProgressType = computed(() => {
  if (orderFaces.value.length >= totalTickets.value) return 'success'
  if (orderFaces.value.length > 0) return 'warning'
  return 'info'
})

const ticketList = ref([])

function buildTicketList() {
  const oldList = ticketList.value || []
  const faces = [...orderFaces.value].sort((a, b) => a.id - b.id)
  const list = []
  let faceIdx = 0
  for (const item of (order.value.items || [])) {
    for (let i = 0; i < (item.quantity || 0); i++) {
      const faceData = faces[faceIdx] || null
      const oldItem = oldList[list.length] || {}
      list.push({
        ticketTypeName: item.ticketTypeName,
        unitPrice: item.unitPrice,
        face: faceData,
        // faceData 存在时用数据库真实姓名，不存在时保留用户已填的值
        realName: faceData?.realName || oldItem.realName || '',
        phone: faceData?.phone || oldItem.phone || '',
      })
      if (faces[faceIdx]) faceIdx++
    }
  }
  ticketList.value = list
}

function isAdultTicket(ticketTypeName) {
  return ticketTypeName === '成人票'
}

async function loadOrder() {
  // 检查是否为团体订单
  const gid = route.query.groupOrderId
  if (gid) {
    isGroupOrder.value = true
    groupOrderId.value = gid
    try {
      const data = await getGroupOrderDetail(gid)
      groupOrder.value = data
    } catch (e) {
      console.error('加载团体订单失败', e)
      ElMessage.error('加载团体订单失败')
      router.push('/orders')
    }
    return
  }

  const orderNo = route.query.orderNo
  if (!orderNo) {
    ElMessage.error('缺少订单编号')
    router.push('/orders')
    return
  }
  try {
    const data = await getOrderDetail(orderNo)
    order.value = data
    buildTicketList()
    if (data.status === 1) await loadOrderFaces()
  } catch (e) {
    console.error('加载订单失败', e)
    router.push('/orders')
  }
}

async function loadOrderFaces() {
  try {
    const faces = await getOrderFaces(order.value.orderNo)
    orderFaces.value = faces || []
    buildTicketList()
  } catch (e) {
    console.error('加载订单人脸失败', e)
  }
}

function openFaceDialog(index) {
  const ticket = ticketList.value[index]
  if (!ticket.realName || !ticket.realName.trim()) {
    ElMessage.warning('请填写使用者真实姓名')
    return
  }
  if (isAdultTicket(ticket.ticketTypeName)) {
    if (!ticket.phone || !ticket.phone.trim()) {
      ElMessage.warning('成人票必须填写手机号')
      return
    }
  }
  currentTicketIndex.value = index
  activeStep.value = 0
  dialogFaceImageBase64.value = ''
  dialogSubmitting.value = false
  faceDialogVisible.value = true
}

function closeFaceDialog() {
  stopCamera()
  currentTicketIndex.value = null
  activeStep.value = 0
  dialogFaceImageBase64.value = ''
  dialogSubmitting.value = false
}

async function startCamera() {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' }, audio: false })
    videoStream.value = stream
    cameraActive.value = true
    await nextTick()
    if (videoRef.value) videoRef.value.srcObject = stream
  } catch (err) {
    console.error('打开摄像头失败', err)
    ElMessage.error('无法访问摄像头，请检查权限设置或改用本地上传')
  }
}

function stopCamera() {
  if (videoStream.value) {
    videoStream.value.getTracks().forEach(track => track.stop())
    videoStream.value = null
  }
  cameraActive.value = false
}

function capturePhoto() {
  if (!videoRef.value || !videoStream.value) return
  const video = videoRef.value
  const canvas = document.createElement('canvas')
  canvas.width = video.videoWidth || 640
  canvas.height = video.videoHeight || 480
  const ctx = canvas.getContext('2d')
  ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
  dialogFaceImageBase64.value = canvas.toDataURL('image/jpeg').split(',')[1]
  stopCamera()
}

function handleDialogFile(event) {
  const file = event.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (e) => { dialogFaceImageBase64.value = e.target.result.split(',')[1] }
  reader.readAsDataURL(file)
}

async function submitDialogFace() {
  if (!dialogFaceImageBase64.value) { ElMessage.warning('请先选择照片'); return }
  dialogSubmitting.value = true
  activeStep.value = 1
  try {
    const ticket = ticketList.value[currentTicketIndex.value]
    const payload = {
      imageBase64: dialogFaceImageBase64.value,
      orderNo: order.value.orderNo,
      faceId: ticket?.face?.id || null,
      realName: ticket.realName || null,
      phone: ticket.phone || null,
    }
    console.log('📤 提交人脸注册: index=' + currentTicketIndex.value + ', realName=' + payload.realName + ', phone=' + payload.phone, payload)
    await registerFace(payload)
    activeStep.value = 2
    await loadOrderFaces()
  } catch (e) {
    activeStep.value = 0
    ElMessage.error(e?.response?.data?.message || '人脸录入失败')
  } finally {
    dialogSubmitting.value = false
  }
}

async function handlePay() {
  paying.value = true
  try {
    if (isGroupOrder.value) {
      // 团体订单支付
      const g = groupOrder.value
      await ElMessageBox.confirm(
        `确认支付团体「${g.groupName}」的订单（共 ${g.totalCount} 人，合计 ¥${g.totalAmount}）？`,
        '确认支付',
        { confirmButtonText: '确认支付', cancelButtonText: '取消', type: 'warning' }
      )
      await confirmGroupPay(groupOrderId.value)
      ElMessage.success('支付成功！')
      // 重新加载以显示最新状态
      const data = await getGroupOrderDetail(groupOrderId.value)
      groupOrder.value = data
    } else {
      const actualPay = order.value.payAmount || order.value.totalAmount - (order.value.discountAmount || 0)
      const confirmText = order.value.discountAmount > 0
        ? `确认支付 ¥${actualPay}？（已优惠 ¥${order.value.discountAmount}）`
        : `确认支付 ¥${order.value.totalAmount}？`
      await ElMessageBox.confirm(confirmText, '确认支付', {
        confirmButtonText: '确认支付', cancelButtonText: '取消', type: 'warning',
      })
      await mockPay({ orderNo: order.value.orderNo, success: true })
      ElMessage.success('支付成功！')
      await loadOrder()
    }
  } catch (e) {
    if (e !== 'cancel') console.error('支付失败', e)
  } finally {
    paying.value = false
  }
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？取消后无法恢复。', '确认取消', {
      confirmButtonText: '确定取消', cancelButtonText: '再想想', type: 'warning',
    })
    await cancelOrder(order.value.orderNo)
    ElMessage.success('订单已取消')
    await loadOrder()
  } catch (e) {
    if (e !== 'cancel') console.error('取消订单失败', e)
  }
}

async function handleApplyRefund() {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入退款原因', '申请退款', {
      confirmButtonText: '提交', cancelButtonText: '取消',
      inputPlaceholder: '请填写退款原因',
      inputValidator: (val) => val && val.trim() ? true : '退款原因不能为空',
    })
    await applyRefund({ orderNo: order.value.orderNo, reason })
    ElMessage.success('退款申请已提交')
    router.push('/orders')
  } catch (e) {
    if (e !== 'cancel') console.error('申请退款失败', e)
  }
}

function copyOrderNo() {
  navigator.clipboard.writeText(order.value.orderNo).then(() => {
    ElMessage.success('订单编号已复制')
  }).catch(() => {
    ElMessage.info('复制失败，请手动复制')
  })
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

onMounted(() => {
  loadOrder()
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
.pay-page { margin: -24px; }

/* ====== 页面头部 ====== */
.page-hero {
  padding: 64px 24px 50px; text-align: center;
}
.hero-icon-wrap {
  width: 80px; height: 80px; margin: 0 auto 20px;
  background: rgba(255,255,255,0.2); border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
}
.hero-emoji { font-size: 40px; }
.hero-title { font-size: clamp(26px, 4vw, 34px); font-weight: 800; color: #fff; margin: 0 0 8px; }
.hero-subtitle { color: rgba(255,255,255,0.85); font-size: 15px; margin: 0 0 28px; }
.hero-actions { display: flex; gap: 12px; justify-content: center; flex-wrap: wrap; }
.hero-btn {
  border: none; border-radius: 14px; padding: 14px 32px;
  font-size: 16px; font-weight: 700; cursor: pointer;
  transition: all 0.25s; white-space: nowrap;
}
.hero-btn--pay { background: #fff; color: #1a1a2e; }
.hero-btn--pay:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.15); }
.hero-btn--pay:disabled { opacity: 0.6; cursor: not-allowed; }
.hero-btn--ghost { background: rgba(255,255,255,0.15); color: #fff; backdrop-filter: blur(4px); }
.hero-btn--ghost:hover { background: rgba(255,255,255,0.25); }
.hero-btn--refund { background: #fff; color: #F56C6C; }
.hero-btn--refund:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.12); }
.btn-inner { display: flex; align-items: center; gap: 6px; }

/* ====== 内容区 ====== */
.content-wrapper { background: transparent; padding: 0 24px 80px; }
.section { max-width: 860px; margin: 0 auto; padding-top: 40px; }

/* 入场动画 */
[v-observe] { opacity: 0; transform: translateY(30px); transition: opacity 0.7s ease, transform 0.7s cubic-bezier(0.22, 0.61, 0.36, 1); }
[v-observe].visible { opacity: 1; transform: translateY(0); }

/* ====== 详情卡片 ====== */
.detail-card {
  background: rgba(255,255,255,0.85); backdrop-filter: blur(10px);
  border-radius: 20px; overflow: hidden;
  box-shadow: var(--shadow-sm); transition: all var(--transition);
}
.detail-card:hover { box-shadow: var(--shadow-md); }
.card-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 22px 32px; border-bottom: 1px solid #f1f5f9;
}
.card-header h3 { font-size: 18px; font-weight: 700; color: #1a1a2e; margin: 0; }
.card-body { padding: 28px 32px; }

/* 状态标签 */
.card-status {
  font-size: 12px; font-weight: 700; padding: 4px 14px; border-radius: 20px;
}
.status--warning { background: #fdf6ec; color: #E6A23C; }
.status--success { background: #f0f9eb; color: #67C23A; }
.status--info    { background: #f4f4f5; color: #909399; }
.status--danger  { background: #fef0f0; color: #F56C6C; }

/* ====== 信息网格 ====== */
.info-grid {
  display: grid; grid-template-columns: repeat(2, 1fr);
  gap: 0; border: 1px solid #f1f5f9; border-radius: 12px; overflow: hidden;
}
.info-item {
  display: flex; flex-direction: column; gap: 4px;
  padding: 16px 20px; border-bottom: 1px solid #f1f5f9;
}
.info-item:nth-child(odd) { border-right: 1px solid #f1f5f9; }
.info-item:nth-last-child(-n+2) { border-bottom: none; }
.info-label { font-size: 12px; color: #94a3b8; font-weight: 600; text-transform: uppercase; }
.info-value { font-size: 15px; color: #1a1a2e; font-weight: 500; display: flex; align-items: center; gap: 8px; }
.info-value.price { color: #E6A23C; font-weight: 800; font-size: 17px; }
.order-no {
  font-family: 'SF Mono', monospace; font-size: 13px;
  color: v-bind('scenicStore.primaryColor'); background: rgba(0,0,0,0.03);
  padding: 4px 10px; border-radius: 6px;
}
.copy-btn {
  border: none; background: none; color: #409EFF; font-size: 12px;
  cursor: pointer; white-space: nowrap; padding: 2px 6px;
  border-radius: 4px; transition: background 0.2s;
}
.copy-btn:hover { background: #ecf5ff; }

/* ====== 票种明细 ====== */
.items-section { margin-top: 28px; }
.items-section h4 { font-size: 15px; font-weight: 700; color: #1a1a2e; margin: 0 0 12px; }
.modern-table {
  --el-table-border-color: #f1f5f9;
  --el-table-header-bg-color: #f8fafc;
  --el-table-row-hover-bg-color: #f8fafc;
  border-radius: 10px; overflow: hidden;
}

/* ====== 支付方式 ====== */
.pay-section { margin-top: 28px; }
.section-title {
  font-size: 15px; font-weight: 700; color: #1a1a2e;
  margin-bottom: 16px; padding-bottom: 10px;
  border-bottom: 2px solid #f1f5f9;
}
.pay-methods { display: flex; gap: 16px; flex-wrap: wrap; }
.pay-method {
  flex: 1; min-width: 120px; padding: 20px 16px;
  border: 2px solid #e4e7ed; border-radius: 14px;
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  cursor: pointer; transition: all 0.3s; background: #fff;
}
.pay-method:hover:not(.disabled) {
  border-color: v-bind('scenicStore.primaryColor');
  transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.08);
}
.pay-method.active {
  border-color: v-bind('scenicStore.primaryColor');
  background: v-bind('primaryColor04');
}
.pay-method.disabled { opacity: 0.4; cursor: not-allowed; }
.pay-icon {
  width: 48px; height: 48px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.pay-icon :deep(svg) { display: block; }
.pay-icon--mock { color: v-bind('scenicStore.primaryColor'); }
.pay-name { font-size: 14px; font-weight: 600; color: #1a1a2e; }

/* ====== 人脸录入 ====== */
.face-tip { color: #64748b; font-size: 14px; margin: 0 0 20px; }
.ticket-face-list { display: flex; flex-direction: column; gap: 12px; }
.ticket-face-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border: 1px solid #f1f5f9; border-radius: 14px;
  background: #fafafa; transition: all 0.3s;
}
.ticket-face-item:hover { border-color: #e4e7ed; box-shadow: var(--shadow-sm); }
.ticket-face-item.has-face {
  border-color: #b3e19d; background: #f6fdf2;
}
.ticket-info { display: flex; flex-direction: column; gap: 4px; }
.ticket-name { font-size: 15px; font-weight: 600; color: #1a1a2e; }
.ticket-price { font-size: 13px; color: #E6A23C; font-weight: 700; }
.ticket-user-info { display: flex; gap: 6px; margin-top: 4px; }
.ticket-user-form { display: flex; gap: 8px; margin-top: 6px; }
.user-tag {
  font-size: 12px; background: #f0f0f0; color: #606266;
  padding: 2px 10px; border-radius: 10px;
}
.required-input :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #e6a23c inset !important;
}
.ticket-face-action { display: flex; align-items: center; gap: 12px; }
.face-placeholder {
  display: flex; flex-direction: column; align-items: center; gap: 2px; width: 60px;
}
.face-ph-icon { font-size: 24px; }
.face-ph-text { font-size: 11px; color: #c0c4cc; }
.face-tag { font-size: 12px; font-weight: 600; padding: 3px 12px; border-radius: 10px; }
.face-tag--ok { background: #f0f9eb; color: #67C23A; }
.face-btn {
  border: none; border-radius: 10px; padding: 8px 18px;
  background: v-bind('scenicStore.primaryColor'); color: #fff;
  font-size: 13px; font-weight: 600; cursor: pointer;
  transition: all 0.2s;
}
.face-btn:hover { filter: brightness(1.1); }
.face-re-btn {
  border: none; background: none; color: #409EFF;
  font-size: 13px; cursor: pointer; padding: 4px 8px; border-radius: 6px;
}
.face-re-btn:hover { background: #ecf5ff; }
.face-complete {
  margin-top: 20px; padding: 14px 20px; border-radius: 12px;
  background: #f0f9eb; color: #67C23A; font-size: 14px; font-weight: 600;
}

/* ====== 底部操作 ====== */
.bottom-section { padding-top: 48px; }
.bottom-actions { display: flex; justify-content: center; gap: 12px; flex-wrap: wrap; }
.bottom-btn {
  border: none; border-radius: 12px; padding: 14px 28px;
  font-size: 15px; font-weight: 600; cursor: pointer;
  transition: all 0.25s;
}
.bottom-btn--outline {
  background: rgba(255,255,255,0.8); color: #1a1a2e;
  border: 1.5px solid #e4e7ed;
}
.bottom-btn--outline:hover { border-color: v-bind('scenicStore.primaryColor'); color: v-bind('scenicStore.primaryColor'); }
.bottom-btn--primary {
  background: v-bind('scenicStore.primaryColor'); color: #fff;
}
.bottom-btn--primary:hover { filter: brightness(1.1); transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.12); }

/* ====== 人脸弹窗 ====== */
.face-dialog-content { padding: 0 12px; }
.face-dialog-content.center {
  display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px 12px;
}
.dialog-tip { color: #606266; font-size: 14px; text-align: center; margin-bottom: 12px; }
.dialog-order-info {
  background: #f5f7fa; padding: 8px 16px; border-radius: 8px;
  text-align: center; color: #909399; font-size: 13px; margin-bottom: 20px;
}
.face-preview-box {
  width: 100%; height: 260px; border-radius: 12px; overflow: hidden;
  background: #f5f7fa; display: flex; align-items: center; justify-content: center; margin-bottom: 20px;
}
.face-preview-img { width: 100%; height: 100%; object-fit: cover; }
.face-preview-placeholder {
  display: flex; flex-direction: column; align-items: center; gap: 8px; color: #c0c4cc;
}
.face-preview-box.camera-box { background: #000; }
.camera-video { width: 100%; height: 100%; object-fit: cover; }
.dialog-actions { display: flex; justify-content: center; gap: 16px; margin-bottom: 16px; }

/* ====== 响应式 ====== */
@media (max-width: 768px) {
  .info-grid { grid-template-columns: 1fr; }
  .info-item:nth-child(odd) { border-right: none; }
  .info-item:nth-last-child(-n+2) { border-bottom: 1px solid #f1f5f9; }
  .info-item:last-child { border-bottom: none; }
  .pay-methods { flex-direction: column; }
  .ticket-face-item { flex-direction: column; align-items: flex-start; gap: 12px; }
  .card-body { padding: 20px; }
  .card-header { padding: 16px 20px; }
}

/* ====== 领取优惠券 ====== */
.coupon-receive-btn {
  border: none; border-radius: 12px; padding: 12px 24px;
  background: v-bind('scenicStore.primaryColor'); color: #fff;
  font-size: 14px; font-weight: 600; cursor: pointer;
  transition: all 0.3s;
}
.coupon-receive-btn:hover { filter: brightness(1.15); transform: translateY(-2px); }
.coupon-used-name {
  font-size: 12px; color: #67C23A; background: #f0f9eb;
  padding: 2px 10px; border-radius: 10px; font-weight: 600;
}
.coupon-cancel-btn {
  border: 1px solid #f56c6c; border-radius: 6px;
  background: #fff; color: #f56c6c; font-size: 12px;
  padding: 3px 10px; cursor: pointer;
  transition: all 0.2s;
}
.coupon-cancel-btn:hover { background: #fef0f0; }
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
.my-coupon-used-btn {
  background: #e8f5e9 !important; color: #67C23A !important; cursor: default !important;
}
.my-coupon-del-btn {
  background: #fff !important; color: #f56c6c !important;
  border: 1px solid #f56c6c !important;
}
.my-coupon-del-btn:hover:not(:disabled) { background: #fef0f0 !important; filter: none !important; }
</style>
