/**
 * DORMIGO ADMIN PORTAL - Additional Admin API Extensions
 * Extends AdminAPI with admin-specific stats chart data support.
 * Loads after admin-api.js — extends the AdminAPI object.
 */

// Extend AdminAPI with chart data helpers
Object.assign(AdminAPI, {

  /**
   * Get booking stats breakdown (count by status).
   * Uses the main bookings endpoint and aggregates client-side.
   */
  getBookingStats: async () => {
    const res = await fetch(
      `${AdminAPI.BASE_URL}/bookings.php`,
      { credentials: 'include' }
    );
    const data = await res.json();
    if (!data.success) return null;
    const counts = { PENDING: 0, APPROVED: 0, ACTIVE: 0, COMPLETED: 0, DECLINED: 0, CANCELLED: 0 };
    (data.data || []).forEach(b => {
      const s = (b.status || '').toUpperCase();
      if (counts.hasOwnProperty(s)) counts[s]++;
      else counts[s] = 1;
    });
    return counts;
  },

  /**
   * Get payment stats breakdown (count by status, total by status).
   */
  getPaymentStats: async () => {
    const res = await fetch(
      `${AdminAPI.BASE_URL}/payments.php`,
      { credentials: 'include' }
    );
    const data = await res.json();
    if (!data.success) return null;
    const counts = { PENDING: 0, PAID: 0, CONFIRMED: 0, FAILED: 0, CANCELLED: 0 };
    let totalRevenue = 0;
    (data.data || []).forEach(p => {
      const s = (p.status || '').toUpperCase();
      if (counts.hasOwnProperty(s)) counts[s]++;
      if (s === 'PAID' || s === 'CONFIRMED') {
        totalRevenue += parseFloat(p.amount) || 0;
      }
    });
    return { counts, totalRevenue };
  }
});
