import request from './request'

// ===== Auth =====
export const api = {
  auth: {
    sendCode: (email, type) => request.post('/auth/email-code', { email, type }),
    register: (data) => request.post('/auth/register', data),
    login: (data) => request.post('/auth/login', data),
    resetPassword: (data) => request.post('/auth/reset-password', data),
  },

  user: {
    profile: () => request.get('/user/profile'),
    updateProfile: (data) => request.put('/user/profile', data),
  },

  course: {
    list: () => request.get('/courses'),
    my: () => request.get('/courses/my'),
    get: (id) => request.get(`/courses/${id}`),
    create: (data) => request.post('/courses', data),
    enroll: (id) => request.post(`/courses/${id}/enroll`),
    unenroll: (id) => request.delete(`/courses/${id}/enroll`),
  },

  knowledge: {
    listByCourse: (courseId) => request.get(`/knowledge/course/${courseId}`),
    create: (data) => request.post('/knowledge', data),
    delete: (id) => request.delete(`/knowledge/${id}`),
    addRelation: (fromId, toId, type) =>
      request.post('/knowledge/relation', null, { params: { fromId, toId, type } }),
    myGraph: (courseId) => request.get(`/knowledge/graph/${courseId}`),
    userGraph: (courseId, userId) => request.get(`/knowledge/graph/${courseId}/user/${userId}`),
  },

  exercise: {
    listByCourse: (courseId, page = 0, size = 20) =>
      request.get(`/exercises/course/${courseId}`, { params: { page, size } }),
    get: (id) => request.get(`/exercises/${id}`),
    create: (data) => request.post('/exercises', data),
    update: (id, data) => request.put(`/exercises/${id}`, data),
    delete: (id) => request.delete(`/exercises/${id}`),
  },

  learning: {
    submit: (data) => request.post('/learning/submit', data),
    history: (page = 0, size = 20) =>
      request.get('/learning/history', { params: { page, size } }),
  },

  grade: {
    pending: (courseId) => request.get(`/grade/pending/course/${courseId}`),
    grade: (recordId, data) => request.post(`/grade/${recordId}`, data),
  },

  recommend: {
    latest: (courseId) => request.get(`/recommend/course/${courseId}`),
    refresh: (courseId) => request.post(`/recommend/course/${courseId}/refresh`),
  },

  notification: {
    list: (page = 0, size = 20) =>
      request.get('/notifications', { params: { page, size } }),
    unreadCount: () => request.get('/notifications/unread-count'),
    markAllRead: () => request.put('/notifications/read-all'),
  },

  file: {
    uploadImage: (file) => {
      const form = new FormData()
      form.append('file', file)
      return request.post('/files/upload-image', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
    },
    getMobileSession: () => request.get('/files/mobile-session'),
  },

  admin: {
    users: () => request.get('/admin/users'),
    setRole: (userId, role) =>
      request.put(`/admin/users/${userId}/role`, null, { params: { role } }),
    retrain: () => request.post('/admin/retrain'),
    statistics: () => request.get('/admin/statistics'),
  },
}
