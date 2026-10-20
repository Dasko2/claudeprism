# Serveur intermédiaire de l'IA (sans clé pour l'utilisateur)

Le launcher n'embarque **aucune clé** : il appelle ce petit serveur, qui garde ta clé Anthropic secrète et n'accepte que
deux usages (aide aux erreurs et assistant mods). Hébergement gratuit avec Cloudflare Workers.

## Mise en place (une seule fois, environ 10 minutes)

1. Crée une clé API sur https://console.anthropic.com/settings/keys (l'usage est facturé sur ton compte Anthropic ;
   mets une limite de dépense dans la console).
2. Crée un compte gratuit sur https://dash.cloudflare.com, puis **Workers & Pages → Create → Worker**, nom : `dasko-ai`.
3. Clique **Edit code**, colle tout le contenu de `worker.js`, puis **Deploy**.
4. Dans le Worker : **Settings → Variables and Secrets → Add** : type *Secret*, nom `ANTHROPIC_API_KEY`, valeur = ta clé.
5. (Recommandé, limite par adresse IP) **Storage & Databases → KV → Create** un namespace `RATE`, puis dans le Worker
   **Settings → Bindings → Add → KV namespace**, nom de variable `RATE`.
6. Copie l'adresse du Worker (du type `https://dasko-ai.TONCOMPTE.workers.dev`).
7. Dans GitHub : dépôt **Settings → Secrets and variables → Actions → Variables → New repository variable**,
   nom `AI_PROXY_URL`, valeur = l'adresse du Worker. Relance le build : le Setup.exe utilisera ce serveur.

Tant que `AI_PROXY_URL` n'est pas définie, le launcher demande à l'utilisateur sa propre clé API.

## Ce que le serveur impose
- consignes et outils fixés côté serveur (impossible de s'en servir comme chatbot général) ;
- modèles et longueur de réponse limités, taille des requêtes limitée ;
- 40 requêtes par heure et par adresse IP si le KV `RATE` est lié.
