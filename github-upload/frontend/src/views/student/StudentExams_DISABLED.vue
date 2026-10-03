<template>
  <div class="student-exams-container">
    <h1>我的考试</h1>
    
    <el-row :gutter="20">
      <el-col v-for="exam in exams" :key="exam.id" :xs="24" :sm="12" :md="8" :lg="8">
        <el-card class="exam-card">
          <div class="exam-status">
            <el-tag :type="exam.status === 'ACTIVE' ? 'success' : 'info'">
              {{ exam.status === 'ACTIVE' ? '进行中' : '未开始' }}
            </el-tag>
          </div>
          <h3>{{ exam.name }}</h3>
          <div class="exam-info">
            <p><i class="el-icon-time"></i> 考试时长：{{ exam.duration }}分钟</p>
            <p><i class="el-icon-document"></i> 题目数量：{{ exam.questions ? exam.questions.length : 0 }}题</p>
            <p v-if="exam.examDate"><i class="el-icon-date"></i> 考试时间：{{ formatDate(exam.examDate) }}</p>
            <p v-if="exam.createBy"><i class="el-icon-user"></i> 发布教师：{{ exam.createBy }}</p>
          </div>
          <p class="exam-desc">{{ exam.description }}</p>
          <div class="exam-actions">
            <el-button 
              type="primary" 
              :disabled="exam.status !== 'ACTIVE'"
              @click="startExam(exam.id)"
            >
              {{ exam.status === 'ACTIVE' ? '开始考试' : '暂未开始' }}
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div v-if="!exams.length" class="no-exams">
      <el-empty description="当前没有可参加的考试"></el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const exams = ref([])
const loading = ref(false)

// 加载考试列表
const loadExams = async () => {
  loading.value = true
  try {
    // 使用axios直接调用后端API
    const response = await axios.get('http://localhost:8080/student/api/exams', {
      withCredentials: true // 确保发送cookie以保持会话
    })
    
    if (response.data && response.data.success) {
      console.log('获取到考试列表:', response.data.exams)
      exams.value = response.data.exams || []
    } else {
      throw new Error(response.data?.message || '获取考试列表失败')
    }
  } catch (error) {
    console.error('获取考试列表失败:', error)
    ElMessage.error('获取考试列表失败，请刷新页面重试')
    
    // 由于没有实际数据，这里使用模拟数据进行演示
    // 实际使用时应删除此部分，仅使用API返回的数据
    exams.value = [
      {
        id: 1,
        name: '2023年高等数学期末考试',
        description: '本次考试涵盖高等数学第一学期全部内容，包括极限、导数、积分等章节。',
        duration: 120,
        status: 'ACTIVE',
        questions: new Array(20),
        examDate: '2023-12-25 14:00:00',
        createBy: '张教授'
      },
      {
        id: 2,
        name: '线性代数期中考试',
        description: '本次考试主要考察矩阵运算和线性方程组的解法。',
        duration: 90,
        status: 'PENDING',
        questions: new Array(15),
        examDate: '2023-12-30 09:30:00',
        createBy: '李教授'
      }
    ]
  } finally {
    loading.value = false
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
}

// 开始考试
const startExam = (examId) => {
  router.push(`/student/exam/${examId}`)
}

onMounted(() => {
  loadExams()
})
</script>

<style scoped>
.student-exams-container {
  padding: 20px;
}

.exam-card {
  margin-bottom: 20px;
  position: relative;
  height: 100%;
}

.exam-status {
  position: absolute;
  right: 20px;
  top: 20px;
}

h3 {
  margin-top: 0;
  margin-bottom: 15px;
  padding-right: 60px;
}

.exam-info {
  color: #606266;
  font-size: 14px;
}

.exam-desc {
  margin: 15px 0;
  color: #303133;
}

.exam-actions {
  margin-top: 15px;
}

.no-exams {
  margin-top: 50px;
  text-align: center;
}
</style> 