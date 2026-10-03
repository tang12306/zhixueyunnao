<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="24">
        <div
          class="dashboard-header"
          v-motion
          :initial="{ opacity: 0, y: -30 }"
          :enter="{ opacity: 1, y: 0, transition: { duration: 600 } }"
          data-aos="fade-down"
        >
          <div class="header-content">
            <!-- 校徽图片 - 请将校徽文件放在 src/assets/ 目录下 -->
            <img src="@/assets/logo.png" alt="江苏师范大学校徽" class="header-logo" v-if="logoExists" @error="logoExists = false"/>
            <Icon icon="mdi:view-dashboard" class="dashboard-icon" v-if="!logoExists"/>
            <div class="header-text">
              <h2 class="dashboard-title">智慧教学云脑系统</h2>
              <p class="dashboard-subtitle">欢迎使用江苏师范大学智慧教学云脑系统，基于AI技术的智能化教学管理平台。</p>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-20">
      <el-col
        :xs="24" :sm="12" :md="8" :lg="6"
        v-for="(card, index) in statCards"
        :key="card.title"
        v-motion
        :initial="{ opacity: 0, y: 50, scale: 0.9 }"
        :enter="{
          opacity: 1,
          y: 0,
          scale: 1,
          transition: {
            duration: 500,
            delay: index * 100,
            ease: 'easeOut'
          }
        }"
        :data-aos="`fade-up`"
        :data-aos-delay="index * 100"
      >
        <el-card
          class="stat-card card-hover"
          :body-style="{ padding: '20px' }"
          @click="handleStatCardClick(card)"
        >
          <div class="stat-content">
            <div class="stat-icon">
              <Icon :icon="card.iconName" class="stat-icon-svg" />
            </div>
            <div class="stat-info">
              <div class="stat-title">{{ card.title }}</div>
              <div class="stat-value" :ref="el => statValueRefs[index] = el">{{ card.value }}</div>
              <div class="stat-trend" v-if="card.trend">
                <Icon :icon="card.trend > 0 ? 'mdi:trending-up' : 'mdi:trending-down'"
                      :class="card.trend > 0 ? 'trend-up' : 'trend-down'" />
                <span>{{ Math.abs(card.trend) }}%</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="mt-20">
      <el-col
        :xs="24" :lg="16"
        v-motion
        :initial="{ opacity: 0, x: -50 }"
        :enter="{ opacity: 1, x: 0, transition: { duration: 600, delay: 400 } }"
        data-aos="fade-right"
        data-aos-delay="400"
      >
        <el-card class="content-card">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <Icon icon="mdi:clock-outline" class="header-icon" />
                <span>最近添加的题目</span>
              </div>
              <el-button
                link
                @click="navigateTo('/question-bank')"
                class="btn-animate"
              >
                查看全部
                <Icon icon="mdi:arrow-right" class="ml-1" />
              </el-button>
            </div>
          </template>
          <el-table
            :data="recentQuestions"
            style="width: 100%"
            v-loading="loadingQuestions"
            class="animated-table"
          >
            <el-table-column prop="subject" label="科目" width="100">
              <template #default="scope">
                <el-tag type="primary" size="small">{{ scope.row.subject }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="题型" width="120">
              <template #default="scope">
                <el-tag :type="getTypeTagType(scope.row.type)" size="small">{{ scope.row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="内容">
              <template #default="scope">
                <div class="question-content-truncate">{{ scope.row.content }}</div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button
                  link
                  size="small"
                  @click="showQuestionDetail(scope.row)"
                  class="btn-animate"
                >
                  <Icon icon="mdi:eye" class="mr-1" />
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col
        :xs="24" :lg="8"
        v-motion
        :initial="{ opacity: 0, x: 50 }"
        :enter="{ opacity: 1, x: 0, transition: { duration: 600, delay: 600 } }"
        data-aos="fade-left"
        data-aos-delay="600"
      >
        <el-card class="content-card">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <Icon icon="mdi:lightning-bolt" class="header-icon" />
                <span>快速操作</span>
              </div>
            </div>
          </template>
          <div class="quick-actions">
            <el-button
              type="primary"
              @click="navigateTo('/question-bank/create')"
              class="action-btn btn-animate"
              v-motion
              :initial="{ scale: 1 }"
              :hover="{ scale: 1.05 }"
              :tap="{ scale: 0.95 }"
            >
              <Icon icon="mdi:plus" class="mr-1" />
              创建题目
            </el-button>
            <el-button
              type="success"
              @click="navigateTo('/auto-quiz/index')"
              class="action-btn btn-animate"
              v-motion
              :initial="{ scale: 1 }"
              :hover="{ scale: 1.05 }"
              :tap="{ scale: 0.95 }"
            >
              <Icon icon="mdi:auto-fix" class="mr-1" />
              智能组卷
            </el-button>
            <el-button
              type="info"
              @click="navigateTo('/auto-quiz/ai-create')"
              class="action-btn btn-animate"
              v-motion
              :initial="{ scale: 1 }"
              :hover="{ scale: 1.05 }"
              :tap="{ scale: 0.95 }"
            >
              <Icon icon="mdi:robot" class="mr-1" />
              AI助手
            </el-button>
          </div>
        </el-card>

        <el-card
          class="mt-20 content-card"
          v-motion
          :initial="{ opacity: 0, y: 30 }"
          :enter="{ opacity: 1, y: 0, transition: { duration: 600, delay: 800 } }"
          data-aos="fade-up"
          data-aos-delay="800"
        >
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <Icon icon="mdi:chart-pie" class="header-icon" />
                <span>科目分布</span>
              </div>
            </div>
          </template>
          <div id="subject-chart" style="height: 300px;" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import api from '../api';
import * as echarts from 'echarts/core';
import { PieChart } from 'echarts/charts';
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { animateNumber } from '../utils/animations';

echarts.use([
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  PieChart,
  CanvasRenderer
]);

const router = useRouter();
const recentQuestions = ref([]);
const loadingQuestions = ref(true);
const statValueRefs = ref([]);

// 校徽图片状态管理
const logoExists = ref(true);

// 统计卡片数据
const statCards = ref([
  {
    title: '题库总数',
    value: 0,
    icon: 'el-icon-document',
    iconName: 'mdi:database',
    trend: 0,
    color: '#409eff'
  },
  {
    title: '题目总数',
    value: 0,
    icon: 'el-icon-edit',
    iconName: 'mdi:file-document-multiple',
    trend: 0,
    color: '#67c23a'
  },
  {
    title: '学院总数',
    value: 0,
    icon: 'el-icon-school',
    iconName: 'mdi:school',
    trend: 0,
    color: '#e6a23c'
  },
  {
    title: '学生总数',
    value: 0,
    icon: 'el-icon-user',
    iconName: 'mdi:account-group',
    trend: 0,
    color: '#f56c6c'
  }
]);

// 加载最近添加的题目
const loadRecentQuestions = async () => {
  try {
    loadingQuestions.value = true;
    const params = {
      page: 0,
      size: 5
    };

    const response = await api.questionsJ.queryQuestions(params);

    if (response && response.content) {
      recentQuestions.value = response.content;
    } else {
      recentQuestions.value = [];
      ElMessage.error('获取最近题目数据格式不正确');
    }
  } catch (error) {
    ElMessage.error('获取最近题目失败');
    console.error('获取最近题目失败:', error);
  } finally {
    loadingQuestions.value = false;
  }
};

// 加载统计数据
const loadStatistics = async () => {
  try {
    // 1. 获取题库总数（科目总数）
    try {
      const subjectsResponse = await api.subjectAdminJ.getAll();
      if (subjectsResponse && Array.isArray(subjectsResponse)) {
        const oldValue = statCards.value[0].value;
        statCards.value[0].value = subjectsResponse.length;

        // 数字动画
        await nextTick();
        if (statValueRefs.value[0]) {
          animateNumber(statValueRefs.value[0], oldValue, subjectsResponse.length, 1000);
        }
      }
    } catch (error) {
      console.error('获取科目数据失败:', error);
    }

    // 2. 获取题目总数
    try {
      const questionsResponse = await api.questionsJ.queryQuestions({ page: 0, size: 1 });
      if (questionsResponse && typeof questionsResponse.totalElements === 'number') {
        const oldValue = statCards.value[1].value;
        statCards.value[1].value = questionsResponse.totalElements;

        // 数字动画
        await nextTick();
        if (statValueRefs.value[1]) {
          animateNumber(statValueRefs.value[1], oldValue, questionsResponse.totalElements, 1000);
        }
      }
    } catch (error) {
      console.error('获取题目数据失败:', error);
    }

    // 3. 获取学院总数
    try {
      const collegesResponse = await api.collegeAdminJ.getAll();
      if (collegesResponse && Array.isArray(collegesResponse)) {
        const oldValue = statCards.value[2].value;
        statCards.value[2].value = collegesResponse.length;

        // 数字动画
        await nextTick();
        if (statValueRefs.value[2]) {
          animateNumber(statValueRefs.value[2], oldValue, collegesResponse.length, 1000);
        }
      }
    } catch (error) {
      console.error('获取学院数据失败:', error);
    }

    // 4. 获取学生总数
    try {
      const studentsResponse = await api.studentJ.getAllStudents();
      if (studentsResponse && Array.isArray(studentsResponse)) {
        const oldValue = statCards.value[3].value;
        statCards.value[3].value = studentsResponse.length;

        // 数字动画
        await nextTick();
        if (statValueRefs.value[3]) {
          animateNumber(statValueRefs.value[3], oldValue, studentsResponse.length, 1000);
        }
      }
    } catch (error) {
      console.error('获取学生数据失败:', error);
    }

  } catch (error) {
    console.error('加载统计数据失败:', error);
    ElMessage.error('加载统计数据失败');
  }
};

// 处理统计卡片点击
const handleStatCardClick = (card) => {
  switch (card.title) {
    case '题库总数':
      navigateTo('/subject-management');
      break;
    case '题目总数':
      navigateTo('/question-bank');
      break;
    case '学院总数':
      navigateTo('/college-management');
      break;
    case '学生总数':
      navigateTo('/student-management');
      break;
  }
};

// 获取题型标签类型
const getTypeTagType = (type) => {
  const typeMap = {
    '单选题': 'primary',
    '多选题': 'success',
    '判断题': 'warning',
    '填空题': 'info',
    '简答题': 'danger'
  };
  return typeMap[type] || 'info'; // 使用'info'而不是'default'，因为Element Plus不支持'default'
};

// 初始化科目分布图表
const initSubjectChart = async () => {
  const chartDom = document.getElementById('subject-chart');
  if (!chartDom) return;
  
  let subjectData = [];
  try {
    const response = await api.subjectAdminJ.getSubjectQuestionCounts();
    if (response && Array.isArray(response)) {
      subjectData = response;
      if (subjectData.length === 0) {
        ElMessage.info('暂无科目题目统计数据');
        subjectData = [{ name: '暂无数据', value: 0 }];
      }
    } else {
      ElMessage.warning('获取科目分布数据失败或格式不正确，将使用默认数据。');
      subjectData = [
        { value: 0, name: '数据加载失败' },
      ];
    }
  } catch (error) {
    console.error("Error fetching subjects for chart:", error);
    ElMessage.error('加载科目分布图表数据时出错。');
    subjectData = [
        { value: 0, name: '图表加载错误' },
      ];
  }

  const myChart = echarts.init(chartDom);
  const option = {
    title: {
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
    },
    series: [
      {
        name: '题目数量',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '16',
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: subjectData
      }
    ]
  };
  
  myChart.setOption(option);
  
  window.addEventListener('resize', () => {
    if (myChart && !myChart.isDisposed()) {
        myChart.resize();
    }
  });
};

// 导航到指定路由
const navigateTo = (path) => {
  router.push(path);
};

// 查看题目详情
const showQuestionDetail = (question) => {
  // Java后端使用id字段，Node.js后端使用_id字段
  const questionId = question.id || question._id;
  if (questionId) {
    // 跳转到题目详情页面
    router.push(`/question-bank/edit/${questionId}`);
  } else {
    console.warn('Question ID not found for detail view', question);
    console.log('Question object:', question); // 添加调试信息
    ElMessage.warning('无法查看题目详情：缺少ID');
  }
};

onMounted(async () => {
  loadRecentQuestions();
  loadStatistics();
  await initSubjectChart();
});
</script>

<style scoped>
.dashboard {
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: calc(100vh - 60px);
}

.mt-20 {
  margin-top: 20px;
}

/* 头部样式 */
.dashboard-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  padding: 30px;
  color: white;
  margin-bottom: 20px;
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.3);
}

.header-content {
  display: flex;
  align-items: center;
}

/* 校徽图片样式 */
.header-logo {
  height: 80px;
  width: auto;
  margin-right: 20px;
  opacity: 0.95;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
}

.dashboard-icon {
  font-size: 3rem;
  margin-right: 20px;
  opacity: 0.9;
}

.dashboard-title {
  margin: 0 0 10px 0;
  font-size: 2.2rem;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.dashboard-subtitle {
  margin: 0;
  font-size: 1.1rem;
  opacity: 0.9;
  line-height: 1.6;
}

/* 统计卡片样式 */
.stat-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  cursor: pointer;
  overflow: hidden;
  position: relative;
  background: white;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
}

.stat-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100px;
  padding: 20px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  margin-right: 15px;
}

