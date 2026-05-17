import client from './client'

export function getConversations() {
  return client.get('/chat/conversations')
}

export function getMessages(conversationId) {
  return client.get(`/chat/conversations/${conversationId}/messages`)
}

export function sendMessage(conversationId, content) {
  return client.post(`/chat/conversations/${conversationId}/messages`, { content })
}

export function createConversation(demandId, targetUserId) {
  return client.post('/chat/conversations', { demandId, targetUserId })
}

export function unreadChatCount() {
  return client.get('/chat/unread-count')
}
