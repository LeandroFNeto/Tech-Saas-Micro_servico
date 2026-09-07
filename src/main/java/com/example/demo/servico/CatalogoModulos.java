package com.example.demo.servico;

import com.example.demo.model.Empresa;
import com.example.demo.model.ModuloEmpresa;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo dos códigos enviados em modulosIniciais / modulosAtivos.
 * Preenche texto de menu e usaIA sem expor a entidade no JSON.
 */
public final class CatalogoModulos {

    private static final Map<String, String> TEXTOS = new LinkedHashMap<>();

    static {
        TEXTOS.put("IA_GEMINI", "Falar com atendente virtual");
        TEXTOS.put("GOOGLE_CALENDAR", "Consultar agenda");
        TEXTOS.put("VER_FOTOS", "Ver fotos do espaço");
        TEXTOS.put("PESQUISAR_DATA", "Pesquisar data disponível");
        TEXTOS.put("MENU_CARDAPIO", "Ver cardápio");
    }

    private CatalogoModulos() {
    }

    public static void aplicar(Empresa empresa, List<String> codigos) {
        if (codigos == null) {
            return;
        }
        if (empresa.getModulosAtivos() == null) {
            empresa.setModulosAtivos(new ArrayList<>());
        } else {
            empresa.getModulosAtivos().clear();
        }

        int ordem = 1;
        for (String codigo : codigos) {
            if (codigo == null || codigo.isBlank()) {
                continue;
            }
            String chave = codigo.trim();
            ModuloEmpresa modulo = new ModuloEmpresa();
            modulo.setEmpresa(empresa);
            modulo.setCodigoAcao(chave);
            modulo.setTextoMenu(TEXTOS.getOrDefault(chave, chave));
            modulo.setOrdemExibicao(ordem++);
            modulo.setAtivo(true);
            empresa.getModulosAtivos().add(modulo);
        }

        empresa.setUsaIA(codigos.stream().anyMatch(c -> "IA_GEMINI".equals(c)));
    }
}
