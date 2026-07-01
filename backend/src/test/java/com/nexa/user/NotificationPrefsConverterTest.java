package com.nexa.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A {@link NotificationPrefsConverter} egységtesztje — különös tekintettel arra, hogy egy
 * korábban (a {@code groupJoinRequest} típus bevezetése előtt) mentett, hiányos JSON is a
 * bekapcsolt alapértelmezést kapja az új mezőnél, ne csendben kikapcsolt állapotot.
 */
class NotificationPrefsConverterTest {

    private final NotificationPrefsConverter converter = new NotificationPrefsConverter();

    @Test
    void legacyJsonWithoutNewFieldDefaultsToEnabled() {
        String legacyJson = """
                {"newPost":false,"friendRequest":true,"friendAccepted":true,"newFollower":false}""";

        NotificationPrefs prefs = converter.convertToEntityAttribute(legacyJson);

        assertThat(prefs.newPost()).isFalse();
        assertThat(prefs.friendRequest()).isTrue();
        assertThat(prefs.friendAccepted()).isTrue();
        assertThat(prefs.newFollower()).isFalse();
        assertThat(prefs.groupJoinRequest()).isTrue();
    }

    @Test
    void roundTripPreservesExplicitFalse() {
        NotificationPrefs prefs = new NotificationPrefs(true, true, true, true, false);
        String json = converter.convertToDatabaseColumn(prefs);

        NotificationPrefs restored = converter.convertToEntityAttribute(json);

        assertThat(restored.groupJoinRequest()).isFalse();
    }

    @Test
    void blankColumnReturnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("")).isNull();
    }

    @Test
    void corruptJsonFallsBackToDefaults() {
        NotificationPrefs prefs = converter.convertToEntityAttribute("{not-json");
        assertThat(prefs).isEqualTo(NotificationPrefs.defaults());
    }
}