.stat-icon-svg {
  font-size: 2rem;
  color: white;
}

.stat-info {
  text-align: right;
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 5px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-trend {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  font-size: 12px;
  font-weight: 500;
}

.trend-up {
  color: #67c23a;
}

.trend-down {
  color: #f56c6c;
}

/* 内容卡片样式 */
.content-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  background: white;
}

.content-card:hover {
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  font-weight: 600;
  color: #333;
}

.header-icon {
  margin-right: 8px;
  font-size: 1.2rem;
  color: #667eea;
}

/* 表格样式 */
.animated-table {
  border-radius: 8px;
  overflow: hidden;
}

.animated-table :deep(.el-table__row) {
  transition: all 0.3s ease;
}

.animated-table :deep(.el-table__row:hover) {
  background-color: #f8f9ff;
  transform: scale(1.01);
}

.question-content-truncate {
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}

/* 快速操作按钮 */
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.action-btn {
  width: 100%;
  height: 50px;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  border: none;
  position: relative;
  overflow: hidden;
}

.action-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s;
}

.action-btn:hover::before {
  left: 100%;
}

/* 图表容器 */
.chart-container {
  border-radius: 8px;
  overflow: hidden;
}

/* 工具类 */
.mr-1 {
  margin-right: 4px;
}

.ml-1 {
  margin-left: 4px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .dashboard {
    padding: 10px;
  }

  .dashboard-header {
    padding: 20px;
  }

  .dashboard-title {
    font-size: 1.8rem;
  }

  .dashboard-subtitle {
    font-size: 1rem;
  }

  .header-content {
    flex-direction: column;
    text-align: center;
  }

  .dashboard-icon {
    margin-right: 0;
    margin-bottom: 15px;
  }

  .stat-content {
    flex-direction: column;
    text-align: center;
    height: auto;
    padding: 15px;
  }

  .stat-icon {
    margin-right: 0;
    margin-bottom: 10px;
  }

  .stat-info {
    text-align: center;
  }
}

/* 动画增强 */
@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.slide-in-up {
  animation: slideInUp 0.6s ease-out;
}
</style>