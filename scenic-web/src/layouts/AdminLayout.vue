<template>
  <div class="admin-layout">
    <el-container>
      <el-aside width="220px" class="sidebar">
        <div class="logo-area">
          <h2>{{ scenicStore.scenicName }}</h2>
          <span class="subtitle">{{ roleTitle }}</span>
        </div>
        <el-menu :default-active="activeMenu" router :background-color="sidebarBg" :text-color="sidebarTextColor" :active-text-color="menuActiveColor" class="sidebar-menu">
          <template v-for="item in visibleMenus" :key="item.path">
            <el-menu-item :index="item.path" :data-id="item.id">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </el-menu-item>
          </template>
          <!-- 拖拽排序入口（管理员及以上可见） -->
          <div v-if="isManagerOrAbove && !sortMode" class="sort-entry" @click="sortMode = true">
            <i class="el-icon-sort"></i> 排序菜单
          </div>
          <div v-if="sortMode" class="sort-actions">
            <el-button size="small" type="success" @click="saveSortOrder">保存顺序</el-button>
            <el-button size="small" @click="cancelSort">取消</el-button>
          </div>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="admin-header">
          <div class="header-left">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/admin' }">后台管理</el-breadcrumb-item>
              <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <!-- 角色切换 -->
            <el-dropdown v-if="availableRoles.length > 1" @command="handleSwitchRole">
              <span class="role-switch-btn">
                <el-tag :type="roleTagType" size="small">{{ userStore.userInfo?.roleName }}</el-tag>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-item
                  v-for="role in availableRoles"
                  :key="role.roleId"
                  :command="role"
                  :class="{ 'is-active': role.roleId === userStore.userInfo?.currentRoleId }"
                >
                  <el-icon v-if="role.roleId === userStore.userInfo?.currentRoleId"><Select /></el-icon>
                  {{ role.roleName }}
                </el-dropdown-item>
              </template>
            </el-dropdown>
            <el-tag v-else :type="roleTagType" size="small">{{ userStore.userInfo?.roleName }}</el-tag>

            <el-button @click="$router.push('/')" size="small">返回前台</el-button>
            <el-dropdown>
              <span class="user-name">
                <el-avatar :size="28" :src="userStore.userInfo?.avatarUrl || ''" class="header-avatar">
                  {{ (userStore.userInfo?.realName || '管')[0] }}
                </el-avatar>
                {{ userStore.userInfo?.realName || '管理员' }}
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </template>
            </el-dropdown>
          </div>
        </el-header>
        <el-main class="admin-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import
 { ref, computed, onMounted, nextTick, watch } from 'vue'
import
 { useRoute, useRouter } from 'vue-router'
import
 { useScenicStore } from '../stores/scenic'
import
 { useUserStore } from '../stores/user'
import
 { ElMessage } from 'element-plus'
import
 {
  DataAnalysis, Ticket, Document, UserFilled, Money,
  Finished, PictureFilled, Monitor, TrendCharts,
  Location, OfficeBuilding, SetUp, ChatDotRound,
  User, Setting, Notebook, Rank, PriceTag, Bell
} from '@element-plus/icons-vue'
import
 Sortable from 'sortablejs'

const route = useRoute()
const router = useRouter()
const scenicStore = useScenicStore()
const userStore = useUserStore()

// 角色判断
const isAdmin = computed(() => userStore.userInfo?.roleCode === 'ADMIN')
const isManager = computed(() => userStore.userInfo?.roleCode === 'MANAGER')
const isChecker = computed(() => userStore.userInfo?.roleCode === 'CHECKER')
const isManagerOrAbove = computed(() => isAdmin.value || isManager.value)

// 主题色（侧边栏采用主题色，菜单激活色使用亮色强调）
const sidebarBg = computed(() => scenicStore.primaryColor || '#304156')
const sidebarTextColor = computed(() => '#bfcbd9')
const menuActiveColor = computed(() => '#ffd04b')

// 可切换的角色列表
const availableRoles = computed(() => userStore.userInfo?.availableRoles || [])

// 菜单定义（完整列表，含角色权限）
let _menuId = 0
function mk(items) {
  return items.map(m => ({ ...m, id: ++_menuId }))
}

