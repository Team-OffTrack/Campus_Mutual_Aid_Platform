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

// ── Team member APIs ──

export function applyTeam(demandId, message) {
  return client.post(`/demands/${demandId}/team/apply`, message ? { message } : {})
}

export function approveApplicant(demandId, applicantId) {
  return client.put(`/demands/${demandId}/team/applicants/${applicantId}/approve`)
}

export function rejectApplicant(demandId, applicantId) {
  return client.put(`/demands/${demandId}/team/applicants/${applicantId}/reject`)
}

export function leaveTeam(demandId) {
  return client.post(`/demands/${demandId}/team/leave`)
}

export function removeTeamMember(demandId, memberId) {
  return client.delete(`/demands/${demandId}/team/members/${memberId}`)
}

export function getTeamMembers(demandId) {
  return client.get(`/demands/${demandId}/team/members`)
}

export function getTeamApplicants(demandId) {
  return client.get(`/demands/${demandId}/team/applicants`)
}

export function getMyMembership(demandId) {
  return client.get(`/demands/${demandId}/team/my-membership`)
}

export function myTeamOrders() {
  return client.get('/demands/my/team')
}
