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
    public float calcularAjusteCanon(Festival festival) {
        return -(tiempoMontajeMin * festival.getCosto().getCostoMontaje());
    }

    @Override
    public String describirAjusteCanon(Festival festival) {
        return "Descuento por montaje (" + tiempoMontajeMin + " min)";
    }

    @Override
    public String getTipo() {
        return "Puesto desarmable";
    }
}