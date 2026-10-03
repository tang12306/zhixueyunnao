// 动画工具函数

/**
 * 页面进入动画配置
 */
export const pageEnterAnimation = {
  initial: {
    opacity: 0,
    y: 30
  },
  enter: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 600,
      ease: 'easeOut'
    }
  }
}

/**
 * 卡片悬停动画配置
 */
export const cardHoverAnimation = {
  initial: {
    scale: 1,
    y: 0
  },
  hover: {
    scale: 1.02,
    y: -5,
    transition: {
      duration: 300,
      ease: 'easeOut'
    }
  }
}

/**
 * 按钮点击动画配置
 */
export const buttonClickAnimation = {
  initial: {
    scale: 1
  },
  tap: {
    scale: 0.95,
    transition: {
      duration: 100,
      ease: 'easeInOut'
    }
  }
}

/**
 * 列表项进入动画配置
 */
export const listItemAnimation = {
  initial: {
    opacity: 0,
    x: -20
  },
  enter: {
    opacity: 1,
    x: 0,
    transition: {
      duration: 400,
      ease: 'easeOut'
    }
  }
}

/**
 * 模态框动画配置
 */
export const modalAnimation = {
  initial: {
    opacity: 0,
    scale: 0.8
  },
  enter: {
    opacity: 1,
    scale: 1,
    transition: {
      duration: 300,
      ease: 'easeOut'
    }
  },
  leave: {
    opacity: 0,
    scale: 0.8,
    transition: {
      duration: 200,
      ease: 'easeIn'
    }
  }
}

/**
 * 数字滚动动画
 */
export function animateNumber(element, start, end, duration = 1000) {
  const startTime = performance.now()
  const range = end - start
  
  function updateNumber(currentTime) {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    
    // 使用缓动函数
    const easeOutQuart = 1 - Math.pow(1 - progress, 4)
    const current = Math.round(start + range * easeOutQuart)
    
    element.textContent = current.toLocaleString()
    
    if (progress < 1) {
      requestAnimationFrame(updateNumber)
    }
  }
  
  requestAnimationFrame(updateNumber)
}

/**
 * 渐变背景动画
 */
export const gradientAnimation = {
  initial: {
    backgroundPosition: '0% 50%'
  },
  animate: {
    backgroundPosition: ['0% 50%', '100% 50%', '0% 50%'],
    transition: {
      duration: 3,
      repeat: Infinity,
      ease: 'linear'
    }
  }
}

/**
 * 加载动画配置
 */
export const loadingAnimation = {
  initial: {
    rotate: 0
  },
  animate: {
    rotate: 360,
    transition: {
      duration: 1,
      repeat: Infinity,
      ease: 'linear'
    }
  }
}

/**
 * 弹跳动画配置
 */
export const bounceAnimation = {
  initial: {
    y: 0
  },
  animate: {
    y: [-10, 0, -10],
    transition: {
      duration: 1,
      repeat: Infinity,
      ease: 'easeInOut'
    }
  }
}

/**
 * 脉冲动画配置
 */
export const pulseAnimation = {
  initial: {
    scale: 1,
    opacity: 1
  },
  animate: {
    scale: [1, 1.05, 1],
    opacity: [1, 0.8, 1],
    transition: {
      duration: 2,
      repeat: Infinity,
      ease: 'easeInOut'
    }
  }
}

/**
 * 淡入淡出动画配置
 */
export const fadeAnimation = {
  initial: {
    opacity: 0
  },
  enter: {
    opacity: 1,
    transition: {
      duration: 500,
      ease: 'easeOut'
    }
  },
  leave: {
    opacity: 0,
    transition: {
      duration: 300,
      ease: 'easeIn'
    }
  }
}

/**
 * 滑动动画配置
 */
export const slideAnimation = {
  slideLeft: {
    initial: { x: -100, opacity: 0 },
    enter: { x: 0, opacity: 1, transition: { duration: 400 } }
  },
  slideRight: {
    initial: { x: 100, opacity: 0 },
    enter: { x: 0, opacity: 1, transition: { duration: 400 } }
  },
  slideUp: {
    initial: { y: 100, opacity: 0 },
    enter: { y: 0, opacity: 1, transition: { duration: 400 } }
  },
  slideDown: {
    initial: { y: -100, opacity: 0 },
    enter: { y: 0, opacity: 1, transition: { duration: 400 } }
  }
}

/**
 * 创建交错动画
 */
export function createStaggerAnimation(delay = 100) {
  return {
    initial: {
      opacity: 0,
      y: 20
    },
    enter: (index) => ({
      opacity: 1,
      y: 0,
      transition: {
        duration: 400,
        delay: index * delay,
        ease: 'easeOut'
      }
    })
  }
}

/**
 * 页面切换动画
 */
export const pageTransition = {
  name: 'page',
  mode: 'out-in',
  enterActiveClass: 'animate__animated animate__fadeInRight',
  leaveActiveClass: 'animate__animated animate__fadeOutLeft'
}

/**
 * 工具函数：添加动画类
 */
export function addAnimationClass(element, animationClass, duration = 1000) {
  return new Promise((resolve) => {
    element.classList.add('animate__animated', animationClass)
    
    setTimeout(() => {
      element.classList.remove('animate__animated', animationClass)
      resolve()
    }, duration)
  })
}

/**
 * 工具函数：观察元素进入视口
 */
export function observeElementEntry(element, callback, options = {}) {
  const defaultOptions = {
    threshold: 0.1,
    rootMargin: '0px 0px -50px 0px'
  }
  
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        callback(entry.target)
        observer.unobserve(entry.target)
      }
    })
  }, { ...defaultOptions, ...options })
  
  observer.observe(element)
  return observer
}
