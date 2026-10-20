package com.unla.epicentrogourmet.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int cantidad;

    @ManyToOne(optional = false)
    @JoinColumn(name = "plato_id")
    private Plato plato;

    public ItemPedido(Plato plato, int cantidad) {
        this.plato = plato;
        this.cantidad = cantidad;
    }

    public float calcularSubtotal() {
        return cantidad * plato.getPrecioVenta();
    }
}