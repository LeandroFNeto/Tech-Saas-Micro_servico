package com.example.demo.repository;

import com.example.demo.model.DataEspecial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RepositoryRestResource(exported = false)
public interface DataEspecialRepository extends JpaRepository<DataEspecial, Long> {

    List<DataEspecial> findByEmpresaIdAndDataGreaterThanEqualOrderByData(Long empresaId, LocalDate data);

    Optional<DataEspecial> findByEmpresaIdAndData(Long empresaId, LocalDate data);

    Optional<DataEspecial> findByIdAndEmpresaId(Long id, Long empresaId);
}
