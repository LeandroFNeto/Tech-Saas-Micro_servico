package com.example.demo.repository;

import com.example.demo.model.Aviso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RepositoryRestResource(exported = false)
public interface AvisoRepository extends JpaRepository<Aviso, Long> {

    List<Aviso> findByAtivoTrueOrderByIdDesc();

    List<Aviso> findAllByOrderByIdDesc();
}
