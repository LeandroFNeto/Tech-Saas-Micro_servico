package com.example.demo.config;

import com.example.demo.dto.whatsapp.SessaoStatusResponseDTO;
import com.example.demo.model.Empresa;
import com.example.demo.repository.EmpresaRepository;
import com.example.demo.servico.ServicoWppConnect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RestauradorSessoesWhatsappTest {

    @Test
    void reabreSessoesComTokenSalvo() {
        EmpresaRepository empresas = mock(EmpresaRepository.class);
        ServicoWppConnect wpp = mock(ServicoWppConnect.class);
        Empresa empresa = new Empresa();
        empresa.setSessaoWhatsapp("sessao_recanto_01");
        when(empresas.findAll()).thenReturn(List.of(empresa));
        when(wpp.iniciarSessao("sessao_recanto_01"))
                .thenReturn(new SessaoStatusResponseDTO("sessao_recanto_01", "CONNECTED", null));

        new RestauradorSessoesWhatsapp(empresas, wpp).restaurarSessoesExistentes();

        verify(wpp).iniciarSessao("sessao_recanto_01");
    }

    @Test
    void ignoraEmpresaSemSessao() {
        EmpresaRepository empresas = mock(EmpresaRepository.class);
        ServicoWppConnect wpp = mock(ServicoWppConnect.class);
        Empresa empresa = new Empresa();
        empresa.setSessaoWhatsapp("  ");
        when(empresas.findAll()).thenReturn(List.of(empresa));

        new RestauradorSessoesWhatsapp(empresas, wpp).restaurarSessoesExistentes();

        verifyNoInteractions(wpp);
    }
}
