package com.example.plataformaSaaS.controller;

import com.example.plataformaSaaS.dto.LoginRequest;
import com.example.plataformaSaaS.dto.RegistroRequest;
import com.example.plataformaSaaS.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class RegistroController {

    private final UsuarioService usuarioService;

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("registroRequest", new RegistroRequest());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("registroRequest") RegistroRequest request,
                           BindingResult bindingResult,
                           HttpSession session,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "registro";
        }
        try {
            var usuario = usuarioService.registrarDesdeDTO(request);
            session.setAttribute("usuarioId", usuario.getId());
            session.setAttribute("esAdmin", usuario.isEsAdmin());
            return "redirect:/planes";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "registro";
        }
    }

    @GetMapping("/login")
    public String mostrarLogin(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginRequest") LoginRequest request,
                       BindingResult bindingResult,
                       HttpSession session,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "login";
        }
        return usuarioService.login(request.getEmail(), request.getPassword())
                .map(usuario -> {
                    session.setAttribute("usuarioId", usuario.getId());
                    session.setAttribute("esAdmin", usuario.isEsAdmin());
                    if (usuario.isEsAdmin()) {
                        return "redirect:/admin/panel";
                    }
                    return "redirect:/dashboard";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "Email o contraseña incorrectos");
                    return "login";
                });
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
