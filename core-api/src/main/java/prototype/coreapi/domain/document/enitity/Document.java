package prototype.coreapi.domain.document.enitity;

import lombok.*;
import org.springframework.data.relational.core.mapping.Table;
import prototype.coreapi.domain.BaseEntity;

@Table(name = "documents")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends BaseEntity {

    private String name; // 원본 파일명

    private String path; // 저장된 파일의 전체 경로

    private String type; // 파일 확장자 (e.g., "pdf")

    private long size; // 파일 크기 (bytes)
}
