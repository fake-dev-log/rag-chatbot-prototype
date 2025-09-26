package prototype.coreapi.domain.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import prototype.coreapi.domain.chat.dto.ChatProjection;
import prototype.coreapi.domain.chat.dto.ChatResponse;
import prototype.coreapi.domain.chat.entity.Chat;
import prototype.coreapi.domain.chat.repository.ChatRepository;
import prototype.coreapi.domain.chatbot.ChatbotService;
import prototype.coreapi.domain.chatbot.dto.ChatChunk;
import prototype.coreapi.domain.chatbot.dto.ChatbotResponse;
import prototype.coreapi.domain.chatbot.dto.SourceDocumentProjection;
import prototype.coreapi.domain.member.MemberService;
import prototype.coreapi.domain.member.entity.Member;
import prototype.coreapi.domain.message.MessageService;
import prototype.coreapi.domain.message.document.Message;
import prototype.coreapi.global.config.CacheConfig;
import prototype.coreapi.global.exception.BusinessException;
import prototype.coreapi.global.exception.ErrorCode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ChatService {

    private final ChatRepository chatRepository;
    private final MemberService memberService;
    private final MessageService messageService;
    private final ChatbotService chatBotService;
    private final ObjectMapper objectMapper;
    private final Cache summaryCache;

    public ChatService(ChatRepository chatRepository, MemberService memberService, MessageService messageService, ChatbotService chatBotService, ObjectMapper objectMapper, CacheManager cacheManager) {
        this.chatRepository = chatRepository;
        this.memberService = memberService;
        this.messageService = messageService;
        this.chatBotService = chatBotService;
        this.objectMapper = objectMapper;
        this.summaryCache = Objects.requireNonNull(cacheManager.getCache(CacheConfig.SUMMARY_CACHE));
    }

    public Mono<Chat> findById(Long id) {
        return chatRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.NO_SUCH_CONTENT_VALUE, "Chat")
                ));
    }

    public Mono<ChatProjection> findByIdWithEmail(Long chatId) {
        return chatRepository.findByIdWithEmail(chatId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.NO_SUCH_CONTENT_VALUE, "Chat")
                ));
    }

    public Flux<ChatResponse> findAllByMemberId(Long memberId) {
        return chatRepository.findAllByMemberWithEmail(memberId)
                .map(proj -> ChatResponse.builder()
                        .id(proj.id())
                        .memberEmail(proj.memberEmail())
                        .title(proj.title())
                        .lastMessagePreview(proj.lastMessagePreview())
                        .isArchived(proj.isArchived())
                        .createdAt(proj.createdAt())
                        .updatedAt(proj.updatedAt())
                        .build()
                );
    }

    public Mono<Chat> createChat(Long memberId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusSeconds(1);

        Mono<Member> memberMono = memberService.findById(memberId);
        Mono<Long> countMono = chatRepository.countByCreatedAtIsBetween(startOfDay, endOfDay);

        return Mono.zip(memberMono, countMono)
                .flatMap(tuple -> {
                    Member member = tuple.getT1();
                    long count = tuple.getT2();

                    Chat chat = Chat.builder()
                            .memberId(member.getId())
                            .build();

                    String title = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " Chat";
                    if (count > 0) {
                        title += " (" + (count + 1) + ")";
                    }
                    chat.updateTitle(title);

                    return chatRepository.save(chat);
                });
    }

    public Flux<ChatChunk> handleUserMessage(Long chatId, String userContent) {
        Mono<Chat> chatMono = findById(chatId).cache();

        return chatMono.flatMapMany(chat -> {
            Mono<Chat> afterUserMessageSaved = messageService.saveUserMessage(chatId, userContent)
                    .flatMap(savedUser -> updateLastMessagePreview(chat, savedUser))
                    .thenReturn(chat);

            return afterUserMessageSaved.flatMapMany(savedChat -> {
                List<String> tokenBuffer = new ArrayList<>();
                List<SourceDocumentProjection> sourcesBuffer = new ArrayList<>();

                // 1. Determine the final summary to be used in lambdas.
                String summaryFromCache = summaryCache.get(chatId, String.class);
                final String previousSummary = (summaryFromCache != null) ? summaryFromCache : savedChat.getSummary();

                // 2. Stream response from chatbot, passing in the conversation summary.
                Flux<ChatChunk> tokenFlux = chatBotService.inference(userContent, previousSummary)
                        .doOnNext(chunk -> {
                            if ("token".equals(chunk.type())) {
                                tokenBuffer.add(chunk.data().asText());
                            } else if ("sources".equals(chunk.type())) {
                                List<SourceDocumentProjection> sources = objectMapper.convertValue(
                                        chunk.data(),
                                        new TypeReference<List<SourceDocumentProjection>>() {}
                                );
                                sourcesBuffer.addAll(sources);
                            }
                        });

                // 3. After streaming is complete, perform post-processing.
                Mono<Void> saveBotMono = Mono.defer(() -> {
                    String fullBotAnswer = String.join("", tokenBuffer);
                    if (fullBotAnswer.isBlank()) {
                        return Mono.empty();
                    }

                    // 4. Optimistic Caching: create and cache a raw summary immediately.
                    String rawSummary = (previousSummary == null ? "" : previousSummary + "\n\n") +
                            "User: " + userContent + "\n" +
                            "AI: " + fullBotAnswer;
                    summaryCache.put(chatId, rawSummary);

                    // 5. In parallel, start the high-quality summarization and save bot message.
                    updateConversationSummaryAsync(chatId, userContent, fullBotAnswer, previousSummary);

                    ChatbotResponse resp = ChatbotResponse.builder()
                            .answer(fullBotAnswer)
                            .sources(sourcesBuffer)
                            .build();

                    return messageService.saveBotMessage(chatId, resp)
                            .flatMap(saved -> updateLastMessagePreview(savedChat, saved))
                            .then();
                });

                return Flux.concat(tokenFlux, saveBotMono.thenMany(Flux.empty()));
            });
        });
    }

    @Async
    public void updateConversationSummaryAsync(Long chatId, String question, String answer, String previousSummary) {
        chatBotService.summarize(previousSummary, question, answer)
                .flatMap(response -> {
                    String newSummary = response.getSummary();
                    // Update cache with the high-quality summary.
                    summaryCache.put(chatId, newSummary);
                    // Persist the new summary to the database.
                    return findById(chatId)
                            .flatMap(chat -> {
                                chat.updateSummary(newSummary);
                                return chatRepository.save(chat);
                            });
                })
                .subscribe(
                        null, // On success, do nothing.
                        error -> log.error("Failed to update summary for chat ID: {}", chatId, error)
                );
    }

    public Mono<Chat> updateLastMessagePreview(Chat chat, Message message) {
        String preview;
        if ("text".equals(message.getContentType()) && !message.getContent().toString().isBlank()) {
            preview = message.getContent().toString();
            if (preview.length() > 100) {
                preview = preview.substring(0, 100) + "...";
            }
        } else {
            preview = message.getContentType();
        }
        chat.updateLastMessagePreview(preview);
        return chatRepository.save(chat);
    }
}