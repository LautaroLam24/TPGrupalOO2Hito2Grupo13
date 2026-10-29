package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.ReporteRentabilidadNetaDTO;
import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.entities.Personal;
import com.unla.epicentrogourmet.entities.UnidadDeVenta;
import com.unla.epicentrogourmet.repositories.IPedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RentabilidadNetaService {

    private final ReporteResponsableService reporteResponsableService;
    private final IPedidoRepository pedidoRepository;

    public RentabilidadNetaService(ReporteResponsableService reporteResponsableService,
                                   IPedidoRepository pedidoRepository) {
        this.reporteResponsableService = reporteResponsableService;
        this.pedidoRepository = pedidoRepository;
    }

    // Rentabilidad neta = (pedidos totales - costo de los platos) - sueldos del personal - canon
    @Transactional(readOnly = true)
    public ReporteRentabilidadNetaDTO calcular(String username, int festivalId) {
        ReporteResponsableService.ContextoFestivalAutorizado contexto =
                reporteResponsableService.autorizarFestival(username, festivalId);
        UnidadDeVenta unidad = contexto.unidad();
        Festival festival = contexto.festival();

        double pedidosTotales = valor(pedidoRepository.sumTotalByUnidadAndFechas(
                unidad.getId(), festival.getFechaInicio(), festival.getFechaFin()));
        double costoPlatos = valor(pedidoRepository.sumCostoPlatosByUnidadAndFechas(
                unidad.getId(), festival.getFechaInicio(), festival.getFechaFin()));
        double sueldos = unidad.getStaff().stream()
                .filter(personal -> !personal.isBaja())
                .mapToDouble(personal -> personal.calcularSueldo(festival))
                .sum();
        double canon = unidad.calcularCanon(festival);

        double gananciaBruta = pedidosTotales - costoPlatos;
        double rentabilidadNeta = gananciaBruta - sueldos - canon;

        return new ReporteRentabilidadNetaDTO(
                unidad.getNombreComercial(),
                festival.getNombre(),
                festival.getFechaInicio(),
                festival.getFechaFin(),
                pedidosTotales,
                costoPlatos,
                gananciaBruta,
                sueldos,
                canon,
                rentabilidadNeta);
    }

    private double valor(Double d) {
        return d == null ? 0.0d : d;
    }
}
