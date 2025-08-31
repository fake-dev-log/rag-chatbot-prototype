package prototype.coreapi.domain.prompt.entity;

import lombok.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import prototype.coreapi.domain.BaseEntity;

/**
 * Represents a prompt template stored in the database.
 */
@Table(name = "prompt_templates")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptTemplate extends BaseEntity {

    @Column("name")
    private String name;

    @Column("template_content")
    private String templateContent;

    public void update(String name, String templateContent) {
        this.name = name;
        this.templateContent = templateContent;
    }
}
