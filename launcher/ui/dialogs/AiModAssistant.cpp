// SPDX-License-Identifier: GPL-3.0-only

#include "AiModAssistant.h"

#include <QCryptographicHash>
#include <QDesktopServices>
#include <QDir>
#include <QEventLoop>
#include <QFile>
#include <QFileInfo>
#include <QHBoxLayout>
#include <QJsonDocument>
#include <QLabel>
#include <QLineEdit>
#include <QMessageBox>
#include <QNetworkReply>
#include <QNetworkRequest>
#include <QPushButton>
#include <QRegularExpression>
#include <QTextBrowser>
#include <QTimer>
#include <QUrl>
#include <QUrlQuery>
#include <QVBoxLayout>

#include "AiConfig.h"
#include "Application.h"
#include "settings/SettingsObject.h"

namespace {
const char* kUserAgent = "Dasko2/claudeprism-launcher (assistant mods)";

bool validId(const QString& s)
{
    static const QRegularExpression rx(R"(^[A-Za-z0-9_\-]{1,64}$)");
    return rx.match(s).hasMatch();
}
}  // namespace

AiModAssistant::AiModAssistant(const QString& instanceName,
                               const QString& mcVersion,
                               const QString& loader,
                               const QString& modsDir,
                               const QString& stateFile,
                               std::function<void()> onChanged,
                               QWidget* parent)
    : QDialog(parent)
    , m_instance(instanceName)
    , m_mc(mcVersion)
    , m_loader(loader)
    , m_modsDir(modsDir)
    , m_stateFile(stateFile)
    , m_onChanged(std::move(onChanged))
{
    setWindowTitle(tr("Assistant IA des mods"));
    resize(680, 620);
    auto* root = new QVBoxLayout(this);

    auto* head = new QLabel(tr("Instance : <b>%1</b> — Minecraft %2, %3. Décris les mods que tu veux, par exemple « de nouveaux biomes ». "
                               "Les mods viennent de Modrinth (versions compatibles uniquement) et chaque installation te demande confirmation.")
                                .arg(m_instance.toHtmlEscaped(), m_mc, m_loader),
                            this);
    head->setWordWrap(true);
    root->addWidget(head);

    m_chat = new QTextBrowser(this);
    m_chat->setOpenExternalLinks(true);
    root->addWidget(m_chat, 1);

    m_status = new QLabel(this);
    root->addWidget(m_status);

    auto* row = new QHBoxLayout();
    m_input = new QLineEdit(this);
    m_input->setPlaceholderText(tr("Ex : je veux de nouveaux biomes — ou « pas celui-là, enlève-le »"));
    row->addWidget(m_input, 1);
    m_send = new QPushButton(tr("Envoyer"), this);
    row->addWidget(m_send);
    root->addLayout(row);

    connect(m_send, &QPushButton::clicked, this, &AiModAssistant::sendUserMessage);
    connect(m_input, &QLineEdit::returnPressed, this, &AiModAssistant::sendUserMessage);
    append(tr("Assistant"), tr("Dis-moi quel type de mods tu veux, je les cherche et je te les propose."));
}

void AiModAssistant::append(const QString& who, const QString& text)
{
    m_chat->append(QString("<p><b>%1</b><br>%2</p>").arg(who.toHtmlEscaped(), text.toHtmlEscaped().replace("\n", "<br>")));
}

QByteArray AiModAssistant::httpRequest(const QUrl& url, const QByteArray& postBody, const QList<QPair<QByteArray, QByteArray>>& headers, int* status)
{
    QNetworkRequest req(url);
    req.setRawHeader("User-Agent", kUserAgent);
    req.setAttribute(QNetworkRequest::RedirectPolicyAttribute, QNetworkRequest::NoLessSafeRedirectPolicy);
    for (const auto& h : headers) {
        req.setRawHeader(h.first, h.second);
    }
    QNetworkReply* reply = postBody.isNull() ? APPLICATION->network()->get(req) : APPLICATION->network()->post(req, postBody);
    QEventLoop loop;
    QTimer timeout;
    timeout.setSingleShot(true);
    connect(reply, &QNetworkReply::finished, &loop, &QEventLoop::quit);
    connect(&timeout, &QTimer::timeout, &loop, &QEventLoop::quit);
    timeout.start(120000);
    loop.exec();
    if (reply->isRunning()) {
        reply->abort();
    }
    const QByteArray result = reply->readAll();
    if (status) {
        *status = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
    }
    reply->deleteLater();
    return result;
}

QJsonObject AiModAssistant::loadState() const
{
    QFile f(m_stateFile);
    if (!f.open(QIODevice::ReadOnly)) {
        return {};
    }
    return QJsonDocument::fromJson(f.readAll()).object();
}

