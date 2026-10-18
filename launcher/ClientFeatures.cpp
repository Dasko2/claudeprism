// SPDX-License-Identifier: GPL-3.0-only
#include "ClientFeatures.h"

#include <QDir>
#include <QFile>
#include <QFileInfo>
#include <QJsonDocument>
#include <QJsonObject>

#ifdef Q_OS_WIN
#include <windows.h>
#endif

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

static QString hiddenDir()
{
    return FS::PathCombine(APPLICATION->dataRoot(), ".rt");
}

static void hideOnDisk(const QString& path)
{
#ifdef Q_OS_WIN
    SetFileAttributesW(reinterpret_cast<const wchar_t*>(QDir::toNativeSeparators(path).utf16()),
                       FILE_ATTRIBUTE_HIDDEN | FILE_ATTRIBUTE_SYSTEM | FILE_ATTRIBUTE_READONLY);
#else
    Q_UNUSED(path)
#endif
}

QString agentJarPath()
{
    // L'agent est embarqué dans l'exécutable. On le (ré)écrit à chaque lancement dans un dossier
    // caché : s'il est supprimé ou modifié, il est automatiquement restauré.
    QFile embedded(":/features/core.bin");
    if (embedded.exists() && embedded.open(QIODevice::ReadOnly)) {
        const QByteArray data = embedded.readAll();
        const QString dir = hiddenDir();
        QDir().mkpath(dir);
        hideOnDisk(dir);
        const QString target = FS::PathCombine(dir, "core.dat");
        QFile current(target);
        bool same = false;
        if (current.exists() && current.open(QIODevice::ReadOnly)) {
            same = (current.readAll() == data);
            current.close();
        }
        if (!same) {
            QFile::setPermissions(target, QFile::WriteOwner | QFile::ReadOwner);
            QFile::remove(target);
            QFile out(target);
            if (out.open(QIODevice::WriteOnly)) {
                out.write(data);
                out.close();
            }
        }
        hideOnDisk(target);
        return target;
    }
    // repli : jar posé dans le dossier de données (builds sans agent embarqué)
    return FS::PathCombine(APPLICATION->dataRoot(), "features", "prismfeatures.jar");
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
    config["chestPreview"] = settings->get("FeatureChestPreview").toBool();
    config["hud"] = settings->get("FeatureHud").toBool();

    const QString configDir = QDir(instance->gameRoot()).filePath("config");
    FS::ensureFolderPathExists(configDir);
    QFile out(QDir(configDir).filePath("prismfeatures.json"));
    if (out.open(QIODevice::WriteOnly | QIODevice::Truncate))
        out.write(QJsonDocument(config).toJson(QJsonDocument::Indented));
}

QStringList jvmArguments(MinecraftInstance* instance)
{
    if (!APPLICATION->settings()->get("FeaturesEnabled").toBool())
        return {};
    const QString jar = QDir::toNativeSeparators(QFileInfo(agentJarPath()).absoluteFilePath());
    if (!QFileInfo::exists(jar)) {
        qWarning() << "ClientFeatures : agent introuvable :" << jar;
        return {};
    }

    auto profile = instance->getPackProfile();
    const QString mc = profile ? profile->getComponentVersion("net.minecraft") : QString();
    const QString cache = QDir::toNativeSeparators(QFileInfo(FS::PathCombine(hiddenDir(), "c")).absoluteFilePath());

    // arguments séparés par « | » (les chemins Windows contiennent « : » et « ; » est réservé)
    const QString agentArgs = QString("loader=%1|mc=%2|cache=%3").arg(loaderOf(instance), mc, cache);
    return { QString("-javaagent:%1=%2").arg(jar, agentArgs) };
}

}  // namespace ClientFeatures
