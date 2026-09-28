import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, register as registerApi, switchRole as switchRoleApi } from '../api'

export const useUserStore = defineStore('user', () => {
  const token = ref(sessionStorage.getItem('token') || '')
  const refreshToken = ref(sessionStorage.getItem('refreshToken') || '')
  const userInfo = ref(JSON.parse(sessionStorage.getItem('userInfo') || 'null'))

  async function login(data) {
    const res = await loginApi(data)
    token.value = res.token
    refreshToken.value = res.refreshToken
    userInfo.value = {
      userId: res.userId,
      username: res.username,
      realName: res.realName,
      phone: res.phone,
      email: res.email,
      avatarUrl: res.avatarUrl,
      roleName: res.roleName,
      roleCode: res.roleCode,
      roleLevel: res.roleLevel,
      currentRoleId: res.currentRoleId,
      availableRoles: res.availableRoles || [],
    }
    sessionStorage.setItem('token', res.token)
    sessionStorage.setItem('refreshToken', res.refreshToken)
    sessionStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    return res
  }

  async function register(data) {
    await registerApi(data)
  }

  /** 切换角色 */
  async function switchRole(role) {
    const res = await switchRoleApi({ roleId: role.roleId })
    token.value = res.token
    refreshToken.value = res.refreshToken
    userInfo.value = {
      userId: res.userId,
      username: res.username,
      realName: res.realName,
      phone: res.phone,
      email: res.email,
      avatarUrl: res.avatarUrl,
      roleName: res.roleName,
      roleCode: res.roleCode,
      roleLevel: res.roleLevel,
      currentRoleId: res.currentRoleId,
      availableRoles: res.availableRoles || [],
    }
    sessionStorage.setItem('token', res.token)
    sessionStorage.setItem('refreshToken', res.refreshToken)
    sessionStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    return res
  }

  function updateUserInfo(info) {
    userInfo.value = { ...userInfo.value, ...info }
    sessionStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  function logout() {
    // 清除AI对话记录（仅清前端显示，不删后端数据）
    const uid = userInfo.value?.userId
    if (uid) sessionStorage.removeItem('ai_messages_' + uid)
    sessionStorage.removeItem('ai_messages_guest')
    sessionStorage.removeItem('ai_session_id')

    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('refreshToken')
    sessionStorage.removeItem('userInfo')
  }

  function isAdmin() {
    return userInfo.value?.roleCode === 'ADMIN'
  }

  function isManager() {
    return userInfo.value?.roleCode === 'MANAGER' || userInfo.value?.roleCode === 'ADMIN'
  }

  function isChecker() {
    return userInfo.value?.roleCode === 'CHECKER'
  }

  return {
    token,
    refreshToken,
    userInfo,
    login,
    register,
    switchRole,
    updateUserInfo,
    logout,
    isAdmin,
    isManager,
    isChecker,
  }
})
