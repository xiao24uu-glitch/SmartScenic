<template>
  <div>
    <el-card>
      <template #header><h3>财务报表</h3></template>
      <el-row :gutter="24" style="margin-bottom: 24px;">
        <el-col :span="8">
          <el-statistic title="今日销售额" :value="dashboard.todaySales || 0" prefix="¥" />
        </el-col>
        <el-col :span="8">
          <el-statistic title="今日订单数" :value="dashboard.todayOrderCount || 0" />
        </el-col>
        <el-col :span="8">
          <el-statistic title="今日入园人数" :value="dashboard.todayEntryCount || 0" />
        </el-col>
      </el-row>
      <v-chart :option="salesOption" style="height: 300px;" />
      <v-chart :option="pieOption" style="height: 300px; margin-top: 24px;" />
      <div style="text-align: center; margin-top: 24px;">
        <el-button type="primary" :loading="exporting" @click="handleExport">导出Excel报表</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDashboard, exportReport } from '../../api'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const dashboard = ref({})
const exporting = ref(false)

const salesOption = computed(() => ({
  title: { text: '近7日销售趋势' },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: (dashboard.value.salesTrend || []).map(i => i.name) },
  yAxis: { type: 'value' },
  series: [{ data: (dashboard.value.salesTrend || []).map(i => i.value), type: 'line', smooth: true }],
}))

const pieOption = computed(() => ({
  title: { text: '票种销售占比' },
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: '60%',
    data: (dashboard.value.ticketTypeDistribution || []).map(i => ({ name: i.name, value: i.value })),
  }],
}))

const handleExport = async () => {
  exporting.value = true
  try {
    const blob = await exportReport()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `财务报表_${new Date().toISOString().slice(0, 10)}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('报表导出成功')
  } catch (e) {
    console.error('导出失败', e)
    ElMessage.error('报表导出失败')
  } finally {
    exporting.value = false
  }
}

onMounted(async () => {
  try { dashboard.value = await getDashboard() }
  catch (e) { console.error(e) }
})
</script>
