package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.EstadoFestival;
import com.unla.epicentrogourmet.dto.FestivalRequestDTO;
import com.unla.epicentrogourmet.dto.FestivalResponseDTO;
import com.unla.epicentrogourmet.dto.RankingUnidadDTO;
import com.unla.epicentrogourmet.dto.ReporteMayoresCanonDTO;
import com.unla.epicentrogourmet.dto.ReporteVentaDTO;
import com.unla.epicentrogourmet.entities.Costo;
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
import java.util.Locale;
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

    @Override
    @Transactional(readOnly = true)
    public List<ReporteMayoresCanonDTO> unidadesMayorCanon(int idFestival, int x) {
        if (x < 1) {
            throw new IllegalArgumentException(
                    "La cantidad de unidades debe ser un número entero mayor o igual a 1.");
        }
        Festival festival = traerPorId(idFestival)
                .orElseThrow(() -> new IllegalArgumentException("El festival solicitado no existe."));

        List<ReporteMayoresCanonDTO> sinPosicion = new ArrayList<>();
        for (UnidadDeVenta unidad : traerUnidades(idFestival)) {
            sinPosicion.add(new ReporteMayoresCanonDTO(
                    0,
                    unidad.getCodigo(),
                    unidad.getNombreComercial(),
                    unidad.getTipo(),
                    unidad.calcularCanon(festival)));
        }
        sinPosicion.sort(Comparator.comparingDouble(ReporteMayoresCanonDTO::canon).reversed()
                .thenComparing(ReporteMayoresCanonDTO::nombreComercial));

        List<ReporteMayoresCanonDTO> mayores = new ArrayList<>();
        for (int i = 0; i < Math.min(x, sinPosicion.size()); i++) {
            ReporteMayoresCanonDTO r = sinPosicion.get(i);
            mayores.add(new ReporteMayoresCanonDTO(
                    i + 1, r.codigo(), r.nombreComercial(), r.tipo(), r.canon()));
        }
        return mayores;
    }

    // API REST (Hito 3)

    @Override
    @Transactional(readOnly = true)
    public List<FestivalResponseDTO> filtrar(String nombre, LocalDate desde, LocalDate hasta,
                                             EstadoFestival estado) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' (" + desde
                    + ") no puede ser posterior a la fecha 'hasta' (" + hasta + ").");
        }
        LocalDate hoy = LocalDate.now();
        String nombreBuscado = (nombre == null || nombre.isBlank())
                ? null
                : nombre.trim().toLowerCase(Locale.ROOT);

        return festivalRepository.findAllByOrderByFechaInicioAsc().stream()
                .filter(f -> nombreBuscado == null
                        || f.getNombre().toLowerCase(Locale.ROOT).contains(nombreBuscado))
                // se solapa con el rango [desde, hasta]
                .filter(f -> desde == null || !f.getFechaFin().isBefore(desde))
                .filter(f -> hasta == null || !f.getFechaInicio().isAfter(hasta))
                .filter(f -> estado == null || calcularEstado(f, hoy) == estado)
                .map(f -> aResponse(f, hoy))
                .toList();
    }

    @Override
    @Transactional
    public FestivalResponseDTO alta(FestivalRequestDTO request) {
        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del festival es obligatorio.");
        }
        if (request.fechaInicio() == null || request.fechaFin() == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias.");
        }
        if (request.fechaFin().isBefore(request.fechaInicio())) {
            throw new IllegalArgumentException(
                    "La fecha de fin no puede ser anterior a la fecha de inicio.");
        }
        validarMayorACero("costoSuperficie", request.costoSuperficie());
        validarNoNegativo("costoMontaje", request.costoMontaje());
        validarMayorACero("plusElectricidad", request.plusElectricidad());
        validarMayorACero("sueldoBase", request.sueldoBase());
        validarNoNegativo("plusAntiguedad", request.plusAntiguedad());
        validarMayorACero("plusCocinero", request.plusCocinero());
        validarMayorACero("plusAyudante", request.plusAyudante());
        validarMayorACero("plusLavaplatos", request.plusLavaplatos());

        String nombre = request.nombre().trim();
        if (festivalRepository.existsByNombreIgnorandoMayusculasYEspacios(nombre)) {
            throw new IllegalArgumentException("Ya existe un festival con el nombre '" + nombre + "'.");
        }

        Costo costo = new Costo();
        costo.setCostoSuperficie(request.costoSuperficie());
        costo.setCostoMontaje(request.costoMontaje());
        costo.setPlusElectricidad(request.plusElectricidad());
        costo.setSueldoBase(request.sueldoBase());
        costo.setPlusAntiguedad(request.plusAntiguedad());
        costo.setPlusCocinero(request.plusCocinero());
        costo.setPlusAyudante(request.plusAyudante());
        costo.setPlusLavaplatos(request.plusLavaplatos());

        Festival festival = new Festival();
        festival.setNombre(nombre);
        festival.setFechaInicio(request.fechaInicio());
        festival.setFechaFin(request.fechaFin());
        festival.setCosto(costo); // se guarda en cascada

        return aResponse(festivalRepository.save(festival), LocalDate.now());
    }

    private EstadoFestival calcularEstado(Festival festival, LocalDate hoy) {
        if (festival.estaActivo(hoy)) {
            return EstadoFestival.EN_CURSO;
        }
        return hoy.isBefore(festival.getFechaInicio())
                ? EstadoFestival.PROXIMO
                : EstadoFestival.FINALIZADO;
    }

    private FestivalResponseDTO aResponse(Festival festival, LocalDate hoy) {
        Costo costo = festival.getCosto();
        return new FestivalResponseDTO(
                festival.getId(),
                festival.getNombre(),
                festival.getFechaInicio(),
                festival.getFechaFin(),
                festival.calcularDuracionDias(),
                calcularEstado(festival, hoy),
                festival.getUnidades().size(),
                costo.getCostoSuperficie(),
                costo.getCostoMontaje(),
                costo.getPlusElectricidad(),
                costo.getSueldoBase(),
                costo.getPlusAntiguedad(),
                costo.getPlusCocinero(),
                costo.getPlusAyudante(),
                costo.getPlusLavaplatos());
    }

    private void validarMayorACero(String campo, Float valor) {
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("El campo " + campo + " debe ser mayor a 0.");
        }
    }

    private void validarNoNegativo(String campo, Float valor) {
        if (valor == null || valor < 0) {
            throw new IllegalArgumentException("El campo " + campo + " debe ser mayor o igual a 0.");
        }
    }
}
