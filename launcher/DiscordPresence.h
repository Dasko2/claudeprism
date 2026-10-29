// SPDX-License-Identifier: GPL-3.0-only
/*
 *  Discord Rich Presence sans bibliothèque : parle directement au client Discord par son tube local.
 *  Affiche « Joue à <nom de l'application> », la version de Minecraft et le serveur rejoint
 *  (lu dans config/prismfeatures-status.json, écrit par le mod intégré).
 */
#pragma once

#include <QDateTime>
#include <QLocalSocket>
#include <QObject>
#include <QPointer>
#include <QTimer>

class MinecraftInstance;

class DiscordPresence : public QObject {
    Q_OBJECT
   public:
    explicit DiscordPresence(QObject* parent = nullptr);
    /// Démarre l'affichage pour cette instance (ne fait rien si désactivé ou sans identifiant d'application).
    void start(MinecraftInstance* instance);
    void stop();

   private:
    bool connectIpc();
    void send(int opcode, const QByteArray& json);
    void update();

    QLocalSocket m_socket;
    QTimer m_timer;
    QPointer<MinecraftInstance> m_instance;
    QString m_clientId;
    qint64 m_startedAt = 0;
    QString m_lastServer = QStringLiteral("\x01");
    bool m_ready = false;
    QMetaObject::Connection m_conn;
};
