package prototype.coreapi.domain.member;

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

import static prototype.coreapi.global.util.SecurityContextUtil.isAdmin;
import static prototype.coreapi.global.util.SecurityContextUtil.isNotAuthenticated;

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

                    // Admin 권한이면 role/status 설정 허용
                    if (!isNotAuthenticated() && isAdmin()) {
                        if (request.getRole() != null) {
                            member.updateRole(request.getRole());
                        }
                        if (request.getStatus() != null) {
                            member.updateStatus(request.getStatus());
                        }
                    }

                    // 리액티브 저장
                    return memberRepository.save(member);
                });
    }
}
