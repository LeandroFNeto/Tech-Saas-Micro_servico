package com.example.demo.servico;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class GuardaCotaIA {

    private static final Logger log = LoggerFactory.getLogger(GuardaCotaIA.class);
    private static final ZoneId FUSO_BRASILIA = ZoneId.of("America/Sao_Paulo");

    public enum Situacao { LIVRE, PAUSADA_APOS_429, TETO_GASTO_DIARIO, TETO_CHAMADAS_DIA, TETO_CHAMADAS_MINUTO }

    private final int maximoPorMinuto;
    private final int maximoPorDia;
    private final double tetoGastoDiarioReais;
    private final double precoEntradaUsdPorMilhao;
    private final double precoSaidaUsdPorMilhao;
    private final double cotacaoUsdBrl;
    private final double custoEstimadoPorChamadaReais;
    private final long pausaAposCotaSegundos;
    private final Clock relogio;

    private LocalDate diaAtual;
    private int chamadasNoDia;
    private double gastoNoDia;
    private long minutoAtual = -1;
    private int chamadasNoMinuto;
    private Instant pausadoAte;
    private boolean avisouTetoDoDia;

    @Autowired
    public GuardaCotaIA(
            @Value("${gemini.max-chamadas-por-minuto:15}") int maximoPorMinuto,
            @Value("${gemini.max-chamadas-por-dia:3000}") int maximoPorDia,
            @Value("${gemini.teto-gasto-diario-reais:15.0}") double tetoGastoDiarioReais,
            @Value("${gemini.preco-entrada-usd-por-milhao:0.30}") double precoEntradaUsdPorMilhao,
            @Value("${gemini.preco-saida-usd-por-milhao:2.50}") double precoSaidaUsdPorMilhao,
            @Value("${gemini.cotacao-usd-brl:5.50}") double cotacaoUsdBrl,
            @Value("${gemini.custo-estimado-chamada-reais:0.005}") double custoEstimadoPorChamadaReais,
            @Value("${gemini.pausa-apos-429-segundos:300}") long pausaAposCotaSegundos) {
        this(maximoPorMinuto, maximoPorDia, tetoGastoDiarioReais, precoEntradaUsdPorMilhao, precoSaidaUsdPorMilhao,
                cotacaoUsdBrl, custoEstimadoPorChamadaReais, pausaAposCotaSegundos, Clock.system(FUSO_BRASILIA));
    }

    GuardaCotaIA(int maximoPorMinuto, int maximoPorDia, double tetoGastoDiarioReais, double precoEntradaUsdPorMilhao,
                 double precoSaidaUsdPorMilhao, double cotacaoUsdBrl, double custoEstimadoPorChamadaReais,
                 long pausaAposCotaSegundos, Clock relogio) {
        this.maximoPorMinuto = maximoPorMinuto;
        this.maximoPorDia = maximoPorDia;
        this.tetoGastoDiarioReais = tetoGastoDiarioReais;
        this.precoEntradaUsdPorMilhao = precoEntradaUsdPorMilhao;
        this.precoSaidaUsdPorMilhao = precoSaidaUsdPorMilhao;
        this.cotacaoUsdBrl = cotacaoUsdBrl;
        this.custoEstimadoPorChamadaReais = custoEstimadoPorChamadaReais;
        this.pausaAposCotaSegundos = pausaAposCotaSegundos;
        this.relogio = relogio;
    }

    public synchronized boolean reservar() {
        Situacao atual = situacaoAgora();
        if (atual == Situacao.TETO_GASTO_DIARIO && !avisouTetoDoDia) {
            avisouTetoDoDia = true;
            log.warn("Teto diario de gasto da IA atingido; modelo suspenso ate o proximo dia");
        }
        if (atual != Situacao.LIVRE) {
            return false;
        }
        chamadasNoMinuto++;
        chamadasNoDia++;
        return true;
    }

    public synchronized Situacao situacao() {
        return situacaoAgora();
    }

    private Situacao situacaoAgora() {
        Instant agora = relogio.instant();
        virarDiaSeNecessario();
        if (pausadoAte != null && agora.isBefore(pausadoAte)) {
            return Situacao.PAUSADA_APOS_429;
        }
        if (gastoNoDia >= tetoGastoDiarioReais) {
            return Situacao.TETO_GASTO_DIARIO;
        }
        if (chamadasNoDia >= maximoPorDia) {
            return Situacao.TETO_CHAMADAS_DIA;
        }
        long minuto = agora.getEpochSecond() / 60;
        if (minuto != minutoAtual) {
            minutoAtual = minuto;
            chamadasNoMinuto = 0;
        }
        if (chamadasNoMinuto >= maximoPorMinuto) {
            return Situacao.TETO_CHAMADAS_MINUTO;
        }
        return Situacao.LIVRE;
    }

    public synchronized void registrarUso(Integer tokensEntrada, Integer tokensSaida) {
        virarDiaSeNecessario();
        if (tokensEntrada == null || tokensSaida == null) {
            gastoNoDia += custoEstimadoPorChamadaReais;
            return;
        }
        double usd = (tokensEntrada * precoEntradaUsdPorMilhao + tokensSaida * precoSaidaUsdPorMilhao) / 1_000_000d;
        gastoNoDia += usd * cotacaoUsdBrl;
    }

    public synchronized void registrarChamadaSemResposta() {
        virarDiaSeNecessario();
        gastoNoDia += custoEstimadoPorChamadaReais;
    }

    public synchronized void registrarCotaEsgotada() {
        pausadoAte = relogio.instant().plusSeconds(pausaAposCotaSegundos);
    }

    synchronized double gastoDoDiaReais() {
        virarDiaSeNecessario();
        return gastoNoDia;
    }

    private void virarDiaSeNecessario() {
        LocalDate hoje = LocalDate.now(relogio);
        if (!hoje.equals(diaAtual)) {
            diaAtual = hoje;
            chamadasNoDia = 0;
            gastoNoDia = 0;
            avisouTetoDoDia = false;
        }
    }
}
