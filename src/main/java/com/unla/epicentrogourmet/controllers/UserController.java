package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.helpers.ViewRouteHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {

    @GetMapping("/login")
    public String login(Model model,
                        @RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout) {
        model.addAttribute("error", error);
        model.addAttribute("logout", logout);
        return ViewRouteHelper.USER_LOGIN;
    }

    @GetMapping("/loginsuccess")
    public String loginSuccess() {
        // Por ahora todos van al inicio. Cuando existan los paneles de cada rol, acá se redirige según el rol.
        return "redirect:" + ViewRouteHelper.ROUTE_INDEX;
    }
}