package com.unla.epicentrogourmet.dto;

import java.util.List;

public record ReporteCanonDTO(
        String nombreComercial,
        String tipo,
        String codigo,
        float superficieM2,
        Boolean requiereElectricidad,
        Integer cantCarpas,
        Integer tiempoMontajeMin,
        List<CanonUnidadDTO> canones,
        double canonTotal) {
}
