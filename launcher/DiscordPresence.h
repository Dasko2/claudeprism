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
