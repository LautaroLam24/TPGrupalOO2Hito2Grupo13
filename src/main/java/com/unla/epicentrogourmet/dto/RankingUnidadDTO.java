package com.unla.epicentrogourmet.dto;

public record RankingUnidadDTO(
        int posicion,
        String codigo,
        String nombreComercial,
        String tipo,
        double recaudacion) {
}
