package com.example.plataformaSaaS.controller;

import com.example.plataformaSaaS.dto.FacturaPagoDTO;
import com.example.plataformaSaaS.dto.MetodoPagoResumenDTO;
import com.example.plataformaSaaS.dto.PayPalRequest;
import com.example.plataformaSaaS.dto.TarjetaRequest;
import com.example.plataformaSaaS.dto.TransferenciaRequest;
import com.example.plataformaSaaS.service.FacturaService;
import com.example.plataformaSaaS.service.MetodoPagoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PagoController {

    private final FacturaService facturaService;
    private final MetodoPagoService metodoPagoService;

    @GetMapping("/factura/{id}/pagar")
    public String mostrarPago(@PathVariable Long id, HttpSession session, Model model,
                              RedirectAttributes redirectAttributes) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return "redirect:/login";

        try {
            FacturaPagoDTO factura = facturaService.obtenerFacturaParaPago(id, usuarioId);
            List<MetodoPagoResumenDTO> metodos = metodoPagoService.listarResumenPorUsuario(usuarioId);
            model.addAttribute("factura", factura);
            model.addAttribute("metodosPago", metodos);
            return "pagar";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/facturas";
        }
    }

    @PostMapping("/factura/{id}/pagar")
    public String procesarPago(@PathVariable Long id,
                               @RequestParam Long metodoPagoId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return "redirect:/login";

        try {
            facturaService.procesarPago(id, metodoPagoId, usuarioId);
            redirectAttributes.addFlashAttribute("success", "Factura pagada exitosamente!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/facturas";
    }

    @GetMapping("/metodos-pago")
    public String verMetodosPago(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return "redirect:/login";

        model.addAttribute("metodosPago", metodoPagoService.listarResumenPorUsuario(usuarioId));
        model.addAttribute("tarjetaRequest", new TarjetaRequest());
        model.addAttribute("payPalRequest", new PayPalRequest());
        model.addAttribute("transferenciaRequest", new TransferenciaRequest());
        return "metodos-pago";
    }

    @PostMapping("/metodos-pago/tarjeta")
    public String agregarTarjeta(@Valid @ModelAttribute("tarjetaRequest") TarjetaRequest request,
                                 BindingResult bindingResult,
                                 HttpSession session,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return "redirect:/login";

        if (bindingResult.hasErrors()) {
            model.addAttribute("metodosPago", metodoPagoService.listarResumenPorUsuario(usuarioId));
            model.addAttribute("payPalRequest", new PayPalRequest());
            model.addAttribute("transferenciaRequest", new TransferenciaRequest());
            model.addAttribute("activeTab", "tarjeta");
            return "metodos-pago";
        }

        try {
            metodoPagoService.guardarTarjetaPorUsuarioId(usuarioId, request.getNumeroTarjeta(),
                    request.getTitular(), request.getFechaExpiracion());
            redirectAttributes.addFlashAttribute("success", "Tarjeta agregada correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/metodos-pago";
    }

    @PostMapping("/metodos-pago/paypal")
    public String agregarPayPal(@Valid @ModelAttribute("payPalRequest") PayPalRequest request,
                                BindingResult bindingResult,
                                HttpSession session,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return "redirect:/login";

        if (bindingResult.hasErrors()) {
            model.addAttribute("metodosPago", metodoPagoService.listarResumenPorUsuario(usuarioId));
            model.addAttribute("tarjetaRequest", new TarjetaRequest());
            model.addAttribute("transferenciaRequest", new TransferenciaRequest());
            model.addAttribute("activeTab", "paypal");
            return "metodos-pago";
        }

        try {
            metodoPagoService.guardarPayPalPorUsuarioId(usuarioId, request.getEmailPaypal());
            redirectAttributes.addFlashAttribute("success", "Cuenta PayPal agregada correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/metodos-pago";
    }

    @PostMapping("/metodos-pago/transferencia")
    public String agregarTransferencia(@Valid @ModelAttribute("transferenciaRequest") TransferenciaRequest request,
                                       BindingResult bindingResult,
                                       HttpSession session,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return "redirect:/login";

        if (bindingResult.hasErrors()) {
            model.addAttribute("metodosPago", metodoPagoService.listarResumenPorUsuario(usuarioId));
            model.addAttribute("tarjetaRequest", new TarjetaRequest());
            model.addAttribute("payPalRequest", new PayPalRequest());
            model.addAttribute("activeTab", "transferencia");
            return "metodos-pago";
        }

        try {
            metodoPagoService.guardarTransferenciaPorUsuarioId(usuarioId, request.getNumeroCuenta(), request.getBanco());
            redirectAttributes.addFlashAttribute("success", "Cuenta bancaria agregada correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/metodos-pago";
    }

    @PostMapping("/metodos-pago/eliminar/{id}")
    public String eliminarMetodoPago(@PathVariable Long id,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return "redirect:/login";

        try {
            metodoPagoService.eliminarPorUsuario(id, usuarioId);
            redirectAttributes.addFlashAttribute("success", "Metodo de pago eliminado");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/metodos-pago";
    }
}
