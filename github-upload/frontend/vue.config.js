const { defineConfig } = require('@vue/cli-service')

// 后端地址：开发时把 /api、/questions 转发过去，浏览器看到的是同源请求，会话 Cookie 和 CSRF 令牌都能正常工作
const backendTarget = process.env.VUE_APP_BACKEND_URL || 'http://localhost:8080'

module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false, // 关闭 ESLint 保存时检查

  devServer: {
    port: 8083, // 设置开发服务器端口为8083（与错误信息一致）
    host: 'localhost',
    open: false, // 不自动打开浏览器
    hot: true, // 启用热重载
    historyApiFallback: true, // 支持HTML5 History API
    proxy: {
      '/api': { target: backendTarget },
      '/questions': { target: backendTarget }
    },
    allowedHosts: 'all', // 允许所有主机访问
    client: {
      overlay: {
        errors: true,
        warnings: false
      }
    },
    // 添加静态文件服务配置
    static: {
      directory: require('path').join(__dirname, 'dist'),
      publicPath: '/'
    }
  },

  configureWebpack: {
    optimization: {
      splitChunks: {
        chunks: 'all',
        maxSize: 244000, // 限制chunk大小，避免过大的chunk
        cacheGroups: {
          vendor: {
            name: 'chunk-vendors',
            test: /[\\/]node_modules[\\/]/,
            priority: 10,
            chunks: 'initial'
          },
          common: {
            name: 'chunk-common',
            minChunks: 2,
            priority: 5,
            chunks: 'initial',
            reuseExistingChunk: true
          },
          // 添加views模块的分组
          views: {
            name: 'chunk-views',
            test: /[\\/]src[\\/]views[\\/]/,
            priority: 8,
            chunks: 'async',
            reuseExistingChunk: true
          }
        }
      }
    },
    // 添加resolve配置，确保模块解析正确
    resolve: {
      alias: {
        '@': require('path').resolve(__dirname, 'src')
      }
    }
  },

  // 修复chunk加载问题
  publicPath: process.env.NODE_ENV === 'production' ? './' : '/',

  // 输出目录
  outputDir: 'dist',

  // 静态资源目录
  assetsDir: 'static'
})