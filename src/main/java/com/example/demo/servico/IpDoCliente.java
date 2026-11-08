package com.example.demo.servico;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@Component
public class IpDoCliente {

    private static final Logger log = LoggerFactory.getLogger(IpDoCliente.class);
    private static final String CABECALHO = "X-Forwarded-For";

    private final List<Faixa> proxiesConfiaveis;

    public IpDoCliente(@Value("${seguranca.proxies-confiaveis:}") String proxiesConfiaveis) {
        this.proxiesConfiaveis = interpretar(proxiesConfiaveis);
    }

    public String resolver(HttpServletRequest requisicao) {
        String remoto = requisicao.getRemoteAddr();
        if (remoto == null || proxiesConfiaveis.isEmpty() || !confiavel(remoto)) {
            return remoto;
        }
        String encaminhado = requisicao.getHeader(CABECALHO);
        if (encaminhado == null || encaminhado.isBlank()) {
            return remoto;
        }
        String[] cadeia = encaminhado.split(",");
        String candidato = null;
        for (int i = cadeia.length - 1; i >= 0; i--) {
            String endereco = cadeia[i].trim();
            if (!literalDeIp(endereco)) {
                return remoto;
            }
            candidato = endereco;
            if (!confiavel(endereco)) {
                return endereco;
            }
        }
        return candidato == null ? remoto : candidato;
    }

    private boolean confiavel(String endereco) {
        byte[] bytes = bytesDe(endereco);
        if (bytes == null) {
            return false;
        }
        for (Faixa faixa : proxiesConfiaveis) {
            if (faixa.contem(bytes)) {
                return true;
            }
        }
        return false;
    }

    private static List<Faixa> interpretar(String configuracao) {
        List<Faixa> faixas = new ArrayList<>();
        if (configuracao == null) {
            return faixas;
        }
        for (String item : configuracao.split(",")) {
            String limpo = item.trim();
            if (limpo.isEmpty()) {
                continue;
            }
            String endereco = limpo;
            Integer prefixo = null;
            int barra = limpo.indexOf('/');
            if (barra >= 0) {
                endereco = limpo.substring(0, barra);
                try {
                    prefixo = Integer.parseInt(limpo.substring(barra + 1));
                } catch (NumberFormatException e) {
                    log.warn("Entrada inválida em PROXIES_CONFIAVEIS ignorada.");
                    continue;
                }
            }
            byte[] bytes = bytesDe(endereco);
            if (bytes == null || (prefixo != null && (prefixo < 0 || prefixo > bytes.length * 8))) {
                log.warn("Entrada inválida em PROXIES_CONFIAVEIS ignorada.");
                continue;
            }
            faixas.add(new Faixa(bytes, prefixo == null ? bytes.length * 8 : prefixo));
        }
        return faixas;
    }

    private static boolean literalDeIp(String texto) {
        return bytesDe(texto) != null;
    }

    private static byte[] bytesDe(String texto) {
        if (texto == null || texto.isEmpty() || !texto.matches("[0-9a-fA-F:.]+") || !(texto.contains(".") || texto.contains(":"))) {
            return null;
        }
        try {
            return InetAddress.getByName(texto).getAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private record Faixa(byte[] base, int prefixo) {

        boolean contem(byte[] endereco) {
            if (endereco.length != base.length) {
                return false;
            }
            int bytesInteiros = prefixo / 8;
            for (int i = 0; i < bytesInteiros; i++) {
                if (endereco[i] != base[i]) {
                    return false;
                }
            }
            int bitsRestantes = prefixo % 8;
            if (bitsRestantes == 0) {
                return true;
            }
            int mascara = 0xFF << (8 - bitsRestantes) & 0xFF;
            return (endereco[bytesInteiros] & mascara) == (base[bytesInteiros] & mascara);
        }
    }
}
