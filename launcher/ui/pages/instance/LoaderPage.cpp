// SPDX-License-Identifier: GPL-3.0-only
/*
 *  Prism Launcher - Minecraft Launcher
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3.
 */

#include "LoaderPage.h"

#include <QFrame>
#include <QGridLayout>
#include <QHBoxLayout>
#include <QLabel>
#include <QMessageBox>
#include <QPushButton>
#include <QScrollArea>
#include <QStyle>
#include <QVBoxLayout>

#include "minecraft/PackProfile.h"
#include "ui/dialogs/InstallLoaderDialog.h"

LoaderPage::LoaderPage(MinecraftInstance* instance, QWidget* parent) : QWidget(parent), m_instance(instance)
{
    setObjectName("loaderPage");
    setStyleSheet(
        "#loaderPage QFrame#loaderCard { background: palette(base); border: 1px solid palette(midlight); border-radius: 14px; }"
        "#loaderPage QFrame#loaderCard[installed=\"true\"] { border: 2px solid palette(highlight); }"
        "#loaderPage QLabel#loaderName { font-size: 16px; font-weight: 700; }"
        "#loaderPage QLabel#muted { color: palette(placeholder-text); }"
        "#loaderPage QPushButton { border-radius: 8px; padding: 7px 14px; }");

    auto* root = new QVBoxLayout(this);
    root->setContentsMargins(20, 16, 20, 16);
    root->setSpacing(14);

    auto* title = new QLabel(tr("Version modée"), this);
    QFont titleFont = title->font();
    titleFont.setBold(true);
    titleFont.setPointSize(titleFont.pointSize() > 0 ? titleFont.pointSize() + 5 : 18);
    title->setFont(titleFont);
    root->addWidget(title);

    m_summary = new QLabel(this);
    m_summary->setObjectName("muted");
    m_summary->setWordWrap(true);
    root->addWidget(m_summary);

    auto* grid = new QGridLayout();
    grid->setSpacing(14);

    struct Def {
        const char* uid;
        const char* name;
        const char* icon;
    };
    const Def defs[] = {
        { "net.neoforged", "NeoForge", "neoforged" },
        { "net.minecraftforge", "Forge", "forge" },
        { "net.fabricmc.fabric-loader", "Fabric", "fabricmc" },
        { "org.quiltmc.quilt-loader", "Quilt", "quiltmc" },
        { "com.mumfrey.liteloader", "LiteLoader", "liteloader" },
    };

    int index = 0;
    for (const auto& def : defs) {
        LoaderCard card;
        card.uid = QString::fromUtf8(def.uid);
        card.name = QString::fromUtf8(def.name);
        card.iconName = QString::fromUtf8(def.icon);

        auto* frame = new QFrame(this);
        frame->setObjectName("loaderCard");
        frame->setProperty("installed", false);
        frame->setMinimumWidth(220);
        auto* layout = new QVBoxLayout(frame);
        layout->setContentsMargins(18, 16, 18, 16);
        layout->setSpacing(8);

        auto* icon = new QLabel(frame);
        icon->setPixmap(QIcon::fromTheme(card.iconName).pixmap(56, 56));
        icon->setAlignment(Qt::AlignCenter);
        layout->addWidget(icon);

        auto* name = new QLabel(card.name, frame);
        name->setObjectName("loaderName");
        name->setAlignment(Qt::AlignCenter);
        layout->addWidget(name);

        card.status = new QLabel(frame);
        card.status->setObjectName("muted");
        card.status->setAlignment(Qt::AlignCenter);
        card.status->setWordWrap(true);
        layout->addWidget(card.status);

        layout->addStretch(1);

        card.install = new QPushButton(frame);
        layout->addWidget(card.install);
        card.remove = new QPushButton(tr("Retirer"), frame);
        layout->addWidget(card.remove);

        const QString uid = card.uid;
        connect(card.install, &QPushButton::clicked, this, [this, uid] { installLoader(uid); });

        m_cards.append(card);
        grid->addWidget(frame, index / 3, index % 3);
        // stocke le cadre pour le rafraîchissement (propriété "installed")
        card.status->setProperty("frame", QVariant::fromValue<QObject*>(frame));
        ++index;
    }

    // les boutons "Retirer" ont besoin de l'entrée complète : on les connecte après coup
    for (int i = 0; i < m_cards.size(); ++i) {
        const LoaderCard card = m_cards.at(i);
        connect(card.remove, &QPushButton::clicked, this, [this, card] { removeLoader(card); });
    }

    root->addLayout(grid);
    root->addStretch(1);

    auto* profile = m_instance->getPackProfile();
    if (profile) {
        connect(profile, &QAbstractItemModel::dataChanged, this, &LoaderPage::refresh);
        connect(profile, &QAbstractItemModel::rowsInserted, this, &LoaderPage::refresh);
        connect(profile, &QAbstractItemModel::rowsRemoved, this, &LoaderPage::refresh);
        connect(profile, &QAbstractItemModel::modelReset, this, &LoaderPage::refresh);
    }

    refresh();
}

void LoaderPage::refresh()
{
    auto* profile = m_instance->getPackProfile();
    if (!profile) {
        return;
    }

    const QString minecraft = profile->getComponentVersion("net.minecraft");
    QString installedName;

    for (auto& card : m_cards) {
        const QString version = profile->getComponentVersion(card.uid);
        const bool installed = profile->getComponent(card.uid) != nullptr;
        if (installed) {
            installedName = (card.name + " " + version).trimmed();
        }
        card.status->setText(installed ? tr("Installé : %1").arg(version) : tr("Non installé"));
        card.install->setText(installed ? tr("Changer de version") : tr("Installer"));
        card.remove->setVisible(installed);

        if (auto* frame = qobject_cast<QFrame*>(card.status->property("frame").value<QObject*>())) {
            frame->setProperty("installed", installed);
            frame->style()->unpolish(frame);
            frame->style()->polish(frame);
        }
    }

    const QString mc = minecraft.isNull() ? tr("inconnue") : minecraft;
    if (installedName.isEmpty()) {
        m_summary->setText(tr("Minecraft %1 — aucune version modée installée (Vanilla). Choisissez un loader ci-dessous.").arg(mc));
    } else {
        m_summary->setText(tr("Minecraft %1 — loader actuel : %2").arg(mc, installedName));
    }
}

void LoaderPage::installLoader(const QString& uid)
{
    auto* profile = m_instance->getPackProfile();
    if (!profile) {
        return;
    }
    InstallLoaderDialog dialog(profile, uid, this);
    dialog.exec();
    refresh();
    if (m_container) {
        m_container->refreshContainer();
    }
}

void LoaderPage::removeLoader(const LoaderCard& card)
{
    auto* profile = m_instance->getPackProfile();
    if (!profile) {
        return;
    }
    const auto answer = QMessageBox::question(this, tr("Retirer %1").arg(card.name), tr("Voulez-vous vraiment retirer %1 de cette instance ?").arg(card.name),
                                              QMessageBox::Yes | QMessageBox::No, QMessageBox::No);
    if (answer != QMessageBox::Yes) {
        return;
    }
    profile->remove(card.uid);
    refresh();
    if (m_container) {
        m_container->refreshContainer();
    }
}