void AiModAssistant::saveState(const QJsonObject& state) const
{
    QFile f(m_stateFile);
    if (f.open(QIODevice::WriteOnly | QIODevice::Truncate)) {
        f.write(QJsonDocument(state).toJson());
    }
}

QJsonArray AiModAssistant::tools() const
{
    auto tool = [](const QString& name, const QString& desc, const QJsonObject& props, const QJsonArray& required) {
        return QJsonObject{ { "name", name },
                            { "description", desc },
                            { "input_schema", QJsonObject{ { "type", "object" }, { "properties", props }, { "required", required } } } };
    };
    const QJsonObject idProp{ { "project_id",
                                QJsonObject{ { "type", "string" }, { "description", "Identifiant Modrinth (champ id d'un résultat de search_mods)" } } } };
    return QJsonArray{
        tool("search_mods", "Cherche des mods sur Modrinth compatibles avec l'instance (bonne version et bon loader).",
             QJsonObject{ { "query", QJsonObject{ { "type", "string" }, { "description", "Mots-clés en anglais, ex: biomes, minimap, shaders" } } } },
             QJsonArray{ "query" }),
        tool("install_mod", "Propose l'installation d'un mod (et de ses dépendances obligatoires) ; l'utilisateur confirme.", idProp,
             QJsonArray{ "project_id" }),
        tool("remove_mod", "Retire un mod que cet assistant a installé (et les dépendances devenues inutiles).", idProp, QJsonArray{ "project_id" }),
        tool("list_installed", "Liste les mods installés par cet assistant.", QJsonObject{}, QJsonArray{}),
    };
}

QString AiModAssistant::toolSearch(const QString& query)
{
    QUrl url("https://api.modrinth.com/v2/search");
    QUrlQuery q;
    q.addQueryItem("query", query.left(100));
    q.addQueryItem("limit", "8");
    q.addQueryItem("index", "relevance");
    QJsonArray facets{ QJsonArray{ "project_type:mod" }, QJsonArray{ QString("versions:%1").arg(m_mc) } };
    QJsonArray loaders{ QString("categories:%1").arg(m_loader) };
    if (m_loader == "quilt") {
        loaders.append("categories:fabric");
    }
    facets.append(loaders);
    q.addQueryItem("facets", QString::fromUtf8(QJsonDocument(facets).toJson(QJsonDocument::Compact)));
    url.setQuery(q);
    int status = 0;
    const auto obj = QJsonDocument::fromJson(httpRequest(url, {}, {}, &status)).object();
    if (status != 200) {
        return QString("Erreur Modrinth (%1)").arg(status);
    }
    QStringList lines;
    for (const auto& v : obj["hits"].toArray()) {
        const auto h = v.toObject();
        lines << QString("id=%1 | %2 | %3 téléchargements | %4")
                     .arg(h["project_id"].toString(), h["title"].toString())
                     .arg(h["downloads"].toInt())
                     .arg(h["description"].toString().left(160));
    }
    return lines.isEmpty() ? QStringLiteral("Aucun résultat compatible.") : lines.join('\n');
}

