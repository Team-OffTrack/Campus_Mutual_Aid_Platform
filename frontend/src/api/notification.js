import client from './client'

export function listNotifications() {
  return client.get('/notifications')
}

export function unreadCount() {
  return client.get('/notifications/unread-count')
}

export function markRead(notificationId) {
  return client.put(`/notifications/${notificationId}/read`)
}

export function markAllRead() {
  return client.put('/notifications/read-all')
}
