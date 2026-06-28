import { apiFetch } from '../lib/api'

/** A backend által adott ICE-szerver lista (GET /api/calls/ice-servers, lásd CallController). */
type IceServersResponse = { iceServers: RTCIceServer[] }

/** A RTCPeerConnection-höz használandó STUN/TURN szerverek lekérése. */
export async function getIceServers(): Promise<RTCIceServer[]> {
  const { iceServers } = await apiFetch<IceServersResponse>('/calls/ice-servers')
  return iceServers
}
