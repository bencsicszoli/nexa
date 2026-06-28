import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import { useAuth } from '../auth/AuthContext'
import { useStomp } from '../realtime/StompProvider'
import { getIceServers } from './callApi'
import type { CallPeer, CallSignal, CallSignalType, CallStatus } from './types'

/**
 * Az 1:1 videohívás (#13) kliensoldali állapotgépe a megosztott STOMP-kapcsolaton ({@link useStomp}).
 * A jelzés (SDP/ICE) a {@code /app/call.signal} célra megy, a beérkező jelzés a {@code /user/queue/call}
 * címen jön — a backend csak relézi a hívás két résztvevője között ({@code CallService}).
 *
 * A WebRTC-folyam: a hívó {@code OFFER}-t küld (saját média + ajánlat), a hívott {@code ANSWER}-rel
 * felel elfogadáskor, közben mindkét fél {@code ICE}-jelölteket cserél. A bontás/elutasítás/visszavonás
 * a {@code HANGUP}/{@code REJECT}/{@code CANCEL} keretekkel; ha a hívott épp foglalt, {@code BUSY} megy
 * vissza. A {@code RTCPeerConnection} és a média-streamek refekben élnek; a UI-hoz a streameket és a
 * státuszt state-ben tükrözzük.
 */
type CallContextValue = {
  status: CallStatus
  peer: CallPeer | null
  localStream: MediaStream | null
  remoteStream: MediaStream | null
  micOn: boolean
  cameraOn: boolean
  /** Hívás indítása egy kétszemélyes szálon (a hívott felé). */
  startCall: (conversationId: string, peer: CallPeer) => Promise<void>
  /** Bejövő hívás elfogadása. */
  acceptCall: () => Promise<void>
  /** Bejövő hívás elutasítása. */
  rejectCall: () => void
  /** Saját oldali bontás/visszavonás (állapottól függően HANGUP vagy CANCEL). */
  hangup: () => void
  toggleMic: () => void
  toggleCamera: () => void
}

const CallContext = createContext<CallContextValue | null>(null)

/** A getUserMedia kérése: kamera + mikrofon. */
const MEDIA_CONSTRAINTS: MediaStreamConstraints = { video: true, audio: true }

