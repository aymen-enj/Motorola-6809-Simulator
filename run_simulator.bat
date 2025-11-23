@echo off
echo ========================================
echo Simulateur Motorola 6809 - Test des Flags
echo ========================================
echo.

echo Compilation en cours...
javac src/sim/Simulateur6809.java
if errorlevel 1 (
    echo ERREUR: Compilation echouee
    pause
    exit /b 1
)

echo Compilation reussie !
echo.
echo Lancement du simulateur...
echo.
echo Instructions :
echo - Cliquez sur "Pas a Pas" pour tester les flags
echo - Observez le registre CC (flags binaires)
echo - Consultez GUIDE_TEST_FLAGS.md pour les details
echo.

java -cp src sim.Simulateur6809

pause
