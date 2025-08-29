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

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberMapper memberMapper;

    public Mono<Boolean> existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    public Mono<Member> findById(Long id) {
        return memberRepository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.NOT_FOUND_USER)));
    }

    public Mono<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.NOT_FOUND_USER)));
    }

    public Mono<Member> createMember(MemberRequest request) {
        return memberRepository.existsByEmail(request.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException(ErrorCode.EXIST_EMAIL));
                    }
                    // 엔티티 생성 & 비밀번호 암호화
                    Member member = memberMapper.toEntity(request);
                    member.encodePassword(request.getPassword(), passwordEncoder);

                    // Check if authenticated and is ADMIN reactively
                    return ReactiveSecurityContextHolder.getContext()
                            .map(SecurityContext::getAuthentication)
                            .defaultIfEmpty(null) // Handle unauthenticated case
                            .flatMap(authentication -> {
                                if (authentication != null && authentication.isAuthenticated() &&
                                        authentication.getAuthorities().stream()
                                                .anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.ADMIN.name()))) {
                                    // Admin 권한이면 role/status 설정 허용
                                    if (request.getRole() != null) {
                                        member.updateRole(request.getRole());
                                    }
                                    if (request.getStatus() != null) {
                                        member.updateStatus(request.getStatus());
                                    }
                                }
                                return memberRepository.save(member); // Reactive save
                            });
                });
    }
}
