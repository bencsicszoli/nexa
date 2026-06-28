import { useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { Mic, MicOff, Phone, PhoneOff, Video, VideoOff } from 'lucide-react'
import { useCall } from './CallContext'
import type { CallPeer } from './types'

/**
 * A videohívás (#13) teljes képernyős felülete. Az {@link useCall} státuszától függően mutat:
 * bejövő hívás (elfogad/elutasít), kimenő hívás (csörög/megszakít), vagy élő hívás (távoli videó
 * teljes méretben, saját kép kicsiben, némítás/kamera/bontás vezérlőkkel). A {@code AppShell}
 * mindig mountolja, így a bejövő hívás bárhol a felületen megjelenik.
 */
export default function CallOverlay() {
  const { status } = useCall()
  if (status === 'idle') return null
  if (status === 'incoming') return <IncomingCall />
  if (status === 'outgoing') return <OutgoingCall />
  return <ActiveCall />
}

/** Egy MediaStream megjelenítése egy <video> elemben (a srcObject-et nem lehet attribútumban átadni). */
function VideoView({
  stream,
  muted,
  className,
}: {
  stream: MediaStream | null
  muted?: boolean
  className?: string
}) {
  const ref = useRef<HTMLVideoElement | null>(null)
  useEffect(() => {
    if (ref.current && ref.current.srcObject !== stream) {
      ref.current.srcObject = stream
    }
  }, [stream])
  return <video ref={ref} autoPlay playsInline muted={muted} className={className} />
}

/** Nagy, kör alakú avatar a hívásképernyőkre (a kis listás Avatar helyett). */
function BigAvatar({ peer }: { peer: CallPeer | null }) {
  const name = peer?.name ?? '?'
  if (peer?.avatarUrl) {
    return <img src={peer.avatarUrl} alt={name} className="h-28 w-28 rounded-full object-cover" />
  }
  const monogram = name
    .trim()
    .split(/\s+/)
    .map((p) => p[0] ?? '')
    .slice(0, 2)
    .join('')
    .toUpperCase()
  return (
    <span className="flex h-28 w-28 items-center justify-center rounded-full bg-brand text-4xl font-semibold text-white">
      {monogram}
    </span>
  )
}

function IncomingCall() {
  const { t } = useTranslation()
  const { peer, acceptCall, rejectCall } = useCall()
  return (
    <Backdrop>
      <div className="flex w-full max-w-sm flex-col items-center gap-6 rounded-3xl bg-white p-8 text-center shadow-2xl">
        <BigAvatar peer={peer} />
        <div>
          <p className="text-lg font-semibold text-slate-900">{peer?.name}</p>
          <p className="text-sm text-slate-500">{t('call.incoming')}</p>
        </div>
        <div className="flex items-center gap-8">
          <CircleButton color="rose" label={t('call.reject')} onClick={rejectCall}>
            <PhoneOff className="h-6 w-6" />
          </CircleButton>
          <CircleButton color="emerald" label={t('call.accept')} onClick={() => void acceptCall()}>
            <Phone className="h-6 w-6" />
          </CircleButton>
        </div>
      </div>
    </Backdrop>
  )
}

function OutgoingCall() {
  const { t } = useTranslation()
  const { peer, hangup } = useCall()
  return (
    <Backdrop>
      <div className="flex w-full max-w-sm flex-col items-center gap-6 rounded-3xl bg-white p-8 text-center shadow-2xl">
        <BigAvatar peer={peer} />
        <div>
          <p className="text-lg font-semibold text-slate-900">{peer?.name}</p>
          <p className="text-sm text-slate-500">{t('call.calling')}</p>
        </div>
        <CircleButton color="rose" label={t('call.cancel')} onClick={hangup}>
          <PhoneOff className="h-6 w-6" />
        </CircleButton>
      </div>
    </Backdrop>
  )
}

function ActiveCall() {
  const { t } = useTranslation()
  const { peer, localStream, remoteStream, micOn, cameraOn, toggleMic, toggleCamera, hangup } =
    useCall()

  return (
    <div className="fixed inset-0 z-50 flex flex-col bg-slate-900">
      {/* Távoli videó (teljes méret); amíg nincs kép, a hívott neve/avatárja látszik. */}
      <div className="relative flex-1 overflow-hidden">
        {remoteStream ? (
          <VideoView stream={remoteStream} className="h-full w-full bg-black object-cover" />
        ) : (
          <div className="flex h-full w-full flex-col items-center justify-center gap-4 text-white">
            <BigAvatar peer={peer} />
            <p className="text-sm text-slate-300">{t('call.connecting')}</p>
          </div>
        )}

        {/* A hívott neve felül */}
        <div className="absolute left-0 right-0 top-0 bg-gradient-to-b from-black/50 to-transparent p-4">
          <p className="text-center font-medium text-white">{peer?.name}</p>
        </div>

        {/* Saját kép kicsiben, jobb alul */}
        <div className="absolute bottom-4 right-4 h-36 w-28 overflow-hidden rounded-xl border-2 border-white/30 bg-black shadow-lg sm:h-44 sm:w-32">
          {cameraOn ? (
            <VideoView stream={localStream} muted className="h-full w-full object-cover" />
          ) : (
            <div className="flex h-full w-full items-center justify-center text-white/60">
              <VideoOff className="h-6 w-6" />
            </div>
          )}
        </div>
      </div>

      {/* Vezérlők */}
      <div className="flex items-center justify-center gap-6 bg-slate-900 py-6">
        <CircleButton
          color={micOn ? 'slate' : 'rose'}
          label={micOn ? t('call.muteMic') : t('call.unmuteMic')}
          onClick={toggleMic}
        >
          {micOn ? <Mic className="h-6 w-6" /> : <MicOff className="h-6 w-6" />}
        </CircleButton>
        <CircleButton color="rose" label={t('call.hangup')} onClick={hangup}>
          <PhoneOff className="h-6 w-6" />
        </CircleButton>
        <CircleButton
          color={cameraOn ? 'slate' : 'rose'}
          label={cameraOn ? t('call.stopCamera') : t('call.startCamera')}
          onClick={toggleCamera}
        >
          {cameraOn ? <Video className="h-6 w-6" /> : <VideoOff className="h-6 w-6" />}
        </CircleButton>
      </div>
    </div>
  )
}

function Backdrop({ children }: { children: React.ReactNode }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
      {children}
    </div>
  )
}

const COLORS: Record<string, string> = {
  emerald: 'bg-emerald-500 hover:bg-emerald-600 text-white',
  rose: 'bg-rose-500 hover:bg-rose-600 text-white',
  slate: 'bg-white/20 hover:bg-white/30 text-white',
}

function CircleButton({
  color,
  label,
  onClick,
  children,
}: {
  color: keyof typeof COLORS | string
  label: string
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      title={label}
      className={`flex h-14 w-14 items-center justify-center rounded-full transition-colors ${COLORS[color] ?? COLORS.slate}`}
    >
      {children}
    </button>
  )
}
