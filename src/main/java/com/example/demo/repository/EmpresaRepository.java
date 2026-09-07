package com.example.demo.repository;

import com.example.demo.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RepositoryRestResource(exported = false)
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    Empresa findBySessaoWhatsapp(String sessaoWhatsapp);

    @Query("SELECT DISTINCT e FROM Empresa e LEFT JOIN FETCH e.modulosAtivos ORDER BY e.id")
    List<Empresa> listarComModulos();

    @Query("SELECT e FROM Empresa e LEFT JOIN FETCH e.modulosAtivos WHERE e.sessaoWhatsapp = :sessao")
    Empresa buscarPorSessaoComModulos(@Param("sessao") String sessao);
}
