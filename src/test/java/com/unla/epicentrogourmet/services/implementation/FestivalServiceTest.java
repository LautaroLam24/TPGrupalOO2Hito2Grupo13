package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.RankingUnidadDTO;
import com.unla.epicentrogourmet.dto.ReporteVentaDTO;
import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.entities.FoodTruck;
import com.unla.epicentrogourmet.entities.PuestoDesarmable;
import com.unla.epicentrogourmet.entities.UnidadDeVenta;
import com.unla.epicentrogourmet.exceptions.ReporteResponsableException;
import com.unla.epicentrogourmet.repositories.IFestivalRepository;
import com.unla.epicentrogourmet.repositories.IPedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FestivalServiceTest {

    private static final String USERNAME = "responsable@epicentro.com";
    private static final int FESTIVAL_ID = 20;

    @Mock
    private IFestivalRepository festivalRepository;

    @Mock
    private ReporteResponsableService reporteResponsableService;

    @Mock
    private IPedidoRepository pedidoRepository;

    private FestivalService service;
    private FoodTruck unidadResponsable;
    private Festival festival;

    @BeforeEach
    void setUp() {
        service = new FestivalService(
                festivalRepository,
                reporteResponsableService,
                pedidoRepository);

        unidadResponsable = new FoodTruck();
        unidadResponsable.setId(10);
        festival = new Festival();
        festival.setId(FESTIVAL_ID);
        festival.setNombre("Festival Sur");
        festival.setFechaInicio(LocalDate.of(2026, 10, 1));
        festival.setFechaFin(LocalDate.of(2026, 10, 5));
    }

    @Test
    void autorizaFestivalYConsultaIdYFechasExactasSinFiltrarPorUnidadResponsable() {
        autorizarFestival();
        when(pedidoRepository.sumTotalByFestivalAndFechas(
                FESTIVAL_ID, festival.getFechaInicio(), festival.getFechaFin()))
                .thenReturn(450000.5d);

        ReporteVentaDTO reporte = service.reporteRecaudacion(USERNAME, FESTIVAL_ID);

        assertEquals("Festival Sur", reporte.festival());
        assertEquals(450000.5f, reporte.recaudacionTotal(), 0.001f);
        verify(reporteResponsableService).autorizarFestival(USERNAME, FESTIVAL_ID);
        verify(pedidoRepository).sumTotalByFestivalAndFechas(
                FESTIVAL_ID, festival.getFechaInicio(), festival.getFechaFin());
        verifyNoInteractions(festivalRepository);
        verifyNoMoreInteractions(reporteResponsableService, pedidoRepository);
    }

    @Test
    void agregadoNuloProduceDtoConTotalCero() {
        autorizarFestival();
        when(pedidoRepository.sumTotalByFestivalAndFechas(
                FESTIVAL_ID, festival.getFechaInicio(), festival.getFechaFin()))
                .thenReturn(null);

        ReporteVentaDTO reporte = service.reporteRecaudacion(USERNAME, FESTIVAL_ID);

        assertEquals(new ReporteVentaDTO("Festival Sur", 0.0f), reporte);
    }

    @Test
    void agregadoCeroPermaneceCero() {
        autorizarFestival();
        when(pedidoRepository.sumTotalByFestivalAndFechas(
                FESTIVAL_ID, festival.getFechaInicio(), festival.getFechaFin()))
                .thenReturn(0.0d);

        ReporteVentaDTO reporte = service.reporteRecaudacion(USERNAME, FESTIVAL_ID);

        assertEquals(0.0f, reporte.recaudacionTotal(), 0.0f);
    }

    @Test
    void importeNegativoHistoricoSeRepresentaSinCorregirloACero() {
        autorizarFestival();
        when(pedidoRepository.sumTotalByFestivalAndFechas(
                FESTIVAL_ID, festival.getFechaInicio(), festival.getFechaFin()))
                .thenReturn(-1250.75d);

        ReporteVentaDTO reporte = service.reporteRecaudacion(USERNAME, FESTIVAL_ID);

        assertEquals(-1250.75f, reporte.recaudacionTotal(), 0.001f);
    }

    @Test
    void rechazaResultadosNoFinitos() {
        autorizarFestival();
        when(pedidoRepository.sumTotalByFestivalAndFechas(
                FESTIVAL_ID, festival.getFechaInicio(), festival.getFechaFin()))
                .thenReturn(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);

        assertAll(
                () -> assertThrows(ReporteResponsableException.class,
                        () -> service.reporteRecaudacion(USERNAME, FESTIVAL_ID)),
                () -> assertThrows(ReporteResponsableException.class,
                        () -> service.reporteRecaudacion(USERNAME, FESTIVAL_ID)),
                () -> assertThrows(ReporteResponsableException.class,
                        () -> service.reporteRecaudacion(USERNAME, FESTIVAL_ID)));
    }

    @Test
    void rechazaResultadosFueraDelRangoDeFloat() {
        autorizarFestival();
        when(pedidoRepository.sumTotalByFestivalAndFechas(
                FESTIVAL_ID, festival.getFechaInicio(), festival.getFechaFin()))
                .thenReturn((double) Float.MAX_VALUE * 2, (double) -Float.MAX_VALUE * 2);

        assertAll(
                () -> assertThrows(ReporteResponsableException.class,
                        () -> service.reporteRecaudacion(USERNAME, FESTIVAL_ID)),
                () -> assertThrows(ReporteResponsableException.class,
                        () -> service.reporteRecaudacion(USERNAME, FESTIVAL_ID)));
    }

    @Test
    void errorDeAutorizacionImpideConsultarPedidos() {
        ReporteResponsableException esperado = new ReporteResponsableException("Festival no autorizado.");
        when(reporteResponsableService.autorizarFestival(USERNAME, FESTIVAL_ID))
                .thenThrow(esperado);

        ReporteResponsableException resultado = assertThrows(
                ReporteResponsableException.class,
                () -> service.reporteRecaudacion(USERNAME, FESTIVAL_ID));

        assertSame(esperado, resultado);
        verifyNoInteractions(pedidoRepository, festivalRepository);
    }

    @Test
    void operacionesExistentesConservanSusConsultas() {
        Festival otro = new Festival();
        when(festivalRepository.findAll()).thenReturn(List.of(festival));
        when(festivalRepository.findByFechaFinGreaterThanEqualOrderByFechaInicioAsc(LocalDate.now()))
                .thenReturn(List.of(otro));

        assertEquals(List.of(festival), service.traerTodos());
        assertEquals(List.of(otro), service.traerDisponibles());

        verifyNoInteractions(reporteResponsableService, pedidoRepository);
    }

    @Test
    void jpqlSumaPedidoTotalGlobalSinItemsNiDistinctSobreImportes()
            throws NoSuchMethodException {
        Method metodo = IPedidoRepository.class.getMethod(
                "sumTotalByFestivalAndFechas", int.class, LocalDate.class, LocalDate.class);
        Query query = metodo.getAnnotation(Query.class);
        String jpql = query.value().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);

        assertAll(
                () -> assertTrue(jpql.contains("select sum(p.total)")),
                () -> assertTrue(jpql.contains("from festival f")),
                () -> assertTrue(jpql.contains("join f.unidades u")),
                () -> assertTrue(jpql.contains("join u.pedidos p")),
                () -> assertTrue(jpql.contains("where f.id = :festivalid")),
                () -> assertTrue(jpql.contains("p.fecha between :fechainicio and :fechafin")),
                () -> assertFalse(jpql.contains("itempedido")),
                () -> assertFalse(jpql.contains("p.items")),
                () -> assertFalse(jpql.contains("sum(distinct")),
                () -> assertFalse(jpql.contains(":unidadid")));
    }

    @Test
    void traerUnidadesDevuelveLasUnidadesDelFestivalSinConsultarOtrasDependencias() {
        PuestoDesarmable puesto = new PuestoDesarmable();
        puesto.setId(11);
        List<UnidadDeVenta> unidades = List.of(unidadResponsable, puesto);
        when(festivalRepository.findUnidadesByFestivalId(FESTIVAL_ID)).thenReturn(unidades);

        List<UnidadDeVenta> resultado = service.traerUnidades(FESTIVAL_ID);

        assertEquals(unidades, resultado);
        verify(festivalRepository).findUnidadesByFestivalId(FESTIVAL_ID);
        verifyNoMoreInteractions(festivalRepository);
        verifyNoInteractions(reporteResponsableService, pedidoRepository);
    }

    @Test
    void traerUnidadesDeFestivalSinUnidadesDevuelveListaVacia() {
        when(festivalRepository.findUnidadesByFestivalId(FESTIVAL_ID)).thenReturn(List.of());

        assertTrue(service.traerUnidades(FESTIVAL_ID).isEmpty());
    }

    @Test
    void traerPorIdDevuelveElFestivalOVacioSiNoExiste() {
        when(festivalRepository.findById(FESTIVAL_ID)).thenReturn(Optional.of(festival));
        when(festivalRepository.findById(99)).thenReturn(Optional.empty());

        assertEquals(Optional.of(festival), service.traerPorId(FESTIVAL_ID));
        assertTrue(service.traerPorId(99).isEmpty());
        verifyNoInteractions(reporteResponsableService, pedidoRepository);
    }

    @Test
    void jpqlTraeUnidadesDelFestivalConResponsableYOrdenadasPorCodigo()
            throws NoSuchMethodException {
        Method metodo = IFestivalRepository.class.getMethod("findUnidadesByFestivalId", int.class);
        Query query = metodo.getAnnotation(Query.class);
        String jpql = query.value().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);

        assertAll(
                () -> assertTrue(jpql.contains("select u")),
                () -> assertTrue(jpql.contains("join f.unidades u")),
                () -> assertTrue(jpql.contains("join fetch u.responsable")),
                () -> assertTrue(jpql.contains("where f.id = :festivalid")),
                () -> assertTrue(jpql.contains("order by u.codigo")));
    }

    @Test
    void rankingUnidadesOrdenaPorRecaudacionDescendenteYAsignaPosiciones() {
        unidadResponsable.setCodigo("FTK0000001");
        unidadResponsable.setNombreComercial("Ruedas de Fuego");
        PuestoDesarmable puesto = puesto(11, "PDS0000001", "La Carpa Criolla");
        when(pedidoRepository.findRecaudacionPorUnidadDeFestival(FESTIVAL_ID))
                .thenReturn(List.of(
                        new Object[]{11, 18000.0d},
                        new Object[]{10, 35000.0d}));
        when(festivalRepository.findUnidadesByFestivalId(FESTIVAL_ID))
                .thenReturn(List.of(puesto, unidadResponsable));

        List<RankingUnidadDTO> ranking = service.rankingUnidades(FESTIVAL_ID);

        assertEquals(List.of(
                new RankingUnidadDTO(1, "FTK0000001", "Ruedas de Fuego", "Food truck", 35000.0d),
                new RankingUnidadDTO(2, "PDS0000001", "La Carpa Criolla", "Puesto desarmable", 18000.0d)),
                ranking);
        verifyNoInteractions(reporteResponsableService);
    }

    @Test
    void rankingUnidadesIncluyeUnidadesConRecaudacionCeroAlFinal() {
        unidadResponsable.setNombreComercial("Ruedas de Fuego");
        PuestoDesarmable sinPedidos = puesto(11, "PDS0000100", "El Rincón Dulce");
        when(pedidoRepository.findRecaudacionPorUnidadDeFestival(FESTIVAL_ID))
                .thenReturn(List.of(
                        new Object[]{11, 0},
                        new Object[]{10, 18000.0d}));
        when(festivalRepository.findUnidadesByFestivalId(FESTIVAL_ID))
                .thenReturn(List.of(sinPedidos, unidadResponsable));

        List<RankingUnidadDTO> ranking = service.rankingUnidades(FESTIVAL_ID);

        assertEquals(2, ranking.size());
        assertEquals(new RankingUnidadDTO(2, "PDS0000100", "El Rincón Dulce", "Puesto desarmable", 0.0d),
                ranking.get(1));
    }

    @Test
    void rankingUnidadesDesempataPorNombreComercial() {
        unidadResponsable.setNombreComercial("Zeta");
        PuestoDesarmable puesto = puesto(11, "PDS0000001", "Alfa");
        when(pedidoRepository.findRecaudacionPorUnidadDeFestival(FESTIVAL_ID))
                .thenReturn(List.of(
                        new Object[]{10, 5000.0d},
                        new Object[]{11, 5000.0d}));
        when(festivalRepository.findUnidadesByFestivalId(FESTIVAL_ID))
                .thenReturn(List.of(unidadResponsable, puesto));

        List<RankingUnidadDTO> ranking = service.rankingUnidades(FESTIVAL_ID);

        assertEquals("Alfa", ranking.get(0).nombreComercial());
        assertEquals(1, ranking.get(0).posicion());
        assertEquals("Zeta", ranking.get(1).nombreComercial());
        assertEquals(2, ranking.get(1).posicion());
    }

    @Test
    void rankingUnidadesDeFestivalInexistenteDevuelveListaVacia() {
        when(pedidoRepository.findRecaudacionPorUnidadDeFestival(99)).thenReturn(List.of());
        when(festivalRepository.findUnidadesByFestivalId(99)).thenReturn(List.of());

        assertTrue(service.rankingUnidades(99).isEmpty());
        verifyNoInteractions(reporteResponsableService);
    }

    @Test
    void jpqlRankingUsaLeftJoinConFiltroDeFechasEnElOnYAgrupaPorIdDeUnidad()
            throws NoSuchMethodException {
        Method metodo = IPedidoRepository.class.getMethod(
                "findRecaudacionPorUnidadDeFestival", int.class);
        Query query = metodo.getAnnotation(Query.class);
        String jpql = query.value().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        String where = jpql.substring(jpql.indexOf(" where "));

        assertAll(
                () -> assertTrue(jpql.contains("select u.id, coalesce(sum(p.total), 0)")),
                () -> assertTrue(jpql.contains("join f.unidades u")),
                () -> assertTrue(jpql.contains(
                        "left join u.pedidos p on p.fecha between f.fechainicio and f.fechafin")),
                () -> assertTrue(where.contains("where f.id = :festivalid")),
                () -> assertFalse(where.contains("p.fecha")),
                () -> assertTrue(jpql.contains("group by u.id")),
                () -> assertFalse(jpql.contains("group by u ")));
    }

    private PuestoDesarmable puesto(int id, String codigo, String nombreComercial) {
        PuestoDesarmable puesto = new PuestoDesarmable();
        puesto.setId(id);
        puesto.setCodigo(codigo);
        puesto.setNombreComercial(nombreComercial);
        return puesto;
    }

    private void autorizarFestival() {
        when(reporteResponsableService.autorizarFestival(USERNAME, FESTIVAL_ID))
                .thenReturn(new ReporteResponsableService.ContextoFestivalAutorizado(
                        unidadResponsable, festival));
    }
}
