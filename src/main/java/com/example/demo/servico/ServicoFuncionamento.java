package com.example.demo.servico;

import com.example.demo.model.DataEspecial;
import com.example.demo.model.Empresa;
import com.example.demo.model.HorarioFuncionamento;
import com.example.demo.model.PerfilFuncionamento;
import com.example.demo.repository.DataEspecialRepository;
import com.example.demo.repository.HorarioFuncionamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ServicoFuncionamento {

    public static final LocalTime ABERTURA_PADRAO = LocalTime.of(8, 0);
    public static final LocalTime FECHAMENTO_PADRAO = LocalTime.of(22, 0);
    public static final int DIAS_MAXIMOS_A_FRENTE = 180;

    public record Janela(boolean aberto, LocalTime abre, LocalTime fecha, int duracaoMinutos, String motivo) {

        public static Janela fechada(String motivo) {
            return new Janela(false, null, null, 0, motivo);
        }

        public boolean comporta(LocalTime inicio) {
            if (!aberto || inicio == null || abre == null || fecha == null) {
                return false;
            }
            if (inicio.isBefore(abre)) {
                return false;
            }
            LocalTime fim = inicio.plusMinutes(duracaoMinutos);
            if (fecha.equals(LocalTime.MIDNIGHT)) {
                return fim.equals(LocalTime.MIDNIGHT) || fim.isAfter(inicio);
            }
            return !fim.isBefore(inicio) && !fim.isAfter(fecha);
        }
    }

    @Autowired private HorarioFuncionamentoRepository horarioRepository;
    @Autowired private DataEspecialRepository dataEspecialRepository;

    public Janela janelaDa(Empresa empresa, LocalDate data) {
        Optional<DataEspecial> especial = empresa.getId() == null
                ? Optional.empty()
                : dataEspecialRepository.findByEmpresaIdAndData(empresa.getId(), data);
        List<HorarioFuncionamento> linhas = empresa.getId() == null
                ? List.of()
                : horarioRepository.findByEmpresaId(empresa.getId());
        return calcular(empresa, data, especial.orElse(null), linhas);
    }

    static Janela calcular(Empresa empresa, LocalDate data, DataEspecial especial, List<HorarioFuncionamento> linhas) {
        int duracaoEmpresa = empresa.duracaoEfetivaMinutos();
        if (especial != null) {
            if (especial.isFechado()) {
                return Janela.fechada(motivoOuPadrao(especial.getMotivo(), "dia fechado"));
            }
            if (especial.getAbre() != null && especial.getFecha() != null) {
                return new Janela(true, especial.getAbre(), especial.getFecha(), duracaoEmpresa, especial.getMotivo());
            }
        }

        Optional<String> feriado = CalendarioFeriados.nomeDoFeriado(empresa.getUf(), data);
        PerfilFuncionamento perfil = feriado.isPresent()
                ? PerfilFuncionamento.FERIADO
                : PerfilFuncionamento.doDiaDaSemana(data.getDayOfWeek());

        HorarioFuncionamento linha = buscar(linhas, perfil);
        if (linha == null && perfil == PerfilFuncionamento.FERIADO) {
            linha = buscar(linhas, PerfilFuncionamento.DOMINGO);
        }
        String motivoFeriado = feriado.map(nome -> "feriado: " + nome).orElse(null);
        if (linha == null) {
            return new Janela(true, ABERTURA_PADRAO, FECHAMENTO_PADRAO, duracaoEmpresa, motivoFeriado);
        }
        if (linha.isFechado()) {
            return Janela.fechada(motivoFeriado != null ? motivoFeriado : "não abrimos neste dia da semana");
        }
        int duracao = linha.getDuracaoMinutos() != null ? linha.getDuracaoMinutos() : duracaoEmpresa;
        return new Janela(true, linha.getAbre(), linha.getFecha(), duracao, motivoFeriado);
    }

    public static List<LocalTime> iniciosPossiveis(Janela janela) {
        List<LocalTime> inicios = new ArrayList<>();
        if (!janela.aberto() || janela.duracaoMinutos() <= 0) {
            return inicios;
        }
        LocalTime inicio = janela.abre();
        while (janela.comporta(inicio)) {
            inicios.add(inicio);
            LocalTime proximo = inicio.plusMinutes(janela.duracaoMinutos());
            if (proximo.equals(LocalTime.MIDNIGHT) || !proximo.isAfter(inicio)) {
                break;
            }
            inicio = proximo;
        }
        return inicios;
    }

    public enum Situacao { FECHADO, LOTADO, LIVRE }

    public Situacao situacaoDoDia(Empresa empresa, LocalDate data, Janela janela,
                                  List<ServicoGoogleagenda.Intervalo> ocupados) {
        if (!janela.aberto()) {
            return Situacao.FECHADO;
        }
        return iniciosLivres(empresa, data, janela, ocupados).isEmpty() ? Situacao.LOTADO : Situacao.LIVRE;
    }

    public List<LocalTime> iniciosLivres(Empresa empresa, LocalDate data, Janela janela,
                                         List<ServicoGoogleagenda.Intervalo> ocupados) {
        int intervalo = empresa.intervaloEfetivoMinutos();
        return iniciosPossiveis(janela).stream().filter(inicio -> {
            LocalDateTime comeco = data.atTime(inicio);
            return !ServicoGoogleagenda.conflita(ocupados, comeco, comeco.plusMinutes(janela.duracaoMinutos()), intervalo);
        }).toList();
    }

    public List<LocalDate> proximasDatasAbertas(Empresa empresa, LocalDate aPartirDe, int quantidade) {
        List<LocalDate> abertas = new ArrayList<>();
        LocalDate limite = aPartirDe.plusDays(DIAS_MAXIMOS_A_FRENTE);
        List<HorarioFuncionamento> linhas = empresa.getId() == null
                ? List.of()
                : horarioRepository.findByEmpresaId(empresa.getId());
        Map<LocalDate, DataEspecial> especiais = empresa.getId() == null
                ? Map.of()
                : dataEspecialRepository.findByEmpresaIdAndDataGreaterThanEqualOrderByData(empresa.getId(), aPartirDe)
                        .stream().collect(Collectors.toMap(DataEspecial::getData, e -> e, (a, b) -> a));
        LocalDate dia = aPartirDe;
        while (abertas.size() < quantidade && !dia.isAfter(limite)) {
            if (calcular(empresa, dia, especiais.get(dia), linhas).aberto()) {
                abertas.add(dia);
            }
            dia = dia.plusDays(1);
        }
        return abertas;
    }

    private static HorarioFuncionamento buscar(List<HorarioFuncionamento> linhas, PerfilFuncionamento perfil) {
        return linhas.stream().filter(l -> l.getPerfil() == perfil).findFirst().orElse(null);
    }

    private static String motivoOuPadrao(String motivo, String padrao) {
        return motivo == null || motivo.isBlank() ? padrao : motivo.trim();
    }
}
