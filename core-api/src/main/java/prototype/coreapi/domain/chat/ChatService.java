package prototype.coreapi.domain.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
import prototype.coreapi.global.exception.BusinessException;
import prototype.coreapi.global.exception.ErrorCode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing chat conversations and messages.
 * Handles operations such as finding, creating, and updating chats,
 * as well as processing user messages and interacting with the chatbot service.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final MemberService memberService;
    private final MessageService messageService;
    private final ChatbotService chatBotService;
    private final ObjectMapper objectMapper;

    /**
     * Finds a chat by its ID.
     * @param id The ID of the chat to find.
     * @return A Mono emitting the found Chat, or an error if not found.
     * @throws BusinessException if the chat is not found.
     */
    public Mono<Chat> findById(Long id) { 
        return chatRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.NO_SUCH_CONTENT_VALUE, "Chat")
                ));
    }

    /**
     * Finds a chat projection by chat ID, including the member's email.
     * @param chatId The ID of the chat to find.
     * @return A Mono emitting the ChatProjection, or an error if not found.
     * @throws BusinessException if the chat is not found.
     */
    public Mono<ChatProjection> findByIdWithEmail(Long chatId) {
        return chatRepository.findByIdWithEmail(chatId)
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.NO_SUCH_CONTENT_VALUE, "Chat")
                ));
    }

    /**
     * Finds all chat rooms associated with a specific member ID.
     * @param memberId The ID of the member.
     * @return A Flux of ChatResponse containing all chat rooms for the member.
     */
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

    /**
     * Creates a new chat room for a given member.
     * The chat title is generated based on the current date and a sequential number if multiple chats are created on the same day.
     * @param memberId The ID of the member for whom to create the chat.
     * @return A Mono emitting the newly created Chat entity.
     */
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

    /**
     * Handles a user's message in a specific chat.
     * It saves the user's message, updates the chat preview, interacts with the chatbot service,
     * and then saves the chatbot's response.
     * @param chatId The ID of the chat room.
     * @param userContent The content of the user's message.
     * @return A Flux of ChatChunk representing the streamed response from the chatbot.
     */
    public Flux<ChatChunk> handleUserMessage(Long chatId, String userContent) {
        
        return findById(chatId)
                .flatMapMany(chat -> {
                    // 1) Save user message → update preview
                    Mono<Chat> afterUser = messageService.saveUserMessage(chatId, userContent)
                            .flatMap(savedUser -> updateLastMessagePreview(chat, savedUser))
                            .thenReturn(chat);

                    return afterUser.flatMapMany(savedChat -> {
                        List<String> tokenBuffer = new ArrayList<>();
                        List<SourceDocumentProjection> sourcesBuffer = new ArrayList<>();

                        Flux<ChatChunk> tokenFlux = chatBotService.inference(userContent)
                                // Send tokens as they arrive, buffering them
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

                        // Save Mono executed once at the end of the token stream
                        Mono<Void> saveBotMono = Mono.defer(() -> {
                            String full = String.join("", tokenBuffer);
                            ChatbotResponse resp = ChatbotResponse.builder()
                                    .answer(full)
                                    .sources(sourcesBuffer)
                                    .build();
                            return messageService.saveBotMessage(chatId, resp)
                                    .flatMap(saved -> updateLastMessagePreview(savedChat, saved))
                                    .then();
                        });

                        // Execute saveBotMono after the token Flux
                        return Flux.concat(tokenFlux, saveBotMono.thenMany(Flux.empty()));
                    });
                });
    }

    /**
     * Updates the last message preview of a chat.
     * The preview is truncated if it exceeds 100 characters.
     * @param chat The chat entity to update.
     * @param message The latest message to use for the preview.
     * @return A Mono emitting the updated Chat entity.
     */
    public Mono<Chat> updateLastMessagePreview(Chat chat, Message message) {
        
        String preview;
        if ("text".equals(message.getContentType())
                && !message.getContent().toString().isBlank()) {

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