#!/bin/bash

if [[ ! -e /root/conf/private.key ]]; then
    openssl req -new -newkey rsa:4096 -days 365 -nodes -x509 -subj "/C=US/ST=Apache/L=Fundation/O=/CN=james.apache.org" -keyout /root/conf/private.key -out /root/conf/private.csr
fi

#BEGIN ADDED FOR WILDFLY TESTING
echo "Copying Test Configuration"
cp /root/testconf/*.xml /root/conf/
#END ADDED FOR WILDFLY TESTING

wait-for-it.sh --host=localhost --port=9999 --strict --timeout=0 -- ./initialdata.sh &

java -Djdk.tls.ephemeralDHKeySize=2048 \
     -classpath '/root/resources:/root/classes:/root/libs/*' \
     -javaagent:/root/libs/openjpa-3.2.0.jar \
     -Dlogback.configurationFile=/root/conf/logback.xml \
      -Dworking.directory=/root/ org.apache.james.JPAJamesServerMain