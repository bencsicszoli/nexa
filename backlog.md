# Nexa — Megvalósítási backlog (18 kártya)

Minden kártya úgy van vágva, hogy a befejezése után a **frontenden azonnal tesztelhető** a változás.
Függőségek: 1→2→3 alap; 4–9 párhuzamosítható 3 után; 10 a 7–9 után; 11–13 a 10 után;
14–15 bármikor a 3 után; 16–18 a végén.

## 0. Alap

### 1. Monorepo váz + Docker Compose + health check
Vite+React+TS+Tailwind frontend, Spring Boot backend, Postgres+Redis konténer.
**Teszt:** a kezdőoldal betölt és zöld „UP" backend-állapotot mutat.

### 2. App shell (A elrendezés) + design system + EN/HU i18n
Top bar, bal navigáció, jobb sáv, üres hírfolyam-placeholder; react-i18next nyelvváltóval.
**Teszt:** navigáció a vázban, reszponzivitás, nyelvváltás megváltoztatja a feliratokat.

### 3. Regisztráció + bejelentkezés (JWT)
Spring Security, regisztrációs/bejelentkezési űrlap, Argon2/bcrypt jelszó-hash, védett route-ok, refresh token.
**Teszt:** regisztráció, belépés, „bejelentkezett" állapot, kilépés.

## 1. Tartalom

### 4. Profil
Profil megtekintése/szerkesztése (név, avatar, bio), avatar feltöltés R2-be presigned URL-lel.
**Teszt:** profil szerkesztése, avatar feltöltése, a változás látszik.

### 5. Szöveges bejegyzés létrehozása
Szerkesztődoboz, poszt API, megjelenítés a saját profilon.
**Teszt:** poszt írása, azonnali megjelenés.

### 6. Kép/videó a bejegyzésben
R2 presigned feltöltés, média-megjelenítés és videolejátszás a posztban.
**Teszt:** kép és videó feltöltése, megjelenítés/lejátszás a posztban.

## 2. Kapcsolati gráf

### 7. Ismerősök
Ismerőskérés küldése/elfogadása/elutasítása, ismerőslista.
**Teszt:** két fiók ismerőssé válik.

### 8. Követés
Tartalomgyártók követése/lekövetése, követési lista.
**Teszt:** követés bekapcsolása, megjelenés a listában.

### 9. Csoportok
Csoport létrehozása (létrehozó = admin), böngészés, csatlakozás/kilépés, csoportoldal posztokkal.
**Teszt:** csoport létrehozása, csatlakozás, posztolás a csoportban.

## 3. Hírfolyam + valós idő

### 10. Időrendi hírfolyam
Ismerős + követett + tag-csoport posztok aggregálása, legfrissebb felül, lapozás (cursor-alapú).
**Teszt:** a hírfolyam a helyes posztokat helyes sorrendben mutatja.

### 11. Valós idejű értesítés
WebSocket/STOMP; ha egy kapcsolat posztol → értesítés; kattintásra frissül a hírfolyam, megjelenik az új poszt.
**Teszt:** két böngésző: egyik posztol, másik értesítést kap, kattintásra frissül.

### 12. Chat (1:1 és csoport)
WebSocket üzenetküldés, chat UI, üzenet-előzmények, online jelenlét (Redis).
**Teszt:** üzenetek élő küldése/fogadása, előzmény betöltése.

### 13. Videohívás (1:1, WebRTC)
WebSocket signaling (SDP/ICE), STUN/TURN (coturn), hívás UI.
**Teszt:** két felhasználó között elindul a videohívás.

## 4. Bevétel + befejezés

### 14. Paddle előfizetés
14 napos trial, Paddle hosztolt checkout, számlázási portál, webhook → előfizetési állapot a DB-ben.
**Teszt:** regisztráció → trial → előfizetés Paddle sandboxban, az állapot frissül.

### 15. Trial/előfizetés gating
Lejárt trial után paywall, funkciók korlátozása fizetés nélkül.
**Teszt:** lejárt trial szimulálása → paywall jelenik meg.

### 16. Keresés
Felhasználók, csoportok és bejegyzések keresése.
**Teszt:** keresés releváns találatokat ad.

### 17. Értesítési központ + beállítások
Értesítési preferenciák, előzmény; adatvédelmi és fiókbeállítások, 2FA.
**Teszt:** beállítás módosítása, értesítési előzmény, 2FA bekapcsolása.

### 18. GDPR + biztonsági megerősítés + deploy/observability
Adatexport, fióktörlés, rate limiting, security headerek, HTTPS, monitoring/logging, staging URL.
**Teszt:** adatexport letöltése, fiók törlése, staging URL elérése.
