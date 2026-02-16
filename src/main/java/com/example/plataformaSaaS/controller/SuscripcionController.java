package com.example.plataformaSaaS.controller;

import com.example.plataformaSaaS.service.PlanService;
import com.example.plataformaSaaS.service.SuscripcionService;
import com.example.plataformaSaaS.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class SuscripcionController {

    private final SuscripcionService suscripcionService;
    private final PlanService planService;
    private final UsuarioService usuarioService;

    @GetMapping("/planes")
    public String verPlanes(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }
        Boolean esAdmin = (Boolean) session.getAttribute("esAdmin");
        if (esAdmin != null && esAdmin) {
            return "redirect:/admin/panel";
        }

        model.addAttribute("planes", planService.listarPlanesActivos());
        suscripcionService.obtenerSuscripcionActiva(usuarioId)
                .ifPresentOrElse(
                        s -> model.addAttribute("suscripcionActiva", s),
                        () -> model.addAttribute("suscripcionActiva", null)
                );
        return "planes";
    }

    @PostMapping("/suscripcion/crear/{planId}")
    public String crearSuscripcion(@PathVariable Long planId,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }

        try {
            suscripcionService.crearSuscripcionPorIds(usuarioId, planId);
            redirectAttributes.addFlashAttribute("success", "¡Suscripción creada exitosamente!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/suscripcion/cambiar/{planId}")
    public String cambiarPlan(@PathVariable Long planId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }

        try {
            suscripcionService.cambiarPlanPorUsuario(usuarioId, planId);
            redirectAttributes.addFlashAttribute("success", "¡Plan cambiado exitosamente!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/suscripcion/cancelar")
    public String cancelarSuscripcion(HttpSession session, RedirectAttributes redirectAttributes) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }

        try {
            suscripcionService.cancelarSuscripcionPorUsuario(usuarioId);
            redirectAttributes.addFlashAttribute("success", "Suscripción cancelada");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/dashboard";
    }

    @GetMapping("/suscripcion")
    public String verSuscripcion(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }

        usuarioService.buscarPorId(usuarioId).ifPresent(u -> model.addAttribute("usuario", u));
        suscripcionService.obtenerSuscripcionActiva(usuarioId)
                .ifPresentOrElse(
                        s -> model.addAttribute("suscripcion", s),
                        () -> model.addAttribute("suscripcion", null)
                );
        model.addAttribute("planes", planService.listarPlanesActivos());

        return "suscripcion";
    }
}
