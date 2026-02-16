package com.example.plataformaSaaS.controller;

import com.example.plataformaSaaS.service.AuditoriaService;
import com.example.plataformaSaaS.service.FacturaService;
import com.example.plataformaSaaS.service.ScheduledBillingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final FacturaService facturaService;
    private final AuditoriaService auditoriaService;
    private final ScheduledBillingService scheduledBillingService;

    @GetMapping("/panel")
    public String panel(HttpSession session, Model model) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("totalFacturas", facturaService.listarTodas().size());
        return "admin/panel";
    }

    @GetMapping("/facturas")
    public String verFacturas(HttpSession session, Model model,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                              @RequestParam(required = false) BigDecimal montoMin,
                              @RequestParam(required = false) BigDecimal montoMax) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        if (desde != null && hasta != null) {
            model.addAttribute("facturas", facturaService.filtrarPorFecha(desde, hasta));
            model.addAttribute("filtroFechaDesde", desde);
            model.addAttribute("filtroFechaHasta", hasta);
        } else if (montoMin != null && montoMax != null) {
            model.addAttribute("facturas", facturaService.filtrarPorMonto(montoMin, montoMax));
            model.addAttribute("filtroMontoMin", montoMin);
            model.addAttribute("filtroMontoMax", montoMax);
        } else {
            model.addAttribute("facturas", facturaService.listarTodas());
        }

        return "admin/facturas";
    }

    @GetMapping("/auditoria")
    public String verAuditoria(HttpSession session, Model model) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("registros", auditoriaService.obtenerAuditoriaSuscripciones());
        return "admin/auditoria";
    }

    @PostMapping("/facturacion-manual")
    public String ejecutarFacturacion(HttpSession session, RedirectAttributes redirectAttributes) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        int facturasGeneradas = scheduledBillingService.ejecutarFacturacionManual();
        redirectAttributes.addFlashAttribute("success",
                "Facturación ejecutada. Facturas generadas: " + facturasGeneradas);
        return "redirect:/admin/panel";
    }

    private boolean esAdmin(HttpSession session) {
        Boolean esAdmin = (Boolean) session.getAttribute("esAdmin");
        return esAdmin != null && esAdmin;
    }
}
