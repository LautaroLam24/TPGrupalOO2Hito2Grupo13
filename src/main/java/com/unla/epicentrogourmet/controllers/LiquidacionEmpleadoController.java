package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.entities.LiquidacionHaberes;
import com.unla.epicentrogourmet.services.implementation.LiquidacionPdfService;
import com.unla.epicentrogourmet.services.implementation.LiquidacionResponsableService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.security.Principal;

@Controller
@PreAuthorize("hasRole('EMPLEADO')")
public class LiquidacionEmpleadoController {
    private final LiquidacionResponsableService service;
    private final LiquidacionPdfService pdfService;
    public LiquidacionEmpleadoController(LiquidacionResponsableService service, LiquidacionPdfService pdfService) {
        this.service=service; this.pdfService=pdfService;
    }
    @GetMapping("/empleado/mis-liquidaciones")
    public String listar(Model model, Principal principal) {
        model.addAttribute("liquidaciones", service.liquidacionesEmpleado(principal.getName()));
        return "empleado/mis-liquidaciones";
    }
    @GetMapping("/empleado/mis-liquidaciones/{id}")
    public String verDetalle(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("liquidacion", service.liquidacionVisibleParaEmpleado(id, principal.getName()));
        return "empleado/liquidacion-detalle";
    }

    @GetMapping("/empleado/mis-liquidaciones/{id}/pdf")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id, Principal principal) {
        LiquidacionHaberes l = service.liquidacionVisibleParaEmpleado(id, principal.getName());
        byte[] pdf = pdfService.generar(l);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=liquidacion-" + l.getPeriodo() + ".pdf")
                .body(pdf);
    }
}
