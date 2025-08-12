package prototype.coreapi.domain.message.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceDocument {

    @Field("file_name")
    private String fileName;

    private String title;

    @Field("page_number")
    private int pageNumber;

    private String snippet;
}
