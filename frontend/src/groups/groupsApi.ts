import { apiFetch } from '../lib/api'
import type { PostMediaInput } from '../posts/postApi'
import type { Post } from '../posts/types'
import type { Group, GroupMember } from './types'

// A backend /api/groups végpontjai (lásd com.nexa.group.GroupController).

/**
 * A felhasználó csoportjai megváltoztak (létrehozás / csatlakozás / kilépés).
 * A bal navigáció „Csoportjaim" listája erre az eseményre frissül — ugyanaz a minta,
 * mint az AUTH_LOGOUT_EVENT (lásd lib/api).
 */
export const GROUPS_CHANGED_EVENT = 'nexa:groups-changed'

function emitGroupsChanged(): void {
  window.dispatchEvent(new Event(GROUPS_CHANGED_EVENT))
}

/** Csoportok böngészése a hívó tagsági szerepével (csatlakozáshoz). */
export function browseGroups(query: string): Promise<Group[]> {
  const q = query.trim()
  const suffix = q ? `?query=${encodeURIComponent(q)}` : ''
  return apiFetch<Group[]>(`/groups${suffix}`)
}

/** A bejelentkezett felhasználó csoportjai. */
export function getMyGroups(): Promise<Group[]> {
  return apiFetch<Group[]>('/groups/mine')
}

/** Egy csoport részletei. */
export function getGroup(groupId: string): Promise<Group> {
  return apiFetch<Group>(`/groups/${groupId}`)
}

/** Egy csoport tagjai (adminok elöl). */
export function getGroupMembers(groupId: string): Promise<GroupMember[]> {
  return apiFetch<GroupMember[]>(`/groups/${groupId}/members`)
}

/** Új csoport létrehozása (a létrehozó admin lesz). */
export async function createGroup(name: string, description: string): Promise<Group> {
  const group = await apiFetch<Group>('/groups', {
    method: 'POST',
    body: { name, description },
  })
  emitGroupsChanged()
  return group
}

/** Csatlakozás egy csoporthoz (idempotens). */
export async function joinGroup(groupId: string): Promise<Group> {
  const group = await apiFetch<Group>(`/groups/${groupId}/join`, { method: 'POST' })
  emitGroupsChanged()
  return group
}

/** Kilépés egy csoportból (idempotens; az utolsó admin nem léphet ki). */
export async function leaveGroup(groupId: string): Promise<Group> {
  const group = await apiFetch<Group>(`/groups/${groupId}/leave`, { method: 'POST' })
  emitGroupsChanged()
  return group
}

/** Egy csoport bejegyzései, legfrissebb felül. */
export function getGroupPosts(groupId: string): Promise<Post[]> {
  return apiFetch<Post[]>(`/groups/${groupId}/posts`)
}

/** Bejegyzés írása a csoportba (csak tag). */
export function createGroupPost(
  groupId: string,
  content: string,
  media: PostMediaInput[] = [],
): Promise<Post> {
  return apiFetch<Post>(`/groups/${groupId}/posts`, {
    method: 'POST',
    body: { content, media },
  })
}
