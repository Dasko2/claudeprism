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
        tr("Fonctionnalités en jeu intégrées (comme dans Lunar Client). Elles sont intégrées au launcher et chargées "
           "automatiquement au lancement. Décochez pour les désactiver."),
        this);
    intro->setWordWrap(true);
    layout->addWidget(intro);

    m_master = new QCheckBox(tr("Activer les fonctionnalités intégrées"), this);
    m_nightVision = new QCheckBox(tr("Night vision : touche N en jeu pour voir clair la nuit et dans les grottes"), this);
    m_shulker = new QCheckBox(tr("Contenu des shulker boxes affiché dans l'infobulle de l'objet"), this);
    m_chest = new QCheckBox(tr("Contenu des coffres / barils déjà ouverts affiché en visant le bloc, sans l'ouvrir"), this);
    m_hud = new QCheckBox(tr("Affichage FPS et coordonnées"), this);
    m_minimap = new QCheckBox(tr("Minimap : petite carte du dessus en haut à droite"), this);
    for (auto* box : { m_master, m_nightVision, m_shulker, m_chest, m_hud, m_minimap })
        layout->addWidget(box);

    connect(m_master, &QCheckBox::toggled, this, &FeaturesPage::updateEnabledState);

    auto* status = new QLabel(this);
    status->setWordWrap(true);
    status->setText(tr("Important : elles nécessitent une instance Fabric ou Quilt (onglet « Version modée »), en Minecraft 1.21.11 pour l'instant. "
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
    m_chest->setChecked(s->get("FeatureChestPreview").toBool());
    m_hud->setChecked(s->get("FeatureHud").toBool());
    m_minimap->setChecked(s->get("FeatureMinimap").toBool());
    updateEnabledState();
}

void FeaturesPage::updateEnabledState()
{
    const bool on = m_master->isChecked();
    for (auto* box : { m_nightVision, m_shulker, m_chest, m_hud, m_minimap })
        box->setEnabled(on);
}

bool FeaturesPage::apply()
{
    auto* s = APPLICATION->settings();
    s->set("FeaturesEnabled", m_master->isChecked());
    s->set("FeatureNightVision", m_nightVision->isChecked());
    s->set("FeatureShulkerPreview", m_shulker->isChecked());
    s->set("FeatureChestPreview", m_chest->isChecked());
    s->set("FeatureHud", m_hud->isChecked());
    s->set("FeatureMinimap", m_minimap->isChecked());
    return true;
}
