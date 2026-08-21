/* ═══════════════════════════════════════════════════════════
   docs.js — 文档页交互：侧边栏平滑滚动 / 滚动监听 /
   面包屑同步
   ═══════════════════════════════════════════════════════════ */

const sections = document.querySelectorAll('section[id]')
const navLinks = document.querySelectorAll('.docs-nav a')
const breadcrumbCurrent = document.getElementById('breadcrumb-current')

const sectionTitles = {
  quickstart: '快速开始',
  annotations: '混淆注解参考',
  authserver: '授权服务器 API',
  deployment: '部署指南'
}

const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

/* 侧边栏点击平滑滚动 */
navLinks.forEach((link) => {
  link.addEventListener('click', (e) => {
    e.preventDefault()
    const target = document.querySelector(link.getAttribute('href'))
    if (target) {
      target.scrollIntoView({
        behavior: prefersReducedMotion ? 'auto' : 'smooth',
        block: 'start'
      })
      history.replaceState(null, '', link.getAttribute('href'))
    }
  })
})

/* 滚动监听：高亮当前章节 + 同步面包屑 */
const onScroll = () => {
  let current = ''
  sections.forEach((section) => {
    const sectionTop = section.getBoundingClientRect().top + window.scrollY - 120
    if (window.scrollY >= sectionTop) current = section.id
  })

  navLinks.forEach((link) => {
    link.classList.toggle('nav-active', link.getAttribute('href') === `#${current}`)
  })

  if (current && breadcrumbCurrent && sectionTitles[current]) {
    breadcrumbCurrent.textContent = sectionTitles[current]
  }
}

window.addEventListener('scroll', onScroll, { passive: true })
onScroll()
