/**
 * DORMIGO ADMIN PORTAL - Admin API Service
 * Extends the base API with admin-specific session-authenticated calls.
 * Relies on server-side PHP sessions for security.
 */

const AdminAPI = (() => {
  const getBasePath = () => {
    const path = window.location.pathname;
    const adminIndex = path.indexOf('/admin');
    if (adminIndex !== -1) {
      return path.substring(0, adminIndex);
    }
    if (path.includes('/Dormigo_Functional')) return '/Dormigo_Functional';
    if (path.includes('/Dormigo_Function-WEB-ADMIN')) return '/Dormigo_Function-WEB-ADMIN';
    return '';
  };

  const BASE_URL = `${getBasePath()}/php_backend/api`;
  const CANONICAL_API_URL = `${window.location.origin}/Dormigo_Backend/api`;

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

  async function canonicalRequest(endpoint, options = {}) {
    const defaultHeaders = { 'Content-Type': 'application/json' };
    const response = await fetch(`${CANONICAL_API_URL}/${endpoint}`, {
      credentials: 'include',
      ...options,
      headers: {
        ...defaultHeaders,
        ...(options.headers || {})
      }
    });
    const text = await response.text();
    try {
      return JSON.parse(text);
    } catch {
      throw new Error('Invalid canonical API response');
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

    // ── Authentication & Two-Factor OTP ─────────────────────────────────────────
    adminLogin: async (email, password) => {
      return request('admin_auth.php?action=login', {
        method: 'POST',
        body: JSON.stringify({ email, password })
      });
    },

    verifyLoginOtp: async (otpCode) => {
      return request('admin_auth.php?action=verify_login_otp', {
        method: 'POST',
        body: JSON.stringify({ otp_code: otpCode })
      });
    },

    resendOtp: async (purpose = 'LOGIN_2FA') => {
      return request('admin_auth.php?action=resend_otp', {
        method: 'POST',
        body: JSON.stringify({ purpose })
      });
    },

    requestPasswordOtp: async (currentPassword, newPassword) => {
      return request('admin_auth.php?action=request_password_otp', {
        method: 'POST',
        body: JSON.stringify({ 
          current_password: currentPassword, 
          new_password: newPassword 
        })
      });
    },

    confirmPasswordChange: async (otpCode) => {
      return request('admin_auth.php?action=confirm_password_change', {
        method: 'POST',
        body: JSON.stringify({ otp_code: otpCode })
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

    getPropertyVerifications: async () => {
      return request('verifications.php?type=house');
    },

    reviewPropertyVerification: async (houseId, status, rejectionReason = '') => {
      return canonicalRequest('review_property_verification.php', {
        method: 'PATCH',
        body: JSON.stringify({
          house_id: houseId,
          status,
          rejection_reason: rejectionReason
        })
      });
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

    getLandlordSubscriptions: async (params = {}) => {
      return request(`subscriptions.php${toQueryString(params)}`);
    },

    reviewLandlordSubscription: async (subscriptionId, status, rejectionReason = '') => {
      return request('subscriptions.php', {
        method: 'PATCH',
        body: JSON.stringify({ subscription_id: subscriptionId, status, rejection_reason: rejectionReason })
      });
    },

    getSubscriptionPaymentConfig: async () => {
      return request('subscription_payment_config.php');
    },

    // ── Reviews ───────────────────────────────────────────────────────────────
    getReviews: async (params = {}) => {
      return request(`reviews.php${toQueryString(params)}`);
    },

    // ── Notifications ─────────────────────────────────────────────────────────
    getNotifications: async () => {
      return request('notifications.php');
    },

    markNotificationRead: async (notificationId) => {
      return request('notifications.php', {
        method: 'PATCH',
        body: JSON.stringify({ notification_id: notificationId })
      });
    }
  };
})();
