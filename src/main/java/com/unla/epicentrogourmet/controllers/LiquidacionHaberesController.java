package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.repositories.IFestivalRepository;
import com.unla.epicentrogourmet.services.implementation.LiquidacionHaberesService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LiquidacionHaberesController {

    private final IFestivalRepository festivalRepository;
    private final LiquidacionHaberesService liquidacionService;

    public LiquidacionHaberesController(
            IFestivalRepository festivalRepository,
            LiquidacionHaberesService liquidacionService) {
        this.festivalRepository = festivalRepository;
        this.liquidacionService = liquidacionService;
    }

    @GetMapping("/empleado/liquidacion")
    public String mostrarFormulario(Model model) {
        model.addAttribute("festivales",
                festivalRepository.findAll());
        return "empleado/liquidacion";
    }

    @PostMapping("/empleado/liquidacion")
    public String calcular(
            @RequestParam int festivalId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            model.addAttribute("festivales",
                    festivalRepository.findAll());

            model.addAttribute("liquidacion",
                    liquidacionService.liquidar(
                            authentication.getName(), festivalId));

            return "empleado/liquidacion";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "error", e.getMessage());
            return "redirect:/empleado/liquidacion";
        }
    }
}