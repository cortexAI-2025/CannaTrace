# Sécurité et dépendances

## Contrôles et choix
- Aucun token d'API, secret serveur ou clé de signature de production ajouté. JWT_SECRET reste externe au prototype Python.
- APK debug isolé (`com.cannatrace.debug`), réseau retiré du manifeste debug et rejeté par l'intercepteur ; aucun worker automatique de synchronisation/anonymisation/alerte en debug.
- SQLCipher + clé aléatoire Keystore conservés ; persistance de clé vérifiée par écriture synchrone des préférences chiffrées. Sauvegarde système désactivée.
- Quantités finies, bornées et à 3 décimales ; unité immuable ; motifs requis ; identifiants stables ; écritures et événement dans une transaction Room.
- Journal et mouvements protégés contre UPDATE/DELETE par déclencheurs SQLite. Suppression de lots interdite : archivage seulement.
- Profil lecteur refuse les mutations dans la couche de données, opérateur ne peut pas archiver. Ce sont des profils fictifs sélectionnables localement, pas une authentification réelle.
- Références d'opérations UUID et empreintes de requête : répétition identique sans duplication, contenu différent rejeté ; contrôle de révision pour changements concurrents.
- Scanner libère caméra, analyseur, client ML Kit et executor ; ne navigue que pour un lot local reconnu.
- Retrait de la création locale de comptes qui utilisait `password.hashCode()` ; authentification serveur non finalisée.

## Dépendances
Frameworks existants conservés : Kotlin 1.9.24, Compose, Hilt, Room 2.6.1, SQLCipher 4.5.4, CameraX 1.3.4, ML Kit 17.3.0, ZXing 3.5.3, Retrofit/OkHttp. Le retrait du plugin Compose incompatible et le compilateur Compose 1.5.14 proviennent de PR #1.

iText retiré : inutilisé, l'exporteur existant utilise PdfDocument Android natif ; la coordonnée déclarée était incorrecte pour 7.2.5 et introduisait inutilement une licence AGPL/commerciale (https://itextpdf.com/products/itext-core). Dépôt JitPack retiré, aucune dépendance ne le nécessite. Aucun nouvel SDK applicatif ajouté. Wrapper Gradle vérifié par empreinte officielle.

## Limites
Recherche par motifs sur 18 commits : aucun candidat pour clés privées, tokens GitHub, Stripe live ou clés AWS, sans garantie exhaustive. Aucun accès aux alertes de sécurité privées du compte n'est revendiqué. Les dépendances Android anciennes et transitives nécessitent un suivi de maintenance, de compatibilité Android récente et de vulnérabilités au-delà du seul build. Une absence de résultat de scanner ne prouve pas l'absence de vulnérabilité.

Le backend prototype reste hors périmètre de déploiement : isolation d'organisations, rôles cohérents avec Android, révocation JWT, rotation des refresh tokens, audit atomique et stock distant non implémentés. Aucune base distante n'a été contactée.
