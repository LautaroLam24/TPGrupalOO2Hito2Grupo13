package com.unla.epicentrogourmet.dto;

public record ReporteMayoresCanonDTO(
        int posicion,
        String codigo,
        String nombreComercial,
        String tipo,
        double canon) {
}
