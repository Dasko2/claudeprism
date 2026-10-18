// SPDX-License-Identifier: GPL-3.0-only
/*
 *  Fonctionnalités en jeu intégrées (night vision, aperçu des shulkers / coffres, HUD...).
 *  Le mod est CACHÉ : il n'est jamais copié dans le dossier mods (donc invisible dans la page Mods
 *  et non partageable avec un export d'instance). Le launcher le charge uniquement au lancement
 *  avec -javaagent (valable pour tous les loaders) ; l'agent choisit lui-même la bonne charge utile.
 */
#pragma once

#include <QString>
#include <QStringList>

class MinecraftInstance;

namespace ClientFeatures {
/// "fabric", "quilt", "forge", "neoforge" ou "" si l'instance n'a pas de loader géré.
QString loaderOf(MinecraftInstance* instance);

/// Jar unique (java agent) : <données>/features/prismfeatures.jar
QString agentJarPath();

/// Écrit la configuration du mod et nettoie une éventuelle ancienne copie visible. Avant le lancement.
void apply(MinecraftInstance* instance);

/// Arguments JVM qui chargent le mod caché (vide si désactivé / non disponible pour ce loader).
QStringList jvmArguments(MinecraftInstance* instance);
}  // namespace ClientFeatures
