package com.unla.epicentrogourmet.dto;

import java.time.LocalDate;

public record EmpleadoListadoDTO(
        int id,
        String nombreCompleto,
        int dni,
        String tipo,
        String detalle,
        LocalDate fechaIngreso,
        int antiguedad,
        String email) {
}