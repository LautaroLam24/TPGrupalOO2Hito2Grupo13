package com.unla.epicentrogourmet.dto;

import java.time.LocalDate;

public record PedidoCierreDTO(int id, LocalDate fecha, float importe) {
}
