package com.unla.epicentrogourmet.controllers;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.unla.epicentrogourmet.services.implementation.PersonalService;

@Controller
public class FestivalActualController {

    private final PersonalService personalService;

    public FestivalActualController(PersonalService personalService) {
        this.personalService = personalService;
    }

    @GetMapping("/empleado/hoy")
    public String hoy(Principal principal, Model model) {
        try {
            model.addAttribute("actividad",
                    personalService.traerFestivalActual(principal.getName()));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "empleado/hoy";
    }
}