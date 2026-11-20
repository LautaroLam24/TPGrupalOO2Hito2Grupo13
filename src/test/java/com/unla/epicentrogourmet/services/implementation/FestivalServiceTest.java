package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.EstadoFestival;
import com.unla.epicentrogourmet.dto.FestivalRequestDTO;
import com.unla.epicentrogourmet.dto.FestivalResponseDTO;
import com.unla.epicentrogourmet.dto.RankingUnidadDTO;
import com.unla.epicentrogourmet.dto.ReporteMayoresCanonDTO;
import com.unla.epicentrogourmet.dto.ReporteVentaDTO;
import com.unla.epicentrogourmet.entities.Costo;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    @Test
    void unidadesMayorCanonOrdenaPorCanonDescendenteYAsignaPosiciones() {
        prepararUnidadesConCanon();

        List<ReporteMayoresCanonDTO> mayores = service.unidadesMayorCanon(FESTIVAL_ID, 3);

        assertEquals(List.of(
                new ReporteMayoresCanonDTO(1, "PDS0000001", "La Carpa Criolla", "Puesto desarmable", 9100.0d),
                new ReporteMayoresCanonDTO(2, "FTK0000001", "Ruedas de Fuego", "Food truck", 8000.0d),
                new ReporteMayoresCanonDTO(3, "PDS0000100", "El Rincón Dulce", "Puesto desarmable", 6900.0d)),
                mayores);
        verifyNoInteractions(reporteResponsableService, pedidoRepository);
    }

    @Test
    void unidadesMayorCanonDevuelveSoloLasPrimerasX() {
        prepararUnidadesConCanon();

        List<ReporteMayoresCanonDTO> mayores = service.unidadesMayorCanon(FESTIVAL_ID, 2);

        assertEquals(2, mayores.size());
        assertEquals("La Carpa Criolla", mayores.get(0).nombreComercial());
        assertEquals("Ruedas de Fuego", mayores.get(1).nombreComercial());
    }

    @Test
    void unidadesMayorCanonConXMayorQueLaCantidadDevuelveTodas() {
        prepararUnidadesConCanon();

        List<ReporteMayoresCanonDTO> mayores = service.unidadesMayorCanon(FESTIVAL_ID, 10);

        assertEquals(3, mayores.size());
        assertEquals(3, mayores.get(2).posicion());
    }

    @Test
    void unidadesMayorCanonDesempataPorNombreComercial() {
        festival.setCosto(costo());
        PuestoDesarmable zeta = puesto(11, "PDS0000011", "Zeta");
        zeta.setSuperficieM2(10);
        PuestoDesarmable alfa = puesto(12, "PDS0000012", "Alfa");
        alfa.setSuperficieM2(10);
        when(festivalRepository.findById(FESTIVAL_ID)).thenReturn(Optional.of(festival));
        when(festivalRepository.findUnidadesByFestivalId(FESTIVAL_ID)).thenReturn(List.of(zeta, alfa));

        List<ReporteMayoresCanonDTO> mayores = service.unidadesMayorCanon(FESTIVAL_ID, 2);

        assertEquals("Alfa", mayores.get(0).nombreComercial());
        assertEquals("Zeta", mayores.get(1).nombreComercial());
    }

    @Test
    void unidadesMayorCanonRechazaXInvalidoSinConsultarRepositorios() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.unidadesMayorCanon(FESTIVAL_ID, 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.unidadesMayorCanon(FESTIVAL_ID, -3)));
        verifyNoInteractions(festivalRepository, reporteResponsableService, pedidoRepository);
    }

    @Test
    void unidadesMayorCanonDeFestivalSinUnidadesDevuelveListaVacia() {
        festival.setCosto(costo());
        when(festivalRepository.findById(FESTIVAL_ID)).thenReturn(Optional.of(festival));
        when(festivalRepository.findUnidadesByFestivalId(FESTIVAL_ID)).thenReturn(List.of());

        assertTrue(service.unidadesMayorCanon(FESTIVAL_ID, 3).isEmpty());
    }

    @Test
    void unidadesMayorCanonDeFestivalInexistenteLanzaExcepcion() {
        when(festivalRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.unidadesMayorCanon(99, 3));
        verify(festivalRepository).findById(99);
        verifyNoMoreInteractions(festivalRepository);
    }

    private void prepararUnidadesConCanon() {
        festival.setCosto(costo());
        unidadResponsable.setCodigo("FTK0000001");
        unidadResponsable.setNombreComercial("Ruedas de Fuego");
        unidadResponsable.setSuperficieM2(12);
        unidadResponsable.setRequiereElectricidad(true);
        PuestoDesarmable carpa = puesto(11, "PDS0000001", "La Carpa Criolla");
        carpa.setSuperficieM2(20);
        carpa.setTiempoMontajeMin(90);
        PuestoDesarmable rincon = puesto(100, "PDS0000100", "El Rincón Dulce");
        rincon.setSuperficieM2(15);
        rincon.setTiempoMontajeMin(60);
        when(festivalRepository.findById(FESTIVAL_ID)).thenReturn(Optional.of(festival));
        when(festivalRepository.findUnidadesByFestivalId(FESTIVAL_ID))
                .thenReturn(List.of(unidadResponsable, carpa, rincon));
    }

    private Costo costo() {
        Costo costo = new Costo();
        costo.setCostoSuperficie(500);
        costo.setCostoMontaje(10);
        costo.setPlusElectricidad(2000);
        return costo;
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

    // ---------------------------------------------------------------
    // API REST (Hito 3): alta de festival
    // ---------------------------------------------------------------

    @Test
    void altaCorrectaGuardaFestivalConCostosYDevuelveDto() {
        LocalDate inicio = LocalDate.now().plusDays(30);
        when(festivalRepository.existsByNombreIgnorandoMayusculasYEspacios("Festival de Otoño"))
                .thenReturn(false);
        when(festivalRepository.save(any(Festival.class))).thenAnswer(invocacion -> {
            Festival guardado = invocacion.getArgument(0);
            guardado.setId(50);
            return guardado;
        });

        FestivalResponseDTO respuesta = service.alta(
                request("  Festival de Otoño  ", inicio, inicio.plusDays(4)));

        ArgumentCaptor<Festival> captor = ArgumentCaptor.forClass(Festival.class);
        verify(festivalRepository).save(captor.capture());
        Festival guardado = captor.getValue();
        assertAll(
                () -> assertEquals("Festival de Otoño", guardado.getNombre()),
                () -> assertEquals(inicio, guardado.getFechaInicio()),
                () -> assertEquals(500f, guardado.getCosto().getCostoSuperficie()),
                () -> assertEquals(50000f, guardado.getCosto().getPlusLavaplatos()),
                () -> assertEquals(50, respuesta.id()),
                () -> assertEquals("Festival de Otoño", respuesta.nombre()),
                () -> assertEquals(5, respuesta.duracionDias()),
                () -> assertEquals(EstadoFestival.PROXIMO, respuesta.estado()),
                () -> assertEquals(0, respuesta.cantidadUnidades()),
                () -> assertEquals(10f, respuesta.costoMontaje()),
                () -> assertEquals(150000f, respuesta.plusCocinero()));
        verifyNoInteractions(reporteResponsableService, pedidoRepository);
    }

    @Test
    void altaConFechaFinAnteriorAFechaInicioLanzaExcepcionYNoGuarda() {
        LocalDate inicio = LocalDate.now().plusDays(30);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.alta(request("Festival de Otoño", inicio, inicio.minusDays(1))));

        assertEquals("La fecha de fin no puede ser anterior a la fecha de inicio.", ex.getMessage());
        verifyNoInteractions(festivalRepository);
    }

    @Test
    void altaDeUnSoloDiaEsValida() {
        LocalDate dia = LocalDate.now().plusDays(10);
        when(festivalRepository.save(any(Festival.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals(1, service.alta(request("Festival Express", dia, dia)).duracionDias());
    }

    @Test
    void altaConNombreRepetidoConOtrasMayusculasYEspaciosLanzaExcepcion() {
        LocalDate inicio = LocalDate.now().plusDays(30);
        when(festivalRepository.existsByNombreIgnorandoMayusculasYEspacios("festival SUR"))
                .thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.alta(request("   festival SUR ", inicio, inicio.plusDays(2))));

        assertEquals("Ya existe un festival con el nombre 'festival SUR'.", ex.getMessage());
        verify(festivalRepository, never()).save(any());
    }

    @Test
    void jpqlDeNombreRepetidoIgnoraMayusculasYEspaciosDeLosCostados() throws NoSuchMethodException {
        Method metodo = IFestivalRepository.class.getMethod(
                "existsByNombreIgnorandoMayusculasYEspacios", String.class);
        String jpql = metodo.getAnnotation(Query.class).value()
                .replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);

        assertTrue(jpql.contains("where lower(trim(f.nombre)) = lower(trim(:nombre))"));
    }

    @ParameterizedTest(name = "{0} = {1} se rechaza")
    @CsvSource({
            "costoSuperficie, 0", "costoSuperficie, -1",
            "plusElectricidad, 0", "plusElectricidad, -100",
            "sueldoBase, 0", "sueldoBase, -1",
            "plusCocinero, 0", "plusCocinero, -5",
            "plusAyudante, 0", "plusAyudante, -5",
            "plusLavaplatos, 0", "plusLavaplatos, -5"
    })
    void altaConCostoObligatorioEnCeroONegativoLanzaExcepcion(String campo, float valor) {
        LocalDate inicio = LocalDate.now().plusDays(30);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.alta(requestConCosto(campo, valor, inicio)));

        assertEquals("El campo " + campo + " debe ser mayor a 0.", ex.getMessage());
        verifyNoInteractions(festivalRepository);
    }

    @ParameterizedTest(name = "{0} negativo se rechaza")
    @CsvSource({"costoMontaje", "plusAntiguedad"})
    void altaConCostoMontajeOPlusAntiguedadNegativoLanzaExcepcion(String campo) {
        LocalDate inicio = LocalDate.now().plusDays(30);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.alta(requestConCosto(campo, -1f, inicio)));

        assertEquals("El campo " + campo + " debe ser mayor o igual a 0.", ex.getMessage());
        verifyNoInteractions(festivalRepository);
    }

    @Test
    void altaAceptaCostoMontajeYPlusAntiguedadEnCero() {
        LocalDate inicio = LocalDate.now().plusDays(30);
        FestivalRequestDTO base = request("Festival Sin Montaje", inicio, inicio.plusDays(1));
        FestivalRequestDTO conCeros = new FestivalRequestDTO(base.nombre(), base.fechaInicio(),
                base.fechaFin(), 500f, 0f, 2000f, 100000f, 0f, 150000f, 100000f, 50000f);
        when(festivalRepository.save(any(Festival.class))).thenAnswer(i -> i.getArgument(0));

        FestivalResponseDTO respuesta = service.alta(conCeros);

        assertEquals(0f, respuesta.costoMontaje());
        assertEquals(0f, respuesta.plusAntiguedad());
    }

    // ---------------------------------------------------------------
    // API REST (Hito 3): filtros del listado
    // ---------------------------------------------------------------

    @Test
    void filtrarSinFiltrosDevuelveTodosEnElOrdenDelRepositorio() {
        prepararFestivalesParaFiltrar();

        List<FestivalResponseDTO> resultado = service.filtrar(null, null, null, null);

        assertEquals(List.of("Festival de Verano", "Festival Primavera Gourmet",
                "Primavera Criolla", "Festival del Invierno"), nombres(resultado));
    }

    @Test
    void filtrarCalculaEstadoDuracionYCantidadDeUnidades() {
        prepararFestivalesParaFiltrar();

        List<FestivalResponseDTO> resultado = service.filtrar(null, null, null, null);

        assertAll(
                () -> assertEquals(EstadoFestival.FINALIZADO, resultado.get(0).estado()),
                () -> assertEquals(EstadoFestival.EN_CURSO, resultado.get(1).estado()),
                () -> assertEquals(EstadoFestival.PROXIMO, resultado.get(2).estado()),
                () -> assertEquals(5, resultado.get(1).duracionDias()),
                () -> assertEquals(2, resultado.get(1).cantidadUnidades()),
                () -> assertEquals(500f, resultado.get(1).costoSuperficie()));
    }

    @Test
    void filtrarPorNombreEsContieneYNoDistingueMayusculas() {
        prepararFestivalesParaFiltrar();

        List<FestivalResponseDTO> resultado = service.filtrar("  PRIMAVERA ", null, null, null);

        assertEquals(List.of("Festival Primavera Gourmet", "Primavera Criolla"), nombres(resultado));
    }

    @Test
    void filtrarPorRangoDevuelveLosFestivalesQueSeSolapanIncluyendoBordes() {
        prepararFestivalesParaFiltrar();
        LocalDate hoy = LocalDate.now();

        // [hoy+2, hoy+30]: toca el último día del que está en curso y el primero del próximo.
        List<FestivalResponseDTO> resultado = service.filtrar(null, hoy.plusDays(2), hoy.plusDays(30), null);

        assertEquals(List.of("Festival Primavera Gourmet", "Primavera Criolla"), nombres(resultado));
    }

    @Test
    void filtrarSoloConDesdeOSoloConHastaUsaRangoAbierto() {
        prepararFestivalesParaFiltrar();
        LocalDate hoy = LocalDate.now();

        assertEquals(List.of("Primavera Criolla", "Festival del Invierno"),
                nombres(service.filtrar(null, hoy.plusDays(3), null, null)));
        assertEquals(List.of("Festival de Verano"),
                nombres(service.filtrar(null, null, hoy.minusDays(3), null)));
    }

    @Test
    void filtrarPorEstadoUsaLaFechaDeHoy() {
        prepararFestivalesParaFiltrar();

        assertEquals(List.of("Festival Primavera Gourmet"),
                nombres(service.filtrar(null, null, null, EstadoFestival.EN_CURSO)));
        assertEquals(List.of("Primavera Criolla", "Festival del Invierno"),
                nombres(service.filtrar(null, null, null, EstadoFestival.PROXIMO)));
        assertEquals(List.of("Festival de Verano"),
                nombres(service.filtrar(null, null, null, EstadoFestival.FINALIZADO)));
    }

    @Test
    void filtrarCombinandoNombreRangoYEstado() {
        prepararFestivalesParaFiltrar();
        LocalDate hoy = LocalDate.now();

        List<FestivalResponseDTO> resultado = service.filtrar(
                "primavera", hoy.minusDays(10), hoy.plusDays(100), EstadoFestival.PROXIMO);

        assertEquals(List.of("Primavera Criolla"), nombres(resultado));
    }

    @Test
    void filtrarSinCoincidenciasDevuelveListaVacia() {
        prepararFestivalesParaFiltrar();

        assertTrue(service.filtrar("otoño", null, null, null).isEmpty());
    }

    @Test
    void filtrarConDesdePosteriorAHastaLanzaExcepcionSinConsultarElRepositorio() {
        LocalDate desde = LocalDate.of(2026, 12, 31);
        LocalDate hasta = LocalDate.of(2026, 1, 1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.filtrar(null, desde, hasta, null));

        assertEquals("La fecha 'desde' (2026-12-31) no puede ser posterior a la fecha 'hasta' (2026-01-01).",
                ex.getMessage());
        verifyNoInteractions(festivalRepository);
    }

    private void prepararFestivalesParaFiltrar() {
        LocalDate hoy = LocalDate.now();
        Festival enCurso = festivalApi(2, "Festival Primavera Gourmet", hoy.minusDays(2), hoy.plusDays(2));
        enCurso.getUnidades().add(unidadResponsable);
        enCurso.getUnidades().add(puesto(11, "PDS0000001", "La Carpa Criolla"));
        when(festivalRepository.findAllByOrderByFechaInicioAsc()).thenReturn(List.of(
                festivalApi(1, "Festival de Verano", hoy.minusDays(60), hoy.minusDays(55)),
                enCurso,
                festivalApi(3, "Primavera Criolla", hoy.plusDays(30), hoy.plusDays(33)),
                festivalApi(4, "Festival del Invierno", hoy.plusDays(90), hoy.plusDays(95))));
    }

    private Festival festivalApi(int id, String nombre, LocalDate inicio, LocalDate fin) {
        Festival f = new Festival();
        f.setId(id);
        f.setNombre(nombre);
        f.setFechaInicio(inicio);
        f.setFechaFin(fin);
        f.setCosto(costo());
        return f;
    }

    private List<String> nombres(List<FestivalResponseDTO> festivales) {
        return festivales.stream().map(FestivalResponseDTO::nombre).toList();
    }

    private FestivalRequestDTO request(String nombre, LocalDate inicio, LocalDate fin) {
        return new FestivalRequestDTO(nombre, inicio, fin,
                500f, 10f, 2000f, 100000f, 5000f, 150000f, 100000f, 50000f);
    }

    private FestivalRequestDTO requestConCosto(String campo, float valor, LocalDate inicio) {
        float[] c = {500f, 10f, 2000f, 100000f, 5000f, 150000f, 100000f, 50000f};
        List<String> campos = List.of("costoSuperficie", "costoMontaje", "plusElectricidad", "sueldoBase",
                "plusAntiguedad", "plusCocinero", "plusAyudante", "plusLavaplatos");
        c[campos.indexOf(campo)] = valor;
        return new FestivalRequestDTO("Festival de Otoño", inicio, inicio.plusDays(2),
                c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7]);
    }
}
