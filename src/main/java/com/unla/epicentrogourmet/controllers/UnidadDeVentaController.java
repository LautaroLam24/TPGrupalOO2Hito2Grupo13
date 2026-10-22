package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.dto.RegistroUnidadResponsableForm;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller //controlador MVC
@RequestMapping(ViewRouteHelper.ROUTE_UNIDAD_REGISTRO) //ruta base para los endpoints de este controlador
public class UnidadDeVentaController {

    static final String FORM_ATTRIBUTE = "registroUnidadForm";

    private final UnidadDeVentaService unidadDeVentaService;
    private final IFestivalService festivalService;

    public UnidadDeVentaController(UnidadDeVentaService unidadDeVentaService,
                                   IFestivalService festivalService) {
        this.unidadDeVentaService = unidadDeVentaService;
        this.festivalService = festivalService;
    }

    @GetMapping
    public String mostrarFormulario(Model model) {
        if (!model.containsAttribute(FORM_ATTRIBUTE)) {
            model.addAttribute(FORM_ATTRIBUTE, new RegistroUnidadResponsableForm());
        }
        cargarFestivales(model);
        return ViewRouteHelper.UNIDAD_REGISTRO;
    }

    @PostMapping
    public String registrar(@Valid @ModelAttribute(FORM_ATTRIBUTE) RegistroUnidadResponsableForm form,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            form.setPassword(null);
            cargarFestivales(model);
            return ViewRouteHelper.UNIDAD_REGISTRO;
        }

        try {
            unidadDeVentaService.registrarConResponsable(form);
        } catch (RegistroUnidadException ex) {
            bindingResult.reject("registro.unidad.error", ex.getMessage());
            form.setPassword(null);
            cargarFestivales(model);
            return ViewRouteHelper.UNIDAD_REGISTRO;
        }

        redirectAttributes.addFlashAttribute(
                "mensajeExito", "La unidad y su responsable se registraron correctamente.");
        return "redirect:" + ViewRouteHelper.ROUTE_UNIDAD_REGISTRO;
    }

    private void cargarFestivales(Model model) {
        model.addAttribute("festivales", festivalService.traerDisponibles());
    }
}
