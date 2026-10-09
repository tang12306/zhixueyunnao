<template>
  <div ref="chartRef" class="chart-container"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts/core';
import { PieChart } from 'echarts/charts';
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import api from '@/api';

echarts.use([TitleComponent, TooltipComponent, LegendComponent, PieChart, CanvasRenderer]);

const chartRef = ref(null);
let chart = null;

const resize = () => chart && chart.resize();

// 各科目题目数量，取数失败时显示一个占位扇区
const loadData = async () => {
  try {
    const counts = await api.subjectAdminJ.getSubjectQuestionCounts();
    if (!Array.isArray(counts)) {
      ElMessage.warning('科目分布数据格式不正确');
      return [{ value: 0, name: '数据加载失败' }];
    }
    return counts.length > 0 ? counts : [{ value: 0, name: '暂无数据' }];
  } catch (error) {
    console.error('加载科目分布失败:', error);
    ElMessage.error('加载科目分布图表数据时出错');
    return [{ value: 0, name: '图表加载错误' }];
  }
};

onMounted(async () => {
  const data = await loadData();
  // 等数据时已经离开页面
  if (!chartRef.value) {
    return;
  }
  chart = echarts.init(chartRef.value);
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left' },
    series: [
      {
        name: '题目数量',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: false, position: 'center' },
        emphasis: { label: { show: true, fontSize: '16', fontWeight: 'bold' } },
        labelLine: { show: false },
        data
      }
    ]
  });
  window.addEventListener('resize', resize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize);
  if (chart) {
    chart.dispose();
    chart = null;
  }
});
</script>

<style scoped>
.chart-container {
  height: 300px;
  border-radius: 8px;
  overflow: hidden;
}
</style>
