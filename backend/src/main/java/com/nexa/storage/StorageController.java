package com.nexa.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * A lokál tároló feltöltési végpontja: a kliens ide PUT-tel küldi a nyers bájtokat
 * az aláírt {@code token}-nel. (Az s3 providernél a kliens közvetlenül az R2-be tölt,
 * így ez a végpont csak a {@code local} providernél él.)
 */
@RestController
@RequestMapping("/api/storage")
@ConditionalOnProperty(name = "nexa.storage.provider", havingValue = "local", matchIfMissing = true)
public class StorageController {

    private final LocalStorageService storage;

    public StorageController(LocalStorageService storage) {
        this.storage = storage;
    }

    @PutMapping("/upload")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void upload(
            @RequestParam("token") String token,
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            @RequestBody byte[] body) {
        storage.store(token, contentType, body);
    }
}
