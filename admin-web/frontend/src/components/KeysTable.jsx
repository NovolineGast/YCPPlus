import { useState } from 'react';
import styles from './KeysTable.module.css';

function KeysTable({ keys, onBan, onUnban, onDelete }) {
  const [selectedKey, setSelectedKey] = useState(null);

  const getStatusColor = (status) => {
    switch (status) {
      case 'active': return 'var(--cipher-safe)';
      case 'expired': return 'var(--text-dim)';
      case 'banned': return 'var(--cipher-alert)';
      default: return 'var(--text-dim)';
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '—';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  };

  const formatDays = (days) => {
    if (days < 0) return 'Expired';
    if (days === 0) return 'Today';
    if (days === 1) return '1 day';
    return `${days} days`;
  };

  if (keys.length === 0) {
    return (
      <div className={styles.empty}>
        <p>No keys generated yet</p>
        <p className={styles.emptyHint}>Click "Generate Keys" to create your first license key</p>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <div className={styles.tableWrapper}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Key</th>
              <th>Status</th>
              <th>Created</th>
              <th>Expires</th>
              <th>Logins</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {keys.map((key) => (
              <tr
                key={key.key}
                className={selectedKey === key.key ? styles.selected : ''}
                onClick={() => setSelectedKey(key.key === selectedKey ? null : key.key)}
              >
                <td>
                  <code className={styles.keyCode}>{key.key}</code>
                </td>
                <td>
                  <span
                    className={styles.status}
                    style={{
                      color: getStatusColor(key.status),
                      borderColor: getStatusColor(key.status)
                    }}
                  >
                    {key.status}
                  </span>
                </td>
                <td className={styles.date}>{formatDate(key.createdAt)}</td>
                <td className={styles.date}>
                  {formatDate(key.expiresAt)}
                  <span className={styles.daysHint}>
                    {formatDays(key.daysUntilExpiry)}
                  </span>
                </td>
                <td className={styles.centered}>{key.loginCount}</td>
                <td>
                  <div className={styles.actions}>
                    {key.status === 'banned' ? (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onUnban(key.key);
                        }}
                        className={styles.actionBtn}
                        title="Unban"
                      >
                        Unban
                      </button>
                    ) : (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onBan(key.key);
                        }}
                        className={styles.actionBtn}
                        title="Ban"
                      >
                        Ban
                      </button>
                    )}
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onDelete(key.key);
                      }}
                      className={`${styles.actionBtn} ${styles.danger}`}
                      title="Delete"
                    >
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default KeysTable;
