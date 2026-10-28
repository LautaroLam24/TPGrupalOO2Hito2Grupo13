package com.unla.epicentrogourmet.dto;

import java.time.LocalDate;

import com.unla.epicentrogourmet.entities.CategoriaCocina;
import com.unla.epicentrogourmet.entities.Turno;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@NoArgsConstructor
public class ModificarPersonalForm {

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
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaNacimiento;

    @NotNull(message = "La fecha de ingreso es obligatoria.")
    @PastOrPresent(message = "La fecha de ingreso no puede ser futura.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaIngreso;

    // Solo para mostrar: no se modifican
    private String tipoEmpleado;
    private String email;

    // Datos exclusivos del cajero
    private Turno turno;

    // Datos exclusivos del empleado de cocina
    private CategoriaCocina categoria;

    @Size(max = 100, message = "La especialidad no puede superar los 100 caracteres.")
    private String especialidad;
}