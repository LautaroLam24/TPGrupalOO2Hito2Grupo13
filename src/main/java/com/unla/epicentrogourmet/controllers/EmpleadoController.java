package com.unla.epicentrogourmet.controllers;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
}