package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.CanonUnidadDTO;
import com.unla.epicentrogourmet.dto.RegistroUnidadResponsableForm;
import com.unla.epicentrogourmet.dto.ReporteCanonDTO;
import com.unla.epicentrogourmet.entities.Costo;
import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.entities.FoodTruck;
import com.unla.epicentrogourmet.entities.Personal;
import com.unla.epicentrogourmet.entities.PuestoDesarmable;
import com.unla.epicentrogourmet.entities.Role;
import com.unla.epicentrogourmet.entities.RoleName;
import com.unla.epicentrogourmet.entities.UnidadDeVenta;
import com.unla.epicentrogourmet.entities.User;
import com.unla.epicentrogourmet.exceptions.RegistroUnidadException;
import com.unla.epicentrogourmet.exceptions.ReporteResponsableException;
import com.unla.epicentrogourmet.repositories.IFestivalRepository;
import com.unla.epicentrogourmet.repositories.IFoodTruckRepository;
import com.unla.epicentrogourmet.repositories.IPersonalRepository;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
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

    @Mock
    private IPersonalRepository personalRepository;

    private UnidadDeVentaService service;

    @BeforeEach
    void setUp() {
        service = new UnidadDeVentaService(
                unidadDeVentaRepository,
                foodTruckRepository,
                festivalRepository,
                userRepository,
                roleRepository,
                passwordEncoder,
                personalRepository);
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

    @Test
    void traeLaUnidadDelResponsable() {
        FoodTruck unidad = new FoodTruck();
        when(unidadDeVentaRepository.findByResponsableUsername("responsable@test.com"))
                .thenReturn(List.of(unidad));

        Optional<UnidadDeVenta> resultado = service.traerUnidadDelUsuario("responsable@test.com");

        assertSame(unidad, resultado.orElseThrow());
        verifyNoInteractions(personalRepository);
    }

    @Test
    void traeLaUnidadDelEmpleado() {
        Personal empleado = mock(Personal.class);
        PuestoDesarmable unidad = new PuestoDesarmable();
        when(unidadDeVentaRepository.findByResponsableUsername("empleado@test.com")).thenReturn(List.of());
        when(personalRepository.findByUserUsernameAndBajaFalse("empleado@test.com")).thenReturn(List.of(empleado));
        when(unidadDeVentaRepository.findByPersonal(empleado)).thenReturn(Optional.of(unidad));

        Optional<UnidadDeVenta> resultado = service.traerUnidadDelUsuario("empleado@test.com");

        assertSame(unidad, resultado.orElseThrow());
    }

    @Test
    void devuelveVacioSiElUsuarioNoTieneUnidad() {
        when(unidadDeVentaRepository.findByResponsableUsername("admin@test.com")).thenReturn(List.of());
        when(personalRepository.findByUserUsernameAndBajaFalse("admin@test.com")).thenReturn(List.of());

        assertTrue(service.traerUnidadDelUsuario("admin@test.com").isEmpty());
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

    // CU15: canon de la unidad por festival

    private static final String RESPONSABLE_CANON = "responsable1@epicentro.com";

    @Test
    void canonPorFestivalDeFoodTruckConElectricidadSumaElPlus() {
        FoodTruck foodTruck = foodTruckCanon(true);
        Festival festival = festivalConCosto("Festival de la Cerveza", -5, 10, 500f, 10f, 2000f);
        prepararCanon(foodTruck, List.of(festival));

        ReporteCanonDTO reporte = service.canonPorFestival(RESPONSABLE_CANON);

        assertEquals("Ruedas de Fuego", reporte.nombreComercial());
        assertEquals("Food truck", reporte.tipo());
        assertEquals("FTK0000001", reporte.codigo());
        assertEquals(12f, reporte.superficieM2());
        assertEquals(Boolean.TRUE, reporte.requiereElectricidad());
        assertNull(reporte.cantCarpas());
        assertNull(reporte.tiempoMontajeMin());

        CanonUnidadDTO canon = reporte.canones().get(0);
        assertEquals("Festival de la Cerveza", canon.festival());
        assertEquals("En curso", canon.estado());
        assertEquals(500f, canon.costoSuperficie());
        assertEquals(6000.0d, canon.subtotalSuperficie());
        assertEquals("Plus por conexión eléctrica", canon.conceptoAjuste());
        assertEquals(2000f, canon.costoUnitarioAjuste());
        assertEquals(2000.0d, canon.ajuste());
        assertEquals(8000.0d, canon.canon());
        assertEquals(8000.0d, reporte.canonTotal());
    }

    @Test
    void canonPorFestivalDeFoodTruckSinElectricidadNoSumaPlus() {
        FoodTruck foodTruck = foodTruckCanon(false);
        Festival festival = festivalConCosto("Festival de la Cerveza", -5, 10, 500f, 10f, 2000f);
        prepararCanon(foodTruck, List.of(festival));

        ReporteCanonDTO reporte = service.canonPorFestival(RESPONSABLE_CANON);

        CanonUnidadDTO canon = reporte.canones().get(0);
        assertEquals(Boolean.FALSE, reporte.requiereElectricidad());
        assertEquals("Sin conexión eléctrica", canon.conceptoAjuste());
        assertEquals(0.0d, canon.ajuste());
        assertEquals(6000.0d, canon.canon());
        assertEquals(6000.0d, reporte.canonTotal());
    }

    @Test
    void canonPorFestivalDePuestoDescuentaElMontaje() {
        PuestoDesarmable puesto = new PuestoDesarmable();
        puesto.setId(2);
        puesto.setCodigo("PDS0000001");
        puesto.setNombreComercial("La Carpa Criolla");
        puesto.setSuperficieM2(20f);
        puesto.setCantCarpas(2);
        puesto.setTiempoMontajeMin(90);
        Festival festival = festivalConCosto("Festival de la Cerveza", -5, 10, 500f, 10f, 2000f);
        prepararCanon(puesto, List.of(festival));

        ReporteCanonDTO reporte = service.canonPorFestival(RESPONSABLE_CANON);

        assertEquals("Puesto desarmable", reporte.tipo());
        assertNull(reporte.requiereElectricidad());
        assertEquals(2, reporte.cantCarpas());
        assertEquals(90, reporte.tiempoMontajeMin());

        CanonUnidadDTO canon = reporte.canones().get(0);
        assertEquals(10000.0d, canon.subtotalSuperficie());
        assertEquals("Descuento por montaje (90 min)", canon.conceptoAjuste());
        assertEquals(10f, canon.costoUnitarioAjuste());
        assertEquals(-900.0d, canon.ajuste());
        assertEquals(9100.0d, canon.canon());
        assertEquals(9100.0d, reporte.canonTotal());
    }

    @Test
    void canonPorFestivalUsaLosCostosDeCadaFestivalOrdenaPorInicioYSumaElTotal() {
        FoodTruck foodTruck = foodTruckCanon(true);
        Festival vegano = festivalConCosto("Festival Vegano", 30, 40, 600f, 10f, 2500f);
        Festival cerveza = festivalConCosto("Festival de la Cerveza", -5, 10, 500f, 10f, 2000f);
        Festival pizza = festivalConCosto("Festival de la Pizza", -60, -40, 500f, 10f, 2000f);
        // el repositorio los devuelve por fecha de inicio descendente
        prepararCanon(foodTruck, List.of(vegano, cerveza, pizza));

        ReporteCanonDTO reporte = service.canonPorFestival(RESPONSABLE_CANON);

        assertEquals(
                List.of("Festival de la Pizza", "Festival de la Cerveza", "Festival Vegano"),
                reporte.canones().stream().map(CanonUnidadDTO::festival).toList());
        assertEquals(
                List.of("Finalizado", "En curso", "Próximo"),
                reporte.canones().stream().map(CanonUnidadDTO::estado).toList());
        assertEquals(
                List.of(8000.0d, 8000.0d, 9700.0d),
                reporte.canones().stream().map(CanonUnidadDTO::canon).toList());
        assertEquals(7200.0d, reporte.canones().get(2).subtotalSuperficie());
        assertEquals(2500.0d, reporte.canones().get(2).ajuste());
        assertEquals(25700.0d, reporte.canonTotal());
    }

    @Test
    void canonPorFestivalDeUnidadSinFestivalesDevuelveListaVaciaYTotalCero() {
        prepararCanon(foodTruckCanon(true), List.of());

        ReporteCanonDTO reporte = service.canonPorFestival(RESPONSABLE_CANON);

        assertEquals("Ruedas de Fuego", reporte.nombreComercial());
        assertTrue(reporte.canones().isEmpty());
        assertEquals(0.0d, reporte.canonTotal());
    }

    @Test
    void canonPorFestivalSinUnidadLanzaExcepcion() {
        when(unidadDeVentaRepository.findByResponsableUsername(RESPONSABLE_CANON)).thenReturn(List.of());
        when(personalRepository.findByUserUsernameAndBajaFalse(RESPONSABLE_CANON)).thenReturn(List.of());

        assertThrows(ReporteResponsableException.class,
                () -> service.canonPorFestival(RESPONSABLE_CANON));
        verifyNoInteractions(festivalRepository);
    }

    private void prepararCanon(UnidadDeVenta unidad, List<Festival> festivales) {
        when(unidadDeVentaRepository.findByResponsableUsername(RESPONSABLE_CANON)).thenReturn(List.of(unidad));
        when(festivalRepository.findByUnidadId(unidad.getId())).thenReturn(festivales);
    }

    private FoodTruck foodTruckCanon(boolean requiereElectricidad) {
        FoodTruck foodTruck = new FoodTruck();
        foodTruck.setId(1);
        foodTruck.setCodigo("FTK0000001");
        foodTruck.setNombreComercial("Ruedas de Fuego");
        foodTruck.setSuperficieM2(12f);
        foodTruck.setRequiereElectricidad(requiereElectricidad);
        return foodTruck;
    }

    private Festival festivalConCosto(String nombre, int diasInicio, int diasFin,
                                      float costoSuperficie, float costoMontaje, float plusElectricidad) {
        Costo costo = new Costo();
        costo.setCostoSuperficie(costoSuperficie);
        costo.setCostoMontaje(costoMontaje);
        costo.setPlusElectricidad(plusElectricidad);

        Festival festival = new Festival();
        festival.setNombre(nombre);
        festival.setFechaInicio(LocalDate.now().plusDays(diasInicio));
        festival.setFechaFin(LocalDate.now().plusDays(diasFin));
        festival.setCosto(costo);
        return festival;
    }
}
