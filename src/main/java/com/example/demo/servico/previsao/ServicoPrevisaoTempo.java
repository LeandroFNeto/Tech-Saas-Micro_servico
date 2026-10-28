package com.example.demo.servico.previsao;

import com.example.demo.dto.DiaReservaDTO;
import com.example.demo.dto.previsao.PrevisaoDiaDTO;
import com.example.demo.dto.previsao.PrevisaoTempoResponseDTO;
import com.example.demo.model.Empresa;
import com.example.demo.servico.ServicoReserva;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ServicoPrevisaoTempo {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    public static final Duration VALIDADE_CACHE = Duration.ofHours(8);
    public static final int TETO_CHAMADAS_POR_CIDADE_DIA = 3;
    public static final int LIMITE_TROCAS_CIDADE_DIA = 5;
    public static final int MAXIMO_DIAS = 8;
    public static final int PRIMEIRO_DIA_ESTIMATIVA = 5;

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter FORMATO_RESERVA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ProvedorPrevisao provedor;
    private final ServicoReserva servicoReserva;
    private final Clock relogio;

    private final ConcurrentHashMap<String, EntradaCache> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ContadorDia> chamadasPorCidade = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ContadorDia> trocasPorEmpresa = new ConcurrentHashMap<>();

    @Autowired
    public ServicoPrevisaoTempo(ProvedorPrevisao provedor, ServicoReserva servicoReserva) {
        this(provedor, servicoReserva, Clock.system(FUSO));
    }

    public ServicoPrevisaoTempo(ProvedorPrevisao provedor, ServicoReserva servicoReserva, Clock relogio) {
        this.provedor = provedor;
        this.servicoReserva = servicoReserva;
        this.relogio = relogio.withZone(FUSO);
    }

    public Optional<ProvedorPrevisao.Localizacao> geocodificar(String cidade) {
        return provedor.geocodificar(cidade);
    }

    public boolean consumirTrocaDeCidade(Long empresaId) {
        LocalDate hoje = LocalDate.now(relogio);
        ContadorDia contador = trocasPorEmpresa.compute(empresaId, (id, atual) ->
                atual == null || !atual.dia().equals(hoje) ? new ContadorDia(hoje, 1) : new ContadorDia(hoje, atual.total() + 1));
        return contador.total() <= LIMITE_TROCAS_CIDADE_DIA;
    }

    public void estornarTrocaDeCidade(Long empresaId) {
        LocalDate hoje = LocalDate.now(relogio);
        trocasPorEmpresa.computeIfPresent(empresaId, (id, atual) ->
                atual.dia().equals(hoje) && atual.total() > 0 ? new ContadorDia(hoje, atual.total() - 1) : atual);
    }

    public PrevisaoTempoResponseDTO consultar(Empresa empresa) {
        boolean semCidade = empresa.getCidadePrevisao() == null || empresa.getCidadePrevisao().isBlank()
                || empresa.getLatitude() == null || empresa.getLongitude() == null;
        if (!Boolean.TRUE.equals(empresa.getPrevisaoTempoAtiva()) || semCidade) {
            return indisponivel(true, empresa.getCidadePrevisaoResolvida());
        }

        EntradaCache entrada = obterEntrada(empresa.getLatitude(), empresa.getLongitude());
        if (entrada == null) {
            return indisponivel(true, empresa.getCidadePrevisaoResolvida());
        }

        boolean reservasDisponiveis = true;
        Set<LocalDate> diasReservados = new HashSet<>();
        String calendarId = empresa.getGoogleCalendarId();
        if (calendarId != null && !calendarId.isBlank()) {
            List<DiaReservaDTO> reservas = servicoReserva.listarReservas(empresa);
            if (reservas == null) {
                reservasDisponiveis = false;
            } else {
                for (DiaReservaDTO reserva : reservas) {
                    if ("Alugado".equals(reserva.status())) {
                        diasReservados.add(LocalDate.parse(reserva.data(), FORMATO_RESERVA));
                    }
                }
            }
        }

        LocalDate hoje = LocalDate.now(relogio);
        List<ProvedorPrevisao.DiaBruto> futuros = entrada.dias().stream()
                .filter(dia -> !dia.data().isBefore(hoje))
                .limit(MAXIMO_DIAS)
                .toList();
        List<PrevisaoDiaDTO> dias = new ArrayList<>();
        for (int i = 0; i < futuros.size(); i++) {
            ProvedorPrevisao.DiaBruto dia = futuros.get(i);
            dias.add(new PrevisaoDiaDTO(
                    dia.data(),
                    dia.data().getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR),
                    diasReservados.contains(dia.data()),
                    dia.resumo(),
                    dia.codigoTempo(),
                    dia.tempMax(),
                    dia.tempMin(),
                    CalculadoraRiscoDesmarcacao.arredondarChance(dia.probabilidadeChuva()),
                    CalculadoraRiscoDesmarcacao.arredondarChuva(dia.chuvaMm()),
                    CalculadoraRiscoDesmarcacao.classificar(dia.probabilidadeChuva(), dia.chuvaMm(), dia.codigoTempo()).name(),
                    i >= PRIMEIRO_DIA_ESTIMATIVA));
        }

        return new PrevisaoTempoResponseDTO(
                true,
                reservasDisponiveis,
                LocalDateTime.ofInstant(entrada.obtidaEm(), FUSO),
                empresa.getCidadePrevisaoResolvida(),
                List.copyOf(dias));
    }

    private EntradaCache obterEntrada(double latitude, double longitude) {
        String chave = chaveCoordenada(latitude, longitude);
        Instant agora = relogio.instant();
        EntradaCache existente = cache.get(chave);
        if (existente != null && Duration.between(existente.obtidaEm(), agora).compareTo(VALIDADE_CACHE) < 0) {
            return existente;
        }
        if (!reservarChamada(chave)) {
            return existente;
        }
        try {
            List<ProvedorPrevisao.DiaBruto> dias = provedor.previsaoDiaria(latitude, longitude);
            EntradaCache nova = new EntradaCache(List.copyOf(dias), agora);
            cache.put(chave, nova);
            return nova;
        } catch (RuntimeException falha) {
            return existente;
        }
    }

    private boolean reservarChamada(String chave) {
        LocalDate hoje = LocalDate.now(relogio);
        boolean[] permitido = new boolean[1];
        chamadasPorCidade.compute(chave, (k, atual) -> {
            ContadorDia base = atual == null || !atual.dia().equals(hoje) ? new ContadorDia(hoje, 0) : atual;
            if (base.total() >= TETO_CHAMADAS_POR_CIDADE_DIA) {
                permitido[0] = false;
                return base;
            }
            permitido[0] = true;
            return new ContadorDia(hoje, base.total() + 1);
        });
        return permitido[0];
    }

    private static String chaveCoordenada(double latitude, double longitude) {
        return String.format(Locale.ROOT, "%.2f,%.2f", latitude, longitude);
    }

    private static PrevisaoTempoResponseDTO indisponivel(boolean reservasDisponiveis, String cidade) {
        return new PrevisaoTempoResponseDTO(false, reservasDisponiveis, null, cidade, List.of());
    }

    private record EntradaCache(List<ProvedorPrevisao.DiaBruto> dias, Instant obtidaEm) {
    }

    private record ContadorDia(LocalDate dia, int total) {
    }
}
