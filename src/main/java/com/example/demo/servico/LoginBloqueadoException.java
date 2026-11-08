package com.example.demo.servico;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class LoginBloqueadoException extends ResponseStatusException {

    private final long segundosParaTentarDeNovo;

    public LoginBloqueadoException(long segundosParaTentarDeNovo) {
        super(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas de login. Tente novamente mais tarde.");
        this.segundosParaTentarDeNovo = segundosParaTentarDeNovo;
    }

    public long getSegundosParaTentarDeNovo() {
        return segundosParaTentarDeNovo;
    }

    @Override
    public HttpHeaders getHeaders() {
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.set(HttpHeaders.RETRY_AFTER, String.valueOf(segundosParaTentarDeNovo));
        return cabecalhos;
    }
}
