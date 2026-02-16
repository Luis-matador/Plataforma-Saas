package com.example.plataformaSaaS.controller;

import com.example.plataformaSaaS.model.Suscripcion;
import com.example.plataformaSaaS.model.Usuario;
import com.example.plataformaSaaS.service.FacturaService;
import com.example.plataformaSaaS.service.SuscripcionService;
import com.example.plataformaSaaS.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UsuarioService usuarioService;
    private final SuscripcionService suscripcionService;
    private final FacturaService facturaService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }
        Boolean esAdmin = (Boolean) session.getAttribute("esAdmin");
        if (esAdmin != null && esAdmin) {
            return "redirect:/admin/panel";
        }

        Optional<Usuario> optUsuario = usuarioService.buscarPorId(usuarioId);
        if (optUsuario.isEmpty()) {
            return "redirect:/login";
        }

        Usuario usuario = optUsuario.get();
        Optional<Suscripcion> suscripcionActiva = suscripcionService.obtenerSuscripcionActiva(usuarioId);

        model.addAttribute("usuario", usuario);
        model.addAttribute("suscripcion", suscripcionActiva.orElse(null));
        model.addAttribute("facturas", facturaService.listarFacturasUsuario(usuarioId));
        model.addAttribute("tieneSuscripcion", suscripcionActiva.isPresent());
        model.addAttribute("ultimaFacturaPendienteId", facturaService.obtenerUltimaFacturaPendienteId(usuarioId));

        return "dashboard";
    }
}
