package com.example.demo.strategy.experimental;

import com.example.demo.model.Empresa;

public interface ModuloExperimental {

    String nome();

    String descricao();

    String responderSimulado(Empresa empresaDaSessao, String texto);
}