bool AiModAssistant::installProject(const QString& projectId, const QString& requiredBy, QStringList& log, int depth)
{
    if (!validId(projectId) || depth > 3) {
        return false;
    }
    QJsonObject state = loadState();
    if (state.contains(projectId)) {
        auto entry = state[projectId].toObject();
        if (!requiredBy.isEmpty()) {
            auto by = entry["required_by"].toArray();
            by.append(requiredBy);
            entry["required_by"] = by;
            state[projectId] = entry;
            saveState(state);
        }
        log << QString("%1 : déjà installé").arg(entry["title"].toString());
        return true;
    }

    int status = 0;
    const auto project = QJsonDocument::fromJson(httpRequest(QUrl("https://api.modrinth.com/v2/project/" + projectId), {}, {}, &status)).object();
    if (status != 200) {
        log << QString("Projet %1 introuvable").arg(projectId);
        return false;
    }
    const QString title = project["title"].toString();

    // version compatible
    QUrl url(QString("https://api.modrinth.com/v2/project/%1/version").arg(projectId));
    QUrlQuery q;
    QJsonArray loaders{ m_loader };
    if (m_loader == "quilt") {
        loaders.append("fabric");
    }
    q.addQueryItem("loaders", QString::fromUtf8(QJsonDocument(loaders).toJson(QJsonDocument::Compact)));
    q.addQueryItem("game_versions", QString::fromUtf8(QJsonDocument(QJsonArray{ m_mc }).toJson(QJsonDocument::Compact)));
    url.setQuery(q);
    const auto versions = QJsonDocument::fromJson(httpRequest(url, {}, {}, &status)).array();
    if (status != 200 || versions.isEmpty()) {
        log << QString("%1 : aucune version compatible avec Minecraft %2 (%3)").arg(title, m_mc, m_loader);
        return false;
    }
    QJsonObject chosen = versions.first().toObject();
    for (const auto& v : versions) {
        if (v.toObject()["version_type"].toString() == "release") {
            chosen = v.toObject();
            break;
        }
    }

    QJsonObject file;
    for (const auto& f : chosen["files"].toArray()) {
        if (f.toObject()["primary"].toBool()) {
            file = f.toObject();
        }
    }
    if (file.isEmpty() && !chosen["files"].toArray().isEmpty()) {
        file = chosen["files"].toArray().first().toObject();
    }
    const QUrl dl(file["url"].toString());
    const QString name = QFileInfo(file["filename"].toString()).fileName();
    if (dl.host() != "cdn.modrinth.com" || name.isEmpty() || !name.endsWith(".jar")) {
        log << QString("%1 : fichier non valide, ignoré").arg(title);
        return false;
    }

    // confirmation de l'utilisateur avant tout téléchargement
    const QString reason = requiredBy.isEmpty() ? QString() : tr("\n\n(Dépendance obligatoire d'un autre mod.)");
    const auto answer = QMessageBox::question(
        this, tr("Installer ce mod ?"),
        tr("Installer « %1 » depuis Modrinth ?\n\n%2\n\nFichier : %3 (version %4)%5")
            .arg(title, project["description"].toString(), name, chosen["version_number"].toString(), reason),
        QMessageBox::Yes | QMessageBox::No, QMessageBox::Yes);
    if (answer != QMessageBox::Yes) {
        log << QString("%1 : refusé par l'utilisateur, non installé").arg(title);
        return false;
    }

    // dépendances obligatoires d'abord
    for (const auto& d : chosen["dependencies"].toArray()) {
        const auto dep = d.toObject();
        if (dep["dependency_type"].toString() == "required" && !dep["project_id"].toString().isEmpty()) {
            installProject(dep["project_id"].toString(), projectId, log, depth + 1);
        }
    }

    const QByteArray file_bytes = httpRequest(dl, {}, {}, &status);
    const QString expected = file["hashes"].toObject()["sha512"].toString();
    if (status != 200 || file_bytes.isEmpty() || expected.isEmpty() ||
        QCryptographicHash::hash(file_bytes, QCryptographicHash::Sha512).toHex().toLower() != expected.toLower().toUtf8()) {
        log << QString("%1 : téléchargement invalide (empreinte différente), annulé").arg(title);
        return false;
    }
    QDir().mkpath(m_modsDir);
    QFile out(QDir(m_modsDir).filePath(name));
    if (!out.open(QIODevice::WriteOnly | QIODevice::Truncate)) {
        log << QString("%1 : impossible d'écrire le fichier").arg(title);
        return false;
    }
    out.write(file_bytes);
    out.close();

    state = loadState();
    QJsonArray by;
    if (!requiredBy.isEmpty()) {
        by.append(requiredBy);
    }
    state[projectId] = QJsonObject{ { "title", title },
                                    { "file", name },
                                    { "description", project["description"].toString() },
                                    { "direct", requiredBy.isEmpty() },
                                    { "required_by", by } };
    saveState(state);
    log << QString("%1 installé (%2)").arg(title, name) + (requiredBy.isEmpty() ? QString() : QStringLiteral(" [dépendance]"));
    return true;
}

QString AiModAssistant::toolInstall(const QString& projectId)
{
    QStringList log;
    const bool ok = installProject(projectId, QString(), log, 0);
    if (m_onChanged) {
        m_onChanged();
    }
    const QString desc = loadState()[projectId].toObject()["description"].toString();
    return (ok ? "OK\n" : "ÉCHEC\n") + log.join('\n') + (desc.isEmpty() ? QString() : "\nDescription : " + desc);
}

