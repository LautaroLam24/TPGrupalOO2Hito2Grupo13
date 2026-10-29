package com.unla.epicentrogourmet.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class FoodTruck extends UnidadDeVenta {

    @Column(nullable = false, unique = true, length = 10)
    private String patente;

    private boolean requiereElectricidad;

    @Override
    public float calcularAjusteCanon(Festival festival) {
        return requiereElectricidad ? festival.getCosto().getPlusElectricidad() : 0;
    }

    @Override
    public String describirAjusteCanon(Festival festival) {
        return requiereElectricidad ? "Plus por conexión eléctrica" : "Sin conexión eléctrica";
    }

    @Override
    public String getTipo() {
        return "Food truck";
    }
}