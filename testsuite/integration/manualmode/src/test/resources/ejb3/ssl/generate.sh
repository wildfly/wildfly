#!/bin/sh
# script for re-generating the keystores and truststores needed for SSLEJBRemoteClientTestCase

# Create server keystore - file server.keystore
keytool -genkey -v -alias jbossalias -keyalg RSA -keysize 1024 -keystore server.keystore -validity 3650 -keypass 123456 -storepass 123456 -dname "CN=localhost, OU=Server Unit, O=Server, L=Brno, S=JBoss, C=IN"

# Export Server's Public Key - file server.cer
keytool -export -keystore server.keystore -alias jbossalias -file server.cer -keypass 123456 -storepass 123456

# Export Client Key Store - file client.keystore
keytool -genkey -v -alias clientalias -keyalg RSA -keysize 1024 -keystore client.keystore -validity 3650 -keypass abcdef -storepass abcdef -dname "CN=localhost, OU=Client Unit, O=Client, L=Brno, S=JBoss, C=IN"

# Exporting Client's Public Key - file client.cer
keytool -export -keystore client.keystore -alias clientalias -file client.cer -keypass abcdef -storepass abcdef

# Importing Client's Public key into server's truststore
keytool -import -v -trustcacerts -alias clientalias -file client.cer -keystore server.keystore -keypass 123456 -storepass 123456

# Importing Server's Public key into client's truststore
keytool -import -v -trustcacerts -alias jbossalias -file server.cer -keystore client.keystore -keypass abcdef -storepass abcdef
