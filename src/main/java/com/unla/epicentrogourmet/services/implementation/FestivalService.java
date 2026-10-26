package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.RankingUnidadDTO;
import com.unla.epicentrogourmet.dto.ReporteVentaDTO;
import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.entities.UnidadDeVenta;
import com.unla.epicentrogourmet.exceptions.ReporteResponsableException;
import com.unla.epicentrogourmet.repositories.IFestivalRepository;
import com.unla.epicentrogourmet.repositories.IPedidoRepository;
import com.unla.epicentrogourmet.services.IFestivalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service("festivalService")
public class FestivalService implements IFestivalService {

    private final IFestivalRepository festivalRepository;
    private final ReporteResponsableService reporteResponsableService;
    private final IPedidoRepository pedidoRepository;

    public FestivalService(IFestivalRepository festivalRepository,
                           ReporteResponsableService reporteResponsableService,
                           IPedidoRepository pedidoRepository) {
        this.festivalRepository = festivalRepository;
        this.reporteResponsableService = reporteResponsableService;
        this.pedidoRepository = pedidoRepository;
    }

    @Override
    public List<Festival> traerTodos() {
        return festivalRepository.findAll();
    }

    @Override
    public List<Festival> traerDisponibles() {
        return festivalRepository.findByFechaFinGreaterThanEqualOrderByFechaInicioAsc(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteVentaDTO reporteRecaudacion(String username, int festivalId) {
        ReporteResponsableService.ContextoFestivalAutorizado contexto =
                reporteResponsableService.autorizarFestival(username, festivalId);
        Festival festival = contexto.festival();

        Double totalAgregado = pedidoRepository.sumTotalByFestivalAndFechas(
                festival.getId(), festival.getFechaInicio(), festival.getFechaFin());
        double total = totalAgregado == null ? 0.0d : totalAgregado;
        if (!Double.isFinite(total)
                || total > Float.MAX_VALUE
                || total < -Float.MAX_VALUE) {
            throw new ReporteResponsableException(
                    "No fue posible representar la recaudación total del festival.");
        }

        float recaudacion = (float) total;
        if (!Float.isFinite(recaudacion)) {
            throw new ReporteResponsableException(
                    "No fue posible representar la recaudación total del festival.");
        }
        return new ReporteVentaDTO(festival.getNombre(), recaudacion);
    }

    @Override
    public List<Festival> traerTodosPorFecha() {
        return festivalRepository.findAllByOrderByFechaInicioAsc();
    }

    @Override
    public Optional<Festival> traerPorId(int idFestival) {
        return festivalRepository.findById(idFestival);
    }

    @Override
    public List<UnidadDeVenta> traerUnidades(int idFestival) {
        return festivalRepository.findUnidadesByFestivalId(idFestival);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RankingUnidadDTO> rankingUnidades(int idFestival) {
        Map<Integer, Double> recaudacionPorUnidad = new HashMap<>();
        for (Object[] fila : pedidoRepository.findRecaudacionPorUnidadDeFestival(idFestival)) {
            recaudacionPorUnidad.put(((Number) fila[0]).intValue(), ((Number) fila[1]).doubleValue());
        }

        List<RankingUnidadDTO> sinPosicion = new ArrayList<>();
        for (UnidadDeVenta unidad : festivalRepository.findUnidadesByFestivalId(idFestival)) {
            sinPosicion.add(new RankingUnidadDTO(
                    0,
                    unidad.getCodigo(),
                    unidad.getNombreComercial(),
                    unidad.getTipo(),
                    recaudacionPorUnidad.getOrDefault(unidad.getId(), 0.0d)));
        }
        sinPosicion.sort(Comparator.comparingDouble(RankingUnidadDTO::recaudacion).reversed()
                .thenComparing(RankingUnidadDTO::nombreComercial));

        List<RankingUnidadDTO> ranking = new ArrayList<>();
        for (int i = 0; i < sinPosicion.size(); i++) {
            RankingUnidadDTO r = sinPosicion.get(i);
            ranking.add(new RankingUnidadDTO(
                    i + 1, r.codigo(), r.nombreComercial(), r.tipo(), r.recaudacion()));
        }
        return ranking;
    }
}
