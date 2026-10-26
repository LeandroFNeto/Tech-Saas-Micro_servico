package com.example.demo.bdd;

import org.springframework.stereotype.Component;

@Component
public class BddContexto {

    public String tokenAdmin;
    public String jwt;
    public int status;
    public String resposta;
    public String sessao;
    public Exception erroFactory;
}
