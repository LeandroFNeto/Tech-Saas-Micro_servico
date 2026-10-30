package com.example.demo.graphql;

import com.example.demo.dto.empresa.EmpresaResponseDTO;
import com.example.demo.dto.empresa.ModuloMenuDTO;
import com.example.demo.model.Empresa;
import com.example.demo.model.ModuloEmpresa;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomiaTokenAuditoriaTest {

    private static final Locale RELATORIO = Locale.forLanguageTag("pt-BR");

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void auditaEconomiaDeTokensEntreRestEGraphql() throws Exception {
        Empresa empresa = empresaCompleta();

        String jsonRest = objectMapper.writeValueAsString(respostaAdmin(empresa));
        String jsonGraphql = objectMapper.copy()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .writeValueAsString(consultaDaIa(empresa));

        double tokensRest = estimarTokens(jsonRest);
        double tokensGraphql = estimarTokens(jsonGraphql);
        double economia = (tokensRest - tokensGraphql) / tokensRest * 100.0;

        System.out.println(relatorio(jsonRest, jsonGraphql, tokensRest, tokensGraphql, economia));

        JsonNode grafo = objectMapper.readTree(jsonGraphql);
        assertEquals(Set.of("nome", "tabelaDePrecos"), Set.copyOf(campos(grafo)));
        assertEquals("Recanto Vista Alegre", grafo.get("nome").asText());
        assertTrue(jsonRest.contains("regrasLocacao"));
        assertTrue(jsonRest.contains("urlsGaleria"));
        assertFalse(jsonGraphql.contains("googleCalendarId"));
        assertTrue(tokensGraphql < tokensRest);
        assertTrue(economia > 0.0);
    }

    private static double estimarTokens(String json) {
        return json.length() / 4.0;
    }

    private static String relatorio(
            String jsonRest,
            String jsonGraphql,
            double tokensRest,
            double tokensGraphql,
            double economia) {
        return """
                === Auditoria de economia de tokens (Gemini) ===
                REST tradicional (EmpresaResponseDTO): %s tokens (%d caracteres)
                GraphQL (nome + tabelaDePrecos): %s tokens (%d caracteres)
                Economia de tokens: %s%%
                Redução de custo na API do Gemini: %s%%
                """.formatted(
                numero(tokensRest),
                jsonRest.length(),
                numero(tokensGraphql),
                jsonGraphql.length(),
                numero(economia),
                numero(economia));
    }

    private static String numero(double valor) {
        return String.format(RELATORIO, "%.2f", valor);
    }

    private static List<String> campos(JsonNode json) {
        List<String> nomes = new ArrayList<>();
        json.fieldNames().forEachRemaining(nomes::add);
        return nomes;
    }

    private static EmpresaConsulta consultaDaIa(Empresa empresa) {
        return new EmpresaConsulta(
                null,
                empresa.getNome(),
                empresa.getTabelaDePrecos(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private static EmpresaResponseDTO respostaAdmin(Empresa empresa) {
        List<ModuloMenuDTO> menu = empresa.getModulosAtivos().stream()
                .map(modulo -> new ModuloMenuDTO(
                        modulo.getCodigoAcao(),
                        modulo.getTextoMenu(),
                        modulo.getAtivo(),
                        modulo.getOrdemExibicao()))
                .toList();
        return new EmpresaResponseDTO(
                empresa.getId(),
                empresa.getNome(),
                empresa.getSessaoWhatsapp(),
                "DISCONNECTED",
                empresa.getLinkGoogleMaps(),
                empresa.getLinkFotoPrincipal(),
                List.copyOf(empresa.getUrlsGaleria()),
                empresa.getLinkGaleria(),
                empresa.getPermiteReservaAutomatica(),
                empresa.getPermitePrevisaoTempo(),
                empresa.getLocacaoPorHora(),
                empresa.getRegrasLocacao(),
                menu,
                empresa.getCidadePrevisao(),
                empresa.getCidadePrevisaoResolvida(),
                Boolean.TRUE.equals(empresa.getPrevisaoTempoAtiva()),
                empresa.getAtivo(),
                null,
                LocalDateTime.of(2026, 10, 22, 14, 30));
    }

    private static Empresa empresaCompleta() {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setNome("Recanto Vista Alegre");
        empresa.setUsaIA(true);
        empresa.setSessaoWhatsapp("sessao_recanto_01");
        empresa.setMensagemSaudacao(
                "Olá! Sou o assistente do Recanto Vista Alegre. Posso falar de fotos, localização, preços, regras e reservas.");
        empresa.setRamoDeAtuacao("Locação de espaço para eventos");
        empresa.setTabelaDePrecos("Diária R$ 500. Hora R$ 80.");
        empresa.setRegrasLocacao("""
                Check-in às 14h e check-out às 12h. Cancelamento com 5 dias de antecedência devolve 80% do sinal. \
                Som alto só até 22h. Proibido soltar fogos. Capacidade máxima de 80 pessoas. \
                O sinal de 30% confirma a data e o restante é pago no check-in. Animais apenas na área externa.""");
        empresa.setLinkGoogleMaps("https://maps.app.goo.gl/recanto-vista-alegre");
        empresa.setLinkFotoPrincipal("https://res.cloudinary.com/demo/image/upload/v1/hero_recanto.jpg");
        empresa.setLinkGaleria("https://drive.google.com/drive/folders/galeria-recanto");
        empresa.setUrlsGaleria(List.of(
                "https://res.cloudinary.com/demo/image/upload/v1/galeria-piscina.jpg",
                "https://res.cloudinary.com/demo/image/upload/v1/galeria-salao.jpg",
                "https://res.cloudinary.com/demo/image/upload/v1/galeria-jardim.jpg"));
        empresa.setGoogleCalendarId("recanto-vista-alegre@group.calendar.google.com");
        empresa.setLocacaoPorHora(false);
        empresa.setPermiteReservaAutomatica(true);
        empresa.setModulosAtivos(List.of(
                modulo("VER_FOTOS", "Ver fotos do espaço", 1),
                modulo("VER_LOCALIZACAO", "Ver localização", 2),
                modulo("MENU_CARDAPIO", "Ver tabela de preços", 3),
                modulo("VER_REGRAS", "Ver regras do espaço", 4),
                modulo("SOLICITAR_RESERVA", "Solicitar reserva", 5)));
        return empresa;
    }

    private static ModuloEmpresa modulo(String codigo, String texto, int ordem) {
        ModuloEmpresa modulo = new ModuloEmpresa();
        modulo.setCodigoAcao(codigo);
        modulo.setTextoMenu(texto);
        modulo.setOrdemExibicao(ordem);
        modulo.setAtivo(true);
        return modulo;
    }
}
