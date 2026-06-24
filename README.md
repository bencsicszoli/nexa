# Nexa

Reklámmentes, előfizetéses közösségi platform (14 napos próbaidővel). Algoritmusmentes,
**időrendi** hírfolyam: csak ismerősök, követett alkotók és tag-csoportok bejegyzései jelennek meg.

## Funkciók (terv)

- Tartalom: szöveg, kép, videó · ismerősök · követés · csoportok (létrehozó = admin)
- Discord-szerű chat és videohívás (WebRTC)
- Valós idejű értesítés új tartalomról (kattintásra frissül a hírfolyam)
- Kétnyelvű felület: **magyar / angol**
- Előfizetés Paddle-lel (Merchant of Record — EU-ÁFA kezelve)

## Technológia

| Réteg | Eszköz |
|-------|--------|
| Frontend | React + TypeScript + Tailwind + Vite, react-i18next, TanStack Query |
| Backend | Spring Boot (Java 21), REST + WebSocket/STOMP + WebRTC-signaling |
| Adatbázis | PostgreSQL (+ JSONB), Redis (cache/realtime), R2/S3 (média) |
| Fizetés | Paddle |

## Gyors indítás (helyi fejlesztés)

Két terminál:

```bash
# 1) Backend  (http://localhost:8080)
cd backend
mvn spring-boot:run

# 2) Frontend (http://localhost:5173)
cd frontend
npm install
npm run dev
```

Nyisd meg a <http://localhost:5173> címet — a kezdőoldalnak **zöld „UP"** backend-állapotot kell mutatnia.

## Docker (teljes stack)

```bash
cp .env.example .env   # töltsd ki a titkokat
docker compose up --build
```

Ez elindítja a PostgreSQL-t, Redist, a backendet és a frontendet.

## Backlog

A megvalósítás 18 kártyára bontva: lásd [`backlog.md`](./backlog.md).
A kártyák GitHub Projectbe töltéséhez: [`scripts/setup-github-project.sh`](./scripts/setup-github-project.sh).

## Tervek / makettek

A főoldal három koncepciója: `~/Képek/plan1.png` (kiválasztott — klasszikus 3 oszlop),
`plan2.png` (fókuszált egy oszlop), `plan3.png` (Discord-stílus).
