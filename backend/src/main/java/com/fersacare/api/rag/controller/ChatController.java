package com.fersacare.api.rag.controller;

import com.fersacare.api.rag.dto.ChatRequest;
import com.fersacare.api.rag.dto.ChatResponse;
import com.fersacare.api.rag.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Chat da área pública (sem login): pergunta → resposta + fontes. */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse perguntar(@Valid @RequestBody ChatRequest req) {
        return chatService.perguntar(req.pergunta());
    }
}
