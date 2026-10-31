#include "Lang.h"

#include <QFile>
#include <QHash>
#include <QJsonDocument>
#include <QJsonObject>

#include "Application.h"
#include "settings/SettingsObject.h"

namespace {
QHash<QString, QString> load(const QString& code)
{
    QHash<QString, QString> out;
    QFile f(":/prismlang/" + code + ".json");
    if (!f.open(QIODevice::ReadOnly))
        return out;
    const QJsonObject o = QJsonDocument::fromJson(f.readAll()).object();
    for (auto it = o.begin(); it != o.end(); ++it)
        out.insert(it.key(), it.value().toString());
    return out;
}

const QHash<QString, QString>& base()
{
    static const QHash<QString, QString> m = load("fr_fr");
    return m;
}

const QHash<QString, QString>& current()
{
    static const QHash<QString, QString> m = [] {
        QString code = APPLICATION->settings()->get("Language").toString().toLower();
        QHash<QString, QString> r = load(code);
        if (r.isEmpty())
            r = load(code.section('_', 0, 0));
        return r;
    }();
    return m;
}
}

QString Lang::t(const char* key)
{
    const QString k = QString::fromUtf8(key);
    if (auto it = current().constFind(k); it != current().constEnd())
        return it.value();
    if (auto it = base().constFind(k); it != base().constEnd())
        return it.value();
    return k;
}
