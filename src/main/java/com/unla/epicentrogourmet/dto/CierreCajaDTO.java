package com.unla.epicentrogourmet.dto;

import java.time.LocalDate;
import java.util.List;

public record CierreCajaDTO(
        String unidad,
        String cajero,
        LocalDate fecha,
        int cantidadPedidos,
        float total,
        List<PedidoCierreDTO> pedidos) {

    public CierreCajaDTO {
        pedidos = List.copyOf(pedidos);
    }
}
