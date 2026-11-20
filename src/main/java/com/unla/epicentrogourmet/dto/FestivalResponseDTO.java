package com.unla.epicentrogourmet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Datos del festival")
public record FestivalResponseDTO(
        @Schema(description = "ID del festival", example = "4") int id,
        @Schema(description = "Nombre del festival", example = "Festival de la Milanesa") String nombre,
        @Schema(description = "Fecha de inicio", type = "string", format = "date", example = "2027-03-10")
        LocalDate fechaInicio,
        @Schema(description = "Fecha de fin", type = "string", format = "date", example = "2027-03-14")
        LocalDate fechaFin,
        @Schema(description = "Duración en días", example = "5") int duracionDias,
        @Schema(description = "Estado del festival", example = "PROXIMO") EstadoFestival estado,
        @Schema(description = "Cantidad de unidades", example = "0") int cantidadUnidades,
        @Schema(description = "Costo por m²", example = "500") float costoSuperficie,
        @Schema(description = "Montaje por minuto", example = "10") float costoMontaje,
        @Schema(description = "Plus por electricidad", example = "2000") float plusElectricidad,
        @Schema(description = "Sueldo base", example = "100000") float sueldoBase,
        @Schema(description = "Plus por antigüedad", example = "5000") float plusAntiguedad,
        @Schema(description = "Plus de cocinero", example = "150000") float plusCocinero,
        @Schema(description = "Plus de ayudante", example = "100000") float plusAyudante,
        @Schema(description = "Plus de lavaplatos", example = "50000") float plusLavaplatos) {
}
