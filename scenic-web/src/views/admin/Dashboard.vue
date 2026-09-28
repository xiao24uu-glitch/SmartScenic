<template>
  <div class="dashboard">
    <!-- 欢迎区域 -->
    <div class="welcome-banner" :class="{ 'light-bg': isLightBg }" :style="{ background: welcomeBg }">
      <div class="welcome-left">
        <el-avatar :size="52" :src="userStore.userInfo?.avatarUrl || ''" :class="{ 'welcome-avatar': true, 'avatar-light': isLightBg }">
          {{ (userStore.userInfo?.realName || '管')[0] }}
        </el-avatar>
        <div class="welcome-text" :class="{ 'text-dark': isLightBg }">
          <div class="welcome-greeting">{{ greeting }}，{{ userStore.userInfo?.realName || '管理员' }} 👋</div>
          <div class="welcome-quote">{{ quote }}</div>
        </div>
      </div>
      <div class="welcome-date" :class="{ 'text-dark': isLightBg }">{{ currentDate }}</div>
    </div>

    <el-row :gutter="24" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #e6f7ff;">
            <el-icon :size="28" color="#1890ff"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboard.todayOrderCount || 0 }}</div>
            <div class="stat-label">今日订单数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #f6ffed;">
            <el-icon :size="28" color="#52c41a"><Money /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">¥{{ dashboard.todaySales || 0 }}</div>
            <div class="stat-label">今日销售额</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #e6f7ff;">
            <el-icon :size="28" color="#1890ff"><UserFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboard.todayEntryCount || 0 }}</div>
            <div class="stat-label">今日入园</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #fff7e6;">
            <el-icon :size="28" color="#E6A23C"><UserFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboard.todayExitCount || 0 }}</div>
            <div class="stat-label">今日出园</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: #fff1f0;">
            <el-icon :size="28" color="#f5222d"><TrendCharts /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboard.currentInPark || 0 }}</div>
            <div class="stat-label">当前在园人数</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="24" style="margin-top: 24px;">
      <el-col :span="12">
        <el-card>
          <template #header>近7日销售趋势</template>
          <v-chart :option="salesTrendOption" style="height: 300px;" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>票种销售占比</template>
          <v-chart :option="ticketTypeOption" style="height: 300px;" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="24" style="margin-top: 24px;">
      <el-col :span="24">
        <el-card>
          <template #header>今日时段入园分布</template>
          <v-chart :option="hourlyEntryOption" style="height: 300px;" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDashboard } from '../../api'
import { useUserStore } from '../../stores/user'
import { useScenicStore } from '../../stores/scenic'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, PieChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const userStore = useUserStore()
const scenicStore = useScenicStore()
const dashboard = ref({})

// 欢迎横幅背景色：使用主题色，默认白色
const welcomeBg = computed(() => scenicStore.primaryColor || '#ffffff')
const isLightBg = computed(() => !scenicStore.primaryColor || scenicStore.primaryColor === '#ffffff')

// 根据时段返回问候语
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

// 励志语录池
const quotes = [
  '不积跬步，无以至千里；不积小流，无以成江海。',
  '成功不是将来才有的，而是从决定去做的那一刻起，持续累积而成。',
  '每一天都是一个新的开始，深呼吸，从头再来。',
  '世上无难事，只要肯登攀。',
  '业精于勤，荒于嬉；行成于思，毁于随。',
  '千里之行，始于足下。',
  '细节决定成败，态度决定高度。',
  '简单的事情重复做，重复的事情用心做。',
  '用心服务每一位游客，让景区因你而更美。',
  '今天的努力，是明天惊喜的铺垫。',
  '志当存高远，路自脚下行。',
  '宝剑锋从磨砺出，梅花香自苦寒来。',
  '人生在勤，不索何获。',
  '路漫漫其修远兮，吾将上下而求索。',
  '长风破浪会有时，直挂云帆济沧海。',
  '天行健，君子以自强不息。',
  '博观而约取，厚积而薄发。',
  '山重水复疑无路，柳暗花明又一村。',
  '纸上得来终觉浅，绝知此事要躬行。',
  '莫愁前路无知己，天下谁人不识君。',
  '千淘万漉虽辛苦，吹尽狂沙始到金。',
  '海阔凭鱼跃，天高任鸟飞。',
  '不要等待机会，而要创造机会。',
  '把每一件简单的事做好就是不简单。',
  '行动是治愈恐惧的良药，而犹豫将不断滋养恐惧。',
  '所有的胜利，与征服自己的胜利比起来，都是微不足道。',
  '卓越不是一种行为，而是一种习惯。',
  '最困难的时候，就是离成功不远了。',
  '只有不断找寻机会的人，才会及时把握机会。',
  '每一个认真付出的日子，都值得被铭记。',
  '你的努力，终将成就无可替代的自己。',
]

// 当日随机选取一条语录
const quote = computed(() => quotes[new Date().getDate() % quotes.length])

// 当前日期
const currentDate = computed(() => {
  const now = new Date()
  const weekMap = ['日', '一', '二', '三', '四', '五', '六']
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 星期${weekMap[now.getDay()]}`
})

const salesTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: (dashboard.value.salesTrend || []).map(i => i.name) },
  yAxis: { type: 'value' },
  series: [{ data: (dashboard.value.salesTrend || []).map(i => i.value), type: 'line', smooth: true, areaStyle: {} }],
}))

const ticketTypeOption = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    data: (dashboard.value.ticketTypeDistribution || []).map(i => ({ name: i.name, value: i.value })),
  }],
}))

const hourlyEntryOption = computed(() => {
  // 以当前整点为基础，向前6小时、向后5小时，共12小时
  const now = new Date()
  const currentHour = now.getHours()
  const allHours = []
  for (let i = -6; i <= 5; i++) {
    let h = currentHour + i
    if (h < 0) h += 24
    if (h >= 24) h -= 24
    allHours.push(h)
  }
  const rawData = dashboard.value.hourlyEntryDistribution || []
  const dataMap = {}
  rawData.forEach(item => { dataMap[item.name] = item.value })
  return {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: allHours.map(h => h + '时') },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ data: allHours.map(h => dataMap[h] || 0), type: 'bar' }],
  }
})

onMounted(async () => {
  try {
    dashboard.value = await getDashboard()
  } catch (e) {
    console.error('加载仪表盘数据失败', e)
  }
})
</script>

<style scoped>
.welcome-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 12px;
  padding: 24px 32px;
  margin-bottom: 24px;
  color: #fff;
  transition: background 0.3s;
}

.welcome-banner.light-bg {
  color: #333;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.welcome-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.welcome-avatar {
  flex-shrink: 0;
  border: 3px solid rgba(255,255,255,0.5);
}

.welcome-avatar.avatar-light {
  border-color: #e4e7ed;
}

.welcome-greeting {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 6px;
}

.welcome-quote {
  font-size: 14px;
  opacity: 0.85;
  max-width: 500px;
}

.welcome-date {
  font-size: 14px;
  opacity: 0.75;
  white-space: nowrap;
}

.text-dark .welcome-quote,
.text-dark.welcome-date {
  opacity: 0.7;
}

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
}

.stat-card .el-card__body {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}
</style>
