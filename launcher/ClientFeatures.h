#pragma once

#include <QString>
#include <QStringList>

class MinecraftInstance;

namespace ClientFeatures {

QString loaderOf(MinecraftInstance* instance);

void apply(MinecraftInstance* instance);

void backupLatestWorld(MinecraftInstance* instance);

QString statusText(MinecraftInstance* instance);

QStringList jvmArguments(MinecraftInstance* instance);
}
