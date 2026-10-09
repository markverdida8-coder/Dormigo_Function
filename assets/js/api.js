/**
 * DORMIGO - Central Reusable API Service
 * Encapsulates all REST API interactions between Web Frontend and PHP Backend.
 */

const API = (() => {
  // Compute base path relative to current URL
  const getBasePath = () => {
    const path = window.location.pathname;
    if (path.includes('/Dormigo_Functional')) {
      return '/Dormigo_Functional';
    }
    return '';
  };

  const BASE_URL = `${getBasePath()}/php_backend/api`;

  /**
   * Core request helper
   */
  async function request(endpoint, options = {}) {
    const url = `${BASE_URL}/${endpoint}`;
    const defaultHeaders = {};

    if (!(options.body instanceof FormData)) {
      defaultHeaders['Content-Type'] = 'application/json';
    }

    const config = {
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
      } catch (err) {
        console.error('API Non-JSON Response:', text);
        throw new Error('Invalid server response');
      }

      if (!response.ok && data && data.message) {
        throw new Error(data.message);
      }
      return data;
    } catch (error) {
      console.error(`API Error on [${options.method || 'GET'}] ${endpoint}:`, error);
      throw error;
    }
  }

  function toQueryString(params = {}) {
    const query = new URLSearchParams();
    for (const [key, val] of Object.entries(params)) {
      if (val !== undefined && val !== null && val !== '') {
        query.append(key, val);
      }
    }
    const qStr = query.toString();
    return qStr ? `?${qStr}` : '';
  }

  return {
    BASE_URL,
    getBasePath,

    // ==========================================
    // AUTHENTICATION
    // ==========================================
    login: async (email, password) => {
      return await request('login.php', {
        method: 'POST',
        body: JSON.stringify({ email, password })
      });
    },

    register: async (userData) => {
      return await request('register.php', {
        method: 'POST',
        body: JSON.stringify(userData)
      });
    },

    // ==========================================
    // USERS
    // ==========================================
    getUsers: async (params = {}) => {
      return await request(`users.php${toQueryString(params)}`);
    },

    getUserById: async (userId) => {
      return await request(`users.php?user_id=${userId}`);
    },

    updateUser: async (userData) => {
      return await request('users.php', {
        method: 'PATCH',
        body: JSON.stringify(userData)
      });
    },

    // ==========================================
    // BOARDING HOUSES
    // ==========================================
    getBoardingHouses: async (params = {}) => {
      return await request(`boarding_houses.php${toQueryString(params)}`);
    },

    getBoardingHouseById: async (houseId) => {
      const res = await request(`boarding_houses.php?house_id=${houseId}`);
      if (res.success && res.data && res.data.length > 0) {
        return { success: true, data: res.data[0] };
      }
      return res;
    },

    createBoardingHouse: async (payload, files = null) => {
      if (files && files.length > 0) {
        const formData = new FormData();
        formData.append('payload', JSON.stringify(payload));
        for (let i = 0; i < files.length; i++) {
          formData.append('photos[]', files[i]);
        }
        return await request('boarding_houses.php', {
          method: 'POST',
          body: formData
        });
      } else {
        return await request('boarding_houses.php', {
          method: 'POST',
          body: JSON.stringify(payload)
        });
      }
    },

    updateBoardingHouse: async (houseData) => {
      return await request('boarding_houses.php', {
        method: 'PATCH',
        body: JSON.stringify(houseData)
      });
    },

    deleteBoardingHouse: async (houseId) => {
      return await request('boarding_houses.php', {
        method: 'DELETE',
        body: JSON.stringify({ house_id: houseId })
      });
    },

    // ==========================================
    // ROOMS
    // ==========================================
    getRooms: async (params = {}) => {
      return await request(`rooms.php${toQueryString(params)}`);
    },

    createRoom: async (roomData) => {
      return await request('rooms.php', {
        method: 'POST',
        body: JSON.stringify(roomData)
      });
    },

    updateRoom: async (roomData) => {
      return await request('rooms.php', {
        method: 'PATCH',
        body: JSON.stringify(roomData)
      });
    },

    deleteRoom: async (roomId) => {
      return await request('rooms.php', {
        method: 'DELETE',
        body: JSON.stringify({ room_id: roomId })
      });
    },

    // ==========================================
    // AMENITIES
    // ==========================================
    getAmenities: async () => {
      return await request('amenities.php');
    },

    // ==========================================
    // BOOKINGS
    // ==========================================
    getBookings: async (params = {}) => {
      return await request(`bookings.php${toQueryString(params)}`);
    },

    createBooking: async (bookingData) => {
      return await request('bookings.php', {
        method: 'POST',
        body: JSON.stringify(bookingData)
      });
    },

    updateBookingStatus: async (bookingId, status) => {
      return await request('bookings.php', {
        method: 'PATCH',
        body: JSON.stringify({ booking_id: bookingId, status })
      });
    },

    // ==========================================
    // PAYMENTS
    // ==========================================
    getPayments: async (params = {}) => {
      return await request(`payments.php${toQueryString(params)}`);
    },

    createPayment: async (paymentData) => {
      return await request('payments.php', {
        method: 'POST',
        body: JSON.stringify(paymentData)
      });
    },

    updatePaymentStatus: async (paymentId, status, details = {}) => {
      return await request('payments.php', {
        method: 'PATCH',
        body: JSON.stringify({
          payment_id: paymentId,
          status,
          ...details
        })
      });
    },

    // ==========================================
    // REVIEWS
    // ==========================================
    getReviews: async (params = {}) => {
      return await request(`reviews.php${toQueryString(params)}`);
    },

    createReview: async (reviewData) => {
      return await request('reviews.php', {
        method: 'POST',
        body: JSON.stringify(reviewData)
      });
    },

    // ==========================================
    // MESSAGES & CHAT
    // ==========================================
    getConversations: async (userId) => {
      return await request(`messages.php?user_id=${userId}`);
    },

    getMessages: async (userId, otherUserId) => {
      return await request(`messages.php?user_id=${userId}&other_user_id=${otherUserId}`);
    },

    sendMessage: async (senderId, receiverId, messageText, houseId = null) => {
      return await request('messages.php', {
        method: 'POST',
        body: JSON.stringify({
          sender_id: senderId,
          receiver_id: receiverId,
          message_text: messageText,
          house_id: houseId
        })
      });
    },

    markMessagesRead: async (receiverId, senderId) => {
      return await request('messages.php', {
        method: 'PATCH',
        body: JSON.stringify({ receiver_id: receiverId, sender_id: senderId })
      });
    },

    // ==========================================
    // NOTIFICATIONS
    // ==========================================
    getNotifications: async (userId) => {
      return await request(`notifications.php?user_id=${userId}`);
    },

    createNotification: async (notifData) => {
      return await request('notifications.php', {
        method: 'POST',
        body: JSON.stringify(notifData)
      });
    },

    markNotificationRead: async (notificationId) => {
      return await request('notifications.php', {
        method: 'PATCH',
        body: JSON.stringify({ notification_id: notificationId })
      });
    },

    markAllNotificationsRead: async (userId) => {
      return await request('notifications.php', {
        method: 'PATCH',
        body: JSON.stringify({ user_id: userId, mark_all_read: true })
      });
    },

    // ==========================================
    // VERIFICATIONS
    // ==========================================
    getVerifications: async (type = 'student', params = {}) => {
      return await request(`verifications.php${toQueryString({ type, ...params })}`);
    },

    submitVerification: async (type, formData) => {
      return await request('verifications.php', {
        method: 'POST',
        body: formData
      });
    },

    updateVerification: async (verificationId, type, status, rejectionReason = null, reviewedBy = 1) => {
      return await request('verifications.php', {
        method: 'PATCH',
        body: JSON.stringify({
          verification_id: verificationId,
          type,
          status,
          rejection_reason: rejectionReason,
          reviewed_by: reviewedBy
        })
      });
    },

    // ==========================================
    // SYSTEM & LANDLORD STATISTICS
    // ==========================================
    getStats: async (role = 'admin', id = null) => {
      const params = { role };
      if (role === 'landlord' && id) {
        params.landlord_id = id;
      }
      return await request(`stats.php${toQueryString(params)}`);
    }
  };
})();
