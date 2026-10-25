package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.PlatoDTO;
import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.entities.FoodTruck;
import com.unla.epicentrogourmet.exceptions.PlatoEstrellaException;
import com.unla.epicentrogourmet.exceptions.ReporteResponsableException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatoServiceTest {

    private static final String USERNAME = "responsable@epicentro.com";

    @Mock
    private ReporteResponsableService reporteResponsableService;

    @Mock
    private IPedidoRepository pedidoRepository;

    private PlatoService service;
    private FoodTruck unidad;

    @BeforeEach
    void setUp() {
        service = new PlatoService(
                reporteResponsableService,
                pedidoRepository);

        unidad = new FoodTruck();
        unidad.setId(10);
        unidad.setNombreComercial("Ruedas de Fuego");
    }

    @Test
    void prepararExponeSoloUnidadPropiaYTodosSusFestivalesEnElOrdenDelRepository() {
        Festival futuro = festival(3, "Festival futuro", LocalDate.now().plusDays(10), LocalDate.now().plusDays(12));
        Festival vigente = festival(2, "Festival vigente", LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        Festival finalizado = festival(1, "Festival finalizado", LocalDate.now().minusDays(10), LocalDate.now().minusDays(8));
        when(reporteResponsableService.preparar(USERNAME))
                .thenReturn(new ReporteResponsableService.ContextoReportes(
                        unidad.getNombreComercial(),
                        List.of(opcion(futuro), opcion(vigente), opcion(finalizado))));

        PlatoService.ContextoPlatoEstrella contexto = service.preparar(USERNAME);

        assertEquals("Ruedas de Fuego", contexto.unidad());
        assertEquals(List.of(3, 2, 1),
                contexto.festivales().stream().map(PlatoService.FestivalOpcion::id).toList());
        assertThrows(UnsupportedOperationException.class,
                () -> contexto.festivales().add(contexto.festivales().get(0)));
        verify(reporteResponsableService).preparar(USERNAME);
        verifyNoMoreInteractions(reporteResponsableService, pedidoRepository);
    }

    @Test
    void rechazaUsernameNuloVacioOEnBlancoAntesDeConsultar() {
        when(reporteResponsableService.preparar(null))
                .thenThrow(new ReporteResponsableException("Se requiere un responsable autenticado."));
        when(reporteResponsableService.preparar(""))
                .thenThrow(new ReporteResponsableException("Se requiere un responsable autenticado."));
        when(reporteResponsableService.preparar("   "))
                .thenThrow(new ReporteResponsableException("Se requiere un responsable autenticado."));

        assertAll(
                () -> assertThrows(PlatoEstrellaException.class, () -> service.preparar(null)),
                () -> assertThrows(PlatoEstrellaException.class, () -> service.preparar("")),
                () -> assertThrows(PlatoEstrellaException.class, () -> service.preparar("   ")));

        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void rechazaResponsableSinUnidad() {
        when(reporteResponsableService.preparar(USERNAME))
                .thenThrow(new ReporteResponsableException(
                        "No se encontró una unidad de venta para el responsable autenticado."));

        PlatoEstrellaException error = assertThrows(
                PlatoEstrellaException.class,
                () -> service.preparar(USERNAME));

        assertTrue(error.getMessage().contains("No se encontró una unidad"));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void rechazaCardinalidadInesperadaDeUnidades() {
        when(reporteResponsableService.preparar(USERNAME))
                .thenThrow(new ReporteResponsableException(
                        "La configuración de unidades del responsable autenticado es inconsistente."));

        PlatoEstrellaException error = assertThrows(
                PlatoEstrellaException.class,
                () -> service.preparar(USERNAME));

        assertTrue(error.getMessage().contains("inconsistente"));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void rechazaUsuarioDeshabilitado() {
        when(reporteResponsableService.preparar(USERNAME))
                .thenThrow(new ReporteResponsableException(
                        "El responsable autenticado no se encuentra habilitado o autorizado."));

        PlatoEstrellaException error = assertThrows(
                PlatoEstrellaException.class,
                () -> service.preparar(USERNAME));

        assertTrue(error.getMessage().contains("habilitado o autorizado"));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void rechazaUsuarioConRolIncorrecto() {
        when(reporteResponsableService.preparar(USERNAME))
                .thenThrow(new ReporteResponsableException(
                        "El responsable autenticado no se encuentra habilitado o autorizado."));

        PlatoEstrellaException error = assertThrows(
                PlatoEstrellaException.class,
                () -> service.preparar(USERNAME));

        assertTrue(error.getMessage().contains("habilitado o autorizado"));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void festivalAsociadoUsaUnidadYFechasExactasYMapeaPedidosDistintos() {
        LocalDate inicio = LocalDate.of(2026, 10, 1);
        LocalDate fin = LocalDate.of(2026, 10, 5);
        Festival festival = festival(20, "Festival Sur", inicio, fin);
        autorizarConsulta(festival);
        when(pedidoRepository.findRankingPlatosPorPedidos(unidad.getId(), inicio, fin))
                .thenReturn(List.<Object[]>of(new Object[]{7, "Pizza", 3L}));

        Optional<PlatoDTO> resultado = service.platoEstrella(USERNAME, festival.getId());

        assertEquals(Optional.of(new PlatoDTO("Pizza", 3, "Ruedas de Fuego")), resultado);
        verify(reporteResponsableService).autorizarFestival(USERNAME, festival.getId());
        verify(pedidoRepository).findRankingPlatosPorPedidos(unidad.getId(), inicio, fin);
        verifyNoMoreInteractions(reporteResponsableService, pedidoRepository);
    }

    @Test
    void festivalFinalizadoSePuedeConsultar() {
        Festival festival = festival(
                20, "Festival pasado", LocalDate.now().minusDays(10), LocalDate.now().minusDays(5));
        autorizarConsulta(festival);
        when(pedidoRepository.findRankingPlatosPorPedidos(
                unidad.getId(), festival.getFechaInicio(), festival.getFechaFin()))
                .thenReturn(List.<Object[]>of(new Object[]{7, "Pizza", 2L}));

        Optional<PlatoDTO> resultado = service.platoEstrella(USERNAME, festival.getId());

        assertEquals(2, resultado.orElseThrow().cantidadPedidos());
    }

    @Test
    void festivalFuturoSinPedidosDevuelveAusenciaDeResultado() {
        Festival festival = festival(
                21, "Festival futuro", LocalDate.now().plusDays(5), LocalDate.now().plusDays(8));
        autorizarConsulta(festival);
        when(pedidoRepository.findRankingPlatosPorPedidos(
                unidad.getId(), festival.getFechaInicio(), festival.getFechaFin()))
                .thenReturn(List.of());

        Optional<PlatoDTO> resultado = service.platoEstrella(USERNAME, festival.getId());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void festivalInexistenteONoAsociadoSeRechazaSinConsultarPedidos() {
        when(reporteResponsableService.autorizarFestival(USERNAME, 999))
                .thenThrow(new ReporteResponsableException(
                        "El festival seleccionado no existe o no está asociado a la unidad administrada."));

        PlatoEstrellaException error = assertThrows(
                PlatoEstrellaException.class,
                () -> service.platoEstrella(USERNAME, 999));

        assertTrue(error.getMessage().contains("no existe o no está asociado"));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void rechazaFechaInicialNula() {
        Festival festival = festival(20, "Festival inválido", null, LocalDate.now());
        rechazarFechas(festival);

        assertThrows(PlatoEstrellaException.class,
                () -> service.platoEstrella(USERNAME, festival.getId()));

        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void rechazaFechaFinalNula() {
        Festival festival = festival(20, "Festival inválido", LocalDate.now(), null);
        rechazarFechas(festival);

        assertThrows(PlatoEstrellaException.class,
                () -> service.platoEstrella(USERNAME, festival.getId()));

        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void rechazaInicioPosteriorAlFin() {
        Festival festival = festival(
                20, "Festival inválido", LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1));
        rechazarFechas(festival);

        PlatoEstrellaException error = assertThrows(
                PlatoEstrellaException.class,
                () -> service.platoEstrella(USERNAME, festival.getId()));

        assertTrue(error.getMessage().contains("intervalo de fechas inválido"));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void rankingVacioNoCreaGanadorArtificial() {
        Festival festival = festivalVigente();
        autorizarConsulta(festival);
        when(pedidoRepository.findRankingPlatosPorPedidos(
                unidad.getId(), festival.getFechaInicio(), festival.getFechaFin()))
                .thenReturn(List.of());

        assertTrue(service.platoEstrella(USERNAME, festival.getId()).isEmpty());
    }

    @Test
    void anteEmpateTomaUnicamenteLaPrimeraFilaOrdenadaPorElRepository() {
        Festival festival = festivalVigente();
        autorizarConsulta(festival);
        when(pedidoRepository.findRankingPlatosPorPedidos(
                unidad.getId(), festival.getFechaInicio(), festival.getFechaFin()))
                .thenReturn(List.of(
                        new Object[]{1, "Empanada", 4L},
                        new Object[]{2, "Pizza", 4L}));

        PlatoDTO ganador = service.platoEstrella(USERNAME, festival.getId()).orElseThrow();

        assertEquals("Empanada", ganador.nombre());
        assertEquals(4, ganador.cantidadPedidos());
    }

    @Test
    void rechazaConteoFueraDelRangoDelDto() {
        Festival festival = festivalVigente();
        autorizarConsulta(festival);
        when(pedidoRepository.findRankingPlatosPorPedidos(
                unidad.getId(), festival.getFechaInicio(), festival.getFechaFin()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{1, "Pizza", (long) Integer.MAX_VALUE + 1}));

        assertThrows(PlatoEstrellaException.class,
                () -> service.platoEstrella(USERNAME, festival.getId()));
    }

    @Test
    void jpqlExpresaUnidadRangoInclusivoConteoDistintoDesempateYVentasHistoricas()
            throws NoSuchMethodException {
        Method metodo = IPedidoRepository.class.getMethod(
                "findRankingPlatosPorPedidos", int.class, LocalDate.class, LocalDate.class);
        Query query = metodo.getAnnotation(Query.class);
        String jpql = query.value().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);

        assertAll(
                () -> assertTrue(jpql.contains("where u.id = :unidadid")),
                () -> assertTrue(jpql.contains("item.plato = p")),
                () -> assertTrue(jpql.contains("pedido.fecha between :fechainicio and :fechafin")),
                () -> assertTrue(jpql.contains("count(distinct pedido.id)")),
                () -> assertTrue(jpql.contains("order by count(distinct pedido.id) desc")),
                () -> assertTrue(jpql.contains("lower(p.nombre) asc")),
                () -> assertTrue(jpql.contains("p.id asc")),
                () -> assertFalse(jpql.contains("item.cantidad")),
                () -> assertFalse(jpql.contains("baja")));
    }

    private void autorizarConsulta(Festival festival) {
        when(reporteResponsableService.autorizarFestival(USERNAME, festival.getId()))
                .thenReturn(new ReporteResponsableService.ContextoFestivalAutorizado(
                        unidad, festival));
    }

    private void rechazarFechas(Festival festival) {
        when(reporteResponsableService.autorizarFestival(USERNAME, festival.getId()))
                .thenThrow(new ReporteResponsableException(
                        "El festival seleccionado tiene un intervalo de fechas inválido."));
    }

    private ReporteResponsableService.FestivalOpcion opcion(Festival festival) {
        return new ReporteResponsableService.FestivalOpcion(
                festival.getId(),
                festival.getNombre(),
                festival.getFechaInicio(),
                festival.getFechaFin());
    }

    private Festival festivalVigente() {
        return festival(
                20, "Festival vigente", LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
    }

    private Festival festival(int id, String nombre, LocalDate inicio, LocalDate fin) {
        Festival festival = new Festival();
        festival.setId(id);
        festival.setNombre(nombre);
        festival.setFechaInicio(inicio);
        festival.setFechaFin(fin);
        return festival;
    }

}
