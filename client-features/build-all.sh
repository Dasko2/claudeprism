#!/bin/sh
# Construit le jar unique prismfeatures.jar (agent + toutes les charges utiles).
# Prérequis : JDK 21 et Gradle 8.8+.
set -e
cd "$(dirname "$0")"

# 1) charges utiles : une par loader / version de Minecraft
mkdir -p agent/src/main/resources/payloads
gradle build
cp build/libs/prismfeatures-fabric-*.jar agent/src/main/resources/payloads/fabric-1.21.1.jar
# (ajouter ici d'autres payloads : fabric-1.20.1.jar, ... forge-1.20.1.jar, neoforge-1.21.1.jar)

# 2) agent unique qui les embarque
cd agent
gradle jar
echo "OK -> agent/build/libs/prismfeatures-1.0.0.jar"
echo "Copier ce fichier dans <dossier de données du launcher>/features/prismfeatures.jar"
