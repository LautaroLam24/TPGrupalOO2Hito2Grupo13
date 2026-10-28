package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.configuration.SecurityConfiguration;
import com.unla.epicentrogourmet.dto.FiltroPlatoDTO;
import com.unla.epicentrogourmet.dto.FiltroPlatoDTO.EstadoPlato;
import com.unla.epicentrogourmet.dto.PlatoForm;
import com.unla.epicentrogourmet.exceptions.GestionPlatoException;
import com.unla.epicentrogourmet.services.IFestivalService;
import com.unla.epicentrogourmet.services.implementation.PlatoService;
import com.unla.epicentrogourmet.services.implementation.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({PlatoController.class, HomeController.class})
@Import(SecurityConfiguration.class)
class PlatoControllerTest {

    private static final String USERNAME = "responsable@epicentro.com";
    private static final int PLATO_ID = 7;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlatoService platoService;

    @MockBean
    private IFestivalService festivalService;

    @MockBean
    private UserService userService;

    @Test
    void anonimoEsRedirigidoAlLogin() throws Exception {
        mockMvc.perform(get("/responsable/platos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(platoService);
    }

    @Test
    void empleadoNoPuedeAcceder() throws Exception {
        mockMvc.perform(get("/responsable/platos")
                        .session(sesionAutenticada(USERNAME, "ROLE_EMPLEADO")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(platoService);
    }

    @Test
    void administradorNoPuedeAcceder() throws Exception {
        mockMvc.perform(get("/responsable/platos/nuevo")
                        .session(sesionAutenticada(USERNAME, "ROLE_ADMIN")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(platoService);
    }

    @Test
    void listadoUsaUsernameAutenticadoYMuestraActivosEInactivos() throws Exception {
        PlatoService.ContextoGestionPlatos contexto = new PlatoService.ContextoGestionPlatos(
                "Ruedas de Fuego",
                List.of(
                        new PlatoService.PlatoResumen(7, "Pizza", 1500f, 900f, true),
                        new PlatoService.PlatoResumen(8, "Empanada", 900f, 500f, false)));
        when(platoService.filtrar(
                org.mockito.ArgumentMatchers.eq(USERNAME),
                any(FiltroPlatoDTO.class))).thenReturn(contexto);

        mockMvc.perform(get("/responsable/platos").session(sesionResponsable()))
                .andExpect(status().isOk())
                .andExpect(view().name("responsable/platos/index"))
                .andExpect(model().attributeExists("filtros"))
                .andExpect(model().attribute("unidad", "Ruedas de Fuego"))
                .andExpect(model().attribute("platos", contexto.platos()))
                .andExpect(content().string(containsString("Pizza")))
                .andExpect(content().string(containsString("Empanada")))
                .andExpect(content().string(containsString("Activo")))
                .andExpect(content().string(containsString("Dado de baja")))
                .andExpect(content().string(containsString("/responsable/platos/7/editar")))
                .andExpect(content().string(containsString("/responsable/platos/7/baja")))
                .andExpect(content().string(containsString("href=\"/responsable/platos\"")));

        ArgumentCaptor<FiltroPlatoDTO> captor = ArgumentCaptor.forClass(FiltroPlatoDTO.class);
        verify(platoService).filtrar(
                org.mockito.ArgumentMatchers.eq(USERNAME), captor.capture());
        assertEquals(EstadoPlato.TODOS, captor.getValue().getEstado());
    }

    @Test
    void errorDeListadoSeMuestraComoMensajeFuncional() throws Exception {
        String mensaje = "No se encontró una unidad de venta para el responsable autenticado.";
        when(platoService.filtrar(
                org.mockito.ArgumentMatchers.eq(USERNAME),
                any(FiltroPlatoDTO.class)))
                .thenThrow(new GestionPlatoException(mensaje));

        mockMvc.perform(get("/responsable/platos").session(sesionResponsable()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", mensaje))
                .andExpect(content().string(containsString(mensaje)));
    }

    @Test
    void filtrosCombinadosSeEnlazanSeConservanYUsanUsernameAutenticado() throws Exception {
        PlatoService.ContextoGestionPlatos contexto = contextoGestion(List.of(
                new PlatoService.PlatoResumen(7, "Pizza veggie", 2500f, 1200f, true)));
        when(platoService.filtrar(
                org.mockito.ArgumentMatchers.eq(USERNAME),
                any(FiltroPlatoDTO.class))).thenReturn(contexto);

        mockMvc.perform(get("/responsable/platos")
                        .session(sesionResponsable())
                        .param("nombre", "pizza")
                        .param("precioVentaMinimo", "1000")
                        .param("precioVentaMaximo", "3000")
                        .param("costoProduccionMinimo", "400")
                        .param("costoProduccionMaximo", "1500")
                        .param("estado", "ACTIVOS"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("filtros", allOf(
                        hasProperty("nombre", is("pizza")),
                        hasProperty("precioVentaMinimo", is(1000f)),
                        hasProperty("precioVentaMaximo", is(3000f)),
                        hasProperty("costoProduccionMinimo", is(400f)),
                        hasProperty("costoProduccionMaximo", is(1500f)),
                        hasProperty("estado", is(EstadoPlato.ACTIVOS)))))
                .andExpect(content().string(containsString("value=\"pizza\"")))
                .andExpect(content().string(containsString("value=\"ACTIVOS\" selected=\"selected\"")))
                .andExpect(content().string(containsString("Pizza veggie")));

        ArgumentCaptor<FiltroPlatoDTO> captor = ArgumentCaptor.forClass(FiltroPlatoDTO.class);
        verify(platoService).filtrar(
                org.mockito.ArgumentMatchers.eq(USERNAME), captor.capture());
        assertEquals(1500f, captor.getValue().getCostoProduccionMaximo());
    }

    @Test
    void estadoInactivosSeEnlazaYDelega() throws Exception {
        when(platoService.filtrar(
                org.mockito.ArgumentMatchers.eq(USERNAME),
                any(FiltroPlatoDTO.class))).thenReturn(contextoGestion(List.of()));

        mockMvc.perform(get("/responsable/platos")
                        .session(sesionResponsable())
                        .param("estado", "INACTIVOS"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("filtros",
                        hasProperty("estado", is(EstadoPlato.INACTIVOS))));

        ArgumentCaptor<FiltroPlatoDTO> captor = ArgumentCaptor.forClass(FiltroPlatoDTO.class);
        verify(platoService).filtrar(
                org.mockito.ArgumentMatchers.eq(USERNAME), captor.capture());
        assertEquals(EstadoPlato.INACTIVOS, captor.getValue().getEstado());
    }

    @Test
    void resultadoVacioConFiltrosMuestraMensajeYEnlaceLimpiar() throws Exception {
        when(platoService.filtrar(
                org.mockito.ArgumentMatchers.eq(USERNAME),
                any(FiltroPlatoDTO.class))).thenReturn(contextoGestion(List.of()));

        mockMvc.perform(get("/responsable/platos")
                        .session(sesionResponsable())
                        .param("nombre", "sushi"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "No hay Platos que coincidan con los filtros seleccionados.")))
                .andExpect(content().string(containsString("href=\"/responsable/platos\"")))
                .andExpect(content().string(containsString("Limpiar")));
    }

    @Test
    void errorNumericoNoEjecutaBusquedaNiMuestraResultados() throws Exception {
        mockMvc.perform(get("/responsable/platos")
                        .session(sesionResponsable())
                        .param("precioVentaMinimo", "no-es-numero"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("filtros", "precioVentaMinimo"))
                .andExpect(model().attribute("resultadosValidos", false))
                .andExpect(content().string(containsString(
                        "Ingrese un precio mínimo numérico mayor o igual que cero.")))
                .andExpect(content().string(not(containsString("<tbody>"))));

        verify(platoService, never()).filtrar(any(), any());
    }

    @Test
    void limiteNegativoNoEjecutaBusquedaYConservaElValorRechazado() throws Exception {
        mockMvc.perform(get("/responsable/platos")
                        .session(sesionResponsable())
                        .param("costoProduccionMaximo", "-1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("filtros", "costoProduccionMaximo"))
                .andExpect(content().string(containsString("value=\"-1.0\"")))
                .andExpect(content().string(containsString(
                        "Ingrese un costo máximo numérico mayor o igual que cero.")));

        verify(platoService, never()).filtrar(any(), any());
    }

    @Test
    void rangoInvertidoMuestraErrorFuncionalSinCatalogoCompleto() throws Exception {
        String mensaje = "El precio mínimo no puede ser mayor que el precio máximo.";
        when(platoService.filtrar(
                org.mockito.ArgumentMatchers.eq(USERNAME),
                any(FiltroPlatoDTO.class))).thenThrow(new GestionPlatoException(mensaje));

        mockMvc.perform(get("/responsable/platos")
                        .session(sesionResponsable())
                        .param("precioVentaMinimo", "200")
                        .param("precioVentaMaximo", "100"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", mensaje))
                .andExpect(model().attribute("resultadosValidos", false))
                .andExpect(content().string(containsString(mensaje)))
                .andExpect(content().string(not(containsString("<tbody>"))));
    }

    @Test
    void estadoDesconocidoGeneraErrorDeBindingSinConvertirseEnTodos() throws Exception {
        mockMvc.perform(get("/responsable/platos")
                        .session(sesionResponsable())
                        .param("estado", "ARCHIVADOS"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("filtros", "estado"))
                .andExpect(model().attribute("resultadosValidos", false))
                .andExpect(content().string(containsString("Seleccione un estado válido.")));

        verify(platoService, never()).filtrar(any(), any());
    }

    @Test
    void usernameYUnidadManipuladosNoAlteranElContextoAutorizado() throws Exception {
        when(platoService.filtrar(
                org.mockito.ArgumentMatchers.eq(USERNAME),
                any(FiltroPlatoDTO.class))).thenReturn(contextoGestion(List.of()));

        mockMvc.perform(get("/responsable/platos")
                        .session(sesionResponsable())
                        .param("username", "intruso@epicentro.com")
                        .param("unidadId", "999")
                        .param("estado", "ACTIVOS"))
                .andExpect(status().isOk());

        ArgumentCaptor<FiltroPlatoDTO> captor = ArgumentCaptor.forClass(FiltroPlatoDTO.class);
        verify(platoService).filtrar(
                org.mockito.ArgumentMatchers.eq(USERNAME), captor.capture());
        assertEquals(EstadoPlato.ACTIVOS, captor.getValue().getEstado());
        verify(platoService, never()).filtrar(
                org.mockito.ArgumentMatchers.eq("intruso@epicentro.com"), any());
    }

    @Test
    void formularioDeAltaNoExponeUnidadIdEstadoNiIdEditable() throws Exception {
        when(platoService.prepararAlta(USERNAME)).thenReturn(contextoAlta());

        mockMvc.perform(get("/responsable/platos/nuevo").session(sesionResponsable()))
                .andExpect(status().isOk())
                .andExpect(view().name("responsable/platos/form"))
                .andExpect(model().attribute("unidad", "Ruedas de Fuego"))
                .andExpect(model().attribute("edicion", false))
                .andExpect(model().attributeExists("platoForm"))
                .andExpect(content().string(containsString("Nuevo Plato")))
                .andExpect(content().string(not(containsString("name=\"unidadId\""))))
                .andExpect(content().string(not(containsString("name=\"baja\""))))
                .andExpect(content().string(not(containsString("name=\"id\""))));

        verify(platoService).prepararAlta(USERNAME);
    }

    @Test
    void altaExitosaUsaPrgUsernameAutenticadoEIgnoraParametrosAdicionales() throws Exception {
        when(platoService.alta(any(), any(PlatoForm.class)))
                .thenReturn(new PlatoService.PlatoResumen(7, "Pizza", 1500f, 900f, true));

        mockMvc.perform(post("/responsable/platos")
                        .session(sesionResponsable())
                        .param("nombre", "Pizza")
                        .param("precioVenta", "1500")
                        .param("costoProduccion", "900")
                        .param("unidadId", "999")
                        .param("baja", "true")
                        .param("id", "123")
                        .param("username", "intruso@epicentro.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/responsable/platos"))
                .andExpect(flash().attribute("mensajeExito", containsString("Pizza")));

        ArgumentCaptor<PlatoForm> captor = ArgumentCaptor.forClass(PlatoForm.class);
        verify(platoService).alta(org.mockito.ArgumentMatchers.eq(USERNAME), captor.capture());
        assertEquals("Pizza", captor.getValue().getNombre());
        assertEquals(1500f, captor.getValue().getPrecioVenta());
        assertEquals(900f, captor.getValue().getCostoProduccion());
        verify(platoService, never()).alta(org.mockito.ArgumentMatchers.eq("intruso@epicentro.com"), any());
    }

    @Test
    void erroresJakartaVuelvenAlFormularioSinInvocarAlta() throws Exception {
        when(platoService.prepararAlta(USERNAME)).thenReturn(contextoAlta());

        mockMvc.perform(post("/responsable/platos")
                        .session(sesionResponsable())
                        .param("nombre", " ")
                        .param("precioVenta", "0")
                        .param("costoProduccion", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("responsable/platos/form"))
                .andExpect(model().attributeHasFieldErrors(
                        "platoForm", "nombre", "precioVenta", "costoProduccion"))
                .andExpect(content().string(containsString("El nombre es obligatorio")));

        verify(platoService, never()).alta(any(), any());
    }

    @Test
    void errorDeNegocioEnAltaSeMuestraSinExponerDetallesInternos() throws Exception {
        String mensaje = "Ya existe un Plato activo con ese nombre en la unidad administrada.";
        when(platoService.alta(any(), any(PlatoForm.class)))
                .thenThrow(new GestionPlatoException(mensaje));
        when(platoService.prepararAlta(USERNAME)).thenReturn(contextoAlta());

        mockMvc.perform(post("/responsable/platos")
                        .session(sesionResponsable())
                        .param("nombre", "Pizza")
                        .param("precioVenta", "1500")
                        .param("costoProduccion", "900"))
                .andExpect(status().isOk())
                .andExpect(view().name("responsable/platos/form"))
                .andExpect(model().attributeHasErrors("platoForm"))
                .andExpect(content().string(containsString(mensaje)));
    }

    @Test
    void formularioDeEdicionCargaCamposActuales() throws Exception {
        PlatoForm form = form("Pizza", 1500f, 900f);
        when(platoService.prepararModificacion(USERNAME, PLATO_ID))
                .thenReturn(new PlatoService.ContextoFormularioPlato(
                        "Ruedas de Fuego", form, true));

        mockMvc.perform(get("/responsable/platos/7/editar").session(sesionResponsable()))
                .andExpect(status().isOk())
                .andExpect(view().name("responsable/platos/form"))
                .andExpect(model().attribute("platoId", PLATO_ID))
                .andExpect(model().attribute("platoForm", form))
                .andExpect(content().string(containsString("Modificar Plato")))
                .andExpect(content().string(containsString("value=\"Pizza\"")));

        verify(platoService).prepararModificacion(USERNAME, PLATO_ID);
    }

    @Test
    void modificacionExitosaUsaPostYPrg() throws Exception {
        when(platoService.modificar(any(), anyInt(), any(PlatoForm.class)))
                .thenReturn(new PlatoService.PlatoResumen(7, "Fugazzeta", 1800f, 950f, true));

        mockMvc.perform(post("/responsable/platos/7")
                        .session(sesionResponsable())
                        .param("nombre", "Fugazzeta")
                        .param("precioVenta", "1800")
                        .param("costoProduccion", "950")
                        .param("unidadId", "999")
                        .param("baja", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/responsable/platos"))
                .andExpect(flash().attribute("mensajeExito", containsString("se modificó")));

        ArgumentCaptor<PlatoForm> captor = ArgumentCaptor.forClass(PlatoForm.class);
        verify(platoService).modificar(
                org.mockito.ArgumentMatchers.eq(USERNAME),
                org.mockito.ArgumentMatchers.eq(PLATO_ID),
                captor.capture());
        assertEquals("Fugazzeta", captor.getValue().getNombre());
    }

    @Test
    void platoIdManipuladoEnEdicionSeRechazaConMensajeSeguro() throws Exception {
        String mensaje = "El Plato no existe o no pertenece a la unidad administrada.";
        when(platoService.prepararModificacion(USERNAME, 999))
                .thenThrow(new GestionPlatoException(mensaje));

        mockMvc.perform(get("/responsable/platos/999/editar").session(sesionResponsable()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/responsable/platos"))
                .andExpect(flash().attribute("error", mensaje));
    }

    @Test
    void bajaSeRealizaUnicamentePorPost() throws Exception {
        mockMvc.perform(post("/responsable/platos/7/baja").session(sesionResponsable()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/responsable/platos"))
                .andExpect(flash().attribute("mensajeExito", containsString("dio de baja")));

        verify(platoService).baja(USERNAME, PLATO_ID);
    }

    @Test
    void noExisteBajaPorGet() throws Exception {
        mockMvc.perform(get("/responsable/platos/7/baja").session(sesionResponsable()))
                .andExpect(status().isMethodNotAllowed());

        verify(platoService, never()).baja(any(), anyInt());
    }

    @Test
    void errorFuncionalDeBajaSeConservaComoFlash() throws Exception {
        String mensaje = "El Plato ya se encuentra dado de baja.";
        org.mockito.Mockito.doThrow(new GestionPlatoException(mensaje))
                .when(platoService).baja(USERNAME, PLATO_ID);

        mockMvc.perform(post("/responsable/platos/7/baja").session(sesionResponsable()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", mensaje));
    }

    @Test
    void inicioDelResponsableConservaAccesosYAgregaNavegacionAPlatos() throws Exception {
        when(festivalService.traerTodos()).thenReturn(List.of());

        mockMvc.perform(get("/index").session(sesionResponsable()))
                .andExpect(status().isOk())
                .andExpect(view().name("home/index"))
                .andExpect(content().string(containsString("Alta de empleado")))
                .andExpect(content().string(containsString("Reportes")))
                .andExpect(content().string(containsString("Platos")))
                .andExpect(content().string(containsString("/responsable/platos")));
    }

    private PlatoService.ContextoFormularioPlato contextoAlta() {
        return new PlatoService.ContextoFormularioPlato(
                "Ruedas de Fuego", new PlatoForm(), false);
    }

    private PlatoService.ContextoGestionPlatos contextoGestion(
            List<PlatoService.PlatoResumen> platos) {
        return new PlatoService.ContextoGestionPlatos("Ruedas de Fuego", platos);
    }

    private PlatoForm form(String nombre, Float precioVenta, Float costoProduccion) {
        PlatoForm form = new PlatoForm();
        form.setNombre(nombre);
        form.setPrecioVenta(precioVenta);
        form.setCostoProduccion(costoProduccion);
        return form;
    }

    private MockHttpSession sesionResponsable() {
        return sesionAutenticada(USERNAME, "ROLE_RESPONSABLE");
    }

    private MockHttpSession sesionAutenticada(String username, String authority) {
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new TestingAuthenticationToken(username, null, authority));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                contexto);
        return session;
    }
}
