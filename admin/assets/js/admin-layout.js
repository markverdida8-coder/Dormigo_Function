/**
 * DORMIGO ADMIN PORTAL - Shared Sidebar Renderer
 * Injects the admin sidebar and topbar into every admin page.
 * Call AdminLayout.init('page-key') at the top of each page.
 */

const AdminLayout = (() => {

  const NAV_ITEMS = [
    {
      section: 'Overview',
      items: [
        { key: 'dashboard', label: 'Dashboard', icon: 'fa-gauge-high', href: 'dashboard.html' }
      ]
    },
    {
      section: 'Users',
      items: [
        { key: 'users-all',       label: 'All Users',   icon: 'fa-users',        href: 'users.html' },
        { key: 'users-students',  label: 'Students',    icon: 'fa-user-graduate', href: 'users.html?tab=students' },
        { key: 'users-landlords', label: 'Landlords',   icon: 'fa-house-user',   href: 'users.html?tab=landlords' }
      ]
    },
    {
      section: 'Verification Center',
      items: [
        { key: 'verif-students',  label: 'Student Verification',  icon: 'fa-id-card',    href: 'verifications-students.html', badgeId: 'nav-badge-sv' },
        { key: 'verif-landlords', label: 'Landlord Verification', icon: 'fa-certificate', href: 'verifications-landlords.html', badgeId: 'nav-badge-lv' }
      ]
    },
    {
      section: 'Monitoring',
      items: [
        { key: 'boarding-houses', label: 'Boarding Houses', icon: 'fa-building',        href: 'boarding-houses.html' },
        { key: 'bookings',        label: 'Bookings',        icon: 'fa-calendar-check',  href: 'bookings.html' },
        { key: 'payments',        label: 'Payments',        icon: 'fa-credit-card',     href: 'payments.html' },
        { key: 'reviews',         label: 'Reviews',         icon: 'fa-star',            href: 'reviews.html' }
      ]
    },
    {
      section: 'System',
      items: [
        { key: 'notifications', label: 'Notifications', icon: 'fa-bell',   href: 'notifications.html' },
        { key: 'profile',       label: 'Admin Profile', icon: 'fa-circle-user', href: 'profile.html' }
      ]
    }
  ];

  const buildSidebar = (activeKey, pageTitle) => {
    const navHtml = NAV_ITEMS.map(group => {
      const itemsHtml = group.items.map(item => {
        const isActive = item.key === activeKey ? ' active' : '';
        const badge = item.badgeId ? `<span class="adm-nav-badge" id="${item.badgeId}" style="display:none;"></span>` : '';
        return `<a href="${item.href}" class="adm-nav-link${isActive}" title="${item.label}">
          <i class="fa-solid ${item.icon}"></i>
          <span>${item.label}</span>
          ${badge}
        </a>`;
      }).join('');
      return `<div class="adm-nav-section">${group.section}</div>${itemsHtml}`;
    }).join('');

    return `
    <!-- SIDEBAR OVERLAY -->
    <div class="adm-sidebar-overlay" id="sidebarOverlay" onclick="AdminLayout.closeSidebar()"></div>

    <!-- SIDEBAR -->
    <aside class="adm-sidebar" id="adminSidebar">
      <div class="adm-sidebar-header">
        <a href="dashboard.html" class="adm-sidebar-brand">
          <div class="adm-brand-icon"><i class="fa-solid fa-house-lock"></i></div>
          <div class="adm-brand-label">
            DORMIGO
            <span>Admin Portal</span>
          </div>
        </a>
        <button class="adm-sidebar-close" onclick="AdminLayout.closeSidebar()">
          <i class="fa-solid fa-xmark"></i>
        </button>
      </div>

      <nav class="adm-sidebar-nav">${navHtml}</nav>

      <div class="adm-sidebar-footer">
        <div class="adm-sidebar-avatar" id="sidebar-admin-avatar">?</div>
        <div class="adm-sidebar-user-info">
          <div class="adm-sidebar-user-name" id="sidebar-admin-name">Administrator</div>
          <div class="adm-sidebar-user-role">System Admin</div>
        </div>
        <button class="adm-logout-btn" onclick="AdminLayout.logout()" title="Logout">
          <i class="fa-solid fa-right-from-bracket"></i>
        </button>
      </div>
    </aside>

    <!-- MAIN WRAPPER -->
    <div class="adm-main">
      <header class="adm-topbar">
        <div class="adm-topbar-left">
          <button class="adm-menu-toggle" onclick="AdminLayout.toggleSidebar()">
            <i class="fa-solid fa-bars"></i>
          </button>
          <h2 class="adm-page-title" id="adm-topbar-title">${pageTitle}</h2>
        </div>
        <div class="adm-topbar-right">
          <a href="notifications.html" class="adm-topbar-badge-btn" title="Notifications">
            <i class="fa-solid fa-bell"></i>
            <span class="adm-notif-dot" id="topbar-notif-dot" style="display:none;"></span>
          </a>
          <a href="profile.html" class="adm-topbar-badge-btn" title="Admin Profile">
            <i class="fa-solid fa-circle-user"></i>
          </a>
        </div>
      </header>
      <main class="adm-content" id="adm-main-content">
    `;
  };

  const endMain = () => `</main></div>`;

  const openSidebar = () => {
    document.getElementById('adminSidebar').classList.add('open');
    document.getElementById('sidebarOverlay').classList.add('show');
    document.body.style.overflow = 'hidden';
  };

  const closeSidebar = () => {
    document.getElementById('adminSidebar').classList.remove('open');
    document.getElementById('sidebarOverlay').classList.remove('show');
    document.body.style.overflow = '';
  };

  const toggleSidebar = () => {
    const sidebar = document.getElementById('adminSidebar');
    if (sidebar.classList.contains('open')) closeSidebar();
    else openSidebar();
  };

  const logout = () => AdminAuth.logout();

  /**
   * Main initialization: inject layout, verify session, load stats badges.
   * @param {string} activeKey - The nav item key to mark active
   * @param {string} pageTitle - Display title in the topbar
   * @returns {Promise<Object>} - The authenticated admin user object
   */
  const init = async (activeKey, pageTitle = 'Admin Portal') => {
    // Inject sidebar + topbar HTML into layout placeholder
    const layoutEl = document.getElementById('adm-layout-body');
    if (layoutEl) {
      layoutEl.innerHTML = buildSidebar(activeKey, pageTitle) + layoutEl.innerHTML + endMain();
    }

    // Verify admin session server-side
    const user = await AdminAuth.requireAdminAuth();
    if (!user) return null;

    // Populate sidebar user info
    AdminApp.renderSidebarUser(user);

    // Load pending verification badges
    try {
      const stats = await AdminAPI.getAdminStats();
      if (stats.success && stats.data) {
        const sv = stats.data.pending_student_verifications || 0;
        const lv = stats.data.pending_landlord_verifications || 0;
        if (sv > 0) {
          const el = document.getElementById('nav-badge-sv');
          if (el) { el.textContent = sv; el.style.display = 'inline-flex'; }
        }
        if (lv > 0) {
          const el = document.getElementById('nav-badge-lv');
          if (el) { el.textContent = lv; el.style.display = 'inline-flex'; }
        }
        if (sv + lv > 0) {
          const dot = document.getElementById('topbar-notif-dot');
          if (dot) dot.style.display = 'block';
        }
      }
    } catch {}

    return user;
  };

  return { init, buildSidebar, endMain, openSidebar, closeSidebar, toggleSidebar, logout };
})();
