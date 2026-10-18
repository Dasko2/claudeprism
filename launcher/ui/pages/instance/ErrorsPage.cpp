// SPDX-License-Identifier: GPL-3.0-only
/*
 *  Prism Launcher - Minecraft Launcher
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3.
 */

#include "ErrorsPage.h"

#include <QClipboard>
#include <QDateTime>
#include <QDir>
#include <QFile>
#include <QFileInfo>
#include <QFrame>
#include <QGuiApplication>
#include <QHBoxLayout>
#include <QLabel>
#include <QPlainTextEdit>
#include <QPushButton>
#include <QRegularExpression>
#include <QScrollArea>
#include <QStringConverter>
#include <QTextStream>
#include <QToolButton>
#include <QVBoxLayout>

#include "DesktopServices.h"
#include "MessageLevel.h"

namespace {
constexpr int kMaxCards = 80;
constexpr qint64 kMaxLogBytes = 4 * 1024 * 1024;

const QRegularExpression& traceRegex()
{
    static const QRegularExpression rx(R"(^\s+at\s|^\s*Caused by:|^\s*\.\.\. \d+ more|^\s*Suppressed:)");
    return rx;
}

const QRegularExpression& exceptionRegex()
{
    static const QRegularExpression rx(R"(^(Exception in thread|[\w.$]+(Exception|Error)\b))");
    return rx;
}

const QRegularExpression& stampRegex()
{
    static const QRegularExpression rx(R"(^\[(\d{2}:\d{2}:\d{2})\])");
    return rx;
}

// "[12:00:00] [Render thread/ERROR]: message" -> "message"
QString cleanTitle(QString line, QString* timeOut)
{
    const auto stamp = stampRegex().match(line);
    if (stamp.hasMatch() && timeOut) {
        *timeOut = stamp.captured(1);
    }
    static const QRegularExpression prefix(R"(^\[\d{2}:\d{2}:\d{2}\]\s*(\[[^\]]*\]:?\s*)?)");
    line.remove(prefix);
    return line.trimmed();
}
}  // namespace

