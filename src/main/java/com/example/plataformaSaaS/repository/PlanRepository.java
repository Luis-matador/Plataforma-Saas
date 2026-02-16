package com.example.plataformaSaaS.repository;

import com.example.plataformaSaaS.model.Plan;
import com.example.plataformaSaaS.model.enums.NivelPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByNivel(NivelPlan nivel);

    List<Plan> findByActivoTrue();
}
