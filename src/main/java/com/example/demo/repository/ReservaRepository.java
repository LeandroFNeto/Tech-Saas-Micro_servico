package com.example.demo.repository;

import com.example.demo.model.Reserva;
import com.example.demo.model.SituacaoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RepositoryRestResource(exported = false)
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    Optional<Reserva> findByEmpresaIdAndGoogleEventId(Long empresaId, String googleEventId);

    Optional<Reserva> findByIdAndEmpresaId(Long id, Long empresaId);

    List<Reserva> findByEmpresaIdAndDataBetweenOrderByDataAscHorarioAsc(Long empresaId, LocalDate inicio, LocalDate fim);

    List<Reserva> findByEmpresaIdOrderByDataAsc(Long empresaId);

    List<Reserva> findBySituacaoAndLembreteEnviarEmLessThanEqual(SituacaoReserva situacao, LocalDateTime limite);

    List<Reserva> findBySituacaoAndLembreteEnviadoEmLessThanEqual(SituacaoReserva situacao, LocalDateTime limite);

    List<Reserva> findByDataLessThanAndSituacaoNot(LocalDate data, SituacaoReserva situacao);

    List<Reserva> findByContatoId(Long contatoId);

    List<Reserva> findByContatoIdAndSituacaoInAndDataGreaterThanEqualOrderByDataAsc(
            Long contatoId,
            Collection<SituacaoReserva> situacoes,
            LocalDate dataMinima);

    @Modifying
    @Query("DELETE FROM Reserva r WHERE r.data < :dataLimite")
    void apagarReservasEncerradasAntesDe(@Param("dataLimite") LocalDate dataLimite);
}
