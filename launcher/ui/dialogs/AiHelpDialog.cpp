// SPDX-License-Identifier: GPL-3.0-only

#include "AiHelpDialog.h"

#include <QDesktopServices>
#include <QDir>
#include <QJsonArray>
#include <QJsonDocument>
#include <QJsonObject>
#include <QLabel>
#include <QLineEdit>
#include <QNetworkReply>
#include <QNetworkRequest>
#include <QPlainTextEdit>
#include <QPushButton>
#include <QRegularExpression>
#include <QTextBrowser>
#include <QUrl>
#include <QVBoxLayout>

#include "AiConfig.h"
#include "Application.h"
#include "settings/SettingsObject.h"

namespace {
}  // namespace

AiHelpDialog::AiHelpDialog(const QString& errorText, const QString& context, QWidget* parent)
    : QDialog(parent), m_error(redact(errorText)), m_context(redact(context))
{
    setWindowTitle(tr("Demander à l'IA"));
    resize(640, 560);
    auto* root = new QVBoxLayout(this);
    root->setSpacing(10);

    auto* info = new QLabel(
        tr("L'erreur ci-dessous (et la version de Minecraft / du loader) sera envoyée à l'IA pour t'aider. "
           "Les chemins personnels et les jetons sont masqués avant l'envoi."),
        this);
    info->setWordWrap(true);
    root->addWidget(info);

    auto* preview = new QPlainTextEdit(this);
    preview->setReadOnly(true);
    preview->setMaximumHeight(130);
    preview->setPlainText(m_error);
    root->addWidget(preview);

    m_question = new QPlainTextEdit(this);
    m_question->setPlaceholderText(tr("Ta question (facultatif) : par exemple « Que dois-je faire pour que ça relance ? »"));
    m_question->setMaximumHeight(70);
    root->addWidget(m_question);

    m_sendButton = new QPushButton(tr("Demander de l'aide"), this);
    connect(m_sendButton, &QPushButton::clicked, this, &AiHelpDialog::send);
    root->addWidget(m_sendButton);

    m_status = new QLabel(this);
    root->addWidget(m_status);

    m_answer = new QTextBrowser(this);
    m_answer->setOpenExternalLinks(true);
    root->addWidget(m_answer, 1);
}

QString AiHelpDialog::redact(QString text)
{
    const QString home = QDir::homePath();
    if (!home.isEmpty()) {
        text.replace(home, "~");
        text.replace(QDir::toNativeSeparators(home), "~");
    }
    static const QRegularExpression token(R"((?i)(accessToken|access_token|session|token|password|key)(["'=:\s]+)[A-Za-z0-9._\-]{8,})");
    text.replace(token, "\\1\\2[masqué]");
    static const QRegularExpression ip(R"(\b\d{1,3}(\.\d{1,3}){3}\b)");
    text.replace(ip, "[ip]");
    if (text.size() > 7000) {
        text = text.left(7000) + "\n[... tronqué ...]";
    }
    return text;
}

void AiHelpDialog::send()
{
    const QString proxy = aiProxyUrl();
    if (proxy.isEmpty()) {
        m_status->setText(tr("L'IA n'est pas encore disponible dans cette version du launcher."));
        return;
    }

    QString user = tr("Contexte :\n%1\n\nErreur :\n%2").arg(m_context, m_error);
    const QString q = m_question->toPlainText().trimmed();
    if (!q.isEmpty()) {
        user += tr("\n\nQuestion de l'utilisateur : %1").arg(q);
    }

    QJsonObject body;
    body["mode"] = "errors";
    body["context"] = m_context;
    body["messages"] = QJsonArray{ QJsonObject{ { "role", "user" }, { "content", user } } };

    QNetworkRequest req{ QUrl(proxy) };
    req.setHeader(QNetworkRequest::ContentTypeHeader, "application/json");

    m_sendButton->setEnabled(false);
    m_status->setText(tr("L'IA réfléchit…"));
    m_answer->clear();
    m_reply = APPLICATION->network()->post(req, QJsonDocument(body).toJson(QJsonDocument::Compact));
    connect(m_reply, &QNetworkReply::finished, this, &AiHelpDialog::onFinished);
}

void AiHelpDialog::onFinished()
{
    auto* reply = m_reply;
    m_reply = nullptr;
    m_sendButton->setEnabled(true);
    const QByteArray reply_body = reply->readAll();
    const int status = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
    reply->deleteLater();

    const QJsonObject obj = QJsonDocument::fromJson(reply_body).object();
    if (status == 401) {
        m_status->setText(tr("Clé API refusée : vérifie-la."));
        return;
    }
    if (status != 200) {
        const QString msg = obj["error"].toObject()["message"].toString();
        m_status->setText(tr("Erreur (%1) : %2").arg(status).arg(msg.isEmpty() ? tr("impossible de joindre l'IA") : msg));
        return;
    }
    QString answer;
    for (const auto& part : obj["content"].toArray()) {
        answer += part.toObject()["text"].toString();
    }
    m_status->clear();
    m_answer->setMarkdown(answer);
}
