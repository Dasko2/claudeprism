// SPDX-License-Identifier: GPL-3.0-only
#include <QMap>
#include "ClientFeatures.h"

#include <QDateTime>
#include <QDebug>
#include <QDir>
#include <QFile>
#include <QFileInfo>
#include <QJsonDocument>
#include <QJsonObject>

#include "Application.h"
#include "FileSystem.h"
#include "minecraft/MinecraftInstance.h"
#include "minecraft/PackProfile.h"
#include "settings/SettingsObject.h"

namespace ClientFeatures {

QString loaderOf(MinecraftInstance* instance)
{
    auto profile = instance ? instance->getPackProfile() : nullptr;
    if (!profile)
        return {};
    if (profile->getComponent("net.neoforged"))
        return "neoforge";
    if (profile->getComponent("net.minecraftforge"))
        return "forge";
    if (profile->getComponent("org.quiltmc.quilt-loader"))
        return "quilt";
    if (profile->getComponent("net.fabricmc.fabric-loader"))
        return "fabric";
    return {};
}

static QString featuresDir()
{
    return FS::PathCombine(APPLICATION->dataRoot(), "features");
}

/// Extrait la charge utile embarquée (:/features/<loader>-<mc>.jar) et renvoie son chemin ; vide si absente.
static QString extractPayload(const QString& loader, const QString& mc)
{
    // version du mod la plus proche : les versions intermédiaires utilisent le build compatible
    static const QMap<QString, QString> alias = { { "1.21", "1.21.1" } };
    const QString resolved = alias.value(mc, mc);
    QStringList names;
    names << QString("%1-%2.jar").arg(loader, resolved);
    if (resolved != mc)
        names << QString("%1-%2.jar").arg(loader, mc);
    // 26.1.2 -> 26.1 : les versions de correctif utilisent le build de la version mineure
    const auto parts = mc.split('.');
    if (parts.size() >= 3)
        names << QString("%1-%2.%3.jar").arg(loader, parts[0], parts[1]);

    for (const QString& name : names) {
        QFile embedded(":/features/" + name);
        if (!embedded.exists() || !embedded.open(QIODevice::ReadOnly))
            continue;
        const QByteArray data = embedded.readAll();
        const QString dir = FS::PathCombine(featuresDir(), "payload");
        QDir().mkpath(dir);
        const QString target = FS::PathCombine(dir, name);
        QFile current(target);
        bool same = false;
        if (current.exists() && current.open(QIODevice::ReadOnly)) {
            same = (current.readAll() == data);
            current.close();
        }
        if (!same) {
            QFile::remove(target);
            QFile out(target);
            if (out.open(QIODevice::WriteOnly)) {
                out.write(data);
                out.close();
            }
        }
        return QFileInfo(target).absoluteFilePath();
    }
    return {};
}

void apply(MinecraftInstance* instance)
{
    if (!instance)
        return;
    auto* settings = APPLICATION->settings();

    // le mod est caché : on supprime toute copie visible laissée par une ancienne version
    QFile::remove(QDir(QDir(instance->gameRoot()).filePath("mods")).filePath("prismfeatures.jar"));

    if (!settings->get("FeaturesEnabled").toBool())
        return;

    QJsonObject config;
    config["nightVision"] = settings->get("FeatureNightVision").toBool();
    config["shulkerPreview"] = settings->get("FeatureShulkerPreview").toBool();
    config["hud"] = settings->get("FeatureHud").toBool();

    const QString configDir = QDir(instance->gameRoot()).filePath("config");
    FS::ensureFolderPathExists(configDir);
    QFile out(QDir(configDir).filePath("prismfeatures.json"));
    if (out.open(QIODevice::WriteOnly | QIODevice::Truncate))
        out.write(QJsonDocument(config).toJson(QJsonDocument::Indented));
}

void backupLatestWorld(MinecraftInstance* instance)
{
    if (!instance || !APPLICATION->settings()->get("AutoWorldBackup").toBool())
        return;
    QDir saves(QDir(instance->gameRoot()).filePath("saves"));
    const auto worlds = saves.entryInfoList(QDir::Dirs | QDir::NoDotAndDotDot, QDir::Time);
    if (worlds.isEmpty())
        return;
    const QFileInfo world = worlds.first();
    const QString root = QDir(instance->instanceRoot()).filePath("world-backups");
    FS::ensureFolderPathExists(root);
    const QString dst = QDir(root).filePath(world.fileName() + "-" + QDateTime::currentDateTime().toString("yyyyMMdd-HHmmss"));
    FS::copy(world.absoluteFilePath(), dst)();

    // on garde les 5 plus récentes
    QDir backups(root);
    const auto all = backups.entryInfoList(QDir::Dirs | QDir::NoDotAndDotDot, QDir::Time);
    for (int i = 5; i < all.size(); ++i)
        QDir(all[i].absoluteFilePath()).removeRecursively();
}

QString statusText(MinecraftInstance* instance)
{
    if (!APPLICATION->settings()->get("FeaturesEnabled").toBool())
        return "fonctionnalités désactivées dans les paramètres";
    auto profile = instance ? instance->getPackProfile() : nullptr;
    const QString mc = profile ? profile->getComponentVersion("net.minecraft") : QString();
    const QString loader = loaderOf(instance);
    if (loader.isEmpty())
        return QString("aucun mod loader (Vanilla), minecraft=%1 -> installez Fabric dans « Version modée »").arg(mc);
    if (loader != "fabric" && loader != "quilt")
        return QString("loader %1 pas encore pris en charge").arg(loader);
    const QString payload = extractPayload(loader, mc);
    if (payload.isEmpty())
        return QString("aucune charge utile pour %1 %2 (versions prises en charge : 1.19 à 1.21.11 et 26.1 à 26.3)").arg(loader, mc);
    return QString("OK : %1 %2, mod chargé depuis %3").arg(loader, mc, QFileInfo(payload).fileName());
}

QStringList jvmArguments(MinecraftInstance* instance)
{
    if (!APPLICATION->settings()->get("FeaturesEnabled").toBool())
        return {};

    const QString loader = loaderOf(instance);
    auto profile = instance->getPackProfile();
    const QString mc = profile ? profile->getComponentVersion("net.minecraft") : QString();

    if (loader != "fabric" && loader != "quilt") {
        qWarning() << "ClientFeatures : loader" << (loader.isEmpty() ? "vanilla" : loader) << "non pris en charge pour l'instant";
        return {};
    }
    const QString payload = extractPayload(loader, mc);
    if (payload.isEmpty()) {
        qWarning() << "ClientFeatures : aucune charge utile pour" << loader << mc;
        return {};
    }
    const QString path = QDir::toNativeSeparators(payload);
    qInfo() << "ClientFeatures : chargement de" << path;

    // Fabric et Quilt chargent directement ce jar, sans qu'il soit dans le dossier mods (donc invisible).
    QStringList args;
    args << QString("-Dfabric.addMods=%1").arg(path);
    if (loader == "quilt")
        args << QString("-Dloader.addMods=%1").arg(path);
    return args;
}

}  // namespace ClientFeatures
