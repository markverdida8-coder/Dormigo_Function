/**
 * DORMIGO ADMIN PORTAL - Admin Authentication Helper
 * Client-side session guard that works WITH the server-side PHP session.
 * Does NOT rely solely on localStorage — it always verifies with the backend.
 */

const AdminAuth = (() => {
  const STORAGE_KEY = 'dormigo_admin_user';

  const getUser = () => {
    try {
      const d = localStorage.getItem(STORAGE_KEY);
      return d ? JSON.parse(d) : null;
    } catch {
      return null;
    }
  };

  const setUser = (user) => {
    if (!user) {
      localStorage.removeItem(STORAGE_KEY);
    } else {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
    }
  };

  /**
   * Verify admin session with backend. Redirect to login if not authenticated.
   * Returns the user object or null.
   */
  const requireAdminAuth = async () => {
    try {
      const res = await AdminAPI.checkAdminSession();
      if (res.success && res.authenticated && res.user) {
        setUser(res.user);
        return res.user;
      }
    } catch (err) {
      console.warn('Admin session check failed:', err);
    }

    // Not authenticated — redirect to admin login
    setUser(null);
    const base = AdminAPI.getBasePath();
    window.location.href = `${base}/admin/login.html`;
    return null;
  };

  /**
   * Login flow — calls backend, sets local cache, returns user.
   */
  const login = async (email, password) => {
    const res = await AdminAPI.adminLogin(email, password);
    if (res.success && res.user) {
      setUser(res.user);
    }
    return res;
  };

  /**
   * Logout — destroys server session and clears local cache.
   */
  const logout = async () => {
    try {
      await AdminAPI.adminLogout();
    } catch {}
    setUser(null);
    const base = AdminAPI.getBasePath();
    window.location.href = `${base}/admin/login.html`;
  };

  /**
   * If already logged in, skip login page.
   */
  const redirectIfLoggedIn = async () => {
    try {
      const res = await AdminAPI.checkAdminSession();
      if (res.success && res.authenticated) {
        const base = AdminAPI.getBasePath();
        window.location.href = `${base}/admin/dashboard.html`;
      }
    } catch {}
  };

  return { getUser, setUser, requireAdminAuth, login, logout, redirectIfLoggedIn };
})();