const allMenusBase = mk([
  // 检票员菜单
  { path: '/admin/checker-dashboard', label: '检票工作台', icon: DataAnalysis, role: 'checker' },
  { path: '/admin/entries', label: '入园记录', icon: Finished, role: 'checker' },
  // 管理员菜单
  { path: '/admin/dashboard', label: '仪表盘', icon: DataAnalysis, role: 'manager' },
  { path: '/admin/tickets', label: '票务管理', icon: Ticket, role: 'manager' },
  { path: '/admin/orders', label: '订单管理', icon: Document, role: 'manager' },
  { path: '/admin/groups', label: '团体票管理', icon: UserFilled, role: 'manager' },
  { path: '/admin/refunds', label: '退款审核', icon: Money, role: 'manager' },
  { path: '/admin/entries', label: '检票记录', icon: Finished, role: 'manager' },
  { path: '/admin/faces', label: '人脸库管理', icon: PictureFilled, role: 'manager' },
  { path: '/admin/monitor', label: '客流监控', icon: Monitor, role: 'manager' },
  { path: '/admin/reports', label: '财务报表', icon: TrendCharts, role: 'manager' },
  { path: '/admin/spots', label: '景点管理', icon: Location, role: 'manager' },
  { path: '/admin/facilities', label: '设施管理', icon: OfficeBuilding, role: 'manager' },
  { path: '/admin/gates', label: '闸机管理', icon: SetUp, role: 'manager' },
  { path: '/admin/coupons', label: '优惠券管理', icon: PriceTag, role: 'manager' },
  { path: '/admin/announcements', label: '公告管理', icon: Bell, role: 'manager' },
  { path: '/admin/ai-logs', label: 'AI对话记录', icon: ChatDotRound, role: 'manager' },
  // 超级管理员专属
  { path: '/admin/users', label: '用户管理', icon: User, role: 'admin' },
  { path: '/admin/config', label: '系统配置', icon: Setting, role: 'admin' },
  { path: '/admin/oper-logs', label: '操作日志', icon: Notebook, role: 'admin' },
])

// 从 localStorage 恢复排序顺序
function loadSavedOrder() {
  try {
    const saved = JSON.parse(localStorage.getItem('adminMenuOrder') || '[]')
    if (!saved.length) return allMenusBase.slice()
    // 按 saved 顺序排列，未在 saved 中的追加到末尾
    const map = new Map(allMenusBase.map(m => [m.id, m]))
    const ordered = []
    for (const id of saved) {
      if (map.has(id)) { ordered.push(map.get(id)); map.delete(id) }
    }
    ordered.push(...map.values())
    return ordered
  } catch {
    return allMenusBase.slice()
  }
}
const menus = ref(loadSavedOrder())

// 根据角色过滤可见菜单
const visibleMenus = computed(() => {
  if (isChecker.value) return menus.value.filter(m => m.role === 'checker')
  const base = menus.value.filter(m => m.role === 'manager')
  if (isAdmin.value) {
    const adminOnly = menus.value.filter(m => m.role === 'admin')
    return [...base, ...adminOnly]
  }
  return base
})

// 拖拽排序
const sortMode = ref(false)
let sortableInstance = null

function initSortable() {
  nextTick(() => {
    const el = document.querySelector('.sidebar-menu > .el-scrollbar') || document.querySelector('.sidebar-menu')
    if (!el) return
    const ul = el.querySelector('.el-menu') || el
    if (sortableInstance) { sortableInstance.destroy(); sortableInstance = null }
    sortableInstance = new Sortable(ul, {
      handle: '.el-menu-item',
      animation: 200,
      ghostClass: 'ghost-menu-item',
      disabled: !sortMode.value,
      onEnd({ oldIndex, newIndex }) {
        if (oldIndex === newIndex) return
        // 在 filtered 视图中移动，需要映射回 menus 数组
        const item = visibleMenus.value[oldIndex]
        const realIndex = menus.value.findIndex(m => m.id === item.id)
        const targetItem = visibleMenus.value[newIndex]
        const targetRealIndex = menus.value.findIndex(m => m.id === targetItem.id)
        menus.value.splice(realIndex, 1)
        menus.value.splice(targetRealIndex, 0, item)
      },
    })
  })
}

