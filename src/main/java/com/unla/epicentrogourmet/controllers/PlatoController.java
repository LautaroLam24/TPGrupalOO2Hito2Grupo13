package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.dto.PlatoForm;
import com.unla.epicentrogourmet.exceptions.GestionPlatoException;
import com.unla.epicentrogourmet.helpers.ViewRouteHelper;
import com.unla.epicentrogourmet.services.implementation.PlatoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping(ViewRouteHelper.ROUTE_PLATOS_RESPONSABLE)
@PreAuthorize("hasRole('RESPONSABLE')")
public class PlatoController {

    static final String FORM_ATTRIBUTE = "platoForm";

    private final PlatoService platoService;

    public PlatoController(PlatoService platoService) {
        this.platoService = platoService;
    }

    @GetMapping
    public String listar(Authentication authentication, Model model) {
        try {
            PlatoService.ContextoGestionPlatos contexto =
                    platoService.prepararGestion(nombreUsuario(authentication));
            model.addAttribute("unidad", contexto.unidad());
            model.addAttribute("platos", contexto.platos());
        } catch (GestionPlatoException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return ViewRouteHelper.PLATOS_RESPONSABLE;
    }

    @GetMapping("/nuevo")
    public String mostrarAlta(Authentication authentication, Model model) {
        cargarAltaSegura(authentication, model);
        return ViewRouteHelper.PLATO_FORM;
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute(FORM_ATTRIBUTE) PlatoForm form,
                        BindingResult bindingResult,
                        Authentication authentication,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            cargarAltaSegura(authentication, model);
            return ViewRouteHelper.PLATO_FORM;
        }

        try {
            PlatoService.PlatoResumen creado =
                    platoService.alta(nombreUsuario(authentication), form);
            redirectAttributes.addFlashAttribute(
                    "mensajeExito", "El Plato " + creado.nombre() + " se creó correctamente.");
            return redirigirAlListado();
        } catch (GestionPlatoException ex) {
            bindingResult.reject("plato.gestion.error", ex.getMessage());
            cargarAltaSegura(authentication, model);
            return ViewRouteHelper.PLATO_FORM;
        }
    }

    @GetMapping("/{platoId}/editar")
    public String mostrarModificacion(@PathVariable int platoId,
                                      Authentication authentication,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        try {
            cargarModificacion(authentication, platoId, model);
            return ViewRouteHelper.PLATO_FORM;
        } catch (GestionPlatoException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return redirigirAlListado();
        }
    }

    @PostMapping("/{platoId}")
    public String modificar(@PathVariable int platoId,
                            @Valid @ModelAttribute(FORM_ATTRIBUTE) PlatoForm form,
                            BindingResult bindingResult,
                            Authentication authentication,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            cargarModificacionSegura(authentication, platoId, model);
            return ViewRouteHelper.PLATO_FORM;
        }

        try {
            PlatoService.PlatoResumen modificado =
                    platoService.modificar(nombreUsuario(authentication), platoId, form);
            redirectAttributes.addFlashAttribute(
                    "mensajeExito", "El Plato " + modificado.nombre() + " se modificó correctamente.");
            return redirigirAlListado();
        } catch (GestionPlatoException ex) {
            bindingResult.reject("plato.gestion.error", ex.getMessage());
            cargarModificacionSegura(authentication, platoId, model);
            return ViewRouteHelper.PLATO_FORM;
        }
    }

    @PostMapping("/{platoId}/baja")
    public String darDeBaja(@PathVariable int platoId,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            platoService.baja(nombreUsuario(authentication), platoId);
            redirectAttributes.addFlashAttribute(
                    "mensajeExito", "El Plato se dio de baja correctamente.");
        } catch (GestionPlatoException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return redirigirAlListado();
    }

    private void cargarAltaSegura(Authentication authentication, Model model) {
        try {
            PlatoService.ContextoFormularioPlato contexto =
                    platoService.prepararAlta(nombreUsuario(authentication));
            cargarContextoFormulario(contexto, null, model);
        } catch (GestionPlatoException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("edicion", false);
            if (!model.containsAttribute(FORM_ATTRIBUTE)) {
                model.addAttribute(FORM_ATTRIBUTE, new PlatoForm());
            }
        }
    }

    private void cargarModificacion(Authentication authentication, int platoId, Model model) {
        PlatoService.ContextoFormularioPlato contexto = platoService.prepararModificacion(
                nombreUsuario(authentication), platoId);
        cargarContextoFormulario(contexto, platoId, model);
    }

    private void cargarModificacionSegura(Authentication authentication,
                                           int platoId,
                                           Model model) {
        try {
            cargarModificacion(authentication, platoId, model);
        } catch (GestionPlatoException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("edicion", true);
            model.addAttribute("platoId", platoId);
        }
    }

    private void cargarContextoFormulario(PlatoService.ContextoFormularioPlato contexto,
                                          Integer platoId,
                                          Model model) {
        model.addAttribute("unidad", contexto.unidad());
        model.addAttribute("edicion", contexto.edicion());
        if (platoId != null) {
            model.addAttribute("platoId", platoId);
        }
        if (!model.containsAttribute(FORM_ATTRIBUTE)) {
            model.addAttribute(FORM_ATTRIBUTE, contexto.form());
        }
    }

    private String nombreUsuario(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }

    private String redirigirAlListado() {
        return "redirect:" + ViewRouteHelper.ROUTE_PLATOS_RESPONSABLE;
    }
}
