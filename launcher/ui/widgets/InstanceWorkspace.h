// SPDX-License-Identifier: GPL-3.0-only
/*
 *  Prism Launcher - Minecraft Launcher
 *
 *  Zone de travail de la fenêtre principale : affiche, pour l'instance sélectionnée,
 *  les pages d'instance (console, mods, serveurs, resource packs, shaders, versions...)
 *  directement dans la fenêtre principale au lieu d'une fenêtre séparée.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3.
 */

#pragma once

#include <QHash>
#include <QList>
#include <QStringList>
#include <QWidget>

#include "ui/pages/BasePage.h"
#include "ui/pages/BasePageContainer.h"

class QLabel;
class QStackedLayout;
class QTabBar;
class MinecraftInstance;
class ErrorsPage;

class InstanceWorkspace : public QWidget, public BasePageContainer {
    Q_OBJECT

   public:
    explicit InstanceWorkspace(QWidget* parent = nullptr);
    ~InstanceWorkspace() override;

    /// Définit l'instance affichée (nullptr pour vider). Les pages sont créées à la demande.
    void setInstance(MinecraftInstance* instance, bool saveBefore = true);
    MinecraftInstance* instance() const { return m_instance; }

    /// Titre / sous-titre affichés au-dessus des pages.
    void setHeader(const QString& title, const QString& subtitle);

    /// Affiche une section : chaque id de page devient un sous-onglet. Retourne false si rien n'est affichable.
    bool showSection(const QStringList& pageIds, const QString& preferredId = QString());

    /// Affiche un message (aucune instance, section indisponible...).
    void showMessage(const QString& text);

    /// Sauvegarde les réglages de toutes les pages créées.
    bool saveAll();

    int errorCount() const;
    QString currentPageId() const;

    // BasePageContainer
    bool selectPage(QString pageId) override;
    BasePage* selectedPage() const override { return m_current; }
    BasePage* getPage(QString pageId) override;
    void refreshContainer() override;
    bool requestClose() override { return false; }

   signals:
    void errorCountChanged(int count);
    /// Une page demande l'affichage d'une autre page (ex. depuis la console).
    void pageRequested(QString pageId);

   private slots:
    void onTabChanged(int index);

   private:
    void ensurePages();
    void registerPage(BasePage* page);
    void destroyPages();
    void rebuildTabs(const QString& preferredId);
    void showPage(BasePage* page);

   private:
    MinecraftInstance* m_instance = nullptr;

    QLabel* m_title = nullptr;
    QLabel* m_subtitle = nullptr;
    QTabBar* m_tabs = nullptr;
    QStackedLayout* m_stack = nullptr;
    QLabel* m_message = nullptr;

    QList<BasePage*> m_pages;
    QHash<QString, BasePage*> m_byId;
    ErrorsPage* m_errors = nullptr;
    QStringList m_sectionIds;
    BasePage* m_current = nullptr;
    bool m_pagesCreated = false;
};
