// Redirect to login if not logged in
if (!isLoggedIn()) {
  window.location.href = 'index.html';
}

// ============================================================
// State
// ============================================================

let currentGoalId   = null;
let currentRoadmap  = null;
let allGoals        = [];

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
      // Auto-select first goal
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
         id="goalItem_${goal.id}"
         onclick="selectGoal(${goal.id})">
      <div class="goal-item-number">Goal ${goal.userGoalNumber || ''}</div>
      <div class="goal-item-text">${goal.goalDescription}</div>
    </div>
  `).join('');
}

async function selectGoal(goalId) {
  currentGoalId = goalId;

  // Update sidebar active state
  document.querySelectorAll('.goal-item').forEach(el => el.classList.remove('active'));
  const activeItem = document.getElementById(`goalItem_${goalId}`);
  if (activeItem) activeItem.classList.add('active');

  // Show goal detail panel
  document.getElementById('emptyState').classList.add('hidden');
  document.getElementById('goalDetail').classList.remove('hidden');

  const goal = allGoals.find(g => g.id === goalId);
  if (goal) {
    document.getElementById('goalTitle').textContent = goal.goalDescription;
    document.getElementById('goalMeta').textContent  = `${goal.targetDurationMonths} months`;
    document.getElementById('goalStatus').textContent = goal.status;
    document.getElementById('goalStatus').className =
      `badge badge-${goal.status.toLowerCase()}`;
  }

  // Load roadmap for this goal
  await loadRoadmapForGoal(goalId);
}

// ============================================================
// Roadmap
// ============================================================

async function loadRoadmapForGoal(goalId) {
  try {
    const roadmaps = await Roadmaps.getByUser(getUserId());
    // Find the most recent roadmap for this goal
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

  // Show spinner
  document.getElementById('generatingState').classList.remove('hidden');
  document.getElementById('progressSection').classList.add('hidden');
  document.getElementById('tasksSection').classList.add('hidden');
  document.getElementById('generateBtn').disabled = true;
  document.getElementById('generateBtn').textContent = 'Generating...';

  try {
    currentRoadmap = await Roadmaps.generate(getUserId(), currentGoalId);
    renderRoadmap(currentRoadmap);
    showToast('Roadmap generated successfully!', 'success');
  } catch (err) {
    showToast(err.message || 'Failed to generate roadmap', 'error');
  } finally {
    document.getElementById('generatingState').classList.add('hidden');
    document.getElementById('generateBtn').disabled = false;
    document.getElementById('generateBtn').textContent = 'Regenerate Roadmap';
  }
}

function renderRoadmap(roadmap) {
  // Progress
  const pct = roadmap.totalTasks > 0
    ? Math.round((roadmap.completedTasks / roadmap.totalTasks) * 100)
    : 0;

  document.getElementById('progressSection').classList.remove('hidden');
  document.getElementById('progressPct').textContent  = `${pct}%`;
  document.getElementById('progressFill').style.width = `${pct}%`;
  document.getElementById('progressStats').textContent =
    `${roadmap.completedTasks} of ${roadmap.totalTasks} tasks completed`;

  // Tasks
  document.getElementById('tasksSection').classList.remove('hidden');
  document.getElementById('tasksList').innerHTML =
    roadmap.tasks.map(task => renderTask(task)).join('');
}

function renderTask(task) {
  const isChecked = task.completed;
  return `
    <div class="task-card ${isChecked ? 'completed' : ''}" id="taskCard_${task.id}">
      <div class="task-checkbox ${isChecked ? 'checked' : ''}"
           onclick="toggleTask(${task.id}, ${!isChecked})">
        ${isChecked ? '✓' : ''}
      </div>
      <div class="task-body">
        <div class="task-priority">Step ${task.priority}</div>
        <div class="task-description">${task.description}</div>
        ${task.skillName ? `<span class="task-skill">${task.skillName}</span>` : ''}
      </div>
      <div class="task-actions">
        ${task.skillName ? `
          <button class="btn btn-ghost btn-sm" onclick="showResources(${task.id}, '${task.skillName}')">
            Resources
          </button>` : ''}
      </div>
    </div>
  `;
}

// ============================================================
// Task completion
// ============================================================

async function toggleTask(taskId, completed) {
  try {
    await Progress.completeTask(taskId, completed);

    // Update task card UI immediately
    const card     = document.getElementById(`taskCard_${taskId}`);
    const checkbox = card.querySelector('.task-checkbox');

    card.classList.toggle('completed', completed);
    checkbox.classList.toggle('checked', completed);
    checkbox.innerHTML = completed ? '✓' : '';
    checkbox.setAttribute('onclick', `toggleTask(${taskId}, ${!completed})`);

    // Update progress bar
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
      ? Math.round((progress.completedTasks / progress.totalTasks) * 100)
      : 0;

    document.getElementById('progressPct').textContent  = `${pct}%`;
    document.getElementById('progressFill').style.width = `${pct}%`;
    document.getElementById('progressStats').textContent =
      `${progress.completedTasks} of ${progress.totalTasks} tasks completed`;
  } catch (err) {
    // silently fail — UI is already updated
  }
}

// ============================================================
// Recalculate
// ============================================================

async function recalculateRoadmap() {
  if (!currentRoadmap) return;

  document.getElementById('generatingState').classList.remove('hidden');
  document.getElementById('tasksSection').classList.add('hidden');

  try {
    const progress = await Progress.recalculate(currentRoadmap.id);
    // Reload roadmap to get new tasks
    await loadRoadmapForGoal(currentGoalId);
    showToast('Roadmap updated with new steps!', 'success');
  } catch (err) {
    showToast(err.message || 'Recalculate failed', 'error');
  } finally {
    document.getElementById('generatingState').classList.add('hidden');
  }
}

// ============================================================
// Resources modal
// ============================================================

async function showResources(taskId, skillName) {
  document.getElementById('resourcesModalTitle').textContent =
    `Resources — ${skillName}`;
  document.getElementById('resourcesList').innerHTML =
    '<div class="loading-text">Loading resources...</div>';
  document.getElementById('resourcesModal').classList.remove('hidden');

  try {
    // Find skill ID from current roadmap tasks
    const task = currentRoadmap.tasks.find(t => t.id === taskId);
    const resources = task?.resources || [];

    if (resources.length === 0) {
      document.getElementById('resourcesList').innerHTML =
        '<div class="loading-text">No resources found for this skill yet.</div>';
      return;
    }

    document.getElementById('resourcesList').innerHTML =
      resources.map(r => `
        <a href="${r.link || '#'}" target="_blank" class="resource-card">
          <span class="resource-type-badge">${r.type}</span>
          <span class="resource-title">${r.title}</span>
          <span class="resource-difficulty">${r.difficulty}</span>
        </a>
      `).join('');

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
  document.getElementById('goalDescription').value = '';
  document.getElementById('goalDuration').value = '12';
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

// ============================================================
// Empty state
// ============================================================

function showEmptyState() {
  document.getElementById('emptyState').classList.remove('hidden');
  document.getElementById('goalDetail').classList.add('hidden');
}

// ============================================================
// Toast notification
// ============================================================

function showToast(message, type = 'info') {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.className   = `toast ${type}`;
  toast.classList.remove('hidden');

  setTimeout(() => {
    toast.classList.add('hidden');
  }, 3000);
}
