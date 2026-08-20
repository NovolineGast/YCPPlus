import { useState, useEffect } from 'react';
import { dashboard, keys } from '../api';
import styles from './Dashboard.module.css';
import StatsCard from '../components/StatsCard';
import KeysTable from '../components/KeysTable';
import GenerateModal from '../components/GenerateModal';

function Dashboard({ onLogout }) {
  const [stats, setStats] = useState(null);
  const [keysList, setKeysList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const appName = localStorage.getItem('appName') || 'Admin';

  const loadData = async () => {
    try {
      const [statsRes, keysRes] = await Promise.all([
        dashboard.getStats(),
        keys.getAll()
      ]);
      setStats(statsRes.data);
      setKeysList(keysRes.data);
    } catch (err) {
      console.error('Failed to load data:', err);
      if (err.response?.status === 401) {
        onLogout();
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleGenerate = async (amount, days) => {
    await keys.generate(amount, days);
    await loadData();
  };

  const handleBan = async (key) => {
    await keys.ban(key);
    await loadData();
  };

  const handleUnban = async (key) => {
    await keys.unban(key);
    await loadData();
  };

  const handleDelete = async (key) => {
    if (confirm(`Delete key ${key}?`)) {
      await keys.delete(key);
      await loadData();
    }
  };

  if (loading) {
    return (
      <div className={styles.loading}>
        <div className={styles.spinner} />
        <p>Loading dashboard...</p>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.headerLeft}>
          <div className={styles.logo}>
            <div className={styles.logoIcon}>⚡</div>
            <h1>YCPPlus</h1>
          </div>
          <div className={styles.appBadge}>{appName}</div>
        </div>
        <button onClick={onLogout} className={styles.logout}>
          Sign Out
        </button>
      </header>

      <main className={styles.main}>
        <section className={styles.stats}>
          <StatsCard
            label="Total Keys"
            value={stats.totalKeys}
            variant="neutral"
          />
          <StatsCard
            label="Active"
            value={stats.activeKeys}
            variant="safe"
          />
          <StatsCard
            label="Expiring Soon"
            value={stats.expiringSoon}
            variant="alert"
            hint="within 7 days"
          />
          <StatsCard
            label="Total Logins"
            value={stats.totalLogins}
            variant="neutral"
          />
        </section>

        <section className={styles.actions}>
          <button
            onClick={() => setShowModal(true)}
            className={styles.primaryAction}
          >
            Generate Keys
          </button>
          <button
            onClick={loadData}
            className={styles.secondaryAction}
          >
            Refresh
          </button>
        </section>

        <KeysTable
          keys={keysList}
          onBan={handleBan}
          onUnban={handleUnban}
          onDelete={handleDelete}
        />
      </main>

      {showModal && (
        <GenerateModal
          onGenerate={handleGenerate}
          onClose={() => setShowModal(false)}
        />
      )}
    </div>
  );
}

export default Dashboard;
