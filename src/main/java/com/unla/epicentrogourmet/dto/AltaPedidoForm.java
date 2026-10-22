package com.unla.epicentrogourmet.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AltaPedidoForm {

    @NotEmpty(message = "El pedido debe contener al menos un plato disponible.")
    @Valid
    private List<ItemPedidoForm> items = new ArrayList<>();

    public AltaPedidoForm(List<ItemPedidoForm> items) {
        this.items = new ArrayList<>(items);
    }
}
