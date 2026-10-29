package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.exceptions.ReporteResponsableException;
import com.unla.epicentrogourmet.helpers.ViewRouteHelper;
import com.unla.epicentrogourmet.services.implementation.UnidadDeVentaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(ViewRouteHelper.ROUTE_REPORTES_RESPONSABLE)
@PreAuthorize("hasRole('RESPONSABLE')")
public class CanonUnidadController {

    private final UnidadDeVentaService unidadDeVentaService;

    public CanonUnidadController(UnidadDeVentaService unidadDeVentaService) {
        this.unidadDeVentaService = unidadDeVentaService;
    }

    @GetMapping("/canon")
    public String mostrarCanon(Authentication authentication, Model model) {
        try {
            model.addAttribute("reporte", unidadDeVentaService.canonPorFestival(authentication.getName()));
        } catch (ReporteResponsableException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return ViewRouteHelper.CANON_UNIDAD;
    }
}
