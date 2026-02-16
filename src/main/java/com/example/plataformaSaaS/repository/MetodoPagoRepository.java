package com.example.plataformaSaaS.repository;

import com.example.plataformaSaaS.model.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Long> {

    List<MetodoPago> findByUsuarioId(Long usuarioId);
}
