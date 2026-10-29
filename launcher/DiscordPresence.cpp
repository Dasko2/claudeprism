// SPDX-License-Identifier: GPL-3.0-only
#include "DiscordPresence.h"

#include <QCoreApplication>
#include <QDir>
#include <QFile>
#include <QJsonDocument>
#include <QJsonObject>
#include <QStandardPaths>
#include <QUuid>

#include "Application.h"
#include "ClientFeatures.h"
#include "minecraft/MinecraftInstance.h"
#include "minecraft/PackProfile.h"
#include "settings/SettingsObject.h"

DiscordPresence::DiscordPresence(QObject* parent) : QObject(parent)
{
    m_timer.setInterval(5000);
    connect(&m_timer, &QTimer::timeout, this, &DiscordPresence::update);
}

bool DiscordPresence::connectIpc()
{
    if (m_socket.state() == QLocalSocket::ConnectedState)
        return true;
    QStringList bases;
#ifdef Q_OS_WIN
    bases << QString();
#else
    const QString runtime = qEnvironmentVariable("XDG_RUNTIME_DIR", QStringLiteral("/tmp"));
    bases << runtime + "/" << runtime + "/app/com.discordapp.Discord/" << runtime + "/snap.discord/";
#endif
    for (const QString& base : bases) {
        for (int i = 0; i < 10; ++i) {
            m_socket.connectToServer(base + QStringLiteral("discord-ipc-") + QString::number(i));
            if (m_socket.waitForConnected(300))
                return true;
        }
    }
    return false;
}

void DiscordPresence::send(int opcode, const QByteArray& json)
{
    QByteArray packet;
    QDataStream out(&packet, QIODevice::WriteOnly);
    out.setByteOrder(QDataStream::LittleEndian);
    out << qint32(opcode) << qint32(json.size());
    packet.append(json);
    m_socket.write(packet);
    m_socket.flush();
}

void DiscordPresence::start(MinecraftInstance* instance)
{
    auto* s = APPLICATION->settings();
    m_clientId = s->get("DiscordClientId").toString().trimmed();
    if (!s->get("DiscordRpc").toBool() || m_clientId.isEmpty() || !instance)
        return;
    stop();
    m_instance = instance;
    m_startedAt = QDateTime::currentSecsSinceEpoch();
    m_ready = false;
    if (!connectIpc())
        return;  // Discord n'est pas ouvert : on n'insiste pas
    send(0, QJsonDocument(QJsonObject{ { "v", 1 }, { "client_id", m_clientId } }).toJson(QJsonDocument::Compact));
    m_socket.waitForReadyRead(500);
    m_socket.readAll();
    m_ready = true;
    m_lastServer = QStringLiteral("\x01");
    update();
    m_timer.start();
    m_conn = connect(instance, &BaseInstance::runningStatusChanged, this, [this](bool running) {
        if (!running)
            stop();
    });
}

void DiscordPresence::stop()
{
    QObject::disconnect(m_conn);
    m_timer.stop();
    if (m_ready && m_socket.state() == QLocalSocket::ConnectedState) {
        QJsonObject args{ { "pid", int(QCoreApplication::applicationPid()) } };
        send(1, QJsonDocument(QJsonObject{ { "cmd", "SET_ACTIVITY" }, { "args", args }, { "nonce", QUuid::createUuid().toString() } })
                    .toJson(QJsonDocument::Compact));
        m_socket.disconnectFromServer();
    }
    m_ready = false;
}

void DiscordPresence::update()
{
    if (!m_ready || !m_instance || m_socket.state() != QLocalSocket::ConnectedState)
        return;
    m_socket.readAll();

    QString server;
    QFile f(QDir(m_instance->gameRoot()).filePath("config/prismfeatures-status.json"));
    if (f.open(QIODevice::ReadOnly))
        server = QJsonDocument::fromJson(f.readAll()).object().value("server").toString();
    if (server == m_lastServer)
        return;
    m_lastServer = server;

    const QString mc = m_instance->getPackProfile()->getComponentVersion("net.minecraft");
    QString loader = ClientFeatures::loaderOf(m_instance.data());
    if (!loader.isEmpty())
        loader[0] = loader[0].toUpper();
    const QString details = "Minecraft " + mc + (loader.isEmpty() ? QString() : " (" + loader + ")");
    const QString state = server.isEmpty() ? tr("Dans les menus") : server == "Solo" ? tr("En solo") : tr("Serveur : %1").arg(server);

    QJsonObject activity{ { "details", details },
                          { "state", state },
                          { "timestamps", QJsonObject{ { "start", qint64(m_startedAt) } } } };
    QJsonObject args{ { "pid", int(QCoreApplication::applicationPid()) }, { "activity", activity } };
    send(1, QJsonDocument(QJsonObject{ { "cmd", "SET_ACTIVITY" }, { "args", args }, { "nonce", QUuid::createUuid().toString() } })
                .toJson(QJsonDocument::Compact));
}
