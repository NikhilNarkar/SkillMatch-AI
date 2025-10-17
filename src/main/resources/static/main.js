let userFullName = "";

document.addEventListener("DOMContentLoaded", () => {
  updateUI();
  const loginForm = document.getElementById("loginForm");
  if (loginForm) loginForm.onsubmit = loginHandler;
  const profileForm = document.getElementById("profileForm");
  if (profileForm) profileForm.onsubmit = saveProfileHandler;
  const resumeForm = document.getElementById("resumeForm");
  if (resumeForm) resumeForm.onsubmit = uploadResumeHandler;
  const logoutBtn = document.getElementById("logoutBtn");
  if (logoutBtn) logoutBtn.onclick = doLogout;
});

function showSection(id) {
  for (let section of ["dashboardSection","profileSection","resumeSection","loginSection"]) {
    const el = document.getElementById(section);
    if (el) el.style.display = 'none';
  }
  const target = document.getElementById(id);
  if (target) target.style.display = 'block';
}

function updateUI() {
  if (window.Api && window.Api.isLoggedIn()) {
    const logoutBtn = document.getElementById("logoutBtn");
    if (logoutBtn) logoutBtn.style.display = "inline-block";
    const user = window.Api.getUser();
    userFullName = [user?.firstName, user?.lastName].filter(Boolean).join(" ");
    fetchProfile();
    showSection("dashboardSection");
  } else {
    const logoutBtn = document.getElementById("logoutBtn");
    if (logoutBtn) logoutBtn.style.display = "none";
    showSection("loginSection");
  }
}

// --- Authentication ---
async function loginHandler(e) {
  e.preventDefault();
  const email = document.getElementById("loginEmail").value.trim();
  const password = document.getElementById("loginPassword").value;
  const msgDiv = document.getElementById("loginMsg");
  msgDiv.textContent = "";
  try {
    const data = await window.Api.login({ email, password });
    userFullName = [data.firstName, data.lastName].filter(Boolean).join(" ");
    // Redirect to standalone dashboard page for clarity
    window.location.href = "/dashboard.html";
  } catch (err) {
    msgDiv.textContent = err.message || "Invalid credentials!";
  }
}

// --- Profile ---
async function fetchProfile() {
  if (!(window.Api && window.Api.isLoggedIn())) return;
  const res = await fetch("http://localhost:8080/api/v1/candidate/profile", {
    headers: { Authorization: "Bearer " + localStorage.getItem('sm_token') }
  });
  if (res.ok) {
    let data = await res.json();
    document.getElementById("userFullName").textContent = data.firstName ? (data.firstName + " " + data.lastName) : "";
    document.getElementById("profileDob").value = data.dateOfBirth || "";
    document.getElementById("profileGender").value = data.gender || "MALE";
    document.getElementById("profileCity").value = data.city || "";
    document.getElementById("profileState").value = data.state || "";
    document.getElementById("profileCountry").value = data.country || "";
    document.getElementById("profileExperience").value = data.experienceYears || 0;
    document.getElementById("profileExpectedSalary").value = data.expectedSalary || 0;
    document.getElementById("profileBio").value = data.bio || "";
  }
}

async function saveProfileHandler(e) {
  e.preventDefault();
  let msgDiv = document.getElementById("profileMsg");
  msgDiv.textContent = "";
  let payload = {
    dateOfBirth: document.getElementById("profileDob").value,
    gender: document.getElementById("profileGender").value,
    city: document.getElementById("profileCity").value,
    state: document.getElementById("profileState").value,
    country: document.getElementById("profileCountry").value,
    experienceYears: +document.getElementById("profileExperience").value,
    expectedSalary: +document.getElementById("profileExpectedSalary").value,
    bio: document.getElementById("profileBio").value,
    // Add additional fields if present in backend (address, postalCode, etc.)
    currentSalary: 0,
    availability: "IMMEDIATE"
  };
  const res = await fetch("http://localhost:8080/api/v1/candidate/profile", {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: "Bearer " + localStorage.getItem('sm_token') },
    body: JSON.stringify(payload)
  });
  msgDiv.textContent = res.ok ? "Profile updated!" : "Error updating profile";
}

// --- Resume ---
async function uploadResumeHandler(e) {
  e.preventDefault();
  let msgDiv = document.getElementById("resumeMsg");
  msgDiv.textContent = "";
  let fileInput = document.getElementById("resumeFile");
  if (!fileInput.files.length) return;
  let formData = new FormData();
  formData.append("resume", fileInput.files[0]);
  const res = await fetch("http://localhost:8080/api/v1/resumes/upload", {
    method: "POST",
    headers: { Authorization: "Bearer " + localStorage.getItem('sm_token') },
    body: formData
  });
  msgDiv.textContent = res.ok ? "Resume uploaded successfully!" : "Upload failed.";
}

// --- Logout ---
function doLogout() {
  if (window.Api) window.Api.logout();
  updateUI();
}
