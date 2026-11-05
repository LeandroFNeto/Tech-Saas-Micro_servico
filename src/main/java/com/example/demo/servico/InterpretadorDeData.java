package com.example.demo.servico;

import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InterpretadorDeData {

    public sealed interface Resultado permits Certa, Duvidosa, Nenhuma {
    }

    public record Certa(LocalDate data) implements Resultado {
    }

    public record Duvidosa(LocalDate sugestao) implements Resultado {
    }

    public record Nenhuma() implements Resultado {
    }

    private static final Map<String, Integer> MESES = Map.ofEntries(
            Map.entry("janeiro", 1), Map.entry("jan", 1),
            Map.entry("fevereiro", 2), Map.entry("fev", 2),
            Map.entry("marco", 3), Map.entry("mar", 3),
            Map.entry("abril", 4), Map.entry("abr", 4),
            Map.entry("maio", 5), Map.entry("mai", 5),
            Map.entry("junho", 6), Map.entry("jun", 6),
            Map.entry("julho", 7), Map.entry("jul", 7),
            Map.entry("agosto", 8), Map.entry("ago", 8),
            Map.entry("setembro", 9), Map.entry("set", 9),
            Map.entry("outubro", 10), Map.entry("out", 10),
            Map.entry("novembro", 11), Map.entry("nov", 11),
            Map.entry("dezembro", 12), Map.entry("dez", 12));

    private static final Map<String, DayOfWeek> DIAS_DA_SEMANA = Map.of(
            "segunda", DayOfWeek.MONDAY,
            "terca", DayOfWeek.TUESDAY,
            "quarta", DayOfWeek.WEDNESDAY,
            "quinta", DayOfWeek.THURSDAY,
            "sexta", DayOfWeek.FRIDAY,
            "sabado", DayOfWeek.SATURDAY,
            "domingo", DayOfWeek.SUNDAY);

    private static final Pattern ISO = Pattern.compile("(?<!\\d)(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})(?!\\d)");
    private static final Pattern NUMERICA = Pattern.compile(
            "(?<![\\d:])(\\d{1,2})\\s*[/\\-.]\\s*(\\d{1,2})(?:\\s*[/\\-.]\\s*(\\d{4}|\\d{2}))?(?![\\d:])");
    private static final Pattern NUMERICA_COM_ESPACO = Pattern.compile(
            "(?<![\\d:])(\\d{1,2})\\s+(\\d{1,2})\\s+(\\d{4}|\\d{2})(?![\\d:])");
    private static final Pattern COM_NOME_DO_MES = Pattern.compile(
            "(?<!\\d)(\\d{1,2})\\s*(?:o\\s+)?(?:de\\s+)?([a-z]{3,9})\\.?(?:\\s*(?:de\\s+)?(\\d{4}))?(?!\\d)");
    private static final Pattern DIA_E_MES_POR_NUMERO = Pattern.compile(
            "\\bdia\\s+(\\d{1,2})\\s+(\\d{1,2})(?:\\s+(\\d{4}|\\d{2}))?(?!\\d)");
    private static final Pattern SO_O_DIA = Pattern.compile("\\bdia\\s+(\\d{1,2})(?![\\d/\\-.:])");

    public Resultado interpretar(String texto, LocalDate hoje) {
        if (texto == null || texto.isBlank() || hoje == null) {
            return new Nenhuma();
        }
        String t = normalizar(texto);

        Matcher iso = ISO.matcher(t);
        if (iso.find()) {
            return montar(inteiro(iso.group(3)), inteiro(iso.group(2)), inteiro(iso.group(1)), hoje);
        }

        Matcher numerica = NUMERICA.matcher(t);
        while (numerica.find()) {
            int dia = inteiro(numerica.group(1));
            int mes = inteiro(numerica.group(2));
            if (mes < 1 || mes > 12 || dia < 1 || dia > 31) {
                if (numerica.group(3) != null) {
                    return new Duvidosa(null);
                }
                continue;
            }
            return montar(dia, mes, ano(numerica.group(3)), hoje);
        }

        Matcher comNome = COM_NOME_DO_MES.matcher(t);
        while (comNome.find()) {
            Integer mes = MESES.get(comNome.group(2));
            if (mes == null) {
                continue;
            }
            Integer ano = comNome.group(3) == null ? null : inteiro(comNome.group(3));
            return montar(inteiro(comNome.group(1)), mes, ano, hoje);
        }

        Matcher diaEMes = DIA_E_MES_POR_NUMERO.matcher(t);
        if (diaEMes.find()) {
            return montar(inteiro(diaEMes.group(1)), inteiro(diaEMes.group(2)), ano(diaEMes.group(3)), hoje);
        }

        Matcher espaco = NUMERICA_COM_ESPACO.matcher(t);
        if (espaco.find()) {
            return montar(inteiro(espaco.group(1)), inteiro(espaco.group(2)), ano(espaco.group(3)), hoje);
        }

        Matcher soODia = SO_O_DIA.matcher(t);
        if (soODia.find()) {
            return proximoDia(inteiro(soODia.group(1)), hoje);
        }

        if (t.contains("depois de amanha")) {
            return new Certa(hoje.plusDays(2));
        }
        if (t.contains("amanha")) {
            return new Certa(hoje.plusDays(1));
        }
        if (Pattern.compile("\\bhoje\\b").matcher(t).find()) {
            return new Certa(hoje);
        }

        for (Map.Entry<String, DayOfWeek> dia : DIAS_DA_SEMANA.entrySet()) {
            if (Pattern.compile("\\b" + dia.getKey() + "\\b").matcher(t).find()) {
                return new Duvidosa(proximoDiaDaSemana(dia.getValue(), hoje));
            }
        }
        return new Nenhuma();
    }

    private Resultado montar(int dia, int mes, Integer ano, LocalDate hoje) {
        if (mes < 1 || mes > 12 || dia < 1 || dia > 31) {
            return new Duvidosa(null);
        }
        try {
            if (ano != null) {
                return new Certa(LocalDate.of(ano, mes, dia));
            }
            LocalDate neste = LocalDate.of(hoje.getYear(), mes, dia);
            if (!neste.isBefore(hoje)) {
                return new Certa(neste);
            }
            return new Duvidosa(LocalDate.of(hoje.getYear() + 1, mes, dia));
        } catch (DateTimeException e) {
            return new Duvidosa(null);
        }
    }

    private Resultado proximoDia(int dia, LocalDate hoje) {
        if (dia < 1 || dia > 31) {
            return new Duvidosa(null);
        }
        LocalDate candidato = hoje.withDayOfMonth(1);
        for (int i = 0; i < 14; i++) {
            if (dia <= candidato.lengthOfMonth()) {
                LocalDate data = candidato.withDayOfMonth(dia);
                if (!data.isBefore(hoje)) {
                    return new Duvidosa(data);
                }
            }
            candidato = candidato.plusMonths(1);
        }
        return new Duvidosa(null);
    }

    private LocalDate proximoDiaDaSemana(DayOfWeek alvo, LocalDate hoje) {
        int diferenca = (alvo.getValue() - hoje.getDayOfWeek().getValue() + 7) % 7;
        return hoje.plusDays(diferenca == 0 ? 7 : diferenca);
    }

    private Integer ano(String texto) {
        if (texto == null) {
            return null;
        }
        int valor = inteiro(texto);
        return texto.length() <= 2 ? 2000 + valor : valor;
    }

    private int inteiro(String texto) {
        return Integer.parseInt(texto);
    }

    private String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).replace(",", " ").replaceAll("\\s+", " ").trim();
    }
}
