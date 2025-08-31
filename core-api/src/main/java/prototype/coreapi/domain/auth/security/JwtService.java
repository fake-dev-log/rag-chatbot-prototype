package prototype.coreapi.domain.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import prototype.coreapi.global.exception.BusinessException;
import prototype.coreapi.global.exception.ErrorCode;

import java.security.Key;
import java.util.*;

/**
 * Service for handling JSON Web Token (JWT) operations.
 * This includes generating, validating, and extracting information from JWTs.
 * It uses HS256 algorithm for signing and supports both access and refresh tokens.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.issuer}")
    private String ISSUER;

    @Value("${jwt.access-expiration}") // e.g., 2 hours
    private long ACCESS_TOKEN_EXPIRATION;

    @Value("${jwt.refresh-expiration}") // e.g., 7 days
    private long REFRESH_TOKEN_EXPIRATION;

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ===== Issue =====
    /**
     * Generates an access token for a given member.
     * The token includes member ID as subject and roles as claims.
     * @param memberId The ID of the member.
     * @param authorities The collection of granted authorities (roles) for the member.
     * @return The generated JWT access token string.
     */
    public String generateAccessToken(Long memberId, Collection<? extends GrantedAuthority> authorities) {
        
        Map<String, Object> claims = new HashMap<>();
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        claims.put("roles", roles);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(memberId.toString())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setIssuer(ISSUER)
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generates a refresh token for a given user ID.
     * @param userId The ID of the user.
     * @return The generated JWT refresh token string.
     */
    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuer(ISSUER)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ===== Verify =====
    /**
     * Validates the authenticity and expiration of a JWT.
     * @param token The JWT string to validate.
     * @return true if the token is valid and not expired, false otherwise.
     */
    public boolean isValidToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !isExpiredToken(claims);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isExpiredToken(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    // ===== Extract Info =====
    /**
     * Extracts all claims from a JWT.
     * @param token The JWT string.
     * @return The Claims object containing all parsed claims.
     * @throws BusinessException if the token signature is invalid.
     */
    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (SignatureException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * Extracts the member ID (subject) from a JWT.
     * @param token The JWT string.
     * @return The member ID as a Long.
     */
    public Long extractMemberId(String token) {
        String sub = extractAllClaims(token).getSubject();
        return Long.parseLong(sub);
    }

    /**
     * Extracts the roles (authorities) from a JWT.
     * @param token The JWT string.
     * @return A List of role strings.
     */
    public List<String> extractRoles(String token) {
        Object rolesObject = extractAllClaims(token).get("roles");
        if (rolesObject instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    /**
     * Retrieves the expiration date from a JWT.
     * @param token The JWT string.
     * @return The expiration Date of the token.
     */
    public Date getExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    /**
     * Calculates the remaining validity time of a JWT in milliseconds.
     * @param token The JWT string.
     * @return The remaining validity in milliseconds.
     */
    public long getRemainingValidity(String token) {
        return getExpiration(token).getTime() - System.currentTimeMillis();
    }
}