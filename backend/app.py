"""CannaTrace development API. Not approved for regulated production use."""
import os
import secrets
import time
from datetime import datetime, timedelta, timezone
from typing import Optional

import jwt
from fastapi import Depends, FastAPI, HTTPException, Query
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, Field
from pwdlib import PasswordHash
from sqlalchemy import Boolean, Column, Float, ForeignKey, Integer, String, create_engine, select
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./cannatrace-dev.db")
SECRET = os.getenv("JWT_SECRET")
if not SECRET or len(SECRET) < 32:
    raise RuntimeError("Set JWT_SECRET to a random value of at least 32 characters")
engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False} if DATABASE_URL.startswith("sqlite") else {})
SessionLocal = sessionmaker(engine)
hasher = PasswordHash.recommended()
auth = HTTPBearer()
app = FastAPI(title="CannaTrace API", version="0.1.0")


class Base(DeclarativeBase):
    pass


class User(Base):
    __tablename__ = "users"
    id = Column(String, primary_key=True)
    email = Column(String, unique=True, nullable=False)
    password_hash = Column(String, nullable=False)
    role = Column(String, nullable=False, default="PRODUCTEUR")


class Batch(Base):
    __tablename__ = "batches"
    id = Column(String, primary_key=True)
    batchNumber = Column(String, unique=True, nullable=False)
    strainName = Column(String, nullable=False)
    status = Column(String, nullable=False)
    weight = Column(Float, nullable=False)
    thcContent = Column(Float, nullable=False)
    cbdContent = Column(Float, nullable=False)
    createdAt = Column(Integer, nullable=False)
    updatedAt = Column(Integer, nullable=False)
    ownerId = Column(String, ForeignKey("users.id"), nullable=False)
    locationId = Column(String, nullable=False)
    parentBatchId = Column(String, nullable=True)
    isClosed = Column(Boolean, nullable=False, default=False)
    hashChain = Column(String, nullable=False, default="")
    notes = Column(String, nullable=False, default="")


class LoginRequest(BaseModel):
    email: str
    password: str


class RefreshRequest(BaseModel):
    refreshToken: str


class BatchInput(BaseModel):
    id: str
    batchNumber: str
    strainName: str
    status: str
    weight: float = Field(ge=0)
    thcContent: float = Field(ge=0, le=100)
    cbdContent: float = Field(ge=0, le=100)
    createdAt: int
    updatedAt: int
    ownerId: str
    locationId: str
    parentBatchId: Optional[str] = None
    isClosed: bool = False
    hashChain: str = ""
    notes: str = ""


class BatchPage(BaseModel):
    content: list[BatchInput]
    totalElements: int
    totalPages: int
    page: int
    size: int


def db():
    with SessionLocal() as session:
        yield session


def token(user: User, kind: str, minutes: int) -> str:
    now = datetime.now(timezone.utc)
    return jwt.encode({"sub": user.id, "role": user.role, "type": kind,
                       "iat": now, "exp": now + timedelta(minutes=minutes)}, SECRET, algorithm="HS256")


def current_user(credentials: HTTPAuthorizationCredentials = Depends(auth), session: Session = Depends(db)) -> User:
    try:
        payload = jwt.decode(credentials.credentials, SECRET, algorithms=["HS256"])
        if payload.get("type") != "access":
            raise ValueError("wrong token type")
        user = session.get(User, payload["sub"])
        if user is None:
            raise ValueError("unknown user")
        return user
    except (jwt.PyJWTError, KeyError, ValueError):
        raise HTTPException(401, "Invalid credentials")


@app.on_event("startup")
def initialize():
    Base.metadata.create_all(engine)
    email = os.getenv("BOOTSTRAP_EMAIL")
    password = os.getenv("BOOTSTRAP_PASSWORD")
    if email and password:
        if len(password) < 16:
            raise RuntimeError("BOOTSTRAP_PASSWORD must contain at least 16 characters")
        with SessionLocal() as session:
            if session.scalar(select(User.id).limit(1)) is None:
                session.add(User(id=secrets.token_hex(16), email=email.lower(),
                                 password_hash=hasher.hash(password), role="ADMIN"))
                session.commit()


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/v1/auth/login")
def login(request: LoginRequest, session: Session = Depends(db)):
    user = session.scalar(select(User).where(User.email == request.email.lower()))
    if not user or not hasher.verify(request.password, user.password_hash):
        raise HTTPException(401, "Invalid credentials")
    return {"accessToken": token(user, "access", 30), "refreshToken": token(user, "refresh", 10080),
            "expiresIn": 1800, "userId": user.id, "role": user.role}


