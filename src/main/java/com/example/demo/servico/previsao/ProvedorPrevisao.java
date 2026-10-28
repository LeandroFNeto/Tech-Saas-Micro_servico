package com.example.demo.servico.previsao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProvedorPrevisao {

    Optional<Localizacao> geocodificar(String cidade);

    List<DiaBruto> previsaoDiaria(double latitude, double longitude);

    record Localizacao(double latitude, double longitude, String nomeResolvido) {
    }

    record DiaBruto(
            LocalDate data,
            String resumo,
            int codigoTempo,
            double tempMax,
            double tempMin,
            double probabilidadeChuva,
            double chuvaMm) {
    }
}
