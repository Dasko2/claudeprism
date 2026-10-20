// SPDX-License-Identifier: GPL-3.0-only
/*
 *  Aide d'une IA sur une erreur Minecraft : envoie l'erreur à l'API Claude avec une consigne
 *  stricte (aider uniquement l'utilisateur à résoudre son problème).
 */

#pragma once

#include <QDialog>

class QLabel;
class QLineEdit;
class QPlainTextEdit;
class QPushButton;
class QTextBrowser;
class QNetworkReply;

class AiHelpDialog : public QDialog {
    Q_OBJECT

   public:
    /// errorText : titre + trace de l'erreur ; context : version de Minecraft, loader, etc.
    AiHelpDialog(const QString& errorText, const QString& context, QWidget* parent = nullptr);

   private slots:
    void send();
    void onFinished();

   private:
    static QString redact(QString text);

    QString m_error;
    QString m_context;
    QPlainTextEdit* m_question = nullptr;
    QPushButton* m_sendButton = nullptr;
    QTextBrowser* m_answer = nullptr;
    QLabel* m_status = nullptr;
    QNetworkReply* m_reply = nullptr;
};
