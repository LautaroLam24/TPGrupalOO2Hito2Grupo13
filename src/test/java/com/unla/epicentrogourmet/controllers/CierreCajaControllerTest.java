package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.dto.CierreCajaDTO;
import com.unla.epicentrogourmet.dto.PedidoCierreDTO;
import com.unla.epicentrogourmet.exceptions.CierreCajaException;
import com.unla.epicentrogourmet.session.SeguimientoPedidosSesion;
import com.unla.epicentrogourmet.services.implementation.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class CierreCajaControllerTest {

    private static final String USERNAME = "cajero@epicentro.com";
    private static final String OTRO_USERNAME = "otro@epicentro.com";

    @Mock
    private PedidoService pedidoService;

    private SeguimientoPedidosSesion seguimiento;
    private CierreCajaController controller;
    private MockMvc mockMvc;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        seguimiento = new SeguimientoPedidosSesion();
        controller = new CierreCajaController(pedidoService, seguimiento);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        authentication = new TestingAuthenticationToken(USERNAME, null, "ROLE_EMPLEADO");
    }

    @Test
    void getUsaIdsDeSesionYExponeElResumen() throws Exception {
        MockHttpSession session = sessionConIds(10, 20);
        CierreCajaDTO cierre = cierre(10, 20);
        when(pedidoService.cierreCaja(USERNAME, Set.of(10, 20))).thenReturn(cierre);

        mockMvc.perform(get("/empleado/caja/cierre")
                        .session(session)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("pedido/cierre-caja"))
                .andExpect(model().attribute("cierre", cierre));

        assertEquals(Set.of(10, 20), seguimiento.obtenerIds(session, USERNAME));
    }

    @Test
    void getVacioValidaContextoMedianteElServiceSinModificarSesion() throws Exception {
        MockHttpSession session = new MockHttpSession();
        CierreCajaDTO cierre = cierreVacio();
        when(pedidoService.cierreCaja(USERNAME, Set.of())).thenReturn(cierre);

        mockMvc.perform(get("/empleado/caja/cierre")
                        .session(session)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(model().attribute("cierre", cierre));

        assertTrue(seguimiento.obtenerIds(session, USERNAME).isEmpty());
    }

    @Test
    void getMuestraFlashConfirmadoSoloAlUsernamePropietario() throws Exception {
        MockHttpSession session = new MockHttpSession();
        CierreCajaDTO cierre = cierre(10);

        mockMvc.perform(get("/empleado/caja/cierre")
                        .session(session)
                        .principal(authentication)
                        .flashAttr(CierreCajaController.CIERRE_CONFIRMADO, cierre)
                        .flashAttr(CierreCajaController.CIERRE_CONFIRMADO_OWNER, USERNAME))
                .andExpect(status().isOk())
                .andExpect(model().attribute("cierre", cierre))
                .andExpect(model().attribute("confirmado", true));

        verifyNoInteractions(pedidoService);
    }

    @Test
    void getDescartaFlashDeOtroUsuarioYReconstruyeSuPropioResumen() throws Exception {
        MockHttpSession session = sessionConIds(20);
        CierreCajaDTO ajeno = cierre(10);
        CierreCajaDTO propio = cierre(20);
        when(pedidoService.cierreCaja(USERNAME, Set.of(20))).thenReturn(propio);

        mockMvc.perform(get("/empleado/caja/cierre")
                        .session(session)
                        .principal(authentication)
                        .flashAttr(CierreCajaController.CIERRE_CONFIRMADO, ajeno)
                        .flashAttr(CierreCajaController.CIERRE_CONFIRMADO_OWNER, OTRO_USERNAME))
                .andExpect(status().isOk())
                .andExpect(model().attribute("cierre", propio))
                .andExpect(model().attributeDoesNotExist("confirmado"));
    }

    @Test
    void postValidoRecalculaLimpiaYRedirigeConFlash() throws Exception {
        MockHttpSession session = sessionConIds(10, 20);
        CierreCajaDTO cierre = cierre(10, 20);
        when(pedidoService.cierreCaja(USERNAME, Set.of(10, 20))).thenReturn(cierre);

        mockMvc.perform(post("/empleado/caja/cierre")
                        .session(session)
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/empleado/caja/cierre"))
                .andExpect(flash().attribute(CierreCajaController.CIERRE_CONFIRMADO, cierre))
                .andExpect(flash().attribute(
                        CierreCajaController.CIERRE_CONFIRMADO_OWNER, USERNAME));

        assertTrue(seguimiento.obtenerIds(session, USERNAME).isEmpty());
    }

    @Test
    void postVacioNoConfirmaNiInvocaElService() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/empleado/caja/cierre")
                        .session(session)
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/empleado/caja/cierre"))
                .andExpect(flash().attributeExists("mensajeInformativo"));

        verifyNoInteractions(pedidoService);
    }

    @Test
    void errorDelServiceConservaTodosLosIds() throws Exception {
        MockHttpSession session = sessionConIds(10, 99);
        doThrow(new CierreCajaException("No fue posible validar los pedidos pendientes del cierre."))
                .when(pedidoService).cierreCaja(USERNAME, Set.of(10, 99));

        mockMvc.perform(post("/empleado/caja/cierre")
                        .session(session)
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("mensajeError"));

        assertEquals(Set.of(10, 99), seguimiento.obtenerIds(session, USERNAME));
    }

    @Test
    void segundoPostNoConfirmaNuevamenteElConjuntoConsumido() throws Exception {
        MockHttpSession session = sessionConIds(10);
        when(pedidoService.cierreCaja(USERNAME, Set.of(10))).thenReturn(cierre(10));

        confirmar(session);
        confirmar(session);

        verify(pedidoService, times(1)).cierreCaja(USERNAME, Set.of(10));
        assertTrue(seguimiento.obtenerIds(session, USERNAME).isEmpty());
    }

    @Test
    void pedidoNuevoDespuesDelCierreFormaOtroCiclo() throws Exception {
        MockHttpSession session = sessionConIds(10);
        when(pedidoService.cierreCaja(USERNAME, Set.of(10))).thenReturn(cierre(10));
        when(pedidoService.cierreCaja(USERNAME, Set.of(30))).thenReturn(cierre(30));

        confirmar(session);
        seguimiento.registrar(session, USERNAME, 30);
        confirmar(session);

        verify(pedidoService).cierreCaja(USERNAME, Set.of(30));
        assertTrue(seguimiento.obtenerIds(session, USERNAME).isEmpty());
    }

    @Test
    void parametrosManipuladosNoSustituyenIdsNiIdentidadDeSesion() throws Exception {
        MockHttpSession session = sessionConIds(10);
        when(pedidoService.cierreCaja(USERNAME, Set.of(10))).thenReturn(cierre(10));

        mockMvc.perform(post("/empleado/caja/cierre")
                        .session(session)
                        .principal(authentication)
                        .param("pedidoIds", "999")
                        .param("username", OTRO_USERNAME)
                        .param("total", "1"))
                .andExpect(status().is3xxRedirection());

        verify(pedidoService).cierreCaja(USERNAME, Set.of(10));
        verify(pedidoService, never()).cierreCaja(eq(OTRO_USERNAME), eq(Set.of(999)));
    }

    @Test
    void dosConfirmacionesConcurrentesConsumenUnaSolaVezEnLaMismaJvm() throws Exception {
        MockHttpSession session = sessionConIds(10);
        when(pedidoService.cierreCaja(USERNAME, Set.of(10))).thenReturn(cierre(10));
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<String> primera = executor.submit(() -> confirmarDirecto(session, inicio));
            Future<String> segunda = executor.submit(() -> confirmarDirecto(session, inicio));
            inicio.countDown();

            assertEquals("redirect:/empleado/caja/cierre", primera.get());
            assertEquals("redirect:/empleado/caja/cierre", segunda.get());
        } finally {
            executor.shutdownNow();
        }

        verify(pedidoService, times(1)).cierreCaja(USERNAME, Set.of(10));
        assertTrue(seguimiento.obtenerIds(session, USERNAME).isEmpty());
    }

    private String confirmarDirecto(MockHttpSession session, CountDownLatch inicio) throws Exception {
        inicio.await();
        return controller.confirmar(
                authentication, session, new RedirectAttributesModelMap());
    }

    private void confirmar(MockHttpSession session) throws Exception {
        mockMvc.perform(post("/empleado/caja/cierre")
                        .session(session)
                        .principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/empleado/caja/cierre"));
    }

    private MockHttpSession sessionConIds(int... ids) {
        MockHttpSession session = new MockHttpSession();
        for (int id : ids) {
            seguimiento.registrar(session, USERNAME, id);
        }
        return session;
    }

    private CierreCajaDTO cierre(int... ids) {
        List<PedidoCierreDTO> pedidos = java.util.Arrays.stream(ids)
                .mapToObj(id -> new PedidoCierreDTO(id, LocalDate.now(), 1000f * id))
                .toList();
        float total = 0f;
        for (PedidoCierreDTO pedido : pedidos) {
            total += pedido.importe();
        }
        return new CierreCajaDTO(
                "Ruedas de Fuego",
                "Ana Pérez",
                LocalDate.now(),
                pedidos.size(),
                total,
                pedidos);
    }

    private CierreCajaDTO cierreVacio() {
        return new CierreCajaDTO(
                "Ruedas de Fuego", "Ana Pérez", LocalDate.now(), 0, 0f, List.of());
    }
}
