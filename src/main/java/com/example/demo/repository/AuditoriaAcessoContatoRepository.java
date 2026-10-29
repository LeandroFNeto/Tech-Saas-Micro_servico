package com.example.demo.repository;

import com.example.demo.model.AuditoriaAcessoContato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RepositoryRestResource(exported = false)
public interface AuditoriaAcessoContatoRepository extends JpaRepository<AuditoriaAcessoContato, Long> {

    long countByEmpresaIdAndDataHoraAfter(Long empresaId, LocalDateTime limite);

    @Modifying
    @Query("DELETE FROM AuditoriaAcessoContato a WHERE a.dataHora < :dataHoraLimite")
    void apagarAuditoriasAntesDe(@Param("dataHoraLimite") LocalDateTime dataHoraLimite);
}
