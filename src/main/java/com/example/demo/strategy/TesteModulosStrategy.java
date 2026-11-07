package com.example.demo.strategy;

import com.example.demo.model.Empresa;
import com.example.demo.servico.ServicoMensagem;
import com.example.demo.strategy.experimental.ModuloExperimental;
import com.example.demo.strategy.experimental.RegistroModulosExperimentais;
import com.example.demo.util.MascaraLog;
import com.example.demo.util.WhatsappUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TesteModulosStrategy implements ModuloAtendimentoStrategy {

    public static final String RAMO = "TESTE_MODULOS";

    private static final Logger log = LoggerFactory.getLogger(TesteModulosStrategy.class);
    private static final String COMANDO_LISTAR = "/modulos";
    private static final String COMANDO_EXECUTAR = "/modulo ";

    static final String MENSAGEM_NAO_AUTORIZADO =
            "Este número não está autorizado a usar o bot de teste de módulos.";
    static final String MENSAGEM_SEM_AUTORIZADOS =
            "Não há número autorizado para o bot de teste de módulos. Nenhum módulo foi executado.";
    static final String MENSAGEM_AJUDA =
            "Comandos: */modulos* lista os módulos registrados e */modulo <nome> <texto>* executa um deles.";

    @Autowired private ServicoMensagem servicoMensagem;
    @Autowired private RegistroModulosExperimentais registro;

    @Value("${gamb.teste-modulos.numeros-autorizados:}")
    private String numerosAutorizados;

    @Override
    public String getRamoDeAtuacao() {
        return RAMO;
    }

    @Override
    public void processarMensagem(Empresa empresa, String numeroCliente, String textoRecebido, String estadoAtual) {
        String texto = textoRecebido == null ? "" : textoRecebido.trim();
        log.info("Bot de teste recebeu mensagem de {} ({})", MascaraLog.numero(numeroCliente), MascaraLog.tamanho(texto));

        List<String> autorizados = listaAutorizados();
        if (autorizados.isEmpty()) {
            enviar(empresa, numeroCliente, MENSAGEM_SEM_AUTORIZADOS);
            return;
        }
        if (!autorizados.contains(WhatsappUtil.normalizarParaChaveEstado(numeroCliente))) {
            enviar(empresa, numeroCliente, MENSAGEM_NAO_AUTORIZADO);
            return;
        }

        String minusculo = texto.toLowerCase(Locale.ROOT);
        if (minusculo.equals(COMANDO_LISTAR)) {
            enviar(empresa, numeroCliente, descreverModulos());
            return;
        }
        if (minusculo.startsWith(COMANDO_EXECUTAR)) {
            executar(empresa, numeroCliente, texto.substring(COMANDO_EXECUTAR.length()).trim());
            return;
        }
        enviar(empresa, numeroCliente, MENSAGEM_AJUDA);
    }

    private void executar(Empresa empresa, String numeroCliente, String argumentos) {
        String[] partes = argumentos.split("\\s+", 2);
        String conteudo = partes.length > 1 ? partes[1].trim() : "";
        Optional<ModuloExperimental> modulo = registro.buscar(partes[0]);
        if (modulo.isEmpty()) {
            enviar(empresa, numeroCliente, "Módulo não encontrado. Envie */modulos* para ver os registrados.");
            return;
        }
        if (conteudo.isEmpty()) {
            enviar(empresa, numeroCliente, "Informe o texto do teste: */modulo " + modulo.get().nome() + " <texto>*.");
            return;
        }
        enviar(empresa, numeroCliente, modulo.get().responderSimulado(empresa, conteudo));
    }

    private String descreverModulos() {
        List<ModuloExperimental> modulos = registro.listar();
        if (modulos.isEmpty()) {
            return "Nenhum módulo experimental registrado.";
        }
        return "Módulos registrados:\n" + modulos.stream()
                .map(m -> "- " + m.nome() + ": " + m.descricao())
                .collect(Collectors.joining("\n"));
    }

    private List<String> listaAutorizados() {
        if (numerosAutorizados == null || numerosAutorizados.isBlank()) {
            return List.of();
        }
        return Arrays.stream(numerosAutorizados.split("[,;\\s]+"))
                .map(WhatsappUtil::normalizarParaChaveEstado)
                .filter(numero -> !numero.isEmpty())
                .toList();
    }

    private void enviar(Empresa empresa, String numeroCliente, String texto) {
        servicoMensagem.enviarMensagemWPP(empresa, numeroCliente, texto);
    }
}
