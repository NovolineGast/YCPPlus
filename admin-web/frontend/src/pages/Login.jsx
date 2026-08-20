import { useState } from 'react';
import { auth } from '../api';
import styles from './Login.module.css';

function Login({ onLogin }) {
  const [isInitMode, setIsInitMode] = useState(false);
  const [appName, setAppName] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = isInitMode
        ? await auth.init(appName, password)
        : await auth.login(appName, password);

      if (isInitMode) {
        setIsInitMode(false);
        setError('Admin initialized. Please log in.');
        setPassword('');
      } else {
        localStorage.setItem('appName', appName);
        onLogin(response.data.token);
      }
    } catch (err) {
      setError(err.response?.data?.error || 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <div className={styles.header}>
          <div className={styles.logo}>
            <div className={styles.logoIcon}>⚡</div>
            <h1 className={styles.title}>YCPPlus</h1>
          </div>
          <p className={styles.subtitle}>
            {isInitMode ? 'Initialize Admin Account' : 'Authorization Management'}
          </p>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.field}>
            <label htmlFor="appName" className={styles.label}>
              Application Name
            </label>
            <input
              id="appName"
              type="text"
              value={appName}
              onChange={(e) => setAppName(e.target.value)}
              className={styles.input}
              placeholder="MyApp"
              required
              autoFocus
            />
          </div>

          <div className={styles.field}>
            <label htmlFor="password" className={styles.label}>
              Password
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={styles.input}
              placeholder="••••••••"
              required
            />
          </div>

          {error && (
            <div className={styles.error}>
              {error}
            </div>
          )}

          <button
            type="submit"
            className={styles.submit}
            disabled={loading}
          >
            {loading ? 'Please wait...' : isInitMode ? 'Initialize' : 'Sign In'}
          </button>

          <button
            type="button"
            className={styles.toggle}
            onClick={() => {
              setIsInitMode(!isInitMode);
              setError('');
            }}
          >
            {isInitMode ? 'Already have an account? Sign in' : 'First time? Initialize admin'}
          </button>
        </form>
      </div>

      <div className={styles.scanlines} />
    </div>
  );
}

export default Login;
