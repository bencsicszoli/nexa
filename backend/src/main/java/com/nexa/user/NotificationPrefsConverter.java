package com.nexa.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * A {@link NotificationPrefs} JSON-szöveggé alakítása a tároláshoz (#17). Egy {@code text}
 * oszlopba mentünk JSON-t — ez hordozható a PostgreSQL és a teszt-H2 között is. (A {@code JSONB}
 * itt nem ad előnyt, mert a prefekbe sosem kérdezünk SQL-ben; a felhasználót betöltve, Java-ban
 * olvassuk őket.) Olvasáskor a sérült/üres értéket az alapértelmezésre esünk vissza, hogy egy
 * korábbi formátum se akassza meg a bejelentkezést.
 *
 * <p>A mezőnkénti olvasás {@link JsonNode#path(String)}-sal (nem a record közvetlen
 * deszerializálásával) történik, hogy egy <b>korábban mentett, még hiányos JSON</b> (mielőtt egy
 * új típus bekerült a recordba) az adott mezőnél is a bekapcsolt alapértelmezést kapja — ne a
 * Java {@code boolean} nulla-értékét ({@code false}), ami csendben kikapcsolná az új típust a
 * régi felhasználóknak.
 */
@Converter
public class NotificationPrefsConverter implements AttributeConverter<NotificationPrefs, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(NotificationPrefs prefs) {
        if (prefs == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(prefs);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize NotificationPrefs", e);
        }
    }

    @Override
    public NotificationPrefs convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            return new NotificationPrefs(
                    node.path("newPost").asBoolean(true),
                    node.path("friendRequest").asBoolean(true),
                    node.path("friendAccepted").asBoolean(true),
                    node.path("newFollower").asBoolean(true),
                    node.path("groupJoinRequest").asBoolean(true));
        } catch (Exception e) {
            return NotificationPrefs.defaults();
        }
    }
}
