# Livraison CannaTrace V1 bêta Android — 3 novembre 2026

**Verdict global : NO-GO pour une utilisation réelle ou une mise en production.** Une bêta locale persistante est livrée pour recette sur données fictives. La caméra physique, le cycle de vie sur téléphone, le Keystore matériel et les exigences réglementaires restent à valider. Aucun GO global n'est prononcé.

## GitHub
- Branche : `feature/cannatrace-v1-finalization`.
- PR de finalisation, ouverte en brouillon : https://github.com/cortexAI-2025/CannaTrace/pull/2.
- Branche de base inchangée : `claude/cannatrace-android-app-wi4mP`, `68afb6b`.
- PR #1 toujours ouverte en brouillon, non fusionnée. Ses changements sont inclus dans la branche de finalisation.
- `6d4f33c` : cœur bêta, audit, QR, stocks, tests et correctifs issus de la branche de revue.
- `c510fd3` : compatibilité CSV Android 8, permissions et défilement après création.
- `a21988f` : tests Mockito sans attachement d'agent JVM (interfaces).
- `4f26273` : dernier changement de code applicatif, chemin notification interdit en debug.
- `8e82487` : correction de l'attente de navigation dans le test d'interface. Les commits de documentation ultérieurs ne changent pas l'application.

## Fonctions livrées
| Fonction | État et portée |
|---|---|
| Lots | Création UUID, désignation, catégorie, description, origine/fournisseur, date, quantité/unité, statut, emplacement |
| Modifications | Motif et révision attendue ; corrections documentaires historisées ; identifiants et unité immuables |
| Stock | Entrées, sorties, correction, transfert du lot entier ; refus des quantités invalides et stocks négatifs |
| Transactions | Lot, mouvement et événement dans une seule transaction Room ; rollback en cas d'échec d'audit |
| Doublons | UUID d'opération et empreinte de requête ; rejeu identique sans duplication, contenu différent refusé |
| Historique | Événements append-only, dates, acteur fictif, avant/après, motifs et références ; chaînage SHA-256 |
| Archivage | Administrateur fictif seulement ; lot conservé, lecture seule et identifiant non réutilisable |
| QR | UUID opaque, génération et décodage d'image testés, recherche locale ; intégration caméra à recetter sur téléphone |
| Android | Navigation Compose, recherche, indicateurs issus des données, historique et états d'erreur ; thème existant conservé |
| Persistance | Room/SQLCipher conservés, base debug séparée, clé Keystore et sauvegarde système désactivée |
| Permissions | Profils fictifs admin/opérateur/lecteur contrôlés dans le service ; ne constituent pas une authentification |
| Réseau | INTERNET absent de l'APK debug ; intercepteur bloque les requêtes et workers automatiques désactivés |

Les écrans historiques et l'API prototype sont conservés dans les sources. Le parcours debug V1 ne prétend pas valider leurs fonctions de santé, de dispensation, de synchronisation ou de production.

## Résultats effectivement obtenus
| Vérification | Résultat |
|---|---|
| Backend `python -m pytest -q` | 1 test réussi, trois avertissements de dépréciation |
| `testDebugUnitTest` | 42 tests réussis, 0 échec : 18 transitions, 7 audit, 8 hash, 6 règles/QR image/rôles, 3 résolution QR |
| Compilation Kotlin/Java, `assembleDebug` | Réussies localement avec JDK 17 / SDK 34 |
| `assembleDebugAndroidTest` | Réussie, y compris après correction du test de navigation |
| `lintDebug` | Réussi : 0 erreur, 160 avertissements ; résumé `validation/lint-summary.json` |
| Room/SQLCipher sur émulateur API 29 | 4 tests de données réussis : cycle complet/persistance, concurrence/rollback, migration, bonne/mauvaise clé |
| Interface Compose sur émulateur | Réussie sur le commit `8e82487` : création, ouverture de fiche, session lecteur et absence des actions d’écriture |
| OSV graphe Android résolu | 141 composants applicatifs, aucune vulnérabilité connue signalée |
| pip-audit backend | Aucune vulnérabilité connue signalée |
| Recherche de secrets | 18 commits initiaux, aucun candidat sur motifs ciblés ; contrôle non exhaustif |
| Appareil/caméra physique | Non exécuté ; QR image de référence décodé en JVM |
| Synchronisation distante | Non applicable à la bêta, désactivée ; fiabilité non revendiquée |

