package com.dongyao.myblog.gateway.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class GatewayTokenVerifierTest {
    @Test
    void acceptsTokenSignedWithSharedSecret() {
        GatewayTokenVerifier verifier = new GatewayTokenVerifier("test-secret");

        assertThat(verifier.isValid(token("1:commander:" + Instant.now().getEpochSecond(), "test-secret"))).isTrue();
    }

    @Test
    void rejectsTamperedToken() {
        GatewayTokenVerifier verifier = new GatewayTokenVerifier("test-secret");

        assertThat(verifier.isValid("myblog.bad.signature")).isFalse();
    }

    private static String token(String payload, String secret) {
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return "myblog." + encodedPayload + "." + sign(encodedPayload, secret);
    }

    private static String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
