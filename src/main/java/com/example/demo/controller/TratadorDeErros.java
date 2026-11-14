package com.example.demo.controller;

import com.example.demo.dto.erro.ErroDTO;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class TratadorDeErros extends ResponseEntityExceptionHandler {

    static final String MENSAGEM_INTERNA = "Erro interno";
    static final String MENSAGEM_INVALIDA = "Requisição inválida";

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> tratarViolacao(ConstraintViolationException ex, WebRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, MENSAGEM_INVALIDA, new HttpHeaders());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> tratarAcessoNegado(AccessDeniedException ex) {
        return resposta(HttpStatus.FORBIDDEN, "Acesso negado", new HttpHeaders());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> tratarAutenticacao(AuthenticationException ex) {
        return resposta(HttpStatus.UNAUTHORIZED, "Não autorizado", new HttpHeaders());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> tratarInesperado(Exception ex) {
        StackTraceElement[] pilha = ex.getStackTrace();
        log.error("Exceção não tratada {} em {}", ex.getClass().getName(), pilha.length > 0 ? pilha[0] : "origem desconhecida");
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, MENSAGEM_INTERNA, new HttpHeaders());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(erro -> erro.getField() + " " + erro.getDefaultMessage())
                .orElse(MENSAGEM_INVALIDA);
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos: " + mensagem, headers);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String mensagem;
        if (ex instanceof ResponseStatusException rse && rse.getReason() != null) {
            mensagem = rse.getReason();
        } else if (statusCode.is5xxServerError()) {
            mensagem = MENSAGEM_INTERNA;
        } else {
            mensagem = MENSAGEM_INVALIDA;
        }
        return resposta(statusCode, mensagem, headers);
    }

    private ResponseEntity<Object> resposta(HttpStatusCode status, String mensagem, HttpHeaders headers) {
        return ResponseEntity.status(status).headers(headers).body(new ErroDTO(mensagem));
    }
}
