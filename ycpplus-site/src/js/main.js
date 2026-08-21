/* ═══════════════════════════════════════════════════════════
   main.js — 共享交互：图标渲染 / 移动端菜单 / 滚动动画 /
   性能条动画 / 数字计数 / 代码复制 / 返回顶部
   ═══════════════════════════════════════════════════════════ */

const ICONS = {
  check: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><path d="m9 11 3 3L22 4"/></svg>',
  copy: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect width="14" height="14" x="8" y="8" rx="2" ry="2"/><path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/></svg>',
  checkSmall: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 6 9 17l-5-5"/></svg>',
  arrowUp: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m5 12 7-7 7 7"/><path d="M12 19V5"/></svg>'
}

const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

/* ===== 图标渲染（内联 SVG，替代 CDN 依赖） ===== */
document.querySelectorAll('[data-icon]').forEach((el) => {
  const icon = ICONS[el.dataset.icon]
  if (icon) el.innerHTML = icon
})

/* ===== 移动端菜单 ===== */
const menuToggle = document.querySelector('.menu-toggle')
const mobileMenu = document.getElementById('mobile-menu')

if (menuToggle && mobileMenu) {
  const setMenuOpen = (open) => {
    menuToggle.setAttribute('aria-expanded', String(open))
    menuToggle.setAttribute('aria-label', open ? '关闭菜单' : '打开菜单')
    mobileMenu.classList.toggle('open', open)
  }

  menuToggle.addEventListener('click', () => {
    setMenuOpen(menuToggle.getAttribute('aria-expanded') !== 'true')
  })

  mobileMenu.querySelectorAll('a').forEach((link) => {
    link.addEventListener('click', () => setMenuOpen(false))
  })

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') setMenuOpen(false)
  })

  document.addEventListener('click', (e) => {
    if (mobileMenu.classList.contains('open') && !e.target.closest('.site-header')) {
      setMenuOpen(false)
    }
  })

  window.addEventListener('resize', () => {
    if (window.innerWidth > 640) setMenuOpen(false)
  })
}

/* ===== 滚动进入动画（同级元素自动错峰） ===== */
const revealEls = document.querySelectorAll('.reveal')

revealEls.forEach((el) => {
  const siblings = Array.from(el.parentElement.children).filter((c) => c.classList.contains('reveal'))
  const index = siblings.indexOf(el)
  if (index > 0) el.style.setProperty('--reveal-delay', `${Math.min(index * 80, 320)}ms`)
})

if (prefersReducedMotion || !('IntersectionObserver' in window)) {
  revealEls.forEach((el) => el.classList.add('is-visible'))
} else {
  const revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible')
          revealObserver.unobserve(entry.target)
        }
      })
    },
    { threshold: 0.12, rootMargin: '0px 0px -8% 0px' }
  )
  revealEls.forEach((el) => revealObserver.observe(el))
}

/* ===== 性能对比条动画 ===== */
const perfBars = document.querySelector('.perf-bars')
const perfFills = document.querySelectorAll('.perf-bar-fill')

if (perfFills.length && !prefersReducedMotion) {
  const targets = Array.from(perfFills).map((f) => f.style.width)
  perfFills.forEach((f) => {
    f.style.width = '0%'
  })

  const animateBars = () => {
    perfFills.forEach((f, i) => {
      setTimeout(() => {
        f.style.width = targets[i]
      }, 150 + i * 180)
    })
  }

  if (perfBars && 'IntersectionObserver' in window) {
    const barObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            animateBars()
            barObserver.disconnect()
          }
        })
      },
      { threshold: 0.4 }
    )
    barObserver.observe(perfBars)
  } else {
    animateBars()
  }
}

/* ===== 数字计数动画 ===== */
const counters = document.querySelectorAll('[data-count]')

if (counters.length && !prefersReducedMotion && 'IntersectionObserver' in window) {
  const formatNumber = (n) => n.toLocaleString('en-US')

  const animateCounter = (el) => {
    const target = parseInt(el.dataset.count, 10)
    const suffix = el.dataset.suffix || ''
    const duration = 1400
    const start = performance.now()

    const tick = (now) => {
      const progress = Math.min((now - start) / duration, 1)
      const eased = 1 - Math.pow(1 - progress, 3)
      el.textContent = formatNumber(Math.round(target * eased)) + suffix
      if (progress < 1) requestAnimationFrame(tick)
    }
    requestAnimationFrame(tick)
  }

  const counterObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          animateCounter(entry.target)
          counterObserver.unobserve(entry.target)
        }
      })
    },
    { threshold: 0.6 }
  )
  counters.forEach((el) => counterObserver.observe(el))
}

/* ===== 代码块复制按钮 ===== */
document.querySelectorAll('.code-block').forEach((block) => {
  const btn = document.createElement('button')
  btn.className = 'copy-btn'
  btn.type = 'button'
  btn.setAttribute('aria-label', '复制代码')
  btn.innerHTML = `${ICONS.copy}<span>复制</span>`
  block.appendChild(btn)

  btn.addEventListener('click', async () => {
    const code = block.querySelector('pre')?.innerText ?? block.innerText
    try {
      await navigator.clipboard.writeText(code)
    } catch {
      const textarea = document.createElement('textarea')
      textarea.value = code
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      textarea.remove()
    }
    btn.classList.add('copied')
    btn.innerHTML = `${ICONS.checkSmall}<span>已复制</span>`
    setTimeout(() => {
      btn.classList.remove('copied')
      btn.innerHTML = `${ICONS.copy}<span>复制</span>`
    }, 1800)
  })
})

/* ===== 返回顶部 ===== */
const backToTop = document.getElementById('back-to-top')

if (backToTop) {
  const toggleVisibility = () => {
    backToTop.classList.toggle('visible', window.scrollY > 600)
  }
  window.addEventListener('scroll', toggleVisibility, { passive: true })
  toggleVisibility()

  backToTop.addEventListener('click', () => {
    window.scrollTo({
      top: 0,
      behavior: prefersReducedMotion ? 'auto' : 'smooth'
    })
  })
}
