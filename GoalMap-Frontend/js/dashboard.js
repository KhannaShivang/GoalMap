// Redirect to login if not logged in
if (!isLoggedIn()) {
  window.location.href = 'index.html';
}

// ============================================================
// State
// ============================================================

let currentGoalId  = null;
let currentRoadmap = null;
let allGoals       = [];
let activeQuiz     = null;

// ============================================================
// Init
// ============================================================

document.addEventListener('DOMContentLoaded', async () => {
  document.getElementById('navUserName').textContent = getUserName() || 'User';
  await loadGoals();
});

// ============================================================
// Goals
// ============================================================

async function loadGoals() {
  try {
    allGoals = await Goals.getByUser(getUserId());
    renderGoalsList();
    if (allGoals.length === 0) {
      showEmptyState();
    } else {
      selectGoal(allGoals[0].id);
    }
  } catch (err) {
    showToast('Failed to load goals', 'error');
  }
}

function renderGoalsList() {
  const container = document.getElementById('goalsList');
  if (allGoals.length === 0) {
    container.innerHTML = '<div class="loading-text">No goals yet</div>';
    return;
  }
  container.innerHTML = allGoals.map(goal => `
    <div class="goal-item ${goal.id === currentGoalId ? 'active' : ''}"
         id="goalItem_${goal.id}" onclick="selectGoal(${goal.id})">
      <div class="goal-item-number">Goal ${goal.userGoalNumber || ''}</div>
      <div class="goal-item-text">${goal.goalDescription}</div>
    </div>
  `).join('');
}

async function selectGoal(goalId) {
  currentGoalId = goalId;
  document.querySelectorAll('.goal-item').forEach(el => el.classList.remove('active'));
  const activeItem = document.getElementById(`goalItem_${goalId}`);
  if (activeItem) activeItem.classList.add('active');

  document.getElementById('emptyState').classList.add('hidden');
  document.getElementById('goalDetail').classList.remove('hidden');

  const goal = allGoals.find(g => g.id === goalId);
  if (goal) {
    document.getElementById('goalTitle').textContent  = goal.goalDescription;
    document.getElementById('goalMeta').textContent   = `${goal.targetDurationMonths} months`;
    document.getElementById('goalStatus').textContent = goal.status;
    document.getElementById('goalStatus').className   =
      `badge badge-${goal.status.toLowerCase()}`;
  }
  await loadRoadmapForGoal(goalId);
}

// ============================================================
// Roadmap
// ============================================================

async function loadRoadmapForGoal(goalId) {
  try {
    const roadmaps = await Roadmaps.getByUser(getUserId());
    currentRoadmap = roadmaps
      .filter(r => r.goalId === goalId)
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))[0] || null;

    if (currentRoadmap) {
      document.getElementById('generateBtn').textContent = 'Regenerate Roadmap';
      renderRoadmap(currentRoadmap);
    } else {
      document.getElementById('generateBtn').textContent = 'Generate AI Roadmap';
      document.getElementById('progressSection').classList.add('hidden');
      document.getElementById('tasksSection').classList.add('hidden');
      document.getElementById('generatingState').classList.add('hidden');
    }
  } catch (err) {
    showToast('Failed to load roadmap', 'error');
  }
}

async function generateRoadmap() {
  if (!currentGoalId) return;
  document.getElementById('generatingState').classList.remove('hidden');
  document.getElementById('progressSection').classList.add('hidden');
  document.getElementById('tasksSection').classList.add('hidden');
  document.getElementById('generateBtn').disabled    = true;
  document.getElementById('generateBtn').textContent = 'Generating...';

  try {
    currentRoadmap = await Roadmaps.generate(getUserId(), currentGoalId);
    renderRoadmap(currentRoadmap);
    showToast('Roadmap generated!', 'success');
  } catch (err) {
    showToast(err.message || 'Failed to generate roadmap', 'error');
  } finally {
    document.getElementById('generatingState').classList.add('hidden');
    document.getElementById('generateBtn').disabled    = false;
    document.getElementById('generateBtn').textContent = 'Regenerate Roadmap';
  }
}

function renderRoadmap(roadmap) {
  const pct = roadmap.totalTasks > 0
    ? Math.round((roadmap.completedTasks / roadmap.totalTasks) * 100) : 0;

  document.getElementById('progressSection').classList.remove('hidden');
  document.getElementById('progressPct').textContent  = `${pct}%`;
  document.getElementById('progressFill').style.width = `${pct}%`;
  document.getElementById('progressStats').textContent =
    `${roadmap.completedTasks} of ${roadmap.totalTasks} tasks completed`;

  document.getElementById('tasksSection').classList.remove('hidden');
  document.getElementById('tasksList').innerHTML =
    roadmap.tasks.map(task => renderTaskCard(task)).join('');
}

