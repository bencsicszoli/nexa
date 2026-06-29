package com.nexa.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexa.security.Totp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A kétlépcsős hitelesítés (2FA/TOTP, #17) végpontok közötti tesztje: setup → enable (a teszt
 * a titokból érvényes TOTP-t generál) → helyreállító kódok; a login challenge tokent ad (nincs
 * token); a /login/2fa jó/rossz kódra reagál; a challenge token védett végponton elutasul; a
 * helyreállító kód egyszer használható; disable után a sima login újra tokent ad; a 2FA nélküli
 * felhasználó loginja változatlan.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TwoFactorFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }

    /** Regisztrál; visszaadja a "Bearer <accessToken>" headert. */
    private String register(String email) throws Exception {
        var res = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","displayName":"User","password":"supersecret"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return "Bearer " + json(res.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void fullTwoFactorLifecycle() throws Exception {
        String email = "tfa@example.com";
        String auth = register(email);

        // 1) Setup — titok + otpauth URI, a 2FA még nem aktív.
        var setupRes = mockMvc.perform(post("/api/auth/2fa/setup").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").isNotEmpty())
                .andExpect(jsonPath("$.otpauthUri").value(org.hamcrest.Matchers.startsWith("otpauth://totp/")))
                .andReturn();
        String secret = json(setupRes.getResponse().getContentAsString()).get("secret").asText();

        // 2) Enable egy érvényes TOTP-vel → 10 helyreállító kód.
        var enableRes = mockMvc.perform(post("/api/auth/2fa/enable").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\"}".formatted(Totp.currentCode(secret))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoveryCodes.length()").value(10))
                .andReturn();
        String recoveryCode = json(enableRes.getResponse().getContentAsString())
                .get("recoveryCodes").get(0).asText();

        // A /auth/me jelzi a bekapcsolt 2FA-t.
        mockMvc.perform(get("/api/auth/me").header("Authorization", auth))
                .andExpect(jsonPath("$.totpEnabled").value(true));

        // 3) Login → nincs token, csak challenge token.
        JsonNode loginBody = json(login(email, "supersecret"));
        assertThat(loginBody.get("twoFactorRequired").asBoolean()).isTrue();
        assertThat(loginBody.has("accessToken")).isFalse();
        String challenge = loginBody.get("challengeToken").asText();

        // 4) Rossz kód → 400.
        String valid = Totp.currentCode(secret);
        String wrong = (valid.charAt(0) == '0' ? "1" : "0") + valid.substring(1);
        mockMvc.perform(post("/api/auth/login/2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challengeToken\":\"%s\",\"code\":\"%s\"}".formatted(challenge, wrong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_2FA_CODE"));

        // 5) Jó kód → token-páros.
        mockMvc.perform(post("/api/auth/login/2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challengeToken\":\"%s\",\"code\":\"%s\"}"
                                .formatted(challenge, Totp.currentCode(secret))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // 6) A challenge token védett végponton nem érvényes access token → 401.
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + challenge))
                .andExpect(status().isUnauthorized());

        // 7) Helyreállító kód: egyszer működik.
        String challenge2 = json(login(email, "supersecret")).get("challengeToken").asText();
        mockMvc.perform(post("/api/auth/login/2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challengeToken\":\"%s\",\"code\":\"%s\"}".formatted(challenge2, recoveryCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // ...másodszor már nem.
        String challenge3 = json(login(email, "supersecret")).get("challengeToken").asText();
        mockMvc.perform(post("/api/auth/login/2fa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challengeToken\":\"%s\",\"code\":\"%s\"}".formatted(challenge3, recoveryCode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_2FA_CODE"));

        // 8) Disable → utána a sima login újra tokent ad.
        mockMvc.perform(post("/api/auth/2fa/disable").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\"}".formatted(Totp.currentCode(secret))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"supersecret\"}".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.twoFactorRequired").doesNotExist());
    }

    @Test
    void userWithoutTwoFactorLogsInNormally() throws Exception {
        register("no-tfa@example.com");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-tfa@example.com\",\"password\":\"supersecret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("no-tfa@example.com"));
    }
}
