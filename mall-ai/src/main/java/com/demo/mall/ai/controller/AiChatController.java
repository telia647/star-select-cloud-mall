package com.demo.mall.ai.controller;

import com.demo.mall.ai.dto.AiChatRequest;
import com.demo.mall.ai.dto.AiChatResponse;
import com.demo.mall.ai.dto.AiConversationResponse;
import com.demo.mall.ai.dto.AiMessageResponse;
import com.demo.mall.ai.service.AiChatService;
import com.demo.mall.common.api.Result;
import com.demo.mall.common.security.header.SecurityHeaders;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                       @Valid @RequestBody AiChatRequest request) {
        log.info("AI chat request received, userId={}, conversationId={}", userId, request.conversationId());
        return Result.success(aiChatService.chat(userId, request));
    }

    @PostMapping(path = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter streamChat(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                 @Valid @RequestBody AiChatRequest request) {
        log.info("AI stream chat request received, userId={}, conversationId={}", userId, request.conversationId());
        return aiChatService.streamChat(userId, request);
    }

    @GetMapping("/conversations")
    public Result<List<AiConversationResponse>> conversations(@RequestHeader(SecurityHeaders.USER_ID) Long userId) {
        return Result.success(aiChatService.conversations(userId));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public Result<List<AiMessageResponse>> messages(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                                    @PathVariable("conversationId") Long conversationId) {
        return Result.success(aiChatService.messages(userId, conversationId));
    }

    @DeleteMapping("/conversations/{conversationId}")
    public Result<Void> deleteConversation(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                           @PathVariable("conversationId") Long conversationId) {
        aiChatService.deleteConversation(userId, conversationId);
        return Result.success();
    }
}
