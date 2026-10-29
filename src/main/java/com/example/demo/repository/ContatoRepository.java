package com.example.demo.repository;

import com.example.demo.model.Contato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RepositoryRestResource(exported = false)
public interface ContatoRepository extends JpaRepository<Contato, Long> {

    Optional<Contato> findByEmpresaIdAndNumeroIndice(Long empresaId, String numeroIndice);

    List<Contato> findByEmpresaIdOrderByUltimaConversaEmDesc(Long empresaId);

    Optional<Contato> findByIdAndEmpresaId(Long id, Long empresaId);

    @Query("SELECT c FROM Contato c WHERE (c.ultimaConversaEm IS NULL OR c.ultimaConversaEm < :limite) AND NOT EXISTS (SELECT r FROM Reserva r WHERE r.contato = c AND r.data >= :limiteData)")
    List<Contato> findInativosParaRetencao(@Param("limite") LocalDateTime limite, @Param("limiteData") LocalDate limiteData);
}
