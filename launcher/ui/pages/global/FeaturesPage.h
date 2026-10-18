// SPDX-License-Identifier: GPL-3.0-only
#pragma once

#include <QWidget>

#include "ui/pages/BasePage.h"

class QCheckBox;

class FeaturesPage : public QWidget, public BasePage {
    Q_OBJECT

   public:
    explicit FeaturesPage(QWidget* parent = nullptr);

    QString displayName() const override { return tr("Fonctionnalités"); }
    QIcon icon() const override { return QIcon::fromTheme("launcher"); }
    QString id() const override { return "features-settings"; }
    bool apply() override;
    void openedImpl() override { load(); }

   private:
    void load();
    void updateEnabledState();

    QCheckBox* m_master = nullptr;
    QCheckBox* m_nightVision = nullptr;
    QCheckBox* m_shulker = nullptr;
    QCheckBox* m_chest = nullptr;
    QCheckBox* m_hud = nullptr;
};
