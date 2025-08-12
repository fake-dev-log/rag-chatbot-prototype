package prototype.coreapi.domain.message;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import prototype.coreapi.domain.chatbot.dto.ChatbotResponse;
import prototype.coreapi.domain.message.document.Message;
import prototype.coreapi.domain.message.document.SourceDocument;
import prototype.coreapi.domain.message.repository.MessageRepository;
import prototype.coreapi.global.enums.Sender;
import prototype.coreapi.global.mongosequence.SequenceGeneratorService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepo;
    private final SequenceGeneratorService seqGen;

    public Mono<Message> saveUserMessage(Long chatId, String content) {
        return seqGen.generateSequence("chat_" + chatId)
                .flatMap(seq -> {
                    Message msg = new Message();
                    msg.setChatId(chatId);
                    msg.setSender(Sender.USER);
                    msg.setContent(content);
                    msg.setContentType("text");
                    msg.setSequence(seq);
                    msg.setSources(null);
                    return messageRepo.save(msg);
                });
    }

    public Mono<Message> saveBotMessage(Long chatId, ChatbotResponse botResponse) {
        return  seqGen.generateSequence("chat_" + chatId)
                .flatMap(seq -> {
                    Message msg = new Message();
                    msg.setChatId(chatId);
                    msg.setSender(Sender.BOT);
                    msg.setContent(botResponse.getAnswer());
                    msg.setContentType("text");
                    msg.setSequence(seq);
                    var sources = botResponse.getSources();
                    if (sources != null) {
                        msg.setSources(sources.stream()
                                .map(src -> new SourceDocument(
                                        src.fileName(),
                                        src.title(),
                                        src.pageNumber(),
                                        src.snippet()
                                ))
                                .toList()
                        );
                    }
                    return messageRepo.save(msg);
                });
    }

    public Flux<Message> getChatMessages(Long chatId) {
        return messageRepo.findByChatIdOrderBySequenceAsc(chatId);
    }
}
