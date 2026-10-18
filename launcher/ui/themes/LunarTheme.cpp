// SPDX-License-Identifier: GPL-3.0-only
/*
 *  Prism Launcher - Minecraft Launcher
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
#include "LunarTheme.h"
#include <QObject>

QString LunarTheme::id()
{
    return "lunar";
}

QString LunarTheme::name()
{
    return QObject::tr("Modern (Lunar)");
}

QString LunarTheme::tooltip()
{
    return QObject::tr("Modern dark interface with a sidebar and rounded cards");
}

bool LunarTheme::hasStyleSheet()
{
    return true;
}

QPalette LunarTheme::colorScheme()
{
    QPalette p;
    p.setColor(QPalette::Window, QColor(0x0b, 0x0d, 0x12));
    p.setColor(QPalette::WindowText, QColor(0xe8, 0xec, 0xf4));
    p.setColor(QPalette::Base, QColor(0x11, 0x14, 0x1b));
    p.setColor(QPalette::AlternateBase, QColor(0x17, 0x1b, 0x25));
    p.setColor(QPalette::ToolTipBase, QColor(0x1f, 0x24, 0x31));
    p.setColor(QPalette::ToolTipText, QColor(0xe8, 0xec, 0xf4));
    p.setColor(QPalette::Text, QColor(0xe8, 0xec, 0xf4));
    p.setColor(QPalette::Button, QColor(0x1f, 0x24, 0x31));
    p.setColor(QPalette::ButtonText, QColor(0xe8, 0xec, 0xf4));
    p.setColor(QPalette::BrightText, QColor(0xff, 0x5c, 0x6c));
    p.setColor(QPalette::Link, QColor(0x6c, 0xa6, 0xff));
    p.setColor(QPalette::Highlight, QColor(0x4c, 0x8d, 0xff));
    p.setColor(QPalette::HighlightedText, Qt::white);
    p.setColor(QPalette::PlaceholderText, QColor(0x6b, 0x74, 0x8a));
    return fadeInactive(p, fadeAmount(), fadeColor());
}

double LunarTheme::fadeAmount()
{
    return 0.5;
}

QColor LunarTheme::fadeColor()
{
    return QColor(0x0b, 0x0d, 0x12);
}

// Palette used below:
//   window #0b0d12  sidebar/panels #11141b  cards #171b25  hover #1f2431  border #262c3b
//   text #e8ecf4  muted #8a93a8  accent #4c8dff -> #7b5cff
QString LunarTheme::appStyleSheet()
{
    static const char* part1 = R"QSS(
QToolTip { color: #e8ecf4; background-color: #1f2431; border: 1px solid #333b50; border-radius: 6px; padding: 4px 8px; }

QMainWindow, QDialog { background-color: #0b0d12; }
QWidget#centralWidget { background-color: #0b0d12; }
QAbstractScrollArea#instanceView { background-color: #0b0d12; border: none; }

/* ---------- Header of the main window ---------- */
QWidget#headerBar { background: transparent; }
QLabel#headerTitle { font-size: 22px; font-weight: 700; color: #ffffff; }
QLabel#headerSubtitle { color: #8a93a8; font-size: 12px; }
QLabel#accountPill { background-color: #171b25; border: 1px solid #262c3b; border-radius: 16px; padding: 6px 14px; color: #e8ecf4; font-weight: 600; }

