/**
 * DORMIGO - Authentication & Session Helper
 */

const Auth = (() => {
  const STORAGE_KEY = 'dormigo_user';

  const getUser = () => {
    try {
      const data = localStorage.getItem(STORAGE_KEY);
      return data ? JSON.parse(data) : null;
    } catch (e) {
      console.error('Error parsing stored user:', e);
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

  const isLoggedIn = () => {
    return !!getUser();
  };

  const getRole = () => {
    const user = getUser();
    return user ? user.user_type : null;
  };

  const getDashboardUrl = (role) => {
    const base = API.getBasePath();
    const r = (role || getRole() || '').toUpperCase();
    if (r === 'STUDENT') return `${base}/student/dashboard.html`;
    if (r === 'LANDLORD') return `${base}/landlord/dashboard.html`;
    if (r === 'ADMIN') return `${base}/admin/dashboard.html`;
    return `${base}/login.html`;
  };

  const logout = () => {
    localStorage.removeItem(STORAGE_KEY);
    const base = API.getBasePath();
    window.location.href = `${base}/login.html`;
  };

  const requireAuth = (allowedRoles = []) => {
    const base = API.getBasePath();
    const user = getUser();

    if (!user) {
      window.location.href = `${base}/login.html?redirect=${encodeURIComponent(window.location.pathname)}`;
      return null;
    }

    if (allowedRoles.length > 0) {
      const userRole = (user.user_type || '').toUpperCase();
      const normalizedAllowed = allowedRoles.map(r => r.toUpperCase());

      if (!normalizedAllowed.includes(userRole)) {
        // Redirect to user's proper dashboard
        window.location.href = getDashboardUrl(userRole);
        return null;
      }
    }

    return user;
  };

  const redirectIfLoggedIn = () => {
    const user = getUser();
    if (user && user.user_type) {
      window.location.href = getDashboardUrl(user.user_type);
    }
  };

  return {
    getUser,
    setUser,
    isLoggedIn,
    getRole,
    getDashboardUrl,
    logout,
    requireAuth,
    redirectIfLoggedIn
  };
})();
