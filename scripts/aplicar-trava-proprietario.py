from pathlib import Path

raiz = Path(".")

# 1. Atualizar WhatsappWebhookDTO.java
dto_path = raiz / "src/main/java/com/example/demo/dto/WhatsappWebhookDTO.java"
content = dto_path.read_text(encoding="utf-8")

if "import java.time.Clock;" not in content:
    content = content.replace(
        "import java.io.IOException;",
        "import java.io.IOException;\nimport java.time.Clock;\nimport java.time.Instant;"
    )

if "Long timestamp" not in content:
    marcador_orig = "Boolean fromMe,"
    novo_bloco = """Boolean fromMe,

        @JsonProperty("timestamp")
        @JsonAlias("t")
        @Schema(description = "Timestamp Unix da mensagem em segundos ou milissegundos", example = "1730592000")
        Long timestamp,

        @JsonProperty("to")
        @Schema(description = "JID do destinatário no formato plano do WPPConnect", example = "5511999999999@c.us")
        String to,"""
    content = content.replace(marcador_orig, novo_bloco, 1)

if "ehMensagemAntiga" not in content:
    marcador_metodo = "public boolean deveDescartarComoRuido()"
    novos_metodos = """public boolean ehMensagemAntiga(Clock clock) {
        if (timestamp == null) {
            return false;
        }
        long epochSegundos = timestamp > 100_000_000_000L ? (timestamp / 1000) : timestamp;
        Instant agora = clock == null ? Instant.now() : clock.instant();
        long segundosAtraso = agora.getEpochSecond() - epochSegundos;
        return segundosAtraso > 120;
    }

    public String resolverContatoAlvo() {
        if (enviadaPorMim()) {
            if (temTexto(to)) {
                return resolverLid(to);
            }
            if (id != null && temTexto(id.remoteJid())) {
                return resolverLid(id.remoteJid());
            }
        }
        return resolverRemoteJid();
    }

    public boolean deveDescartarComoRuido()"""
    content = content.replace(marcador_metodo, novos_metodos, 1)

dto_path.write_text(content, encoding="utf-8")
print("1. WhatsappWebhookDTO.java OK")

# 2. Atualizar GerenciadorSessao.java
sessao_path = raiz / "src/main/java/com/example/demo/servico/GerenciadorSessao.java"
sessao_content = sessao_path.read_text(encoding="utf-8")

if "TRAVA_PROPRIETARIO_MINUTOS" not in sessao_content:
    sessao_content = sessao_content.replace(
        "public static final int TTL_MINUTOS = 30;",
        "public static final int TTL_MINUTOS = 30;\n    public static final int TRAVA_PROPRIETARIO_MINUTOS = 20;"
    )

if "travasProprietario" not in sessao_content:
    sessao_content = sessao_content.replace(
        "private final Map<String, DadosReservaSessao> reservas = new ConcurrentHashMap<>();",
        "private final Map<String, DadosReservaSessao> reservas = new ConcurrentHashMap<>();\n    private final Map<String, LocalDateTime> travasProprietario = new ConcurrentHashMap<>();"
    )

if "ativarTravaProprietario" not in sessao_content:
    metodos_trava = """    public void ativarTravaProprietario(String numeroCliente) {
        if (numeroCliente == null || numeroCliente.isBlank()) {
            return;
        }
        travasProprietario.put(chave(numeroCliente), LocalDateTime.now(clock));
    }

    public boolean estaComTravaProprietarioAtiva(String numeroCliente) {
        if (numeroCliente == null || numeroCliente.isBlank()) {
            return false;
        }
        String chaveSessao = chave(numeroCliente);
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime momento = travasProprietario.get(chaveSessao);
        if (momento == null) {
            return false;
        }
        if (Duration.between(momento, agora).toMinutes() >= TRAVA_PROPRIETARIO_MINUTOS) {
            travasProprietario.remove(chaveSessao);
            return false;
        }
        return true;
    }

    public void removerTravaProprietario(String numeroCliente) {
        if (numeroCliente == null || numeroCliente.isBlank()) {
            return;
        }
        travasProprietario.remove(chave(numeroCliente));
    }

    public DadosReservaSessao getOuCriarReserva"""
    sessao_content = sessao_content.replace("    public DadosReservaSessao getOuCriarReserva", metodos_trava, 1)

