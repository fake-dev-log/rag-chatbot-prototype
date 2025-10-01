package prototype.coreapi.domain.document.entity;

import lombok.*;
import org.springframework.data.relational.core.mapping.Table;
import prototype.coreapi.domain.BaseEntity;
import prototype.coreapi.domain.document.enums.IndexingStatus;

@Table(name = "documents")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends BaseEntity {

    private String name; // User-provided original file name

    private String storedName; // UUID-based name for internal storage

    private String path; // Full path of the saved file

    private String type; // File extension (e.g., "pdf")

    private long size; // File size (bytes)

    private String category; // Document category for filtering

    private IndexingStatus status; // Indexing process status

    public void updateStatus(IndexingStatus status) {
        this.status = status;
    }
}