#!/bin/sh
set -e
cd "$(dirname "$0")"
MC=${1:-1.21.11}; YARN=${2:-1.21.11+build.6}; COMPAT=${3:-new}; FAMILY=${4:-dc}; JAVA=${5:-21}
mkdir -p agent/src/main/resources/payloads
gradle clean build -Pminecraft_version=$MC -Pyarn_mappings=$YARN -Pcompat=$COMPAT -Pfamily=$FAMILY -PjavaVersion=$JAVA
cp build/libs/prismfeatures-fabric-*.jar agent/src/main/resources/payloads/fabric-$MC.jar
