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

  function appendStructured(structured) {
    const wrap = document.createElement('div');
    wrap.className = 'msg bot';

    const card = document.createElement('div');
    card.style.display = 'grid';
    card.style.gap = '10px';

    function addSection(title, itemsOrText) {
      if (!itemsOrText) return;
      const section = document.createElement('div');

      const h = document.createElement('div');
      h.style.fontWeight = '700';
      h.style.marginBottom = '4px';
      h.textContent = title;
      section.appendChild(h);

      if (Array.isArray(itemsOrText)) {
        if (itemsOrText.length === 0) return;
        const ul = document.createElement('ul');
        ul.style.margin = '0';
        ul.style.paddingLeft = '18px';
        itemsOrText.forEach((t) => {
          const li = document.createElement('li');
          li.textContent = String(t);
          ul.appendChild(li);
        });
        section.appendChild(ul);
      } else {
        const p = document.createElement('div');
        p.textContent = String(itemsOrText);
        section.appendChild(p);
      }

      card.appendChild(section);
    }

    addSection('Summary', structured.summary);
    addSection('Key points', structured.keyPoints);
    addSection('Next steps', structured.nextSteps);
    addSection('Examples', structured.examples);
    addSection('Warnings', structured.warnings);
    addSection('Follow-up questions', structured.followUpQuestions);

    if (card.childElementCount === 0) {
      addSection('Answer', structured.rawText || 'No response');
    }

    wrap.appendChild(card);
    chatEl.appendChild(wrap);
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
      if (data?.structured && typeof data.structured === 'object') {
        appendStructured(data.structured);
      } else {
        appendMessage(data.response ?? 'No response', 'bot');
      }
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



