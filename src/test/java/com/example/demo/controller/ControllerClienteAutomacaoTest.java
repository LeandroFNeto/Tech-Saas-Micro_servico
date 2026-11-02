package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.AuditoriaAcessoContatoRepository;
import com.example.demo.repository.ContatoRepository;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.repository.ReservaRepository;
import com.example.demo.servico.ServicoAcessoEmpresa;
import com.example.demo.servico.ServicoLembreteReserva;
import com.example.demo.servico.ServicoSincronizacaoAgenda;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ControllerClienteAutomacao.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ServicoAcessoEmpresa.class)
class ControllerClienteAutomacaoTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmpresaRepository empresaRepository;

    @MockitoBean
    private ServicoSincronizacaoAgenda servicoSincronizacaoAgenda;

    @MockitoBean
    private ServicoLembreteReserva servicoLembreteReserva;

    @MockitoBean
    private ReservaRepository reservaRepository;

    @MockitoBean
    private ContatoRepository contatoRepository;

    @MockitoBean
    private AuditoriaAcessoContatoRepository auditoriaRepository;

    private Empresa empresa;
    private Usuario usuarioCliente;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(10L);
        empresa.setNome("Recanto dos Testes");
        empresa.setSessaoWhatsapp("sessao-automacao");
        empresa.setAtivo(true);

        usuarioCliente = new Usuario();
        usuarioCliente.setId(20L);
        usuarioCliente.setEmail("cliente@recanto.com.br");
        usuarioCliente.setRole(Role.CLIENTE);
        usuarioCliente.setEmpresa(empresa);

        when(empresaRepository.buscarPorSessaoComModulos("sessao-automacao")).thenReturn(empresa);
        when(empresaRepository.findBySessaoWhatsapp("sessao-automacao")).thenReturn(empresa);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComoCliente() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        usuarioCliente,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))
                )
        );
    }

    private void autenticarComoAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin-user",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );
    }

    @Test
    @DisplayName("Admin deve receber 403 Forbidden ao tentar listar reservas de clientes")
    void listarReservas_admin_retorna403() throws Exception {
        autenticarComoAdmin();

        mockMvc.perform(get("/empresas/sessao-automacao/reservas"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Requisicao sem autenticacao deve receber 401 Unauthorized")
    void listarReservas_semAutenticacao_retorna401() throws Exception {
        mockMvc.perform(get("/empresas/sessao-automacao/reservas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Parametro dias menor que 1 deve retornar 400 Bad Request")
    void listarReservas_diasMenorQueUm_retorna400() throws Exception {
        autenticarComoCliente();

        mockMvc.perform(get("/empresas/sessao-automacao/reservas").param("dias", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Parametro dias maior que 60 deve retornar 400 Bad Request")
    void listarReservas_diasMaiorQueSessenta_retorna400() throws Exception {
        autenticarComoCliente();

        mockMvc.perform(get("/empresas/sessao-automacao/reservas").param("dias", "61"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Listar reservas com sucesso retorna 200 e lista mapeada com flags corretas")
    void listarReservas_sucesso_retorna200() throws Exception {
        autenticarComoCliente();

        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        Contato contato = new Contato();
        contato.setId(5L);
        contato.setNome("Carlos Silva");
        contato.setWhatsappId("5511988887777@c.us");
        contato.setFinalTelefone("7777");
        contato.setNaoContatar(false);

        Reserva reserva = new Reserva();
        reserva.setId(100L);
        reserva.setEmpresa(empresa);
        reserva.setContato(contato);
        reserva.setData(hoje.plusDays(3));
        reserva.setHorario("14:00");
        reserva.setTitulo("Festa Aniversario");
        reserva.setSituacao(SituacaoReserva.AGUARDANDO_LEMBRETE);
        reserva.setOrigem(OrigemReserva.BOT);

        when(reservaRepository.findByEmpresaIdAndDataBetweenOrderByDataAscHorarioAsc(anyLong(), any(), any()))
                .thenReturn(List.of(reserva));

        mockMvc.perform(get("/empresas/sessao-automacao/reservas").param("dias", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].titulo").value("Festa Aniversario"))
                .andExpect(jsonPath("$[0].nomeCliente").value("Carlos Silva"))
                .andExpect(jsonPath("$[0].telefoneMascarado").value("(11) *****-7777"))
                .andExpect(jsonPath("$[0].podeAvisar").value(true))
                .andExpect(jsonPath("$[0].riscoPerda").value(false))
                .andExpect(jsonPath("$[0].precisaAtencao").value(false));

        verify(servicoSincronizacaoAgenda).sincronizarSeNecessario(empresa, 30);
    }

    @Test
    @DisplayName("Enviar lembrete para reserva inexistente retorna 404 Not Found")
    void enviarLembrete_reservaInexistente_retorna404() throws Exception {
        autenticarComoCliente();
        when(reservaRepository.findByIdAndEmpresaId(999L, 10L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/empresas/sessao-automacao/reservas/999/lembrete"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Enviar lembrete para reserva sem contato deve retornar 409 Conflict")
    void enviarLembrete_semContato_retorna409() throws Exception {
        autenticarComoCliente();

        Reserva reserva = new Reserva();
        reserva.setId(101L);
        reserva.setEmpresa(empresa);
        reserva.setContato(null);
        reserva.setData(LocalDate.now().plusDays(2));

        when(reservaRepository.findByIdAndEmpresaId(101L, 10L)).thenReturn(Optional.of(reserva));

        mockMvc.perform(post("/empresas/sessao-automacao/reservas/101/lembrete"))
                .andExpect(status().isConflict());

        verify(servicoLembreteReserva, never()).enviarLembrete(any(), any());
    }

    @Test
    @DisplayName("Enviar lembrete para contato que optou por nao contatar deve retornar 409 Conflict")
    void enviarLembrete_contatoNaoContatar_retorna409() throws Exception {
        autenticarComoCliente();

        Contato contato = new Contato();
        contato.setNaoContatar(true);

        Reserva reserva = new Reserva();
        reserva.setId(102L);
        reserva.setEmpresa(empresa);
        reserva.setContato(contato);
        reserva.setData(LocalDate.now().plusDays(2));

        when(reservaRepository.findByIdAndEmpresaId(102L, 10L)).thenReturn(Optional.of(reserva));

        mockMvc.perform(post("/empresas/sessao-automacao/reservas/102/lembrete"))
                .andExpect(status().isConflict());

        verify(servicoLembreteReserva, never()).enviarLembrete(any(), any());
    }

    @Test
    @DisplayName("Enviar lembrete para reserva em data passada deve retornar 409 Conflict")
    void enviarLembrete_dataPassada_retorna409() throws Exception {
        autenticarComoCliente();

        Contato contato = new Contato();
        contato.setNaoContatar(false);

        Reserva reserva = new Reserva();
        reserva.setId(103L);
        reserva.setEmpresa(empresa);
        reserva.setContato(contato);
        reserva.setData(LocalDate.now().minusDays(1));

        when(reservaRepository.findByIdAndEmpresaId(103L, 10L)).thenReturn(Optional.of(reserva));

        mockMvc.perform(post("/empresas/sessao-automacao/reservas/103/lembrete"))
                .andExpect(status().isConflict());

        verify(servicoLembreteReserva, never()).enviarLembrete(any(), any());
    }

    @Test
    @DisplayName("Falha no envio da mensagem via WhatsApp deve retornar 502 Bad Gateway")
    void enviarLembrete_falhaNoWhatsapp_retorna502() throws Exception {
        autenticarComoCliente();

        Contato contato = new Contato();
        contato.setNaoContatar(false);

        Reserva reserva = new Reserva();
        reserva.setId(104L);
        reserva.setEmpresa(empresa);
        reserva.setContato(contato);
        reserva.setData(LocalDate.now().plusDays(1));
        reserva.setSituacao(SituacaoReserva.AGUARDANDO_LEMBRETE);

        when(reservaRepository.findByIdAndEmpresaId(104L, 10L)).thenReturn(Optional.of(reserva));
        when(servicoLembreteReserva.enviarLembrete(any(), any())).thenReturn(false);

        mockMvc.perform(post("/empresas/sessao-automacao/reservas/104/lembrete"))
                .andExpect(status().isBadGateway());
    }

    @Test
    @DisplayName("Envio bem-sucedido de lembrete retorna 200 OK")
    void enviarLembrete_sucesso_retorna200() throws Exception {
        autenticarComoCliente();

        Contato contato = new Contato();
        contato.setId(8L);
        contato.setNome("Juliana");
        contato.setWhatsappId("5511977776666@c.us");
        contato.setFinalTelefone("6666");
        contato.setNaoContatar(false);

        Reserva reserva = new Reserva();
        reserva.setId(105L);
        reserva.setEmpresa(empresa);
        reserva.setContato(contato);
        reserva.setData(LocalDate.now().plusDays(2));
        reserva.setSituacao(SituacaoReserva.AGUARDANDO_LEMBRETE);

        when(reservaRepository.findByIdAndEmpresaId(105L, 10L)).thenReturn(Optional.of(reserva));
        when(servicoLembreteReserva.enviarLembrete(any(), any())).thenReturn(true);

        mockMvc.perform(post("/empresas/sessao-automacao/reservas/105/lembrete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(105))
                .andExpect(jsonPath("$.nomeCliente").value("Juliana"));
    }

    @Test
    @DisplayName("Vincular contato com payload vazio sem contatoId nem telefone retorna 400 Bad Request")
    void vincularContato_semDados_retorna400() throws Exception {
        autenticarComoCliente();

        Reserva reserva = new Reserva();
        reserva.setId(200L);
        reserva.setEmpresa(empresa);
        when(reservaRepository.findByIdAndEmpresaId(200L, 10L)).thenReturn(Optional.of(reserva));

        mockMvc.perform(put("/empresas/sessao-automacao/reservas/200/contato")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Vincular contato com telefone invalido retorna 400 Bad Request")
    void vincularContato_telefoneInvalido_retorna400() throws Exception {
        autenticarComoCliente();

        Reserva reserva = new Reserva();
        reserva.setId(201L);
        reserva.setEmpresa(empresa);
        when(reservaRepository.findByIdAndEmpresaId(201L, 10L)).thenReturn(Optional.of(reserva));

        mockMvc.perform(put("/empresas/sessao-automacao/reservas/201/contato")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"telefone-invalido\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Vincular contato com contatoId inexistente retorna 404 Not Found")
    void vincularContato_contatoIdInexistente_retorna404() throws Exception {
        autenticarComoCliente();

        Reserva reserva = new Reserva();
        reserva.setId(202L);
        reserva.setEmpresa(empresa);
        when(reservaRepository.findByIdAndEmpresaId(202L, 10L)).thenReturn(Optional.of(reserva));
        when(contatoRepository.findByIdAndEmpresaId(888L, 10L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/empresas/sessao-automacao/reservas/202/contato")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contatoId\":888}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Vincular contato com telefone valido cria/obtem contato e retorna 200 OK")
    void vincularContato_telefoneValido_retorna200() throws Exception {
        autenticarComoCliente();

        Reserva reserva = new Reserva();
        reserva.setId(203L);
        reserva.setEmpresa(empresa);
        reserva.setData(LocalDate.now().plusDays(5));
        when(reservaRepository.findByIdAndEmpresaId(203L, 10L)).thenReturn(Optional.of(reserva));

        Contato novoContato = new Contato();
        novoContato.setId(50L);
        novoContato.setNome("Marcos");
        novoContato.setWhatsappId("5511987654321@c.us");
        novoContato.setFinalTelefone("4321");
        when(servicoSincronizacaoAgenda.obterOuCriarContatoPorTelefone(any(), anyString())).thenReturn(novoContato);

        mockMvc.perform(put("/empresas/sessao-automacao/reservas/203/contato")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"(11) 98765-4321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contatoId").value(50))
                .andExpect(jsonPath("$.situacao").value("AGUARDANDO_LEMBRETE"));

        verify(reservaRepository).save(reserva);
    }

    @Test
    @DisplayName("Listar contatos retorna lista com mascaramento correto para diferentes formatos")
    void listarContatos_sucesso_retornaMascaraCorreta() throws Exception {
        autenticarComoCliente();

        Contato c1 = new Contato();
        c1.setId(1L);
        c1.setNome("LID User");
        c1.setWhatsappId("123456@lid");
        c1.setFinalTelefone("1234");
        c1.setOrigem(OrigemContato.CONVERSA);

        Contato c2 = new Contato();
        c2.setId(2L);
        c2.setNome("BR User");
        c2.setWhatsappId("5511999998888@c.us");
        c2.setFinalTelefone("8888");
        c2.setOrigem(OrigemContato.AGENDA);

        Contato c3 = new Contato();
        c3.setId(3L);
        c3.setNome("No 55 User");
        c3.setWhatsappId("11999994444@c.us");
        c3.setFinalTelefone("4444");
        c3.setOrigem(OrigemContato.CONVERSA);

        when(contatoRepository.findByEmpresaIdOrderByUltimaConversaEmDesc(10L))
                .thenReturn(List.of(c1, c2, c3));

        mockMvc.perform(get("/empresas/sessao-automacao/contatos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].telefoneMascarado").value("lid:*****"))
                .andExpect(jsonPath("$[1].telefoneMascarado").value("(11) *****-8888"))
                .andExpect(jsonPath("$[2].telefoneMascarado").value("(11) *****-4444"));
    }

    @Test
    @DisplayName("Obter numero completo quando limite horario excedido retorna 429 Too Many Requests")
    void obterNumeroCompleto_limiteExcedido_retorna429() throws Exception {
        autenticarComoCliente();

        when(auditoriaRepository.countByEmpresaIdAndDataHoraAfter(anyLong(), any()))
                .thenReturn(10L);

        mockMvc.perform(post("/empresas/sessao-automacao/contatos/1/numero"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Obter numero completo de contato inexistente retorna 404 Not Found")
    void obterNumeroCompleto_contatoInexistente_retorna404() throws Exception {
        autenticarComoCliente();

        when(auditoriaRepository.countByEmpresaIdAndDataHoraAfter(anyLong(), any()))
                .thenReturn(0L);
        when(contatoRepository.findByIdAndEmpresaId(999L, 10L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/empresas/sessao-automacao/contatos/999/numero"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Obter numero completo com sucesso registra auditoria e retorna apenas digitos")
    void obterNumeroCompleto_sucesso_registraAuditoriaERetornaNumero() throws Exception {
        autenticarComoCliente();

        Contato contato = new Contato();
        contato.setId(25L);
        contato.setNome("Renata");
        contato.setWhatsappId("5511998877665@c.us");

        when(auditoriaRepository.countByEmpresaIdAndDataHoraAfter(anyLong(), any()))
                .thenReturn(2L);
        when(contatoRepository.findByIdAndEmpresaId(25L, 10L)).thenReturn(Optional.of(contato));

        mockMvc.perform(post("/empresas/sessao-automacao/contatos/25/numero"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("5511998877665"));

        verify(auditoriaRepository).save(any(AuditoriaAcessoContato.class));
    }
}
