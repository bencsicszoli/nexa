import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Camera, Loader2, Trash2 } from 'lucide-react'
import Avatar from '../components/Avatar'
import AvatarCropper from '../components/AvatarCropper'
import PostComposer from '../components/PostComposer'
import PostCard from '../components/PostCard'
import { useAuth } from '../auth/AuthContext'
import { errorKey } from '../auth/errorKey'
import {
  confirmAvatar,
  removeAvatar,
  requestAvatarUploadUrl,
  updateProfile,
  uploadAvatarFile,
} from '../profile/profileApi'
import { getMyPosts } from '../posts/postApi'
import type { Post } from '../posts/types'

const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
const MAX_BYTES = 5 * 1024 * 1024
const BIO_MAX = 280

export default function ProfilePage() {
  const { t, i18n } = useTranslation()
  const { user, updateUser } = useAuth()
  const fileInput = useRef<HTMLInputElement>(null)

  const [displayName, setDisplayName] = useState(user?.displayName ?? '')
  const [bio, setBio] = useState(user?.bio ?? '')
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  // A kivágásra váró, épp kiválasztott fájl (megnyitja a kivágó ablakot).
  const [pendingFile, setPendingFile] = useState<File | null>(null)
  // Visszajelzés: vagy egy siker-, vagy egy hibaüzenet-kulcs.
  const [feedback, setFeedback] = useState<{ kind: 'ok' | 'error'; key: string } | null>(null)

  // A saját bejegyzések (legfrissebb felül) — itt jelenik meg az új poszt azonnal.
  const [posts, setPosts] = useState<Post[]>([])
  const [postsLoading, setPostsLoading] = useState(true)
  const [postsError, setPostsError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    getMyPosts()
      .then((loaded) => {
        if (active) setPosts(loaded)
      })
      .catch((err) => {
        if (active) setPostsError(errorKey(err))
      })
      .finally(() => {
        if (active) setPostsLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  if (!user) return null

  const memberSince = new Date(user.createdAt).toLocaleDateString(i18n.language, {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })

  async function onSave(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setFeedback(null)
    try {
      const updated = await updateProfile(displayName.trim(), bio.trim())
      updateUser(updated)
      setFeedback({ kind: 'ok', key: 'profile.saved' })
    } catch (err) {
      setFeedback({ kind: 'error', key: errorKey(err) })
    } finally {
      setSaving(false)
    }
  }

  function onPickFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    e.target.value = '' // hogy ugyanaz a fájl újra kiválasztható legyen
    if (!file) return

    setFeedback(null)
    if (!ALLOWED_TYPES.includes(file.type)) {
      setFeedback({ kind: 'error', key: 'auth.error.UNSUPPORTED_IMAGE_TYPE' })
      return
    }
    if (file.size > MAX_BYTES) {
      setFeedback({ kind: 'error', key: 'auth.error.PAYLOAD_TOO_LARGE' })
      return
    }
    // A feltöltés a kivágás megerősítése után indul (lásd onCropped).
    setPendingFile(file)
  }

  // A kivágóból kapott négyzetes kép (image/jpeg) feltöltése.
  async function onCropped(blob: Blob) {
    setPendingFile(null)
    setUploading(true)
    try {
      const contentType = 'image/jpeg'
      const cropped = new File([blob], 'avatar.jpg', { type: contentType })
      const target = await requestAvatarUploadUrl(contentType)
      await uploadAvatarFile(target.uploadUrl, cropped)
      const updated = await confirmAvatar(target.key)
      updateUser(updated)
      setFeedback({ kind: 'ok', key: 'profile.avatarUpdated' })
    } catch (err) {
      setFeedback({ kind: 'error', key: errorKey(err) })
    } finally {
      setUploading(false)
    }
  }

  async function onRemoveAvatar() {
    setFeedback(null)
    setUploading(true)
    try {
      const updated = await removeAvatar()
      updateUser(updated)
      setFeedback({ kind: 'ok', key: 'profile.avatarRemoved' })
    } catch (err) {
      setFeedback({ kind: 'error', key: errorKey(err) })
    } finally {
      setUploading(false)
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <header className="rounded-2xl border border-slate-200 bg-white p-6">
        <h1 className="text-lg font-semibold text-slate-900">{t('profile.title')}</h1>
        <p className="mt-0.5 text-sm text-slate-500">{t('profile.subtitle')}</p>

        <div className="mt-5 flex items-center gap-4">
          <div className="relative">
            <Avatar name={user.displayName} src={user.avatarUrl} size="lg" className="h-20 w-20 text-2xl" />
            {uploading && (
              <span className="absolute inset-0 flex items-center justify-center rounded-full bg-black/40">
                <Loader2 className="h-6 w-6 animate-spin text-white" />
              </span>
            )}
          </div>
          <div className="flex flex-col gap-2">
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                disabled={uploading}
                onClick={() => fileInput.current?.click()}
                className="inline-flex items-center gap-2 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
              >
                <Camera className="h-4 w-4" />
                {uploading ? t('profile.uploading') : t('profile.changeAvatar')}
              </button>
              {user.avatarUrl && (
                <button
                  type="button"
                  disabled={uploading}
                  onClick={onRemoveAvatar}
                  className="inline-flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 disabled:opacity-60"
                >
                  <Trash2 className="h-4 w-4" />
                  {t('profile.removeAvatar')}
                </button>
              )}
            </div>
            <p className="text-xs text-slate-400">{t('profile.avatarHint')}</p>
          </div>
          <input
            ref={fileInput}
            type="file"
            accept={ALLOWED_TYPES.join(',')}
            onChange={onPickFile}
            className="hidden"
          />
        </div>
      </header>

      <form onSubmit={onSave} className="rounded-2xl border border-slate-200 bg-white p-6">
        <div className="flex flex-col gap-4">
          <label className="flex flex-col gap-1">
            <span className="text-sm font-medium text-slate-700">{t('profile.displayName')}</span>
            <input
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              minLength={2}
              maxLength={50}
              required
              className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm outline-none transition-colors focus:border-brand focus:bg-white"
            />
          </label>

          <label className="flex flex-col gap-1">
            <span className="text-sm font-medium text-slate-700">{t('profile.bio')}</span>
            <textarea
              value={bio}
              onChange={(e) => setBio(e.target.value.slice(0, BIO_MAX))}
              rows={3}
              placeholder={t('profile.bioPlaceholder')}
              className="resize-none rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm outline-none transition-colors placeholder:text-slate-400 focus:border-brand focus:bg-white"
            />
            <span className="self-end text-xs text-slate-400">
              {bio.length}/{BIO_MAX}
            </span>
          </label>

          <div className="flex items-center justify-between gap-3">
            <p className="text-xs text-slate-400">
              {t('profile.memberSince', { date: memberSince })}
            </p>
            <button
              type="submit"
              disabled={saving}
              className="rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-dark disabled:opacity-60"
            >
              {saving ? t('profile.saving') : t('profile.save')}
            </button>
          </div>

          {feedback && (
            <p
              className={`text-sm ${feedback.kind === 'ok' ? 'text-emerald-600' : 'text-rose-600'}`}
              role={feedback.kind === 'error' ? 'alert' : 'status'}
            >
              {t(feedback.key)}
            </p>
          )}
        </div>
      </form>

      {/* Saját bejegyzések — itt írható és azonnal itt jelenik meg az új poszt (#5) */}
      <section className="flex flex-col gap-4">
        <h2 className="px-1 text-sm font-semibold text-slate-700">{t('posts.myPosts')}</h2>
        <PostComposer onCreated={(post) => setPosts((prev) => [post, ...prev])} />

        {postsLoading ? (
          <div className="flex justify-center py-8 text-slate-400">
            <Loader2 className="h-5 w-5 animate-spin" />
          </div>
        ) : postsError ? (
          <p className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-600" role="alert">
            {t(postsError)}
          </p>
        ) : posts.length === 0 ? (
          <p className="rounded-2xl border border-dashed border-slate-300 bg-white px-4 py-8 text-center text-sm text-slate-500">
            {t('posts.empty')}
          </p>
        ) : (
          posts.map((post) => (
            <PostCard
              key={post.id}
              post={post}
              editable
              onUpdated={(updated) =>
                setPosts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)))
              }
              onDeleted={(id) => setPosts((prev) => prev.filter((p) => p.id !== id))}
            />
          ))
        )}
      </section>

      {pendingFile && (
        <AvatarCropper
          file={pendingFile}
          onCancel={() => setPendingFile(null)}
          onConfirm={onCropped}
        />
      )}
    </div>
  )
}
