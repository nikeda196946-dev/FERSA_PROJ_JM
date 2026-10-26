package com.fersacare.api.sharepoint;

import com.fersacare.api.sharepoint.dto.ArquivoSharePointDTO;
import com.fersacare.api.sharepoint.dto.ImportarSharePointRequest;
import com.fersacare.api.sharepoint.dto.ResultadoImportacaoDTO;
import com.fersacare.api.sharepoint.dto.SharePointStatusDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Importação de manuais do SharePoint (CMS, só ADMIN). */
@RestController
@RequestMapping("/api/admin/sharepoint")
@PreAuthorize("hasRole('ADMIN')")
public class SharePointController {

    private final SharePointService sharePointService;

    public SharePointController(SharePointService sharePointService) {
        this.sharePointService = sharePointService;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        SharePointStatusDTO s = sharePointService.status();
        return Map.of("status", s, "categoriaPelaPastaPadrao", sharePointService.categoriaPelaPastaPadrao());
    }

    /** PDFs da pasta configurada, com a situação de cada um (NOVO, IMPORTADO, ALTERADO...). */
    @GetMapping("/arquivos")
    public List<ArquivoSharePointDTO> listar() {
        return sharePointService.listar();
    }

    /** Baixa e importa os arquivos escolhidos (ou todos os novos/alterados). A ingestão segue em segundo plano. */
    @PostMapping("/importar")
    public List<ResultadoImportacaoDTO> importar(@RequestBody(required = false) ImportarSharePointRequest req) {
        return sharePointService.importar(req);
    }
}
