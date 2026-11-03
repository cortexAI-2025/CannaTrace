# Audit initial — 3 novembre 2026

## Références observées avant modification
- Branche par défaut : `claude/cannatrace-android-app-wi4mP`, `68afb6b` (pas de branche main).
- Branche corrective : `claude/review-and-fix-errors-E20e8`, `da41d59` (8 corrections examinées).
- PR #1 : ouverte, brouillon, fusionnable, aucun commentaire de revue ; head `8ab7301`, 11 commits, 9 fichiers. Elle corrige le plugin Compose/Kotlin, désactive les mocks en release et ajoute une API FastAPI prototype.
- CI #19 : backend réussi, Android échoué avant compilation : `chmod: cannot access ./gradlew`. Wrapper scripts et JAR absents du dépôt.
- Base de finalisation : head PR #1 + reprise des correctifs `da41d59`, sans fusion GitHub.

## État initial (inspection de code, pas recette appareil)
| Fonction | État | Constat |
|---|---|---|
| Kotlin / Compose / Hilt / navigation | Partiel | Socle riche mais build non démontré |
| Room / SQLCipher | Partiel | Entité divergence manquante sur branche par défaut, pas de migration |
| Lots | Partiel | Création/listes ; validation numérique insuffisante, audit non atomique |
| Stock | Partiel | Écritures sans contrôle atomique des soldes, transferts absents |
| QR génération / caméra | Partiel | JSON métier exposé ; format généré non résolu ; navigation détail TODO |
| Audit | Partiel | Chaînage, mais erreurs de journal ignorées et mutations non atomiques |
| Authentification | Simulée en debug | Backend incomplet, pas d'isolation organisation démontrée |
| Synchronisation | Bloquée | Pull mock susceptible d'écraser des données ; pas d'outbox/conflits testés |
| API prototype PR #1 | Partiel | Auth JWT et lots ; stock/audit/organisations/révocation absents |
| APK / tests Android | Bloqué | Wrapper absent, SDK absent dans cet environnement |
| Conformité | Non démontrée | Aucune certification ANSM ou ANRAC établie |

## Architecture cible retenue avant développement
Conserver Compose, Hilt, Room/SQLCipher, CameraX, ML Kit et ZXing. Ajouter une couche transactionnelle locale de traçabilité utilisant la table lots existante, des métadonnées et un journal append-only. Une transaction Room couvre lot, mouvement et événement. Identifiants UUID non réutilisables, opérations idempotentes, contrôle de révision, quantités bornées et unités explicites. Journal chaîné avec anciennes/nouvelles valeurs, acteur de démonstration et motif. Une application debug isolée donne accès au parcours V1 ; les écrans historiques restent dans les sources. Aucun deuxième backend.

La bêta est mono-appareil et fictive : profils de démonstration administrateur/opérateur/lecteur, pas une authentification multi-utilisateur. Aucun accès API ni synchronisation automatique. La synchronisation distante et la sécurité multi-organisation restent NO-GO. Aucun service payant ou déploiement.
