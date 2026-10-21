package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.helpers.ViewRouteHelper;
import com.unla.epicentrogourmet.services.IFestivalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final IFestivalService festivalService;

    public HomeController(IFestivalService festivalService) {
        this.festivalService = festivalService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:" + ViewRouteHelper.ROUTE_INDEX;
    }

    @GetMapping("/index")
    public String index(Model model) {
        model.addAttribute("festivales", festivalService.traerTodos());
        return ViewRouteHelper.INDEX;
    }
}