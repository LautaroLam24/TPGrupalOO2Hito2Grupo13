package com.unla.epicentrogourmet.controllers;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
            model.addAttribute("historial",
                    personalService.traerHistorial(principal.getName()));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "empleado/historial";
    }
}