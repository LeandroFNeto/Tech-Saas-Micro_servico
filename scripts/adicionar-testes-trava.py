from pathlib import Path

raiz = Path(".")

# 1. GerenciadorSessaoTest.java
gs_test_path = raiz / "src/test/java/com/example/demo/servico/GerenciadorSessaoTest.java"
gs_content = gs_test_path.read_text(encoding="utf-8")

novos_testes_gs = """    @Test
    @DisplayName("Ativa trava do proprietário e expira após 20 minutos")
    void deveAtivarTravaProprietarioEExpirarApos20Minutos() {
        RelogioControlado relogio = new RelogioControlado(INICIO_RELOGIO);
        GerenciadorSessao gerenciador = new GerenciadorSessao(relogio);

        gerenciador.ativarTravaProprietario(CLIENTE);

        assertThat(gerenciador.estaComTravaProprietarioAtiva(CLIENTE)).isTrue();
        assertThat(gerenciador.estaComTravaProprietarioAtiva("551188887777")).isTrue();

        relogio.avancarMinutos(19);
        assertThat(gerenciador.estaComTravaProprietarioAtiva(CLIENTE)).isTrue();

        relogio.avancarMinutos(1);
        assertThat(gerenciador.estaComTravaProprietarioAtiva(CLIENTE)).isFalse();
    }

    @Test
    @DisplayName("Remove trava do proprietário manualmente")
    void deveRemoverTravaProprietarioManualmente() {
        gerenciadorSessao.ativarTravaProprietario(CLIENTE);
        assertThat(gerenciadorSessao.estaComTravaProprietarioAtiva(CLIENTE)).isTrue();

        gerenciadorSessao.removerTravaProprietario(CLIENTE);
        assertThat(gerenciadorSessao.estaComTravaProprietarioAtiva(CLIENTE)).isFalse();
    }

    private static final class RelogioControlado"""

if "deveAtivarTravaProprietarioEExpirarApos20Minutos" not in gs_content:
    gs_content = gs_content.replace("    private static final class RelogioControlado", novos_testes_gs, 1)
    gs_test_path.write_text(gs_content, encoding="utf-8")
    print("1. GerenciadorSessaoTest atualizado")

# 2. WhatsappWebhookDTOTest.java
dto_test_path = raiz / "src/test/java/com/example/demo/dto/WhatsappWebhookDTOTest.java"
dto_content = dto_test_path.read_text(encoding="utf-8")

if "java.time.Clock" not in dto_content:
    dto_content = dto_content.replace(
        "import org.junit.jupiter.api.Test;",
        "import org.junit.jupiter.api.Test;\nimport java.time.Clock;\nimport java.time.Instant;\nimport java.time.ZoneId;"
    )

novos_testes_dto = """    @Test
    @DisplayName("Deserializa timestamp e detecta mensagem antiga com atraso superior a 2 minutos")
    void deveDetectarMensagemAntigaPeloTimestamp() throws Exception {
        Instant agora = Instant.parse("2026-11-02T22:00:00Z");
        Clock clockFixo = Clock.fixed(agora, ZoneId.of("UTC"));

        String jsonRecente = \"\"\"
            {
              "session": "RecantoBot",
              "from": "5511999999999@c.us",
              "type": "chat",
              "body": "Mensagem recente",
              "timestamp": 1793656750
            }
            \"\"\";

        String jsonAntigo = \"\"\"
            {
              "session": "RecantoBot",
              "from": "5511999999999@c.us",
              "type": "chat",
              "body": "Mensagem histórica",
              "timestamp": 1793656600
            }
            \"\"\";

        WhatsappWebhookDTO recente = mapper.readValue(jsonRecente, WhatsappWebhookDTO.class);
        WhatsappWebhookDTO antigo = mapper.readValue(jsonAntigo, WhatsappWebhookDTO.class);

        assertThat(recente.ehMensagemAntiga(clockFixo)).isFalse();
        assertThat(antigo.ehMensagemAntiga(clockFixo)).isTrue();
    }

    @Test
    @DisplayName("Resolve contato alvo para o destinatário to quando enviada pelo dono")
    void deveResolverContatoAlvoQuandoEnviadaPorMim() throws Exception {
        String json = \"\"\"
            {
              "session": "RecantoBot",
              "from": "5511888888888@c.us",
              "to": "5511999999999@c.us",
              "type": "chat",
              "fromMe": true,
              "body": "Olá, sou o dono"
            }
            \"\"\";

        WhatsappWebhookDTO dto = mapper.readValue(json, WhatsappWebhookDTO.class);
        assertThat(dto.resolverContatoAlvo()).isEqualTo("5511999999999@c.us");
    }
}"""

