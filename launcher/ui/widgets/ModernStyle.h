// SPDX-License-Identifier: GPL-3.0-only
/*
 *  Prism Launcher - Minecraft Launcher
 *
 *  Feuilles de style de la nouvelle interface moderne (barre du haut, colonne de navigation,
 *  page des instances, zone de travail). Toutes les couleurs viennent de la palette du thème
 *  actif (palette(...)), donc l'interface suit le thème clair / sombre choisi dans les réglages.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3.
 */

#pragma once

#include <QString>

namespace ModernStyle {

inline QString topBar()
{
    return QStringLiteral(
        "QFrame#topBar { background: palette(base); border-bottom: 1px solid palette(midlight); }"

        "QToolButton#headButton { background: transparent; border: 2px solid transparent; border-radius: 16px; padding: 3px; }"
        "QToolButton#headButton:hover { background: rgba(127,127,127,45); border: 2px solid palette(highlight); }"
        "QLabel#accountName { font-size: 15px; font-weight: 700; }"
        "QLabel#accountHint { color: palette(placeholder-text); font-size: 11px; }"
        "QToolButton#accountArrow { background: transparent; border: none; border-radius: 10px; padding: 6px 8px; font-size: 14px; }"
        "QToolButton#accountArrow:hover { background: rgba(127,127,127,45); }"
        "QToolButton#accountArrow::menu-indicator { image: none; }"

        "QFrame#instanceChip { background: rgba(127,127,127,34); border-radius: 16px; }"
        "QLabel#chipName { font-size: 14px; font-weight: 700; }"
        "QLabel#chipInfo { color: palette(placeholder-text); font-size: 11px; }"

        "QToolButton#launchButton { background: palette(highlight); color: palette(highlighted-text); border: 2px solid transparent; "
        "border-top-left-radius: 16px; border-bottom-left-radius: 16px; border-top-right-radius: 4px; border-bottom-right-radius: 4px; "
        "padding: 10px 30px; font-size: 15px; font-weight: 800; }"
        "QToolButton#launchButton:hover { border: 2px solid rgba(255,255,255,90); }"
        "QToolButton#launchButton:disabled { background: rgba(127,127,127,70); color: palette(placeholder-text); }"
        "QToolButton#launchMenuButton { background: palette(highlight); color: palette(highlighted-text); border: 2px solid transparent; "
        "border-top-right-radius: 16px; border-bottom-right-radius: 16px; border-top-left-radius: 4px; border-bottom-left-radius: 4px; "
        "padding: 10px 10px; font-size: 14px; font-weight: 800; }"
        "QToolButton#launchMenuButton:hover { border: 2px solid rgba(255,255,255,90); }"
        "QToolButton#launchMenuButton:disabled { background: rgba(127,127,127,70); color: palette(placeholder-text); }"
        "QToolButton#launchMenuButton::menu-indicator { image: none; }"

        "QToolButton#killButton { background: #e5484d; color: white; border: 2px solid transparent; border-radius: 16px; "
        "padding: 10px 20px; font-size: 14px; font-weight: 800; }"
        "QToolButton#killButton:hover { border: 2px solid rgba(255,255,255,90); }"

        "QToolButton#moreButton { background: rgba(127,127,127,34); border: none; border-radius: 16px; padding: 8px 14px; "
        "font-size: 18px; font-weight: 800; }"
        "QToolButton#moreButton:hover { background: rgba(127,127,127,70); }"
        "QToolButton#moreButton::menu-indicator { image: none; }");
}

inline QString rail()
{
    return QStringLiteral(
        "QFrame#rail { background: palette(base); border-right: 1px solid palette(midlight); }"
        "QWidget#railInner { background: transparent; }"
        "QScrollArea#railScroll { background: transparent; border: none; }"

        "QPushButton#railButton, QPushButton#railSub { text-align: left; background: transparent; border: none; border-radius: 12px; "
        "padding: 11px 14px; font-size: 13px; font-weight: 600; }"
        "QPushButton#railButton:hover, QPushButton#railSub:hover { background: rgba(127,127,127,45); }"
        "QPushButton#railButton:checked, QPushButton#railSub:checked { background: palette(highlight); color: palette(highlighted-text); }"
        "QPushButton#railButton:disabled, QPushButton#railSub:disabled { color: rgba(127,127,127,150); }"
        "QPushButton#railSub { padding: 8px 14px 8px 40px; font-size: 12px; font-weight: 500; }"

        "QLabel#railCaption { color: palette(placeholder-text); font-size: 10px; font-weight: 700; padding: 14px 14px 4px 14px; }"
        "QLabel#railBadge { background: #e5484d; color: white; border-radius: 9px; padding: 1px 7px; font-size: 11px; font-weight: 800; }"
        "QFrame#railSeparator { background: palette(midlight); border: none; max-height: 1px; min-height: 1px; }");
}

inline QString instancesPage()
{
    return QStringLiteral(
        "QWidget#instancesPage { background: palette(base); }"
        "QLabel#pageTitle { font-size: 26px; font-weight: 800; }"
        "QLabel#pageSubtitle { color: palette(placeholder-text); font-size: 12px; }"
        "QPushButton#createButton { background: palette(highlight); color: palette(highlighted-text); border: 2px solid transparent; "
        "border-radius: 16px; padding: 11px 24px; font-size: 14px; font-weight: 800; }"
        "QPushButton#createButton:hover { border: 2px solid rgba(255,255,255,90); }");
}

inline QString workspaceHeader()
{
    return QStringLiteral(
        "QLabel#workspaceTitle { font-size: 24px; font-weight: 800; }"
        "QLabel#workspaceSubtitle { color: palette(placeholder-text); font-size: 12px; }");
}

inline QString workspaceTabs()
{
    return QStringLiteral(
        "QTabBar#pageTabs { background: transparent; }"
        "QTabBar#pageTabs::tab { background: rgba(127,127,127,34); border-radius: 12px; padding: 8px 16px; margin-right: 6px; "
        "font-weight: 600; }"
        "QTabBar#pageTabs::tab:hover { background: rgba(127,127,127,70); }"
        "QTabBar#pageTabs::tab:selected { background: palette(highlight); color: palette(highlighted-text); }");
}

inline QString workspaceMessage()
{
    return QStringLiteral("QLabel#workspaceMessage { color: palette(placeholder-text); font-size: 15px; padding: 40px; }");
}

}  // namespace ModernStyle
