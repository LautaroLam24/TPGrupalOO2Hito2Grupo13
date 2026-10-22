package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.dto.AltaPedidoForm;
import com.unla.epicentrogourmet.entities.Pedido;
import com.unla.epicentrogourmet.entities.Plato;
import com.unla.epicentrogourmet.exceptions.AltaPedidoException;
import com.unla.epicentrogourmet.services.implementation.PedidoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    private static final String USERNAME = "cajero@epicentro.com";

    @Mock
    private PedidoService pedidoService;

    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;
    private Authentication authentication;
    private Plato plato;
    private PedidoService.ContextoAltaPedido contexto;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new PedidoController(pedidoService))
                .setValidator(validator)
                .build();
        authentication = new TestingAuthenticationToken(USERNAME, null, "ROLE_EMPLEADO");

        plato = new Plato();
        plato.setId(7);
        plato.setNombre("Pizza");
        plato.setPrecioVenta(6000f);
        contexto = new PedidoService.ContextoAltaPedido(
                "Ruedas de Fuego",
                "Festival vigente",
                List.of(plato));
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void getMuestraSoloElContextoAutorizadoEInicializaLosItems() throws Exception {
        when(pedidoService.prepararAlta(USERNAME)).thenReturn(contexto);

        mockMvc.perform(get("/empleado/pedidos/nuevo").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("pedido/alta-pedido"))
                .andExpect(model().attribute("unidad", "Ruedas de Fuego"))
                .andExpect(model().attribute("festival", "Festival vigente"))
                .andExpect(model().attribute("platos", List.of(plato)))
                .andExpect(model().attributeExists(PedidoController.FORM_ATTRIBUTE));

        verify(pedidoService).prepararAlta(USERNAME);
    }

    @Test
    void postValidoUsaElUsuarioAutenticadoYRedirige() throws Exception {
        Pedido pedido = new Pedido();
        pedido.setId(25);
        pedido.setTotal(12000f);
        when(pedidoService.registrar(eq(USERNAME), any(AltaPedidoForm.class))).thenReturn(pedido);

        mockMvc.perform(post("/empleado/pedidos/nuevo")
                        .principal(authentication)
                        .param("items[0].platoId", "7")
                        .param("items[0].cantidad", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/empleado/pedidos/nuevo"))
                .andExpect(flash().attribute("mensajeExito", containsString("pedido #25")));

        ArgumentCaptor<AltaPedidoForm> captor = ArgumentCaptor.forClass(AltaPedidoForm.class);
        verify(pedidoService).registrar(eq(USERNAME), captor.capture());
        assertEquals(7, captor.getValue().getItems().get(0).getPlatoId());
        assertEquals(2, captor.getValue().getItems().get(0).getCantidad());
    }

    @Test
    void cantidadInvalidaVuelveAlFormularioSinInvocarElAlta() throws Exception {
        when(pedidoService.prepararAlta(USERNAME)).thenReturn(contexto);

        mockMvc.perform(post("/empleado/pedidos/nuevo")
                        .principal(authentication)
                        .param("items[0].platoId", "7")
                        .param("items[0].cantidad", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("pedido/alta-pedido"))
                .andExpect(model().attributeHasFieldErrors(
                        PedidoController.FORM_ATTRIBUTE,
                        "items[0].cantidad"));

        verify(pedidoService, never()).registrar(any(), any());
    }

    @Test
    void postVacioSeRechazaYReconstruyeLasFilasConDatosConfiables() throws Exception {
        when(pedidoService.prepararAlta(USERNAME)).thenReturn(contexto);

        mockMvc.perform(post("/empleado/pedidos/nuevo").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(view().name("pedido/alta-pedido"))
                .andExpect(model().attributeHasFieldErrors(
                        PedidoController.FORM_ATTRIBUTE,
                        "items"))
                .andExpect(model().attribute(
                        PedidoController.FORM_ATTRIBUTE,
                        hasProperty("items", hasSize(1))));

        verify(pedidoService, never()).registrar(any(), any());
    }

    @Test
    void errorDeNegocioSePresentaEnElFormulario() throws Exception {
        String mensaje = "La unidad no tiene un festival activo en la fecha actual.";
        doThrow(new AltaPedidoException(mensaje))
                .when(pedidoService).registrar(eq(USERNAME), any(AltaPedidoForm.class));
        when(pedidoService.prepararAlta(USERNAME)).thenReturn(contexto);

        mockMvc.perform(post("/empleado/pedidos/nuevo")
                        .principal(authentication)
                        .param("items[0].platoId", "7")
                        .param("items[0].cantidad", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("pedido/alta-pedido"))
                .andExpect(model().attributeHasErrors(PedidoController.FORM_ATTRIBUTE));
    }
}
