package com.example.demo.servico;

import com.example.demo.model.Empresa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ServicoIARealIT {

    private String apiKey;
    private ServicoIA servicoIA;
    private Empresa empresa;

    @BeforeEach
    void setUp() throws IOException {
        apiKey = extrairChaveDoEnv();
        assumeTrue(apiKey != null && !apiKey.isBlank(), "GEMINI_API_KEY não encontrada no .env; pulando teste de integração real.");

        servicoIA = new ServicoIA(
                null,
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent",
                apiKey,
                10000
        );

        empresa = new Empresa();
        empresa.setId(10L);
        empresa.setNome("Recanto das Palmeiras");
        empresa.setRamoDeAtuacao("LOCACAO");
        empresa.setTabelaDePrecos("Diária R$ 500,00 nos finais de semana");
        empresa.setRegrasLocacao("Proibido som alto após as 22h");
        empresa.setLinkGoogleMaps("https://maps.google.com/?q=-23.5,-46.6");
        empresa.setUsaIA(true);
    }

    @Test
    @DisplayName("Executa chamada real sem mock à API do Google Gemini com a chave do .env")
    void deveResponderChamadaRealAoGemini() {
        Optional<String> resposta = servicoIA.responderAtendimento(empresa, "Bom dia! Quanto custa a diária?");

        assertThat(resposta).isPresent();
        assertThat(resposta.get()).isNotBlank();
        System.out.println("==================================================");
        System.out.println("RESPOSTA REAL DO GEMINI (SEM MOCK):");
        System.out.println(resposta.get());
        System.out.println("==================================================");
    }

    private String extrairChaveDoEnv() throws IOException {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            return System.getenv("GEMINI_API_KEY");
        }
        List<String> linhas = Files.readAllLines(envFile.toPath());
        for (String linha : linhas) {
            String l = linha.trim();
            if (l.startsWith("GEMINI_API_KEY=")) {
                return l.substring("GEMINI_API_KEY=".length()).trim().replace("\"", "").replace("'", "");
            }
        }
        return System.getenv("GEMINI_API_KEY");
    }
}
