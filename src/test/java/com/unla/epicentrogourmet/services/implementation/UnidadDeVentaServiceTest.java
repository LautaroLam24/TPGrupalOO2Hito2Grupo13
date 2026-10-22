package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.RegistroUnidadResponsableForm;
import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.entities.FoodTruck;
import com.unla.epicentrogourmet.entities.PuestoDesarmable;
import com.unla.epicentrogourmet.entities.Role;
import com.unla.epicentrogourmet.entities.RoleName;
import com.unla.epicentrogourmet.entities.UnidadDeVenta;
import com.unla.epicentrogourmet.entities.User;
import com.unla.epicentrogourmet.exceptions.RegistroUnidadException;
import com.unla.epicentrogourmet.repositories.IFestivalRepository;
import com.unla.epicentrogourmet.repositories.IFoodTruckRepository;
import com.unla.epicentrogourmet.repositories.IRoleRepository;
import com.unla.epicentrogourmet.repositories.IUnidadDeVentaRepository;
import com.unla.epicentrogourmet.repositories.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnidadDeVentaServiceTest {

    @Mock
    private IUnidadDeVentaRepository unidadDeVentaRepository;

    @Mock
    private IFoodTruckRepository foodTruckRepository;

    @Mock
    private IFestivalRepository festivalRepository;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IRoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UnidadDeVentaService service;

    @BeforeEach
    void setUp() {
        service = new UnidadDeVentaService(
                unidadDeVentaRepository,
                foodTruckRepository,
                festivalRepository,
                userRepository,
                roleRepository,
                passwordEncoder);
    }

    @Test
    void registraFoodTruckConResponsableYFestival() {
        Festival festival = festivalDisponible();
        Role role = new Role(RoleName.ROLE_RESPONSABLE);
        RegistroUnidadResponsableForm form = formularioFoodTruck();
        prepararRegistroExitoso(form, festival, role);

        service.registrarConResponsable(form);

        ArgumentCaptor<UnidadDeVenta> captor = ArgumentCaptor.forClass(UnidadDeVenta.class);
        InOrder ordenPersistencia = inOrder(unidadDeVentaRepository, festivalRepository);
        ordenPersistencia.verify(unidadDeVentaRepository).saveAndFlush(captor.capture());
        ordenPersistencia.verify(festivalRepository).saveAndFlush(festival);

        FoodTruck unidad = assertInstanceOf(FoodTruck.class, captor.getValue());
        assertEquals("ABCD123456", unidad.getCodigo());
        assertEquals("AA123BB", unidad.getPatente());
        assertTrue(unidad.isRequiereElectricidad());
        assertEquals("hash-bcrypt", unidad.getResponsable().getPassword());
        assertTrue(unidad.getResponsable().isEnabled());
        assertSame(role, unidad.getResponsable().getRole());
        assertTrue(festival.getUnidades().contains(unidad));
        verify(passwordEncoder).encode("secreta");
    }

    @Test
    void registraPuestoDesarmableSinConsultarPatentes() {
        Festival festival = festivalDisponible();
        Role role = new Role(RoleName.ROLE_RESPONSABLE);
        RegistroUnidadResponsableForm form = formularioPuesto();
        prepararRegistroExitoso(form, festival, role);

        service.registrarConResponsable(form);

        ArgumentCaptor<UnidadDeVenta> captor = ArgumentCaptor.forClass(UnidadDeVenta.class);
        verify(unidadDeVentaRepository).saveAndFlush(captor.capture());
        PuestoDesarmable unidad = assertInstanceOf(PuestoDesarmable.class, captor.getValue());
        assertEquals(3, unidad.getCantCarpas());
        assertEquals(45, unidad.getTiempoMontajeMin());
        assertSame(role, unidad.getResponsable().getRole());
        assertTrue(festival.getUnidades().contains(unidad));
        verifyNoInteractions(foodTruckRepository);
    }

    @Test
    void rechazaCodigoQueNoIncluyeNumeros() {
        RegistroUnidadResponsableForm form = formularioFoodTruck();
        form.setCodigo("ABCDEFGHIJ");

        RegistroUnidadException error = assertThrows(
                RegistroUnidadException.class,
                () -> service.registrarConResponsable(form));

        assertTrue(error.getMessage().contains("al menos una letra y un número"));
        verifyNoInteractions(unidadDeVentaRepository, festivalRepository, userRepository);
    }

    @Test
    void rechazaCodigoDuplicadoDespuesDeNormalizarlo() {
        RegistroUnidadResponsableForm form = formularioFoodTruck();
        when(unidadDeVentaRepository.existsByCodigo("ABCD123456")).thenReturn(true);

        RegistroUnidadException error = assertThrows(
                RegistroUnidadException.class,
                () -> service.registrarConResponsable(form));

        assertEquals("El código de unidad ya está registrado.", error.getMessage());
        verify(unidadDeVentaRepository).existsByCodigo("ABCD123456");
        verifyNoInteractions(festivalRepository, userRepository);
    }

    @Test
    void rechazaUsernameDuplicado() {
        RegistroUnidadResponsableForm form = formularioFoodTruck();
        when(unidadDeVentaRepository.existsByCodigo("ABCD123456")).thenReturn(false);
        when(userRepository.findByUsername(form.getUsername())).thenReturn(Optional.of(new User()));

        RegistroUnidadException error = assertThrows(
                RegistroUnidadException.class,
                () -> service.registrarConResponsable(form));

        assertEquals("El email del responsable ya está registrado.", error.getMessage());
        verifyNoInteractions(festivalRepository);
    }

    @Test
    void rechazaPatenteDuplicadaDespuesDeNormalizarla() {
        RegistroUnidadResponsableForm form = formularioFoodTruck();
        permitirCodigoYUsuarioUnicos(form);
        when(foodTruckRepository.existsByPatente("AA123BB")).thenReturn(true);

        RegistroUnidadException error = assertThrows(
                RegistroUnidadException.class,
                () -> service.registrarConResponsable(form));

        assertEquals("La patente ya está registrada.", error.getMessage());
        verify(foodTruckRepository).existsByPatente("AA123BB");
        verifyNoInteractions(festivalRepository);
    }

    @Test
    void rechazaFestivalInexistente() {
        RegistroUnidadResponsableForm form = formularioFoodTruck();
        permitirDatosUnicos(form);
        when(festivalRepository.findById(form.getFestivalId())).thenReturn(Optional.empty());

        RegistroUnidadException error = assertThrows(
                RegistroUnidadException.class,
                () -> service.registrarConResponsable(form));

        assertEquals("El festival seleccionado no existe.", error.getMessage());
    }

    @Test
    void rechazaFestivalFinalizado() {
        RegistroUnidadResponsableForm form = formularioFoodTruck();
        Festival finalizado = festivalDisponible();
        finalizado.setFechaFin(LocalDate.now().minusDays(1));
        permitirDatosUnicos(form);
        when(festivalRepository.findById(form.getFestivalId())).thenReturn(Optional.of(finalizado));

        RegistroUnidadException error = assertThrows(
                RegistroUnidadException.class,
                () -> service.registrarConResponsable(form));

        assertEquals("El festival seleccionado ya finalizó.", error.getMessage());
    }

    @Test
    void rechazaFoodTruckSinPatente() {
        RegistroUnidadResponsableForm form = formularioFoodTruck();
        form.setPatente(" ");

        RegistroUnidadException error = assertThrows(
                RegistroUnidadException.class,
                () -> service.registrarConResponsable(form));

        assertEquals("La patente es obligatoria para un Food Truck.", error.getMessage());
        verifyNoInteractions(unidadDeVentaRepository, festivalRepository);
    }

    @Test
    void rechazaPuestoConCantidadDeCarpasInvalida() {
        RegistroUnidadResponsableForm form = formularioPuesto();
        form.setCantCarpas(0);

        RegistroUnidadException error = assertThrows(
                RegistroUnidadException.class,
                () -> service.registrarConResponsable(form));

        assertEquals("La cantidad de carpas debe ser mayor que cero.", error.getMessage());
        verifyNoInteractions(unidadDeVentaRepository, festivalRepository);
    }

    @Test
    void rechazaPuestoConTiempoDeMontajeNegativo() {
        RegistroUnidadResponsableForm form = formularioPuesto();
        form.setTiempoMontajeMin(-1);

        RegistroUnidadException error = assertThrows(
                RegistroUnidadException.class,
                () -> service.registrarConResponsable(form));

        assertEquals("El tiempo de montaje debe ser mayor o igual a cero.", error.getMessage());
        verifyNoInteractions(unidadDeVentaRepository, festivalRepository);
    }

    @Test
    void rechazaSuperficieNoPositiva() {
        RegistroUnidadResponsableForm form = formularioFoodTruck();
        form.setSuperficieM2(0f);

        RegistroUnidadException error = assertThrows(
                RegistroUnidadException.class,
                () -> service.registrarConResponsable(form));

        assertEquals("La superficie debe ser mayor que cero.", error.getMessage());
        verifyNoInteractions(unidadDeVentaRepository, festivalRepository);
    }

    @Test
    void rechazaRegistroCuandoFaltaRolResponsable() {
        RegistroUnidadResponsableForm form = formularioFoodTruck();
        Festival festival = festivalDisponible();
        permitirDatosUnicos(form);
        when(festivalRepository.findById(form.getFestivalId())).thenReturn(Optional.of(festival));
        when(roleRepository.findByRole(RoleName.ROLE_RESPONSABLE)).thenReturn(Optional.empty());

        RegistroUnidadException error = assertThrows(
                RegistroUnidadException.class,
                () -> service.registrarConResponsable(form));

        assertEquals("No está configurado el rol de responsable.", error.getMessage());
    }

    @Test
    void propagaFallaInesperadaParaPermitirRollback() {
        Festival festival = festivalDisponible();
        Role role = new Role(RoleName.ROLE_RESPONSABLE);
        RegistroUnidadResponsableForm form = formularioFoodTruck();
        prepararDatosPrevios(form, festival, role);
        RuntimeException falla = new IllegalStateException("falla inesperada");
        when(unidadDeVentaRepository.saveAndFlush(any(UnidadDeVenta.class))).thenThrow(falla);

        RuntimeException resultado = assertThrows(
                RuntimeException.class,
                () -> service.registrarConResponsable(form));

        assertSame(falla, resultado);
        verify(festivalRepository, never()).saveAndFlush(any(Festival.class));
    }

    @Test
    void traduceConflictoDeIntegridadSinFiltrarDetalles() {
        Festival festival = festivalDisponible();
        Role role = new Role(RoleName.ROLE_RESPONSABLE);
        RegistroUnidadResponsableForm form = formularioFoodTruck();
        prepararDatosPrevios(form, festival, role);
        when(unidadDeVentaRepository.saveAndFlush(any(UnidadDeVenta.class)))
                .thenThrow(new DataIntegrityViolationException("constraint interno"));

        RegistroUnidadException error = assertThrows(
                RegistroUnidadException.class,
                () -> service.registrarConResponsable(form));

        assertEquals(
                "No se pudo registrar la unidad porque alguno de sus datos únicos ya existe.",
                error.getMessage());
    }

    private void prepararRegistroExitoso(RegistroUnidadResponsableForm form,
                                         Festival festival,
                                         Role role) {
        prepararDatosPrevios(form, festival, role);
        when(unidadDeVentaRepository.saveAndFlush(any(UnidadDeVenta.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(festivalRepository.saveAndFlush(festival)).thenReturn(festival);
    }

    private void prepararDatosPrevios(RegistroUnidadResponsableForm form,
                                      Festival festival,
                                      Role role) {
        permitirDatosUnicos(form);
        when(festivalRepository.findById(form.getFestivalId())).thenReturn(Optional.of(festival));
        when(roleRepository.findByRole(RoleName.ROLE_RESPONSABLE)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(form.getPassword())).thenReturn("hash-bcrypt");
    }

    private void permitirDatosUnicos(RegistroUnidadResponsableForm form) {
        permitirCodigoYUsuarioUnicos(form);
        if (UnidadDeVentaService.TIPO_FOOD_TRUCK.equals(form.getTipoUnidad())) {
            when(foodTruckRepository.existsByPatente("AA123BB")).thenReturn(false);
        }
    }

    private void permitirCodigoYUsuarioUnicos(RegistroUnidadResponsableForm form) {
        when(unidadDeVentaRepository.existsByCodigo(form.getCodigo().toUpperCase(Locale.ROOT))).thenReturn(false);
        when(userRepository.findByUsername(form.getUsername())).thenReturn(Optional.empty());
    }

    private Festival festivalDisponible() {
        Festival festival = new Festival();
        festival.setId(1);
        festival.setNombre("Festival vigente");
        festival.setFechaInicio(LocalDate.now());
        festival.setFechaFin(LocalDate.now().plusDays(3));
        return festival;
    }

    private RegistroUnidadResponsableForm formularioFoodTruck() {
        RegistroUnidadResponsableForm form = formularioComun();
        form.setTipoUnidad(UnidadDeVentaService.TIPO_FOOD_TRUCK);
        form.setCodigo("abcd123456");
        form.setPatente("aa123bb");
        form.setRequiereElectricidad(true);
        return form;
    }

    private RegistroUnidadResponsableForm formularioPuesto() {
        RegistroUnidadResponsableForm form = formularioComun();
        form.setTipoUnidad(UnidadDeVentaService.TIPO_PUESTO_DESARMABLE);
        form.setCodigo("puesto1234");
        form.setCantCarpas(3);
        form.setTiempoMontajeMin(45);
        return form;
    }

    private RegistroUnidadResponsableForm formularioComun() {
        RegistroUnidadResponsableForm form = new RegistroUnidadResponsableForm();
        form.setFestivalId(1);
        form.setNombreComercial("Unidad de prueba");
        form.setSuperficieM2(20f);
        form.setUsername("responsable@ejemplo.com");
        form.setPassword("secreta");
        return form;
    }
}
