package prototype.coreapi.domain.chat.entity;

import lombok.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import prototype.coreapi.domain.BaseEntity;

@Table("chats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Chat extends BaseEntity {

    @Column("member_id")
    private Long memberId;

    @Column("title")
    private String title;

    @Column("summary")
    private String summary;

    @Column("last_message_preview")
    private String lastMessagePreview;

    @Column("is_archived")
    @Builder.Default
    private boolean isArchived = false;

    public void updateLastMessagePreview(String lastMessagePreview) {
        this.lastMessagePreview = lastMessagePreview;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateSummary(String summary) {
        this.summary = summary;
    }
}
