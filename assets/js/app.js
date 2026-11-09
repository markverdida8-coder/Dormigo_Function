/**
 * DORMIGO - Common Application Utilities
 */

const App = (() => {
  /**
   * Escape HTML to prevent XSS attacks
   */
  const escapeHtml = (unsafe) => {
    if (unsafe === null || unsafe === undefined) return '';
    return String(unsafe)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  };

  /**
   * Format numbers to Philippine Pesos (PHP)
   */
  const formatCurrency = (amount) => {
    const val = parseFloat(amount);
    if (isNaN(val)) return '₱0.00';
    return '₱' + val.toLocaleString('en-PH', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
  };

  /**
   * Format date strings
   */
  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return dateStr;
      return d.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
      });
    } catch {
      return dateStr;
    }
  };

  const formatDateTime = (dateStr) => {
    if (!dateStr) return 'N/A';
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return dateStr;
      return d.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return dateStr;
    }
  };

  /**
   * Badge helper for all Dormigo entities
   */
  const statusBadge = (status) => {
    if (!status) return '<span class="badge badge-secondary">Unknown</span>';
    const s = String(status).toUpperCase();

    // Map status to badge styles
    switch (s) {
      case 'AVAILABLE':
      case 'ACTIVE':
      case 'APPROVED':
      case 'PAID':
      case 'CONFIRMED':
      case 'VERIFIED':
        return `<span class="badge badge-success"><i class="fa-solid fa-circle-check"></i> ${s}</span>`;

      case 'PENDING':
        return `<span class="badge badge-warning"><i class="fa-solid fa-clock"></i> ${s}</span>`;

      case 'OCCUPIED':
      case 'DECLINED':
      case 'REJECTED':
      case 'FAILED':
        return `<span class="badge badge-danger"><i class="fa-solid fa-circle-xmark"></i> ${s}</span>`;

      case 'MAINTENANCE':
        return `<span class="badge badge-warning"><i class="fa-solid fa-screwdriver-wrench"></i> ${s}</span>`;

      case 'CANCELLED':
      case 'INACTIVE':
      case 'COMPLETED':
      default:
        return `<span class="badge badge-muted"><i class="fa-solid fa-circle-minus"></i> ${s}</span>`;
    }
  };

  /**
   * Toast notification helper
   */
  const toast = (message, type = 'success') => {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      container.className = 'toast-container';
      document.body.appendChild(container);
    }

    const toastEl = document.createElement('div');
    toastEl.className = `toast toast-${type}`;

    let icon = 'fa-circle-check';
    if (type === 'error' || type === 'danger') icon = 'fa-triangle-exclamation';
    if (type === 'info') icon = 'fa-circle-info';
    if (type === 'warning') icon = 'fa-bell';

    toastEl.innerHTML = `
      <i class="fa-solid ${icon}"></i>
      <div class="toast-message">${escapeHtml(message)}</div>
      <button class="toast-close" onclick="this.parentElement.remove()">&times;</button>
    `;

    container.appendChild(toastEl);

    setTimeout(() => {
      toastEl.classList.add('toast-fade');
      setTimeout(() => toastEl.remove(), 300);
    }, 4000);
  };

  /**
   * Helper to resolve property photo path
   */
  const resolvePhotoUrl = (path) => {
    if (!path) {
      return 'https://images.unsplash.com/photo-1555854877-bab0e564b8d5?auto=format&fit=crop&w=800&q=80';
    }
    if (path.startsWith('http://') || path.startsWith('https://')) {
      return path;
    }
    const base = API.getBasePath();
    const cleanPath = path.startsWith('/') ? path.slice(1) : path;
    return `${base}/php_backend/${cleanPath}`;
  };

  /**
   * Render public navigation bar state
   */
  const renderNavAuth = () => {
    const navAuth = document.getElementById('nav-auth-container');
    if (!navAuth) return;

    const user = Auth.getUser();
    const base = API.getBasePath();

    if (user) {
      const dashboardUrl = Auth.getDashboardUrl(user.user_type);
      navAuth.innerHTML = `
        <div class="dropdown user-nav-dropdown">
          <a href="${dashboardUrl}" class="btn btn-outline-primary btn-sm">
            <i class="fa-solid fa-gauge-high me-1"></i> Dashboard
          </a>
          <button class="btn btn-primary btn-sm" onclick="Auth.logout()">
            <i class="fa-solid fa-right-from-bracket"></i>
          </button>
        </div>
      `;
    } else {
      navAuth.innerHTML = `
        <a href="${base}/login.html" class="btn btn-outline-primary btn-sm">Log In</a>
        <a href="${base}/register.html" class="btn btn-primary btn-sm">Sign Up</a>
      `;
    }
  };

  return {
    escapeHtml,
    formatCurrency,
    formatDate,
    formatDateTime,
    statusBadge,
    toast,
    resolvePhotoUrl,
    renderNavAuth
  };
})();