if "deveDetectarMensagemAntigaPeloTimestamp" not in dto_content:
    dto_content = dto_content[:-1] + novos_testes_dto # substitui a última chave fechando a classe
    dto_test_path.write_text(dto_content, encoding="utf-8")
    print("2. WhatsappWebhookDTOTest atualizado")

# 3. ControllerWhatsappTest.java
ctrl_test_path = raiz / "src/test/java/com/example/demo/controller/ControllerWhatsappTest.java"
ctrl_content = ctrl_test_path.read_text(encoding="utf-8")

marcador_ctrl = """        verify(observador).logFiltro(eq("teste-sessao"), anyString(), contains("Anti-X9"));
        verifyNoInteractions(moduloFactory);
    }"""

novo_bloco_ctrl = """        verify(observador).logFiltro(eq("teste-sessao"), anyString(), contains("Anti-X9"));
        verify(gerenciadorSessao).ativarTravaProprietario("551188887777");
        verifyNoInteractions(moduloFactory);
    }

    @Test
    @DisplayName("Deve pausar atendimento e não responder quando trava do proprietário estiver ativa")
    void devePausarAtendimentoQuandoTravaProprietarioEstiverAtiva() throws Exception {
        when(gerenciadorSessao.estaComTravaProprietarioAtiva("551188887777")).thenReturn(true);

        String payload = \"\"\"
            {
                "session": "teste-sessao",
                "from": "5511988887777@c.us",
                "body": "Olá, quero agendar",
                "type": "chat",
                "fromMe": false
            }
        \"\"\";

        mockMvc.perform(post("/webhook/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(observador).logFiltro(eq("teste-sessao"), eq("5511988887777@c.us"), contains("20 minutos"));
        verifyNoInteractions(moduloFactory);
    }

    @Test
    @DisplayName("Deve descartar mensagem antiga sem chamar módulo de atendimento")
    void deveDescartarMensagemAntiga() throws Exception {
        long timestampMuitoAntigo = java.time.Instant.now().minus(java.time.Duration.ofMinutes(10)).getEpochSecond();
        String payload = String.format(\"\"\"
            {
                "session": "teste-sessao",
                "from": "5511988887777@c.us",
                "body": "Mensagem histórica",
                "type": "chat",
                "fromMe": false,
                "timestamp": %d
            }
        \"\"\", timestampMuitoAntigo);

        mockMvc.perform(post("/webhook/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(observador).logFiltro(eq("teste-sessao"), eq("5511988887777@c.us"), contains("antiga"));
        verifyNoInteractions(moduloFactory);
    }"""

if "devePausarAtendimentoQuandoTravaProprietarioEstiverAtiva" not in ctrl_content:
    if marcador_ctrl in ctrl_content:
        ctrl_content = ctrl_content.replace(marcador_ctrl, novo_bloco_ctrl, 1)
    else:
        ctrl_content = ctrl_content.replace(marcador_ctrl.replace("\n", "\r\n"), novo_bloco_ctrl.replace("\n", "\r\n"), 1)
    ctrl_test_path.write_text(ctrl_content, encoding="utf-8")
    print("3. ControllerWhatsappTest atualizado")
