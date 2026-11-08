package com.example.demo.servico;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public class ConversaCurta {

    public enum Tipo { CUMPRIMENTO, BEM_ESTAR, AGRADECIMENTO, DESPEDIDA }

    public record Resposta(Tipo tipo, String texto) {
    }

    private static final int TAMANHO_MAXIMO_TEXTO = 60;
    private static final int LIMITE_MEMORIA = 2000;

    private static final Pattern BOM_DIA = Pattern.compile("\\bbom ?dia\\b");
    private static final Pattern BOA_TARDE = Pattern.compile("\\bboa ?tarde\\b");
    private static final Pattern BOA_NOITE = Pattern.compile("\\bboa ?noite\\b");
    private static final Pattern SAUDACAO = Pattern.compile("\\b(o+i+|o+la+|opa+|e ai|eae|salve|hey|hello|hi|oie+)\\b");
    private static final Pattern BEM_ESTAR = Pattern.compile(
            "\\b(tudo bem|tudo bom|td bem|tudo certo|como vai|como voce esta|como vc esta|como esta|blz|beleza|e voce|e vc|e com voce)\\b");
    private static final Pattern AGRADECIMENTO = Pattern.compile(
            "\\b(muito )?(obrigad[oa]|obrg|obg|brigad[oa]|valeu|vlw|agradeco|grato|grata)\\b");
    private static final Pattern DESPEDIDA = Pattern.compile(
            "\\b(tchau|xau|ate mais|ate logo|ate amanha|ate breve|falou|flw|abraco|abracos)\\b");
    private static final Pattern PREENCHIMENTO = Pattern.compile("\\b(pessoal|gente|galera|amigo|amiga|voces|ok|entao|e)\\b");

    private static final List<String> FRASES_CUMPRIMENTO = List.of(
            "Oi! Que bom ter você por aqui.",
            "Olá! Tudo bem por aí?",
            "Oi, tudo bem? Seja bem-vindo(a)!",
            "E aí! Que bom falar com você.");
    private static final List<String> FRASES_BOM_DIA = List.of(
            "Bom dia! Tudo bem?",
            "Bom dia! Que bom falar com você.",
            "Bom dia! Espero que seu dia esteja sendo ótimo.");
    private static final List<String> FRASES_BOA_TARDE = List.of(
            "Boa tarde! Tudo bem?",
            "Boa tarde! Que bom falar com você.",
            "Boa tarde! Espero que sua tarde esteja ótima.");
    private static final List<String> FRASES_BOA_NOITE = List.of(
            "Boa noite! Tudo bem?",
            "Boa noite! Que bom falar com você.",
            "Boa noite! Espero que sua noite esteja ótima.");
    private static final List<String> FRASES_BEM_ESTAR = List.of(
            "Tudo ótimo por aqui, obrigado por perguntar! E com você?",
            "Tudo certo por aqui! E você, tudo bem?",
            "Por aqui está tudo bem, obrigado! Espero que com você também.",
            "Tudo tranquilo por aqui! E por aí?");
    private static final List<String> FRASES_AGRADECIMENTO = List.of(
            "Por nada! Qualquer coisa é só chamar.",
            "Imagina, foi um prazer ajudar!",
            "Disponha! Se precisar de mais alguma coisa, estou por aqui.",
            "De nada! Pode me chamar quando quiser.");
    private static final List<String> FRASES_DESPEDIDA = List.of(
            "Até mais! Qualquer coisa é só chamar.",
            "Tchau! Foi um prazer falar com você.",
            "Até logo! Estou por aqui quando precisar.",
            "Valeu, até a próxima!");

    private final Map<String, Integer> ultimaFrase = new HashMap<>();

    public Optional<Resposta> responder(String contato, String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String restante = normalizar(texto);
        if (restante.isEmpty() || restante.length() > TAMANHO_MAXIMO_TEXTO) {
            return Optional.empty();
        }

        boolean dia = BOM_DIA.matcher(restante).find();
        restante = BOM_DIA.matcher(restante).replaceAll(" ");
        boolean tarde = BOA_TARDE.matcher(restante).find();
        restante = BOA_TARDE.matcher(restante).replaceAll(" ");
        boolean noite = BOA_NOITE.matcher(restante).find();
        restante = BOA_NOITE.matcher(restante).replaceAll(" ");
        boolean despedida = DESPEDIDA.matcher(restante).find();
        restante = DESPEDIDA.matcher(restante).replaceAll(" ");
        boolean agradecimento = AGRADECIMENTO.matcher(restante).find();
        restante = AGRADECIMENTO.matcher(restante).replaceAll(" ");
        boolean bemEstar = BEM_ESTAR.matcher(restante).find();
        restante = BEM_ESTAR.matcher(restante).replaceAll(" ");
        boolean saudacao = SAUDACAO.matcher(restante).find();
        restante = SAUDACAO.matcher(restante).replaceAll(" ");
        restante = PREENCHIMENTO.matcher(restante).replaceAll(" ").trim();

        boolean reconheceu = dia || tarde || noite || despedida || agradecimento || bemEstar || saudacao;
        if (!reconheceu || !restante.isEmpty()) {
            return Optional.empty();
        }

        String chave = contato == null ? "" : contato;
        if (despedida) {
            return Optional.of(new Resposta(Tipo.DESPEDIDA, escolher(chave, "despedida", FRASES_DESPEDIDA)));
        }
        if (agradecimento) {
            return Optional.of(new Resposta(Tipo.AGRADECIMENTO, escolher(chave, "agradecimento", FRASES_AGRADECIMENTO)));
        }
        String turno = dia ? "Bom dia! " : tarde ? "Boa tarde! " : noite ? "Boa noite! " : saudacao ? "Oi! " : "";
        if (bemEstar) {
            return Optional.of(new Resposta(Tipo.BEM_ESTAR, turno + escolher(chave, "bem", FRASES_BEM_ESTAR)));
        }
        if (dia) {
            return Optional.of(new Resposta(Tipo.CUMPRIMENTO, escolher(chave, "dia", FRASES_BOM_DIA)));
        }
        if (tarde) {
            return Optional.of(new Resposta(Tipo.CUMPRIMENTO, escolher(chave, "tarde", FRASES_BOA_TARDE)));
        }
        if (noite) {
            return Optional.of(new Resposta(Tipo.CUMPRIMENTO, escolher(chave, "noite", FRASES_BOA_NOITE)));
        }
        return Optional.of(new Resposta(Tipo.CUMPRIMENTO, escolher(chave, "oi", FRASES_CUMPRIMENTO)));
    }

    private synchronized String escolher(String contato, String grupo, List<String> frases) {
        if (ultimaFrase.size() > LIMITE_MEMORIA) {
            ultimaFrase.clear();
        }
        String chave = contato + "|" + grupo;
        Integer anterior = ultimaFrase.get(chave);
        int indice = ThreadLocalRandom.current().nextInt(frases.size());
        if (anterior != null && indice == anterior) {
            indice = (indice + 1) % frases.size();
        }
        ultimaFrase.put(chave, indice);
        return frases.get(indice);
    }

    private String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }
}
