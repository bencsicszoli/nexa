import { apiFetch } from '../lib/api'
import type { SearchResults } from './types'

// A backend /api/search végpontja (lásd com.nexa.search.SearchController).

/** Egyesített keresés felhasználókra, csoportokra és bejegyzésekre egy kifejezésre. */
export function search(query: string): Promise<SearchResults> {
  return apiFetch<SearchResults>(`/search?q=${encodeURIComponent(query)}`)
}
