package prototype.coreapi.domain.auth;


import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import prototype.coreapi.domain.auth.dto.AuthInfo;
import prototype.coreapi.domain.auth.dto.AuthRequest;
import prototype.coreapi.domain.auth.dto.AuthTokens;
import prototype.coreapi.domain.auth.security.JwtService;
import prototype.coreapi.domain.member.MemberService;
import prototype.coreapi.domain.member.entity.Member;
import prototype.coreapi.global.exception.BusinessException;
import prototype.coreapi.global.exception.ErrorCode;
import prototype.coreapi.global.redis.AccessTokenStoreProvider;
import prototype.coreapi.global.redis.RefreshTokenStoreProvider;
import prototype.coreapi.global.redis.TokenBlacklistStoreProvider;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ReactiveAuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final MemberService memberService;

    private final AccessTokenStoreProvider accessTokenStoreProvider;
    private final RefreshTokenStoreProvider refreshTokenStoreProvider;
    private final TokenBlacklistStoreProvider tokenBlacklistStoreProvider;

    public Mono<AuthInfo> signIn(AuthRequest request) {
        var token = new UsernamePasswordAuthenticationToken(
                request.getEmail(), request.getPassword()
        );

        return authenticationManager.authenticate(token)
                .cast(UsernamePasswordAuthenticationToken.class)
                .flatMap(auth -> {
                    Member member = (Member) auth.getPrincipal();
                    return validateUser(member)
                            .doOnSuccess(Member::updateLastLoginAt)
                            .map(validMember -> {
                                AuthTokens tokens = createAndStoreAuthTokens(validMember);
                                return createAuthInfoFromAuthTokensAndMember(tokens, validMember);
                            });
                })
                .onErrorMap(BadCredentialsException.class,
                        ex -> new BusinessException(ErrorCode.WRONG_PASSWORD))
                .onErrorMap(UsernameNotFoundException.class,
                        ex -> new BusinessException(ErrorCode.WRONG_USERNAME))
                .onErrorMap(AuthenticationException.class,
                        ex -> new BusinessException(ErrorCode.LOGIN_FAILED));
    }

    public Mono<AuthInfo> refreshAccessToken(String requestRefreshToken) {
        if (!jwtService.isValidToken(requestRefreshToken)) {
            return Mono.error(new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN));
        }
        Long userId = jwtService.extractMemberId(requestRefreshToken);

        return Mono.fromCallable(() ->
                        refreshTokenStoreProvider.get(userId)
                                .orElseThrow(() -> new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN))
                )
                .flatMap(stored -> {
                    if (!stored.equals(requestRefreshToken)) {
                        return Mono.error(new BusinessException(ErrorCode.INVALID_TOKEN));
                    }
                    return memberService.findById(userId);
                })
                .flatMap(member -> validateUser(member)
                        .map(validMember -> {
                            AuthTokens tokens = createAndStoreAuthTokens(validMember);
                            return createAuthInfoFromAuthTokensAndMember(tokens, validMember);
                        })
                )
                .doOnError(BusinessException.class, ex -> {
                    // 사용 불가 사용자면 리프레시 토큰 삭제
                    ErrorCode code = ex.getErrorCode();
                    if (code == ErrorCode.NOT_FOUND_USER
                            || code == ErrorCode.DELETED_USER
                            || code == ErrorCode.SUSPENDED_USER
                            || code == ErrorCode.INACTIVE_USER) {
                        refreshTokenStoreProvider.delete(userId);
                    }
                });
    }

    public void signOut(String accessToken, Long userId) {
        refreshTokenStoreProvider.delete(userId);
        long ttlMillis = jwtService.getRemainingValidity(accessToken);
        if (ttlMillis >= 1000) {
            tokenBlacklistStoreProvider.blacklistHashed(
                    accessToken, Duration.ofMillis(ttlMillis)
            );
        }
    }

    private Mono<Member> validateUser(Member member) {
        if (member == null) {
            return Mono.error(new BusinessException(ErrorCode.NOT_FOUND_USER));
        }
        if (!member.isEnabled()) {
            return Mono.error(new BusinessException(ErrorCode.DELETED_USER));
        }
        if (!member.isAccountNonLocked()) {
            return Mono.error(new BusinessException(ErrorCode.SUSPENDED_USER));
        }
        if (!member.isAccountNonExpired()) {
            return Mono.error(new BusinessException(ErrorCode.INACTIVE_USER));
        }
        return Mono.just(member);
    }

    private AuthTokens createAndStoreAuthTokens(Member member) {
        String accessToken = jwtService.generateAccessToken(member.getId(), member.getAuthorities());
        String refreshToken = jwtService.generateRefreshToken(member.getId());

        Duration accessTtl = Duration.ofMillis(jwtService.getRemainingValidity(accessToken));
        accessTokenStoreProvider.save(member.getId(), accessToken, accessTtl);

        Duration refreshTtl = Duration.ofMillis(jwtService.getRemainingValidity(refreshToken));
        refreshTokenStoreProvider.save(member.getId(), refreshToken, refreshTtl);

        return new AuthTokens(accessToken, refreshToken, refreshTtl);
    }

    private AuthInfo createAuthInfoFromAuthTokensAndMember(
            AuthTokens tokens, Member member
    ) {
        return new AuthInfo(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.refreshTokenTtl(),
                member.getId(),
                member.getEmail(),
                member.getRole(),
                member.getLastLoginAt()
        );
    }
}