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
    public float calcularCanon(Festival festival) {
        Costo costo = festival.getCosto();
        float canon = getSuperficieM2() * costo.getCostoSuperficie();
        if (requiereElectricidad) {
            canon += costo.getPlusElectricidad();
        }
        return canon;
    }

    @Override
    public String getTipo() {
        return "Food truck";
    }
}