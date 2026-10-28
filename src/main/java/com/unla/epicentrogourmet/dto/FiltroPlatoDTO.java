package com.unla.epicentrogourmet.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FiltroPlatoDTO {

    private String nombre;

    @PositiveOrZero(message = "El precio mínimo no puede ser negativo.")
    private Float precioVentaMinimo;

    @PositiveOrZero(message = "El precio máximo no puede ser negativo.")
    private Float precioVentaMaximo;

    @PositiveOrZero(message = "El costo mínimo no puede ser negativo.")
    private Float costoProduccionMinimo;

    @PositiveOrZero(message = "El costo máximo no puede ser negativo.")
    private Float costoProduccionMaximo;

    private EstadoPlato estado = EstadoPlato.TODOS;

    public enum EstadoPlato {
        TODOS,
        ACTIVOS,
        INACTIVOS
    }
}
