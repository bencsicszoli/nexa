import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Loader2, PenLine } from 'lucide-react'
import Avatar from './Avatar'
import { useAuth } from '../auth/AuthContext'
import { errorKey } from '../auth/errorKey'
import { createPost } from '../posts/postApi'
import type { Post } from '../posts/types'

const MAX_CHARS = 5000

type Props = {
  /** A sikeresen létrehozott bejegyzéssel hívódik meg (pl. a lista elejére fűzéshez). */
  onCreated: (post: Post) => void
}

/**
 * Szövegszerkesztő doboz új bejegyzés létrehozásához (#5 kártya).
 * A média (#6) később külön gombokkal egészíti ki.
 */
export default function PostComposer({ onCreated }: Props) {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [content, setContent] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const trimmed = content.trim()
  const canSubmit = trimmed.length > 0 && !submitting

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!canSubmit) return
    setSubmitting(true)
    setError(null)
    try {
      const post = await createPost(trimmed)
      setContent('')
      onCreated(post)
    } catch (err) {
      setError(errorKey(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={onSubmit} className="rounded-2xl border border-slate-200 bg-white p-4">
      <div className="flex items-start gap-3">
        <Avatar name={user?.displayName ?? 'Nexa'} src={user?.avatarUrl} size="md" />
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value.slice(0, MAX_CHARS))}
          rows={3}
          placeholder={t('composer.placeholder')}
          className="w-full resize-none rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm outline-none transition-colors placeholder:text-slate-400 focus:border-brand focus:bg-white"
        />
      </div>
      <div className="mt-3 flex items-center justify-between gap-3 border-t border-slate-100 pt-3">
        <span className="text-xs text-slate-400">
          {content.length}/{MAX_CHARS}
        </span>
        <div className="flex items-center gap-3">
          {error && (
            <span className="text-sm text-rose-600" role="alert">
              {t(error)}
            </span>
          )}
          <button
            type="submit"
            disabled={!canSubmit}
            className="inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
          >
            {submitting ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <PenLine className="h-4 w-4" />
            )}
            {submitting ? t('composer.posting') : t('composer.post')}
          </button>
        </div>
      </div>
    </form>
  )
}
