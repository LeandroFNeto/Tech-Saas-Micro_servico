package com.example.demo.strategy.experimental;

import com.example.demo.dto.assistente.AssistenteChatRequestDTO;
import com.example.demo.model.Empresa;
import com.example.demo.servico.assistente.AssistenteTutoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ModuloTesteVirtual implements ModuloExperimental {

    public static final String NOME = "teste-virtual";

    @Autowired private AssistenteTutoriaService assistenteTutoriaService;

    @Override
    public String nome() {
        return NOME;
    }

    @Override
    public String descricao() {
        return "Simula o bot de atendimento de uma empresa numa conversa de cliente final, sem enviar mensagem a cliente.";
    }

    @Override
    public String responderSimulado(Empresa empresaDaSessao, String texto) {
        return assistenteTutoriaService
                .responderCliente(empresaDaSessao, new AssistenteChatRequestDTO(texto, "TESTE_VIRTUAL"))
                .resposta();
    }
}
