/* ═══════════════════════════════════════════════════════════
   docs.js — 文档页：目录滚动监听（scrollspy）+ 面包屑同步 +
   平滑锚点滚动
   ═══════════════════════════════════════════════════════════ */

(function initDocsNav() {
  const sections = Array.from(document.querySelectorAll('.docs-section[id]'));
  const navLinks = Array.from(document.querySelectorAll('.docs-nav a'));
  const breadcrumbCurrent = document.getElementById('breadcrumb-current');
  if (!sections.length || !navLinks.length) return;

  const sectionTitles = {};
  sections.forEach((s) => {
    const h = s.querySelector('h2');
    if (h) sectionTitles[s.id] = h.textContent.trim();
  });

  /* 平滑锚点滚动（带顶栏 + 面包屑偏移） */
  navLinks.forEach((link) => {
    link.addEventListener('click', (e) => {
      const id = link.getAttribute('href').slice(1);
      const target = document.getElementById(id);
      if (!target) return;
      e.preventDefault();
      const offset = 96;
      const top = target.getBoundingClientRect().top + window.scrollY - offset;
      window.scrollTo({
        top,
        behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches
          ? 'auto'
          : 'smooth',
      });
      history.replaceState(null, '', '#' + id);
    });
  });

  /* scrollspy */
  let ticking = false;
  const spy = () => {
    ticking = false;
    const line = window.scrollY + 120;
    let current = sections[0] ? sections[0].id : '';
    sections.forEach((section) => {
      if (section.offsetTop <= line) current = section.id;
    });
    navLinks.forEach((link) => {
      const active = link.getAttribute('href') === '#' + current;
      link.classList.toggle('nav-active', active);
      if (active) link.setAttribute('aria-current', 'true');
      else link.removeAttribute('aria-current');
    });
    if (breadcrumbCurrent && sectionTitles[current]) {
      breadcrumbCurrent.textContent = sectionTitles[current];
    }
  };

  window.addEventListener(
    'scroll',
    () => {
      if (!ticking) {
        ticking = true;
        requestAnimationFrame(spy);
      }
    },
    { passive: true }
  );

  spy();

  /* 若带 hash 进入，定位到对应章节 */
  if (location.hash) {
    const target = document.getElementById(location.hash.slice(1));
    if (target) {
      window.setTimeout(() => {
        window.scrollTo({ top: target.getBoundingClientRect().top + window.scrollY - 96 });
      }, 60);
    }
  }
})();
