package prototype.coreapi.domain.document;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import prototype.coreapi.domain.BaseEntity;

@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends BaseEntity {

    @Id
    private Long id;

    private String name; // 원본 파일명

    private String path; // 저장된 파일의 전체 경로

    private String type; // 파일 확장자 (e.g., "pdf")

    private long size; // 파일 크기 (bytes)

    @Builder
    public Document(Long id, String name, String path, String type, long size) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.type = type;
        this.size = size;
    }
}
