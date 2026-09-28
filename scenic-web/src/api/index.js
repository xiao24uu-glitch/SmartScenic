import request from './request'

// ==================== 认证 ====================
export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const refreshToken = (refreshToken) => request.post('/auth/refresh', { refreshToken })
export const switchRole = (data) => request.post('/auth/switch-role', data)
export const getProfile = () => request.get('/auth/profile')
export const updateProfile = (data) => request.put('/auth/profile', data)
export const changePassword = (data) => request.put('/auth/password', data)
export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/auth/avatar/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export const uploadAvatarTemp = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/auth/avatar/upload-temp', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ==================== 公共 ====================
export const getScenicInfo = () => request.get('/public/scenic-info')
export const getTicketTypes = () => request.get('/public/ticket-types')
export const getSpots = () => request.get('/public/spots')
export const getFacilities = () => request.get('/public/facilities')
export const getAiStats = () => request.get('/public/ai-stats')
export const getPublicAnnouncements = () => request.get('/public/announcements')
export const getCrowdHeatmap = () => request.get('/public/crowd-heatmap')

// ==================== 配置 ====================
export const getConfig = () => request.get('/config')

// ==================== 订单 ====================
export const createOrder = (data) => request.post('/orders', data)
export const getOrderDetail = (orderNo) => request.get(`/orders/${orderNo}`)
export const getUserOrders = (params) => request.get('/orders/list', { params })
export const mockPay = (data) => request.post('/orders/pay', data)
export const cancelOrder = (orderNo) => request.put(`/orders/${orderNo}/cancel`)
export const removeOrderCoupon = (orderNo) => request.delete(`/orders/${orderNo}/coupon`)
export const useOrderCoupon = (orderNo, userCouponId) => request.put(`/orders/${orderNo}/coupon`, null, { params: { userCouponId } })
export const deleteOrder = (orderNo) => request.delete(`/orders/${orderNo}`)
export const applyRefund = (data) => request.post('/orders/refund', data)
export const requestModifyOrder = (orderNo, visitDate) => request.put(`/orders/${orderNo}/modify`, null, { params: { visitDate } })
export const getRefundList = (params) => request.get('/orders/refund/list', { params })

// ==================== 人脸 ====================
export const registerFace = (data) => request.post('/face/register', data)
export const faceSearch = (data) => request.post('/face/search', data)
export const getMyFace = () => request.get('/face/my-face')
export const deleteMyFace = () => request.delete('/face/my-face')
export const getOrderFaces = (orderNo) => request.get('/face/order-faces', { params: { orderNo } })
export const compareFaces = (data) => request.post('/face/compare', data)
export const cleanExpiredFaces = () => request.post('/face/clean')
export const reRegisterMemberFace = (data) => request.post('/face/re-register-member', data)

// ==================== 团体 ====================
export const downloadTemplate = () => request.get('/group/template', { responseType: 'blob' })
export const importGroup = (formData) => request.post('/group/import', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const getGroupOrders = (params) => request.get('/group/orders', { params })
export const getMyGroupOrders = (params) => request.get('/group/my-orders', { params })
export const getGroupOrderDetail = (id) => request.get(`/group/orders/${id}`)
export const auditGroupOrder = (data) => request.post('/group/audit', data)
export const confirmGroupPay = (id) => request.post(`/group/pay/${id}`)
export const updateGroupOrder = (id, data) => request.put(`/group/orders/${id}`, null, { params: data })
export const deleteGroupOrder = (id) => request.delete(`/group/orders/${id}`)
export const deleteGroupOrders = (ids) => request.delete('/group/orders/batch', { data: ids })
export const deleteGroupMember = (id) => request.delete(`/group/members/${id}`)
export const getGroupFaceProgress = (groupOrderId) => request.get(`/group/face-register-progress/${groupOrderId}`)

// ==================== 后台管理 ====================
export const getDashboard = () => request.get('/admin/dashboard')
export const exportReport = () => request.get('/admin/reports/export', { responseType: 'blob' })
export const getAdminOrders = (params) => request.get('/admin/orders', { params })
export const updateAdminOrder = (id, data) => request.put(`/admin/orders/${id}`, data)
export const deleteAdminOrder = (id) => request.delete(`/admin/orders/${id}`)
export const deleteAdminOrders = (ids) => request.delete('/admin/orders/batch', { data: ids })
export const auditOrderModify = (id, approved) => request.post(`/admin/orders/${id}/audit-modify`, null, { params: { approved } })
export const getAdminRefunds = (params) => request.get('/admin/refunds', { params })
export const auditRefund = (data) => request.post('/admin/refunds/audit', data)
export const deleteAdminRefund = (id) => request.delete(`/admin/refunds/${id}`)
export const deleteAdminRefunds = (ids) => request.delete('/admin/refunds/batch', { data: ids })
export const getEntryLogs = (params) => request.get('/admin/entry-logs', { params })
export const deleteEntryLog = (id) => request.delete(`/admin/entry-logs/${id}`)
export const deleteEntryLogs = (ids) => request.delete('/admin/entry-logs/batch', { data: ids })
export const getFaceData = (params) => request.get('/admin/face-data', { params })
export const deleteFaceData = (id) => request.delete(`/admin/face-data/${id}`)
export const deleteFaceDatas = (ids) => request.delete('/admin/face-data/batch', { data: ids })
export const getUsers = (params) => request.get('/admin/users', { params })
export const createUser = (data) => request.post('/admin/users', data)
export const updateUser = (id, data) => request.put(`/admin/users/${id}`, data)
export const deleteUsers = (ids) => request.delete('/admin/users/batch', { data: ids })
export const updateUserRoles = (id, roleIds) => request.put(`/admin/users/${id}/roles`, roleIds)
export const updateUserStatus = (id, status) => request.put(`/admin/users/${id}/status`, { status })
export const getRoles = () => request.get('/admin/roles')
export const getAdminConfigs = (params) => request.get('/admin/configs', { params })
export const updateConfig = (data) => request.put('/admin/configs', data)
export const getAdminSpots = () => request.get('/admin/spots')
export const saveSpot = (data) => request.post('/admin/spots', data)
export const deleteSpot = (id) => request.delete(`/admin/spots/${id}`)
export const deleteSpots = (ids) => request.delete('/admin/spots/batch', { data: ids })
export const updateSpotSort = (spots) => request.put('/admin/spots/sort', spots)
export const getAdminFacilities = () => request.get('/admin/facilities')
export const saveFacility = (data) => request.post('/admin/facilities', data)
export const deleteFacility = (id) => request.delete(`/admin/facilities/${id}`)
export const deleteFacilities = (ids) => request.delete('/admin/facilities/batch', { data: ids })
export const uploadImage = (file, category = 'common') => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/admin/upload/image?category=${category}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export const getAiConversations = (params) => request.get('/admin/ai-conversations', { params })
export const deleteAiConversation = (id) => request.delete(`/admin/ai-conversations/${id}`)
export const deleteAiConversations = (ids) => request.delete('/admin/ai-conversations/batch', { data: ids })
export const getOperLogs = (params) => request.get('/admin/oper-logs', { params })
export const deleteOperLogs = (ids) => request.delete('/admin/oper-logs/batch', { data: ids })
export const getAdminTicketTypes = () => request.get('/admin/ticket-types')
export const getAdminScenic = () => request.get('/admin/scenic')
export const updateAdminScenic = (data) => request.put('/admin/scenic', data)
export const createTicketType = (data) => request.post('/admin/ticket-types', data)
export const updateTicketType = (id, data) => request.put(`/admin/ticket-types/${id}`, data)
export const deleteTicketType = (id) => request.delete(`/admin/ticket-types/${id}`)
export const deleteTicketTypes = (ids) => request.delete('/admin/ticket-types/batch', { data: ids })

// ==================== AI助手 ====================
export const aiChat = (data) => {
  const token = sessionStorage.getItem('token')
  return fetch('/api/v1/ai/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify(data),
  })
}
export const aiTravelogue = (data) => {
  const token = sessionStorage.getItem('token')
  return fetch('/api/v1/ai/travelogue', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify(data),
  })
}
export const downloadTravelogueFile = (data) => {
  const token = sessionStorage.getItem('token')
  return fetch('/api/v1/ai/travelogue/download', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify(data),
  })
}
export const submitAiFeedback = (data) => request.post('/ai/feedback', data)

