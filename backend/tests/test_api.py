import os
import secrets
import tempfile

db_file = tempfile.NamedTemporaryFile(suffix=".db", delete=False)
os.environ["DATABASE_URL"] = "sqlite:///" + db_file.name
os.environ["JWT_SECRET"] = secrets.token_urlsafe(48)
os.environ["BOOTSTRAP_EMAIL"] = "admin@example.invalid"
os.environ["BOOTSTRAP_PASSWORD"] = "a-long-random-test-password"

from fastapi.testclient import TestClient
from app import app

def test_auth_and_batch_lifecycle():
    with TestClient(app) as client:
        assert client.get("/health").status_code == 200
        assert client.get("/v1/batches").status_code in (401, 403)
        assert client.post("/v1/auth/login", json={"email": "admin@example.invalid", "password": "wrong"}).status_code == 401
        login = client.post("/v1/auth/login", json={"email": "admin@example.invalid", "password": "a-long-random-test-password"})
        assert login.status_code == 200
        data = login.json()
        headers = {"Authorization": "Bearer " + data["accessToken"]}
        body = {"id": "lot-1", "batchNumber": "FR-TEST-001", "strainName": "test",
                "status": "GRAINE", "weight": 0, "thcContent": 0, "cbdContent": 0,
                "createdAt": 1, "updatedAt": 1, "ownerId": data["userId"], "locationId": "site-1"}
        assert client.post("/v1/batches", json=body, headers=headers).status_code == 201
        assert client.post("/v1/batches", json=body, headers=headers).status_code == 409
        assert client.get("/v1/batches", headers=headers).json()["totalElements"] == 1
        assert client.post("/v1/batches/lot-1/transition", params={"newStatus": "GERMINATION", "reason": "test step"}, headers=headers).status_code == 200
        assert client.post("/v1/batches/lot-1/transition", params={"newStatus": "STOCKAGE", "reason": "invalid jump"}, headers=headers).status_code == 409
        assert client.get("/v1/batches/lot-1", headers=headers).json()["status"] == "GERMINATION"
        assert client.post("/v1/auth/refresh", json={"refreshToken": data["refreshToken"]}).status_code == 200
