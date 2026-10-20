// SPDX-License-Identifier: GPL-3.0-only
// Adresse du serveur intermédiaire de l'IA (aucune clé à saisir pour l'utilisateur quand elle est définie).
#pragma once

#include <QString>

#include "Application.h"
#include "ai_proxy_url.h"
#include "settings/SettingsObject.h"

inline QString aiProxyUrl()
{
    QString url = APPLICATION->settings()->get("AiProxyUrl").toString().trimmed();
    if (url.isEmpty()) {
        url = QString::fromUtf8(PRISM_AI_PROXY_URL).trimmed();
    }
    return url.startsWith("https://") ? url : QString();
}