ErrorsPage::ErrorsPage(MinecraftInstance* instance, QWidget* parent) : QWidget(parent), m_instance(instance)
{
    setObjectName("errorsPage");
    setStyleSheet(
        "#errorsPage QFrame#logFileCard { background: palette(base); border: 1px solid palette(midlight); border-radius: 12px; }"
        "#errorsPage QFrame#errorCard { background: palette(base); border: 1px solid palette(midlight); border-left: 5px solid #e5484d; "
        "border-radius: 10px; }"
        "#errorsPage QFrame#errorCard[level=\"FATAL\"], #errorsPage QFrame#errorCard[level=\"CRASH\"] { border-left: 5px solid #b4232a; }"
        "#errorsPage QLabel#levelBadge { background: #e5484d; color: white; border-radius: 6px; padding: 1px 8px; font-weight: 700; "
        "font-size: 11px; }"
        "#errorsPage QLabel#cardTitle { font-weight: 600; }"
        "#errorsPage QLabel#muted { color: palette(placeholder-text); }"
        "#errorsPage QPlainTextEdit { background: palette(window); border: 1px solid palette(midlight); border-radius: 8px; "
        "font-family: monospace; }"
        "#errorsPage QPushButton, #errorsPage QToolButton { border-radius: 8px; padding: 6px 12px; }");

    auto* root = new QVBoxLayout(this);
    root->setContentsMargins(18, 14, 18, 14);
    root->setSpacing(12);

    // --- carte "fichier log" ---
    auto* fileCard = new QFrame(this);
    fileCard->setObjectName("logFileCard");
    auto* fileLayout = new QVBoxLayout(fileCard);
    fileLayout->setContentsMargins(16, 12, 16, 12);
    fileLayout->setSpacing(6);

    auto* fileTitle = new QLabel(tr("Fichier log"), fileCard);
    QFont titleFont = fileTitle->font();
    titleFont.setBold(true);
    titleFont.setPointSize(titleFont.pointSize() > 0 ? titleFont.pointSize() + 1 : 11);
    fileTitle->setFont(titleFont);
    fileLayout->addWidget(fileTitle);

    m_pathLabel = new QLabel(fileCard);
    m_pathLabel->setTextInteractionFlags(Qt::TextSelectableByMouse);
    m_pathLabel->setWordWrap(true);
    fileLayout->addWidget(m_pathLabel);

    m_sourceLabel = new QLabel(fileCard);
    m_sourceLabel->setObjectName("muted");
    fileLayout->addWidget(m_sourceLabel);

    auto* fileButtons = new QHBoxLayout();
    fileButtons->setSpacing(8);
    auto* openFile = new QPushButton(tr("Ouvrir le fichier log"), fileCard);
    auto* openFolder = new QPushButton(tr("Ouvrir le dossier"), fileCard);
    auto* copyPath = new QPushButton(tr("Copier le chemin"), fileCard);
    auto* reload = new QPushButton(tr("Actualiser"), fileCard);
    fileButtons->addWidget(openFile);
    fileButtons->addWidget(openFolder);
    fileButtons->addWidget(copyPath);
    fileButtons->addStretch(1);
    fileButtons->addWidget(reload);
    fileLayout->addLayout(fileButtons);
    root->addWidget(fileCard);

    connect(openFile, &QPushButton::clicked, this, [this] {
        const QString path = logFilePath();
        if (QFileInfo::exists(path)) {
            DesktopServices::openPath(QFileInfo(path));
        } else {
            DesktopServices::openPath(QFileInfo(path).absolutePath(), true);
        }
    });
    connect(openFolder, &QPushButton::clicked, this, [this] { DesktopServices::openPath(QFileInfo(logFilePath()).absolutePath(), true); });
    connect(copyPath, &QPushButton::clicked, this, [this] { QGuiApplication::clipboard()->setText(QDir::toNativeSeparators(logFilePath())); });
    connect(reload, &QPushButton::clicked, this, &ErrorsPage::refresh);

    // --- résumé ---
    m_summaryLabel = new QLabel(this);
    QFont summaryFont = m_summaryLabel->font();
    summaryFont.setBold(true);
    m_summaryLabel->setFont(summaryFont);
    root->addWidget(m_summaryLabel);

    // --- liste de cartes ---
    auto* scroll = new QScrollArea(this);
    scroll->setWidgetResizable(true);
    scroll->setFrameShape(QFrame::NoFrame);
    auto* holder = new QWidget(scroll);
    holder->setObjectName("errorsHolder");
    holder->setStyleSheet("#errorsHolder { background: transparent; }");
    m_cardsLayout = new QVBoxLayout(holder);
    m_cardsLayout->setContentsMargins(0, 0, 6, 0);
    m_cardsLayout->setSpacing(10);

    auto* emptyLabel = new QLabel(tr("Aucune erreur détectée pour le moment."), holder);
    emptyLabel->setObjectName("muted");
    emptyLabel->setAlignment(Qt::AlignCenter);
    m_emptyState = emptyLabel;
    m_cardsLayout->addWidget(m_emptyState);
    m_cardsLayout->addStretch(1);

    scroll->setWidget(holder);
    root->addWidget(scroll, 1);

    // --- rafraîchissement automatique ---
    m_refreshTimer.setSingleShot(true);
    m_refreshTimer.setInterval(600);
    connect(&m_refreshTimer, &QTimer::timeout, this, &ErrorsPage::refresh);

    connect(m_instance, &BaseInstance::launchTaskChanged, this, &ErrorsPage::onLaunchTaskChanged);
    connect(m_instance, &BaseInstance::runningStatusChanged, this, [this](bool) { scheduleRefresh(); });
    attachModel(m_instance->getLaunchTask());

    refresh();
}

QString ErrorsPage::logFilePath() const
{
    return QDir(m_instance->gameRoot()).filePath("logs/latest.log");
}

void ErrorsPage::attachModel(LaunchTask* task)
{
    if (m_model) {
        disconnect(m_model.get(), nullptr, this, nullptr);
    }
    m_model.reset();
    if (task) {
        m_model = task->getLogModel();
        if (m_model) {
            connect(m_model.get(), &QAbstractItemModel::rowsInserted, this, &ErrorsPage::scheduleRefresh);
            connect(m_model.get(), &QAbstractItemModel::modelReset, this, &ErrorsPage::scheduleRefresh);
            connect(m_model.get(), &QAbstractItemModel::rowsRemoved, this, &ErrorsPage::scheduleRefresh);
        }
    }
}

void ErrorsPage::onLaunchTaskChanged(LaunchTask* task)
{
    attachModel(task);
    scheduleRefresh();
}

void ErrorsPage::scheduleRefresh()
{
    if (!m_refreshTimer.isActive()) {
        m_refreshTimer.start();
    }
}

