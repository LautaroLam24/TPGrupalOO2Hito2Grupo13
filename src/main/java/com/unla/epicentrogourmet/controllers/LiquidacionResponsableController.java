package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.dto.LiquidacionResponsableDTO;
import com.unla.epicentrogourmet.entities.LiquidacionHaberes;
import com.unla.epicentrogourmet.services.implementation.EmailService;
import com.unla.epicentrogourmet.services.implementation.LiquidacionPdfService;
import com.unla.epicentrogourmet.services.implementation.LiquidacionResponsableService;
import org.springframework.mail.MailException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;

@Controller
@PreAuthorize("hasRole('RESPONSABLE')")
public class LiquidacionResponsableController {
    private final LiquidacionResponsableService service;
    private final LiquidacionPdfService pdfService;
    private final EmailService emailService;

    public LiquidacionResponsableController(LiquidacionResponsableService service, LiquidacionPdfService pdfService, EmailService emailService) {
        this.service=service; this.pdfService=pdfService; this.emailService=emailService;
    }

    @GetMapping("/responsable/liquidaciones")
    public String index(Model model, Principal principal) {
        cargarListas(model, principal.getName());
        return "responsable/liquidaciones";
    }

    @PostMapping("/responsable/liquidaciones/calcular")
    public String calcular(@RequestParam int empleadoId, @RequestParam int festivalId, @RequestParam String periodo,
            Model model, Principal principal) {
        cargarListas(model, principal.getName());
        try {
            model.addAttribute("liquidacion", service.calcular(principal.getName(), empleadoId, festivalId, periodo));
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "responsable/liquidaciones";
    }

    @PostMapping("/responsable/liquidaciones/enviar")
    public String enviar(@RequestParam int empleadoId, @RequestParam int festivalId, @RequestParam String periodo,
            Principal principal, RedirectAttributes redirect) {
        try {
            LiquidacionResponsableDTO dto = service.calcular(principal.getName(), empleadoId, festivalId, periodo);
            LiquidacionHaberes guardada = service.guardarEnviada(principal.getName(), dto);
            byte[] pdf = pdfService.generar(guardada);
            emailService.enviarLiquidacion(dto.getEmail(), dto.getEmpleado(), dto.getPeriodo(), pdf);
            service.marcarComoEnviada(guardada.getId());
            redirect.addFlashAttribute("mensaje", "Liquidación guardada y enviada por correo al empleado.");
        } catch (MailException e) {
            redirect.addFlashAttribute("error", "La liquidación quedó guardada, pero no se pudo enviar el correo. Revisá la configuración SMTP.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/responsable/liquidaciones";
    }

    private void cargarListas(Model model, String username) {
        model.addAttribute("empleados", service.empleadosDelResponsable(username));
        model.addAttribute("festivales", service.festivalesDelResponsable(username));
    }
}
