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

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.issuer}")
    private String ISSUER;

    @Value("${jwt.access-expiration}") // 예: 2시간
    private long ACCESS_TOKEN_EXPIRATION;

    @Value("${jwt.refresh-expiration}") // 예: 7일
    private long REFRESH_TOKEN_EXPIRATION;

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ===== 발급 =====
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

    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuer(ISSUER)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ===== 검증 =====
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

    // ===== 정보 추출 =====
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

    public Long extractMemberId(String token) {
        String sub = extractAllClaims(token).getSubject();
        return Long.parseLong(sub);
    }

    public List<String> extractRoles(String token) {
        Object rolesObject = extractAllClaims(token).get("roles");
        if (rolesObject instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    public Date getExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public long getRemainingValidity(String token) {
        return getExpiration(token).getTime() - System.currentTimeMillis();
    }
}