sessao_path.write_text(sessao_content, encoding="utf-8")
print("2. GerenciadorSessao.java OK")

# 3. Atualizar ControllerWhatsapp.java
ctrl_path = raiz / "src/main/java/com/example/demo/controller/ControllerWhatsapp.java"
ctrl_content = ctrl_path.read_text(encoding="utf-8")

if "java.time.Clock" not in ctrl_content:
    ctrl_content = ctrl_content.replace(
        "import com.example.demo.util.WhatsappUtil;",
        "import com.example.demo.util.WhatsappUtil;\nimport java.time.Clock;"
    )

if "private Clock clock" not in ctrl_content:
    ctrl_content = ctrl_content.replace(
        "@Autowired private ServicoRespostaLembrete servicoRespostaLembrete;",
        "@Autowired private ServicoRespostaLembrete servicoRespostaLembrete;\n    @Autowired(required = false) private Clock clock = Clock.systemDefaultZone();\n\n    void setClock(Clock clock) {\n        this.clock = clock == null ? Clock.systemDefaultZone() : clock;\n    }"
    )

trecho_antigo_controller = """            if (payload.ehEventoConexao()) {
                return ResponseEntity.ok().build();
            }

            if (payload.enviadaPorMim()) {
                observador.logFiltro(sessao, rawFrom, "Dono do celular digitou (Anti-X9)");
                return ResponseEntity.ok().build();
            }

            if (payload.deveDescartarComoRuido()) {
                observador.logFiltro(sessao, rawFrom, "Mensagem ignorada (Grupo, Status ou Tipo Inválido)");
                return ResponseEntity.ok().build();
            }

            String chaveEstado = WhatsappUtil.normalizarParaChaveEstado(rawFrom);"""

trecho_novo_controller = """            if (payload.ehEventoConexao()) {
                return ResponseEntity.ok().build();
            }

            if (payload.ehMensagemAntiga(clock)) {
                observador.logFiltro(sessao, rawFrom, "Mensagem antiga descartada");
                return ResponseEntity.ok().build();
            }

            if (payload.enviadaPorMim()) {
                String contatoAlvo = payload.resolverContatoAlvo();
                if (contatoAlvo != null && !contatoAlvo.isBlank()) {
                    gerenciadorSessao.ativarTravaProprietario(WhatsappUtil.normalizarParaChaveEstado(contatoAlvo));
                }
                observador.logFiltro(sessao, rawFrom, "Dono do celular digitou (Anti-X9)");
                return ResponseEntity.ok().build();
            }

            if (payload.deveDescartarComoRuido()) {
                observador.logFiltro(sessao, rawFrom, "Mensagem ignorada (Grupo, Status ou Tipo Inválido)");
                return ResponseEntity.ok().build();
            }

            String chaveEstado = WhatsappUtil.normalizarParaChaveEstado(rawFrom);

            if (gerenciadorSessao.estaComTravaProprietarioAtiva(chaveEstado)) {
                observador.logFiltro(sessao, rawFrom, "Atendimento pausado: proprietário atendeu nos últimos 20 minutos");
                return ResponseEntity.ok().build();
            }"""

if "ehMensagemAntiga" not in ctrl_content:
    if trecho_antigo_controller in ctrl_content:
        ctrl_content = ctrl_content.replace(trecho_antigo_controller, trecho_novo_controller, 1)
    else:
        # tentar com CRLF
        ctrl_content = ctrl_content.replace(
            trecho_antigo_controller.replace("\n", "\r\n"),
            trecho_novo_controller.replace("\n", "\r\n"),
            1
        )

ctrl_path.write_text(ctrl_content, encoding="utf-8")
print("3. ControllerWhatsapp.java OK")
