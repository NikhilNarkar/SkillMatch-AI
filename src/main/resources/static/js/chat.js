(() => {
  const chatEl = document.getElementById('chat');
  const inputEl = document.getElementById('prompt');
  const sendBtn = document.getElementById('send');
  const clearBtn = document.getElementById('clear');

  function appendMessage(text, from) {
    const div = document.createElement('div');
    div.className = `msg ${from === 'me' ? 'me' : 'bot'}`;
    div.textContent = text;
    chatEl.appendChild(div);
    chatEl.scrollTop = chatEl.scrollHeight;
  }

  function setLoading(loading) {
    sendBtn.disabled = loading;
    inputEl.disabled = loading;
  }

  async function send() {
    const text = inputEl.value.trim();
    if (!text) return;
    appendMessage(text, 'me');
    inputEl.value = '';
    setLoading(true);
    try {
      const res = await fetch('/api/v1/chat/ask', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prompt: text })
      });
      const data = await res.json();
      appendMessage(data.response ?? 'No response', 'bot');
    } catch (e) {
      appendMessage('Error: ' + e.message, 'bot');
    } finally {
      setLoading(false);
      inputEl.focus();
    }
  }

  sendBtn.addEventListener('click', send);
  clearBtn.addEventListener('click', () => { chatEl.innerHTML = ''; });
  inputEl.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') send();
  });

  appendMessage('Welcome to SkillMatch AI Assistant. How can I help you today?', 'bot');
})(); 



