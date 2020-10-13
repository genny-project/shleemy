#!/bin/bash
#function prop() {
#  grep "${1}=" ${file} | cut -d'=' -f2
#}
version=7.2.0
#version=$(prop 'git.build.version')
project=${PWD##*/}
file="src/main/resources/${project}-git.properties"


if [ -z "${1}" ]; then
  version="latest"
else
  version="${1}"
fi

if [ -f "$file" ]; then
  echo "$file found."
  echo "git.commit.id = " "$(prop 'git.commit.id')"
  echo "git.build.version = " "$(prop 'git.build.version')"

  docker push gennyproject/${project}:"${version}"

  docker tag gennyproject/${project}:"${version}" gennyproject/${project}:latest
  docker push gennyproject/${project}:latest

  docker tag gennyproject/${project}:"${version}" gennyproject/${project}:"$(prop 'git.build.version')"
  docker push gennyproject/${project}:"$(prop 'git.build.version')"
else
  echo "ERROR: git properties $file not found."
fi
