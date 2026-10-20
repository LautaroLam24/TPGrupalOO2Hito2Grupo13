package com.unla.epicentrogourmet.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter @NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private LocalDate fecha;

    private float total;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Set<ItemPedido> items = new HashSet<>();

    public float calcularTotal() {
        float suma = 0;
        for (ItemPedido item : items) {
            suma += item.calcularSubtotal();
        }
        return suma;
    }
}