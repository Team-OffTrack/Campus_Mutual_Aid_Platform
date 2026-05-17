import client from './client'

export function createEvaluation(data) {
  return client.post('/evaluations', data)
}

export function updateEvaluation(evaluationId, data) {
  return client.put(`/evaluations/${evaluationId}`, data)
}

export function getEvaluationsByDemand(demandId) {
  return client.get(`/evaluations/demand/${demandId}`)
}

export function getMyEvaluation(demandId) {
  return client.get('/evaluations/mine', { params: { demandId } })
}

export function getEvaluationsByUser(userId) {
  return client.get(`/evaluations/user/${userId}`)
}
