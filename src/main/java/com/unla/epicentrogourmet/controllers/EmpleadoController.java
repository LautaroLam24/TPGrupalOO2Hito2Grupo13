package com.unla.epicentrogourmet.controllers;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.unla.epicentrogourmet.dto.EmpleadoListadoDTO;
import com.unla.epicentrogourmet.services.implementation.PersonalService;

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
            Model model) {

        List<EmpleadoListadoDTO> empleados = personalService.filtrar(texto, tipo);

        model.addAttribute("empleados", empleados);
        model.addAttribute("texto", texto == null ? "" : texto);
        model.addAttribute("tipo", tipo == null ? "" : tipo);
        return "responsable/empleados/listado";
    }

    @PostMapping("/{id}/baja")
    public String baja(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            String nombre = personalService.baja(id);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Se dio de baja a " + nombre + ". Ya no puede iniciar sesión.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/responsable/empleados";
    }

    @GetMapping("/inactivos")
    public String listarInactivos(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String tipo,
            Model model) {

        model.addAttribute("empleados", personalService.filtrarInactivos(texto, tipo));
        model.addAttribute("texto", texto == null ? "" : texto);
        model.addAttribute("tipo", tipo == null ? "" : tipo);
        return "responsable/empleados/inactivos";
    }

    @PostMapping("/{id}/reactivar")
    public String reactivar(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            String nombre = personalService.reactivar(id);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Se reactivó a " + nombre + ". Ya puede iniciar sesión.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/responsable/empleados/inactivos";
    }
}