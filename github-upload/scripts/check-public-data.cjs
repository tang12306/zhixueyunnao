const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');

const root = path.resolve(__dirname, '..');
const files = execFileSync('git', ['ls-files', '-z', '--cached', '--others', '--exclude-standard'], {
  cwd: root,
  encoding: 'utf8'
}).split('\0').filter(Boolean);
const violations = [];
const read = file => fs.readFileSync(path.join(root, file), 'utf8');
const report = (file, line, category) => violations.push({ file, line, category });

// Only report file locations and categories, never credential values.
for (const file of files) {
  if (path.basename(file) === 'package-lock.json') continue;
  // 工作区里已删除、尚未暂存的文件不会被提交，跳过
  if (!fs.existsSync(path.join(root, file))) continue;
  const data = fs.readFileSync(path.join(root, file));
  if (data.includes(0)) continue;
  const text = data.toString('utf8');
  if (!Buffer.from(text).equals(data)) continue;
  text.split(/\r?\n/).forEach((line, index) => {
    if (/\bsk-[A-Za-z0-9_-]{16,}/.test(line)) {
      report(file, index + 1, 'hardcoded-api-key');
    }
    if (/\bgh(?:p|o|u|s|r)_[A-Za-z0-9]{20,}|\bgithub_pat_[A-Za-z0-9_]{20,}/.test(line)) {
      report(file, index + 1, 'github-token');
    }
    if (/-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/.test(line)) {
      report(file, index + 1, 'private-key');
    }
    if (/^\s*mysql\b[^\r\n]*\s-p[^\s;"']/.test(line)) {
      report(file, index + 1, 'inline-database-password');
    }
    if (path.resolve(root, file) === __filename) return;
    for (const match of line.matchAll(/(?<![\w])\d{8,12}(?![\w])/g)) {
      const value = match[0];
      const demoId = /^990000\d{3}$/.test(value) && Number(value.slice(-3)) >= 1 && Number(value.slice(-3)) <= 58;
      const nonIdentity = value === '604800000' || (path.basename(file) === 'mvnw' && value === '4294967296');
      if (!demoId && !nonIdentity) report(file, index + 1, 'non-demo-numeric-identifier');
    }
  });
}

const requirePattern = (file, pattern, category, absent = false) => {
  const matches = pattern.test(read(file));
  if (matches === absent) report(file, 1, category);
};

const initializer = 'src/main/java/com/_1/init/DemoDataInitializer.java';
requirePattern(initializer, /@Profile\("dev"\)/, 'demo-data-dev-only');
requirePattern(initializer, /for \(int index = 1; index <= 58; index\+\+\)/, 'demo-roster-count');
requirePattern(initializer, /String studentId = String\.format\("990000%03d", index\);/, 'demo-student-id-format');
requirePattern(initializer, /String studentName = String\.format\("示例学生%03d", index\);/, 'demo-student-name-format');
requirePattern(initializer, /new StudentData\(studentId, studentName\)/, 'student-field-order');
requirePattern(initializer, /new StudentData\("/, 'literal-student-roster', true);
requirePattern(initializer, /saveUser\("demo\.teacher", "演示教师", "DemoTeacher123!", "TEACHER"\)/, 'demo-teacher');
requirePattern(initializer, /saveUser\(data\.getId\(\), data\.getName\(\), data\.getId\(\), "STUDENT"\)/, 'demo-student-role');
requirePattern('src/main/java/com/_1/controller/SettingsController.java', /addAttribute\("developers", "项目开发团队"\)/, 'neutral-developer-credit');

const properties = 'src/main/resources/application.properties';
requirePattern(properties, /^spring\.datasource\.username=\$\{DB_USERNAME:root\}\r?$/m, 'database-user-config');
requirePattern(properties, /^spring\.datasource\.password=\$\{DB_PASSWORD\}\r?$/m, 'database-password-config');
requirePattern(properties, /^deepseek\.api\.key=\$\{DEEPSEEK_API_KEY:\}\r?$/m, 'deepseek-key-config');
requirePattern(properties, /^openai\.api\.key=\$\{OPENAI_API_KEY:\}\r?$/m, 'openai-key-config');
requirePattern('docker-compose.yml', /MYSQL_ROOT_PASSWORD: \$\{DB_PASSWORD:\?/, 'mysql-container-password-config');

const accounts = 'docs/系统账号密码说明.md';
requirePattern(accounts, /虚构演示数据/, 'demo-data-notice');
requirePattern(accounts, /用户名\*\*：990000010\r?\n- \*\*密码\*\*：990000010/, 'demo-student-login-doc');
requirePattern(accounts, /"username": "demo\.teacher"/, 'demo-teacher-login-doc');
requirePattern(accounts, /"name": "演示教师"/, 'demo-teacher-display-doc');
requirePattern('src/main/resources/templates/scores.html', /score\.studentNumber\}">990000010</, 'demo-score-student-id');
requirePattern('src/main/resources/templates/scores.html', /score\.studentName\}">示例学生010</, 'demo-score-student-name');

if (violations.length) {
  console.error(JSON.stringify(violations, null, 2));
  process.exitCode = 1;
} else {
  console.log('Public-data checks passed: synthetic identities, credential configuration, and sample consistency.');
  console.log('This check does not anonymize runtime databases, inspect Git history/images, or detect every possible personal name.');
}
