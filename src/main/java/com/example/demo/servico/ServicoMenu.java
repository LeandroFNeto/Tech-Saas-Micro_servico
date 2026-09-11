package com.example.demo.servico;

import com.example.demo.model.Empresa;
import com.example.demo.model.ModuloEmpresa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class ServicoMenu {

    @Transactional(readOnly = true)
    public String montarMenuPrincipal(Empresa empresa) {
        StringBuilder menu = new StringBuilder();
        String saudacao = empresa.getMensagemSaudacao();
        menu.append(saudacao == null || saudacao.isBlank() ? "Olá!" : saudacao).append("\n\n");
        menu.append("👉 *Como posso te ajudar hoje?*\n(_Digite apenas o número da opção desejada_):\n");

        int numeroOpcao = 1;
        for (ModuloEmpresa modulo : listarAtivosOrdenados(empresa)) {
            menu.append(numeroOpcao).append("️⃣ - ").append(modulo.getTextoMenu()).append("\n");
            numeroOpcao++;
        }

        menu.append("\n💡 *Dica:* Para pesquisar disponibilidade, escolha a opção correspondente e digite a data no formato *DD/MM/AAAA* (Ex: 25/12/2026).");
        menu.append("\n🔄 *Lembrete:* Digite *0* a qualquer momento da conversa para voltar a este menu inicial.");
        return menu.toString();
    }

    @Transactional(readOnly = true)
    public String descobrirAcao(Empresa empresa, String numeroDigitado) {
        try {
            int numero = Integer.parseInt(numeroDigitado.trim());
            List<ModuloEmpresa> ativos = listarAtivosOrdenados(empresa);
            if (numero < 1 || numero > ativos.size()) {
                return "OPCAO_INVALIDA";
            }
            return ativos.get(numero - 1).getCodigoAcao();
        } catch (NumberFormatException | NullPointerException e) {
            return "OPCAO_INVALIDA";
        }
    }

    public List<ModuloEmpresa> listarAtivosOrdenados(Empresa empresa) {
        if (empresa == null || empresa.getModulosAtivos() == null) {
            return List.of();
        }
        return empresa.getModulosAtivos().stream()
                .filter(modulo -> Boolean.TRUE.equals(modulo.getAtivo()))
                .sorted(Comparator.comparing(ModuloEmpresa::getOrdemExibicao, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }
}
