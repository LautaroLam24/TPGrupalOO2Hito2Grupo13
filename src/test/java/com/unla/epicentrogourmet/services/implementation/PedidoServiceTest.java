package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.AltaPedidoForm;
import com.unla.epicentrogourmet.dto.ItemPedidoForm;
import com.unla.epicentrogourmet.entities.Cajero;
import com.unla.epicentrogourmet.entities.EmpleadoCocina;
import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.entities.FoodTruck;
import com.unla.epicentrogourmet.entities.Pedido;
import com.unla.epicentrogourmet.entities.Plato;
import com.unla.epicentrogourmet.entities.Role;
import com.unla.epicentrogourmet.entities.RoleName;
import com.unla.epicentrogourmet.entities.UnidadDeVenta;
import com.unla.epicentrogourmet.entities.User;
import com.unla.epicentrogourmet.exceptions.AltaPedidoException;
import com.unla.epicentrogourmet.repositories.IFestivalRepository;
import com.unla.epicentrogourmet.repositories.IPedidoRepository;
import com.unla.epicentrogourmet.repositories.IPersonalRepository;
import com.unla.epicentrogourmet.repositories.IUnidadDeVentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class PedidoServiceTest {

    private static final String USERNAME = "cajero@epicentro.com";

    @Mock
    private IPersonalRepository personalRepository;

    @Mock
    private IUnidadDeVentaRepository unidadDeVentaRepository;

    @Mock
    private IFestivalRepository festivalRepository;

    @Mock
    private IPedidoRepository pedidoRepository;

    private PedidoService service;
    private Cajero cajero;
    private FoodTruck unidad;
    private Plato pizza;
    private Plato bebida;

    @BeforeEach
    void setUp() {
        service = new PedidoService(
                personalRepository,
                unidadDeVentaRepository,
                festivalRepository,
                pedidoRepository);

        cajero = cajeroActivo(USERNAME);
        unidad = new FoodTruck();
        unidad.setId(10);
        unidad.setNombreComercial("Ruedas de Fuego");
        unidad.getStaff().add(cajero);

        pizza = plato(1, "Pizza", 6000f, false);
        bebida = plato(2, "Bebida", 3500f, false);
        unidad.getPlatos().add(pizza);
        unidad.getPlatos().add(bebida);
    }

    @Test
    void registraPedidoConFechaPreciosTotalesYAsociacionAUnidad() {
        autorizarConFestivales(List.of(festivalActivo("Festival vigente")));
        when(pedidoRepository.saveAndFlush(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido pedido = invocation.getArgument(0);
            pedido.setId(25);
            return pedido;
        });
        when(unidadDeVentaRepository.saveAndFlush(unidad)).thenReturn(unidad);

        Pedido pedido = service.registrar(USERNAME, formulario(
                new ItemPedidoForm(pizza.getId(), 2),
                new ItemPedidoForm(bebida.getId(), 1)));

        assertEquals(25, pedido.getId());
        assertEquals(LocalDate.now(), pedido.getFecha());
        assertEquals(15500f, pedido.getTotal(), 0.001f);
        assertEquals(2, pedido.getItems().size());
        assertEquals(15500f,
                pedido.getItems().stream().map(item -> item.calcularSubtotal()).reduce(0f, Float::sum),
                0.001f);
        assertTrue(unidad.getPedidos().contains(pedido));

        InOrder persistencia = inOrder(pedidoRepository, unidadDeVentaRepository);
        persistencia.verify(pedidoRepository).saveAndFlush(pedido);
        persistencia.verify(unidadDeVentaRepository).saveAndFlush(unidad);
    }

    @Test
    void prepararAltaSoloExponePlatosActivos() {
        Plato inactivo = plato(3, "Archivado", 1000f, true);
        unidad.getPlatos().add(inactivo);
        autorizarConFestivales(List.of(festivalActivo("Festival vigente")));

        PedidoService.ContextoAltaPedido contexto = service.prepararAlta(USERNAME);

        assertEquals("Ruedas de Fuego", contexto.unidad());
        assertEquals("Festival vigente", contexto.festival());
        assertEquals(List.of("Bebida", "Pizza"),
                contexto.platos().stream().map(Plato::getNombre).toList());
    }

    @Test
    void rechazaPedidoSinItems() {
        autorizarConFestivales(List.of(festivalActivo("Festival vigente")));

        AltaPedidoException error = assertThrows(
                AltaPedidoException.class,
                () -> service.registrar(USERNAME, new AltaPedidoForm()));

        assertTrue(error.getMessage().contains("al menos un ítem"));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void rechazaPedidoSinCantidadesSeleccionadas() {
        autorizarConFestivales(List.of(festivalActivo("Festival vigente")));

        AltaPedidoException error = assertThrows(
                AltaPedidoException.class,
                () -> service.registrar(USERNAME, formulario(new ItemPedidoForm(pizza.getId(), null))));

        assertTrue(error.getMessage().contains("al menos un ítem"));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void rechazaCantidadNoPositiva() {
        autorizarConFestivales(List.of(festivalActivo("Festival vigente")));

        AltaPedidoException error = assertThrows(
                AltaPedidoException.class,
                () -> service.registrar(USERNAME, formulario(new ItemPedidoForm(pizza.getId(), 0))));

        assertTrue(error.getMessage().contains("mayor que cero"));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void rechazaPlatosDuplicados() {
        autorizarConFestivales(List.of(festivalActivo("Festival vigente")));

        AltaPedidoException error = assertThrows(
                AltaPedidoException.class,
                () -> service.registrar(USERNAME, formulario(
                        new ItemPedidoForm(pizza.getId(), 1),
                        new ItemPedidoForm(pizza.getId(), 2))));

        assertTrue(error.getMessage().contains("más de una vez"));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void rechazaPlatoInexistente() {
        autorizarConFestivales(List.of(festivalActivo("Festival vigente")));

        AltaPedidoException error = assertThrows(
                AltaPedidoException.class,
                () -> service.registrar(USERNAME, formulario(new ItemPedidoForm(999, 1))));

        assertTrue(error.getMessage().contains("no existe o no pertenece"));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void rechazaPlatoExistenteDeOtraUnidad() {
        Plato ajeno = plato(99, "Plato ajeno", 4000f, false);
        autorizarConFestivales(List.of(festivalActivo("Festival vigente")));

        AltaPedidoException error = assertThrows(
                AltaPedidoException.class,
                () -> service.registrar(USERNAME, formulario(new ItemPedidoForm(ajeno.getId(), 1))));

        assertTrue(error.getMessage().contains("no existe o no pertenece"));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void rechazaPlatoInactivo() {
        Plato inactivo = plato(3, "Archivado", 1000f, true);
        unidad.getPlatos().add(inactivo);
        autorizarConFestivales(List.of(festivalActivo("Festival vigente")));

        AltaPedidoException error = assertThrows(
                AltaPedidoException.class,
                () -> service.registrar(USERNAME, formulario(new ItemPedidoForm(inactivo.getId(), 1))));

        assertTrue(error.getMessage().contains("no se encuentra activo"));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void rechazaUnidadSinFestivalAsociado() {
        autorizarConFestivales(List.of());

        AltaPedidoException error = assertThrows(
                AltaPedidoException.class,
                () -> service.registrar(USERNAME, formulario(new ItemPedidoForm(pizza.getId(), 1))));

        assertTrue(error.getMessage().contains("no tiene festivales asociados"));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void rechazaFestivalAsociadoInactivo() {
        Festival inactivo = festivalActivo("Festival finalizado");
        inactivo.setFechaInicio(LocalDate.now().minusDays(10));
        inactivo.setFechaFin(LocalDate.now().minusDays(1));
        autorizarConFestivales(List.of(inactivo));

        AltaPedidoException error = assertThrows(
                AltaPedidoException.class,
                () -> service.registrar(USERNAME, formulario(new ItemPedidoForm(pizza.getId(), 1))));

        assertTrue(error.getMessage().contains("no tiene un festival activo"));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void rechazaFestivalesActivosSimultaneos() {
        autorizarConFestivales(List.of(
                festivalActivo("Festival uno"),
                festivalActivo("Festival dos")));

        AltaPedidoException error = assertThrows(
                AltaPedidoException.class,
                () -> service.registrar(USERNAME, formulario(new ItemPedidoForm(pizza.getId(), 1))));

        assertTrue(error.getMessage().contains("varios festivales activos"));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void rechazaEmpleadoQueNoEsCajero() {
        EmpleadoCocina cocinero = new EmpleadoCocina();
        cocinero.setUser(usuarioActivo(USERNAME));
        when(personalRepository.findByUserUsernameAndBajaFalse(USERNAME)).thenReturn(List.of(cocinero));

        AltaPedidoException error = assertThrows(
                AltaPedidoException.class,
                () -> service.prepararAlta(USERNAME));

        assertTrue(error.getMessage().contains("no es un cajero"));
        verifyNoInteractions(festivalRepository, pedidoRepository);
    }

    @Test
    void rechazaCajeroDadoDeBaja() {
        when(personalRepository.findByUserUsernameAndBajaFalse(USERNAME)).thenReturn(List.of());

        AltaPedidoException error = assertThrows(
                AltaPedidoException.class,
                () -> service.prepararAlta(USERNAME));

        assertTrue(error.getMessage().contains("empleado activo"));
        verifyNoInteractions(unidadDeVentaRepository, festivalRepository, pedidoRepository);
    }

    @Test
    void rechazaCajeroConUsuarioDeshabilitado() {
        cajero.getUser().setEnabled(false);
        when(personalRepository.findByUserUsernameAndBajaFalse(USERNAME)).thenReturn(List.of(cajero));

        AltaPedidoException error = assertThrows(
                AltaPedidoException.class,
                () -> service.prepararAlta(USERNAME));

        assertTrue(error.getMessage().contains("no se encuentra activo o autorizado"));
        verifyNoInteractions(unidadDeVentaRepository, festivalRepository, pedidoRepository);
    }

    @Test
    void rechazaCajeroQueNoPerteneceAUnaUnidad() {
        when(personalRepository.findByUserUsernameAndBajaFalse(USERNAME)).thenReturn(List.of(cajero));
        when(unidadDeVentaRepository.findByPersonal(cajero)).thenReturn(Optional.empty());

        AltaPedidoException error = assertThrows(
                AltaPedidoException.class,
                () -> service.prepararAlta(USERNAME));

        assertTrue(error.getMessage().contains("no pertenece a una unidad"));
        verifyNoInteractions(festivalRepository, pedidoRepository);
    }

    @Test
    void propagaFallaAlAsociarLaUnidadParaQueLaTransaccionHagaRollback() {
        autorizarConFestivales(List.of(festivalActivo("Festival vigente")));
        when(pedidoRepository.saveAndFlush(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        IllegalStateException falla = new IllegalStateException("falló la asociación");
        when(unidadDeVentaRepository.saveAndFlush(unidad)).thenThrow(falla);

        IllegalStateException resultado = assertThrows(
                IllegalStateException.class,
                () -> service.registrar(USERNAME, formulario(new ItemPedidoForm(pizza.getId(), 1))));

        assertSame(falla, resultado);
        verify(pedidoRepository).saveAndFlush(any(Pedido.class));
    }

    private void autorizarConFestivales(List<Festival> festivales) {
        when(personalRepository.findByUserUsernameAndBajaFalse(USERNAME)).thenReturn(List.of(cajero));
        when(unidadDeVentaRepository.findByPersonal(cajero)).thenReturn(Optional.of(unidad));
        when(festivalRepository.findByUnidadId(unidad.getId())).thenReturn(festivales);
    }

    private AltaPedidoForm formulario(ItemPedidoForm... items) {
        return new AltaPedidoForm(List.of(items));
    }

    private Cajero cajeroActivo(String username) {
        Cajero resultado = new Cajero();
        resultado.setUser(usuarioActivo(username));
        resultado.setBaja(false);
        return resultado;
    }

    private User usuarioActivo(String username) {
        return new User(username, "hash", true, new Role(RoleName.ROLE_EMPLEADO));
    }

    private Festival festivalActivo(String nombre) {
        Festival festival = new Festival();
        festival.setNombre(nombre);
        festival.setFechaInicio(LocalDate.now().minusDays(1));
        festival.setFechaFin(LocalDate.now().plusDays(1));
        return festival;
    }

    private Plato plato(int id, String nombre, float precio, boolean baja) {
        Plato plato = new Plato();
        plato.setId(id);
        plato.setNombre(nombre);
        plato.setPrecioVenta(precio);
        plato.setBaja(baja);
        return plato;
    }
}
