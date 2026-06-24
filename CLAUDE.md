# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## A termék — mi a Nexa

Reklámmentes, **előfizetéses** (havidíjas, **14 napos próbaidővel**) közösségi platform. A megkülönböztető jegy:
a hírfolyam **algoritmusmentes** és **időrendi** (legfrissebb felül).

Funkcionális követelmények (a megrendelő szerint):
- Tartalomfeltöltés: **szöveg, kép, videó**.
- **Ismerősök** (kétirányú), **követés** (egyirányú: tartalomgyártók, közéleti szereplők).
- **Csoportok**: a létrehozó automatikusan **admin**; lehet csatlakozni / kilépni.
- **Hírfolyam**: KIZÁRÓLAG ismerősök + követettek posztjai + a felhasználó **tag-csoportjainak** bejegyzései,
  időrendben. Ez kőbe vésett szabály — ne kerüljön bele ajánló/algoritmikus rangsor.
- **Csevegés és videohívás** (Discord-szerű).
- **Valós idejű értesítés**: ha böngészés közben egy kapcsolat új tartalmat tölt fel, értesítés jelenik meg;
  az értesítésre kattintva **frissül a hírfolyam**, és ott is megjelenik az új bejegyzés.
- **Kétnyelvű** felület: **magyar / angol**, választható.
- **Fizetés/előfizetés biztonsága garantált**; az ügyféladatok védelme elsődleges.

## Technológiai döntések (megbeszélve — ne térj el tőlük indoklás nélkül)

| Réteg | Választás | Megjegyzés |
|-------|-----------|-----------|
| Frontend | React + TypeScript + Tailwind + **Vite** | i18n: **react-i18next** (EN/HU); szerverállapot: TanStack Query |
| Backend | **Spring Boot (Java 21), Maven** | REST + WebSocket/STOMP + WebRTC-signaling. **Gradle nincs telepítve — Maven a build.** |
| Adatbázis | **SQL-first: PostgreSQL** (+ `JSONB`) | Az érzékeny/üzleti adat (user, kapcsolat, fizetés) ide kerül, ACID-dal |
| Cache / realtime | **Redis** | gyorsítótár, online jelenlét, pub/sub a hírfolyam-fan-outhoz |
| Médiatárolás | **R2 / S3** objektumtároló | kép/videó **presigned URL**-lel; a DB csak metaadatot/URL-t tárol (gazdaságos) |
| Fizetés | **Paddle** (Merchant of Record) | a Paddle intézi az EU-ÁFA bevallást; **kártyaadat sosem érinti a szervert** |

Adatbázis-elv: a NoSQL **nem** kiindulópont. PostgreSQL + Redis a default; külön NoSQL réteg (pl. ScyllaDB a
materializált timeline-ra) csak akkor, ha a **mérés** indokolja — ne vezess be előre.

## Munkamódszer (KÖTELEZŐ)

