#!/bin/bash
realm=crowtech

echo "Building native"

pwd=${PWD##*/}
pwd=aible-quarkus
function prop() {
  grep "${1}=" ${file} | cut -d'=' -f2
}

echo "Building docker ${realm}/${pwd}:${version}"

./mvnw  package -Pnative -Dquarkus.native.container-build=true -Dquarkus.container-image.build=true -DskipTests=true
version=$(cat src/main/resources/${pwd}-git.properties | grep 'git.build.version' | cut -d'=' -f2)
#version=$(grep 'git.build.version')
echo $version
docker tag ${USER}/${pwd}:${version} ${realm}/${pwd}:native

