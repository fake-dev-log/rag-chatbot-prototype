package prototype.coreapi.domain.chat;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import prototype.coreapi.domain.auth.dto.LoginPrincipal;
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

@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
@Tag(name = "채팅", description = "채팅 기능 관련 API")
public class ChatController {

    private final ChatService chatService;
    private final MessageService msgService;
    private final ChatMapper chatMapper;
    private final MessageMapper messageMapper;
    private final ReactorControllerUtil util;

    @GetMapping
    public Flux<ChatResponse> getChats(@AuthenticationPrincipal LoginPrincipal principal) {
        return chatService.findAllByMemberId(principal.memberId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Long> createChat(@AuthenticationPrincipal LoginPrincipal principal) {
        return chatService.createChat(principal.memberId())
                .map(Chat::getId);
    }

    @GetMapping("/{chatId}")
    public Mono<ChatResponse> getMessages(
            @AuthenticationPrincipal LoginPrincipal principal,
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

    @RequestMapping(
            path = "/{chatId}/message",
            method = { RequestMethod.POST, RequestMethod.GET },
            produces = MediaType.APPLICATION_NDJSON_VALUE  // "application/stream+json"
    )
    public Flux<ChatChunk> createMessageAsJson(
            @AuthenticationPrincipal LoginPrincipal principal,
            @PathVariable Long chatId,
            @Valid @RequestBody(required = false) ChatbotRequest req
    ) {
        String query = (req != null) ? req.getQuery() : "";
        if (query == null || query.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        return util.authorize(
                        chatService.findById(principal.memberId()),
                        chat -> chat.getMemberId().equals(principal.memberId()),
                        new BusinessException(ErrorCode.FORBIDDEN)
                ).thenMany(chatService.handleUserMessage(chatId, query)
                        .timeout(Duration.ofSeconds(30))
                        .onErrorResume(TimeoutException.class,
                                ex -> Flux.just(new ChatChunk("error", JsonNodeFactory.instance.textNode("응답 시간이 초과되었습니다.")))))
                .concatWith(Mono.just(new ChatChunk("done", JsonNodeFactory.instance.booleanNode(true))));
    }
}