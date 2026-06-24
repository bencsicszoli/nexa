#!/usr/bin/env bash
#
# Nexa — publikus GitHub repo + GitHub Project (v2) létrehozása és feltöltése a 18 kártyával.
#
# Előfeltétel:
#   1) gh CLI telepítve:     https://cli.github.com/
#   2) bejelentkezve:        gh auth login   (project scope-pal: gh auth refresh -s project,read:project)
#
# Használat:
#   bash scripts/setup-github-project.sh [REPO_NÉV]
# Alapértelmezett repo név: nexa
#
set -euo pipefail

REPO_NAME="${1:-nexa}"
PROJECT_TITLE="Nexa megvalósítás"

command -v gh >/dev/null 2>&1 || { echo "❌ A 'gh' CLI nincs telepítve. Lásd: https://cli.github.com/"; exit 1; }
gh auth status >/dev/null 2>&1 || { echo "❌ Nem vagy bejelentkezve. Futtasd: gh auth login"; exit 1; }

OWNER="$(gh api user --jq .login)"
echo "👤 Felhasználó: $OWNER"

# 1) Repo létrehozása (publikus) és a helyi mappa push-olása
if gh repo view "$OWNER/$REPO_NAME" >/dev/null 2>&1; then
  echo "ℹ️  A repo már létezik: $OWNER/$REPO_NAME"
else
  echo "📦 Repo létrehozása: $OWNER/$REPO_NAME (publikus)"
  gh repo create "$OWNER/$REPO_NAME" --public --source=. --remote=origin --push
fi

# 2) GitHub Project (v2) létrehozása a felhasználó alatt
echo "🗂️  Project létrehozása: $PROJECT_TITLE"
PROJECT_NUMBER="$(gh project create --owner "$OWNER" --title "$PROJECT_TITLE" --format json --jq .number)"
echo "   Project száma: #$PROJECT_NUMBER"

# 3) 18 kártya — címek (cím :: törzs)
declare -a CARDS=(
  "1. Monorepo váz + Docker Compose + health check::Vite+React+TS+Tailwind FE, Spring Boot BE, Postgres+Redis. Teszt: kezdőoldal zöld UP állapotot mutat."
  "2. App shell (A elrendezés) + design system + EN/HU i18n::Top bar, bal nav, jobb sáv, üres hírfolyam; nyelvváltó. Teszt: navigáció + nyelvváltás működik."
  "3. Regisztráció + bejelentkezés (JWT)::Spring Security, űrlapok, jelszó-hash, védett route-ok. Teszt: regisztráció, belépés, kilépés."
  "4. Profil::Megtekintés/szerkesztés, avatar R2-be presigned URL-lel. Teszt: profil szerkesztése, avatar feltöltés."
  "5. Szöveges bejegyzés létrehozása::Composer, poszt API, megjelenítés. Teszt: poszt írása, azonnali megjelenés."
  "6. Kép/videó a bejegyzésben::R2 presigned feltöltés, média megjelenítés. Teszt: kép/videó feltöltés + lejátszás."
  "7. Ismerősök::Kérés/elfogadás/elutasítás, ismerőslista. Teszt: két fiók ismerőssé válik."
  "8. Követés::Követés/lekövetés, követési lista. Teszt: követés + lista."
  "9. Csoportok::Létrehozás (admin), csatlakozás/kilépés, csoportposztok. Teszt: csoport + csatlakozás + poszt."
  "10. Időrendi hírfolyam::Ismerős+követett+csoport posztok időrendben, lapozás. Teszt: helyes posztok, helyes sorrend."
  "11. Valós idejű értesítés::WebSocket; új poszt → értesítés → kattintásra frissül. Teszt: 2 böngésző, értesítés + frissítés."
  "12. Chat (1:1 és csoport)::WebSocket üzenetek, előzmények, jelenlét. Teszt: élő üzenetküldés/fogadás."
  "13. Videohívás (1:1, WebRTC)::Signaling + STUN/TURN, hívás UI. Teszt: két fél közti videohívás elindul."
  "14. Paddle előfizetés::14 napos trial, checkout, webhook → állapot DB-ben. Teszt: trial → előfizetés sandboxban."
  "15. Trial/előfizetés gating::Paywall lejárt trial után, funkciókorlátozás. Teszt: lejárt trial → paywall."
  "16. Keresés::Felhasználók/csoportok/posztok keresése. Teszt: releváns találatok."
  "17. Értesítési központ + beállítások::Preferenciák, előzmény, adatvédelem, 2FA. Teszt: beállítás + 2FA."
  "18. GDPR + biztonság + deploy/observability::Adatexport, fióktörlés, rate limit, HTTPS, monitoring. Teszt: export, törlés, staging URL."
)

echo "🧩 Kártyák hozzáadása ($((${#CARDS[@]})) db)…"
for entry in "${CARDS[@]}"; do
  title="${entry%%::*}"
  body="${entry#*::}"
  gh project item-create "$PROJECT_NUMBER" --owner "$OWNER" --title "$title" --body "$body" >/dev/null
  echo "   ✔ $title"
done

echo ""
echo "✅ Kész. Project: https://github.com/users/$OWNER/projects/$PROJECT_NUMBER"
echo "   Repo:    https://github.com/$OWNER/$REPO_NAME"
