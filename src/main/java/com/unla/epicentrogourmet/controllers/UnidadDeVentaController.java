package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.dto.RegistroUnidadResponsableForm;
import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.exceptions.RegistroUnidadException;
import com.unla.epicentrogourmet.helpers.ViewRouteHelper;
import com.unla.epicentrogourmet.services.IFestivalService;
import com.unla.epicentrogourmet.services.implementation.UnidadDeVentaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller //controlador MVC
@RequestMapping(ViewRouteHelper.ROUTE_ADMIN_UNIDAD_NUEVA) //ruta base para los endpoints de este controlador
public class UnidadDeVentaController {

    static final String FORM_ATTRIBUTE = "registroUnidadForm";

    private final UnidadDeVentaService unidadDeVentaService;
    private final IFestivalService festivalService;

    public UnidadDeVentaController(UnidadDeVentaService unidadDeVentaService,
                                   IFestivalService festivalService) {
        this.unidadDeVentaService = unidadDeVentaService;
        this.festivalService = festivalService;
    }

    // GET: /admin/unidades/nueva?festivalId=2 (festivalId opcional para preseleccionar)
    @GetMapping
    public String mostrarFormulario(@RequestParam(name = "festivalId", required = false) Integer festivalId,
                                    Model model) {
        List<Festival> festivales = festivalService.traerDisponibles();
        if (!model.containsAttribute(FORM_ATTRIBUTE)) {
            RegistroUnidadResponsableForm form = new RegistroUnidadResponsableForm();
            if (festivalId != null && festivales.stream().anyMatch(f -> f.getId() == festivalId)) {
                form.setFestivalId(festivalId);
            }
            model.addAttribute(FORM_ATTRIBUTE, form);
        }
        model.addAttribute("festivales", festivales);
        return ViewRouteHelper.ADMIN_UNIDAD_FORM;
    }

    @PostMapping
    public String registrar(@Valid @ModelAttribute(FORM_ATTRIBUTE) RegistroUnidadResponsableForm form,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            form.setPassword(null);
            cargarFestivales(model);
            return ViewRouteHelper.ADMIN_UNIDAD_FORM;
        }

        try {
            unidadDeVentaService.registrarConResponsable(form);
        } catch (RegistroUnidadException ex) {
            bindingResult.reject("registro.unidad.error", ex.getMessage());
            form.setPassword(null);
            cargarFestivales(model);
            return ViewRouteHelper.ADMIN_UNIDAD_FORM;
        }

        redirectAttributes.addFlashAttribute("mensajeExito",
                "La unidad " + form.getNombreComercial() + " y su responsable (" + form.getUsername()
                        + ") se registraron correctamente.");
        return "redirect:" + ViewRouteHelper.ROUTE_ADMIN_FESTIVALES + "/" + form.getFestivalId() + "/unidades";
    }

    private void cargarFestivales(Model model) {
        model.addAttribute("festivales", festivalService.traerDisponibles());
    }
}
