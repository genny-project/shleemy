#!/bin/bash
#copy the graal certs 
#Contents/Home/lib/security/cacerts
#/Library/Java/JavaVirtualMachines/graalvm-ce-java11-19.3.1/Contents/Home
#mvn package -Pnative -Dquarkus.native.container-build=true  -DskipTests=true

./build-native.sh
#./mvnw package -Pnative -Dquarkus.native.container-build=true -DskipTests=true
docker build -f src/main/docker/Dockerfile.native -t gennyproject/shleemy:latest .
docker tag gennyproject/notes:latest gennyproject/shleemy:7.3.0

