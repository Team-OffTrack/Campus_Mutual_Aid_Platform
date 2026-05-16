import client from './client'

export function listUsers(params) {
  return client.get('/admin/users', { params })
}

export function updateUserStatus(userId, status) {
  return client.put(`/admin/users/${userId}/status`, { status })
}