/* ---------- Left sidebar (main toolbar) ---------- */
QToolBar#mainToolBar { background-color: #11141b; border: none; border-right: 1px solid #1c2130; spacing: 4px; padding: 10px 8px; }
QToolBar#mainToolBar QToolButton { background: transparent; color: #9aa3b8; border: none; border-radius: 10px; padding: 9px 6px; min-width: 66px; font-size: 11px; font-weight: 600; }
QToolBar#mainToolBar QToolButton:hover { background-color: #1b2030; color: #ffffff; }
QToolBar#mainToolBar QToolButton:pressed, QToolBar#mainToolBar QToolButton:checked { background-color: #232a3d; color: #ffffff; }
QToolBar#mainToolBar QToolButton::menu-indicator { image: none; width: 0px; }
QToolBar#mainToolBar QToolButton#addInstanceButton { background: qlineargradient(x1:0, y1:0, x2:1, y2:1, stop:0 #4c8dff, stop:1 #7b5cff); color: #ffffff; }
QToolBar#mainToolBar QToolButton#addInstanceButton:hover { background: qlineargradient(x1:0, y1:0, x2:1, y2:1, stop:0 #66a0ff, stop:1 #8f73ff); }
QToolBar#mainToolBar QLabel#sidebarLogo { background: transparent; padding: 4px 0px 10px 0px; }
QToolBar::separator { background-color: #1c2130; width: 1px; height: 1px; margin: 6px 10px; }

/* ---------- Right panel (selected instance) ---------- */
QToolBar#instanceToolBar { background-color: #11141b; border: none; border-left: 1px solid #1c2130; spacing: 4px; padding: 14px 12px; }
QToolBar#instanceToolBar QToolButton { background-color: transparent; color: #d4daea; border: none; border-radius: 8px; padding: 8px 12px; text-align: left; font-weight: 500; }
QToolBar#instanceToolBar QToolButton:hover { background-color: #1b2030; color: #ffffff; }
QToolBar#instanceToolBar QToolButton:disabled { color: #4d566b; }
QToolBar#instanceToolBar QToolButton::menu-button { border: none; width: 18px; }
QToolBar#instanceToolBar QToolButton#launchButton { background: qlineargradient(x1:0, y1:0, x2:1, y2:0, stop:0 #2fbf71, stop:1 #1fa5a0); color: #ffffff; font-size: 15px; font-weight: 800; padding: 13px 12px; text-align: center; border-radius: 10px; }
QToolBar#instanceToolBar QToolButton#launchButton:hover { background: qlineargradient(x1:0, y1:0, x2:1, y2:0, stop:0 #3ad583, stop:1 #25bdb7); }
QToolBar#instanceToolBar QToolButton#launchButton:disabled { background-color: #1b2030; color: #4d566b; }
QToolBar#instanceToolBar QToolButton#killButton { background-color: rgba(255, 92, 108, 0.12); color: #ff7482; border: 1px solid rgba(255, 92, 108, 0.35); }
QToolBar#instanceToolBar QToolButton#killButton:hover { background-color: rgba(255, 92, 108, 0.25); color: #ffffff; }
QToolBar#instanceToolBar QToolButton#killButton:disabled { background: transparent; color: #4d566b; border: 1px solid #1c2130; }
QToolBar#instanceToolBar QToolButton#deleteButton { color: #ff7482; }
QToolBar#instanceToolBar QToolButton#deleteButton:disabled { color: #4d566b; }
QToolBar#instanceToolBar QToolButton#changeIconButton { background-color: #171b25; border: 1px solid #262c3b; border-radius: 14px; padding: 10px; }
QToolBar#instanceToolBar QToolButton#changeIconButton:hover { border-color: #4c8dff; }
QToolBar#instanceToolBar QLabel#nameLabel { font-size: 15px; font-weight: 700; color: #ffffff; padding: 4px 0px 8px 0px; }

/* ---------- Footer (news) + status bar ---------- */
QToolBar#newsToolBar { background-color: #0e1117; border: none; border-top: 1px solid #1c2130; padding: 2px 10px; }
QToolBar#newsToolBar QToolButton { background: transparent; color: #8a93a8; border: none; border-radius: 6px; padding: 4px 10px; }
QToolBar#newsToolBar QToolButton:hover { color: #ffffff; background-color: #1b2030; }
QStatusBar { background-color: #0e1117; color: #8a93a8; border-top: 1px solid #1c2130; }
QStatusBar::item { border: none; }

/* ---------- Menus ---------- */
QMenuBar { background-color: #11141b; color: #d4daea; border-bottom: 1px solid #1c2130; padding: 2px 6px; }
QMenuBar::item { background: transparent; padding: 6px 12px; border-radius: 6px; }
QMenuBar::item:selected { background-color: #1f2431; }
QMenu { background-color: #151922; color: #e8ecf4; border: 1px solid #262c3b; border-radius: 10px; padding: 6px; }
QMenu::item { padding: 7px 28px 7px 12px; border-radius: 6px; }
QMenu::item:selected { background-color: #4c8dff; color: #ffffff; }
QMenu::item:disabled { color: #4d566b; }
QMenu::separator { height: 1px; background: #262c3b; margin: 6px 8px; }
QMenu::icon { padding-left: 4px; }
)QSS";

    static const char* part2 = R"QSS(
/* ---------- Buttons ---------- */
QPushButton, QToolButton { background-color: #1f2431; color: #e8ecf4; border: 1px solid #2b3246; border-radius: 8px; padding: 6px 14px; }
QPushButton:hover, QToolButton:hover { background-color: #283048; border-color: #3a4560; }
QPushButton:pressed, QToolButton:pressed { background-color: #323b58; }
QPushButton:disabled, QToolButton:disabled { background-color: #151922; color: #4d566b; border-color: #1c2130; }
QPushButton:default { background: qlineargradient(x1:0, y1:0, x2:1, y2:1, stop:0 #4c8dff, stop:1 #7b5cff); border: none; color: #ffffff; font-weight: 600; }
QPushButton:default:hover { background: qlineargradient(x1:0, y1:0, x2:1, y2:1, stop:0 #66a0ff, stop:1 #8f73ff); }
QToolButton[autoRaise="true"], QToolBar QToolButton { border: none; }

/* ---------- Inputs ---------- */
QLineEdit, QPlainTextEdit, QTextEdit, QTextBrowser, QSpinBox, QDoubleSpinBox, QComboBox { background-color: #151922; color: #e8ecf4; border: 1px solid #262c3b; border-radius: 8px; padding: 5px 8px; selection-background-color: #4c8dff; selection-color: #ffffff; }
QLineEdit:focus, QPlainTextEdit:focus, QTextEdit:focus, QSpinBox:focus, QDoubleSpinBox:focus, QComboBox:focus { border: 1px solid #4c8dff; }
QLineEdit:disabled, QSpinBox:disabled, QComboBox:disabled { color: #4d566b; background-color: #11141b; }
QComboBox::drop-down { border: none; width: 22px; }
QComboBox QAbstractItemView { background-color: #151922; border: 1px solid #262c3b; border-radius: 8px; selection-background-color: #4c8dff; outline: none; padding: 4px; }

/* ---------- Views ---------- */
QListView, QTreeView, QTableView, QListWidget, QTreeWidget, QTableWidget { background-color: #11141b; alternate-background-color: #141821; border: 1px solid #1c2130; border-radius: 10px; outline: none; selection-background-color: #4c8dff; selection-color: #ffffff; }
QListView::item, QTreeView::item { padding: 4px; border-radius: 6px; }
QListView::item:hover, QTreeView::item:hover { background-color: #1b2030; }
QListView::item:selected, QTreeView::item:selected { background-color: #4c8dff; color: #ffffff; }
QHeaderView::section { background-color: #151922; color: #8a93a8; border: none; border-bottom: 1px solid #262c3b; padding: 6px 8px; font-weight: 600; }
QTableView { gridline-color: #1c2130; }

/* ---------- Tabs ---------- */
QTabWidget::pane { border: 1px solid #1c2130; border-radius: 10px; top: -1px; background-color: #0e1117; }
QTabBar::tab { background: transparent; color: #8a93a8; padding: 8px 16px; border: none; border-bottom: 2px solid transparent; margin-right: 4px; }
QTabBar::tab:hover { color: #ffffff; }
QTabBar::tab:selected { color: #ffffff; border-bottom: 2px solid #4c8dff; }

/* ---------- Misc ---------- */
QGroupBox { border: 1px solid #1c2130; border-radius: 10px; margin-top: 14px; padding: 12px 8px 8px 8px; background-color: #0e1117; }
QGroupBox::title { subcontrol-origin: margin; subcontrol-position: top left; left: 12px; padding: 0px 6px; color: #8a93a8; font-weight: 600; }
QProgressBar { background-color: #151922; border: none; border-radius: 6px; text-align: center; color: #ffffff; min-height: 14px; }
QProgressBar::chunk { border-radius: 6px; background: qlineargradient(x1:0, y1:0, x2:1, y2:0, stop:0 #4c8dff, stop:1 #7b5cff); }
QScrollBar:vertical { background: transparent; width: 10px; margin: 2px; }
QScrollBar::handle:vertical { background: #2b3246; border-radius: 4px; min-height: 30px; }
QScrollBar::handle:vertical:hover { background: #3a4560; }
QScrollBar:horizontal { background: transparent; height: 10px; margin: 2px; }
QScrollBar::handle:horizontal { background: #2b3246; border-radius: 4px; min-width: 30px; }
QScrollBar::handle:horizontal:hover { background: #3a4560; }
QScrollBar::add-line, QScrollBar::sub-line { width: 0px; height: 0px; }
QScrollBar::add-page, QScrollBar::sub-page { background: transparent; }
QSplitter::handle { background-color: #1c2130; }
QDialogButtonBox QPushButton { min-width: 80px; }
)QSS";

    return QString::fromUtf8(part1) + QString::fromUtf8(part2);
}
