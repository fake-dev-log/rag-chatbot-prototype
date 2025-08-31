package prototype.coreapi.domain.chat;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import prototype.coreapi.domain.auth.dto.SignInPrincipal;
import prototype.coreapi.domain.chat.dto.ChatProjection;
import prototype.coreapi.domain.chat.dto.ChatResponse;
import prototype.coreapi.domain.chat.entity.Chat;
import prototype.coreapi.domain.chat.mapper.ChatMapper;
import prototype.coreapi.domain.chatbot.dto.ChatChunk;
import prototype.coreapi.domain.chatbot.dto.ChatbotRequest;
import prototype.coreapi.domain.message.MessageService;
import prototype.coreapi.domain.message.dto.MessageResponse;
import prototype.coreapi.domain.message.mapper.MessageMapper;
import prototype.coreapi.global.exception.BusinessException;
import prototype.coreapi.global.exception.ErrorCode;
import prototype.coreapi.global.util.ReactorControllerUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * REST controller for managing chat functionalities.
 * Provides endpoints for retrieving chat lists, creating new chats, and handling chat messages.
 * All chat operations require user authentication.
 */
@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
@Tag(name = "Chat", description = " APIs for chat functionalities")
public class ChatController {

    private final ChatService chatService;
    private final MessageService msgService;
    private final ChatMapper chatMapper;
    private final MessageMapper messageMapper;
    private final ReactorControllerUtil util;

    /**
     * Retrieves a list of chat rooms for the authenticated user.
     * @param principal The authenticated user's principal.
     * @return A Flux of ChatResponse containing the user's chat rooms.
     */
    @GetMapping
    public Flux<ChatResponse> getChats(@AuthenticationPrincipal SignInPrincipal principal) {
        
        return chatService.findAllByMemberId(principal.memberId());
    }

    /**
     * Creates a new chat room for the authenticated user.
     * @param principal The authenticated user's principal.
     * @return A Mono emitting the ID of the newly created chat room.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Long> createChat(@AuthenticationPrincipal SignInPrincipal principal) {
        
        return chatService.createChat(principal.memberId())
                .map(Chat::getId);
    }

    /**
     * Retrieves the messages for a specific chat room.
     * Access is restricted to the owner of the chat room.
     * @param principal The authenticated user's principal.
     * @param chatId The ID of the chat room.
     * @return A Mono emitting a ChatResponse containing the chat room details and its messages.
     */
    @GetMapping("/{chatId}")
    public Mono<ChatResponse> getMessages(
            @AuthenticationPrincipal SignInPrincipal principal,
            @PathVariable Long chatId
    ) {
        Mono<ChatProjection> chatMono = util.authorize(
                chatService.findByIdWithEmail(chatId),
                chat -> chat.memberId().equals(principal.memberId()),
                new BusinessException(ErrorCode.FORBIDDEN)
        );

        Mono<List<MessageResponse>> msgsMono = util.collectListFlux(
                msgService.getChatMessages(chatId),
                messageMapper::toResponse
        );

        return Mono.zip(chatMono, msgsMono)
                .map(tuple -> {
                    ChatProjection chat = tuple.getT1();
                    List<MessageResponse> msgs = tuple.getT2();

                    ChatResponse resp = chatMapper.toResponse(chat);
                    resp.setMessages(msgs);
                    return resp;
                });
    }

    /**
     * Handles sending a new message to a chat room and streams the chatbot's response.
     * The response is streamed as Newline-Delimited JSON (NDJSON).
     * Access is restricted to the owner of the chat room.
     * @param principal The authenticated user's principal.
     * @param chatId The ID of the chat room.
     * @param req The ChatbotRequest containing the user's query.
     * @return A Flux of ChatChunk representing the streamed response from the chatbot.
     */
    @PostMapping(
            path = "/{chatId}/message",
            produces = MediaType.APPLICATION_NDJSON_VALUE  // "application/stream+json"
    )
    public Flux<ChatChunk> createMessageAsJson(
            @AuthenticationPrincipal SignInPrincipal principal,
            @PathVariable Long chatId,
            @Valid @RequestBody ChatbotRequest req
    ) {
        return util.authorize(
                        chatService.findById(chatId),
                        chat -> chat.getMemberId().equals(principal.memberId()),
                        new BusinessException(ErrorCode.FORBIDDEN)
                ).thenMany(chatService.handleUserMessage(chatId, req.getQuery())
                        .timeout(Duration.ofSeconds(45))
                        .onErrorResume(TimeoutException.class,
                                ex -> Flux.just(new ChatChunk("error", JsonNodeFactory.instance.textNode("Response time exceeded.")))))
                .concatWith(Mono.just(new ChatChunk("done", JsonNodeFactory.instance.booleanNode(true))));
    }
}