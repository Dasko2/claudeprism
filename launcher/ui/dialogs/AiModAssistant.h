// SPDX-License-Identifier: GPL-3.0-only
/*
 *  Assistant IA des mods : l'utilisateur décrit ce qu'il veut (« de nouveaux biomes »), l'IA cherche sur Modrinth,
 *  propose des mods compatibles avec l'instance (installés seulement après confirmation de l'utilisateur),
 *  explique ce qu'ils font, et peut les retirer sur demande.
 */

#pragma once

#include <QDialog>
#include <QJsonArray>
#include <QJsonObject>
#include <functional>

class QLabel;
class QLineEdit;
class QPushButton;
class QTextBrowser;

class AiModAssistant : public QDialog {
    Q_OBJECT

   public:
    /// modsDir : dossier des mods de l'instance ; onChanged : appelé après une installation/suppression.
    AiModAssistant(const QString& instanceName,
                   const QString& mcVersion,
                   const QString& loader,
                   const QString& modsDir,
                   const QString& stateFile,
                   std::function<void()> onChanged,
                   QWidget* parent = nullptr);

   private slots:
    void sendUserMessage();

   private:
    // réseau (bloquant mais l'interface reste réactive)
    QByteArray httpRequest(const QUrl& url, const QByteArray& postBody, const QList<QPair<QByteArray, QByteArray>>& headers, int* status);
    QJsonObject callClaude();
    void runConversation();

    // outils exposés à l'IA
    QString runTool(const QString& name, const QJsonObject& input);
    QString toolSearch(const QString& query);
    QString toolInstall(const QString& projectId);
    QString toolRemove(const QString& projectId);
    QString toolList();

    bool installProject(const QString& projectId, const QString& requiredBy, QStringList& log, int depth);
    QJsonObject loadState() const;
    void saveState(const QJsonObject& state) const;
    void append(const QString& who, const QString& text);
    QJsonArray tools() const;

    QString m_instance;
    QString m_mc;
    QString m_loader;
    QString m_modsDir;
    QString m_stateFile;
    std::function<void()> m_onChanged;

    QJsonArray m_messages;
    bool m_busy = false;

    QTextBrowser* m_chat = nullptr;
    QLineEdit* m_input = nullptr;
    QLineEdit* m_keyEdit = nullptr;
    QPushButton* m_send = nullptr;
    QLabel* m_status = nullptr;
};
