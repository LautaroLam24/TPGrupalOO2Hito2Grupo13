package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.helpers.ViewRouteHelper;
import com.unla.epicentrogourmet.services.IFestivalService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

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

    // GET: /admin/festivales/{id}/unidades
    @GetMapping("/{id}/unidades")
    public ModelAndView unidades(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        Optional<Festival> festival = festivalService.traerPorId(id);
        if (festival.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeError", "El festival solicitado no existe.");
            return new ModelAndView("redirect:" + ViewRouteHelper.ROUTE_ADMIN_FESTIVALES);
        }
        ModelAndView mAV = new ModelAndView(ViewRouteHelper.ADMIN_UNIDADES_FESTIVAL);
        mAV.addObject("festival", festival.get());
        mAV.addObject("unidades", festivalService.traerUnidades(id));
        return mAV;
    }

    // GET: /admin/festivales/{id}/ranking
    @GetMapping("/{id}/ranking")
    public ModelAndView ranking(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        Optional<Festival> festival = festivalService.traerPorId(id);
        if (festival.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeError", "El festival solicitado no existe.");
            return new ModelAndView("redirect:" + ViewRouteHelper.ROUTE_ADMIN_FESTIVALES);
        }
        ModelAndView mAV = new ModelAndView(ViewRouteHelper.ADMIN_RANKING_FESTIVAL);
        mAV.addObject("festival", festival.get());
        mAV.addObject("ranking", festivalService.rankingUnidades(id));
        return mAV;
    }

    // GET: /admin/festivales/{id}/mayor-canon?x=3
    @GetMapping("/{id}/mayor-canon")
    public ModelAndView mayorCanon(@PathVariable("id") int id,
                                   @RequestParam(name = "x", defaultValue = "3") String x,
                                   RedirectAttributes redirectAttributes) {
        Optional<Festival> festival = festivalService.traerPorId(id);
        if (festival.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeError", "El festival solicitado no existe.");
            return new ModelAndView("redirect:" + ViewRouteHelper.ROUTE_ADMIN_FESTIVALES);
        }
        ModelAndView mAV = new ModelAndView(ViewRouteHelper.ADMIN_MAYOR_CANON_FESTIVAL);
        mAV.addObject("festival", festival.get());
        mAV.addObject("x", x);
        mAV.addObject("totalUnidades", festivalService.traerUnidades(id).size());
        try {
            int cantidad = Integer.parseInt(x.trim());
            mAV.addObject("unidades", festivalService.unidadesMayorCanon(id, cantidad));
            mAV.addObject("cantidadSolicitada", cantidad);
        } catch (NumberFormatException ex) {
            mAV.addObject("error", "La cantidad de unidades debe ser un número entero mayor o igual a 1.");
            mAV.addObject("unidades", List.of());
        } catch (IllegalArgumentException ex) {
            mAV.addObject("error", ex.getMessage());
            mAV.addObject("unidades", List.of());
        }
        return mAV;
    }
}
