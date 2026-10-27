# CannaTrace backend (prototype)

API FastAPI de développement : authentification JWT, consultation/création de lots, mise à jour et transitions. **Non prêt pour la production réglementée**.

## Démarrage

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
export JWT_SECRET="$(python -c 'import secrets; print(secrets.token_urlsafe(48))')"
export BOOTSTRAP_EMAIL="admin@example.invalid"
export BOOTSTRAP_PASSWORD="replace-with-a-long-random-passphrase"
uvicorn app:app --reload
```

Par défaut, la base locale SQLite est `cannatrace-dev.db`. Pour PostgreSQL, installer le pilote psycopg et définir `DATABASE_URL`. La documentation Swagger est disponible sur `/docs`.

L'API utilise le préfixe `/v1`, identique aux routes Retrofit Android. Le compte initial n'est créé que si la base utilisateurs est vide et que les deux variables BOOTSTRAP sont définies.

## Manques bloquants avant production

- Ajouter migrations versionnées, PostgreSQL, sauvegardes et monitoring.
- Implémenter tous les endpoints Android restants : plantes, culture, récolte, transformation, stock, prescriptions, rapports et audit.
- Implémenter audit append-only des changements de lots et corrections.
- Ajouter séparation stricte des organisations, habilitations professionnelles et MFA.
- Éviter de stocker des données patients tant que l'hébergement, le RGPD et les autorisations ne sont pas validés.
- Sécuriser et faire valider la matrice des transitions selon les procédures françaises applicables.
- Déployer derrière TLS, rate limiting, rotation/révocation des jetons et supervision.

Ce prototype ne démontre aucune conformité ANSM et ne doit pas être utilisé avec des données réelles réglementées.
