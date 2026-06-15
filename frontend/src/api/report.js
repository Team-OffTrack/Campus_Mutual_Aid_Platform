import client from './client'

export function createReport(data) {
  return client.post('/reports', data)
}

export function listReports(params) {
  return client.get('/admin/reports', { params })
}

export function resolveReport(id, data) {
  return client.put(`/admin/reports/${id}/resolve`, data)
}
