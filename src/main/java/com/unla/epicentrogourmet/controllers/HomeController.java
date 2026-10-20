package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.helpers.ViewRouteHelper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String root() {
        return "redirect:" + ViewRouteHelper.ROUTE_INDEX;
    }

    @GetMapping("/index")
    public String index() {
        return ViewRouteHelper.INDEX;
    }
}