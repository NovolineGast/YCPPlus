import styles from './StatsCard.module.css';

function StatsCard({ label, value, variant = 'neutral', hint }) {
  const variantClass = styles[variant] || styles.neutral;

  return (
    <div className={`${styles.card} ${variantClass}`}>
      <div className={styles.label}>
        {label}
        {hint && <span className={styles.hint}>{hint}</span>}
      </div>
      <div className={styles.value}>{value}</div>
    </div>
  );
}

export default StatsCard;
