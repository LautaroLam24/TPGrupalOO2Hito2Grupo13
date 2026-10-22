package com.unla.epicentrogourmet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegistroUnidadResponsableForm {

    @NotNull(message = "Debe seleccionar un festival.")
    private Integer festivalId;

    @NotBlank(message = "Debe seleccionar un tipo de unidad.")
    private String tipoUnidad;

    @NotBlank(message = "El código es obligatorio.")
    @Size(min = 10, max = 10, message = "El código debe tener exactamente 10 caracteres.")
    @Pattern(regexp = "[A-Za-z0-9]+", message = "El código sólo puede contener letras y números.")
    private String codigo;

    @NotBlank(message = "El nombre comercial es obligatorio.")
    @Size(max = 100, message = "El nombre comercial no puede superar los 100 caracteres.")
    private String nombreComercial;

    @NotNull(message = "La superficie es obligatoria.")
    @Positive(message = "La superficie debe ser mayor que cero.")
    private Float superficieM2;

    @NotBlank(message = "El email es obligatorio.")
    @Email(message = "Debe ingresar un email válido.")
    @Size(max = 100, message = "El email no puede superar los 100 caracteres.")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria.")
    private String password;

    @Size(max = 10, message = "La patente no puede superar los 10 caracteres.")
    private String patente;

    private boolean requiereElectricidad;

    @Positive(message = "La cantidad de carpas debe ser mayor que cero.")
    private Integer cantCarpas;

    @PositiveOrZero(message = "El tiempo de montaje debe ser mayor o igual a cero.")
    private Integer tiempoMontajeMin;
}