@app.post("/v1/auth/refresh")
def refresh(request: RefreshRequest, session: Session = Depends(db)):
    try:
        payload = jwt.decode(request.refreshToken, SECRET, algorithms=["HS256"])
        if payload.get("type") != "refresh":
            raise ValueError("wrong token type")
        user = session.get(User, payload["sub"])
        if user is None:
            raise ValueError("unknown user")
    except (jwt.PyJWTError, KeyError, ValueError):
        raise HTTPException(401, "Invalid refresh token")
    return {"accessToken": token(user, "access", 30), "refreshToken": token(user, "refresh", 10080),
            "expiresIn": 1800, "userId": user.id, "role": user.role}


def owned_batch(batch_id: str, user: User, session: Session) -> Batch:
    batch = session.get(Batch, batch_id)
    if batch is None or (batch.ownerId != user.id and user.role != "ADMIN"):
        raise HTTPException(404, "Batch not found")
    return batch


def as_input(batch: Batch) -> BatchInput:
    return BatchInput.model_validate({key: getattr(batch, key) for key in BatchInput.model_fields})


@app.get("/v1/batches", response_model=BatchPage)
def batches(page: int = Query(0, ge=0), size: int = Query(50, ge=1, le=100),
            status: Optional[str] = None, user: User = Depends(current_user),
            session: Session = Depends(db)):
    query = select(Batch)
    if user.role != "ADMIN":
        query = query.where(Batch.ownerId == user.id)
    if status:
        query = query.where(Batch.status == status)
    all_batches = session.scalars(query.order_by(Batch.createdAt.desc())).all()
    return BatchPage(content=[as_input(b) for b in all_batches[page*size:(page+1)*size]],
                     totalElements=len(all_batches), totalPages=(len(all_batches)+size-1)//size,
                     page=page, size=size)


@app.get("/v1/batches/{batch_id}", response_model=BatchInput)
def get_batch(batch_id: str, user: User = Depends(current_user), session: Session = Depends(db)):
    return as_input(owned_batch(batch_id, user, session))


@app.post("/v1/batches", response_model=BatchInput, status_code=201)
def create_batch(body: BatchInput, user: User = Depends(current_user), session: Session = Depends(db)):
    if body.ownerId != user.id and user.role != "ADMIN":
        raise HTTPException(403, "Owner mismatch")
    if session.get(Batch, body.id) or session.scalar(select(Batch.id).where(Batch.batchNumber == body.batchNumber)):
        raise HTTPException(409, "Duplicate batch")
    batch = Batch(**body.model_dump())
    session.add(batch)
    session.commit()
    return as_input(batch)


@app.put("/v1/batches/{batch_id}", response_model=BatchInput)
def update_batch(batch_id: str, body: BatchInput, user: User = Depends(current_user),
                 session: Session = Depends(db)):
    batch = owned_batch(batch_id, user, session)
    if batch.isClosed or body.id != batch.id or body.ownerId != batch.ownerId or body.batchNumber != batch.batchNumber or body.status != batch.status:
        raise HTTPException(409, "Immutable identifiers/status or closed batch")
    for key in ("strainName", "weight", "thcContent", "cbdContent", "locationId", "notes"):
        setattr(batch, key, getattr(body, key))
    batch.updatedAt = int(time.time() * 1000)
    session.commit()
    return as_input(batch)


@app.post("/v1/batches/{batch_id}/transition", response_model=BatchInput)
def transition(batch_id: str, newStatus: str, reason: str = "",
               user: User = Depends(current_user), session: Session = Depends(db)):
    batch = owned_batch(batch_id, user, session)
    stages = ["GRAINE", "GERMINATION", "CROISSANCE", "RECOLTE", "SECHAGE",
              "TRANSFORMATION", "CONDITIONNEMENT", "CONTROLE_QUALITE", "STOCKAGE", "DISPENSATION"]
    if batch.isClosed or len(reason.strip()) < 5 or batch.status not in stages or newStatus not in stages or stages.index(newStatus) != stages.index(batch.status) + 1:
        raise HTTPException(409, "Invalid transition or missing justification")
    batch.status = newStatus
    batch.updatedAt = int(time.time() * 1000)
    if newStatus == "DISPENSATION":
        batch.isClosed = True
    session.commit()
    return as_input(batch)
