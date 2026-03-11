import request from './request'

// ===== Auth =====
export const api = {
  auth: {
    sendCode: (email, type, captchaPassToken) => request.post('/auth/email-code', { email, type, captchaPassToken }),
    register: (data) => request.post('/auth/register', data),
    login: (data) => request.post('/auth/login', data),
    resetPassword: (data) => request.post('/auth/reset-password', data),
    resetPasswordByTotp: (data) => request.post('/auth/reset-password-totp', data),
    captchaGenerate: () => request.get('/auth/captcha/generate'),
    captchaVerify: (token, sliderX, track, totalTime) =>
      request.post('/auth/captcha/verify', { token, sliderX, track, totalTime }),
  },

  user: {
    profile: () => request.get('/user/profile'),
    updateProfile: (data) => request.put('/user/profile', data),
    loginRecords: (page = 0, size = 10) =>
      request.get('/user/login-records', { params: { page, size } }),
    getTotpSetup: () => request.get('/user/totp-setup'),
    resetTotp: () => request.post('/user/totp-reset'),
  },

  course: {
    list: () => request.get('/courses'),
    my: () => request.get('/courses/my'),
    get: (id) => request.get(`/courses/${id}`),
    create: (data) => request.post('/courses', data),
    enroll: (id) => request.post(`/courses/${id}/enroll`),
    unenroll: (id) => request.delete(`/courses/${id}/enroll`),
    joinByCode: (inviteCode) => request.post('/courses/join', null, { params: { inviteCode } }),
    students: (id) => request.get(`/courses/${id}/students`),
  },

  knowledge: {
    listByCourse: (courseId) => request.get(`/knowledge/course/${courseId}`),
    create: (data) => request.post('/knowledge', data),
    delete: (id) => request.delete(`/knowledge/${id}`),
    addRelation: (fromId, toId, type) =>
      request.post('/knowledge/relation', null, { params: { fromId, toId, type } }),
    myGraph: (courseId, includePrerequisites = false, maxRelatedNodes = 25) =>
      request.get(`/knowledge/graph/${courseId}`, { params: { includePrerequisites, maxRelatedNodes } }),
    userGraph: (courseId, userId, includePrerequisites = false, maxRelatedNodes = 25) =>
      request.get(`/knowledge/graph/${courseId}/user/${userId}`, { params: { includePrerequisites, maxRelatedNodes } }),
  },

  exercise: {
    listByCourse: (courseId, page = 0, size = 10, keyword = '', sortBy = 'pyExIndex', sortDir = 'asc') =>
      request.get(`/exercises/course/${courseId}`, { params: { page, size, keyword, sortBy, sortDir } }),
    get: (id) => request.get(`/exercises/${id}`),
    create: (data) => request.post('/exercises', data),
    update: (id, data) => request.put(`/exercises/${id}`, data),
    delete: (id) => request.delete(`/exercises/${id}`),
  },

  learning: {
    submit: (data) => request.post('/learning/submit', data),
    history: (page = 0, size = 20) =>
      request.get('/learning/history', { params: { page, size } }),
    answeredIds: () => request.get('/learning/answered-ids'),
    clearHistory: () => request.delete('/learning/history'),
  },

  grade: {
    pending: (courseId) => request.get(`/grade/pending/course/${courseId}`),
    grade: (recordId, data) => request.post(`/grade/${recordId}`, data),
  },

  recommend: {
    latest: (courseId) => request.get(`/recommend/course/${courseId}`),
    refresh: (courseId) => request.post(`/recommend/course/${courseId}/refresh`),
    latestForStudent: (courseId, studentId) =>
      request.get(`/recommend/teacher/course/${courseId}/student/${studentId}`),
    refreshForStudent: (courseId, studentId) =>
      request.post(`/recommend/teacher/course/${courseId}/student/${studentId}/refresh`),
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

  classroom: {
    create: (data) => request.post('/classrooms', data),
    my: () => request.get('/classrooms/my'),
    all: () => request.get('/classrooms/all'),
    joined: () => request.get('/classrooms/joined'),
    detail: (id) => request.get(`/classrooms/${id}`),
    join: (inviteCode) => request.post('/classrooms/join', null, { params: { inviteCode } }),
    leave: (id) => request.delete(`/classrooms/${id}/leave`),
    dissolve: (id) => request.delete(`/classrooms/${id}`),
    removeStudent: (id, studentId) => request.delete(`/classrooms/${id}/students/${studentId}`),
    addCourse: (id, courseId) => request.post(`/classrooms/${id}/courses`, { courseId }),
    removeCourse: (id, courseId) => request.delete(`/classrooms/${id}/courses/${courseId}`),
    enrollStudents: (id, courseId, studentIds) =>
      request.post(`/classrooms/${id}/courses/${courseId}/enroll`, { studentIds: studentIds || [] }),
    unenrollStudents: (id, courseId, studentIds) =>
      request.delete(`/classrooms/${id}/courses/${courseId}/enroll`, { data: { studentIds: studentIds || [] } }),
  },

  admin: {
    users: () => request.get('/admin/users'),
    setRole: (userId, role) =>
      request.put(`/admin/users/${userId}/role`, null, { params: { role } }),
    initDataset: (courseId) => request.post('/admin/init-dataset', null, { params: { courseId } }),
    initPythonData: () => request.post('/admin/init-python-data'),
    syncNeo4j: (courseId) => request.post('/admin/sync-neo4j', null, { params: { courseId } }),
    statistics: () => request.get('/admin/statistics'),
  },
}
