package com.example.demo.servico;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CalendarioFeriados {

    private static final Map<MonthDay, String> NACIONAIS_FIXOS = new LinkedHashMap<>();
    private static final Map<String, Map<MonthDay, String>> ESTADUAIS = new LinkedHashMap<>();

    static {
        NACIONAIS_FIXOS.put(MonthDay.of(1, 1), "Confraternização Universal");
        NACIONAIS_FIXOS.put(MonthDay.of(4, 21), "Tiradentes");
        NACIONAIS_FIXOS.put(MonthDay.of(5, 1), "Dia do Trabalho");
        NACIONAIS_FIXOS.put(MonthDay.of(9, 7), "Independência do Brasil");
        NACIONAIS_FIXOS.put(MonthDay.of(10, 12), "Nossa Senhora Aparecida");
        NACIONAIS_FIXOS.put(MonthDay.of(11, 2), "Finados");
        NACIONAIS_FIXOS.put(MonthDay.of(11, 15), "Proclamação da República");
        NACIONAIS_FIXOS.put(MonthDay.of(12, 25), "Natal");

        estadual("AC", 6, 15, "Aniversário do Acre");
        estadual("AC", 8, 6, "Início da Revolução Acreana");
        estadual("AC", 11, 17, "Tratado de Petrópolis");
        estadual("AL", 9, 16, "Emancipação Política de Alagoas");
        estadual("AM", 9, 5, "Elevação do Amazonas a Província");
        estadual("AP", 9, 13, "Criação do Território Federal do Amapá");
        estadual("BA", 7, 2, "Independência da Bahia");
        estadual("CE", 3, 25, "Data Magna do Ceará");
        estadual("DF", 11, 30, "Dia do Evangélico");
        estadual("MA", 7, 28, "Adesão do Maranhão à Independência");
        estadual("MS", 10, 11, "Criação do Estado de Mato Grosso do Sul");
        estadual("PA", 8, 15, "Adesão do Grão-Pará à Independência");
        estadual("PB", 8, 5, "Fundação do Estado da Paraíba");
        estadual("PE", 3, 6, "Revolução Pernambucana");
        estadual("PI", 10, 19, "Dia do Piauí");
        estadual("PR", 12, 19, "Emancipação Política do Paraná");
        estadual("RJ", 4, 23, "São Jorge");
        estadual("RN", 10, 3, "Mártires de Cunhaú e Uruaçu");
        estadual("RO", 1, 4, "Criação do Estado de Rondônia");
        estadual("RR", 10, 5, "Criação do Estado de Roraima");
        estadual("RS", 9, 20, "Revolução Farroupilha");
        estadual("SC", 8, 11, "Dia do Estado de Santa Catarina");
        estadual("SE", 7, 8, "Autonomia de Sergipe");
        estadual("SP", 7, 9, "Revolução Constitucionalista");
        estadual("TO", 10, 5, "Criação do Estado do Tocantins");
    }

    private CalendarioFeriados() {
    }

    public static final List<String> UFS = List.of(
            "AC", "AL", "AM", "AP", "BA", "CE", "DF", "ES", "GO", "MA", "MG", "MS", "MT", "PA", "PB", "PE",
            "PI", "PR", "RJ", "RN", "RO", "RR", "RS", "SC", "SE", "SP", "TO");

    public static boolean ufValida(String uf) {
        return uf != null && UFS.contains(uf.toUpperCase());
    }

    public static Optional<String> nomeDoFeriado(String uf, LocalDate data) {
        Optional<String> nacional = nomeNacional(data);
        if (nacional.isPresent()) {
            return nacional;
        }
        if (uf == null || uf.isBlank()) {
            return Optional.empty();
        }
        Map<MonthDay, String> doEstado = ESTADUAIS.get(uf.toUpperCase());
        if (doEstado == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(doEstado.get(MonthDay.from(data)));
    }

    public static Optional<String> nomeNacional(LocalDate data) {
        String fixo = NACIONAIS_FIXOS.get(MonthDay.from(data));
        if (fixo != null) {
            return Optional.of(fixo);
        }
        if (data.getYear() >= 2024 && data.getMonthValue() == 11 && data.getDayOfMonth() == 20) {
            return Optional.of("Consciência Negra");
        }
        LocalDate pascoa = pascoa(data.getYear());
        if (data.equals(pascoa)) {
            return Optional.of("Páscoa");
        }
        if (data.equals(pascoa.minusDays(2))) {
            return Optional.of("Sexta-feira Santa");
        }
        if (data.equals(pascoa.minusDays(47))) {
            return Optional.of("Carnaval");
        }
        if (data.equals(pascoa.plusDays(60))) {
            return Optional.of("Corpus Christi");
        }
        return Optional.empty();
    }

    public static LocalDate pascoa(int ano) {
        int a = ano % 19;
        int b = ano / 100;
        int c = ano % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(ano, mes, dia);
    }

    private static void estadual(String uf, int mes, int dia, String nome) {
        ESTADUAIS.computeIfAbsent(uf, ignorado -> new LinkedHashMap<>()).put(MonthDay.of(mes, dia), nome);
    }
}
