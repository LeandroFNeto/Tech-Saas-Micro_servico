package com.example.demo.repository;

import com.example.demo.model.HorarioFuncionamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RepositoryRestResource(exported = false)
public interface HorarioFuncionamentoRepository extends JpaRepository<HorarioFuncionamento, Long> {

    List<HorarioFuncionamento> findByEmpresaId(Long empresaId);

    @Transactional
    void deleteByEmpresaId(Long empresaId);
}
