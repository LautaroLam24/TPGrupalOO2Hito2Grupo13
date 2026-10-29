package com.unla.epicentrogourmet.dto;

import java.time.LocalDate;

public record HistorialFestivalDTO(
        String festival,
        String unidad,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String estado) {
}