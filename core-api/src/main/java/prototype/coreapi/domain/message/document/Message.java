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

    private Object content;        // String or JSON object

    private String contentType;    // e.g., "text", "image"

    private Long sequence;         // Message sequence

    @CreatedDate
    private Instant createdAt;     // Automatically populated

    private List<SourceDocument> sources;
}