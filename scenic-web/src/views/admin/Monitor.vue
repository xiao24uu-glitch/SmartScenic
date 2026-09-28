<template>
  <div>
    <!-- 时段拥挤度热力图 - 放在最上面 -->
    <el-card style="margin-bottom:20px">
      <template #header>
        <div class="card-header">
          <h3>时段拥挤度热力图</h3>
          <span style="font-size:13px;color:#94a3b8;">颜色越深表示该时段越拥挤</span>
        </div>
      </template>
      <div class="heatmap-grid" v-if="heatmap.hourlyData?.length">
        <div v-for="item in heatmap.hourlyData" :key="item.hour" class="heatmap-cell"
             :class="'hm-level-' + item.level"
             :title="item.hour + ':00 - ' + item.count + '人 (' + item.label + ')'">
          <span class="hm-hour">{{ item.hour }}</span>
          <span class="hm-count">{{ item.count }}人</span>
          <span class="hm-label">{{ item.label }}</span>
        </div>
      </div>
      <el-empty v-else description="暂无数据" :image-size="60" />
      <div class="heatmap-legend" v-if="heatmap.hourlyData?.length">
        <span class="legend-item"><span class="legend-color hm-level-1"></span>空闲</span>
        <span class="legend-item"><span class="legend-color hm-level-2"></span>适中</span>
        <span class="legend-item"><span class="legend-color hm-level-3"></span>较挤</span>
        <span class="legend-item"><span class="legend-color hm-level-4"></span>拥挤</span>
      </div>
    </el-card>

    <!-- 客流监控统计 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>客流监控</h3>
          <span style="font-size:13px;color:#94a3b8;">实时更新</span>
        </div>
      </template>
      <el-row :gutter="24" style="margin-bottom:24px">
        <el-col :span="6">
          <el-statistic title="今日入园人数" :value="dashboard.todayEntryCount || 0" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="今日订单数" :value="dashboard.todayOrderCount || 0" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="今日销售额" :value="dashboard.todaySales || 0" prefix="¥" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="当前在园人数" :value="heatmap.currentInPark || 0">
            <template #suffix>
              <el-tag size="small" :type="crowdTagType">{{ heatmap.crowdDesc || '-' }}</el-tag>
            </template>
          </el-statistic>
        </el-col>
      </el-row>
      <!-- 时段入园柱状图 -->
      <v-chart :option="barOption" style="height: 300px; margin-top: 8px;" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDashboard, getAdminCrowdHeatmap } from '../../api'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent } from 'echarts/components'

use([CanvasRenderer, BarChart, LineChart, TitleComponent, TooltipComponent, GridComponent])

const dashboard = ref({})
const heatmap = ref({})

const crowdTagType = computed(() => {
  const map = { 1: 'success', 2: 'warning', 3: 'danger', 4: 'danger' }
  return map[heatmap.value.crowdLevel] || 'info'
})

const barOption = computed(() => {
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
  const seriesData = allHours.map(h => {
    const v = dataMap[h] || 0
    let color = '#a5d6a7'
    if (v > 40) color = '#ef5350'
    else if (v > 25) color = '#ff8a65'
    else if (v > 10) color = '#ffcc80'
    return { value: v, itemStyle: { color } }
  })
  return {
    title: { text: '今日时段入园分布', left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: allHours.map(h => h + '时'),
    },
    yAxis: { type: 'value', name: '人数', minInterval: 1 },
    series: [{
      data: seriesData,
      type: 'bar',
      barMaxWidth: 30,
    }],
  }
})

onMounted(async () => {
  try {
    const [dash, hm] = await Promise.all([
      getDashboard(), getAdminCrowdHeatmap(),
    ])
    dashboard.value = dash || {}
    heatmap.value = hm || {}
  } catch (e) { console.error(e) }
})
</script>

<style scoped>
.card-header {
  display: flex; align-items: center; justify-content: space-between;
}
.card-header h3 { margin: 0; font-size: 16px; }

/* ====== 热力图 ====== */
.heatmap-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
  gap: 8px;
}
.heatmap-cell {
  padding: 12px 8px; border-radius: 10px; text-align: center;
  cursor: default; transition: transform 0.2s;
}
.heatmap-cell:hover { transform: scale(1.05); }
.heatmap-cell.hm-level-1 { background: #e8f5e9; color: #2e7d32; }
.heatmap-cell.hm-level-2 { background: #fff3e0; color: #e65100; }
.heatmap-cell.hm-level-3 { background: #ffe0b2; color: #bf360c; }
.heatmap-cell.hm-level-4 { background: #ffcdd2; color: #c62828; }
.hm-hour { display: block; font-size: 18px; font-weight: 800; margin-bottom: 2px; }
.hm-count { display: block; font-size: 12px; }
.hm-label { display: block; font-size: 10px; margin-top: 2px; opacity: 0.8; }

.heatmap-legend {
  display: flex; gap: 16px; justify-content: center;
  margin-top: 16px; padding-top: 12px; border-top: 1px solid #f1f5f9;
}
.legend-item { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #64748b; }
.legend-color { width: 14px; height: 14px; border-radius: 4px; }
.legend-color.hm-level-1 { background: #e8f5e9; }
.legend-color.hm-level-2 { background: #fff3e0; }
.legend-color.hm-level-3 { background: #ffe0b2; }
.legend-color.hm-level-4 { background: #ffcdd2; }
</style>
