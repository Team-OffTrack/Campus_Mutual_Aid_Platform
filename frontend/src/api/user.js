import client from './client'

export function register(data) {
  return client.post('/user/register', data)
}

export function login(data) {
  return client.post('/user/login', data)
}

export function getProfile() {
  return client.get('/user/profile')
}

export function updateProfile(data) {
  return client.put('/user/profile', data)
}

export function changePassword(data) {
  return client.put('/user/password', data)
}

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return client.post('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