// ==================== 闸机 ====================
export const getGates = () => request.get('/gates')
export const getAdminGates = () => request.get('/admin/gates')
export const saveGate = (data) => request.post('/admin/gates', data)
export const deleteGate = (id) => request.delete(`/admin/gates/${id}`)
export const deleteGates = (ids) => request.delete('/admin/gates/batch', { data: ids })

// ==================== 优惠券 ====================
export const getAdminCoupons = (params) => request.get('/coupons', { params })
export const createCoupon = (data) => request.post('/coupons', data)
export const updateCoupon = (id, data) => request.put(`/coupons/${id}`, data)
export const toggleCoupon = (id) => request.put(`/coupons/${id}/toggle`)
export const deleteCoupons = (ids) => request.delete('/coupons/batch', { data: ids })
export const batchToggleCoupons = (ids, status) => request.put('/coupons/batch/toggle', ids, { params: { status } })
export const receiveCoupon = (couponId) => request.post(`/coupons/${couponId}/receive`)
export const getAvailableCoupons = (orderAmount) => request.get('/coupons/available', { params: { orderAmount } })
export const getAvailableTemplates = () => request.get('/coupons/available-templates')
export const deleteCoupon = (id) => request.delete(`/coupons/${id}`)
export const clearCouponReceivedCount = (id) => request.put(`/coupons/${id}/clear-received`)
export const deleteMyCoupon = (id) => request.delete(`/coupons/user/${id}`)
export const getMyCoupons = (params) => request.get('/coupons/my', { params })

// ==================== 检票员 ====================
export const verifyFace = (data) => request.post('/checker/verify-face', data)
export const manualEntry = (data) => request.post('/checker/manual-entry', data)
export const manualExit = (data) => request.post('/checker/manual-exit', data)
export const getCheckerEntries = (params) => request.get('/checker/entries', { params })
export const deleteCheckerEntries = (ids) => request.delete('/checker/entries/batch', { data: ids })
export const getValidOrders = (params) => request.get('/checker/valid-orders', { params })
export const getCheckerDashboard = () => request.get('/checker/dashboard')

// ==================== 纪念票 ====================
export const getSouvenirTicket = (orderNo) => request.get(`/admin/orders/${orderNo}/souvenir-ticket`)

// ==================== 公告 ====================
export const getAdminAnnouncements = (params) => request.get('/admin/announcements', { params })
export const createAnnouncement = (data) => request.post('/admin/announcements', data)
export const updateAnnouncement = (id, data) => request.put(`/admin/announcements/${id}`, data)
export const deleteAnnouncement = (id) => request.delete(`/admin/announcements/${id}`)
export const deleteAnnouncements = (ids) => request.delete('/admin/announcements/batch', { data: ids })

// ==================== 客流热力图 ====================
export const getAdminCrowdHeatmap = () => request.get('/admin/crowd-heatmap')
