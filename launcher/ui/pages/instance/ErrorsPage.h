// SPDX-License-Identifier: GPL-3.0-only
/*
 *  Prism Launcher - Minecraft Launcher
 *
 *  Page « Erreurs » de la console : affiche le fichier log de l'instance et
 *  chaque erreur détectée sous forme de carte.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3.
 */

#pragma once

#include <QList>
#include <QTimer>
#include <QWidget>

#include "launch/LaunchTask.h"
#include "launch/LogModel.h"
#include "minecraft/MinecraftInstance.h"
#include "ui/pages/BasePage.h"

class QLabel;
class QVBoxLayout;

class ErrorsPage : public QWidget, public BasePage {
    Q_OBJECT

   public:
    explicit ErrorsPage(MinecraftInstance* instance, QWidget* parent = nullptr);
    ~ErrorsPage() override = default;

    QString displayName() const override { return tr("Erreurs"); }
    QIcon icon() const override { return QIcon::fromTheme("status-bad"); }
    QString id() const override { return "errors"; }
    QString helpPage() const override { return "Minecraft-Logs"; }
    void openedImpl() override { refresh(); }

    int errorCount() const { return m_errorCount; }

   signals:
    void errorCountChanged(int count);

   public slots:
    void refresh();

   private slots:
    void onLaunchTaskChanged(LaunchTask* task);
    void scheduleRefresh();

   private:
    struct Entry {
        QString level;    // "ERROR", "FATAL" ou "CRASH"
        QString time;     // heure si connue
        QString title;    // première ligne
        QString details;  // trace complète (peut être vide)
    };

    void attachModel(LaunchTask* task);
    QList<Entry> collectFromModel() const;
    QList<Entry> collectFromFile(const QString& path) const;
    QList<Entry> collectCrashReport(const QString& logPath) const;
    QWidget* createCard(const Entry& entry);
    void rebuild(const QList<Entry>& entries);
    QString logFilePath() const;

   private:
    MinecraftInstance* m_instance;
    shared_qobject_ptr<LogModel> m_model;
    QTimer m_refreshTimer;

    QLabel* m_pathLabel = nullptr;
    QLabel* m_summaryLabel = nullptr;
    QLabel* m_sourceLabel = nullptr;
    QVBoxLayout* m_cardsLayout = nullptr;
    QWidget* m_emptyState = nullptr;
    int m_errorCount = 0;
};
