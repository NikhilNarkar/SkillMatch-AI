# SkillMatch Desktop Chatbot (Python/Tkinter)

This is your original desktop chatbot, embedded in the project as a separate Python app.

## Prerequisites
- Python 3.9+
- pip

## Install
```bash
cd python
pip install -r requirements.txt
```

## Configure API Key
Set your Gemini API key in an environment variable:

Windows PowerShell:
```powershell
$env:GEMINI_API_KEY=\"YOUR_KEY_HERE\"
```

CMD:
```bat
set GEMINI_API_KEY=YOUR_KEY_HERE
```

Linux/macOS:
```bash
export GEMINI_API_KEY=YOUR_KEY_HERE
```

Optional: Choose the model (defaults to `gemini-1.5-flash-latest`):
```bash
export MODEL_NAME=gemini-1.5-pro-latest
```

## Run
```bash
python skillmatch_chatbot.py
```

The desktop window will open with the chat UI.

## Notes
- This app is independent from the Spring Boot server (web project). It runs locally as a desktop UI.
- Never commit real API keys to source control. Use environment variables.


