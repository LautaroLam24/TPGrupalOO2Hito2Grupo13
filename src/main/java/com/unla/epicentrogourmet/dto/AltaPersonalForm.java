package com.unla.epicentrogourmet.dto;

import com.unla.epicentrogourmet.entities.CategoriaCocina;
import com.unla.epicentrogourmet.entities.Turno;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class AltaPersonalForm {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 60, message = "El nombre no puede superar los 60 caracteres.")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio.")
    @Size(max = 60, message = "El apellido no puede superar los 60 caracteres.")
    private String apellido;

    @NotNull(message = "El DNI es obligatorio.")
    @Positive(message = "El DNI debe ser positivo.")
    private Integer dni;

    @NotNull(message = "La fecha de nacimiento es obligatoria.")
    @Past(message = "La fecha de nacimiento debe ser anterior a hoy.")
    private LocalDate fechaNacimiento;

    @NotNull(message = "La fecha de ingreso es obligatoria.")
    @PastOrPresent(message = "La fecha de ingreso no puede ser futura.")
    private LocalDate fechaIngreso;

    @NotBlank(message = "El email es obligatorio.")
    @Email(message = "Debe ingresar un email válido.")
    @Size(max = 100, message = "El email no puede superar los 100 caracteres.")
    private String email;

    @NotBlank(message = "Debe seleccionar un tipo de empleado.")
    private String tipoEmpleado;

    // Datos exclusivos del cajero
    private Turno turno;

    // Datos exclusivos del empleado de cocina
    private CategoriaCocina categoria;
    private String especialidad;
}