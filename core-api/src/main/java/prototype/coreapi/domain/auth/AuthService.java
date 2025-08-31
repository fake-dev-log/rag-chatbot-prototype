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

/**
 * Service responsible for handling user authentication and authorization flows.
 * This includes user sign-in, sign-out, and JWT token management (issuance, refresh, and invalidation).
 * It interacts with member services, JWT services, and Redis for token storage and blacklisting.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final ReactiveAuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final MemberService memberService;

    private final AccessTokenStoreProvider accessTokenStoreProvider;
    private final RefreshTokenStoreProvider refreshTokenStoreProvider;
    private final TokenBlacklistStoreProvider tokenBlacklistStoreProvider;

    /**
     * Authenticates a user and issues new JWT tokens.
     *
     * @param request The user's credentials (email and password).
     * @return A Mono emitting authentication information, including tokens.
     * @throws BusinessException if authentication fails due to wrong credentials or user status.
     */
    public Mono<AuthInfo> signIn(AuthRequest request) {
        var token = new UsernamePasswordAuthenticationToken(
                request.getEmail(), request.getPassword()
        );

        return authenticationManager.authenticate(token)
            .cast(UsernamePasswordAuthenticationToken.class)
            .flatMap(this::processSuccessfulAuthentication)
            .onErrorMap(BadCredentialsException.class,
                    ex -> new BusinessException(ErrorCode.WRONG_PASSWORD))
            .onErrorMap(UsernameNotFoundException.class,
                    ex -> new BusinessException(ErrorCode.WRONG_USERNAME))
            .onErrorMap(AuthenticationException.class,
                    ex -> new BusinessException(ErrorCode.SIGN_IN_FAILED));
    }

    /**
     * Refreshes an access token using a valid refresh token.
     * It validates the provided refresh token against the stored one, fetches the user,
     * validates the user's account status, and then issues and stores new tokens.
     * If user validation fails, the refresh token is deleted to prevent further use.
     *
     * @param requestRefreshToken The refresh token provided by the client.
     * @return A Mono emitting new authentication information.
     * @throws BusinessException if the refresh token is invalid, expired, or the user is not found/invalid.
     */
    public Mono<AuthInfo> refreshAccessToken(String requestRefreshToken) {
        if (!jwtService.isValidToken(requestRefreshToken)) {
            return Mono.error(new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN));
        }
        Long userId = jwtService.extractMemberId(requestRefreshToken);
        // 1. Retrieve the stored refresh token from Redis.
        return refreshTokenStoreProvider.get(userId)
                .flatMap(optionalStored -> {
                    // 2. Validate the provided refresh token against the stored one.
                    String stored = optionalStored.orElseThrow(() -> new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN));
                    if (!stored.equals(requestRefreshToken)) {
                        return Mono.error(new BusinessException(ErrorCode.INVALID_TOKEN));
                    }
                    // 3. Fetch the user associated with the token.
                    return memberService.findById(userId);
                })
                // 4. Validate the user's account status.
                .flatMap(member -> validateUser(member)
                        // 5. Issue and store new tokens.
                        .flatMap(validMember ->
                                createAndStoreAuthTokens(validMember)
                                        .map(tokens -> createAuthInfoFromAuthTokensAndMember(tokens, validMember))
                        )
                )
                // 6. If user validation fails, delete the refresh token to prevent further use.
                .doOnError(BusinessException.class, ex -> {
                    ErrorCode code = ex.getErrorCode();
                    if (code == ErrorCode.NOT_FOUND_USER
                            || code == ErrorCode.DELETED_USER
                            || code == ErrorCode.SUSPENDED_USER
                            || code == ErrorCode.INACTIVE_USER) {
                        refreshTokenStoreProvider.delete(userId).subscribe();
                    }
                });
    }

    /**
     * Signs out a user by invalidating their tokens.
     * This involves deleting the refresh token from Redis and blacklisting the access token
     * for its remaining validity period to prevent unauthorized use.
     * 
     * @param accessToken The access token to be blacklisted.
     * @param userId The ID of the user signing out.
     * @return A Mono that completes when the sign-out process is finished.
     */
    public Mono<Void> signOut(String accessToken, Long userId) {
        // 1. Delete the refresh token from Redis, making it unusable for future refreshes.
        Mono<Void> deleteRefresh = refreshTokenStoreProvider.delete(userId);

        // 2. Add the access token to a blacklist for its remaining validity period.
        // This prevents the token from being used even if it hasn't expired yet.
        long ttlMillis = jwtService.getRemainingValidity(accessToken);
        Mono<Boolean> blacklistMono = Mono.just(false);
        if (ttlMillis >= 1000) { // Only blacklist if the token is valid for at least one more second.
            blacklistMono = tokenBlacklistStoreProvider.blacklistHashed(
                    accessToken, Duration.ofMillis(ttlMillis)
            );
        }

        // 3. Execute both operations concurrently.
        return Mono.when(deleteRefresh, blacklistMono).then();
    }

    /**
     * Processes a successful authentication event by validating the user and issuing JWT tokens.
     * This method is called after the authentication manager successfully verifies credentials.
     * It performs follow-up actions like checking account status, updating the last sign-in timestamp,
     * and generating and storing new tokens.
     *
     * @param auth The successfully authenticated token from the authentication manager,
     * containing the user's principal (Member).
     * @return A Mono emitting the complete authentication information (AuthInfo), including new tokens.
     * @throws BusinessException if the authenticated user's account is invalid (e.g., suspended, deleted).
     */
    private Mono<AuthInfo> processSuccessfulAuthentication(UsernamePasswordAuthenticationToken auth) {
    Member member = (Member) auth.getPrincipal();
    
    return validateUser(member)
            .doOnSuccess(Member::updateLastSignInAt)
            .flatMap(validMember ->
                    createAndStoreAuthTokens(validMember)
                            .map(tokens -> createAuthInfoFromAuthTokensAndMember(tokens, validMember))
            );
    }

    /**
     * Checks if a user's account is in a valid, usable state.
     * This method verifies various account statuses such as enabled, non-locked, and non-expired
     * 
     * @param member The member to validate.
     * @return A Mono emitting the member if valid, or an error otherwise.
     * @throws BusinessException if the member is null or their account status is invalid.
     */
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

    /**
     * Generates new access and refresh tokens for a given member and stores them in Redis.
     * The tokens are stored with their respective time-to-live (TTL) values.
     * 
     * @param member The member for whom to create tokens.
     * @return A Mono emitting the newly created AuthTokens object.
     */
    private Mono<AuthTokens> createAndStoreAuthTokens(Member member) {
        String accessToken = jwtService.generateAccessToken(member.getId(), member.getAuthorities());
        String refreshToken = jwtService.generateRefreshToken(member.getId());

        Duration accessTtl = Duration.ofMillis(jwtService.getRemainingValidity(accessToken));
        Duration refreshTtl = Duration.ofMillis(jwtService.getRemainingValidity(refreshToken));

        // Store both tokens in Redis with their respective TTLs.
        return accessTokenStoreProvider.save(member.getId(), accessToken, accessTtl)
                .then(refreshTokenStoreProvider.save(member.getId(), refreshToken, refreshTtl))
                .thenReturn(new AuthTokens(accessToken, refreshToken, refreshTtl));
    }

    /**
     * Constructs an AuthInfo object from generated tokens and member details.
     * This DTO encapsulates all necessary authentication information to be returned to the client.
     *
     * @param tokens The AuthTokens object containing access and refresh tokens.
     * @param member The Member entity containing user details.
     * @return An AuthInfo object populated with token and member data.
     */
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
                member.getLastSignInAt()
        );
    }
}
