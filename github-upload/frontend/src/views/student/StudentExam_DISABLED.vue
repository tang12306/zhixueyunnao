<template>
  <div class="student-exam-container">
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="10" animated />
    </div>
    
    <div v-else>
      <div class="exam-header">
        <h1>{{ exam.name }}</h1>
        <div class="exam-info">
          <span><i class="el-icon-time"></i> 剩余时间: {{ formatTime(remainingTime) }}</span>
          <span><i class="el-icon-document"></i> 题目数量: {{ exam.questions ? exam.questions.length : 0 }}</span>
        </div>
      </div>
      
      <el-divider />
      
      <div class="exam-content">
        <div v-for="(question, index) in exam.questions" :key="question.id" class="question-item">
          <div class="question-header">
            <span class="question-number">{{ index + 1 }}</span>
            <span class="question-type">{{ getQuestionTypeName(question.type) }}</span>
            <span class="question-score">{{ question.score }}分</span>
          </div>
          
          <div class="question-content" v-html="question.content"></div>
          
          <!-- 选择题 -->
          <div v-if="question.type === 'SINGLE_CHOICE' || question.type === 'MULTIPLE_CHOICE'" class="options">
            <el-radio-group v-if="question.type === 'SINGLE_CHOICE'" v-model="answers[question.id]">
              <el-radio 
                v-for="option in question.options" 
                :key="option.id" 
                :label="option.key"
              >
                {{ option.key }}. {{ option.content }}
              </el-radio>
            </el-radio-group>
            
            <el-checkbox-group v-else v-model="answers[question.id]">
              <el-checkbox 
                v-for="option in question.options" 
                :key="option.id" 
                :label="option.key"
              >
                {{ option.key }}. {{ option.content }}
              </el-checkbox>
            </el-checkbox-group>
          </div>
          
          <!-- 填空题 -->
          <div v-else-if="question.type === 'FILL_BLANK'" class="fill-blank">
            <el-input 
              v-model="answers[question.id]" 
              placeholder="请输入答案" 
              type="textarea" 
              :rows="2"
            ></el-input>
          </div>
          
          <!-- 简答题 -->
          <div v-else-if="question.type === 'SHORT_ANSWER'" class="short-answer">
            <el-input 
              v-model="answers[question.id]" 
              placeholder="请输入答案" 
              type="textarea" 
              :rows="4"
            ></el-input>
          </div>
        </div>
      </div>
      
      <div class="exam-footer">
        <el-button type="primary" @click="submitExam" :loading="submitting">提交试卷</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const examId = route.params.id

const loading = ref(true)
const submitting = ref(false)
const exam = ref({})
const answers = reactive({})
const remainingTime = ref(0)
let timer = null

// 加载考试信息
const loadExam = async () => {
  loading.value = true
  try {
    // 这里应该调用API获取考试详情
    // 暂时使用模拟数据
    exam.value = {
      id: examId,
      name: '2023年高等数学期末考试',
      description: '本次考试涵盖高等数学第一学期全部内容，包括极限、导数、积分等章节。',
      duration: 120, // 分钟
      status: 'ACTIVE',
      questions: [
        {
          id: 1,
          type: 'SINGLE_CHOICE',
          content: '若函数f(x)=x²在点x=1处的导数为：',
          score: 5,
          options: [
            { id: 1, key: 'A', content: '0' },
            { id: 2, key: 'B', content: '1' },
            { id: 3, key: 'C', content: '2' },
            { id: 4, key: 'D', content: '3' }
          ]
        },
        {
          id: 2,
          type: 'MULTIPLE_CHOICE',
          content: '以下哪些是初等函数？',
          score: 5,
          options: [
            { id: 5, key: 'A', content: '幂函数' },
            { id: 6, key: 'B', content: '指数函数' },
            { id: 7, key: 'C', content: '对数函数' },
            { id: 8, key: 'D', content: 'Gamma函数' }
          ]
        },
        {
          id: 3,
          type: 'FILL_BLANK',
          content: '函数f(x)=sin(x)在x=0处的导数是_______。',
          score: 5
        },
        {
          id: 4,
          type: 'SHORT_ANSWER',
          content: '简述微积分基本定理的内容及其意义。',
          score: 10
        }
      ]
    }
    
    // 初始化答案对象
    exam.value.questions.forEach(q => {
      if (q.type === 'MULTIPLE_CHOICE') {
        answers[q.id] = []
      } else {
        answers[q.id] = ''
      }
    })
    
    // 设置考试剩余时间（分钟转秒）
    remainingTime.value = exam.value.duration * 60
    startTimer()
    
  } catch (error) {
    console.error('获取考试信息失败:', error)
    ElMessage.error('获取考试信息失败，请返回重试')
  } finally {
    loading.value = false
  }
}

// 开始计时器
const startTimer = () => {
  timer = setInterval(() => {
    if (remainingTime.value > 0) {
      remainingTime.value--
    } else {
      clearInterval(timer)
      ElMessageBox.alert('考试时间已结束，系统将自动提交您的答案', '提示', {
        confirmButtonText: '确定',
        callback: () => {
          submitExam()
        }
      })
    }
  }, 1000)
}

// 格式化时间显示
const formatTime = (seconds) => {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

// 获取题型名称
const getQuestionTypeName = (type) => {
  const typeMap = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'FILL_BLANK': '填空题',
    'SHORT_ANSWER': '简答题'
  }
  return typeMap[type] || '未知题型'
}

// 提交试卷
const submitExam = async () => {
  submitting.value = true
  try {
    // 这里应该调用API提交答案
    console.log('提交答案:', answers)
    
    // 模拟提交
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    ElMessage.success('提交成功')
    router.push('/student/exams')
  } catch (error) {
    console.error('提交答案失败:', error)
    ElMessage.error('提交答案失败，请重试')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadExam()
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.student-exam-container {
  padding: 20px;
}

.loading-container {
  padding: 40px;
}

.exam-header {
  margin-bottom: 20px;
}

.exam-info {
  display: flex;
  justify-content: space-between;
  color: #606266;
  margin-top: 10px;
}

.question-item {
  margin-bottom: 30px;
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background-color: #fff;
}

.question-header {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.question-number {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  background-color: #409eff;
  color: #fff;
  border-radius: 50%;
  margin-right: 10px;
}

.question-type {
  color: #606266;
  margin-right: 15px;
}

.question-score {
  color: #f56c6c;
}

.question-content {
  margin-bottom: 15px;
  line-height: 1.6;
}

.options {
  margin-left: 20px;
}

.exam-footer {
  margin-top: 30px;
  text-align: center;
}
</style> 