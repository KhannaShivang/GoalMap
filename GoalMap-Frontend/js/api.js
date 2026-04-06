const API_BASE = 'http://localhost:8080/api';

// ============================================================
// Storage helpers
// ============================================================

function getToken()    { return localStorage.getItem('token'); }
function getUserId()   { return localStorage.getItem('userId'); }
function getUserName() { return localStorage.getItem('userName'); }

function saveSession(data) {
  localStorage.setItem('token',    data.token);
  localStorage.setItem('userId',   data.userId);
  localStorage.setItem('userName', data.name);
  localStorage.setItem('email',    data.email);
}

function clearSession() {
  ['token','userId','userName','email'].forEach(k => localStorage.removeItem(k));
}

function isLoggedIn() { return !!getToken(); }

// ============================================================
// Core fetch wrapper
// ============================================================

async function apiFetch(path, options = {}) {
  const token = getToken();
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...(options.headers || {}),
  };

  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });

  if (response.status === 401) {
    clearSession();
    window.location.href = 'index.html';
    return;
  }

  const data = await response.json();
  if (!response.ok) throw new Error(data.message || 'Something went wrong');
  return data;
}

// ============================================================
// Auth
// ============================================================

const Auth = {
  signup: (body) => apiFetch('/auth/signup', { method: 'POST', body: JSON.stringify(body) }),
  login:  (body) => apiFetch('/auth/login',  { method: 'POST', body: JSON.stringify(body) }),
};

// ============================================================
// Goals
// ============================================================

const Goals = {
  create:    (body)   => apiFetch('/goals', { method: 'POST', body: JSON.stringify(body) }),
  getByUser: (userId) => apiFetch(`/goals?userId=${userId}`),
  getById:   (id)     => apiFetch(`/goals/${id}`),
};

// ============================================================
// Roadmaps
// ============================================================

const Roadmaps = {
  generate:  (userId, goalId) => apiFetch('/roadmaps/generate', {
    method: 'POST',
    body: JSON.stringify({ userId, goalId }),
  }),
  getByUser: (userId) => apiFetch(`/roadmaps?userId=${userId}`),
  getById:   (id)     => apiFetch(`/roadmaps/${id}`),
};

// ============================================================
// Progress
// ============================================================

const Progress = {
  getProgress:  (roadmapId) => apiFetch(`/roadmaps/${roadmapId}/progress`),
  completeTask: (taskId, completed) => apiFetch(`/tasks/${taskId}/complete`, {
    method: 'PUT',
    body: JSON.stringify({ completed }),
  }),
  recalculate:  (roadmapId) => apiFetch(`/roadmaps/${roadmapId}/recalculate`, {
    method: 'POST',
  }),
};

// ============================================================
// Subtasks
// ============================================================

const Subtasks = {
  getOrGenerate: (taskId) => apiFetch(`/tasks/${taskId}/subtasks`),
  complete: (subtaskId, completed) => apiFetch(`/subtasks/${subtaskId}/complete`, {
    method: 'PUT',
    body: JSON.stringify({ completed }),
  }),
  status: (taskId) => apiFetch(`/tasks/${taskId}/subtasks/status`),
};

// ============================================================
// Quizzes
// ============================================================

const Quizzes = {
  /**
   * Get a fresh quiz (always generates new one via AI)
   * @param {number} taskId
   * @param {boolean} useRetake - If true, fetches last quiz for re-practice
   */
  get:    (taskId, useRetake = false) => apiFetch(
    useRetake
      ? `/tasks/${taskId}/quiz/retake`
      : `/tasks/${taskId}/quiz`
  ),
  submit: (taskId, answers) => apiFetch(`/tasks/${taskId}/quiz/submit`, {
    method: 'POST',
    body: JSON.stringify({ answers }),
  }),
};

// ============================================================
// Resources
// ============================================================

const Resources = {
    getBySkill: (skillId, limit = 6) =>
        apiFetch(`/resources/${skillId}?limit=${limit}`),

    search: (query, limit = 5) =>
        apiFetch(`/resources/search?query=${encodeURIComponent(query)}&limit=${limit}`),
};

