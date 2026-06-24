package com.nexa.storage;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * Tárolóbeli objektumok törlése a DB-tranzakció <b>commitja után</b>. Így ha a
 * tranzakció visszagördül, nem törlünk ki még élő (hivatkozott) fájlt. A {@link StorageService}
 * törlése amúgy is best-effort, ezért a commit utáni hívás nem boríthatja a kérést.
 *
 * <p>Ha épp nincs aktív tranzakció (pl. teszt vagy nem tranzakciós hívó), a törlés azonnal megtörténik.
 */
@Component
public class DeferredStorageDeleter {

    private final StorageService storageService;

    public DeferredStorageDeleter(StorageService storageService) {
        this.storageService = storageService;
    }

    /** A megadott kulcsú objektumok törlésének ütemezése a commit utánra. {@code null} kulcsok kihagyva. */
    public void deleteAfterCommit(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        List<String> toDelete = keys.stream().filter(k -> k != null && !k.isBlank()).toList();
        if (toDelete.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    toDelete.forEach(storageService::delete);
                }
            });
        } else {
            toDelete.forEach(storageService::delete);
        }
    }
}
