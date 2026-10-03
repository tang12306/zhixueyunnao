import { createRouter, createWebHistory, RouterView } from 'vue-router'
import { h } from 'vue'
import NProgress from 'nprogress'

// 导入组件 - 先只导入基本组件进行测试
import Layout from '../views/Layout.vue';
import Dashboard from '../views/Dashboard.vue';
import Login from '../views/Login.vue';
import NotFound from '../views/NotFound.vue';

// 使用懒加载方式导入其他组件
const QuestionBankIndex = () => import('../views/question-bank/Index.vue');
const QuestionCreate = () => import('../views/question-bank/Create.vue');
const AutoQuizIndex = () => import('../views/auto-quiz/AutoQuizIndex.vue');
const AICreateQuestion = () => import('../views/ai-quiz/AICreateQuestion.vue');
const SelectFromBank = () => import('../views/auto-quiz/SelectFromBank.vue');
const AIExamGeneration = () => import('../views/auto-quiz/AIExamGeneration.vue');
const SubjectManagement = () => import('../views/management/SubjectManagement.vue');
const ChapterManagement = () => import('../views/management/ChapterManagement.vue');
const CollegeManagement = () => import('../views/management/CollegeManagement.vue');
const MajorManagement = () => import('../views/management/MajorManagement.vue');
const ClassManagement = () => import('../views/management/ClassManagement.vue');
const UserProfile = () => import('../views/UserProfile.vue');
const Settings = () => import('../views/Settings.vue');
const StudentList = () => import('../views/student-management/StudentList.vue');
const StudentForm = () => import('../views/student-management/StudentForm.vue');

// 学生端组件 - 已注释
// const StudentLayout = () => import('../views/student/StudentLayout.vue');
// const StudentExams = () => import('../views/student/StudentExams.vue');
// const StudentExam = () => import('../views/student/StudentExam.vue');

// 占位符组件，实际组件创建后替换
// const StudentListPlaceholder = { template: '<div>Student List Page (Placeholder)</div>' };
// const StudentFormPlaceholder = { template: '<div>Student Form Page (Placeholder)</div>' };

const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard,
        meta: { title: '控制台', icon: 'el-icon-s-home' }
      },
      {
        path: 'question-bank',
        name: 'QuestionBank',
        component: { render: () => h(RouterView) },
        children: [
          {
            path: '',
            name: 'QuestionBankIndex',
            component: QuestionBankIndex
          },
          {
            path: 'create',
            name: 'QuestionCreate',
            component: QuestionCreate
          },
          {
            path: 'edit/:id',
            name: 'QuestionEdit',
            component: QuestionCreate
          }
        ]
      },
      {
        path: 'auto-quiz', 
        name: 'AutoQuiz',
        component: { render: () => h(RouterView) },
        redirect: '/auto-quiz/index',
        meta: { title: '智能组卷', icon: 'el-icon-cpu' }, 
        children: [
          {
            path: 'index',
            name: 'AutoQuizIndexPage', 
            component: AutoQuizIndex,
            meta: { title: '组卷方式选择' } 
          },
          {
            path: 'ai-create',
            name: 'AICreateQuestionPage', 
            component: AICreateQuestion,
            meta: { title: 'AI智能出题' } 
          },
          {
            path: 'select-from-bank',
            name: 'SelectFromBankPage', 
            component: SelectFromBank, 
            meta: { title: '题库选题组卷' } 
          },
          {
            path: 'ai-exam-generation',
            name: 'AIExamGenerationPage',
            component: AIExamGeneration,
            meta: { title: 'AI一键出卷' }
          }
        ]
      },
      {
        path: 'management/subjects',
        name: 'SubjectManagementPage',
        component: SubjectManagement,
        meta: { title: '科目管理', icon: 'el-icon-setting' }
      },
      {
        path: 'management/chapters',
        name: 'ChapterManagementPage',
        component: ChapterManagement,
        meta: { title: '章节管理', icon: 'el-icon-notebook' }
      },
      {
        path: 'system-management',
        name: 'SystemManagement',
        component: { render: () => h(RouterView) },
        children: [
          {
            path: 'subjects',
            name: 'SubjectManagement',
            component: SubjectManagement
          },
          {
            path: 'chapters',
            name: 'ChapterManagement',
            component: ChapterManagement
          }
        ]
      },
      {
        path: 'student-management',
        name: 'StudentManagement',
        component: { render: () => h(RouterView) },
        children: [
          {
            path: '',
            name: 'StudentList',
            component: StudentList
          },
          {
            path: 'create',
            name: 'StudentCreate',
            component: StudentForm
          },
          {
            path: 'edit/:id',
            name: 'StudentEdit',
            component: StudentForm
          }
        ]
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: UserProfile
      },
      {
        path: 'settings',
        name: 'Settings',
        component: Settings,
        meta: { title: '系统设置', icon: 'setting' }
      },
      {
        path: '/organization',
        component: { render: () => h(RouterView) },
        redirect: '/organization/colleges',
        name: 'OrganizationManagement',
        meta: { title: '组织管理', icon: 'el-icon-office-building' },
        children: [
          {
            path: 'colleges',
            name: 'CollegeManagement',
            component: CollegeManagement,
            meta: { title: '学院管理' }
          },
          {
            path: 'majors',
            name: 'MajorManagement',
            component: MajorManagement,
            meta: { title: '专业管理' }
          },
          {
            path: 'classes',
            name: 'ClassManagement',
            component: ClassManagement,
            meta: { title: '班级管理' }
          }
        ]
      }
    ]
  },
  // 学生端路由 - 已注释
  /*
  {
    path: '/student',
    component: StudentLayout,
    redirect: '/student/exams',
    children: [
      {
        path: 'exams',
        name: 'StudentExams',
        component: StudentExams,
        meta: { title: '我的考试' }
      },
      {
        path: 'exam/:id',
        name: 'StudentExam',
        component: StudentExam,
        meta: { title: '参加考试' }
      },
      {
        path: 'profile',
        name: 'StudentProfile',
        component: { template: '<div>学生个人信息页面</div>' },
        meta: { title: '个人信息' }
      },
      {
        path: 'scores',
        name: 'StudentScores',
        component: { template: '<div>学生成绩查询页面</div>' },
        meta: { title: '成绩查询' }
      }
    ]
  },
  */
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFound,
    meta: { title: '404' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 开始进度条
  NProgress.start()

  // 设置标题
  document.title = to.meta.title ? `${to.meta.title} - 江苏师范大学智慧教学云脑系统` : '江苏师范大学智慧教学云脑系统'

  // 检查是否已登录
  const isAuthenticated = localStorage.getItem('token')
  if (to.path !== '/login' && !isAuthenticated) {
    next('/login')
  } else {
    next()
  }
})

// 添加全局错误处理
router.onError((error) => {
  console.error('Router error:', error)

  // 如果是chunk加载错误，尝试重新加载页面
  if (error.name === 'ChunkLoadError') {
    console.log('Chunk load error detected, reloading page...')
    window.location.reload()
  }
})

// 路由后置守卫
router.afterEach(() => {
  // 完成进度条
  NProgress.done()
})

export default router 