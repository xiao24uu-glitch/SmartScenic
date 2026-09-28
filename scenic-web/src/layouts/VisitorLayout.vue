<template>
  <div class="visitor-layout">
    <el-container>
      <el-header class="header">
        <router-link to="/" class="header-left">
          <el-image v-if="scenicStore.scenicLogoUrl" :src="scenicStore.scenicLogoUrl" class="logo" />
          <h1 class="title">{{ scenicStore.scenicName }}门票销售及入场系统</h1>
        </router-link>
        <div class="header-right">
          <!-- 导航菜单（用普通链接，免得 el-menu 自动折叠到「...」里） -->
          <nav class="nav-links">
            <router-link to="/" :class="{ active: $route.path === '/' }">首页</router-link>
            <router-link to="/tickets" :class="{ active: $route.path.startsWith('/tickets') }">门票选购</router-link>
            <router-link to="/ai-assistant" :class="{ active: $route.path.startsWith('/ai') }">AI助手</router-link>
            <router-link to="/orders" :class="{ active: $route.path.startsWith('/orders') }">我的订单</router-link>
            <router-link to="/profile" :class="{ active: $route.path.startsWith('/profile') }">个人中心</router-link>
          </nav>

          <!-- 用户区域：统一的下拉（含切换角色、后台管理、退出登录） -->
          <div class="user-area">
            <el-dropdown v-if="userStore.userInfo" @command="handleUserCommand">
              <span class="user-name">
                <el-avatar :size="28" :src="userStore.userInfo?.avatarUrl || ''" class="header-avatar">
                  {{ (userStore.userInfo.realName || userStore.userInfo.username || '用')[0] }}
                </el-avatar>
                {{ userStore.userInfo.realName || userStore.userInfo.username }}
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <!-- 角色切换（多角色时出现） -->
                  <template v-if="userStore.userInfo.availableRoles?.length > 1">
                    <el-dropdown-item
                      v-for="role in userStore.userInfo.availableRoles"
                      :key="role.roleId"
                      :command="'switch_' + role.roleId"
                      :class="{ 'dropdown-active': role.roleId === userStore.userInfo.currentRoleId }"
                    >
                      <el-icon v-if="role.roleId === userStore.userInfo.currentRoleId"><Select /></el-icon>
                      切换至 {{ role.roleName }}
                    </el-dropdown-item>
                  </template>
                  <!-- 后台管理入口（管理员、景区管理员、检票员可见） -->
                  <el-dropdown-item
                    v-if="userStore.isManager() || userStore.isChecker()"
                    command="admin"
                    :divided="userStore.userInfo.availableRoles?.length > 1"
                  >后台管理</el-dropdown-item>
                  <!-- 退出登录 -->
                  <el-dropdown-item :divided="userStore.isManager() || userStore.isChecker() || (userStore.userInfo.availableRoles?.length > 1)" command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button v-else type="primary" size="small" @click="$router.push('/login')">登录</el-button>
          </div>
        </div>
      </el-header>
      <el-main class="main-content" :style="mainContentStyle">
        <router-view v-slot="{ Component }">
          <keep-alive :include="['AiChat', 'Home', 'Tickets', 'Orders', 'Profile', 'GroupPurchase', 'Pay']">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
      <el-footer class="footer">
        <p>© {{ currentYear }} {{ scenicStore.scenicName }} 版权所有 | AI科技赋能 相伴您的每一次游玩</p>
      </el-footer>
    </el-container>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useScenicStore } from '../stores/scenic'
import { useUserStore } from '../stores/user'
import { ArrowDown, Select } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import { computed } from 'vue'

const route = useRoute()
const router = useRouter()
const scenicStore = useScenicStore()
const userStore = useUserStore()

const currentYear = computed(() => new Date().getFullYear())

// 根据路由自动应用对应页面背景图
const mainContentStyle = computed(() => {
  const path = route.path
  let bg = ''
    if (path === '/' || path === '/home') bg = scenicStore.homeBgImage
    else if (path.startsWith('/tickets') || path.startsWith('/group-purchase')) bg = scenicStore.ticketsBgImage
    else if (path.startsWith('/ai-assistant')) bg = scenicStore.aiBgImage
    else if (path.startsWith('/orders')) bg = scenicStore.ordersBgImage
    else if (path.startsWith('/profile')) bg = scenicStore.profileBgImage
    else if (path.startsWith('/pay')) bg = scenicStore.ordersBgImage
    if (!bg) return { backgroundColor: '#f5f7fa' }
  return {
    backgroundImage: `url(${bg})`,
    backgroundSize: 'cover',
    backgroundPosition: 'center',
    backgroundAttachment: 'fixed',
  }
})

async function handleUserCommand(command) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/')
  } else if (command === 'admin') {
    router.push('/admin')
  } else if (command.startsWith('switch_')) {
    const roleId = Number(command.replace('switch_', ''))
    const role = userStore.userInfo?.availableRoles?.find(r => r.roleId === roleId)
    if (!role) return
    if (role.roleId === userStore.userInfo?.currentRoleId) return
    try {
      await userStore.switchRole(role)
      ElMessage.success(`已切换到「${role.roleName}」角色`)
      // 前台切换：留在前台页面
    } catch (e) {
      ElMessage.error('角色切换失败: ' + (e.message || '未知错误'))
    }
  }
}
</script>

<style scoped>
.visitor-layout {
  min-height: 100vh;
}

.header {
  background: v-bind('scenicStore.primaryColor');
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 64px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  cursor: pointer;
}

.logo {
  width: 36px;
  height: 36px;
}

.title {
  color: #fff;
  font-size: 18px;
  white-space: nowrap;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.nav-links {
  display: flex; align-items: center; gap: 8px;
}
.nav-links a {
  color: rgba(255,255,255,0.85);
  text-decoration: none;
  padding: 6px 14px;
  border-radius: 4px;
  font-size: 14px;
  border-bottom: 2px solid transparent;
  white-space: nowrap;
  transition: all 0.2s;
  cursor: pointer;
  pointer-events: auto;
}
.nav-links a:hover { color: #fff; background: rgba(255,255,255,0.1); }
.nav-links a.active {
  color: #ffd04b;
  border-bottom-color: #ffd04b;
}

.user-area {
  display: flex;
  align-items: center;
}

.user-name {
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  outline: none;
}
.user-name:focus { outline: none; }
.header-avatar { flex-shrink: 0; }

.dropdown-active { color: #ffd04b; font-weight: bold; }

.main-content {
  margin-top: 64px;
  min-height: calc(100vh - 124px);
  padding: 24px;
  overflow: visible;
}

.footer {
  text-align: center;
  color: #999;
  font-size: 13px;
  padding: 16px;
  background-color: #fff;
  border-top: 1px solid #eee;
}
</style>
