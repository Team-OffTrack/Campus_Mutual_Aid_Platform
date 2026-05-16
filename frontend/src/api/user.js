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
