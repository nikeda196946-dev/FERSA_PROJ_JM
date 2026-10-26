package com.fersacare.api.common;

import com.fersacare.api.ia.IaIndisponivelException;
import com.fersacare.api.sharepoint.SharePointException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.List;

/**
 * Converte exceções em respostas JSON consistentes ({@link ApiError}).
 * Os controllers não precisam (nem devem) capturar essas exceções —
 * é por isso que a regra de negócio nunca fica no Controller.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiError> tratarNaoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiError.de(404, "Não encontrado", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<ApiError> tratarRequisicaoInvalida(RequisicaoInvalidaException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiError.de(422, "Requisição inválida", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> tratarValidacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .toList();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiError.deValidacao(req.getRequestURI(), detalhes));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> tratarAcessoNegado(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiError.de(403, "Acesso negado", "Seu perfil não tem permissão para esta ação.", req.getRequestURI()));
    }

    @ExceptionHandler(IaIndisponivelException.class)
    public ResponseEntity<ApiError> tratarIaIndisponivel(IaIndisponivelException ex, HttpServletRequest req) {
        log.warn("Gemini indisponível em {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ApiError.de(503, "IA indisponível", "O serviço de IA não respondeu. Tente novamente em instantes.", req.getRequestURI()));
    }

    @ExceptionHandler(SharePointException.class)
    public ResponseEntity<ApiError> tratarSharePoint(SharePointException ex, HttpServletRequest req) {
        log.warn("SharePoint em {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(ApiError.de(502, "Erro no SharePoint", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> tratarArquivoGrande(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(ApiError.de(413, "Arquivo muito grande", "O PDF passa do tamanho máximo permitido.", req.getRequestURI()));
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> tratarRequisicaoMalFormada(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError.de(400, "Requisição mal formada", "Parâmetros ou corpo da requisição inválidos.", req.getRequestURI()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> tratarMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ApiError.de(405, "Método não permitido", "Esta rota não aceita " + ex.getMethod() + ".", req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> tratarErroGenerico(Exception ex, HttpServletRequest req) {
        log.error("Erro não tratado em {} {}", req.getMethod(), req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError.de(500, "Erro interno", "Ocorreu um erro inesperado. Tente novamente.", req.getRequestURI()));
    }
}