function renderTaskCard(task) {
  const isChecked = task.completed;
  return `
    <div class="task-card ${isChecked ? 'completed' : ''}" id="taskCard_${task.id}">

      <!-- Task header -->
      <div class="task-header" onclick="toggleTaskExpand(${task.id})">
        <div class="task-checkbox ${isChecked ? 'checked' : ''}"
             onclick="event.stopPropagation(); toggleTask(${task.id}, ${!isChecked})">
          ${isChecked ? '✓' : ''}
        </div>
        <div class="task-body">
          <div class="task-priority">Step ${task.priority}</div>
          <div class="task-description">${task.description}</div>
          <div class="task-footer">
            ${task.skillName ? `<span class="task-skill">${task.skillName}</span>` : ''}
          </div>
        </div>
        <div class="task-expand-btn" id="expandBtn_${task.id}">▶</div>
      </div>

      <!-- Expandable subtask + quiz section -->
      <div class="task-detail hidden" id="taskDetail_${task.id}">
        <div class="subtask-section">
          <div class="subtask-header">
            <span class="subtask-title">Learning Steps</span>
            <button class="btn btn-ghost btn-sm" onclick="loadSubtasks(${task.id})">
              Load Steps
            </button>
          </div>
          <div id="subtasksList_${task.id}" class="subtasks-list"></div>
        </div>

        <div class="quiz-section" id="quizSection_${task.id}">
          <button class="btn btn-outline btn-full quiz-btn"
                  id="quizBtn_${task.id}"
                  onclick="openQuiz(${task.id}, '${task.description.replace(/'/g, "\\'")}')">
            Take Quiz
          </button>
        </div>
      </div>
    </div>
  `;
}

// ============================================================
// Expand / collapse task
// ============================================================

function toggleTaskExpand(taskId) {
  const detail    = document.getElementById(`taskDetail_${taskId}`);
  const expandBtn = document.getElementById(`expandBtn_${taskId}`);
  const isHidden  = detail.classList.contains('hidden');

  detail.classList.toggle('hidden', !isHidden);
  expandBtn.textContent = isHidden ? '▼' : '▶';

  // Auto-load subtasks on first expand
  if (isHidden) {
    const subtasksList = document.getElementById(`subtasksList_${taskId}`);
    if (subtasksList.innerHTML === '') loadSubtasks(taskId);
  }
}

// ============================================================
// Subtasks
// ============================================================

async function loadSubtasks(taskId) {
  const container = document.getElementById(`subtasksList_${taskId}`);
  container.innerHTML = `
    <div class="subtask-loading">
      <div class="spinner-sm"></div>
      <span>AI is generating learning steps... (30-60s)</span>
    </div>`;

  try {
    const subtasks = await Subtasks.getOrGenerate(taskId);
    renderSubtasks(taskId, subtasks);
    checkQuizAvailability(taskId, subtasks);
  } catch (err) {
    container.innerHTML = `<div class="subtask-error">${err.message}</div>`;
  }
}

function renderSubtasks(taskId, subtasks) {
  const container = document.getElementById(`subtasksList_${taskId}`);
  if (subtasks.length === 0) {
    container.innerHTML = '<div class="subtask-empty">No steps found.</div>';
    return;
  }

  container.innerHTML = subtasks.map(s => `
    <div class="subtask-item ${s.completed ? 'completed' : ''}" id="subtask_${s.id}">
      <div class="subtask-checkbox ${s.completed ? 'checked' : ''}"
           onclick="toggleSubtask(${s.id}, ${taskId}, ${!s.completed})">
        ${s.completed ? '✓' : ''}
      </div>
      <span class="subtask-text">${s.orderIndex}. ${s.description}</span>
    </div>
  `).join('');
}

async function toggleSubtask(subtaskId, taskId, completed) {
  try {
    await Subtasks.complete(subtaskId, completed);

    // Update UI immediately
    const item     = document.getElementById(`subtask_${subtaskId}`);
    const checkbox = item.querySelector('.subtask-checkbox');
    item.classList.toggle('completed', completed);
    checkbox.classList.toggle('checked', completed);
    checkbox.innerHTML  = completed ? '✓' : '';
    checkbox.setAttribute('onclick',
      `toggleSubtask(${subtaskId}, ${taskId}, ${!completed})`);

    // Refresh subtask list to get updated state
    const subtasks = await Subtasks.getOrGenerate(taskId);
    checkQuizAvailability(taskId, subtasks);

  } catch (err) {
    showToast('Failed to update step', 'error');
  }
}

