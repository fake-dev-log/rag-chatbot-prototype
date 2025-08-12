package prototype.coreapi.domain.chat.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import prototype.coreapi.domain.chat.dto.ChatProjection;
import prototype.coreapi.domain.chat.dto.ChatResponse;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface ChatMapper {

    @Mapping(target = "messages", ignore = true)
    ChatResponse toResponse(ChatProjection chat);
}

