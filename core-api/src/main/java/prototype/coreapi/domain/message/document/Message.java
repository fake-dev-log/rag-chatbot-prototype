package prototype.coreapi.domain.message.document;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import prototype.coreapi.global.enums.Sender;

import java.time.Instant;
import java.util.List;

@Document(collection = "messages")
@CompoundIndex(def = "{'chatId': 1, 'sequence': 1}", name = "chat_seq_idx")
@Data
@NoArgsConstructor
public class Message {

    @Id
    private String id;             // MongoDB ObjectId

    private Long chatId;           // RDB conversations.id

    private Sender sender;         // USER or BOT

    private Object content;        // String 이나 JSON 객체

    private String contentType;    // "text", "image", 등

    private Long sequence;         // 메시지 순서

    @CreatedDate
    private Instant createdAt;     // 자동으로 채워짐

    private List<SourceDocument> sources;
}
