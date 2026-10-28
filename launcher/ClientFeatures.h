// SPDX-License-Identifier: GPL-3.0-only
/*
 *  Fonctionnalités en jeu intégrées (night vision, aperçu des shulkers / coffres, HUD...).
 *  Le mod est CACHÉ : il n'est jamais copié dans le dossier mods (donc invisible dans la page Mods
 *  et non partageable avec un export d'instance). Le launcher le charge uniquement au lancement
 *  via -Dfabric.addMods (Fabric) / -Dloader.addMods (Quilt) ; la charge utile adaptée à la version de Minecraft est embarquée dans l'exécutable.
 */
#pragma once

#include <QString>
#include <QStringList>

class MinecraftInstance;

namespace ClientFeatures {
/// "fabric", "quilt", "forge", "neoforge" ou "" si l'instance n'a pas de loader géré.
QString loaderOf(MinecraftInstance* instance);

/// Écrit la configuration du mod et nettoie une éventuelle ancienne copie visible. Avant le lancement.
void apply(MinecraftInstance* instance);

/// Sauvegarde le monde le plus récemment joué (dossier world-backups, 5 copies conservées). Avant le lancement.
void backupLatestWorld(MinecraftInstance* instance);

/// Résumé lisible de l'état (agent trouvé ? loader ? version ?) pour le diagnostic.
QString statusText(MinecraftInstance* instance);

/// Arguments JVM qui chargent le mod caché (vide si désactivé / non disponible pour ce loader).
QStringList jvmArguments(MinecraftInstance* instance);
}  // namespace ClientFeatures