export function CallProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const meId = user?.id ?? null
  const { subscribe, publish } = useStomp()

  const [status, setStatus] = useState<CallStatus>('idle')
  const [peer, setPeer] = useState<CallPeer | null>(null)
  const [localStream, setLocalStream] = useState<MediaStream | null>(null)
  const [remoteStream, setRemoteStream] = useState<MediaStream | null>(null)
  const [micOn, setMicOn] = useState(true)
  const [cameraOn, setCameraOn] = useState(true)

  // Refek, hogy a STOMP-handler és az aszinkron lépések a friss értékeket lássák.
  const statusRef = useRef<CallStatus>('idle')
  statusRef.current = status
  const convIdRef = useRef<string | null>(null)
  const pcRef = useRef<RTCPeerConnection | null>(null)
  const localStreamRef = useRef<MediaStream | null>(null)
  const pendingCandidatesRef = useRef<RTCIceCandidateInit[]>([])
  const incomingOfferRef = useRef<RTCSessionDescriptionInit | null>(null)
  const iceServersRef = useRef<RTCIceServer[] | null>(null)

  /** Jelzés küldése az aktuális hívás szálán. */
  const signal = useCallback(
    (type: CallSignalType, extra?: { sdp?: RTCSessionDescriptionInit; candidate?: RTCIceCandidateInit }) => {
      const conversationId = convIdRef.current
      if (!conversationId) return
      publish('/app/call.signal', {
        conversationId,
        type,
        sdp: extra?.sdp ?? null,
        candidate: extra?.candidate ?? null,
      })
    },
    [publish],
  )

  const cleanup = useCallback(() => {
    if (pcRef.current) {
      pcRef.current.onicecandidate = null
      pcRef.current.ontrack = null
      pcRef.current.onconnectionstatechange = null
      try {
        pcRef.current.close()
      } catch {
        /* a kapcsolat bonthatott — lényegtelen */
      }
      pcRef.current = null
    }
    if (localStreamRef.current) {
      localStreamRef.current.getTracks().forEach((track) => track.stop())
      localStreamRef.current = null
    }
    pendingCandidatesRef.current = []
    incomingOfferRef.current = null
    convIdRef.current = null
    setLocalStream(null)
    setRemoteStream(null)
    setPeer(null)
    setMicOn(true)
    setCameraOn(true)
    setStatus('idle')
  }, [])

  const ensureIceServers = useCallback(async () => {
    if (!iceServersRef.current) {
      try {
        iceServersRef.current = await getIceServers()
      } catch {
        // Tartalék: ha a config-lekérés elhasal, a nyilvános STUN-nal még felépülhet a hívás.
        iceServersRef.current = [{ urls: 'stun:stun.l.google.com:19302' }]
      }
    }
    return iceServersRef.current
  }, [])

  /** Lekéri a médiát és felépíti a peer-kapcsolatot a közös eseménykezelőkkel. */
  const setupPeer = useCallback(async () => {
    const iceServers = await ensureIceServers()
    const stream = await navigator.mediaDevices.getUserMedia(MEDIA_CONSTRAINTS)
    localStreamRef.current = stream
    setLocalStream(stream)
    setMicOn(true)
    setCameraOn(true)

    const pc = new RTCPeerConnection({ iceServers })
    stream.getTracks().forEach((track) => pc.addTrack(track, stream))

    pc.onicecandidate = (event) => {
      if (event.candidate) {
        signal('ICE', { candidate: event.candidate.toJSON() })
      }
    }
    pc.ontrack = (event) => {
      setRemoteStream(event.streams[0] ?? null)
    }
    pc.onconnectionstatechange = () => {
      const state = pc.connectionState
      if (state === 'connected') {
        setStatus('active')
      } else if (state === 'failed' || state === 'closed') {
        cleanup()
      }
    }

    pcRef.current = pc
    return pc
  }, [ensureIceServers, signal, cleanup])

  /** A korai (a remote leírás előtt érkezett) ICE-jelöltek beadása. */
  const flushPendingCandidates = useCallback(async () => {
    const pc = pcRef.current
    if (!pc) return
    const pending = pendingCandidatesRef.current
    pendingCandidatesRef.current = []
    for (const candidate of pending) {
      try {
        await pc.addIceCandidate(new RTCIceCandidate(candidate))
      } catch {
        /* elavult jelölt — kihagyjuk */
      }
    }
  }, [])

  const startCall = useCallback(
    async (conversationId: string, target: CallPeer) => {
      if (statusRef.current !== 'idle') return
      convIdRef.current = conversationId
      setPeer(target)
      setStatus('outgoing')
      try {
        const pc = await setupPeer()
        const offer = await pc.createOffer()
        await pc.setLocalDescription(offer)
        signal('OFFER', { sdp: offer })
      } catch {
        // Pl. a felhasználó megtagadta a kamera/mikrofon hozzáférést.
        signal('CANCEL')
        cleanup()
      }
    },
    [setupPeer, signal, cleanup],
  )

  const acceptCall = useCallback(async () => {
    const offer = incomingOfferRef.current
    if (statusRef.current !== 'incoming' || !offer) return
    try {
      const pc = await setupPeer()
      await pc.setRemoteDescription(new RTCSessionDescription(offer))
      await flushPendingCandidates()
      const answer = await pc.createAnswer()
      await pc.setLocalDescription(answer)
      signal('ANSWER', { sdp: answer })
      setStatus('active')
    } catch {
      signal('REJECT')
      cleanup()
    }
  }, [setupPeer, flushPendingCandidates, signal, cleanup])

  const rejectCall = useCallback(() => {
    signal('REJECT')
    cleanup()
  }, [signal, cleanup])

  const hangup = useCallback(() => {
    // Élő hívás → HANGUP; még meg nem válaszolt kimenő hívás → CANCEL.
    signal(statusRef.current === 'outgoing' ? 'CANCEL' : 'HANGUP')
    cleanup()
  }, [signal, cleanup])

  const toggleMic = useCallback(() => {
    const stream = localStreamRef.current
    if (!stream) return
    const next = !stream.getAudioTracks().every((t) => t.enabled)
    stream.getAudioTracks().forEach((t) => (t.enabled = next))
    setMicOn(next)
  }, [])

  const toggleCamera = useCallback(() => {
    const stream = localStreamRef.current
    if (!stream) return
    const next = !stream.getVideoTracks().every((t) => t.enabled)
    stream.getVideoTracks().forEach((t) => (t.enabled = next))
    setCameraOn(next)
  }, [])

  // --- Beérkező jelzések kezelése ---

  const handleSignal = useCallback(
    async (sig: CallSignal) => {
      switch (sig.type) {
        case 'OFFER': {
          // Foglalt vagyunk (másik hívásban) → BUSY a hívónak, a saját hívásunk érintetlen.
          if (statusRef.current !== 'idle') {
            publish('/app/call.signal', {
              conversationId: sig.conversationId,
              type: 'BUSY',
              sdp: null,
              candidate: null,
            })
            return
          }
          convIdRef.current = sig.conversationId
          incomingOfferRef.current = sig.sdp
          setPeer({ userId: sig.fromUserId, name: sig.fromName, avatarUrl: sig.fromAvatarUrl })
          setStatus('incoming')
          break
        }
        case 'ANSWER': {
          const pc = pcRef.current
          if (!pc || !sig.sdp) return
          try {
            await pc.setRemoteDescription(new RTCSessionDescription(sig.sdp))
            await flushPendingCandidates()
            setStatus('active')
          } catch {
            cleanup()
          }
          break
        }
        case 'ICE': {
          const pc = pcRef.current
          if (!sig.candidate) return
          if (pc && pc.remoteDescription) {
            try {
              await pc.addIceCandidate(new RTCIceCandidate(sig.candidate))
            } catch {
              /* elavult jelölt */
            }
          } else {
            // A remote leírás még nincs beállítva — pufferelünk, és elfogadáskor beadjuk.
            pendingCandidatesRef.current.push(sig.candidate)
          }
          break
        }
        case 'HANGUP':
        case 'REJECT':
        case 'CANCEL':
        case 'BUSY': {
          // A másik fél lezárta/elutasította/visszavonta a hívást, vagy foglalt.
          if (statusRef.current !== 'idle') cleanup()
          break
        }
      }
    },
    [publish, flushPendingCandidates, cleanup],
  )

  // A handler refen át, hogy a feliratkozást ne kelljen újraépíteni minden render-nél.
  const handlerRef = useRef(handleSignal)
  handlerRef.current = handleSignal

  useEffect(() => {
    if (!meId) return
    const unsub = subscribe('/user/queue/call', (frame) => {
      try {
        void handlerRef.current(JSON.parse(frame.body) as CallSignal)
      } catch {
        /* hibás keret — kihagyjuk */
      }
    })
    return () => {
      unsub()
    }
  }, [meId, subscribe])

  // Kijelentkezéskor / unmountkor a folyamatban lévő hívás takarítása.
  useEffect(() => {
    if (!meId && statusRef.current !== 'idle') cleanup()
  }, [meId, cleanup])

  const value = useMemo<CallContextValue>(
    () => ({
      status,
      peer,
      localStream,
      remoteStream,
      micOn,
      cameraOn,
      startCall,
      acceptCall,
      rejectCall,
      hangup,
      toggleMic,
      toggleCamera,
    }),
    [
      status,
      peer,
      localStream,
      remoteStream,
      micOn,
      cameraOn,
      startCall,
      acceptCall,
      rejectCall,
      hangup,
      toggleMic,
      toggleCamera,
    ],
  )

  return <CallContext.Provider value={value}>{children}</CallContext.Provider>
}

export function useCall(): CallContextValue {
  const ctx = useContext(CallContext)
  if (!ctx) {
    throw new Error('useCall csak CallProvider-en belül használható')
  }
  return ctx
}
