package com.unla.epicentrogourmet.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class Costo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private float costoSuperficie;   // por m2
    private float costoMontaje;      // por minuto
    private float plusElectricidad;
    private float sueldoBase;
    private float plusAntiguedad;    // por año
    private float plusCocinero;
    private float plusAyudante;
    private float plusLavaplatos;
}