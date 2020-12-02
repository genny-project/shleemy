#!/bin/bash

version=$(grep 'git.build.version')
pwd=${PWD##*/}
function prop() {
  grep "${1}=" ${file} | cut -d'=' -f2
}


echo "Building docker gennyproject/${pwd}:${version}"
./mvnw clean package -Dquarkus.container-image.build=true -DskipTests=true
docker tag ${USER}/${pwd}:${version}  gennyproject/${pwd}:latest 
docker tag ${USER}/${pwd}:${version}  gennyproject/${pwd}:${version} 