A megvalósítás a **GitHub Project (#7)** alapján megy, **kártyáról kártyára**:
- Backlog: [`backlog.md`](./backlog.md) és a Project: <https://github.com/users/bencsicszoli/projects/7>
  (18 issue, `#1`–`#18`; a sorrend és a függőségek a backlogban / az issue-k „Függőség" sorában).
- Minden kártya úgy van vágva, hogy a befejezése után a változás **azonnal tesztelhető a frontenden**.
- **Egyszerre EGY kártyán dolgozz.** Munka előtt a kártya Status mezőjét állítsd **In Progress**-re.
- A kártya akkor kész, ha **megvalósítottad ÉS leteszteltél** (lásd lentebb a végpont-tesztet és a böngészős
  kézi happy-path ellenőrzést). A commit/PR `Closes #N`-nel zárja az issue-t.
- Ezután **ÁLLJ MEG és értesítsd a felhasználót**, hogy ellenőrizze az eredményt a böngészőben — ne lépj át
  automatikusan a következő kártyára. Korrekció után folytasd.
- **Tesztelés után MINDIG zárd be a 8080-as portot:** ha a teszthez magad indítottad a backendet (akár
  háttérben), állítsd le, mielőtt visszaadod a vezérlést — különben a felhasználó saját `mvn spring-boot:run`-ja
  „port already in use" hibával elhasal. Parancs: `pkill -f 'spring-boot:run|NexaApplication'`, majd ellenőrizd
  `ss -ltnp | grep ':8080'`-nal, hogy felszabadult.

Git: a commit szerző e-mail a GitHub **noreply** cím legyen
(`171580038+bencsicszoli@users.noreply.github.com`) — a privát e-maillel a push elutasításra kerül.

## Gyakori parancsok

```bash
# Backend (http://localhost:8080)
cd backend && mvn spring-boot:run
mvn test                                   # összes teszt
mvn test -Dtest=HealthControllerTest       # egy teszt
mvn test -Dtest=HealthControllerTest#healthReturnsUp   # egy metódus
mvn clean package                          # JAR build
# Teszthez indított backend leállítása (KÖTELEZŐ, hogy a 8080 felszabaduljon):
pkill -f 'spring-boot:run|NexaApplication'; ss -ltnp | grep ':8080' || echo "8080 szabad"

# Frontend (http://localhost:5173 — a /api a 8080-ra proxyzva)
cd frontend && npm install && npm run dev
npm run build        # típusellenőrzés (tsc --noEmit) + vite build
npm run typecheck

# Teljes stack konténerben (előbb: cp .env.example .env)
docker compose up --build

# GitHub Project kezelése
gh project item-list 7 --owner bencsicszoli --format json
gh issue close N --repo bencsicszoli/nexa
```

## Architektúra

Monorepo: `frontend/` (Vite SPA) + `backend/` (Spring Boot), `docker-compose.yml` köti össze
(postgres + redis + backend + frontend).

```
React SPA ──REST──▶ Spring Boot ──▶ PostgreSQL  (user, kapcsolat, csoport, poszt-metaadat, előfizetés)
   │  ▲  ──WS/STOMP─▶ (chat, értesítés) ─▶ Redis (cache, jelenlét, pub/sub)
   │  └─ WS signaling▶ (WebRTC SDP/ICE)
   └─ presigned URL ──────────────────────▶ R2/S3 (kép, videó)
   └─ Paddle hosztolt checkout ──webhook──▶ backend → subscriptions (DB)
```

Konvenciók a meglévő vázból (kövesd új kódnál is):
- Backend csomag: `com.nexa.*`; REST végpontok az **`/api`** prefix alatt (lásd `HealthController`).
  Minden új végponthoz `@WebMvcTest`/integrációs teszt.
- CORS a `WebConfig`-ban, az engedélyezett origin a `nexa.cors.allowed-origins` configból jön. A 3. kártyától
  (Spring Security + JWT) ezt szigorítani kell a token-folyamhoz.
- Frontend márkaszín: a Tailwind `brand` (`#6d28d9`) — a kiválasztott **„A" (3 oszlopos)** főoldal-elrendezést
  követjük (makettek: `~/Képek/plan1.png` a kiválasztott; `plan2/3.png` az alternatívák).
- Titkok: `.env` (gitignore-olt), minta: `.env.example`.

## Állapot

`#1` (váz + health check), `#2` (app shell + i18n), `#3` (regisztráció/bejelentkezés JWT-vel),
`#4` (profil: szerkesztés + avatar), `#5` (szöveges bejegyzés létrehozása) és `#6` (kép/videó a
bejegyzésben) **kész és lezárva**. A következő `#7` (ismerősök).

**Poszt-média (`#6`):** a bejegyzéshez kép/videó csatolható, ugyanazzal a presigned-URL mintával, mint
az avatar. Feltöltési URL: `POST /api/posts/media/upload-url` (kép: JPEG/PNG/WebP/GIF; videó: MP4/WebM),
majd a `POST /api/posts` a média-kulcsokkal jön létre (`media: [{key,type,sizeBytes}]`); a poszt-média a
`post_media` táblában (URL + típus + méret) él. A médiakulcs `posts/` prefixet kap; videóhoz külön, nagyobb
méretkorlát (`nexa.storage.max-video-bytes`, alap 50 MB). Szöveg nélküli, csak médiát tartalmazó poszt is
mehet; üres poszt → `EMPTY_POST`. Megjelenítés a `PostCard`-ban: képrács + beágyazott `<video controls>`.
Támogatott videó: MP4, WebM és **MKV** (`video/x-matroska`); az MKV böngészős lejátszása kodekfüggő.
A saját poszt **szerkeszthető** (csak a szöveg: `PATCH /api/posts/{id}`) és **törölhető**
(`DELETE /api/posts/{id}`) — idegen poszt → `POST_NOT_FOUND` (404, a létezést sem szivárogtatjuk);
a `PostCard` „…" menüjéből, a `ProfilePage` `editable` propján át.

**Fájl-élettartam (árva fájlok ellen):** a `StorageService` `delete(key)` + `keyFromPublicUrl(url)`
párral takarít. A tényleges fájltörlés a DB-**commit után**, best-effort fut
(`DeferredStorageDeleter` + tranzakció-szinkronizáció), így rollbacknál nem törlünk élő fájlt.
Poszt törlésekor a csatolt médiafájlok, avatar **cseréjekor/eltávolításakor** a régi kép is törlődik
— sehol nem marad árva objektum. Az s3 providernek (amikor elkészül) szintén implementálnia kell a
`delete`/`keyFromPublicUrl` metódusokat.

**Tárolás (`#4`-től):** pluggable `StorageService` presigned-URL mintára. Alapból a **`local`** provider
fut (`nexa.storage.provider=local`): aláírt PUT a `/api/storage/upload`-ra, a fájl lemezre kerül
(`nexa.storage.local.dir`, alap `backend/data/media`, gitignore-olt), kiszolgálás a publikus
`GET /api/media/**` alól — külső infra nélkül azonnal tesztelhető. Az **`s3`** provider (R2 presigned URL)
ugyanezt a frontend-szerződést valósítja meg, így átálláskor a frontend nem változik, csak az env — ez
külön (nem böngészőben tesztelhető) infra-feladat, nem a `#6` része.

**Dev-adatbázis (3. kártyától):** a fejlesztőgépen az 5432-es porton másik Postgres fut, ezért a
Nexa Postgres Dockerből a **5433** host-porton megy (`docker compose up -d postgres redis`). A backend
alapból a `localhost:5433/nexa`-ra csatlakozik (`DB_URL/DB_USER/DB_PASSWORD` env-vel felülírható);
a `docker` profilban `postgres:5432`. A backend-tesztek H2 in-memory DB-vel futnak — nem kell élő Postgres.

A backlogot, a függőségeket és a részletes elfogadási kritériumokat lásd a Project issue-iban.
