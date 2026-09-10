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
        TEXTOS.put("VER_LOCALIZACAO", "Ver localização");
        TEXTOS.put("VER_REGRAS", "Ver regras e cancelamento");
        TEXTOS.put("SOLICITAR_RESERVA", "Solicitar uma reserva");
        TEXTOS.put("PESQUISAR_DATA", "Pesquisar data disponível");
        TEXTOS.put("MENU_CARDAPIO", "Ver preços");
    }

    public static final List<String> CODIGOS_MENU_BOT = List.of(
            "VER_FOTOS", "VER_LOCALIZACAO", "MENU_CARDAPIO", "VER_REGRAS", "SOLICITAR_RESERVA");

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

    public static boolean isCodigoMenuBot(String codigo) {
        return codigo != null && CODIGOS_MENU_BOT.contains(codigo.trim());
    }

    public static String textoPadrao(String codigo) {
        if (codigo == null) {
            return "";
        }
        return TEXTOS.getOrDefault(codigo.trim(), codigo.trim());
    }

    /**
     * Atualiza texto e ativo de um item do menu do bot sem apagar módulos de infraestrutura.
     */
    public static void upsertItemMenu(Empresa empresa, String codigoAcao, String textoMenu, Boolean ativo) {
        if (!isCodigoMenuBot(codigoAcao)) {
            return;
        }
        String chave = codigoAcao.trim();
        if (empresa.getModulosAtivos() == null) {
            empresa.setModulosAtivos(new ArrayList<>());
        }

        ModuloEmpresa existente = empresa.getModulosAtivos().stream()
                .filter(modulo -> chave.equals(modulo.getCodigoAcao()))
                .findFirst()
                .orElse(null);

        if (existente == null) {
            ModuloEmpresa modulo = new ModuloEmpresa();
            modulo.setEmpresa(empresa);
            modulo.setCodigoAcao(chave);
            modulo.setTextoMenu(textoMenu == null || textoMenu.isBlank() ? textoPadrao(chave) : textoMenu.trim());
            modulo.setOrdemExibicao(CODIGOS_MENU_BOT.indexOf(chave) + 1);
            modulo.setAtivo(ativo == null || ativo);
            empresa.getModulosAtivos().add(modulo);
            return;
        }

        if (textoMenu != null) {
            existente.setTextoMenu(textoMenu.isBlank() ? textoPadrao(chave) : textoMenu.trim());
        }
        if (ativo != null) {
            existente.setAtivo(ativo);
        }
    }
}
