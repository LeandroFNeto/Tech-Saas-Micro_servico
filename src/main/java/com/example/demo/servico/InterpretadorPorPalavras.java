package com.example.demo.servico;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class InterpretadorPorPalavras implements InterpretadorRespostaLembrete {

    private static final List<String> FRASES_CONFIRMA = List.of(
            "pode ser",
            "tudo certo",
            "estarei la",
            "estarei presente",
            "vou sim",
            "com certeza"
    );

    private static final List<String> PALAVRAS_CONFIRMA = List.of(
            "sim",
            "confirmo",
            "confirmado",
            "confirmada",
            "confirmar",
            "confirma",
            "combinado",
            "combinada",
            "ok"
    );

    private static final Pattern PADRAO_NEGACAO_CONFIRMA = Pattern.compile(
            "\\b(?:nao|nem|nunca|jamais)\\s+(?:\\w+\\s+){0,2}(?:pode ser|tudo certo|estarei la|estarei presente|vou sim|com certeza|sim|confirmo|confirmado|confirmada|confirmar|confirma|combinado|combinada|ok)\\b"
    );

    private static final List<String> FRASES_REMARCA = List.of(
            "nao vou poder",
            "mudar a data",
            "mudar data",
            "outra data",
            "trocar data",
            "nao posso",
            "nao poderei",
            "nao consigo",
            "remarcar a data"
    );

    private static final List<String> PALAVRAS_REMARCA = List.of(
            "cancelar",
            "cancela",
            "desmarcar",
            "desmarca",
            "remarcar",
            "remarca"
    );

    private static final List<String> FRASES_PARAR = List.of(
            "nao quero receber",
            "parar avisos",
            "cancelar mensagens",
            "parar mensagens"
    );

    private static final List<String> PALAVRAS_PARAR = List.of(
            "parar",
            "pare",
            "stop",
            "descadastrar"
    );

    @Override
    public IntencaoResposta interpretar(String texto) {
        if (texto == null || texto.isBlank()) {
            return IntencaoResposta.OUTRO;
        }

        boolean temEmojiJoinha = texto.contains("👍") || texto.contains("\uD83D\uDC4D");
        String normalizado = normalizar(texto);
        String semNegacaoConfirma = PADRAO_NEGACAO_CONFIRMA.matcher(normalizado).replaceAll(" ").trim();
        boolean temNegacaoDireta = !semNegacaoConfirma.equals(normalizado) || normalizado.matches("^nao\\b.*");

        boolean confirma = (temEmojiJoinha && !temNegacaoDireta)
                || casarExpressao(semNegacaoConfirma, FRASES_CONFIRMA, PALAVRAS_CONFIRMA);
        boolean remarca = casarExpressao(normalizado, FRASES_REMARCA, PALAVRAS_REMARCA);
        boolean parar = casarExpressao(normalizado, FRASES_PARAR, PALAVRAS_PARAR);

        int total = (confirma ? 1 : 0) + (remarca ? 1 : 0) + (parar ? 1 : 0);
        if (total != 1) {
            return IntencaoResposta.OUTRO;
        }

        if (confirma) {
            return IntencaoResposta.CONFIRMA;
        }
        if (remarca) {
            return IntencaoResposta.REMARCA;
        }
        return IntencaoResposta.PARAR;
    }

    private boolean casarExpressao(String normalizado, List<String> frases, List<String> palavras) {
        if (normalizado == null || normalizado.isEmpty()) {
            return false;
        }
        for (String frase : frases) {
            if (normalizado.contains(frase)) {
                return true;
            }
        }
        for (String palavra : palavras) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(palavra) + "\\b");
            if (p.matcher(normalizado).find()) {
                return true;
            }
        }
        return false;
    }

    private String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