QList<ErrorsPage::Entry> ErrorsPage::collectFromModel() const
{
    QList<Entry> out;
    if (!m_model) {
        return out;
    }

    const int errorLevel = static_cast<int>(MessageLevelValue::Error);
    const int fatalLevel = static_cast<int>(MessageLevelValue::Fatal);
    const int stdErrLevel = static_cast<int>(MessageLevelValue::StdErr);

    bool open = false;
    Entry current;
    QStringList trace;
    auto flush = [&]() {
        if (open) {
            current.details = trace.join('\n');
            out.append(current);
        }
        open = false;
        current = Entry();
        trace.clear();
    };

    const int rows = m_model->rowCount();
    for (int i = 0; i < rows; ++i) {
        const QModelIndex idx = m_model->index(i, 0);
        const int level = m_model->data(idx, LogModel::LevelRole).toInt();
        const QString line = m_model->data(idx, Qt::DisplayRole).toString();

        const bool isTrace = traceRegex().match(line).hasMatch();
        const bool isError = level == errorLevel || level == fatalLevel;
        const bool isStdErrException = level == stdErrLevel && exceptionRegex().match(line).hasMatch();

        if (open && isTrace) {
            trace.append(line);
            continue;
        }
        if (isError || isStdErrException) {
            flush();
            open = true;
            current.level = (level == fatalLevel) ? QStringLiteral("FATAL") : QStringLiteral("ERROR");
            current.title = cleanTitle(line, &current.time);
            continue;
        }
        flush();
    }
    flush();
    return out;
}

QList<ErrorsPage::Entry> ErrorsPage::collectFromFile(const QString& path) const
{
    QList<Entry> out;
    QFile file(path);
    if (!file.open(QIODevice::ReadOnly | QIODevice::Text)) {
        return out;
    }
    if (file.size() > kMaxLogBytes) {
        file.seek(file.size() - kMaxLogBytes);
        file.readLine();  // jette la ligne coupée
    }

    static const QRegularExpression errorRx(R"(^\[(\d{2}:\d{2}:\d{2})\]\s*\[([^\]]*?)/(ERROR|FATAL)\]:?\s?(.*)$)");

    bool open = false;
    Entry current;
    QStringList trace;
    auto flush = [&]() {
        if (open) {
            current.details = trace.join('\n');
            out.append(current);
        }
        open = false;
        current = Entry();
        trace.clear();
    };

    QTextStream in(&file);
    in.setEncoding(QStringConverter::Utf8);
    while (!in.atEnd()) {
        const QString line = in.readLine();
        const auto match = errorRx.match(line);
        if (match.hasMatch()) {
            flush();
            open = true;
            current.time = match.captured(1);
            current.level = match.captured(3);
            current.title = match.captured(4).trimmed();
            continue;
        }
        if (open && !stampRegex().match(line).hasMatch()) {
            trace.append(line);
            continue;
        }
        flush();
    }
    flush();
    return out;
}

QList<ErrorsPage::Entry> ErrorsPage::collectCrashReport(const QString& logPath) const
{
    QList<Entry> out;
    QDir crashDir(QDir(m_instance->gameRoot()).filePath("crash-reports"));
    if (!crashDir.exists()) {
        return out;
    }
    const auto reports = crashDir.entryInfoList({ "*.txt" }, QDir::Files, QDir::Time);
    if (reports.isEmpty()) {
        return out;
    }
    const QFileInfo newest = reports.first();

    // on ne montre le crash report que s'il date de la dernière session
    const QFileInfo log(logPath);
    if (log.exists() && newest.lastModified().secsTo(log.lastModified()) > 120) {
        return out;
    }
    if (!log.exists() && newest.lastModified().secsTo(QDateTime::currentDateTime()) > 3600) {
        return out;
    }

    Entry entry;
    entry.level = "CRASH";
    entry.time = newest.lastModified().toString("HH:mm:ss");
    entry.title = tr("Crash report : %1").arg(newest.fileName());

    QFile file(newest.absoluteFilePath());
    if (file.open(QIODevice::ReadOnly | QIODevice::Text)) {
        QTextStream in(&file);
        in.setEncoding(QStringConverter::Utf8);
        QStringList head;
        for (int i = 0; i < 40 && !in.atEnd(); ++i) {
            head.append(in.readLine());
        }
        for (const QString& line : head) {
            if (line.startsWith("Description:")) {
                entry.title = tr("Crash : %1").arg(line.mid(12).trimmed());
                break;
            }
        }
        entry.details = head.join('\n');
    }
    out.append(entry);
    return out;
}

