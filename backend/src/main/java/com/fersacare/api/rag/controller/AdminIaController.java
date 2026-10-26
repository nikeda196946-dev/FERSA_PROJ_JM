package com.fersacare.api.rag.controller;

import com.fersacare.api.rag.dto.ChatLivreRequest;
import com.fersacare.api.rag.dto.ChatLivreResponse;
import com.fersacare.api.rag.service.ChatLivreService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Tela "Testar IA" do CMS: chat livre, sem manuais (só ADMIN — gasta cota do Gemini). */
@RestController
@RequestMapping("/api/admin/ia")
@PreAuthorize("hasRole('ADMIN')")
public class AdminIaController {

    private final ChatLivreService chatLivreService;

    public AdminIaController(ChatLivreService chatLivreService) {
        this.chatLivreService = chatLivreService;
    }

    @PostMapping("/livre")
    public ChatLivreResponse perguntar(@Valid @RequestBody ChatLivreRequest req) {
        return chatLivreService.perguntar(req.pergunta());
    }
}
