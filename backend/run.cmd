@echo off
REM Carrega .env e sobe o backend. Nao precisa ter o Maven instalado:
REM o mvnw.cmd baixa o Maven sozinho na primeira vez. Precisa so do Java 21.
REM Uso: run.cmd  (no PowerShell: .\run.cmd)

cd /d "%~dp0"

if not exist .env (
  echo Aviso: .env nao encontrado. Copie .env.example para .env e preencha os valores.
  exit /b 1
)

where java >nul 2>nul
if errorlevel 1 if not defined JAVA_HOME (
  echo Erro: Java nao encontrado. Instale o Java 21 - https://adoptium.net - e abra um novo terminal.
  exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
  echo %%A| findstr /r "^#" >nul || set "%%A=%%B"
)

call mvnw.cmd spring-boot:run
