package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.dto.AltaPersonalForm;
import com.unla.epicentrogourmet.dto.CredencialesAltaDTO;
import com.unla.epicentrogourmet.services.implementation.EmailService;
import com.unla.epicentrogourmet.services.implementation.PersonalService;
import jakarta.validation.Valid;
import org.springframework.mail.MailException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('RESPONSABLE')")
public class PersonalController {

    private final PersonalService personalService;
    private final EmailService emailService;

    public PersonalController(
            PersonalService personalService,
            EmailService emailService) {
        this.personalService = personalService;
        this.emailService = emailService;
    }

    @GetMapping("/responsable/empleados/alta")
    public String mostrarFormulario(Model model) {
        if (!model.containsAttribute("altaPersonalForm")) {
            model.addAttribute(
                    "altaPersonalForm",
                    new AltaPersonalForm());
        }

        return "empleado/alta";
    }

    @PostMapping("/responsable/empleados/alta")
    public String registrar(
            @Valid @ModelAttribute("altaPersonalForm")
            AltaPersonalForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "empleado/alta";
        }

        CredencialesAltaDTO credenciales;

        try {
            credenciales = personalService.alta(form);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject(
                    "alta.personal.error",
                    ex.getMessage());
            return "empleado/alta";
        }

        try {
            emailService.enviarCredenciales(
                    credenciales.email(),
                    credenciales.nombreEmpleado(),
                    credenciales.email(),
                    credenciales.passwordTemporal());

            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El empleado fue registrado y se enviaron "
                            + "sus credenciales por correo.");

        } catch (MailException ex) {
            ex.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "mensajeAdvertencia",
                    "El empleado fue registrado correctamente, "
                            + "pero no se pudo enviar el correo. ");
        }

        return "redirect:/responsable/empleados/alta";
    }
}

