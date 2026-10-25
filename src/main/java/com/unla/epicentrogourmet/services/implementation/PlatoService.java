package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.PlatoDTO;
import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.entities.UnidadDeVenta;
import com.unla.epicentrogourmet.exceptions.PlatoEstrellaException;
import com.unla.epicentrogourmet.exceptions.ReporteResponsableException;
import com.unla.epicentrogourmet.repositories.IPedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PlatoService {

    private final ReporteResponsableService reporteResponsableService;
    private final IPedidoRepository pedidoRepository;

    public PlatoService(ReporteResponsableService reporteResponsableService,
                        IPedidoRepository pedidoRepository) {
        this.reporteResponsableService = reporteResponsableService;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public ContextoPlatoEstrella preparar(String username) {
        try {
            ReporteResponsableService.ContextoReportes contexto =
                    reporteResponsableService.preparar(username);
            List<FestivalOpcion> festivales = contexto.festivales().stream()
                    .map(festival -> new FestivalOpcion(
                            festival.id(),
                            festival.nombre(),
                            festival.fechaInicio(),
                            festival.fechaFin()))
                    .toList();
            return new ContextoPlatoEstrella(contexto.unidad(), festivales);
        } catch (ReporteResponsableException ex) {
            throw new PlatoEstrellaException(ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Optional<PlatoDTO> platoEstrella(String username, int festivalId) {
        ReporteResponsableService.ContextoFestivalAutorizado contexto;
        try {
            contexto = reporteResponsableService.autorizarFestival(username, festivalId);
        } catch (ReporteResponsableException ex) {
            throw new PlatoEstrellaException(ex.getMessage());
        }
        UnidadDeVenta unidad = contexto.unidad();
        Festival festival = contexto.festival();

        List<Object[]> ranking = pedidoRepository.findRankingPlatosPorPedidos(
                unidad.getId(), festival.getFechaInicio(), festival.getFechaFin());
        if (ranking == null) {
            throw resultadoInconsistente();
        }
        if (ranking.isEmpty()) {
            return Optional.empty();
        }

        Object[] ganador = ranking.get(0);
        if (ganador == null
                || ganador.length < 3
                || !(ganador[0] instanceof Number)
                || !(ganador[1] instanceof String nombre)
                || nombre.isBlank()
                || !(ganador[2] instanceof Long cantidad)
                || cantidad <= 0
                || cantidad > Integer.MAX_VALUE) {
            throw resultadoInconsistente();
        }

        return Optional.of(new PlatoDTO(
                nombre,
                cantidad.intValue(),
                unidad.getNombreComercial()));
    }

    private PlatoEstrellaException resultadoInconsistente() {
        return new PlatoEstrellaException(
                "No fue posible determinar el plato estrella con los datos disponibles.");
    }

    public record FestivalOpcion(
            int id,
            String nombre,
            LocalDate fechaInicio,
            LocalDate fechaFin) {
    }

    public record ContextoPlatoEstrella(
            String unidad,
            List<FestivalOpcion> festivales) {

        public ContextoPlatoEstrella {
            festivales = List.copyOf(festivales);
        }
    }
}
