// MongoDB 初始化脚本
// 创建数据库和初始用户

// 切换到目标数据库
db = db.getSiblingDB('education-question-bank');

// 创建应用用户
db.createUser({
  user: 'app_user',
  pwd: 'app_password',
  roles: [
    {
      role: 'readWrite',
      db: 'education-question-bank'
    }
  ]
});

// 创建基础集合
db.createCollection('users');
db.createCollection('questions');
db.createCollection('papers');
db.createCollection('settings');

// 插入默认管理员用户（密码：admin123，已加密）
db.users.insertOne({
  username: 'admin',
  password: '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKXgwHNEM5NjjJLkTCiYaXr6lDm2', // admin123
  role: 'TEACHER',
  name: '系统管理员',
  email: 'admin@example.com',
  enabled: true,
  createdAt: new Date()
});

// 插入默认科目
db.subjects.insertMany([
  { name: '语文', description: '语文学科' },
  { name: '数学', description: '数学学科' },
  { name: '英语', description: '英语学科' }
]);

print('MongoDB 初始化完成');
