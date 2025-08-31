package prototype.coreapi.domain.prompt.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import prototype.coreapi.domain.prompt.dto.PromptRequest;
import prototype.coreapi.domain.prompt.dto.PromptResponse;
import prototype.coreapi.domain.prompt.entity.PromptTemplate;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface PromptMapper {

    PromptTemplate toEntity(PromptRequest request);

    PromptResponse toResponse(PromptTemplate promptTemplate);

}
