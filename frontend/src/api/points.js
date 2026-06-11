import client from './client'

/** Daily check-in. Returns { pointsAwarded, streak, checkinDate }. */
export function checkin() {
  return client.post('/points/checkin')
}

/** Get today's check-in status. Returns { checkedIn, currentStreak, lastCheckinDate }. */
export function getCheckinStatus() {
  return client.get('/points/checkin/status')
}

/** Paginated transaction history. Query: pageNum, pageSize, type (optional). */
export function getTransactions(params = {}) {
  return client.get('/points/transactions', { params })
}
