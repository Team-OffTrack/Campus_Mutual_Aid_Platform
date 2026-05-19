import client from './client'

export function getConversations() {
  return client.get('/chat/conversations')
}

export function getMessages(conversationId) {
  return client.get(`/chat/conversations/${conversationId}/messages`)
}

export function sendMessage(conversationId, { type, content, imageUrl } = {}) {
  return client.post(`/chat/conversations/${conversationId}/messages`, {
    type: type || 'text',
    content: content || '',
    imageUrl: imageUrl || null
  })
}

export function uploadChatImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return client.post('/chat/upload-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function createConversation(demandId, targetUserId) {
  return client.post('/chat/conversations', { demandId, targetUserId })
}

export function unreadChatCount() {
  return client.get('/chat/unread-count')
}
