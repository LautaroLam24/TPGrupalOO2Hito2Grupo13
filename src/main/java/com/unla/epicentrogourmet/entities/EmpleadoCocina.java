package com.unla.epicentrogourmet.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class EmpleadoCocina extends Personal {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaCocina categoria;

    @Column(length = 100)
    private String especialidad;

    @Override
    public float calcularSueldo(Festival festival) {
        Costo costo = festival.getCosto();
        return factorLiquidacion(festival) * (costo.getSueldoBase() + plusCategoria(costo));
    }

    private float plusCategoria(Costo costo) {
        return switch (categoria) {
            case COCINERO -> costo.getPlusCocinero();
            case AYUDANTE -> costo.getPlusAyudante();
            case LAVAPLATOS -> costo.getPlusLavaplatos();
        };
    }
}