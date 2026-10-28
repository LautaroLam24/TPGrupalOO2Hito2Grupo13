package com.unla.epicentrogourmet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlatoForm {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
    private String nombre;

    @NotNull(message = "El precio de venta es obligatorio.")
    @Positive(message = "El precio de venta debe ser mayor que cero.")
    private Float precioVenta;

    @NotNull(message = "El costo de producción es obligatorio.")
    @PositiveOrZero(message = "El costo de producción debe ser mayor o igual que cero.")
    private Float costoProduccion;
}