function checkQuizAvailability(taskId, subtasks) {
  const allDone  = subtasks.length > 0 && subtasks.every(s => s.completed);
  const quizBtn  = document.getElementById(`quizBtn_${taskId}`);
  if (!quizBtn) return;

  if (allDone) {
    quizBtn.classList.remove('hidden');
    quizBtn.textContent = 'Take Quiz — Test Your Knowledge';
  } else {
    const done  = subtasks.filter(s => s.completed).length;
    quizBtn.classList.remove('hidden');
    quizBtn.textContent = `Complete all steps to unlock quiz (${done}/${subtasks.length})`;
    quizBtn.disabled    = true;
    setTimeout(() => { quizBtn.disabled = false; }, 0);
  }
}

// ============================================================
// Task completion
// ============================================================

async function toggleTask(taskId, completed) {
  try {
    await Progress.completeTask(taskId, completed);
    const card     = document.getElementById(`taskCard_${taskId}`);
    const checkbox = card.querySelector('.task-checkbox');
    card.classList.toggle('completed', completed);
    checkbox.classList.toggle('checked', completed);
    checkbox.innerHTML = completed ? '✓' : '';
    checkbox.setAttribute('onclick',
      `event.stopPropagation(); toggleTask(${taskId}, ${!completed})`);
    await refreshProgress();
  } catch (err) {
    showToast('Failed to update task', 'error');
  }
}

async function refreshProgress() {
  if (!currentRoadmap) return;
  try {
    const progress = await Progress.getProgress(currentRoadmap.id);
    const pct = progress.totalTasks > 0
      ? Math.round((progress.completedTasks / progress.totalTasks) * 100) : 0;
    document.getElementById('progressPct').textContent  = `${pct}%`;
    document.getElementById('progressFill').style.width = `${pct}%`;
    document.getElementById('progressStats').textContent =
      `${progress.completedTasks} of ${progress.totalTasks} tasks completed`;
  } catch (err) { /* silently fail */ }
}

// ============================================================
// Recalculate
// ============================================================

async function recalculateRoadmap() {
  if (!currentRoadmap) return;
  document.getElementById('generatingState').classList.remove('hidden');
  document.getElementById('tasksSection').classList.add('hidden');
  try {
    await Progress.recalculate(currentRoadmap.id);
    await loadRoadmapForGoal(currentGoalId);
    showToast('Roadmap updated with new steps!', 'success');
  } catch (err) {
    showToast(err.message || 'Recalculate failed', 'error');
  } finally {
    document.getElementById('generatingState').classList.add('hidden');
  }
}

// ============================================================
// Quiz
// ============================================================

async function openQuiz(taskId, taskDescription) {
  const modal = document.getElementById('quizModal');
  document.getElementById('quizModalTitle').textContent = 'Loading quiz...';
  document.getElementById('quizContent').innerHTML = `
    <div class="quiz-loading">
      <div class="spinner"></div>
      <p>AI is generating your quiz... (30-60s first time)</p>
    </div>`;
  modal.classList.remove('hidden');

  try {
    activeQuiz = await Quizzes.get(taskId);
    renderQuiz(activeQuiz, taskId);
  } catch (err) {
    document.getElementById('quizContent').innerHTML =
      `<div class="quiz-error">${err.message}</div>`;
  }
}

function renderQuiz(quiz, taskId) {
  document.getElementById('quizModalTitle').textContent =
    `Quiz — ${quiz.taskDescription}`;

  const html = `
    <div class="quiz-info">${quiz.totalQuestions} questions • Pass mark: 70%</div>
    <form id="quizForm">
      ${quiz.questions.map((q, i) => `
        <div class="quiz-question">
          <div class="quiz-question-text">
            <span class="quiz-q-num">Q${i + 1}.</span> ${q.question}
          </div>
          <div class="quiz-options">
            ${['A','B','C','D'].map(opt => `
              <label class="quiz-option">
                <input type="radio" name="q_${q.id}" value="${opt}" required />
                <span class="quiz-option-letter">${opt}</span>
                <span class="quiz-option-text">${q['option' + opt]}</span>
              </label>
            `).join('')}
          </div>
        </div>
      `).join('')}
    </form>
    <div class="quiz-actions">
      <button class="btn btn-primary btn-full" onclick="submitQuiz(${taskId})">
        Submit Quiz
      </button>
    </div>
  `;

  document.getElementById('quizContent').innerHTML = html;
}

async function submitQuiz(taskId) {
  const form    = document.getElementById('quizForm');
  const answers = {};

  // Collect answers
  activeQuiz.questions.forEach(q => {
    const selected = form.querySelector(`input[name="q_${q.id}"]:checked`);
    if (selected) answers[q.id] = selected.value;
  });

  // Check all answered
  if (Object.keys(answers).length < activeQuiz.questions.length) {
    showToast('Please answer all questions', 'error');
    return;
  }

  try {
    const result = await Quizzes.submit(taskId, answers);
    renderQuizResult(result);
  } catch (err) {
    showToast(err.message || 'Failed to submit quiz', 'error');
  }
}

