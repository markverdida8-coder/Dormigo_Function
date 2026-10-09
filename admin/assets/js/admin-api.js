/**
 * DORMIGO ADMIN PORTAL - Admin API Service
 * Extends the base API with admin-specific session-authenticated calls.
 * Relies on server-side PHP sessions for security.
 */

const AdminAPI = (() => {
  const getBasePath = () => {
    const path = window.location.pathname;
    if (path.includes('/Dormigo_Functional')) return '/Dormigo_Functional';
    return '';
  };

  const BASE_URL = `${getBasePath()}/php_backend/api`;

  async function request(endpoint, options = {}) {
    const url = `${BASE_URL}/${endpoint}`;
    const defaultHeaders = {};

    if (!(options.body instanceof FormData)) {
      defaultHeaders['Content-Type'] = 'application/json';
    }

    const config = {
      credentials: 'include', // Send cookies for session auth
      ...options,
      headers: {
        ...defaultHeaders,
        ...(options.headers || {})
      }
    };

    try {
      const response = await fetch(url, config);
      const text = await response.text();
      let data;
      try {
        data = JSON.parse(text);
      } catch {
        console.error('Non-JSON response:', text);
        throw new Error('Invalid server response');
      }
      return data;
    } catch (error) {
      console.error(`AdminAPI Error [${options.method || 'GET'}] ${endpoint}:`, error);
      throw error;
    }
  }

  function toQueryString(params = {}) {
    const query = new URLSearchParams();
    for (const [key, val] of Object.entries(params)) {
      if (val !== undefined && val !== null && val !== '') query.append(key, val);
    }
    const q = query.toString();
    return q ? `?${q}` : '';
  }

  return {
    BASE_URL,
    getBasePath,

    // ── Authentication ─────────────────────────────────────────────────────────
    adminLogin: async (email, password) => {
      return request('admin_auth.php', {
        method: 'POST',
        body: JSON.stringify({ email, password })
      });
    },

    adminLogout: async () => {
      return request('admin_auth.php?action=logout');
    },

    checkAdminSession: async () => {
      return request('admin_auth.php?action=check');
    },

    updateAdminProfile: async (data) => {
      return request('admin_auth.php', {
        method: 'PATCH',
        body: JSON.stringify(data)
      });
    },

    // ── Statistics ────────────────────────────────────────────────────────────
    getAdminStats: async () => {
      return request('stats.php?role=admin');
    },

    // ── Users ─────────────────────────────────────────────────────────────────
    getUsers: async (params = {}) => {
      return request(`users.php${toQueryString(params)}`);
    },

    getStudents: async () => {
      return request('users.php?user_type=STUDENT');
    },

    getLandlords: async () => {
      return request('users.php?user_type=LANDLORD');
    },

    getUserById: async (userId) => {
      return request(`users.php?user_id=${userId}`);
    },

    // ── Verifications ────────────────────────────────────────────────────────
    getStudentVerifications: async (params = {}) => {
      return request(`verifications.php${toQueryString({ type: 'student', ...params })}`);
    },

    getLandlordVerifications: async (params = {}) => {
      return request(`verifications.php${toQueryString({ type: 'landlord', ...params })}`);
    },

    approveVerification: async (verificationId, type, reviewedBy) => {
      return request('verifications.php', {
        method: 'PATCH',
        body: JSON.stringify({
          verification_id: verificationId,
          type,
          status: 'VERIFIED',
          reviewed_by: reviewedBy
        })
      });
    },

    rejectVerification: async (verificationId, type, rejectionReason, reviewedBy) => {
      return request('verifications.php', {
        method: 'PATCH',
        body: JSON.stringify({
          verification_id: verificationId,
          type,
          status: 'REJECTED',
          rejection_reason: rejectionReason,
          reviewed_by: reviewedBy
        })
      });
    },

    // ── Boarding Houses ───────────────────────────────────────────────────────
    getBoardingHouses: async (params = {}) => {
      return request(`boarding_houses.php${toQueryString(params)}`);
    },

    getBoardingHouseById: async (houseId) => {
      const res = await request(`boarding_houses.php?house_id=${houseId}`);
      if (res.success && res.data && res.data.length > 0) {
        return { success: true, data: res.data[0] };
      }
      return res;
    },

    // ── Rooms ─────────────────────────────────────────────────────────────────
    getRooms: async (params = {}) => {
      return request(`rooms.php${toQueryString(params)}`);
    },

    // ── Bookings ──────────────────────────────────────────────────────────────
    getBookings: async (params = {}) => {
      return request(`bookings.php${toQueryString(params)}`);
    },

    // ── Payments ──────────────────────────────────────────────────────────────
    getPayments: async (params = {}) => {
      return request(`payments.php${toQueryString(params)}`);
    },

    // ── Reviews ───────────────────────────────────────────────────────────────
    getReviews: async (params = {}) => {
      return request(`reviews.php${toQueryString(params)}`);
    },

    // ── Notifications ─────────────────────────────────────────────────────────
    getAllNotifications: async () => {
      // Note: notifications.php requires a user_id; for admin overview we pass 0
      // which returns all notifications visible to admin
      return request('notifications.php?user_id=0');
    },

    getNotificationsForUser: async (userId) => {
      return request(`notifications.php?user_id=${userId}`);
    }
  };
})();