QWidget* ErrorsPage::createCard(const Entry& entry)
{
    auto* card = new QFrame();
    card->setObjectName("errorCard");
    card->setProperty("level", entry.level);

    auto* layout = new QVBoxLayout(card);
    layout->setContentsMargins(14, 10, 12, 10);
    layout->setSpacing(6);

    auto* top = new QHBoxLayout();
    top->setSpacing(8);
    auto* badge = new QLabel(entry.level, card);
    badge->setObjectName("levelBadge");
    top->addWidget(badge);
    if (!entry.time.isEmpty()) {
        auto* time = new QLabel(entry.time, card);
        time->setObjectName("muted");
        top->addWidget(time);
    }
    top->addStretch(1);
    auto* copy = new QToolButton(card);
    copy->setText(tr("Copier"));
    copy->setToolTip(tr("Copier cette erreur dans le presse-papiers"));
    top->addWidget(copy);
    layout->addLayout(top);

    auto* title = new QLabel(entry.title.isEmpty() ? tr("(message vide)") : entry.title, card);
    title->setObjectName("cardTitle");
    title->setWordWrap(true);
    title->setTextInteractionFlags(Qt::TextSelectableByMouse);
    layout->addWidget(title);

    const QString fullText = entry.details.isEmpty() ? entry.title : entry.title + "\n" + entry.details;
    connect(copy, &QToolButton::clicked, card, [fullText] { QGuiApplication::clipboard()->setText(fullText); });

    if (!entry.details.isEmpty()) {
        auto* toggle = new QToolButton(card);
        toggle->setText(tr("Afficher la trace complète"));
        toggle->setAutoRaise(true);
        layout->addWidget(toggle, 0, Qt::AlignLeft);

        auto* trace = new QPlainTextEdit(card);
        trace->setReadOnly(true);
        trace->setLineWrapMode(QPlainTextEdit::NoWrap);
        trace->setPlainText(entry.details);
        trace->setMaximumHeight(180);
        trace->hide();
        layout->addWidget(trace);

        connect(toggle, &QToolButton::clicked, card, [toggle, trace] {
            const bool show = !trace->isVisible();
            trace->setVisible(show);
            toggle->setText(show ? tr("Masquer la trace") : tr("Afficher la trace complète"));
        });
    }
    return card;
}

void ErrorsPage::rebuild(const QList<Entry>& entries)
{
    // enlève les anciennes cartes (le dernier item est le stretch, le premier l'état vide)
    while (m_cardsLayout->count() > 2) {
        QLayoutItem* item = m_cardsLayout->takeAt(1);
        if (item->widget()) {
            item->widget()->deleteLater();
        }
        delete item;
    }

    int inserted = 0;
    for (int i = entries.size() - 1; i >= 0 && inserted < kMaxCards; --i, ++inserted) {
        m_cardsLayout->insertWidget(1 + inserted, createCard(entries.at(i)));
    }

    m_emptyState->setVisible(entries.isEmpty());

    const int total = entries.size();
    if (total == 0) {
        m_summaryLabel->setText(tr("Aucune erreur"));
    } else if (total == 1) {
        m_summaryLabel->setText(tr("1 erreur détectée"));
    } else if (total > kMaxCards) {
        m_summaryLabel->setText(tr("%1 erreurs détectées (les %2 plus récentes sont affichées)").arg(total).arg(kMaxCards));
    } else {
        m_summaryLabel->setText(tr("%1 erreurs détectées").arg(total));
    }

    if (m_errorCount != total) {
        m_errorCount = total;
        emit errorCountChanged(m_errorCount);
    }
}

void ErrorsPage::refresh()
{
    const QString path = logFilePath();
    m_pathLabel->setText(QDir::toNativeSeparators(path) + (QFileInfo::exists(path) ? QString() : tr("  (fichier introuvable)")));

    QList<Entry> entries;
    if (m_model && m_model->rowCount() > 0) {
        entries = collectFromModel();
        m_sourceLabel->setText(tr("Source des erreurs : console en direct"));
    } else {
        entries = collectFromFile(path);
        m_sourceLabel->setText(tr("Source des erreurs : dernier fichier latest.log"));
    }
    entries.append(collectCrashReport(path));

    rebuild(entries);
}
