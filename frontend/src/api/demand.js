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
