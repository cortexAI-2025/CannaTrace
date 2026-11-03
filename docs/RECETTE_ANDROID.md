# Installation et recette Android — données fictives

## Build et installation
Prérequis : JDK 17, Android SDK 34 / build-tools 34.0.0, accès Google Maven et Maven Central.

```sh
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Android minimum : 8.0 (API 26). Application debug : `com.cannatrace.debug`, version `1.0.0-beta.1` (code 2). APK universel sans filtre ABI, signature debug uniquement. Métadonnées et SHA-256 du fichier livré : [validation/apk-debug.json](validation/apk-debug.json). Aucun Play Store ni clé release. Les bibliothèques natives SQLCipher déterminent les ABI effectives : les vérifier dans le rapport APK après build.

Sur téléphone : transférer l'APK, autoriser temporairement l'installation depuis la source utilisée, installer puis ouvrir « CannaTrace bêta ». Accorder la caméra lors de l'ouverture du scanner. Le fichier livré fait 56 771 464 octets (56,8 Mo) et contient arm64-v8a, armeabi-v7a, x86 et x86_64. Compatible sur le papier avec votre Samsung A14 ; installation/caméra à valider sur ce téléphone. La variante debug supprime INTERNET et les permissions stockage/biométrie/notifications du manifeste principal. Vérifier les permissions fusionnées du fichier réellement produit.

La base `cannatrace-beta.db` est chiffrée par SQLCipher ; clé aléatoire protégée via Keystore. Les sauvegardes automatiques sont désactivées. Désinstaller ou effacer les données supprime la démonstration. Aucun export/restauration durable ou synchronisation n'est fourni. Ne jamais y saisir de données réelles.

## Recette fonctionnelle (à cocher uniquement après exécution)
| Étape | Action | Résultat attendu |
|---|---|---|
| 1 | Installer l'APK debug | Installation Android ≥ 8 |
| 2 | Ouvrir et choisir le profil opérateur | Bandeau bêta locale/fictif |
| 3 | Créer « DEMO Alpha », catégorie « Fleurs fictives », origine et fournisseur fictifs, 100 g, Zone A | UUID opaque, date, fiche et événement CREATE |
| 4 | Afficher le QR sur la fiche | Contenu `cannatrace:v1:lot:<uuid>`, sans secret ni donnée personnelle |
| 5 | Présenter ce QR depuis un second écran au scanner | Fiche du lot ouverte hors ligne |
| 6 | Entrée 20 g, motif « Réception fictive » | Solde 120 g, événement ENTREE |
| 7 | Transfert Zone B, motif « Déplacement fictif » | Solde inchangé, emplacement Zone B, événement TRANSFERT |
| 8 | Sortie 15 g, motif « Sortie fictive » | Solde 105 g, événement SORTIE |
| 9 | Ouvrir le journal et développer avant/après | Quatre événements, dates, acteur demo, motifs et références uniques |
| 10 | Fermer complètement puis rouvrir | Session à sélectionner si processus terminé ; lot, solde et journal conservés |
| 11 | Scanner QR inconnu valide puis QR malformé | Message, aucune fiche étrangère ouverte, bouton réessayer |
| 12 | Profil lecteur | Consultation/scanner possibles ; aucune écriture |
| 13 | Profil opérateur | Écriture possible, archivage interdit |
| 14 | Profil administrateur, archiver avec motif | Lot archivé visible via filtre, lecture seule, journal conservé |
| 15 | Sortie supérieure au solde, quantité négative/NaN/4 décimales, motif vide | Refus, aucun mouvement ni événement partiel |
| 16 | Contrôle qualité, correction et modification documentaire | Événements de correction ajoutés, anciennes valeurs conservées |
| 17 | Vérifier le journal | Chaîne locale cohérente ; aucune authenticité externe revendiquée |
| 18 | Refuser la caméra puis l'autoriser / réouvrir plusieurs fois | Message permission, récupération et caméra libérée à la sortie |

Un transfert déplace le lot entier entre emplacements ; le fractionnement multi-emplacements/lot parent-enfant est hors périmètre de cette bêta. L'unité est immuable après création ; créer un nouveau lot pour une conversion physique, sans conversion implicite g/kg.

## Tests automatiques
- `testDebugUnitTest` : règles numériques/rôles, identifiants, génération puis décodage d'une image QR ZXing, tests historiques.
- `connectedDebugAndroidTest` : transactions Room, scénario stock complet, idempotence, concurrence, erreur d'audit injectée, permissions fictives, archivage, garde SQL append-only, détection de falsification, réouverture et migration simulant le schéma v1.
- Les tests Room instrumentés utilisent une base de test non chiffrée pour isoler les invariants. Un test SQLCipher vérifie aussi la réouverture avec la bonne clé et le refus d’une mauvaise clé. Le stockage Keystore et la caméra nécessitent une validation spécifique sur appareil. La migration v1 est reconstruite par retrait des tables ajoutées ; ce n'est pas un échantillon de production.
- Workflow GitHub : APK debug et rapports, plus émulateur API 29. Un résultat vert ne vaut pas recette de caméra physique.

## Limites de sécurité
Profils fictifs librement sélectionnables. Pas d'authentification serveur, d'organisations, de révocation distante ou de protection contre un propriétaire d'appareil compromis. Journal append-only dans SQLite avec chaînage SHA-256 ; sans ancrage externe, suppression de suffixe ou reconstruction complète par attaquant privilégié non détectable. Les identifiants restent conservés tant que la base n'est pas détruite. Aucun QR n'est une preuve d'authenticité.

Les builds locaux et ceux de GitHub peuvent utiliser des certificats debug différents : leurs SHA-256 et leur signature ne sont pas interchangeables. Conserver le même canal de distribution pour les mises à jour. Ne pas désinstaller pour contourner une incompatibilité de signature si vous souhaitez conserver les données fictives.

## Validation de livraison
Les 42 tests JVM, le lint, les builds APK/test APK, le backend et les 5 tests instrumentés sont validés. Exécution CI : https://github.com/cortexAI-2025/CannaTrace/actions/runs/41602058486. Le tableau de recette physique ci-dessus reste à exécuter sur téléphone, sans présumer de son résultat.
