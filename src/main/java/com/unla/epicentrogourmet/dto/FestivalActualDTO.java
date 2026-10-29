package com.unla.epicentrogourmet.dto;

import java.time.LocalDate;

public record FestivalActualDTO(
        boolean tieneUnidad,
        boolean trabajaHoy,
        String mensaje,
        String unidad,
        String festival,
        LocalDate fechaInicio,
        LocalDate fechaFin) {
}