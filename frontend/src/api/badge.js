import client from './client'

export function getUserBadges() {
  return client.get('/badges')
}

export function wearBadge(badgeKey) {
  return client.post(`/badges/wear/${badgeKey}`)
}

export function unwearBadge() {
  return client.delete('/badges/wear')
}

export function triggerEasterEgg() {
  return client.post('/badges/easter-egg')
}
