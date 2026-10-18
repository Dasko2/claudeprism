// SPDX-License-Identifier: GPL-3.0-only
/*
 *  Prism Launcher - Minecraft Launcher
 *  Copyright (C) 2022 Sefa Eyeoglu <contact@scrumplex.net>
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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *      Copyright 2013-2021 MultiMC Contributors
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

#include "InstanceDelegate.h"
#include <QApplication>
#include <QDebug>
#include <QPainter>
#include <QTextLayout>
#include <QTextOption>
#include <QtMath>

#include <QIcon>
#include <QTextEdit>
#include "BaseInstance.h"
#include "InstanceList.h"

// Origin: Qt
static void viewItemTextLayout(QTextLayout& textLayout, int lineWidth, qreal& height, qreal& widthUsed)
{
    height = 0;
    widthUsed = 0;
    textLayout.beginLayout();
    QString str = textLayout.text();
    while (true) {
        QTextLine line = textLayout.createLine();
        if (!line.isValid())
            break;
        if (line.textLength() == 0)
            break;
        line.setLineWidth(lineWidth);
        line.setPosition(QPointF(0, height));
        height += line.height();
        widthUsed = qMax(widthUsed, line.naturalTextWidth());
    }
    textLayout.endLayout();
}

ListViewDelegate::ListViewDelegate(QObject* parent) : QStyledItemDelegate(parent) {}

namespace {
// Géométrie des cartes d'instance (doit rester cohérente avec InstanceView::m_itemWidth)
constexpr int kCardWidth = 160;
constexpr int kCardPadding = 14;
constexpr int kIconSize = 72;
constexpr int kTextGap = 10;
constexpr int kCardInset = 3;

int cardHeight(const QFontMetrics& fm)
{
    return kCardPadding + kIconSize + kTextGap + fm.height() * 2 + kCardPadding;
}

QRect cardRect(const QRect& itemRect)
{
    return itemRect.adjusted(kCardInset, kCardInset, -kCardInset, -kCardInset);
}

QRect textAreaRect(const QRect& itemRect, const QFontMetrics& fm)
{
    const QRect card = cardRect(itemRect);
    return QRect(card.left() + 8, card.top() + kCardPadding + kIconSize + kTextGap, card.width() - 16, fm.height() * 2 + 2);
}

bool isDark(const QColor& color)
{
    return color.lightness() < 128;
}

void drawStatusPill(QPainter* painter, const QRect& card, const QString& text, const QColor& color, const QFont& font)
{
    QFont small = font;
    small.setPointSizeF(qMax(7.0, font.pointSizeF() > 0 ? font.pointSizeF() - 1.5 : 8.0));
    small.setBold(true);
    painter->setFont(small);
    const QFontMetrics fm(small);
    const int dot = 7;
    const int w = fm.horizontalAdvance(text) + dot + 24;
    const int h = fm.height() + 6;
    QRect pill(card.left() + 8, card.top() + 8, w, h);
    QColor bg = color;
    bg.setAlpha(46);
    painter->setPen(Qt::NoPen);
    painter->setBrush(bg);
    painter->drawRoundedRect(pill, h / 2.0, h / 2.0);
    painter->setBrush(color);
    painter->drawEllipse(QPoint(pill.left() + 9 + dot / 2, pill.center().y()), dot / 2, dot / 2);
    painter->setPen(color);
    painter->drawText(pill.adjusted(9 + dot + 4, 0, -6, 0), Qt::AlignVCenter | Qt::AlignLeft, text);
}
}  // namespace

void ListViewDelegate::paint(QPainter* painter, const QStyleOptionViewItem& option, const QModelIndex& index) const
{
    QStyleOptionViewItem opt = option;
    initStyleOption(&opt, index);
    painter->save();
    painter->setClipRect(option.rect);
    painter->setRenderHint(QPainter::Antialiasing, true);
    painter->setRenderHint(QPainter::SmoothPixmapTransform, true);

    const QFontMetrics fm(opt.font);
    const QRect card = cardRect(opt.rect);
    const bool selected = opt.state & QStyle::State_Selected;
    const bool hovered = opt.state & QStyle::State_MouseOver;
    const bool enabled = opt.state & QStyle::State_Enabled;

    // --- couleurs dérivées de la palette (fonctionne avec les thèmes clair et sombre) ---
    const QColor window = opt.palette.color(QPalette::Window);
    const QColor accent = opt.palette.color(QPalette::Highlight);
    const bool dark = isDark(window);
    const QColor background = dark ? window.lighter(hovered || selected ? 140 : 122) : window.darker(hovered || selected ? 108 : 103);

    painter->setPen(Qt::NoPen);
    painter->setBrush(background);
    painter->drawRoundedRect(card, 16, 16);
    if (selected) {
        QColor tint = accent;
        tint.setAlpha(dark ? 60 : 45);
        painter->setBrush(tint);
        painter->drawRoundedRect(card, 16, 16);
    }

    // bordure : couleur d'accent si sélectionné, très discrète sinon
    QColor border = selected ? accent : opt.palette.color(QPalette::Mid);
    if (!selected) {
        border.setAlpha(hovered ? 140 : 70);
    }
    painter->setPen(QPen(border, selected ? 2.0 : 1.0));
    painter->setBrush(Qt::NoBrush);
    painter->drawRoundedRect(QRectF(card).adjusted(0.5, 0.5, -0.5, -0.5), 16, 16);

    // --- icône ---
    QIcon::Mode mode = enabled ? QIcon::Normal : QIcon::Disabled;
    QIcon::State state = opt.state & QStyle::State_Open ? QIcon::On : QIcon::Off;
    const QRect iconBox(card.center().x() - kIconSize / 2, card.top() + kCardPadding + (hovered ? -2 : 0), kIconSize, kIconSize);
    opt.icon.paint(painter, iconBox, Qt::AlignCenter, mode, state);

    // --- nom (2 lignes max, tronqué avec …) ---
    const QRect textRect = textAreaRect(opt.rect, fm);
    painter->setPen(opt.palette.color(enabled ? QPalette::Normal : QPalette::Disabled, QPalette::Text));
    painter->setFont(opt.font);

    QTextOption textOption;
    textOption.setWrapMode(QTextOption::WrapAtWordBoundaryOrAnywhere);
    textOption.setTextDirection(opt.direction);
    textOption.setAlignment(Qt::AlignHCenter);
    QTextLayout textLayout;
    textLayout.setTextOption(textOption);
    textLayout.setFont(opt.font);
    textLayout.setText(index.data().toString());
    qreal layoutHeight = 0, layoutWidth = 0;
    viewItemTextLayout(textLayout, textRect.width(), layoutHeight, layoutWidth);

    const int lineCount = textLayout.lineCount();
    for (int i = 0; i < lineCount && i < 2; ++i) {
        const QTextLine line = textLayout.lineAt(i);
        const QRect lineRect(textRect.left(), textRect.top() + i * fm.height(), textRect.width(), fm.height());
        if (i == 1 && lineCount > 2) {
            // deuxième ligne coupée : on la tronque proprement
            const QString rest = textLayout.text().mid(line.textStart());
            painter->drawText(lineRect, Qt::AlignHCenter | Qt::AlignVCenter, fm.elidedText(rest, Qt::ElideRight, textRect.width()));
        } else {
            const QString part = textLayout.text().mid(line.textStart(), line.textLength()).trimmed();
            painter->drawText(lineRect, Qt::AlignHCenter | Qt::AlignVCenter, part);
        }
    }

    // --- statut ---
    auto instance = (BaseInstance*)index.data(InstanceList::InstancePointerRole).value<void*>();
    if (instance) {
        if (instance->isRunning()) {
            drawStatusPill(painter, card, QObject::tr("En cours"), QColor(0x3d, 0xdc, 0x84), opt.font);
        } else if (instance->hasCrashed() || instance->hasVersionBroken()) {
            drawStatusPill(painter, card, instance->hasCrashed() ? QObject::tr("Crash") : QObject::tr("Erreur"), QColor(0xe5, 0x48, 0x4d),
                           opt.font);
        }
    }

    painter->restore();
}

QSize ListViewDelegate::sizeHint(const QStyleOptionViewItem& option, [[maybe_unused]] const QModelIndex& index) const
{
    return QSize(kCardWidth, cardHeight(QFontMetrics(option.font)));
}

class NoReturnTextEdit : public QTextEdit {
    Q_OBJECT
   public:
    explicit NoReturnTextEdit(QWidget* parent) : QTextEdit(parent)
    {
        setTextInteractionFlags(Qt::TextEditorInteraction);
        setHorizontalScrollBarPolicy(Qt::ScrollBarPolicy::ScrollBarAlwaysOff);
        setVerticalScrollBarPolicy(Qt::ScrollBarPolicy::ScrollBarAlwaysOff);
    }
    bool event(QEvent* event) override
    {
        auto eventType = event->type();
        if (eventType == QEvent::KeyPress || eventType == QEvent::KeyRelease) {
            QKeyEvent* keyEvent = static_cast<QKeyEvent*>(event);
            auto key = keyEvent->key();
            if ((key == Qt::Key_Return || key == Qt::Key_Enter) && eventType == QEvent::KeyPress) {
                emit editingDone();
                return true;
            }
            if (key == Qt::Key_Tab) {
                return true;
            }
        }
        return QTextEdit::event(event);
    }
   signals:
    void editingDone();
};

void ListViewDelegate::updateEditorGeometry(QWidget* editor,
                                            const QStyleOptionViewItem& option,
                                            [[maybe_unused]] const QModelIndex& index) const
{
    const QFontMetrics fm(option.font);
    QRect textRect = textAreaRect(option.rect, fm);
    textRect.adjust(-4, -2, 4, 4);
    editor->setGeometry(textRect);
}

void ListViewDelegate::setEditorData(QWidget* editor, const QModelIndex& index) const
{
    auto text = index.data(Qt::EditRole).toString();
    QTextEdit* realEditor = qobject_cast<NoReturnTextEdit*>(editor);
    realEditor->setAlignment(Qt::AlignHCenter | Qt::AlignTop);
    realEditor->append(text);
    realEditor->selectAll();
    realEditor->document()->clearUndoRedoStacks();
}

void ListViewDelegate::setModelData(QWidget* editor, QAbstractItemModel* model, const QModelIndex& index) const
{
    QTextEdit* realEditor = qobject_cast<NoReturnTextEdit*>(editor);
    QString text = realEditor->toPlainText();
    text.replace(QChar('\n'), QChar(' '));
    text = text.trimmed();
    // Prevent instance names longer than 128 chars
    text.truncate(128);
    if (text.size() != 0) {
        const auto before = model->data(index).toString();
        model->setData(index, text);
        emit textChanged(before, text);
    }
}

QWidget* ListViewDelegate::createEditor(QWidget* parent,
                                        [[maybe_unused]] const QStyleOptionViewItem& option,
                                        [[maybe_unused]] const QModelIndex& index) const
{
    auto editor = new NoReturnTextEdit(parent);
    connect(editor, &NoReturnTextEdit::editingDone, this, &ListViewDelegate::editingDone);
    return editor;
}

void ListViewDelegate::editingDone()
{
    NoReturnTextEdit* editor = qobject_cast<NoReturnTextEdit*>(sender());
    emit commitData(editor);
    emit closeEditor(editor);
}

#include "InstanceDelegate.moc"
