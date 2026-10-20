package com.unla.epicentrogourmet.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class Cajero extends Personal {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Turno turno;

    @Override
    public float calcularSueldo(Festival festival) {
        Costo costo = festival.getCosto();
        int anios = calcularAntiguedad(festival.getFechaInicio());
        return factorLiquidacion(festival) * (costo.getSueldoBase() + anios * costo.getPlusAntiguedad());
    }
}