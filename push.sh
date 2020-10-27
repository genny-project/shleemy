#!/bin/bash
version=7.3.0
project=${PWD##*/}
file="src/main/resources/${project}-git.properties"


if [ -z "${1}" ]; then
  version="latest"
else
  version="${1}"
fi

if [ -f "$file" ]; then
  docker push gennyproject/${project}:"${version}"
  docker tag gennyproject/${project}:"${version}" gennyproject/${project}:latest
  docker push gennyproject/${project}:latest
else
  echo "ERROR: git properties $file not found."
fi
