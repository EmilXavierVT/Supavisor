package app.services.routeSecurity;

import app.dto.UserDTO;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenSecurityTest {
    private static final String ISSUER = "test-issuer";
    private static final String SECRET_KEY = "bb0114afae7843bedc5e5b7fcec3e0e95018b967a11b78c6ecc3e473896de602";

    private final TokenSecurity tokenSecurity = new TokenSecurity();

    @Test
    void tokenExpiredWithinReturnsTrueForRecentlyExpiredToken() throws Exception {
        String token = createToken(-1_000);

        assertTrue(tokenSecurity.tokenExpiredWithin(token, 1_800_000));
    }

    @Test
    void tokenExpiredWithinReturnsFalseForTokenExpiredOutsideGraceWindow() throws Exception {
        String token = createToken(-1_900_000);

        assertFalse(tokenSecurity.tokenExpiredWithin(token, 1_800_000));
    }

    @Test
    void tokenExpiredWithinReturnsFalseForTokenThatHasNotExpired() throws Exception {
        String token = createToken(60_000);

        assertFalse(tokenSecurity.tokenExpiredWithin(token, 1_800_000));
    }

    @Test
    void tenantIdSurvivesTokenRoundTrip() throws Exception {
        UserDTO user = new UserDTO("user@example.com", Set.of("USER"));
        user.setTenantId(7L);
        String token = tokenSecurity.createToken(user, ISSUER, "60000", SECRET_KEY);

        assertEquals(7L, tokenSecurity.getUserWithRolesFromToken(token).getTenantId());
    }

    @Test
    void missingTenantIdParsesAsNull() throws Exception {
        String token = createToken(60_000);

        assertNull(tokenSecurity.getUserWithRolesFromToken(token).getTenantId());
    }

    private String createToken(long tokenExpireTime) throws Exception {
        UserDTO user = new UserDTO("user@example.com", Set.of("USER"));
        return tokenSecurity.createToken(user, ISSUER, String.valueOf(tokenExpireTime), SECRET_KEY);
    }
}
