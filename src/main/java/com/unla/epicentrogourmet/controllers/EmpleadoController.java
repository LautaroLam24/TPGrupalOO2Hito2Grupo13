package com.unla.epicentrogourmet.controllers;

import java.security.Principal;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.unla.epicentrogourmet.dto.EmpleadoListadoDTO;
import com.unla.epicentrogourmet.dto.ModificarPersonalForm;
import com.unla.epicentrogourmet.services.implementation.PersonalService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/responsable/empleados")
@PreAuthorize("hasRole('RESPONSABLE')")
public class EmpleadoController {

    private final PersonalService personalService;

    public EmpleadoController(PersonalService personalService) {
        this.personalService = personalService;
    }

    @GetMapping
    public String listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String tipo,
            Principal principal,
            Model model) {

        List<EmpleadoListadoDTO> empleados =
                personalService.filtrar(principal.getName(), texto, tipo);

        model.addAttribute("empleados", empleados);
        model.addAttribute("texto", texto == null ? "" : texto);
        model.addAttribute("tipo", tipo == null ? "" : tipo);
        return "responsable/empleados/listado";
    }

    @GetMapping("/inactivos")
    public String listarInactivos(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String tipo,
            Principal principal,
            Model model) {

        model.addAttribute("empleados",
                personalService.filtrarInactivos(principal.getName(), texto, tipo));
        model.addAttribute("texto", texto == null ? "" : texto);
        model.addAttribute("tipo", tipo == null ? "" : tipo);
        return "responsable/empleados/inactivos";
    }

    @PostMapping("/{id}/baja")
    public String baja(
            @PathVariable int id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            String nombre = personalService.baja(principal.getName(), id);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Se dio de baja a " + nombre + ". Ya no puede iniciar sesión.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/responsable/empleados";
    }

    @PostMapping("/{id}/reactivar")
    public String reactivar(
            @PathVariable int id,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            String nombre = personalService.reactivar(principal.getName(), id);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Se reactivó a " + nombre + ". Ya puede iniciar sesión.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/responsable/empleados/inactivos";
    }

    @GetMapping("/{id}/editar")
    public String mostrarEdicion(
            @PathVariable int id,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            model.addAttribute("modificarPersonalForm",
                    personalService.obtenerParaModificar(principal.getName(), id));
            model.addAttribute("empleadoId", id);
            return "responsable/empleados/editar";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/responsable/empleados";
        }
    }

    @PostMapping("/{id}/editar")
    public String editar(
            @PathVariable int id,
            @Valid @ModelAttribute("modificarPersonalForm") ModificarPersonalForm form,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        model.addAttribute("empleadoId", id);

        if (bindingResult.hasErrors()) {
            return "responsable/empleados/editar";
        }

        try {
            String nombre = personalService.modificar(principal.getName(), id, form);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Se modificaron los datos de " + nombre + ".");
            return "redirect:/responsable/empleados";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("modificar.personal.error", ex.getMessage());
            return "responsable/empleados/editar";
        }
    }
}