watch(sortMode, () => {
  if (sortableInstance) sortableInstance.option('disabled', !sortMode.value)
})

function saveSortOrder() {
  localStorage.setItem('adminMenuOrder', JSON.stringify(menus.value.map(m => m.id)))
  ElMessage.success('菜单顺序已保存')
  sortMode.value = false
}

function cancelSort() {
  menus.value = loadSavedOrder()
  sortMode.value = false
}

const roleTagType = computed(() => {
  const map = { ADMIN: 'danger', MANAGER: 'warning', CHECKER: 'success', TOURIST: 'info' }
  return map[userStore.userInfo?.roleCode] || 'info'
})

const roleTitle = computed(() => {
  const map = { ADMIN: '超级管理员', MANAGER: '景区管理后台', CHECKER: '检票员工作台', TOURIST: '游客中心' }
  return map[userStore.userInfo?.roleCode] || '后台管理'
})

const activeMenu = computed(() => route.path)

const pageTitle = computed(() => {
  const map = {
    '/admin/dashboard': '仪表盘',
    '/admin/checker-dashboard': '检票工作台',
    '/admin/tickets': '票务管理',
    '/admin/orders': '订单管理',
    '/admin/groups': '团体票管理',
    '/admin/refunds': '退款审核',
    '/admin/entries': '入园记录',
    '/admin/faces': '人脸库管理',
    '/admin/monitor': '客流监控',
    '/admin/reports': '财务报表',
    '/admin/spots': '景点管理',
    '/admin/facilities': '设施管理',
    '/admin/gates': '闸机管理',
    '/admin/coupons': '优惠券管理',
    '/admin/announcements': '公告管理',
    '/admin/users': '用户管理',
    '/admin/config': '系统配置',
    '/admin/ai-logs': 'AI对话记录',
    '/admin/oper-logs': '操作日志',
  }
  return map[route.path] || ''
})

async function handleSwitchRole(role) {
  if (role.roleId === userStore.userInfo?.currentRoleId) return
  try {
    await userStore.switchRole(role)
    ElMessage.success(`已切换到「${role.roleName}」角色`)
    // 根据角色跳转到对应首页
    if (role.roleCode === 'CHECKER') {
      router.push('/admin/checker-dashboard')
    } else {
      router.push('/admin/dashboard')
    }
  } catch (e) {
    ElMessage.error('角色切换失败: ' + (e.message || '未知错误'))
  }
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

onMounted(() => initSortable())
</script>

<style scoped>
.admin-layout { height: 100vh; }
.sidebar { background-color: v-bind(sidebarBg); overflow-y: auto; }

.logo-area {
  padding: 20px; text-align: center;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.logo-area h2 { color: #fff; font-size: 16px; margin-bottom: 4px; }
.subtitle { color: #bfcbd9; font-size: 12px; }

.sidebar-menu { border-right: none; }

.sort-entry {
  text-align: center;
  color: #6b7a8f;
  font-size: 12px;
  padding: 10px 0;
  cursor: pointer;
  transition: color 0.2s;
}
.sort-entry:hover { color: #409EFF; }

.sort-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
  padding: 8px 16px 16px;
}

:deep(.ghost-menu-item) {
  opacity: 0.4;
  background-color: rgba(64, 158, 255, 0.15) !important;
}

.admin-header {
  background-color: #fff;
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
  height: 56px;
}

.header-right { display: flex; align-items: center; gap: 16px; }

.role-switch-btn {
  cursor: pointer; display


: flex; align-items: center; gap: 4px;
  padding: 2px 8px; border-radius: 4px;
  transition: background 0.2s;
}
.role-switch-btn:hover { background: #f0f2f5; }
.role-switch-btn { outline: none; }
.role-switch-btn:focus { outline: none; }

.is-active { color: #409EFF; font-weight: bold; }

.user-name { cursor: pointer; display: flex; align-items: center; gap: 6px; outline: none; }
.header-avatar { flex-shrink: 0; }
.user-name:focus { outline: none; }

.admin-main { background-color: #f0f2f5; padding: 24px; }
</style>
