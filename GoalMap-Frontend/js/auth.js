// Redirect to dashboard if already logged in
if (isLoggedIn()) {
  window.location.href = 'dashboard.html';
}

// ============================================================
// Tab switching
// ============================================================

function switchTab(tab) {
  const isLogin = tab === 'login';
  document.getElementById('loginForm').classList.toggle('hidden', !isLogin);
  document.getElementById('signupForm').classList.toggle('hidden', isLogin);
  document.getElementById('loginTab').classList.toggle('active', isLogin);
  document.getElementById('signupTab').classList.toggle('active', !isLogin);
  hideMessages();
}

function showError(msg) {
  const el = document.getElementById('errorMsg');
  el.textContent = msg;
  el.classList.remove('hidden');
  document.getElementById('successMsg').classList.add('hidden');
}

function showSuccess(msg) {
  const el = document.getElementById('successMsg');
  el.textContent = msg;
  el.classList.remove('hidden');
  document.getElementById('errorMsg').classList.add('hidden');
}

function hideMessages() {
  document.getElementById('errorMsg').classList.add('hidden');
  document.getElementById('successMsg').classList.add('hidden');
}

function setLoading(btnId, loading, text) {
  const btn = document.getElementById(btnId);
  btn.disabled = loading;
  btn.textContent = loading ? 'Please wait...' : text;
}

// ============================================================
// Login
// ============================================================

async function handleLogin(e) {
  e.preventDefault();
  hideMessages();
  setLoading('loginBtn', true, 'Login');

  try {
    const data = await Auth.login({
      email:    document.getElementById('loginEmail').value.trim(),
      password: document.getElementById('loginPassword').value,
    });

    saveSession(data);
    window.location.href = 'dashboard.html';

  } catch (err) {
    showError(err.message || 'Invalid email or password');
  } finally {
    setLoading('loginBtn', false, 'Login');
  }
}

// ============================================================
// Signup
// ============================================================

async function handleSignup(e) {
  e.preventDefault();
  hideMessages();
  setLoading('signupBtn', true, 'Create Account');

  try {
    const data = await Auth.signup({
      name:            document.getElementById('signupName').value.trim(),
      email:           document.getElementById('signupEmail').value.trim(),
      password:        document.getElementById('signupPassword').value,
      currentSkills:   document.getElementById('signupSkills').value.trim(),
      experienceLevel: document.getElementById('signupLevel').value,
    });

    saveSession(data);
    window.location.href = 'dashboard.html';

  } catch (err) {
    showError(err.message || 'Signup failed. Please try again.');
  } finally {
    setLoading('signupBtn', false, 'Create Account');
  }
}
