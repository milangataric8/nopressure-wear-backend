package rs.nopressurewear.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import rs.nopressurewear.model.User;

import javax.crypto.SecretKey;
import java.util.Date;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class JwtUtil {

    private static final String TOKEN_VERSION_CLAIM = "tv";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(UTF_8));
    }

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", userDetails.getAuthorities())
                .claim(TOKEN_VERSION_CLAIM, tokenVersionOf(userDetails))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Username, expiry and — for a {@link User} — the token-version claim must all match the
     * account's current state. A role change bumps {@code User.tokenVersion}, which makes every
     * JWT issued before that bump fail here, even though it hasn't expired yet.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        if (!username.equals(userDetails.getUsername()) || isTokenExpired(token)) {
            return false;
        }
        if (userDetails instanceof User user) {
            Long tokenVersion = extractTokenVersion(token);
            return tokenVersion != null && tokenVersion == user.getTokenVersion();
        }
        return true;
    }

    private Long extractTokenVersion(String token) {
        Object claim = extractClaims(token).get(TOKEN_VERSION_CLAIM);
        return claim == null ? null : ((Number) claim).longValue();
    }

    private static long tokenVersionOf(UserDetails userDetails) {
        return userDetails instanceof User user ? user.getTokenVersion() : 0L;
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
