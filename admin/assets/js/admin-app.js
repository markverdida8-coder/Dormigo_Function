/**
 * DORMIGO ADMIN PORTAL - Common Utilities
 */

const AdminApp = (() => {

  const escapeHtml = (unsafe) => {
    if (unsafe === null || unsafe === undefined) return '';
    return String(unsafe)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  };

  const formatCurrency = (amount) => {
    const val = parseFloat(amount);
    if (isNaN(val)) return '₱0.00';
    return '₱' + val.toLocaleString('en-PH', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return dateStr;
      return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    } catch { return dateStr; }
  };

  const formatDateTime = (dateStr) => {
    if (!dateStr) return 'N/A';
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return dateStr;
      return d.toLocaleDateString('en-US', {
        month: 'short', day: 'numeric', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
      });
    } catch { return dateStr; }
  };

  const statusBadge = (status) => {
    if (!status) return '<span class="adm-badge adm-badge-muted">—</span>';
    const s = String(status).toUpperCase();
    const map = {
      AVAILABLE:  'success', ACTIVE:    'success', APPROVED:  'success',
      PAID:       'success', CONFIRMED: 'success', VERIFIED:  'success',
      PENDING:    'warning', MAINTENANCE: 'warning',
      OCCUPIED:   'danger',  DECLINED:  'danger',  REJECTED:  'danger', FAILED: 'danger',
      CANCELLED:  'muted',   INACTIVE:  'muted',   COMPLETED: 'muted'
    };
    const iconMap = {
      success: 'fa-circle-check', warning: 'fa-clock', danger: 'fa-circle-xmark', muted: 'fa-circle-minus'
    };
    const type = map[s] || 'muted';
    return `<span class="adm-badge adm-badge-${type}"><i class="fa-solid ${iconMap[type]}"></i> ${s}</span>`;
  };

  const userTypeBadge = (type) => {
    const t = String(type || '').toUpperCase();
    if (t === 'ADMIN') return `<span class="adm-badge adm-badge-amber"><i class="fa-solid fa-shield-halved"></i> ADMIN</span>`;
    if (t === 'LANDLORD') return `<span class="adm-badge adm-badge-blue"><i class="fa-solid fa-house-user"></i> LANDLORD</span>`;
    return `<span class="adm-badge adm-badge-green"><i class="fa-solid fa-user-graduate"></i> STUDENT</span>`;
  };

  const starRating = (rating) => {
    const r = parseInt(rating) || 0;
    let stars = '';
    for (let i = 1; i <= 5; i++) {
      stars += `<i class="fa-${i <= r ? 'solid' : 'regular'} fa-star" style="color: ${i <= r ? '#F59E0B' : '#CBD5E1'}; font-size: 0.85rem;"></i>`;
    }
    return `<span style="display: inline-flex; gap: 2px;">${stars}</span>`;
  };

  const resolvePhotoUrl = (path) => {
    if (!path) return 'https://images.unsplash.com/photo-1555854877-bab0e564b8d5?auto=format&fit=crop&w=800&q=80';
    if (path.startsWith('http://') || path.startsWith('https://')) return path;
    const base = AdminAPI.getBasePath();
    const cleanPath = path.startsWith('/') ? path.slice(1) : path;
    return `${base}/php_backend/${cleanPath}`;
  };

  const avatarInitials = (name) => {
    if (!name) return '?';
    const parts = name.trim().split(' ');
    if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    return name[0].toUpperCase();
  };

  const toast = (message, type = 'success') => {
    let container = document.getElementById('adm-toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'adm-toast-container';
      container.style.cssText = 'position:fixed;top:1.25rem;right:1.25rem;z-index:9999;display:flex;flex-direction:column;gap:0.5rem;pointer-events:none;';
      document.body.appendChild(container);
    }
    const colors = {
      success: { bg: '#1B5E4C', icon: 'fa-circle-check' },
      error:   { bg: '#DC2626', icon: 'fa-triangle-exclamation' },
      warning: { bg: '#D97706', icon: 'fa-bell' },
      info:    { bg: '#2563EB', icon: 'fa-circle-info' }
    };
    const c = colors[type] || colors.success;
    const el = document.createElement('div');
    el.style.cssText = `background:${c.bg};color:white;padding:0.85rem 1.25rem;border-radius:12px;font-size:0.9rem;font-weight:600;display:flex;align-items:center;gap:0.75rem;box-shadow:0 8px 24px rgba(0,0,0,0.18);pointer-events:all;min-width:260px;max-width:380px;transition:all 0.3s;`;
    el.innerHTML = `<i class="fa-solid ${c.icon}" style="font-size:1.1rem;flex-shrink:0;"></i><span style="flex:1;">${escapeHtml(message)}</span><button onclick="this.parentElement.remove()" style="background:none;border:none;color:rgba(255,255,255,0.7);font-size:1.1rem;cursor:pointer;padding:0;line-height:1;">&times;</button>`;
    container.appendChild(el);
    setTimeout(() => { el.style.opacity = '0'; setTimeout(() => el.remove(), 300); }, 4000);
  };

  const confirm = (message, onConfirm, options = {}) => {
    const existing = document.getElementById('adm-confirm-modal');
    if (existing) existing.remove();

    const modal = document.createElement('div');
    modal.id = 'adm-confirm-modal';
    modal.style.cssText = 'position:fixed;inset:0;background:rgba(15,23,42,0.6);z-index:9998;display:flex;align-items:center;justify-content:center;padding:1.5rem;';
    modal.innerHTML = `
      <div style="background:white;border-radius:16px;padding:2rem;max-width:440px;width:100%;box-shadow:0 24px 60px rgba(0,0,0,0.2);">
        <div style="display:flex;align-items:center;gap:1rem;margin-bottom:1rem;">
          <div style="width:48px;height:48px;border-radius:50%;background:${options.danger ? '#FEE2E2' : '#FEF3C7'};color:${options.danger ? '#DC2626' : '#D97706'};display:flex;align-items:center;justify-content:center;font-size:1.35rem;flex-shrink:0;">
            <i class="fa-solid ${options.danger ? 'fa-triangle-exclamation' : 'fa-circle-question'}"></i>
          </div>
          <div>
            <h4 style="font-size:1.1rem;font-weight:700;color:#1E293B;margin:0 0 0.2rem;">${escapeHtml(options.title || 'Confirm Action')}</h4>
            <p style="font-size:0.9rem;color:#64748B;margin:0;">${escapeHtml(message)}</p>
          </div>
        </div>
        ${options.inputLabel ? `
        <div style="margin-bottom:1.25rem;">
          <label style="display:block;font-size:0.875rem;font-weight:600;color:#1E293B;margin-bottom:0.4rem;">${escapeHtml(options.inputLabel)}</label>
          <textarea id="adm-confirm-input" placeholder="${escapeHtml(options.inputPlaceholder || '')}" style="width:100%;padding:0.65rem 1rem;border:1px solid #E2E8F0;border-radius:10px;font-family:inherit;font-size:0.9rem;resize:vertical;min-height:80px;outline:none;" rows="3"></textarea>
        </div>` : ''}
        <div style="display:flex;gap:0.75rem;justify-content:flex-end;">
          <button id="adm-confirm-cancel" style="padding:0.6rem 1.25rem;background:#F1F5F9;color:#475569;border:none;border-radius:10px;font-weight:600;font-size:0.9rem;cursor:pointer;">Cancel</button>
          <button id="adm-confirm-ok" style="padding:0.6rem 1.25rem;background:${options.danger ? '#DC2626' : '#1B5E4C'};color:white;border:none;border-radius:10px;font-weight:600;font-size:0.9rem;cursor:pointer;">${escapeHtml(options.confirmText || 'Confirm')}</button>
        </div>
      </div>
    `;
    document.body.appendChild(modal);

    modal.querySelector('#adm-confirm-cancel').onclick = () => modal.remove();
    modal.querySelector('#adm-confirm-ok').onclick = () => {
      const inputEl = modal.querySelector('#adm-confirm-input');
      const inputVal = inputEl ? inputEl.value.trim() : null;
      modal.remove();
      onConfirm(inputVal);
    };
    modal.onclick = (e) => { if (e.target === modal) modal.remove(); };
  };

  const showLoading = (containerId, message = 'Loading...') => {
    const el = document.getElementById(containerId);
    if (el) el.innerHTML = `
      <div style="text-align:center;padding:3rem;color:#64748B;">
        <i class="fa-solid fa-spinner fa-spin" style="font-size:1.5rem;margin-bottom:0.75rem;display:block;color:#1B5E4C;"></i>
        ${escapeHtml(message)}
      </div>`;
  };

  const showEmpty = (containerId, message = 'No data found.', icon = 'fa-inbox') => {
    const el = document.getElementById(containerId);
    if (el) el.innerHTML = `
      <div style="text-align:center;padding:3rem;color:#94A3B8;">
        <i class="fa-solid ${icon}" style="font-size:2rem;margin-bottom:0.75rem;display:block;opacity:0.5;"></i>
        ${escapeHtml(message)}
      </div>`;
  };

  const showError = (containerId, message = 'Failed to load data.') => {
    const el = document.getElementById(containerId);
    if (el) el.innerHTML = `
      <div style="text-align:center;padding:3rem;color:#DC2626;">
        <i class="fa-solid fa-triangle-exclamation" style="font-size:2rem;margin-bottom:0.75rem;display:block;opacity:0.6;"></i>
        ${escapeHtml(message)}
      </div>`;
  };

  // Render sidebar user info
  const renderSidebarUser = (user) => {
    const nameEl = document.getElementById('sidebar-admin-name');
    const roleEl = document.getElementById('sidebar-admin-role');
    const avatarEl = document.getElementById('sidebar-admin-avatar');
    if (nameEl) nameEl.textContent = user.full_name || 'Administrator';
    if (roleEl) roleEl.textContent = user.user_type || 'Admin';
    if (avatarEl) {
      if (user.profile_image) {
        avatarEl.innerHTML = `<img src="${resolvePhotoUrl(user.profile_image)}" alt="${escapeHtml(user.full_name)}" style="width:100%;height:100%;object-fit:cover;border-radius:50%;">`;
      } else {
        avatarEl.textContent = avatarInitials(user.full_name);
      }
    }
  };

  return {
    escapeHtml, formatCurrency, formatDate, formatDateTime,
    statusBadge, userTypeBadge, starRating, resolvePhotoUrl,
    avatarInitials, toast, confirm, showLoading, showEmpty, showError,
    renderSidebarUser
  };
})();
