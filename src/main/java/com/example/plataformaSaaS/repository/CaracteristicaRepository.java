package com.example.plataformaSaaS.repository;

import com.example.plataformaSaaS.model.Caracteristica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaracteristicaRepository extends JpaRepository<Caracteristica, Long> {
    Optional<Caracteristica> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
}
