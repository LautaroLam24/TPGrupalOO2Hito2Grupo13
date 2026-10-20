package com.unla.epicentrogourmet.entities;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class PuestoDesarmable extends UnidadDeVenta {

    private int cantCarpas;
    private int tiempoMontajeMin;

    @Override
    public float calcularCanon(Festival festival) {
        Costo costo = festival.getCosto();
        return getSuperficieM2() * costo.getCostoSuperficie()
                - tiempoMontajeMin * costo.getCostoMontaje();
    }
}