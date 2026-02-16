package com.example.plataformaSaaS.controller;

import com.example.plataformaSaaS.service.FacturaService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService facturaService;

    @GetMapping("/facturas")
    public String verFacturas(HttpSession session, Model model,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                              @RequestParam(required = false) BigDecimal montoMin,
                              @RequestParam(required = false) BigDecimal montoMax) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }
        Boolean esAdmin = (Boolean) session.getAttribute("esAdmin");
        if (esAdmin != null && esAdmin) {
            return "redirect:/admin/panel";
        }

        if (desde != null && hasta != null) {
            model.addAttribute("facturas", facturaService.filtrarFacturasUsuarioPorFecha(usuarioId, desde, hasta));
            model.addAttribute("filtroFechaDesde", desde);
            model.addAttribute("filtroFechaHasta", hasta);
        }
        else if (montoMin != null && montoMax != null) {
            model.addAttribute("facturas", facturaService.filtrarFacturasUsuarioPorMonto(usuarioId, montoMin, montoMax));
            model.addAttribute("filtroMontoMin", montoMin);
            model.addAttribute("filtroMontoMax", montoMax);
        }
        else {
            model.addAttribute("facturas", facturaService.listarFacturasUsuario(usuarioId));
        }

        model.addAttribute("ultimaFacturaPendienteId", facturaService.obtenerUltimaFacturaPendienteId(usuarioId));
        return "facturas";
    }
}
