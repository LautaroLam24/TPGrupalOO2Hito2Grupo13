package com.unla.epicentrogourmet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de error")
public record ErrorResponseDTO(
        @Schema(description = "Mensaje de error",
                example = "La fecha de fin no puede ser anterior a la fecha de inicio.")
        String error) {
}
