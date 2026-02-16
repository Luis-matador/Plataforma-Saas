package com.example.plataformaSaaS.service;

import com.example.plataformaSaaS.model.Plan;
import com.example.plataformaSaaS.model.enums.NivelPlan;
import com.example.plataformaSaaS.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

    private final PlanRepository planRepository;

    public List<Plan> listarPlanesActivos() {
        return planRepository.findByActivoTrue();
    }

    public List<Plan> listarTodos() {
        return planRepository.findAll();
    }

    public Optional<Plan> buscarPorId(Long id) {
        return planRepository.findById(id);
    }

    public Optional<Plan> buscarPorNivel(NivelPlan nivel) {
        return planRepository.findByNivel(nivel);
    }

    @Transactional
    public Plan guardar(Plan plan) {
        return planRepository.save(plan);
    }
}
