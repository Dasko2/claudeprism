// SPDX-License-Identifier: GPL-3.0-only
/*
 *  Prism Launcher - Minecraft Launcher
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3.
 */

#include "InstanceWorkspace.h"

#include <QHBoxLayout>
#include <QLabel>
#include <QStackedLayout>
#include <QTabBar>
#include <QVBoxLayout>

#include <FileSystem.h>
#include "ui/widgets/ModernStyle.h"
#include "minecraft/MinecraftInstance.h"

#include "ui/pages/instance/DataPackPage.h"
#include "ui/pages/instance/ErrorsPage.h"
#include "ui/pages/instance/InstanceSettingsPage.h"
#include "ui/pages/instance/LoaderPage.h"
#include "ui/pages/instance/LogPage.h"
#include "ui/pages/instance/ManagedPackPage.h"
#include "ui/pages/instance/ModFolderPage.h"
#include "ui/pages/instance/NotesPage.h"
#include "ui/pages/instance/OtherLogsPage.h"
#include "ui/pages/instance/ResourcePackPage.h"
#include "ui/pages/instance/ScreenshotsPage.h"
#include "ui/pages/instance/ServersPage.h"
#include "ui/pages/instance/ShaderPackPage.h"
#include "ui/pages/instance/TexturePackPage.h"
#include "ui/pages/instance/VersionPage.h"
#include "ui/pages/instance/WorldListPage.h"

InstanceWorkspace::InstanceWorkspace(QWidget* parent) : QWidget(parent)
{
    setObjectName("instanceWorkspace");
    setStyleSheet(ModernStyle::pages());

    auto* root = new QVBoxLayout(this);
    root->setContentsMargins(0, 0, 0, 0);
    root->setSpacing(0);

    // --- en-tête ---
    auto* header = new QWidget(this);
    header->setObjectName("workspaceHeader");
    auto* headerLayout = new QVBoxLayout(header);
    headerLayout->setContentsMargins(26, 20, 26, 6);
    headerLayout->setSpacing(2);
    m_title = new QLabel(header);
    m_title->setObjectName("workspaceTitle");
    m_subtitle = new QLabel(header);
    m_subtitle->setObjectName("workspaceSubtitle");
    headerLayout->addWidget(m_title);
    headerLayout->addWidget(m_subtitle);
    header->setStyleSheet(ModernStyle::workspaceHeader());
    root->addWidget(header);

    // --- sous-onglets ---
    auto* tabsHolder = new QWidget(this);
    auto* tabsLayout = new QHBoxLayout(tabsHolder);
    tabsLayout->setContentsMargins(26, 6, 26, 8);
    m_tabs = new QTabBar(tabsHolder);
    m_tabs->setObjectName("pageTabs");
    m_tabs->setExpanding(false);
    m_tabs->setDrawBase(false);
    m_tabs->setUsesScrollButtons(true);
    m_tabs->setFocusPolicy(Qt::NoFocus);
    tabsLayout->addWidget(m_tabs);
    tabsLayout->addStretch(1);
    tabsHolder->setStyleSheet(ModernStyle::workspaceTabs());
    tabsHolder->hide();
    root->addWidget(tabsHolder);
    connect(m_tabs, &QTabBar::currentChanged, this, &InstanceWorkspace::onTabChanged);

    // --- pages ---
    m_stack = new QStackedLayout();
    m_stack->setContentsMargins(0, 0, 0, 0);
    m_message = new QLabel(this);
    m_message->setObjectName("workspaceMessage");
    m_message->setAlignment(Qt::AlignCenter);
    m_message->setWordWrap(true);
    m_message->setStyleSheet(ModernStyle::workspaceMessage());
    m_stack->addWidget(m_message);
    root->addLayout(m_stack, 1);

    showMessage(tr("Sélectionnez d'abord une instance dans l'onglet « Instances »."));
}

InstanceWorkspace::~InstanceWorkspace()
{
    // les pages sont des enfants du widget : Qt s'occupe de les détruire
}

void InstanceWorkspace::setHeader(const QString& title, const QString& subtitle)
{
    m_title->setText(title);
    m_subtitle->setText(subtitle);
    m_subtitle->setVisible(!subtitle.isEmpty());
}

int InstanceWorkspace::errorCount() const
{
    return m_errors ? m_errors->errorCount() : 0;
}

QString InstanceWorkspace::currentPageId() const
{
    return m_current ? m_current->id() : QString();
}

void InstanceWorkspace::setInstance(MinecraftInstance* instance, bool saveBefore)
{
    if (instance == m_instance) {
        return;
    }
    if (saveBefore && m_instance) {
        saveAll();
    }
    destroyPages();
    m_instance = instance;

    if (m_instance) {
        m_errors = new ErrorsPage(m_instance, this);
        connect(m_errors, &ErrorsPage::errorCountChanged, this, &InstanceWorkspace::errorCountChanged);
        registerPage(m_errors);
    }
    emit errorCountChanged(errorCount());
}

void InstanceWorkspace::registerPage(BasePage* page)
{
    auto* widget = dynamic_cast<QWidget*>(page);
    if (!widget) {
        return;
    }
    widget->setParent(this);
    page->stackIndex = m_stack->addWidget(widget);
    page->setParentContainer(this);
    page->updateExtraInfo = [this](const QString& id, const QString& info) {
        auto* target = m_byId.value(id, nullptr);
        if (!target) {
            return;
        }
        for (int i = 0; i < m_tabs->count(); ++i) {
            if (m_tabs->tabData(i).toString() == id) {
                m_tabs->setTabText(i, target->displayName() + info);
            }
        }
    };
    m_pages.append(page);
    m_byId.insert(page->id(), page);
}

