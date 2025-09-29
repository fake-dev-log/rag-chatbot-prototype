package prototype.coreapi.domain.document.entity;

import lombok.*;
import org.springframework.data.relational.core.mapping.Table;
import prototype.coreapi.domain.BaseEntity;

@Table(name = "documents")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends BaseEntity {

    private String name; // Original file name

    private String path; // Full path of the saved file

    private String type; // File extension (e.g., "pdf")

    private long size; // File size (bytes)

    private String category; // Document category for filtering
}