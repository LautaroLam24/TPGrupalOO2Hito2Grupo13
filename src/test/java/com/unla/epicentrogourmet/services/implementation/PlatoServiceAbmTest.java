package com.unla.epicentrogourmet.services.implementation;

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
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
