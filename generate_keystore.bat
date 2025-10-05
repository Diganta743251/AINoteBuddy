@echo off
echo Generating release keystore for AI NoteBuddy...
echo.
echo Please provide the following information:
set /p KEYSTORE_PASSWORD="Enter keystore password: "
set /p KEY_ALIAS="Enter key alias (e.g., ainotebuddy): "
set /p KEY_PASSWORD="Enter key password: "
set /p DNAME_CN="Enter your name or organization: "
set /p DNAME_O="Enter organization name: "
set /p DNAME_C="Enter country code (e.g., US): "

echo.
echo Generating keystore...

keytool -genkey -v -keystore keystore/release.keystore -alias %KEY_ALIAS% -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=%DNAME_CN%, O=%DNAME_O%, C=%DNAME_C%" -storepass %KEYSTORE_PASSWORD% -keypass %KEY_PASSWORD%

echo.
echo Keystore generated successfully!
echo.
echo Add these to your gradle.properties file:
echo KEYSTORE_PASSWORD=%KEYSTORE_PASSWORD%
echo KEY_ALIAS=%KEY_ALIAS%
echo KEY_PASSWORD=%KEY_PASSWORD%
echo.
echo Or set as environment variables for CI/CD
pause