package com.unla.epicentrogourmet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ItemPedidoForm {

    @NotNull(message = "El plato es obligatorio.")
    private Integer platoId;

    @Positive(message = "La cantidad debe ser mayor que cero.")
    private Integer cantidad;

    public ItemPedidoForm(Integer platoId, Integer cantidad) {
        this.platoId = platoId;
        this.cantidad = cantidad;
    }
}
