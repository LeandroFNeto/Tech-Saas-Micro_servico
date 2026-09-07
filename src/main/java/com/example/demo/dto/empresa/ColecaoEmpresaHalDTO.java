package com.example.demo.dto.empresa;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Coleção HAL de GET /empresas (_embedded.empresas). */
public record ColecaoEmpresaHalDTO(
        @JsonProperty("_embedded") Embedded embedded
) {
    public record Embedded(List<EmpresaPainelDTO> empresas) {
    }
}