void InstanceWorkspace::ensurePages()
{
    if (m_pagesCreated || !m_instance) {
        return;
    }
    m_pagesCreated = true;
    auto* inst = m_instance;

    registerPage(new LogPage(inst));
    registerPage(new OtherLogsPage("logs", tr("Fichiers log"), "Other-Logs", inst));

    if (!inst->canEdit()) {
        return;
    }

    registerPage(new VersionPage(inst));
    registerPage(new LoaderPage(inst));
    registerPage(ManagedPackPage::createPage(inst));
    auto* modsPage = new ModFolderPage(inst, inst->loaderModList());
    modsPage->setFilter("%1 (*.zip *.jar *.litemod *.nilmod)");
    registerPage(modsPage);
    registerPage(new CoreModFolderPage(inst, inst->coreModList()));
    registerPage(new NilModFolderPage(inst, inst->nilModList()));
    registerPage(new ResourcePackPage(inst, inst->resourcePackList()));
    registerPage(new GlobalDataPackPage(inst));
    registerPage(new TexturePackPage(inst, inst->texturePackList()));
    registerPage(new ShaderPackPage(inst, inst->shaderPackList()));
    registerPage(new NotesPage(inst));
    registerPage(new WorldListPage(inst, inst->worldList()));
    registerPage(new ServersPage(inst));
    registerPage(new ScreenshotsPage(FS::PathCombine(inst->gameRoot(), "screenshots")));
    registerPage(new InstanceSettingsPage(inst));
}

void InstanceWorkspace::destroyPages()
{
    if (m_current) {
        m_current->closed();
        m_current = nullptr;
    }

    m_tabs->blockSignals(true);
    while (m_tabs->count() > 0) {
        m_tabs->removeTab(0);
    }
    m_tabs->blockSignals(false);

    for (BasePage* page : m_pages) {
        if (auto* widget = dynamic_cast<QWidget*>(page)) {
            m_stack->removeWidget(widget);
            delete widget;
        }
    }
    m_pages.clear();
    m_byId.clear();
    m_errors = nullptr;
    m_sectionIds.clear();
    m_pagesCreated = false;

    m_tabs->parentWidget()->hide();
    m_stack->setCurrentWidget(m_message);
}

bool InstanceWorkspace::saveAll()
{
    for (BasePage* page : m_pages) {
        if (!page->apply()) {
            return false;
        }
    }
    return true;
}

void InstanceWorkspace::showMessage(const QString& text)
{
    if (m_current) {
        m_current->closed();
        m_current = nullptr;
    }
    m_message->setText(text);
    m_stack->setCurrentWidget(m_message);
    m_tabs->parentWidget()->hide();
}

bool InstanceWorkspace::showSection(const QStringList& pageIds, const QString& preferredId)
{
    if (!m_instance) {
        showMessage(tr("Sélectionnez d'abord une instance dans l'onglet « Instances »."));
        return false;
    }
    ensurePages();
    m_sectionIds = pageIds;
    rebuildTabs(preferredId);
    return m_current != nullptr;
}

void InstanceWorkspace::rebuildTabs(const QString& preferredId)
{
    m_tabs->blockSignals(true);
    while (m_tabs->count() > 0) {
        m_tabs->removeTab(0);
    }

    int preferredIndex = -1;
    int keepIndex = -1;
    const QString currentId = currentPageId();
    for (const QString& id : m_sectionIds) {
        BasePage* page = m_byId.value(id, nullptr);
        if (!page || !page->shouldDisplay()) {
            continue;
        }
        const int index = m_tabs->addTab(page->icon(), page->displayName());
        m_tabs->setTabData(index, id);
        if (id == preferredId) {
            preferredIndex = index;
        }
        if (id == currentId) {
            keepIndex = index;
        }
    }
    m_tabs->blockSignals(false);

    if (m_tabs->count() == 0) {
        if (m_instance && !m_instance->canEdit()) {
            showMessage(tr("Cette instance ne peut pas être modifiée : elle est peut-être cassée ou trop ancienne. Consultez les logs pour plus de détails."));
        } else {
            showMessage(tr("Cette section n'est pas disponible pour cette instance (par exemple : aucun mod loader installé)."));
        }
        return;
    }

    m_tabs->parentWidget()->setVisible(m_tabs->count() > 1);

    int target = 0;
    if (preferredIndex >= 0) {
        target = preferredIndex;
    } else if (keepIndex >= 0) {
        target = keepIndex;
    }
    m_tabs->blockSignals(true);
    m_tabs->setCurrentIndex(target);
    m_tabs->blockSignals(false);

    showPage(m_byId.value(m_tabs->tabData(target).toString(), nullptr));
}

void InstanceWorkspace::showPage(BasePage* page)
{
    if (!page) {
        return;
    }
    if (m_current == page) {
        m_stack->setCurrentIndex(page->stackIndex);
        return;
    }
    if (m_current) {
        m_current->closed();
    }
    m_current = page;
    m_stack->setCurrentIndex(page->stackIndex);
    page->opened();
}

void InstanceWorkspace::onTabChanged(int index)
{
    if (index < 0) {
        return;
    }
    showPage(m_byId.value(m_tabs->tabData(index).toString(), nullptr));
}

bool InstanceWorkspace::selectPage(QString pageId)
{
    if (!m_byId.contains(pageId)) {
        return false;
    }
    emit pageRequested(pageId);
    return true;
}

BasePage* InstanceWorkspace::getPage(QString pageId)
{
    return m_byId.value(pageId, nullptr);
}

void InstanceWorkspace::refreshContainer()
{
    if (m_sectionIds.isEmpty() || !m_instance) {
        return;
    }
    rebuildTabs(currentPageId());
}
