# Rapport de validation V1 bêta

**NO-GO provisoire** : validation Android en cours. Ce document sera mis à jour avec les résultats réellement obtenus. Aucune fusion, base de production, donnée réelle, clé release ou publication Play n'est autorisée par cette livraison.

Branche : `feature/cannatrace-v1-finalization`, issue de PR #1 `8ab7301` et des correctifs de `da41d59` repris dans `ff0642e`.

## Périmètre livré pour revue
- Parcours debug local isolé, Room/SQLCipher existant, sans appel réseau ni synchronisation.
- Lots, UUID, métadonnées, unités, archives, recherche et indicateurs calculés sur les données locales.
- QR opaque, génération ZXing, résolution locale et navigation ; caméra CameraX/ML Kit.
- Transactions lot/mouvement/événement, quantités validées, idempotence et contrôle de révision.
- Historique append-only par déclencheurs SQL, chaînage SHA-256, acteur fictif, motif, valeurs et référence.
- Profils fictifs administrateur/opérateur/lecteur, sans revendication d'authentification distante.
- Tests JVM et Room/SQLCipher instrumentés, workflow APK + émulateur, documentation et note réglementaire.

## Résultats acquis
- Audit GitHub PR #1 et CI #19 : effectué ; cause initiale Android = wrapper absent.
- Backend : `python -m pytest -q`, **1 test réussi**, trois avertissements de dépréciation.
- Recherche ciblée de secrets dans 18 commits existants : aucun fichier détecté pour les motifs de tokens GitHub, Stripe live, AWS access key et clés privées. Ce contrôle n'est pas exhaustif et ne remplace pas un scanner spécialisé/rotation des secrets avérés.
- Wrapper Gradle 8.7 restauré depuis distribution officielle ; SHA-256 ZIP `544c35d6bd849ae8a5ed0bcea39ba677dc40f49df7d1835561582da2009b961d`, JAR `cb0da6751c2b753a16ac168bb354870ebb1e162e9083f116729cec9c781156b8`, recoupés avec https://gradle.org/release-checksums/.

## Risques / NO-GO production
Pas d'authentification distante, organisations, révocation serveur, sauvegarde/restauration durable ou synchronisation validée. Données fictives uniquement. Pas d'ancrage externe de l'audit. Migration testée seulement si la suite instrumentée est verte ; caméra physique et Keystore sur appareil à valider. Compatibilité appareils anciens, cycle de vie et dimensions écran à recetter. Exigences ANRAC et modèles de registres non validés : voir REGLEMENTATION.md.
