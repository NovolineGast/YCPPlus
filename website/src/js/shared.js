/* ═══════════════════════════════════════════════════════════
   shared.js — 主题切换 / 移动端菜单 / 滚动状态 / 返回顶部 /
   滚动显现动画 / 代码复制
   ═══════════════════════════════════════════════════════════ */

const root = document.documentElement;
const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/* ── 主题切换（localStorage 持久化，初始值由 head 内联脚本设置） ── */
(function initThemeToggle() {
  const KEY = 'ycp-theme';
  document.querySelectorAll('[data-theme-toggle]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const dark = !root.classList.contains('dark');
      root.classList.add('theme-anim');
      root.classList.toggle('dark', dark);
      window.setTimeout(() => root.classList.remove('theme-anim'), 420);
      try {
        localStorage.setItem(KEY, dark ? 'dark' : 'light');
      } catch (e) {
        /* 隐私模式下忽略 */
      }
    });
  });
})();

/* ── 顶栏滚动状态 ── */
(function initHeaderState() {
  const header = document.getElementById('site-header');
  if (!header) return;
  const update = () => header.classList.toggle('is-scrolled', window.scrollY > 8);
  update();
  window.addEventListener('scroll', update, { passive: true });
})();

/* ── 移动端菜单 ── */
(function initMobileMenu() {
  const toggle = document.getElementById('menu-toggle');
  const menu = document.getElementById('mobile-menu');
  if (!toggle || !menu) return;

  const setState = (open) => {
    toggle.setAttribute('aria-expanded', String(open));
    toggle.setAttribute('aria-label', open ? '关闭菜单' : '打开菜单');
    menu.classList.toggle('is-open', open);
  };

  toggle.addEventListener('click', (e) => {
    e.stopPropagation();
    setState(toggle.getAttribute('aria-expanded') !== 'true');
  });

  menu.querySelectorAll('a').forEach((a) => a.addEventListener('click', () => setState(false)));

  document.addEventListener('click', (e) => {
    if (
      menu.classList.contains('is-open') &&
      !menu.contains(e.target) &&
      !toggle.contains(e.target)
    ) {
      setState(false);
    }
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && menu.classList.contains('is-open')) setState(false);
  });

  window.addEventListener('resize', () => {
    if (window.innerWidth > 640 && menu.classList.contains('is-open')) setState(false);
  });
})();

/* ── 返回顶部 ── */
(function initBackToTop() {
  const btn = document.getElementById('back-to-top');
  if (!btn) return;
  const update = () => btn.classList.toggle('is-visible', window.scrollY > 600);
  update();
  window.addEventListener('scroll', update, { passive: true });
  btn.addEventListener('click', () => {
    window.scrollTo({ top: 0, behavior: reduceMotion ? 'auto' : 'smooth' });
  });
})();

/* ── 滚动显现动画 ── */
(function initReveal() {
  const els = document.querySelectorAll('.reveal');
  if (!els.length) return;

  if (reduceMotion || !('IntersectionObserver' in window)) {
    els.forEach((el) => el.classList.add('revealed'));
    return;
  }

  const io = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('revealed');
          io.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.1, rootMargin: '0px 0px -6% 0px' }
  );

  els.forEach((el) => io.observe(el));
})();

/* ── 代码块复制按钮 ── */
(function initCodeCopy() {
  const ICON_COPY =
    '<svg class="icon-copy" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect width="14" height="14" x="8" y="8" rx="2"/><path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/></svg>';
  const ICON_CHECK =
    '<svg class="icon-check" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 6 9 17l-5-5"/></svg>';

  const copyText = async (text) => {
    if (navigator.clipboard && window.isSecureContext) {
      try {
        await navigator.clipboard.writeText(text);
        return true;
      } catch (e) {
        /* 降级 */
      }
    }
    try {
      const ta = document.createElement('textarea');
      ta.value = text;
      ta.style.position = 'fixed';
      ta.style.opacity = '0';
      document.body.appendChild(ta);
      ta.select();
      const ok = document.execCommand('copy');
      document.body.removeChild(ta);
      return ok;
    } catch (e) {
      return false;
    }
  };

  document.querySelectorAll('.code-block').forEach((block) => {
    const code = block.querySelector('code') || block.querySelector('pre') || block;
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'code-copy';
    btn.setAttribute('aria-label', '复制代码');
    btn.innerHTML = ICON_COPY + ICON_CHECK + '<span class="code-copy-text">复制</span>';
    block.appendChild(btn);

    btn.addEventListener('click', async () => {
      const ok = await copyText(code.innerText.replace(/\n{3,}/g, '\n\n').trimEnd());
      btn.classList.toggle('is-copied', ok);
      const label = btn.querySelector('.code-copy-text');
      label.textContent = ok ? '已复制' : '复制失败';
      window.setTimeout(() => {
        btn.classList.remove('is-copied');
        label.textContent = '复制';
      }, 1600);
    });
  });
})();
