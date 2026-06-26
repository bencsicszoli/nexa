package com.nexa.feed.dto;

import com.nexa.post.dto.PostDto;

import java.util.List;

/**
 * A hírfolyam egy lapja (#10): a bejegyzések időrendben (legfrissebb felül) és egy
 * átlátszatlan {@code nextCursor}, amellyel a következő lap kérhető. A {@code nextCursor}
 * {@code null}, ha nincs több bejegyzés.
 */
public record FeedPageDto(List<PostDto> items, String nextCursor) {
}
