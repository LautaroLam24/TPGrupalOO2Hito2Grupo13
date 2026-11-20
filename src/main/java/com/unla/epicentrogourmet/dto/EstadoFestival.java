package com.unla.epicentrogourmet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado del festival")
public enum EstadoFestival {
    EN_CURSO,
    PROXIMO,
    FINALIZADO
}
