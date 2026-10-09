// vue-cli-service lint 使用的 ESLint 配置。
// .vue 文件由 vue-eslint-parser 解析，<script> 部分交给默认的 espree，不需要 babel 解析器。
module.exports = {
  root: true,
  env: {
    browser: true,
    node: true,
    es2022: true
  },
  parserOptions: {
    ecmaVersion: 2022,
    sourceType: 'module'
  },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-essential'
  ],
  rules: {
    'no-console': 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'warn',
    // 页面文件名如 Index.vue、Create.vue 是单个单词，按路由目录区分即可
    'vue/multi-word-component-names': 'off'
  }
};
