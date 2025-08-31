package prototype.coreapi.domain.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import prototype.coreapi.domain.auth.dto.AuthInfo;
import prototype.coreapi.domain.auth.dto.AuthRequest;
import prototype.coreapi.domain.auth.dto.AuthResponse;
import prototype.coreapi.domain.auth.dto.SignInPrincipal;
import prototype.coreapi.domain.member.MemberService;
import prototype.coreapi.domain.member.dto.MemberRequest;
import prototype.coreapi.domain.member.dto.MemberResponse;
import prototype.coreapi.domain.member.mapper.MemberMapper;
import prototype.coreapi.global.exception.BusinessException;
import prototype.coreapi.global.exception.ErrorCode;
import prototype.coreapi.global.response.RestResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * REST controller for authentication-related operations.
 * Handles user sign-in, sign-up, token refreshing, and sign-out.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs related to authentication")
public class AuthController {

    private final AuthService authService;
    private final MemberService memberService;
    private final MemberMapper memberMapper;

    /**
     * Handles user sign-in. Authenticates the user and issues JWT tokens.
     * The refresh token is set as an HTTP-only cookie.
     * @param request The authentication request containing user credentials.
     * @return A Mono emitting a ResponseEntity with AuthResponse containing access token and user details.
     */
    @PostMapping("/sign-in")
    @Operation(summary = "Sign-in", description = "Delivers authentication information including tokens.")
    public Mono<ResponseEntity<AuthResponse>> signIn(
            @Valid @RequestBody AuthRequest request
    ) {
        
        return authService.signIn(request)
                .map(authInfo -> {
                    Map<String, String> cookieHeaders = createSetRefreshTokenCookieHeader(
                            authInfo.refreshToken(), authInfo.refreshTokenTtl()
                    );
                    AuthResponse body = createAuthResponseFromAuthInfo(authInfo);
                    return RestResponse.ok(cookieHeaders, body);
                });
    }
    /**
     * Handles user sign-up. Creates a new member account.
     * @param request The member request containing new user details.
     * @return A Mono emitting a ResponseEntity with MemberResponse of the newly created member.
     */
    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Sign Up", description = "Creates a new member.")
    public Mono<ResponseEntity<MemberResponse>> signUp(
            @Valid @RequestBody MemberRequest request
    ) {
        
        return memberService.createMember(request)
                .map(memberMapper::toResponse)
                .map(RestResponse::created);
    }

    /**
     * Refreshes the access token using a valid refresh token provided in a cookie.
     * @param refreshToken The refresh token from the cookie.
     * @return A Mono emitting a ResponseEntity with AuthResponse containing a new access token and user details.
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refreshAccessToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        
        if (refreshToken == null) {
            return Mono.error(new BusinessException(ErrorCode.AUTH_FAILED));
        }
        return authService.refreshAccessToken(refreshToken)
                .map(authInfo -> {
                    Map<String, String> cookieHeaders = createSetRefreshTokenCookieHeader(
                            authInfo.refreshToken(), authInfo.refreshTokenTtl()
                    );
                    AuthResponse body = createAuthResponseFromAuthInfo(authInfo);
                    return RestResponse.ok(cookieHeaders, body);
                });
    }

    /**
     * Handles user sign-out. Invalidates the current access and refresh tokens.
     * @param authHeader The Authorization header containing the access token.
     * @param principal The authenticated user's principal.
     * @return A Mono that completes when the sign-out process is finished.
     */
    @PostMapping("/sign-out")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @AuthenticationPrincipal SignInPrincipal principal
    ) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.error(new BusinessException(ErrorCode.AUTH_FAILED));
        }
        String accessToken = authHeader.substring(7);
        return authService.signOut(accessToken, principal.memberId());
    }

    // —————————————————————— helper methods ——————————————————————

    /**
     * Creates an HTTP-only cookie for the refresh token.
     * @param refreshToken The refresh token string.
     * @param ttl The time-to-live for the cookie.
     * @return A Map containing the Set-Cookie header.
     */
    private Map<String, String> createSetRefreshTokenCookieHeader(
            String refreshToken, Duration ttl
    ) {
        
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/auth/refresh")
                .maxAge(ttl)
                .sameSite("Strict")
                .build();
        return Map.of(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private AuthResponse createAuthResponseFromAuthInfo(AuthInfo authInfo) {
        return AuthResponse.builder()
                .accessToken(authInfo.accessToken())
                .id(authInfo.id())
                .email(authInfo.email())
                .role(authInfo.role())
                .lastSignInAt(authInfo.lastSignInAt())
                .build();
    }
}
