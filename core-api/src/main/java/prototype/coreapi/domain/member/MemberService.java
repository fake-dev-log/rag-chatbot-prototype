package prototype.coreapi.domain.member;

import org.springframework.security.core.context.SecurityContext;
import prototype.coreapi.domain.member.dto.MemberRequest;
import prototype.coreapi.domain.member.entity.Member;
import prototype.coreapi.domain.member.mapper.MemberMapper;
import prototype.coreapi.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import prototype.coreapi.global.exception.BusinessException;
import prototype.coreapi.global.exception.ErrorCode;
import reactor.core.publisher.Mono;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import prototype.coreapi.global.enums.Role;
import java.util.Optional;

/**
 * Service for managing member-related operations.
 * Provides functionalities for checking email existence, finding members by ID or email,
 * and creating new members with appropriate role and status handling.
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberMapper memberMapper;

    /**
     * Checks if a member with the given email already exists.
     * @param email The email to check.
     * @return A Mono emitting true if the email exists, false otherwise.
     */
    public Mono<Boolean> existsByEmail(String email) { 
        return memberRepository.existsByEmail(email);
    }

    /**
     * Finds a member by their ID.
     * @param id The ID of the member to find.
     * @return A Mono emitting the found Member, or an error if not found.
     * @throws BusinessException if the member is not found.
     */
    public Mono<Member> findById(Long id) {
        return memberRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.NOT_FOUND_USER)));
    }

     /**
     * Finds a member by their email address.
     * @param email The email address of the member to find.
     * @return A Mono emitting the found Member, or an error if not found.
     * @throws BusinessException if the member is not found.
     */
    public Mono<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.NOT_FOUND_USER)));
    }

    /**
     * Creates a new member. Encodes the password and sets the role/status based on authentication.
     * If an ADMIN user is authenticated, they can set the role and status of the new member.
     * Otherwise, the new member will have default USER role and ACTIVE status.
     * @param request The MemberRequest containing the new member's details.
     * @return A Mono emitting the created Member.
     * @throws BusinessException if the email is already in use.
     */
    public Mono<Member> createMember(MemberRequest request) {
        return memberRepository.existsByEmail(request.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException(ErrorCode.EXIST_EMAIL));
                    }
                    // Create entity & encrypt password
                    Member member = memberMapper.toEntity(request);
                    member.encodePassword(request.getPassword(), passwordEncoder);

                    // Check if authenticated and is ADMIN reactively
                    return ReactiveSecurityContextHolder.getContext()
                            .map(SecurityContext::getAuthentication)
                            .map(Optional::of)
                            .defaultIfEmpty(Optional.empty())
                            .flatMap(authOpt -> {
                                authOpt.ifPresent(authentication -> {
                                    if (authentication.isAuthenticated() &&
                                            authentication.getAuthorities().stream()
                                                    .anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.ADMIN.name()))) {
                                        // If user has ADMIN role, allow setting role/status
                                        if (request.getRole() != null) {
                                            member.updateRole(request.getRole());
                                        }
                                        if (request.getStatus() != null) {
                                            member.updateStatus(request.getStatus());
                                        }
                                    }
                                });
                                return memberRepository.save(member); // Reactive save
                            });
                });
    }
}