function renderQuizResult(result) {
  const passClass = result.passed ? 'pass' : 'fail';
  const passText  = result.passed ? 'Passed! 🎉' : 'Not passed — try reviewing the material';

  const html = `
    <div class="quiz-result-header ${passClass}">
      <div class="quiz-score">${result.score}%</div>
      <div class="quiz-pass-text">${passText}</div>
      <div class="quiz-score-detail">
        ${result.correctAnswers} of ${result.totalQuestions} correct
      </div>
    </div>

    <div class="quiz-results-list">
      ${result.results.map((r, i) => `
        <div class="quiz-result-item ${r.correct ? 'correct' : 'wrong'}">
          <div class="quiz-result-q">
            <span class="result-icon">${r.correct ? '✓' : '✗'}</span>
            <strong>Q${i + 1}:</strong> ${r.question}
          </div>
          <div class="quiz-result-answers">
            <span class="your-answer">Your answer: <strong>${r.yourAnswer}</strong></span>
            ${!r.correct
              ? `<span class="correct-answer">Correct: <strong>${r.correctAnswer}</strong></span>`
              : ''}
          </div>
          ${r.explanation
            ? `<div class="quiz-explanation">${r.explanation}</div>`
            : ''}
        </div>
      `).join('')}
    </div>

    <div class="quiz-actions">
      <button class="btn btn-ghost" onclick="hideQuizModal()">Close</button>
      ${!result.passed
        ? `<button class="btn btn-outline" onclick="retakeQuiz()">Retake Quiz</button>`
        : ''}
    </div>
  `;

  document.getElementById('quizContent').innerHTML = html;
}

async function retakeQuiz() {
  // Fetch the same quiz again for re-practice
  try {
    activeQuiz = await Quizzes.get(activeQuiz.taskId, true); // true = retake mode
    renderQuiz(activeQuiz, activeQuiz.taskId);
  } catch (err) {
    showToast('Failed to load quiz for retake', 'error');
  }
}

function hideQuizModal() {
  document.getElementById('quizModal').classList.add('hidden');
  activeQuiz = null;
}

// ============================================================
// Resources modal
// ============================================================

async function showResources(taskId, skillName) {
  document.getElementById('resourcesModalTitle').textContent = `Resources — ${skillName}`;
  document.getElementById('resourcesList').innerHTML =
    '<div class="loading-text">Loading...</div>';
  document.getElementById('resourcesModal').classList.remove('hidden');

  try {
    const task      = currentRoadmap.tasks.find(t => t.id === taskId);
    const resources = task?.resources || [];
    if (resources.length === 0) {
      document.getElementById('resourcesList').innerHTML =
        '<div class="loading-text">No resources found for this skill yet.</div>';
      return;
    }
    document.getElementById('resourcesList').innerHTML = resources.map(r => `
      <a href="${r.link || '#'}" target="_blank" class="resource-card">
        <span class="resource-type-badge">${r.type}</span>
        <span class="resource-title">${r.title}</span>
        <span class="resource-difficulty">${r.difficulty}</span>
      </a>`).join('');
  } catch (err) {
    document.getElementById('resourcesList').innerHTML =
      '<div class="loading-text">Failed to load resources.</div>';
  }
}

function hideResourcesModal() {
  document.getElementById('resourcesModal').classList.add('hidden');
}

// ============================================================
// New Goal modal
// ============================================================

function showNewGoalModal() {
  document.getElementById('newGoalModal').classList.remove('hidden');
}

function hideNewGoalModal() {
  document.getElementById('newGoalModal').classList.add('hidden');
  document.getElementById('goalDescription').value  = '';
  document.getElementById('goalDuration').value     = '12';
}

async function createGoal(e) {
  e.preventDefault();
  try {
    const newGoal = await Goals.create({
      userId:               parseInt(getUserId()),
      goalDescription:      document.getElementById('goalDescription').value.trim(),
      targetDurationMonths: parseInt(document.getElementById('goalDuration').value),
    });
    hideNewGoalModal();
    await loadGoals();
    selectGoal(newGoal.id);
    showToast('Goal created!', 'success');
  } catch (err) {
    showToast(err.message || 'Failed to create goal', 'error');
  }
}

// ============================================================
// Auth
// ============================================================

function logout() {
  clearSession();
  window.location.href = 'index.html';
}

function showEmptyState() {
  document.getElementById('emptyState').classList.remove('hidden');
  document.getElementById('goalDetail').classList.add('hidden');
}

// ============================================================
// Toast
// ============================================================

function showToast(message, type = 'info') {
  const toast   = document.getElementById('toast');
  toast.textContent = message;
  toast.className   = `toast ${type}`;
  toast.classList.remove('hidden');
  setTimeout(() => toast.classList.add('hidden'), 3500);
}
