# Prism Client Features (mod caché)

Mod client fourni avec le launcher : night vision (touche N), contenu des shulker boxes dans l'infobulle,
mémoire du contenu des coffres/barils déjà ouverts (affiché en les visant), FPS + coordonnées.
Chaque fonction se désactive dans *Paramètres → Fonctionnalités* du launcher.

## Architecture : un seul jar pour tous les loaders
`prismfeatures.jar` est un **java agent** (`-javaagent`, ajouté par le launcher). Il est chargé avant n'importe
quel loader, détecte loader + version de Minecraft (arguments passés par le launcher), extrait la bonne
charge utile (payload) hors du dossier mods et l'accroche au loader. Résultat : un seul fichier pour
l'utilisateur, jamais visible dans la page Mods.

Les payloads sont dans `agent/src/main/resources/payloads/` sous le nom `<loader>-<version>.jar`
(ou `<loader>-<major.minor>.jar`, ou `<loader>-any.jar`). Une version de Minecraft = un payload, car les
classes du jeu changent à chaque version. Le payload actuel : `fabric-1.21.1` (code dans `src/`).

| Loader   | Accroche dans l'agent                         | État |
|----------|-----------------------------------------------|------|
| Fabric   | propriété `fabric.addMods`                    | écrit, non testé |
| Quilt    | propriétés `loader.addMods` / `fabric.addMods`| écrit, non vérifié |
| Forge    | à trouver                                     | non fait |
| NeoForge | à trouver                                     | non fait |

## Compiler
JDK 21 + Gradle 8.8+ : `./build-all.sh` puis copier `agent/build/libs/prismfeatures-1.0.0.jar` dans
`<dossier de données du launcher>/features/prismfeatures.jar`. L'instance Fabric doit contenir Fabric API.
