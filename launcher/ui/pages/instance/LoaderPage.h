// SPDX-License-Identifier: GPL-3.0-only
/*
 *  Prism Launcher - Minecraft Launcher
 *
 *  Page « Version modée » : choisir / changer / retirer le mod loader d'une instance
 *  (NeoForge, Forge, Fabric, Quilt, LiteLoader) à partir de grandes cartes visuelles.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3.
 */

#pragma once

#include <QWidget>

#include "minecraft/MinecraftInstance.h"
#include "ui/pages/BasePage.h"

class QLabel;
class QPushButton;

class LoaderPage : public QWidget, public BasePage {
    Q_OBJECT

   public:
    explicit LoaderPage(MinecraftInstance* instance, QWidget* parent = nullptr);
    ~LoaderPage() override = default;

    QString displayName() const override { return tr("Version modée"); }
    QIcon icon() const override { return QIcon::fromTheme("loadermods"); }
    QString id() const override { return "loader"; }
    QString helpPage() const override { return "Instance-Version"; }
    void openedImpl() override { refresh(); }

   public slots:
    void refresh();

   private:
    struct LoaderCard {
        QString uid;
        QString name;
        QString iconName;
        QLabel* status = nullptr;
        QPushButton* install = nullptr;
        QPushButton* remove = nullptr;
    };

    void installLoader(const QString& uid);
    void removeLoader(const LoaderCard& card);

   private:
    MinecraftInstance* m_instance;
    QLabel* m_summary = nullptr;
    QList<LoaderCard> m_cards;
};
