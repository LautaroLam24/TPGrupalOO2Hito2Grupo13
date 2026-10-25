package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.dto.PlatoDTO;
import com.unla.epicentrogourmet.exceptions.PlatoEstrellaException;
import com.unla.epicentrogourmet.helpers.ViewRouteHelper;
import com.unla.epicentrogourmet.services.implementation.PlatoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping(ViewRouteHelper.ROUTE_REPORTES_RESPONSABLE)
@PreAuthorize("hasRole('RESPONSABLE')")
public class ReporteResponsableController {

    private final PlatoService platoService;

    public ReporteResponsableController(PlatoService platoService) {
        this.platoService = platoService;
    }

    @GetMapping
    public String mostrarReportes() {
        return ViewRouteHelper.REPORTES_RESPONSABLE;
    }

    @GetMapping("/plato-estrella")
    public String mostrarPlatoEstrella(
            @RequestParam(required = false) Integer festivalId,
            Authentication authentication,
            Model model) {
        String username = nombreUsuario(authentication);

        try {
            PlatoService.ContextoPlatoEstrella contexto = platoService.preparar(username);
            model.addAttribute("unidad", contexto.unidad());
            model.addAttribute("festivales", contexto.festivales());

            if (festivalId != null) {
                model.addAttribute("festivalId", festivalId);
                contexto.festivales().stream()
                        .filter(festival -> festival.id() == festivalId)
                        .findFirst()
                        .ifPresent(festival -> model.addAttribute("festivalSeleccionado", festival));

                Optional<PlatoDTO> resultado = platoService.platoEstrella(username, festivalId);
                model.addAttribute("consultaRealizada", true);
                resultado.ifPresentOrElse(
                        plato -> model.addAttribute("platoEstrella", plato),
                        () -> model.addAttribute("sinVentas", true));
            }
        } catch (PlatoEstrellaException ex) {
            model.addAttribute("error", ex.getMessage());
        }

        return ViewRouteHelper.PLATO_ESTRELLA;
    }

    private String nombreUsuario(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }
}
