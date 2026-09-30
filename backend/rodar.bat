@echo off
rem Sobe o backend do A.E.G.I.S. (FastAPI + J.A.R.V.I.S.). Na primeira vez instala tudo sozinho.
cd /d "%~dp0"
if not exist .venv (
    echo Criando ambiente Python e instalando dependencias...
    python -m venv .venv || (echo Instale o Python 3.10+ marcando "Add python.exe to PATH". & pause & exit /b 1)
    .venv\Scripts\python -m pip install -r requirements.txt
)
if not exist .env (
    copy .env.example .env >nul
    echo Cole sua COMIC_VINE_API_KEY no .env, salve e feche o Bloco de Notas para continuar.
    notepad .env
)
echo.
echo Servidor em http://localhost:8000/docs   (Ctrl+C para parar)
.venv\Scripts\python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
pause
