@echo off
cd %~dp0
echo Current directory: %cd%
pause

rem Generate Ziga's public/private key pair into private keystore
echo Generating Ziga's public private key pair
keytool -genkey -alias zigaPrivate -keystore ziga.private -storetype PKCS12 -keyalg rsa -dname "CN=Ziga" -storepass rkpwd1 -keypass rkpwd1 -validity 365
echo Done generating Ziga's key pair
pause

rem Generate Martin's public/private key pair into private keystore
echo Generating Martin's public private key pair
keytool -genkey -alias martinPrivate -keystore martin.private -storetype PKCS12 -keyalg rsa -dname "CN=Martin" -storepass rkpwd1 -keypass rkpwd1 -validity 365
echo Done generating Martin's key pair
pause

rem Generate Vid's public/private key pair into private keystore
echo Generating Vid's public private key pair
keytool -genkey -alias vidPrivate -keystore vid.private -storetype PKCS12 -keyalg rsa -dname "CN=Vid" -storepass rkpwd1 -keypass rkpwd1 -validity 365
echo Done generating Vid's key pair
pause

rem Generate server public/private key pair
echo Generating server public private key pair
keytool -genkey -alias serverPrivate -keystore server.private -storetype PKCS12 -keyalg rsa -dname "CN=localhost" -storepass serverpwd -keypass serverpwd -validity 365
echo Done generating server's key pair
pause

rem Export client public key and import it into public keystore
echo Generating client public key file (Ziga, Martin, Vid)
keytool -export -alias zigaPrivate -keystore ziga.private -file temp.key -storepass rkpwd1
keytool -import -noprompt -alias zigaPublic -keystore client.public -file temp.key -storepass public
del temp.key
pause
keytool -export -alias martinPrivate -keystore martin.private -file temp.key -storepass rkpwd1
keytool -import -noprompt -alias martinPublic -keystore client.public -file temp.key -storepass public
del temp.key
pause
keytool -export -alias vidPrivate -keystore vid.private -file temp.key -storepass rkpwd1
keytool -import -noprompt -alias vidPublic -keystore client.public -file temp.key -storepass public
del temp.key
pause

rem Export server public key and import it into public keystore
echo Generating server public key file
keytool -export -alias serverPrivate -keystore server.private -file temp.key -storepass serverpwd
keytool -import -noprompt -alias serverPublic -keystore server.public -file temp.key -storepass public
del temp.key
pause

echo All keys generated and imported successfully.
pause
