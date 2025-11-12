(() => {
  // Styles
  const style = document.createElement('style');
  style.textContent = `
  .sm-chat-btn {
    position: fixed; right: 20px; bottom: 20px; z-index: 1000;
    width: 56px; height: 56px; border-radius: 50%;
    background: linear-gradient(135deg, #6366f1, #7c3aed);
    color: #fff; display: flex; align-items: center; justify-content: center;
    box-shadow: 0 10px 25px rgba(99,102,241,0.4); cursor: pointer; border: none;
  }
  .sm-chat-panel {
    position: fixed; right: 20px; bottom: 88px; z-index: 1000;
    width: 360px; max-height: 70vh; display: none; flex-direction: column;
    background: #0f172a; color: #e5e7eb; border: 1px solid #334155; border-radius: 14px;
    box-shadow: 0 20px 50px rgba(0,0,0,0.45); overflow: hidden;
  }
  .sm-chat-header {
    display: flex; align-items: center; justify-content: space-between;
    padding: 10px 12px; background: rgba(30,41,59,0.9); border-bottom: 1px solid #334155;
  }
  .sm-chat-title { font-weight: 600; }
  .sm-chat-body { padding: 12px; overflow-y: auto; flex: 1; background: #0b1220; }
  .sm-msg { margin: 8px 0; padding: 10px 12px; border-radius: 10px; max-width: 80%; line-height: 1.35; }
  .sm-me { background: #2563eb; color: #fff; margin-left: auto; }
  .sm-bot { background: #1f2937; color: #e5e7eb; margin-right: auto; }
  .sm-chat-input { display: flex; gap: 8px; padding: 10px; border-top: 1px solid #334155; background: #0f172a; }
  .sm-chat-input input { flex: 1; padding: 10px; border-radius: 10px; border: 1px solid #334155; background: #0b1220; color: #e5e7eb; }
  .sm-send { padding: 10px 12px; background: #6366f1; border: none; border-radius: 10px; color: #fff; cursor: pointer; }
  `;
  document.head.appendChild(style);

  // DOM
  const btn = document.createElement('button');
  btn.className = 'sm-chat-btn';
  btn.innerHTML = '<i class="fa-solid fa-message"></i>';

  const panel = document.createElement('div');
  panel.className = 'sm-chat-panel';
  panel.innerHTML = `
    <div class="sm-chat-header">
      <div class="sm-chat-title">SkillMatch Assistant</div>
      <button id="sm-close" style="background:none;border:none;color:#e5e7eb;">✕</button>
    </div>
    <div id="sm-body" class="sm-chat-body"></div>
    <div class="sm-chat-input">
      <input id="sm-input" type="text" placeholder="Ask me anything..." />
      <button id="sm-send" class="sm-send">Send</button>
    </div>
  `;

  document.body.appendChild(btn);
  document.body.appendChild(panel);

  const bodyEl = panel.querySelector('#sm-body');
  const inputEl = panel.querySelector('#sm-input');
  const sendEl = panel.querySelector('#sm-send');
  const closeEl = panel.querySelector('#sm-close');

  function toggle(open) {
    panel.style.display = open ? 'flex' : 'none';
    if (open) inputEl.focus();
  }

  btn.addEventListener('click', () => toggle(panel.style.display === 'none'));
  closeEl.addEventListener('click', () => toggle(false));

  function append(text, from) {
    const div = document.createElement('div');
    div.className = `sm-msg ${from === 'me' ? 'sm-me' : 'sm-bot'}`;
    div.textContent = text;
    bodyEl.appendChild(div);
    bodyEl.scrollTop = bodyEl.scrollHeight;
  }

  async function send() {
    const text = inputEl.value.trim();
    if (!text) return;
    append(text, 'me');
    inputEl.value = '';
    sendEl.disabled = true;
    try {
      // Prefer external Node chatbot API; then Spring endpoint; then legacy
      let res = await fetch('http://localhost:3000/api/chatbot/message', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: text })
      });
      if (!res.ok) {
        // fallback to Spring Boot unified endpoint
        res = await fetch('/api/chatbot/message', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ message: text })
        });
      }
      if (!res.ok) {
        // final fallback to legacy
        res = await fetch('/api/v1/chat/ask', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ prompt: text })
        });
      }
      const data = await res.json();
      append(data.response ?? 'No response', 'bot');
    } catch (e) {
      append('Error: ' + e.message + ' (Is Node server running with CORS enabled?)', 'bot');
    } finally {
      sendEl.disabled = false;
      inputEl.focus();
    }
  }

  sendEl.addEventListener('click', send);
  inputEl.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') send();
  });

  // Initial message
  append('Hi! I am your SkillMatch assistant. How can I help?', 'bot');
})(); 


