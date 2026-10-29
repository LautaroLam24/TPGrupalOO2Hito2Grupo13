package com.unla.epicentrogourmet.dto;

import java.time.LocalDate;

public record CanonUnidadDTO(
        String festival,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String estado,
        float superficieM2,
        float costoSuperficie,
        double subtotalSuperficie,
        String conceptoAjuste,
        float costoUnitarioAjuste,
        double ajuste,
        double canon) {
}
