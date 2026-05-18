import client from './client'

export function publishDemand(data) {
  return client.post('/demands', data)
}

export function listDemands(params) {
  return client.get('/demands', { params })
}

export function getDemand(demandId) {
  return client.get(`/demands/${demandId}`)
}

export function cancelDemand(demandId) {
  return client.put(`/demands/${demandId}/cancel`)
}

export function acceptDemand(demandId) {
  return client.put(`/demands/${demandId}/accept`)
}

export function completeDemand(demandId) {
  return client.put(`/demands/${demandId}/complete`)
}

export function myOrders(role) {
  return client.get('/demands/my', { params: { role } })
}

export function uploadDemandImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return client.post('/demands/upload-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
