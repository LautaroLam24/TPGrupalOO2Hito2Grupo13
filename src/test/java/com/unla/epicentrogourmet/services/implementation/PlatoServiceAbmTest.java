package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.FiltroPlatoDTO;
import com.unla.epicentrogourmet.dto.FiltroPlatoDTO.EstadoPlato;
import com.unla.epicentrogourmet.dto.PlatoForm;
import com.unla.epicentrogourmet.entities.FoodTruck;
import com.unla.epicentrogourmet.entities.Plato;
import com.unla.epicentrogourmet.entities.Role;
import com.unla.epicentrogourmet.entities.RoleName;
import com.unla.epicentrogourmet.entities.UnidadDeVenta;
import com.unla.epicentrogourmet.entities.User;
import com.unla.epicentrogourmet.exceptions.GestionPlatoException;
import com.unla.epicentrogourmet.repositories.IPedidoRepository;
import com.unla.epicentrogourmet.repositories.IUnidadDeVentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatoServiceAbmTest {

    private static final String USERNAME = "responsable@epicentro.com";

    @Mock
    private ReporteResponsableService reporteResponsableService;

    @Mock
    private IPedidoRepository pedidoRepository;

    @Mock
    private IUnidadDeVentaRepository unidadDeVentaRepository;

    private PlatoService service;
    private FoodTruck unidad;

    @BeforeEach
    void setUp() {
        service = new PlatoService(
                reporteResponsableService,
                pedidoRepository,
                unidadDeVentaRepository);
        unidad = unidadAutorizada(USERNAME);
        lenient().when(unidadDeVentaRepository.findByResponsableUsername(USERNAME))
                .thenReturn(List.of(unidad));
    }

    @Test
    void altaValidaRecortaNombreAsociaUnidadEIniciaActiva() {
        PlatoService.PlatoResumen resultado = service.alta(
                USERNAME, form("  Pizza napolitana  ", 1500f, 900f));

        assertEquals(1, unidad.getPlatos().size());
        Plato creado = unidad.getPlatos().iterator().next();
        assertAll(
                () -> assertEquals("Pizza napolitana", creado.getNombre()),
                () -> assertEquals(1500f, creado.getPrecioVenta()),
                () -> assertEquals(900f, creado.getCostoProduccion()),
                () -> assertFalse(creado.isBaja()),
                () -> assertTrue(resultado.activo()),
                () -> assertEquals("Pizza napolitana", resultado.nombre()));
        verify(unidadDeVentaRepository).saveAndFlush(unidad);
        verificarSinEliminaciones();
    }

    @Test
    void costoCeroEsValidoCuandoElPrecioEsPositivo() {
        service.alta(USERNAME, form("Agua", 100f, 0f));

        Plato creado = unidad.getPlatos().iterator().next();
        assertEquals(0f, creado.getCostoProduccion());
        verify(unidadDeVentaRepository).saveAndFlush(unidad);
    }

    @Test
    void formularioNuloSeRechaza() {
        assertThrows(GestionPlatoException.class, () -> service.alta(USERNAME, null));

        verify(unidadDeVentaRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @MethodSource("nombresInvalidos")
    void nombreNuloVacioOEnBlancoSeRechaza(String nombre) {
        GestionPlatoException error = assertThrows(
                GestionPlatoException.class,
                () -> service.alta(USERNAME, form(nombre, 100f, 20f)));

        assertTrue(error.getMessage().contains("nombre"));
        verify(unidadDeVentaRepository, never()).saveAndFlush(any());
    }

    @Test
    void nombreMayorQueCienCaracteresSeRechaza() {
        GestionPlatoException error = assertThrows(
                GestionPlatoException.class,
                () -> service.alta(USERNAME, form("a".repeat(101), 100f, 20f)));

        assertTrue(error.getMessage().contains("100"));
        verify(unidadDeVentaRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @MethodSource("preciosInvalidos")
    void precioAusenteNoPositivoONoFinitoSeRechaza(Float precio) {
        GestionPlatoException error = assertThrows(
                GestionPlatoException.class,
                () -> service.alta(USERNAME, form("Pizza", precio, 0f)));

        assertTrue(error.getMessage().contains("precio de venta"));
        verify(unidadDeVentaRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @MethodSource("costosInvalidos")
    void costoAusenteNegativoONoFinitoSeRechaza(Float costo) {
        GestionPlatoException error = assertThrows(
                GestionPlatoException.class,
                () -> service.alta(USERNAME, form("Pizza", 100f, costo)));

        assertTrue(error.getMessage().contains("costo de producción"));
        verify(unidadDeVentaRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @MethodSource("costosNoMenoresQuePrecio")
    void precioMenorOIgualAlCostoSeRechaza(Float costo) {
        GestionPlatoException error = assertThrows(
                GestionPlatoException.class,
                () -> service.alta(USERNAME, form("Pizza", 100f, costo)));

        assertTrue(error.getMessage().contains("mayor que el costo"));
        verify(unidadDeVentaRepository, never()).saveAndFlush(any());
    }

    @Test
    void duplicadoActivoSeComparaSinMayusculasYConTrim() {
        unidad.getPlatos().add(plato(1, "  EMPANADA  ", 100f, 50f, false));

        GestionPlatoException error = assertThrows(
                GestionPlatoException.class,
                () -> service.alta(USERNAME, form("empanada", 150f, 60f)));

        assertTrue(error.getMessage().contains("Ya existe"));
        assertEquals(1, unidad.getPlatos().size());
        verify(unidadDeVentaRepository, never()).saveAndFlush(any());
    }

    @Test
    void mismoNombreInactivoPuedeReutilizarse() {
        unidad.getPlatos().add(plato(1, "Empanada", 100f, 50f, true));

        service.alta(USERNAME, form(" empanada ", 150f, 60f));

        assertEquals(2, unidad.getPlatos().size());
        assertEquals(1, unidad.getPlatos().stream().filter(plato -> !plato.isBaja()).count());
        verify(unidadDeVentaRepository).saveAndFlush(unidad);
    }

    @Test
    void mismoNombreEnOtraUnidadNoInterfiere() {
        FoodTruck otraUnidad = unidadAutorizada("otro@epicentro.com");
        otraUnidad.getPlatos().add(plato(9, "Pizza", 100f, 50f, false));

        service.alta(USERNAME, form("Pizza", 200f, 80f));

        assertEquals(1, unidad.getPlatos().size());
        assertNotSame(otraUnidad.getPlatos().iterator().next(), unidad.getPlatos().iterator().next());
    }

    @Test
    void prepararGestionListaActivosEInactivosSoloDeLaUnidadYOrdenados() {
        unidad.getPlatos().add(plato(2, "Pizza", 100f, 50f, true));
        unidad.getPlatos().add(plato(1, "Empanada", 80f, 40f, false));

        PlatoService.ContextoGestionPlatos contexto = service.prepararGestion(USERNAME);

        assertEquals("Ruedas de Fuego", contexto.unidad());
        assertEquals(List.of("Empanada", "Pizza"),
                contexto.platos().stream().map(PlatoService.PlatoResumen::nombre).toList());
        assertTrue(contexto.platos().get(0).activo());
        assertFalse(contexto.platos().get(1).activo());
        assertThrows(UnsupportedOperationException.class,
                () -> contexto.platos().add(contexto.platos().get(0)));
    }

    @Test
    void filtrarConDtoNuloDevuelveCatalogoCompleto() {
        cargarCatalogoParaFiltros();

        PlatoService.ContextoGestionPlatos contexto = service.filtrar(USERNAME, null);

        assertEquals(List.of("Agua", "Empanada", "Pizza especial", "Pizza veggie"),
                nombres(contexto));
    }

    @Test
    void filtrarConDtoVacioEstadoTodosONuloDevuelveCatalogoCompleto() {
        cargarCatalogoParaFiltros();
        FiltroPlatoDTO filtros = new FiltroPlatoDTO();
        filtros.setEstado(null);

        PlatoService.ContextoGestionPlatos contexto = service.filtrar(USERNAME, filtros);

        assertEquals(4, contexto.platos().size());
        assertTrue(contexto.platos().stream().anyMatch(PlatoService.PlatoResumen::activo));
        assertTrue(contexto.platos().stream().anyMatch(plato -> !plato.activo()));
    }

    @Test
    void filtrarNombreEsParcialIgnoraMayusculasYEspaciosExteriores() {
        cargarCatalogoParaFiltros();
        FiltroPlatoDTO filtros = filtros();
        filtros.setNombre("  PiZzA  ");

        PlatoService.ContextoGestionPlatos contexto = service.filtrar(USERNAME, filtros);

        assertEquals(List.of("Pizza especial", "Pizza veggie"), nombres(contexto));
    }

    @ParameterizedTest
    @MethodSource("nombresInvalidos")
    void filtrarNombreNuloVacioOEnBlancoNoRestringe(String nombre) {
        cargarCatalogoParaFiltros();
        FiltroPlatoDTO filtros = filtros();
        filtros.setNombre(nombre);

        assertEquals(4, service.filtrar(USERNAME, filtros).platos().size());
    }

    @Test
    void filtrarPrecioMinimoYMaximoFuncionanIndividualmente() {
        cargarCatalogoParaFiltros();
        FiltroPlatoDTO minimo = filtros();
        minimo.setPrecioVentaMinimo(1500f);
        FiltroPlatoDTO maximo = filtros();
        maximo.setPrecioVentaMaximo(800f);

        assertAll(
                () -> assertEquals(
                        List.of("Pizza especial", "Pizza veggie"),
                        nombres(service.filtrar(USERNAME, minimo))),
                () -> assertEquals(
                        List.of("Agua", "Empanada"),
                        nombres(service.filtrar(USERNAME, maximo))));
    }

    @Test
    void filtrarCostoMinimoYMaximoFuncionanIndividualmente() {
        cargarCatalogoParaFiltros();
        FiltroPlatoDTO minimo = filtros();
        minimo.setCostoProduccionMinimo(900f);
        FiltroPlatoDTO maximo = filtros();
        maximo.setCostoProduccionMaximo(400f);

        assertAll(
                () -> assertEquals(
                        List.of("Pizza especial", "Pizza veggie"),
                        nombres(service.filtrar(USERNAME, minimo))),
                () -> assertEquals(
                        List.of("Agua", "Empanada"),
                        nombres(service.filtrar(USERNAME, maximo))));
    }

    @Test
    void filtrarRangosIncluyeAmbosLimites() {
        cargarCatalogoParaFiltros();
        FiltroPlatoDTO filtros = filtros();
        filtros.setPrecioVentaMinimo(800f);
        filtros.setPrecioVentaMaximo(1500f);
        filtros.setCostoProduccionMinimo(400f);
        filtros.setCostoProduccionMaximo(900f);

        assertEquals(List.of("Empanada", "Pizza especial"),
                nombres(service.filtrar(USERNAME, filtros)));
    }

    @Test
    void filtrarEstadoTodosActivosEInactivosRespetaBajaLogica() {
        cargarCatalogoParaFiltros();
        FiltroPlatoDTO todos = filtros();
        FiltroPlatoDTO activos = filtros();
        activos.setEstado(EstadoPlato.ACTIVOS);
        FiltroPlatoDTO inactivos = filtros();
        inactivos.setEstado(EstadoPlato.INACTIVOS);

        assertAll(
                () -> assertEquals(4, service.filtrar(USERNAME, todos).platos().size()),
                () -> assertEquals(
                        List.of("Agua", "Empanada", "Pizza veggie"),
                        nombres(service.filtrar(USERNAME, activos))),
                () -> assertEquals(
                        List.of("Pizza especial"),
                        nombres(service.filtrar(USERNAME, inactivos))));
    }

    @Test
    void filtrarCombinaTodosLosCriteriosConAnd() {
        cargarCatalogoParaFiltros();
        FiltroPlatoDTO filtros = filtros();
        filtros.setNombre("pizza");
        filtros.setPrecioVentaMinimo(1000f);
        filtros.setPrecioVentaMaximo(3000f);
        filtros.setCostoProduccionMinimo(1000f);
        filtros.setCostoProduccionMaximo(1500f);
        filtros.setEstado(EstadoPlato.ACTIVOS);

        assertEquals(List.of("Pizza veggie"), nombres(service.filtrar(USERNAME, filtros)));
    }

    @Test
    void filtrarSinCoincidenciasDevuelveListaVaciaEInmutable() {
        cargarCatalogoParaFiltros();
        FiltroPlatoDTO filtros = filtros();
        filtros.setNombre("sushi");

        PlatoService.ContextoGestionPlatos contexto = service.filtrar(USERNAME, filtros);

        assertTrue(contexto.platos().isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> contexto.platos().add(
                        new PlatoService.PlatoResumen(99, "Otro", 1f, 0f, true)));
    }

    @ParameterizedTest
    @MethodSource("limitesInvalidosDelFiltro")
    void filtrarRechazaNegativosNanEInfinitosEnCadaLimite(
            String campo, Float valor) {
        FiltroPlatoDTO filtros = filtros();
        asignarLimite(filtros, campo, valor);

        GestionPlatoException error = assertThrows(
                GestionPlatoException.class,
                () -> service.filtrar(USERNAME, filtros));

        assertTrue(error.getMessage().contains("valor finito")
                || error.getMessage().contains("negativo"));
        verify(unidadDeVentaRepository, never()).saveAndFlush(any());
    }

    @Test
    void filtrarRechazaRangosInvertidosSinCorregirlos() {
        FiltroPlatoDTO precio = filtros();
        precio.setPrecioVentaMinimo(101f);
        precio.setPrecioVentaMaximo(100f);
        FiltroPlatoDTO costo = filtros();
        costo.setCostoProduccionMinimo(51f);
        costo.setCostoProduccionMaximo(50f);

        assertAll(
                () -> assertTrue(assertThrows(
                        GestionPlatoException.class,
                        () -> service.filtrar(USERNAME, precio)).getMessage().contains("precio")),
                () -> assertTrue(assertThrows(
                        GestionPlatoException.class,
                        () -> service.filtrar(USERNAME, costo)).getMessage().contains("costo")));
    }

    @Test
    void filtrarPermiteCeroComoLimite() {
        cargarCatalogoParaFiltros();
        FiltroPlatoDTO filtros = filtros();
        filtros.setPrecioVentaMinimo(0f);
        filtros.setCostoProduccionMinimo(0f);
        filtros.setCostoProduccionMaximo(0f);

        assertEquals(List.of("Agua"), nombres(service.filtrar(USERNAME, filtros)));
    }

    @Test
    void filtrarSoloRecorrePlatosDeLaUnidadAutorizada() {
        cargarCatalogoParaFiltros();
        FoodTruck otraUnidad = unidadAutorizada("otro@epicentro.com");
        otraUnidad.getPlatos().add(plato(99, "Pizza ajena", 1700f, 800f, false));
        FiltroPlatoDTO filtros = filtros();
        filtros.setNombre("pizza");

        PlatoService.ContextoGestionPlatos contexto = service.filtrar(USERNAME, filtros);

        assertEquals(List.of("Pizza especial", "Pizza veggie"), nombres(contexto));
        assertFalse(nombres(contexto).contains("Pizza ajena"));
    }

    @Test
    void filtrarMantieneOrdenPorNombreSinMayusculasYLuegoId() {
        unidad.getPlatos().add(plato(3, "pizza", 300f, 100f, false));
        unidad.getPlatos().add(plato(1, "Pizza", 100f, 20f, false));
        unidad.getPlatos().add(plato(2, "alfajor", 200f, 50f, false));

        PlatoService.ContextoGestionPlatos contexto = service.filtrar(USERNAME, filtros());

        assertEquals(List.of(2, 1, 3),
                contexto.platos().stream().map(PlatoService.PlatoResumen::id).toList());
        assertThrows(UnsupportedOperationException.class,
                () -> contexto.platos().clear());
    }

    @Test
    void filtrarReutilizaAutorizacionParaResponsableSinUnidadDeshabilitadoORolIncorrecto() {
        FiltroPlatoDTO filtros = filtros();
        when(unidadDeVentaRepository.findByResponsableUsername("sinunidad@epicentro.com"))
                .thenReturn(List.of());

        assertThrows(GestionPlatoException.class,
                () -> service.filtrar("sinunidad@epicentro.com", filtros));

        unidad.getResponsable().setEnabled(false);
        assertThrows(GestionPlatoException.class, () -> service.filtrar(USERNAME, filtros));

        unidad.getResponsable().setEnabled(true);
        unidad.getResponsable().setRole(new Role(RoleName.ROLE_EMPLEADO));
        assertThrows(GestionPlatoException.class, () -> service.filtrar(USERNAME, filtros));
    }

    @Test
    void filtrarEsSoloLecturaYNoUsaDependenciasDeReportes() {
        cargarCatalogoParaFiltros();

        service.filtrar(USERNAME, filtros());

        verify(unidadDeVentaRepository, never()).save(any(UnidadDeVenta.class));
        verify(unidadDeVentaRepository, never()).saveAll(any());
        verify(unidadDeVentaRepository, never()).saveAndFlush(any());
        verify(unidadDeVentaRepository, never()).flush();
        verificarSinEliminaciones();
        verifyNoInteractions(reporteResponsableService, pedidoRepository);
    }

    @Test
    void prepararAltaEntregaFormularioVacioSinExponerUnidadEditable() {
        PlatoService.ContextoFormularioPlato contexto = service.prepararAlta(USERNAME);

        assertAll(
                () -> assertEquals("Ruedas de Fuego", contexto.unidad()),
                () -> assertFalse(contexto.edicion()),
                () -> assertEquals(null, contexto.form().getNombre()));
    }

    @Test
    void prepararModificacionCargaSoloCamposEditables() {
        Plato existente = plato(7, "Pizza", 100f, 50f, false);
        unidad.getPlatos().add(existente);

        PlatoService.ContextoFormularioPlato contexto =
                service.prepararModificacion(USERNAME, 7);

        assertAll(
                () -> assertTrue(contexto.edicion()),
                () -> assertEquals("Pizza", contexto.form().getNombre()),
                () -> assertEquals(100f, contexto.form().getPrecioVenta()),
                () -> assertEquals(50f, contexto.form().getCostoProduccion()));
    }

    @Test
    void modificacionValidaCambiaSoloCamposEditablesYConservaAsociacion() {
        Plato existente = plato(7, "Pizza", 100f, 50f, false);
        unidad.getPlatos().add(existente);

        PlatoService.PlatoResumen resultado = service.modificar(
                USERNAME, 7, form("  Fugazzeta  ", 250f, 100f));

        assertAll(
                () -> assertEquals(7, existente.getId()),
                () -> assertEquals("Fugazzeta", existente.getNombre()),
                () -> assertEquals(250f, existente.getPrecioVenta()),
                () -> assertEquals(100f, existente.getCostoProduccion()),
                () -> assertFalse(existente.isBaja()),
                () -> assertTrue(unidad.getPlatos().contains(existente)),
                () -> assertEquals(7, resultado.id()));
        verify(unidadDeVentaRepository).saveAndFlush(unidad);
        verificarSinEliminaciones();
    }

    @Test
    void modificacionExcluyeElMismoPlatoAlValidarNombre() {
        Plato existente = plato(7, "Pizza", 100f, 50f, false);
        unidad.getPlatos().add(existente);

        service.modificar(USERNAME, 7, form(" pizza ", 120f, 60f));

        assertEquals("pizza", existente.getNombre());
        verify(unidadDeVentaRepository).saveAndFlush(unidad);
    }

    @Test
    void modificacionConNombreDeOtroActivoSeRechaza() {
        unidad.getPlatos().add(plato(7, "Pizza", 100f, 50f, false));
        unidad.getPlatos().add(plato(8, "Empanada", 80f, 40f, false));

        assertThrows(GestionPlatoException.class,
                () -> service.modificar(USERNAME, 7, form(" EMPANADA ", 120f, 60f)));

        verify(unidadDeVentaRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificacionInexistenteOAjenaSeRechaza() {
        Plato ajeno = plato(9, "Ajeno", 100f, 50f, false);

        GestionPlatoException error = assertThrows(
                GestionPlatoException.class,
                () -> service.modificar(USERNAME, ajeno.getId(), form("Nuevo", 200f, 80f)));

        assertTrue(error.getMessage().contains("no existe o no pertenece"));
        assertEquals("Ajeno", ajeno.getNombre());
        verify(unidadDeVentaRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificacionDeInactivoSeRechaza() {
        Plato inactivo = plato(7, "Pizza", 100f, 50f, true);
        unidad.getPlatos().add(inactivo);

        GestionPlatoException error = assertThrows(
                GestionPlatoException.class,
                () -> service.modificar(USERNAME, 7, form("Nueva", 200f, 80f)));

        assertTrue(error.getMessage().contains("dado de baja"));
        assertEquals("Pizza", inactivo.getNombre());
        verify(unidadDeVentaRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificacionRepiteValidacionesDeNegocio() {
        Plato existente = plato(7, "Pizza", 100f, 50f, false);
        unidad.getPlatos().add(existente);

        assertThrows(GestionPlatoException.class,
                () -> service.modificar(USERNAME, 7, form("Pizza", Float.NaN, 0f)));

        assertEquals(100f, existente.getPrecioVenta());
        verify(unidadDeVentaRepository, never()).saveAndFlush(any());
    }

    @Test
    void bajaValidaConservaRegistroYAsociacion() {
        Plato existente = plato(7, "Pizza", 100f, 50f, false);
        unidad.getPlatos().add(existente);

        service.baja(USERNAME, 7);

        assertTrue(existente.isBaja());
        assertEquals(1, unidad.getPlatos().size());
        assertTrue(unidad.getPlatos().contains(existente));
        verify(unidadDeVentaRepository).saveAndFlush(unidad);
        verificarSinEliminaciones();
    }

    @Test
    void bajaRepetidaSeRechazaYConservaRegistro() {
        Plato existente = plato(7, "Pizza", 100f, 50f, true);
        unidad.getPlatos().add(existente);

        GestionPlatoException error = assertThrows(
                GestionPlatoException.class,
                () -> service.baja(USERNAME, 7));

        assertTrue(error.getMessage().contains("ya se encuentra dado de baja"));
        assertTrue(unidad.getPlatos().contains(existente));
        verify(unidadDeVentaRepository, never()).saveAndFlush(any());
    }

    @Test
    void bajaDePlatoAjenoSeRechaza() {
        Plato ajeno = plato(9, "Ajeno", 100f, 50f, false);

        assertThrows(GestionPlatoException.class, () -> service.baja(USERNAME, ajeno.getId()));

        assertFalse(ajeno.isBaja());
        verify(unidadDeVentaRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @MethodSource("usernamesInvalidos")
    void usernameInvalidoSeRechazaSinConsultar(String username) {
        assertThrows(GestionPlatoException.class, () -> service.prepararGestion(username));

        verify(unidadDeVentaRepository, never()).findByResponsableUsername(username);
    }

    @Test
    void responsableSinUnidadSeRechaza() {
        when(unidadDeVentaRepository.findByResponsableUsername("sinunidad@epicentro.com"))
                .thenReturn(List.of());

        GestionPlatoException error = assertThrows(
                GestionPlatoException.class,
                () -> service.prepararGestion("sinunidad@epicentro.com"));

        assertTrue(error.getMessage().contains("No se encontró una unidad"));
    }

    @Test
    void cardinalidadInesperadaSeRechaza() {
        when(unidadDeVentaRepository.findByResponsableUsername(USERNAME))
                .thenReturn(List.of(unidad, unidadAutorizada(USERNAME)));

        GestionPlatoException error = assertThrows(
                GestionPlatoException.class,
                () -> service.prepararGestion(USERNAME));

        assertTrue(error.getMessage().contains("inconsistente"));
    }

    @Test
    void responsableNoCoincidenteSeRechaza() {
        unidad.getResponsable().setUsername("otro@epicentro.com");

        assertThrows(GestionPlatoException.class, () -> service.prepararGestion(USERNAME));
    }

    @Test
    void usuarioDeshabilitadoSeRechaza() {
        unidad.getResponsable().setEnabled(false);

        GestionPlatoException error = assertThrows(
                GestionPlatoException.class,
                () -> service.prepararGestion(USERNAME));

        assertTrue(error.getMessage().contains("habilitado o autorizado"));
    }

    @Test
    void rolIncorrectoSeRechaza() {
        unidad.getResponsable().setRole(new Role(RoleName.ROLE_EMPLEADO));

        GestionPlatoException error = assertThrows(
                GestionPlatoException.class,
                () -> service.prepararGestion(USERNAME));

        assertTrue(error.getMessage().contains("habilitado o autorizado"));
    }

    @Test
    void dependenciasDeCu20NoSeUsanDuranteElAbm() {
        service.alta(USERNAME, form("Pizza", 100f, 50f));

        verifyNoInteractions(reporteResponsableService, pedidoRepository);
    }

    private void verificarSinEliminaciones() {
        verify(unidadDeVentaRepository, never()).delete(any(UnidadDeVenta.class));
        verify(unidadDeVentaRepository, never()).deleteById(any());
        verify(unidadDeVentaRepository, never()).deleteAll();
    }

    private FoodTruck unidadAutorizada(String username) {
        Role role = new Role(RoleName.ROLE_RESPONSABLE);
        User responsable = new User(username, "hash", true, role);
        FoodTruck foodTruck = new FoodTruck();
        foodTruck.setId(10);
        foodTruck.setNombreComercial("Ruedas de Fuego");
        foodTruck.setResponsable(responsable);
        foodTruck.setPlatos(new HashSet<>());
        return foodTruck;
    }

    private PlatoForm form(String nombre, Float precioVenta, Float costoProduccion) {
        PlatoForm form = new PlatoForm();
        form.setNombre(nombre);
        form.setPrecioVenta(precioVenta);
        form.setCostoProduccion(costoProduccion);
        return form;
    }

    private FiltroPlatoDTO filtros() {
        return new FiltroPlatoDTO();
    }

    private List<String> nombres(PlatoService.ContextoGestionPlatos contexto) {
        return contexto.platos().stream().map(PlatoService.PlatoResumen::nombre).toList();
    }

    private void cargarCatalogoParaFiltros() {
        unidad.getPlatos().add(plato(4, "Agua", 100f, 0f, false));
        unidad.getPlatos().add(plato(1, "Empanada", 800f, 400f, false));
        unidad.getPlatos().add(plato(2, "Pizza especial", 1500f, 900f, true));
        unidad.getPlatos().add(plato(3, "Pizza veggie", 2500f, 1200f, false));
    }

    private void asignarLimite(FiltroPlatoDTO filtros, String campo, Float valor) {
        switch (campo) {
            case "precioVentaMinimo" -> filtros.setPrecioVentaMinimo(valor);
            case "precioVentaMaximo" -> filtros.setPrecioVentaMaximo(valor);
            case "costoProduccionMinimo" -> filtros.setCostoProduccionMinimo(valor);
            case "costoProduccionMaximo" -> filtros.setCostoProduccionMaximo(valor);
            default -> throw new IllegalArgumentException("Campo de prueba desconocido: " + campo);
        }
    }

    private Plato plato(int id,
                        String nombre,
                        float precioVenta,
                        float costoProduccion,
                        boolean baja) {
        Plato plato = new Plato();
        plato.setId(id);
        plato.setNombre(nombre);
        plato.setPrecioVenta(precioVenta);
        plato.setCostoProduccion(costoProduccion);
        plato.setBaja(baja);
        return plato;
    }

    private static Stream<String> nombresInvalidos() {
        return Stream.of(null, "", "   ");
    }

    private static Stream<Arguments> limitesInvalidosDelFiltro() {
        return Stream.of(
                "precioVentaMinimo",
                "precioVentaMaximo",
                "costoProduccionMinimo",
                "costoProduccionMaximo")
                .flatMap(campo -> Stream.of(
                        Arguments.of(campo, -1f),
                        Arguments.of(campo, Float.NaN),
                        Arguments.of(campo, Float.POSITIVE_INFINITY),
                        Arguments.of(campo, Float.NEGATIVE_INFINITY)));
    }

    private static Stream<Float> preciosInvalidos() {
        return Stream.of(null, 0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
    }

    private static Stream<Float> costosInvalidos() {
        return Stream.of(null, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
    }

    private static Stream<Float> costosNoMenoresQuePrecio() {
        return Stream.of(100f, 101f);
    }

    private static Stream<String> usernamesInvalidos() {
        return Stream.of(null, "", "   ");
    }
}
