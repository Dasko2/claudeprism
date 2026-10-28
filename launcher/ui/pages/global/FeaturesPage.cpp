// SPDX-License-Identifier: GPL-3.0-only
#include "FeaturesPage.h"

#include <QCheckBox>
#include <QFileInfo>
#include <QLabel>
#include <QVBoxLayout>

#include "Application.h"
#include "ClientFeatures.h"
#include "settings/SettingsObject.h"

FeaturesPage::FeaturesPage(QWidget* parent) : QWidget(parent)
{
    auto* layout = new QVBoxLayout(this);
    layout->setSpacing(10);

    auto* intro = new QLabel(
        tr("Fonctionnalités en jeu intégrées (comme dans Lunar Client). Dans le jeu, appuyez sur Échap puis « Menu de config » "
           "(ou Maj droite) pour activer chaque fonction, changer les touches et placer les éléments où vous voulez."),
        this);
    intro->setWordWrap(true);
    layout->addWidget(intro);

    m_master = new QCheckBox(tr("Activer les fonctionnalités intégrées"), this);
    m_nightVision = new QCheckBox(tr("Night vision (touche modifiable dans le menu en jeu)"), this);
    m_shulker = new QCheckBox(tr("Vision des shulkers : contenu affiché en survolant le shulker dans l'inventaire"), this);
    m_hud = new QCheckBox(tr("Affichages à l'écran : infos, armure, objets en main, point de mort"), this);
    m_backup = new QCheckBox(tr("Sauvegarder automatiquement le dernier monde joué avant chaque lancement (5 copies conservées)"), this);
    for (auto* box : { m_master, m_nightVision, m_shulker, m_hud, m_backup })
        layout->addWidget(box);

    connect(m_master, &QCheckBox::toggled, this, &FeaturesPage::updateEnabledState);

    auto* status = new QLabel(this);
    status->setWordWrap(true);
    status->setText(tr("Important : elles nécessitent une instance Fabric ou Quilt (onglet « Version modée »), de Minecraft 1.19 à 26.3. "
                       "Sur une instance sans mod loader (Vanilla), elles ne sont pas actives."));
    layout->addWidget(status);
    layout->addStretch(1);

    load();
}

void FeaturesPage::load()
{
    auto* s = APPLICATION->settings();
    m_master->setChecked(s->get("FeaturesEnabled").toBool());
    m_nightVision->setChecked(s->get("FeatureNightVision").toBool());
    m_shulker->setChecked(s->get("FeatureShulkerPreview").toBool());
    m_hud->setChecked(s->get("FeatureHud").toBool());
    m_backup->setChecked(s->get("AutoWorldBackup").toBool());
    updateEnabledState();
}

void FeaturesPage::updateEnabledState()
{
    const bool on = m_master->isChecked();
    for (auto* box : { m_nightVision, m_shulker, m_hud })
        box->setEnabled(on);
}

bool FeaturesPage::apply()
{
    auto* s = APPLICATION->settings();
    s->set("FeaturesEnabled", m_master->isChecked());
    s->set("FeatureNightVision", m_nightVision->isChecked());
    s->set("FeatureShulkerPreview", m_shulker->isChecked());
    s->set("FeatureHud", m_hud->isChecked());
    s->set("AutoWorldBackup", m_backup->isChecked());
    return true;
}
