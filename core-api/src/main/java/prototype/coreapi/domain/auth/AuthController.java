package prototype.coreapi.domain.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import prototype.coreapi.domain.auth.dto.LoginPrincipal;
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

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;
    private final MemberService memberService;
    private final MemberMapper memberMapper;

    @PostMapping("/sign-in")
    @Operation(summary = "로그인", description = "토큰을 비롯한 인증 정보를 전달합니다.")
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

    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "회원 가입", description = "새로운 회원을 생성합니다.")
    public Mono<ResponseEntity<MemberResponse>> signUp(
            @Valid @RequestBody MemberRequest request
    ) {
        return memberService.createMember(request)
                .map(memberMapper::toResponse)
                .map(RestResponse::created);
    }

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

    @PostMapping("/sign-out")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @AuthenticationPrincipal LoginPrincipal principal
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.error(new BusinessException(ErrorCode.AUTH_FAILED));
        }
        String accessToken = authHeader.substring(7);
        return Mono.fromRunnable(() ->
                authService.signOut(accessToken, principal.memberId())
        );
    }

    // —————————————————————— helper methods ——————————————————————

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
                .lastLoginAt(authInfo.lastLoginAt())
                .build();
    }
}
