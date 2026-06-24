package com.nexa.storage;

/**
 * Egy aláírt feltöltési cél: a kliens a {@code uploadUrl}-re PUT-tel tölti fel a
 * bájtokat, majd a {@code key}-jel megerősíti a feltöltést (lásd ProfileController).
 * A frontend-szerződés azonos a lokál és az R2/S3 tárolónál — csak a háttér cserélődik.
 */
public record PresignedUpload(String uploadUrl, String key) {
}
