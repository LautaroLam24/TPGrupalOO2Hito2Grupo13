package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.ReporteRentabilidadNetaDTO;
import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.entities.UnidadDeVenta;
import com.unla.epicentrogourmet.exceptions.ReporteResponsableException;
import com.unla.epicentrogourmet.repositories.IPedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class RentabilidadNetaService {

    private final ReporteResponsableService reporteResponsableService;
    private final IPedidoRepository pedidoRepository;

    public RentabilidadNetaService(ReporteResponsableService reporteResponsableService,
                                   IPedidoRepository pedidoRepository) {
        this.reporteResponsableService = reporteResponsableService;
        this.pedidoRepository = pedidoRepository;
    }

    // Rentabilidad neta del festival completo
    @Transactional(readOnly = true)
    public ReporteRentabilidadNetaDTO calcular(String username, int festivalId) {
        ReporteResponsableService.ContextoFestivalAutorizado contexto =
                reporteResponsableService.autorizarFestival(username, festivalId);
        Festival festival = contexto.festival();
        return calcularPeriodo(contexto.unidad(), festival, festival.getFechaInicio(), festival.getFechaFin());
    }

    // Rentabilidad neta entre dos fechas (inclusive) dentro del periodo del festival
    @Transactional(readOnly = true)
    public ReporteRentabilidadNetaDTO calcularEntreFechas(String username, int festivalId,
                                                          LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ReporteResponsableException("Debe indicar la fecha desde y la fecha hasta.");
        }
        if (desde.isAfter(hasta)) {
            throw new ReporteResponsableException(
                    "La fecha desde no puede ser posterior a la fecha hasta.");
        }
        ReporteResponsableService.ContextoFestivalAutorizado contexto =
                reporteResponsableService.autorizarFestival(username, festivalId);
        Festival festival = contexto.festival();
        if (desde.isBefore(festival.getFechaInicio()) || hasta.isAfter(festival.getFechaFin())) {
            throw new ReporteResponsableException(
                    "El rango de fechas debe estar dentro del período del festival ("
                            + festival.getFechaInicio() + " a " + festival.getFechaFin() + ").");
        }
        return calcularPeriodo(contexto.unidad(), festival, desde, hasta);
    }

    // Rentabilidad neta = (pedidos totales - costo de los platos) - sueldos - canon.
    // Si el periodo es parcial, sueldos y canon se prorratean por dias sobre la duracion del festival.
    private ReporteRentabilidadNetaDTO calcularPeriodo(UnidadDeVenta unidad, Festival festival,
                                                       LocalDate desde, LocalDate hasta) {
        double pedidosTotales = valor(pedidoRepository.sumTotalByUnidadAndFechas(
                unidad.getId(), desde, hasta));
        double costoPlatos = valor(pedidoRepository.sumCostoPlatosByUnidadAndFechas(
                unidad.getId(), desde, hasta));

        long diasPeriodo = ChronoUnit.DAYS.between(desde, hasta) + 1;
        double proporcion = (double) diasPeriodo / festival.calcularDuracionDias();

        double sueldos = unidad.getStaff().stream()
                .filter(personal -> !personal.isBaja())
                .mapToDouble(personal -> personal.calcularSueldo(festival))
                .sum() * proporcion;
        double canon = unidad.calcularCanon(festival) * proporcion;

        double gananciaBruta = pedidosTotales - costoPlatos;
        double rentabilidadNeta = gananciaBruta - sueldos - canon;

        return new ReporteRentabilidadNetaDTO(
                unidad.getNombreComercial(),
                festival.getNombre(),
                desde,
                hasta,
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
