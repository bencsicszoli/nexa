// A hírfolyam frissítésének jelzése böngésző-eseménnyel (a logout-eseményhez hasonló mintával).
// Az értesítésre kattintva a FeedPage erre az eseményre újratölti az első lapot, így az új
// bejegyzés időrendben felül megjelenik (#11 DoD).

export const FEED_REFRESH_EVENT = 'nexa:refresh-feed'

/** Kéri a hírfolyam (újra)betöltését, ha a FeedPage épp meg van nyitva. */
export function requestFeedRefresh(): void {
  window.dispatchEvent(new Event(FEED_REFRESH_EVENT))
}
