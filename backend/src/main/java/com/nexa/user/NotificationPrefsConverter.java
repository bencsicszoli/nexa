package com.nexa.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * A {@link NotificationPrefs} JSON-szöveggé alakítása a tároláshoz (#17). Egy {@code text}
 * oszlopba mentünk JSON-t — ez hordozható a PostgreSQL és a teszt-H2 között is. (A {@code JSONB}
 * itt nem ad előnyt, mert a prefekbe sosem kérdezünk SQL-ben; a felhasználót betöltve, Java-ban
 * olvassuk őket.) Olvasáskor a sérült/üres értéket az alapértelmezésre esünk vissza, hogy egy
 * korábbi formátum se akassza meg a bejelentkezést.
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
            return MAPPER.readValue(json, NotificationPrefs.class);
        } catch (Exception e) {
            return NotificationPrefs.defaults();
        }
    }
}
