@echo off
setlocal EnableDelayedExpansion
title Lancement Automatique SGPA Pharmacie

echo ===================================================
echo      LANCEMENT AUTOMATIQUE SGPA PHARMACIE
echo ===================================================
echo(

REM --- 1. VERIFICATION DE L'ENVIRONNEMENT ---

REM Verification de Java
java -version >nul 2>&1
if errorlevel 1 goto ERR_JAVA

REM Verification de MySQL
mysql --version >nul 2>&1
if errorlevel 1 goto ERR_MYSQL

REM Verification de Maven
call mvn -version >nul 2>&1
if errorlevel 1 goto ERR_MAVEN

REM --- 2. CONFIGURATION ---

set "need_config=1"
if not exist "database.properties" goto DO_CONFIG

echo [INFO] Configuration trouvee (database.properties).
set /p "reconfig=Voulez-vous la reconfigurer ? (O/N) Default=N : "
if /i "!reconfig!"=="O" goto DO_CONFIG
set "need_config=0"
goto SKIP_CONFIG

:DO_CONFIG
echo(
echo --- Configuration de la Base de Donnees ---
echo Veuillez entrer le mot de passe ROOT de votre MySQL local.
echo (Appuyez sur Entree si pas de mot de passe)
set "dbpass="
set /p "dbpass=Mot de passe MySQL : "

echo Generation du fichier database.properties...

echo db.url=jdbc:mysql://127.0.0.1:3306/sgpa_pharmacie?useSSL=false^&serverTimezone=Europe/Paris^&allowPublicKeyRetrieval=true> database.properties
echo db.username=root>> database.properties

if defined dbpass (
    echo db.password=!dbpass!>> database.properties
) else (
    echo db.password=>> database.properties
)

echo db.pool.size=10>> database.properties
echo db.pool.minIdle=5>> database.properties
echo db.pool.idleTimeout=300000>> database.properties
echo db.pool.connectionTimeout=20000>> database.properties
echo db.pool.maxLifetime=1200000>> database.properties

echo [OK] Configuration sauvegardee.

echo(
echo --- Initialisation de la Base de Donnees ---
echo Creation de la base et injection des donnees...

set "mysql_cmd=mysql -u root"
if defined dbpass set "mysql_cmd=!mysql_cmd! -p!dbpass!"

echo Execution de schema.sql...
!mysql_cmd! < sql/schema.sql
if errorlevel 1 goto ERR_SCHEMA

echo Injection des donnees de test...
!mysql_cmd! < sql/seed_data.sql
if errorlevel 1 goto ERR_SEED

echo [SUCCES] Base de donnees initialisee !

:SKIP_CONFIG
REM --- 3. LANCEMENT ---
echo(
echo ===================================================
echo      LANCEMENT DE L'APPLICATION (MAVEN)
echo ===================================================
echo(
echo Lancement en cours...
echo(

call mvn clean javafx:run
if errorlevel 1 goto ERR_APP

echo(
echo Application fermee.
pause
exit /b

:ERR_JAVA
echo [ERREUR] Java n'est pas installe ou n'est pas dans le PATH.
echo Veuillez installer Java 21 ou plus recent.
pause
exit /b

:ERR_MYSQL
echo [ERREUR] MySQL n'est pas installe ou n'est pas dans le PATH.
echo Veuillez installer MySQL Server et l'ajouter au PATH.
pause
exit /b

:ERR_MAVEN
echo [ERREUR] Maven n'est pas installe ou n'est pas dans le PATH.
echo Veuillez installer Maven et l'ajouter au PATH.
pause
exit /b

:ERR_SCHEMA
echo [ERREUR] Echec lors de la creation du schema. Verifiez le mot de passe.
pause
exit /b

:ERR_SEED
echo [ERREUR] Echec lors de l'injection des donnees.
pause
exit /b

:ERR_APP
echo(
echo [ERREUR] L'application s'est fermee avec une erreur.
pause
exit /b
