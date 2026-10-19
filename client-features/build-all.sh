#!/bin/sh
# Construit le payload d'une version : sh build-all.sh <mc> <yarn> <compat>
# (sans argument : 1.21.11). Le jar est copié dans agent/src/main/resources/payloads/.
set -e
cd "$(dirname "$0")"
MC=${1:-1.21.11}; YARN=${2:-1.21.11+build.6}; COMPAT=${3:-new}
mkdir -p agent/src/main/resources/payloads
gradle clean build -Pminecraft_version=$MC -Pyarn_mappings=$YARN -Pcompat=$COMPAT
cp build/libs/prismfeatures-fabric-*.jar agent/src/main/resources/payloads/fabric-$MC.jar
