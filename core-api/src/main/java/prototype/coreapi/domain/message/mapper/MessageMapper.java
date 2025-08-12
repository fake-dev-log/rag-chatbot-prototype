package prototype.coreapi.domain.message.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import prototype.coreapi.domain.message.document.Message;
import prototype.coreapi.domain.message.dto.MessageResponse;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface MessageMapper {

    MessageResponse toResponse(Message message);

    List<MessageResponse> toResponseList(List<Message> messages);
}