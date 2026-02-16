package com.example.plataformaSaaS.config;

import com.example.plataformaSaaS.model.Caracteristica;
import com.example.plataformaSaaS.model.Plan;
import com.example.plataformaSaaS.model.PlanCaracteristica;
import com.example.plataformaSaaS.model.Usuario;
import com.example.plataformaSaaS.model.enums.NivelPlan;
import com.example.plataformaSaaS.repository.CaracteristicaRepository;
import com.example.plataformaSaaS.repository.PlanCaracteristicaRepository;
import com.example.plataformaSaaS.repository.PlanRepository;
import com.example.plataformaSaaS.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PlanRepository planRepository;
    private final UsuarioRepository usuarioRepository;
    private final CaracteristicaRepository caracteristicaRepository;
    private final PlanCaracteristicaRepository planCaracteristicaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (planRepository.count() == 0) {
            crearPlanes();
        }

        if (caracteristicaRepository.count() == 0) {
            crearCaracteristicasYAsociar();
        }

        if (!usuarioRepository.existsByEmail("admin@saas.com")) {
            crearAdmin();
        }
    }

    private void crearPlanes() {
        Plan basic = Plan.builder()
                .nombre("Basic")
                .nivel(NivelPlan.BASIC)
                .precioMensual(new BigDecimal("9.99"))
                .descripcion("Plan básico para usuarios individuales")
                .activo(true)
                .build();

        Plan premium = Plan.builder()
                .nombre("Premium")
                .nivel(NivelPlan.PREMIUM)
                .precioMensual(new BigDecimal("29.99"))
                .descripcion("Plan premium para equipos pequeños")
                .activo(true)
                .build();

        Plan enterprise = Plan.builder()
                .nombre("Enterprise")
                .nivel(NivelPlan.ENTERPRISE)
                .precioMensual(new BigDecimal("99.99"))
                .descripcion("Plan empresarial para grandes organizaciones")
                .activo(true)
                .build();

        planRepository.save(basic);
        planRepository.save(premium);
        planRepository.save(enterprise);

        log.info("Planes creados: Basic (€9.99), Premium (€29.99), Enterprise (€99.99)");
    }

    private void crearCaracteristicasYAsociar() {
        Caracteristica proyectos = caracteristicaRepository.save(Caracteristica.builder()
                .nombre("Proyectos").descripcion("Número máximo de proyectos").icono("bi-folder").build());
        Caracteristica almacenamiento = caracteristicaRepository.save(Caracteristica.builder()
                .nombre("Almacenamiento").descripcion("Espacio de almacenamiento en la nube").icono("bi-hdd").build());
        Caracteristica soporte = caracteristicaRepository.save(Caracteristica.builder()
                .nombre("Soporte").descripcion("Tipo de soporte técnico").icono("bi-headset").build());
        Caracteristica api = caracteristicaRepository.save(Caracteristica.builder()
                .nombre("API Access").descripcion("Acceso a la API REST").icono("bi-code-slash").build());
        Caracteristica sso = caracteristicaRepository.save(Caracteristica.builder()
                .nombre("SSO").descripcion("Single Sign-On corporativo").icono("bi-shield-lock").build());
        Caracteristica sla = caracteristicaRepository.save(Caracteristica.builder()
                .nombre("SLA").descripcion("Acuerdo de nivel de servicio").icono("bi-award").build());

        Plan basic = planRepository.findByNivel(NivelPlan.BASIC).orElseThrow();
        Plan premium = planRepository.findByNivel(NivelPlan.PREMIUM).orElseThrow();
        Plan enterprise = planRepository.findByNivel(NivelPlan.ENTERPRISE).orElseThrow();

        planCaracteristicaRepository.save(PlanCaracteristica.builder()
                .plan(basic).caracteristica(proyectos).limite(5).valor("5").activa(true).build());
        planCaracteristicaRepository.save(PlanCaracteristica.builder()
                .plan(basic).caracteristica(almacenamiento).limite(1).valor("1 GB").activa(true).build());
        planCaracteristicaRepository.save(PlanCaracteristica.builder()
                .plan(basic).caracteristica(soporte).limite(1).valor("Email").activa(true).build());
        planCaracteristicaRepository.save(PlanCaracteristica.builder()
                .plan(basic).caracteristica(api).limite(0).valor("No incluido").activa(false).build());
        planCaracteristicaRepository.save(PlanCaracteristica.builder()
                .plan(basic).caracteristica(sso).limite(0).valor("No incluido").activa(false).build());
        planCaracteristicaRepository.save(PlanCaracteristica.builder()
                .plan(basic).caracteristica(sla).limite(0).valor("No incluido").activa(false).build());

        planCaracteristicaRepository.save(PlanCaracteristica.builder()
                .plan(premium).caracteristica(proyectos).limite(25).valor("25").activa(true).build());
        planCaracteristicaRepository.save(PlanCaracteristica.builder()
                .plan(premium).caracteristica(almacenamiento).limite(10).valor("10 GB").activa(true).build());
        planCaracteristicaRepository.save(PlanCaracteristica.builder()
                .plan(premium).caracteristica(soporte).limite(2).valor("Prioritario").activa(true).build());
        planCaracteristicaRepository.save(PlanCaracteristica.builder()
                .plan(premium).caracteristica(api).limite(-1).valor("Completo").activa(true).build());
        planCaracteristicaRepository.save(PlanCaracteristica.builder()
                .plan(premium).caracteristica(sso).limite(0).valor("No incluido").activa(false).build());
        planCaracteristicaRepository.save(PlanCaracteristica.builder()
                .plan(premium).caracteristica(sla).limite(0).valor("No incluido").activa(false).build());

        planCaracteristicaRepository.save(PlanCaracteristica.builder()
                .plan(enterprise).caracteristica(proyectos).limite(-1).valor("Ilimitados").activa(true).build());
        planCaracteristicaRepository.save(PlanCaracteristica.builder()
                .plan(enterprise).caracteristica(almacenamiento).limite(100).valor("100 GB").activa(true).build());
        planCaracteristicaRepository.save(PlanCaracteristica.builder()
                .plan(enterprise).caracteristica(soporte).limite(3).valor("24/7 dedicado").activa(true).build());
        planCaracteristicaRepository.save(PlanCaracteristica.builder()
                .plan(enterprise).caracteristica(api).limite(-1).valor("Completo").activa(true).build());
        planCaracteristicaRepository.save(PlanCaracteristica.builder()
                .plan(enterprise).caracteristica(sso).limite(-1).valor("Incluido").activa(true).build());
        planCaracteristicaRepository.save(PlanCaracteristica.builder()
                .plan(enterprise).caracteristica(sla).limite(1).valor("99.9%").activa(true).build());

        log.info("Características y asociaciones Plan-Caracteristica creadas (tabla intermedia ManyToMany)");
    }

    private void crearAdmin() {
        Usuario admin = Usuario.builder()
                .nombre("Administrador")
                .email("admin@saas.com")
                .password(passwordEncoder.encode("admin123"))
                .pais("España")
                .fechaRegistro(LocalDateTime.now())
                .esAdmin(true)
                .build();

        usuarioRepository.save(admin);
        log.info("Usuario admin creado: admin@saas.com / admin123");
    }
}
