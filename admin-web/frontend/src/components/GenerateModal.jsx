import { useState } from 'react';
import styles from './GenerateModal.module.css';

function GenerateModal({ onGenerate, onClose }) {
  const [amount, setAmount] = useState(1);
  const [days, setDays] = useState(30);
  const [prefix, setPrefix] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (amount < 1 || amount > 50) {
      setError('Amount must be between 1 and 50');
      return;
    }

    if (days < 1 || days > 9999) {
      setError('Days must be between 1 and 9999');
      return;
    }

    setLoading(true);
    try {
      await onGenerate(amount, days, prefix);
      onClose();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to generate keys');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <div className={styles.header}>
          <h2>Generate License Keys</h2>
          <button onClick={onClose} className={styles.close}>×</button>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.field}>
            <label htmlFor="amount">
              Amount
              <span className={styles.hint}>1-50 keys</span>
            </label>
            <input
              id="amount"
              type="number"
              min="1"
              max="50"
              value={amount}
              onChange={(e) => setAmount(parseInt(e.target.value))}
              className={styles.input}
              required
            />
          </div>

          <div className={styles.field}>
            <label htmlFor="days">
              Validity Period (days)
              <span className={styles.hint}>1-9999 days</span>
            </label>
            <input
              id="days"
              type="number"
              min="1"
              max="9999"
              value={days}
              onChange={(e) => setDays(parseInt(e.target.value))}
              className={styles.input}
              required
            />
          </div>

          <div className={styles.field}>
            <label htmlFor="prefix">
              Custom Prefix
              <span className={styles.hint}>Optional, default: YCP</span>
            </label>
            <input
              id="prefix"
              type="text"
              value={prefix}
              onChange={(e) => setPrefix(e.target.value.toUpperCase())}
              className={styles.input}
              placeholder="YCP"
              maxLength="10"
            />
          </div>

          {error && (
            <div className={styles.error}>{error}</div>
          )}

          <div className={styles.actions}>
            <button
              type="button"
              onClick={onClose}
              className={styles.cancel}
            >
              Cancel
            </button>
            <button
              type="submit"
              className={styles.submit}
              disabled={loading}
            >
              {loading ? 'Generating...' : 'Generate'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default GenerateModal;