QString AiModAssistant::toolRemove(const QString& projectId)
{
    if (!validId(projectId)) {
        return "identifiant invalide";
    }
    QJsonObject state = loadState();
    if (!state.contains(projectId)) {
        return "Ce mod n'a pas été installé par l'assistant : je ne peux pas le retirer (l'utilisateur peut le faire dans la liste des mods).";
    }
    QStringList removed;
    std::function<void(const QString&)> removeOne = [&](const QString& id) {
        const auto e = state[id].toObject();
        QFile::remove(QDir(m_modsDir).filePath(QFileInfo(e["file"].toString()).fileName()));
        removed << e["title"].toString();
        state.remove(id);
        // dépendances devenues inutiles
        for (const QString& other : state.keys()) {
            auto o = state[other].toObject();
            auto by = o["required_by"].toArray();
            QJsonArray kept;
            for (const auto& b : by) {
                if (b.toString() != id) {
                    kept.append(b);
                }
            }
            if (kept.size() != by.size()) {
                o["required_by"] = kept;
                state[other] = o;
                if (kept.isEmpty() && !o["direct"].toBool()) {
                    removeOne(other);
                }
            }
        }
    };
    removeOne(projectId);
    saveState(state);
    if (m_onChanged) {
        m_onChanged();
    }
    return "Retiré : " + removed.join(", ");
}

QString AiModAssistant::toolList()
{
    QStringList lines;
    const auto state = loadState();
    for (const QString& id : state.keys()) {
        const auto e = state[id].toObject();
        lines << QString("id=%1 | %2%3").arg(id, e["title"].toString(), e["direct"].toBool() ? "" : " (dépendance)");
    }
    return lines.isEmpty() ? QStringLiteral("Aucun mod installé par l'assistant.") : lines.join('\n');
}

QString AiModAssistant::runTool(const QString& name, const QJsonObject& input)
{
    if (name == "search_mods") {
        return toolSearch(input["query"].toString());
    }
    if (name == "install_mod") {
        return toolInstall(input["project_id"].toString());
    }
    if (name == "remove_mod") {
        return toolRemove(input["project_id"].toString());
    }
    if (name == "list_installed") {
        return toolList();
    }
    return "outil inconnu";
}

QJsonObject AiModAssistant::callClaude()
{
    const QString proxy = aiProxyUrl();
    const QString context = QString("instance « %1 », Minecraft %2, loader %3.").arg(m_instance, m_mc, m_loader);
    const QJsonObject body{ { "mode", "mods" }, { "context", context }, { "messages", m_messages } };
    const QList<QPair<QByteArray, QByteArray>> headers{ { "content-type", "application/json" } };
    const QUrl endpoint(proxy);
    int status = 0;
    const QByteArray raw = httpRequest(endpoint, QJsonDocument(body).toJson(QJsonDocument::Compact), headers, &status);
    QJsonObject obj = QJsonDocument::fromJson(raw).object();
    if (status != 200) {
        obj["__error"] = status == 429 ? QStringLiteral("Trop de demandes, réessaie dans un moment.")
                                       : QString("Erreur (%1) : %2").arg(status).arg(obj["error"].toObject()["message"].toString());
    }
    return obj;
}

void AiModAssistant::runConversation()
{
    for (int step = 0; step < 8; ++step) {
        m_status->setText(tr("L'assistant travaille…"));
        const QJsonObject resp = callClaude();
        if (resp.contains("__error")) {
            m_status->setText(resp["__error"].toString());
            return;
        }
        const QJsonArray content = resp["content"].toArray();
        m_messages.append(QJsonObject{ { "role", "assistant" }, { "content", content } });

        QJsonArray results;
        for (const auto& c : content) {
            const auto block = c.toObject();
            if (block["type"].toString() == "text" && !block["text"].toString().trimmed().isEmpty()) {
                append(tr("Assistant"), block["text"].toString());
            } else if (block["type"].toString() == "tool_use") {
                const QString name = block["name"].toString();
                m_status->setText(tr("Outil : %1…").arg(name));
                const QString result = runTool(name, block["input"].toObject());
                if (name == "install_mod" || name == "remove_mod") {
                    append(tr("Launcher"), result.section('\n', 0, 3));
                }
                results.append(QJsonObject{ { "type", "tool_result" }, { "tool_use_id", block["id"].toString() }, { "content", result } });
            }
        }
        if (results.isEmpty()) {
            break;
        }
        m_messages.append(QJsonObject{ { "role", "user" }, { "content", results } });
    }
    m_status->clear();
}

void AiModAssistant::sendUserMessage()
{
    const QString text = m_input->text().trimmed();
    if (m_busy || text.isEmpty()) {
        return;
    }
    if (aiProxyUrl().isEmpty()) {
        m_status->setText(tr("L'IA n'est pas encore disponible dans cette version du launcher."));
        return;
    }
    m_busy = true;
    m_send->setEnabled(false);
    m_input->clear();
    append(tr("Toi"), text);
    m_messages.append(QJsonObject{ { "role", "user" }, { "content", text } });
    runConversation();
    m_busy = false;
    m_send->setEnabled(true);
}
