package com.unla.epicentrogourmet.dto;

import java.time.LocalDate;

public record ReporteRentabilidadNetaDTO(
        String unidad,
        String festival,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        double pedidosTotales,
        double costoPlatos,
        double gananciaBruta,
        double sueldos,
        double canon,
        double rentabilidadNeta) {
}
