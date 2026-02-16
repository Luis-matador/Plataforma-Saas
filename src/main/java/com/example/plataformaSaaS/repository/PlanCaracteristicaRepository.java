package com.example.plataformaSaaS.repository;

import com.example.plataformaSaaS.model.PlanCaracteristica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanCaracteristicaRepository extends JpaRepository<PlanCaracteristica, Long> {
    List<PlanCaracteristica> findByPlanId(Long planId);
    List<PlanCaracteristica> findByCaracteristicaId(Long caracteristicaId);
}
