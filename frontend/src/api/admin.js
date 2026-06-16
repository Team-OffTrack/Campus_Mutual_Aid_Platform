import client from './client'

export function getDashboard() {
  return client.get('/admin/dashboard')
}

export function listUsers(params) {
  return client.get('/admin/users', { params })
}

export function updateUserStatus(userId, status) {
  return client.put(`/admin/users/${userId}/status`, { status })
}

export function listDemands(params) {
  return client.get('/admin/demands', { params })
}

export function deleteDemand(demandId) {
  return client.delete(`/admin/demands/${demandId}`)
}
