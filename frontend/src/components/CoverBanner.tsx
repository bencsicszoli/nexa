import { Loader2 } from 'lucide-react'

type CoverBannerProps = {
  /** A borítókép URL-je, vagy null → halvány márkaszínű placeholder. */
  url: string | null
  /** Feltöltés/eltávolítás közben pörgő overlay (csak a saját profilon). */
  busy?: boolean
}

/**
 * A profil-fejléc tetején teljes szélességben megjelenő 3:1-es borítókép. A felső sarkok
 * lekerekítését a szülő kártya {@code overflow-hidden}-je adja (konzisztens a többi kártyával).
 * Ha nincs kép, halvány márkaszínű átmenetet mutat.
 */
export default function CoverBanner({ url, busy }: CoverBannerProps) {
  return (
    <div className="relative aspect-[3/1] w-full bg-gradient-to-br from-brand/25 to-brand/5">
      {url && <img src={url} alt="" className="h-full w-full object-cover" />}
      {busy && (
        <span className="absolute inset-0 flex items-center justify-center bg-black/30">
          <Loader2 className="h-6 w-6 animate-spin text-white" />
        </span>
      )}
    </div>
  )
}