Résultats machine : `validation/unit-tests.json`, `validation/maven-osv.json`, `validation/backend-pip-audit.json` et `validation/apk-debug.json`. Le scan ne garantit pas l'absence de vulnérabilité, ni celle de tous les outils de build.

Validation CI finale : **trois jobs réussis** (android, backend, device-tests), **5 tests instrumentés réussis** sur émulateur API 29. Exécution : https://github.com/cortexAI-2025/CannaTrace/actions/runs/41602058486, commit `8e82487b56c4b49f43a0b956308f842511000a4a`. Le dernier commit de documentation n'affecte pas le code testé.

Les erreurs initiales de wrapper, icône, smart cast et API CSV ont été corrigées. L'échec initial du test UI était une attente prématurée sur le texte encore présent dans le formulaire ; le test attend maintenant la fin de la navigation, sans retirer son assertion de visibilité.

## APK effectivement construit et inspecté
- Fichier livré : `CannaTrace-V1-beta1-universal-debug.apk`.
- Chemin Gradle : `app/build/outputs/apk/debug/app-debug.apk`.
- Copie livrée : `/workspace/scratch/7cbd5a8a475e/CannaTrace-V1-beta1-universal-debug.apk`.
- Taille : **56 771 464 octets**, soit 56,8 Mo.
- SHA-256 : `18bfdb39435b75d7940212944018fc6f2ff50d40650cd9275d3f1af050ba1aed`.
- Version `1.0.0-beta.1` / code 2 ; package `com.cannatrace.debug`.
- Android minimum 8.0 / API 26 ; cible API 34.
- ABI incluses : arm64-v8a, armeabi-v7a, x86 et x86_64.
- Signature vérifiée par apksigner : APK Signature Scheme v2, certificat `CN=Android Debug`, aucune clé release.
- Permissions fusionnées vérifiées par aapt : CAMERA, WAKE_LOCK, ACCESS_NETWORK_STATE et permission interne de récepteur dynamique non exporté. **Aucune permission INTERNET.**
- Les APK CI ont leur propre signature debug/empreinte ; ne pas confondre leurs SHA-256 avec celui du fichier local livré.

## Risques résiduels et conditions avant tout GO
1. Exécuter la recette sur téléphone réel, notamment Samsung A14 : installation, autorisation/refus caméra, lecture depuis un autre écran, fermeture/réouverture, thème et clavier.
2. Remplacer les profils fictifs par une authentification et des habilitations effectives si le produit devient multi-utilisateur ; organisations, révocation et contrôle serveur absents.
3. Concevoir et tester sauvegardes/restauration, archivage durable et synchronisation idempotente/conflits avant usage multi-appareil. Rien de cela n'est garanti par le mode local.
4. Le chaînage local n'a pas d'ancrage externe : il ne prouve ni l'authenticité du produit ni l'intégrité face à un administrateur malveillant de l'appareil. Les identifiants sont conservés tant que la base est conservée ; désinstallation/effacement détruisent cette continuité locale.
5. Valider la migration avec un échantillon représentatif autorisé avant toute reprise réelle. Le test reconstruit un schéma v1 ; aucune donnée de production n'a été utilisée.
6. Vérifier les obligations applicables auprès des textes officiels et de l'ANRAC, notamment registres, conservation, autorisations et support électronique : `REGLEMENTATION.md`. Aucune conformité ANRAC/ANSM/RGPD n'est démontrée.
7. Traiter les avertissements lint et le cycle de maintenance des dépendances avant publication ; compatibilité Android moderne/contraintes de distribution non validées.

Aucune fusion, modification de production, donnée réelle, clé de production, prestation payante, build release ou publication Google Play n'a été effectuée.
