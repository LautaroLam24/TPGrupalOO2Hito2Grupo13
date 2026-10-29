package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.helpers.ViewRouteHelper;
import com.unla.epicentrogourmet.services.IFestivalService;
import com.unla.epicentrogourmet.services.implementation.UnidadDeVentaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class HomeController {

    private final IFestivalService festivalService;
    private final UnidadDeVentaService unidadDeVentaService;

    public HomeController(IFestivalService festivalService,
                          UnidadDeVentaService unidadDeVentaService) {
        this.festivalService = festivalService;
        this.unidadDeVentaService = unidadDeVentaService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:" + ViewRouteHelper.ROUTE_INDEX;
    }

    @GetMapping("/index")
    public String index(Model model, Principal principal) {
        model.addAttribute("festivales", festivalService.traerTodos());
        // principal es null para el usuario anónimo
        if (principal != null) {
            model.addAttribute("unidad",
                    unidadDeVentaService.traerUnidadDelUsuario(principal.getName()).orElse(null));
        }
        return ViewRouteHelper.INDEX;
    }
}