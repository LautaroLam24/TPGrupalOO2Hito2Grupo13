package com.unla.epicentrogourmet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Alta de festival")
public record FestivalRequestDTO(
        @Schema(description = "Nombre del festival", example = "Festival de la Milanesa")
        @NotBlank(message = "es obligatorio")
        @Size(max = 100, message = "no puede superar los 100 caracteres")
        String nombre,

        @Schema(description = "Fecha de inicio", type = "string", format = "date", example = "2027-03-10")
        @NotNull(message = "es obligatoria")
        LocalDate fechaInicio,

        @Schema(description = "Fecha de fin", type = "string", format = "date", example = "2027-03-14")
        @NotNull(message = "es obligatoria")
        LocalDate fechaFin,

        @Schema(description = "Costo por m²", example = "500")
        @NotNull(message = "es obligatorio")
        Float costoSuperficie,

        @Schema(description = "Montaje por minuto", example = "10")
        @NotNull(message = "es obligatorio")
        Float costoMontaje,

        @Schema(description = "Plus por electricidad", example = "2000")
        @NotNull(message = "es obligatorio")
        Float plusElectricidad,

        @Schema(description = "Sueldo base", example = "100000")
        @NotNull(message = "es obligatorio")
        Float sueldoBase,

        @Schema(description = "Plus por antigüedad", example = "5000")
        @NotNull(message = "es obligatorio")
        Float plusAntiguedad,

        @Schema(description = "Plus de cocinero", example = "150000")
        @NotNull(message = "es obligatorio")
        Float plusCocinero,

        @Schema(description = "Plus de ayudante", example = "100000")
        @NotNull(message = "es obligatorio")
        Float plusAyudante,

        @Schema(description = "Plus de lavaplatos", example = "50000")
        @NotNull(message = "es obligatorio")
        Float plusLavaplatos) {
}
