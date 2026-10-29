package com.unla.epicentrogourmet.controllers;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.unla.epicentrogourmet.dto.HistorialFestivalDTO;
import com.unla.epicentrogourmet.services.implementation.PersonalService;

@Controller
public class HistorialEmpleadoController {

    private final PersonalService personalService;

    public HistorialEmpleadoController(PersonalService personalService) {
        this.personalService = personalService;
    }

    @GetMapping("/empleado/historial")
    public String historial(Principal principal, Model model) {
        try {
            List<HistorialFestivalDTO> historial =
                    personalService.traerHistorial(principal.getName());

            model.addAttribute("historial", historial);
            model.addAttribute("aunNoTrabajo",
                    !historial.isEmpty()
                            && historial.stream().allMatch(h -> "Próximo".equals(h.estado())));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "empleado/historial";
    }
}