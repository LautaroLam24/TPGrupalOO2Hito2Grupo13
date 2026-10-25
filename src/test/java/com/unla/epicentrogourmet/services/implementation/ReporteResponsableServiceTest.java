package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.entities.FoodTruck;
import com.unla.epicentrogourmet.entities.Role;
import com.unla.epicentrogourmet.entities.RoleName;
import com.unla.epicentrogourmet.entities.User;
import com.unla.epicentrogourmet.exceptions.ReporteResponsableException;
import com.unla.epicentrogourmet.repositories.IFestivalRepository;
import com.unla.epicentrogourmet.repositories.IUnidadDeVentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteResponsableServiceTest {

    private static final String USERNAME = "responsable@epicentro.com";

    @Mock
    private IUnidadDeVentaRepository unidadDeVentaRepository;

    @Mock
    private IFestivalRepository festivalRepository;

    private ReporteResponsableService service;
    private FoodTruck unidad;

    @BeforeEach
    void setUp() {
        service = new ReporteResponsableService(unidadDeVentaRepository, festivalRepository);
        unidad = new FoodTruck();
        unidad.setId(10);
        unidad.setNombreComercial("Ruedas de Fuego");
        unidad.setResponsable(usuario(USERNAME, true, RoleName.ROLE_RESPONSABLE));
    }

    @Test
    void responsableValidoObtieneContextoInmutableConFestivalesDeTodoEstado() {
        Festival futuro = festival(3, "Futuro", LocalDate.now().plusDays(5), LocalDate.now().plusDays(8));
        Festival vigente = festival(2, "Vigente", LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        Festival finalizado = festival(1, "Finalizado", LocalDate.now().minusDays(8), LocalDate.now().minusDays(5));
        autorizarUnidad();
        when(festivalRepository.findByUnidadId(unidad.getId()))
                .thenReturn(List.of(futuro, vigente, finalizado));

        ReporteResponsableService.ContextoReportes contexto = service.preparar(USERNAME);

        assertEquals("Ruedas de Fuego", contexto.unidad());
        assertEquals(List.of(3, 2, 1), contexto.festivales().stream()
                .map(ReporteResponsableService.FestivalOpcion::id)
                .toList());
        assertThrows(UnsupportedOperationException.class,
                () -> contexto.festivales().add(contexto.festivales().get(0)));
        verify(unidadDeVentaRepository).findByResponsableUsername(USERNAME);
        verify(festivalRepository).findByUnidadId(unidad.getId());
        verifyNoMoreInteractions(unidadDeVentaRepository, festivalRepository);
    }

    @Test
    void rechazaUsernameNuloVacioOEnBlanco() {
        assertAll(
                () -> assertThrows(ReporteResponsableException.class, () -> service.preparar(null)),
                () -> assertThrows(ReporteResponsableException.class, () -> service.preparar("")),
                () -> assertThrows(ReporteResponsableException.class, () -> service.preparar("   ")));

        verifyNoInteractions(unidadDeVentaRepository, festivalRepository);
    }

    @Test
    void rechazaResponsableSinUnidad() {
        when(unidadDeVentaRepository.findByResponsableUsername(USERNAME)).thenReturn(List.of());

        ReporteResponsableException error = assertThrows(
                ReporteResponsableException.class,
                () -> service.preparar(USERNAME));

        assertTrue(error.getMessage().contains("No se encontró una unidad"));
        verifyNoInteractions(festivalRepository);
    }

    @Test
    void rechazaCardinalidadInesperadaDeUnidades() {
        FoodTruck otra = new FoodTruck();
        otra.setResponsable(usuario(USERNAME, true, RoleName.ROLE_RESPONSABLE));
        when(unidadDeVentaRepository.findByResponsableUsername(USERNAME))
                .thenReturn(List.of(unidad, otra));

        ReporteResponsableException error = assertThrows(
                ReporteResponsableException.class,
                () -> service.preparar(USERNAME));

        assertTrue(error.getMessage().contains("inconsistente"));
        verifyNoInteractions(festivalRepository);
    }

    @Test
    void rechazaUnidadSinResponsable() {
        unidad.setResponsable(null);
        autorizarUnidad();

        assertThrows(ReporteResponsableException.class, () -> service.preparar(USERNAME));

        verifyNoInteractions(festivalRepository);
    }

    @Test
    void rechazaResponsableCuyoUsernameNoCoincide() {
        unidad.getResponsable().setUsername("otro@epicentro.com");
        autorizarUnidad();

        assertThrows(ReporteResponsableException.class, () -> service.preparar(USERNAME));

        verifyNoInteractions(festivalRepository);
    }

    @Test
    void rechazaUsuarioDeshabilitado() {
        unidad.getResponsable().setEnabled(false);
        autorizarUnidad();

        ReporteResponsableException error = assertThrows(
                ReporteResponsableException.class,
                () -> service.preparar(USERNAME));

        assertTrue(error.getMessage().contains("habilitado o autorizado"));
        verifyNoInteractions(festivalRepository);
    }

    @Test
    void rechazaRolNuloOIncorrecto() {
        unidad.getResponsable().setRole(null);
        autorizarUnidad();
        assertThrows(ReporteResponsableException.class, () -> service.preparar(USERNAME));

        unidad.getResponsable().setRole(new Role(RoleName.ROLE_ADMIN));
        autorizarUnidad();
        assertThrows(ReporteResponsableException.class, () -> service.preparar(USERNAME));

        verifyNoInteractions(festivalRepository);
    }

    @Test
    void autorizaFestivalAsociadoYDevuelveUnidadYFestivalPersistidos() {
        Festival festival = festival(
                20, "Festival Sur", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));
        autorizarUnidad();
        when(festivalRepository.findByIdAndUnidadId(festival.getId(), unidad.getId()))
                .thenReturn(Optional.of(festival));

        ReporteResponsableService.ContextoFestivalAutorizado contexto =
                service.autorizarFestival(USERNAME, festival.getId());

        assertSame(unidad, contexto.unidad());
        assertSame(festival, contexto.festival());
        verify(festivalRepository).findByIdAndUnidadId(festival.getId(), unidad.getId());
    }

    @Test
    void rechazaFestivalInexistenteONoAsociadoConMensajeUnico() {
        autorizarUnidad();
        when(festivalRepository.findByIdAndUnidadId(999, unidad.getId()))
                .thenReturn(Optional.empty());

        ReporteResponsableException error = assertThrows(
                ReporteResponsableException.class,
                () -> service.autorizarFestival(USERNAME, 999));

        assertTrue(error.getMessage().contains("no existe o no está asociado"));
    }

    @Test
    void rechazaFechaInicialNula() {
        Festival festival = festival(20, "Inválido", null, LocalDate.now());
        autorizarFestival(festival);

        assertThrows(ReporteResponsableException.class,
                () -> service.autorizarFestival(USERNAME, festival.getId()));
    }

    @Test
    void rechazaFechaFinalNula() {
        Festival festival = festival(20, "Inválido", LocalDate.now(), null);
        autorizarFestival(festival);

        assertThrows(ReporteResponsableException.class,
                () -> service.autorizarFestival(USERNAME, festival.getId()));
    }

    @Test
    void rechazaInicioPosteriorAlFin() {
        Festival festival = festival(
                20, "Inválido", LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1));
        autorizarFestival(festival);

        ReporteResponsableException error = assertThrows(
                ReporteResponsableException.class,
                () -> service.autorizarFestival(USERNAME, festival.getId()));

        assertTrue(error.getMessage().contains("intervalo de fechas inválido"));
    }

    private void autorizarUnidad() {
        when(unidadDeVentaRepository.findByResponsableUsername(USERNAME))
                .thenReturn(List.of(unidad));
    }

    private void autorizarFestival(Festival festival) {
        autorizarUnidad();
        when(festivalRepository.findByIdAndUnidadId(festival.getId(), unidad.getId()))
                .thenReturn(Optional.of(festival));
    }

    private Festival festival(int id, String nombre, LocalDate inicio, LocalDate fin) {
        Festival festival = new Festival();
        festival.setId(id);
        festival.setNombre(nombre);
        festival.setFechaInicio(inicio);
        festival.setFechaFin(fin);
        return festival;
    }

    private User usuario(String username, boolean enabled, RoleName roleName) {
        return new User(username, "password", enabled, new Role(roleName));
    }
}
