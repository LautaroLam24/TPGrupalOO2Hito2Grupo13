package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.helpers.ViewRouteHelper;
import com.unla.epicentrogourmet.services.IFestivalService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/admin/festivales")
public class FestivalController {

    private final IFestivalService festivalService;

    public FestivalController(IFestivalService festivalService) {
        this.festivalService = festivalService;
    }

    // GET: /admin/festivales
    @GetMapping("")
    public ModelAndView index() {
        ModelAndView mAV = new ModelAndView(ViewRouteHelper.ADMIN_FESTIVALES);
        mAV.addObject("festivales", festivalService.traerTodosPorFecha());
        return mAV;
    }
}