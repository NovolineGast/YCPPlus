/* ═══════════════════════════════════════════════════════════
   home.js — 首页：性能条动画 + 数字滚动
   ═══════════════════════════════════════════════════════════ */

const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/* 数字滚动 */
function countUp(el) {
  const target = parseInt(el.dataset.count, 10);
  if (Number.isNaN(target)) return;
  if (reduceMotion) {
    el.textContent = target.toLocaleString('en-US');
    return;
  }
  const duration = 1300;
  const start = performance.now();
  const tick = (now) => {
    const p = Math.min((now - start) / duration, 1);
    const eased = 1 - Math.pow(1 - p, 3);
    el.textContent = Math.round(target * eased).toLocaleString('en-US');
    if (p < 1) requestAnimationFrame(tick);
  };
  requestAnimationFrame(tick);
}

/* 性能条：进入视口时填充 + 触发数字动画 */
(function initPerfBars() {
  const bars = document.getElementById('perf-bars');
  if (!bars) return;

  const run = () => {
    bars.classList.add('in-view');
    bars.querySelectorAll('[data-count]').forEach(countUp);
    document.querySelectorAll('.perf-callout [data-count]').forEach(countUp);
  };

  if (reduceMotion || !('IntersectionObserver' in window)) {
    run();
    return;
  }

  const io = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          io.unobserve(entry.target);
          window.setTimeout(run, 150);
        }
      });
    },
    { threshold: 0.35 }
  );

  io.observe(bars);
})();
