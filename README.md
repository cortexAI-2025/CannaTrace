# CannaTrace — Traçabilité Cannabis Médical (Android)

Application Android de traçabilité **seed-to-sale** (de la graine à la vente) pour le cannabis thérapeutique, conforme aux futures exigences de l'**ANSM** (Agence nationale de sécurité du médicament) pour les expérimentations de cannabis médical en France.

---

## Table des matières

1. [Fonctionnalités](#fonctionnalités)
2. [Architecture](#architecture)
3. [Compilation](#compilation)
4. [Configuration API Backend](#configuration-api-backend)
5. [Comptes de test](#comptes-de-test)
6. [Modules métier](#modules-métier)
7. [Sécurité & RGPD](#sécurité--rgpd)
8. [Tests](#tests)
9. [Structure du projet](#structure-du-projet)

---

## Fonctionnalités

- ✅ Gestion des utilisateurs avec rôles (Producteur, Transformateur, Dispensaire, Prescripteur, Patient, Inspecteur ANSM)
- ✅ Authentification sécurisée (email + mot de passe, 2FA, biométrie locale)
- ✅ Enregistrement de chaque graine/clone (QR code / RFID)
- ✅ Suivi complet du pipeline : Graine → Germination → Croissance → Récolte → Séchage → Transformation → Conditionnement → Contrôle Qualité → Stockage → Dispensation
- ✅ Traçabilité des lots : poids, THC/CBD, opérateur, localisation
- ✅ Génération de rapports PDF et CSV (format ANSM)
- ✅ Synchronisation hors-ligne (WorkManager)
- ✅ Scanner QR intégré (CameraX + ML Kit)
- ✅ Alertes automatiques (péremption, rupture de stock)
- ✅ Interface bilingue FR/EN
- ✅ Base locale Room + SQLCipher
- ✅ **Blockchain légère** : hash SHA-256 chaîné pour l'immutabilité des logs d'audit
- ✅ RGPD : anonymisation automatique des données patient après 2 ans

---

## Architecture

```
app/src/main/java/com/cannatrace/
├── data/
│   ├── local/
│   │   ├── database/
│   │   │   ├── CannaTraceDatabase.kt    # Room + SQLCipher
│   │   │   ├── dao/                     # Data Access Objects
│   │   │   └── entities/               # Entités Room
│   │   └── mapper/                     # Domain ↔ Entity mappers
│   ├── remote/
│   │   ├── CannaTraceApi.kt            # Interface Retrofit
│   │   ├── ApiResponse.kt              # Wrapper réponses
│   │   └── MockInterceptor.kt          # Mock local pour démo
│   └── repository/                     # Implémentations des repos
├── domain/
│   ├── model/                          # Modèles métier
│   ├── repository/                     # Interfaces des repos
│   └── usecase/                        # Cas d'utilisation
├── presentation/
│   ├── auth/                           # Écran de connexion
│   ├── dashboard/                      # Tableau de bord
│   ├── batch/                          # Gestion des lots
│   ├── culture/                        # Journal de culture
│   ├── scanner/                        # Scanner QR
│   ├── stock/                          # Gestion des stocks
│   ├── reports/                        # Rapports & exports
│   └── navigation/                     # NavGraph + BottomNav
├── di/                                 # Modules Hilt
├── utils/                              # HashUtils, PdfExporter, etc.
└── workers/                            # WorkManager (sync, RGPD, alertes)
```

**Pattern** : MVVM + Clean Architecture + Repository Pattern  
**DI** : Hilt  
**UI** : Jetpack Compose + Material3  
**DB** : Room + SQLCipher (chiffré)  
**Réseau** : Retrofit + OkHttp  
**Async** : Coroutines + Flow  

---

## Compilation

### Prérequis

- Android Studio Hedgehog (2023.1.1) ou supérieur
- JDK 17
- Android SDK 34
- Gradle 8.x

### Étapes

```bash
# 1. Cloner le dépôt
git clone https://github.com/cortexai-2025/cannatrace.git
cd cannatrace

# 2. Ouvrir dans Android Studio ou compiler en CLI
./gradlew assembleDebug

# 3. Installer sur un appareil/émulateur
./gradlew installDebug
```

### Build Variants

| Variant | Description |
|---------|-------------|
| `debug` | Mode développement, MockInterceptor activé, logs HTTP activés |
| `release` | Production, minification activée, backend réel requis |

---

## Configuration API Backend

### Mode Mock (par défaut)

Le mode mock est activé par défaut dans `NetworkModule.kt` :

```kotlin
// app/src/main/java/com/cannatrace/di/NetworkModule.kt
private const val USE_MOCK = true  // ← Passer à false pour le vrai backend
private const val BASE_URL = "https://api.cannatrace.fr/v1/"
```

Le `MockInterceptor` simule toutes les réponses serveur localement, sans connexion réseau.

### Mode Production

1. Passer `USE_MOCK = false` dans `NetworkModule.kt`
2. Configurer l'URL du backend :
   ```kotlin
   private const val BASE_URL = "https://votre-serveur.cannatrace.fr/api/v1/"
   ```
3. Le backend doit implémenter les endpoints définis dans `CannaTraceApi.kt`

### Endpoints principaux

```
POST   /auth/login
POST   /auth/refresh
GET    /batches
POST   /batches
PUT    /batches/{id}
POST   /batches/{id}/transition
GET    /plants
POST   /plants
GET    /stock-entries
POST   /stock-entries
GET    /prescriptions
POST   /prescriptions
GET    /audit-logs
POST   /audit-logs
GET    /audit-logs/latest
GET    /reports/inventory
```

---

## Comptes de test

En mode mock, les comptes suivants sont disponibles. Le MockInterceptor accepte n'importe quelle combinaison email/mot de passe valide.

| Rôle | Email | Mot de passe |
|------|-------|--------------|
| **Producteur** | `producteur@cannatrace.fr` | `Test1234!` |
| **Transformateur** | `transformateur@cannatrace.fr` | `Test1234!` |
| **Dispensaire** | `dispensaire@cannatrace.fr` | `Test1234!` |
| **Prescripteur** | `prescripteur@cannatrace.fr` | `Test1234!` |
| **Inspecteur ANSM** | `inspecteur@ansm.sante.fr` | `Test1234!` |

> **Note** : Les comptes doivent être pré-chargés dans la base locale Room. En mode démo, créez les utilisateurs via le backend mock ou ajoutez-les directement via le DAO.

---

## Modules métier

### Machine d'états des lots

```
GRAINE → GERMINATION → CROISSANCE → RÉCOLTE → SÉCHAGE
      → TRANSFORMATION → CONDITIONNEMENT → CONTRÔLE QUALITÉ
      → STOCKAGE → DISPENSATION → CLÔTURE

Depuis n'importe quel état actif : → DÉTRUIT (motif obligatoire)
```

**Règle immuabilité** : Un lot clôturé (`isClosed = true`) ne peut plus être modifié. Seule l'ajout d'une `CorrectionNote` avec justification est autorisé.

### Blockchain légère (Audit Logs)

Chaque action critique génère un hash chaîné :

```
hash_n = SHA256(hash_{n-1} | timestamp | action | entityId | data)
```

La fonction est dans `utils/HashUtils.kt`. Toute modification d'un log passé invalide tous les hashes suivants (détectable par `AuditLogRepository.verifyHashChain()`).

### RGPD — Anonymisation des patients

Le `AnonymizationWorker` s'exécute quotidiennement à 2h du matin et anonymise les données patient pour toute prescription dont la dernière dispensation remonte à plus de **2 ans** :

```sql
UPDATE prescriptions 
SET anonymizedPatientId = 'ANONYMISE_RGPD_' || id, adverseEffects = ''
WHERE endDate < (now - 2 ans) AND anonymizedPatientId NOT LIKE 'ANONYMISE_%'
```

### Divergences de stock

Toute divergence entre stock calculé et stock déclaré génère automatiquement un `DivergenceReport` (obligation réglementaire). Le rapport doit être résolu et signé par un opérateur habilité.

---

## Sécurité

| Mesure | Implémentation |
|--------|---------------|
| Chiffrement BDD | SQLCipher (AES-256) |
| Clé BDD | Android Keystore + EncryptedSharedPreferences |
| Hachage | SHA-256 chainé (logs d'audit) |
| Auth tokens | Chiffrement AES-CBC (Android Keystore) |
| Réseau | HTTPS + Network Security Config |
| Biométrie | BiometricPrompt API |

---

## Tests

```bash
# Tests unitaires
./gradlew test

# Tests instrumentés
./gradlew connectedAndroidTest
```

### Tests unitaires inclus

| Fichier | Couvre |
|---------|--------|
| `HashUtilsTest.kt` | SHA-256 déterministe, chaîné, vérification |
| `BatchTransitionTest.kt` | Machine d'états (transitions valides/invalides) |
| `AuditLogTest.kt` | Intégrité de la chaîne d'audit, détection falsification |

---

## Structure du projet

```
CannaTrace/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/cannatrace/      # Code source
│       │   └── res/                      # Ressources
│       └── test/                         # Tests unitaires
├── build.gradle.kts                      # Config projet
├── settings.gradle.kts
├── gradle/
│   ├── libs.versions.toml               # Catalogue de versions
│   └── wrapper/
│       └── gradle-wrapper.properties
└── README.md
```

---

## Conformité réglementaire

Cette application est conçue pour respecter :

- **ANSM** — Décision du 7 octobre 2020 relative à l'expérimentation du cannabis médical en France
- **RGPD** (EU 2016/679) — Droit à l'effacement, anonymisation automatique
- **CNIL** — Recommandations sur la durée de conservation des données de santé
- **ISO 11607** — Conditionnement pour dispositifs médicaux stériles
- **BPF** (Bonnes Pratiques de Fabrication) — Traçabilité et enregistrements

---

## Licence

Propriétaire — Usage réservé aux professionnels de santé habilités.  
© 2024 CannaTrace — Tous droits réservés.
