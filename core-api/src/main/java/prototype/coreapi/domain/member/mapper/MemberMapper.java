package prototype.coreapi.domain.member.mapper;

import prototype.coreapi.domain.member.dto.MemberRequest;
import prototype.coreapi.domain.member.dto.MemberResponse;
import prototype.coreapi.domain.member.entity.Member;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface MemberMapper {

    @Mapping(target = "password", ignore = true) // Injected after encryption
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lastSignInAt", ignore = true)
    Member toEntity(MemberRequest request);

    MemberResponse toResponse(Member member);
}
