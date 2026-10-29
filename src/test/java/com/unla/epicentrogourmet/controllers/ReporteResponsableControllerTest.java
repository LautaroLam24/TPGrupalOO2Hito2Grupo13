package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.configuration.SecurityConfiguration;
import com.unla.epicentrogourmet.dto.PlatoDTO;
import com.unla.epicentrogourmet.dto.ReporteVentaDTO;
import com.unla.epicentrogourmet.exceptions.PlatoEstrellaException;
import com.unla.epicentrogourmet.exceptions.ReporteResponsableException;
import com.unla.epicentrogourmet.services.IFestivalService;
import com.unla.epicentrogourmet.services.implementation.PlatoService;
import com.unla.epicentrogourmet.services.implementation.RentabilidadNetaService;
import com.unla.epicentrogourmet.services.implementation.ReporteResponsableService;
import com.unla.epicentrogourmet.services.implementation.UserService;
import org.junit.jupiter.api.Test;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ReporteResponsableController.class)
@Import(SecurityConfiguration.class)
class ReporteResponsableControllerTest {

    private static final String USERNAME = "responsable@epicentro.com";
    private static final int FESTIVAL_ID = 20;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlatoService platoService;

    @MockBean
    private ReporteResponsableService reporteResponsableService;

    @MockBean
    private IFestivalService festivalService;

    @MockBean
    private RentabilidadNetaService rentabilidadNetaService;

    @MockBean
    private UserService userService;

    @Test
    void anonimoEsRedirigidoAlLogin() throws Exception {
        mockMvc.perform(get("/responsable/reportes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(platoService);
    }

    @Test
    void empleadoNoPuedeAcceder() throws Exception {
        mockMvc.perform(get("/responsable/reportes")
                        .session(sesionAutenticada(USERNAME, "ROLE_EMPLEADO")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(platoService);
    }

    @Test
    void administradorNoPuedeAcceder() throws Exception {
        mockMvc.perform(get("/responsable/reportes/plato-estrella")
                        .session(sesionAutenticada(USERNAME, "ROLE_ADMIN")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(platoService);
    }

    @Test
    void empleadoNoPuedeAccederARecaudacion() throws Exception {
        mockMvc.perform(get("/responsable/reportes/recaudacion")
                        .session(sesionAutenticada(USERNAME, "ROLE_EMPLEADO")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reporteResponsableService, festivalService);
    }

    @Test
    void responsableAccedeALaPortadaConPlatoEstrellaYRecaudacion() throws Exception {
        mockMvc.perform(get("/responsable/reportes")
                        .session(sesionResponsable()))
                .andExpect(status().isOk())
                .andExpect(view().name("responsable/reportes/index"))
                .andExpect(content().string(containsString("Plato estrella")))
                .andExpect(content().string(containsString("Recaudación")))
                .andExpect(content().string(containsString("/responsable/reportes/recaudacion")))
                .andExpect(content().string(not(containsString("/responsable/reportes/rentabilidad"))));

        verifyNoInteractions(platoService, reporteResponsableService, festivalService);
    }

    @Test
    void getSinSeleccionUsaUsernameAutenticadoYMuestraContexto() throws Exception {
        PlatoService.ContextoPlatoEstrella contexto = contexto();
        when(platoService.preparar(USERNAME)).thenReturn(contexto);

        mockMvc.perform(get("/responsable/reportes/plato-estrella")
                        .session(sesionResponsable()))
                .andExpect(status().isOk())
                .andExpect(view().name("responsable/reportes/plato-estrella"))
                .andExpect(model().attribute("unidad", "Ruedas de Fuego"))
                .andExpect(model().attribute("festivales", contexto.festivales()))
                .andExpect(model().attributeDoesNotExist("festivalId", "platoEstrella", "sinVentas"));

        verify(platoService).preparar(USERNAME);
        verify(platoService, never()).platoEstrella(USERNAME, FESTIVAL_ID);
    }

    @Test
    void getConFestivalConservaSeleccionYMuestraResultado() throws Exception {
        PlatoService.ContextoPlatoEstrella contexto = contexto();
        PlatoDTO resultado = new PlatoDTO("Pizza", 4, "Ruedas de Fuego");
        when(platoService.preparar(USERNAME)).thenReturn(contexto);
        when(platoService.platoEstrella(USERNAME, FESTIVAL_ID))
                .thenReturn(Optional.of(resultado));

        mockMvc.perform(get("/responsable/reportes/plato-estrella")
                        .session(sesionResponsable())
                        .param("festivalId", String.valueOf(FESTIVAL_ID)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("festivalId", FESTIVAL_ID))
                .andExpect(model().attribute("festivalSeleccionado", contexto.festivales().get(0)))
                .andExpect(model().attribute("consultaRealizada", true))
                .andExpect(model().attribute("platoEstrella", resultado))
                .andExpect(model().attributeDoesNotExist("sinVentas"));

        verify(platoService).preparar(USERNAME);
        verify(platoService).platoEstrella(USERNAME, FESTIVAL_ID);
    }

    @Test
    void resultadoVacioExponeMensajeInformativo() throws Exception {
        when(platoService.preparar(USERNAME)).thenReturn(contexto());
        when(platoService.platoEstrella(USERNAME, FESTIVAL_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/responsable/reportes/plato-estrella")
                        .session(sesionResponsable())
                        .param("festivalId", String.valueOf(FESTIVAL_ID)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("consultaRealizada", true))
                .andExpect(model().attribute("sinVentas", true))
                .andExpect(model().attributeDoesNotExist("platoEstrella"))
                .andExpect(content().string(containsString(
                        "No existen pedidos para determinar el Plato estrella")));
    }

    @Test
    void errorFuncionalSeMuestraSinExponerUnaVistaDeError() throws Exception {
        String mensaje = "El festival seleccionado no existe o no está asociado a la unidad administrada.";
        when(platoService.preparar(USERNAME)).thenReturn(contexto());
        when(platoService.platoEstrella(USERNAME, 999))
                .thenThrow(new PlatoEstrellaException(mensaje));

        mockMvc.perform(get("/responsable/reportes/plato-estrella")
                        .session(sesionResponsable())
                        .param("festivalId", "999"))
                .andExpect(status().isOk())
                .andExpect(view().name("responsable/reportes/plato-estrella"))
                .andExpect(model().attribute("error", mensaje))
                .andExpect(model().attributeDoesNotExist("platoEstrella"));
    }

    @Test
    void unidadIdManipuladoNoCambiaIdentidadNiContratoDelService() throws Exception {
        when(platoService.preparar(USERNAME)).thenReturn(contexto());
        when(platoService.platoEstrella(USERNAME, FESTIVAL_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/responsable/reportes/plato-estrella")
                        .session(sesionResponsable())
                        .param("festivalId", String.valueOf(FESTIVAL_ID))
                        .param("unidadId", "999")
                        .param("username", "intruso@epicentro.com")
                        .param("fechaInicio", "2000-01-01"))
                .andExpect(status().isOk());

        verify(platoService).preparar(USERNAME);
        verify(platoService).platoEstrella(USERNAME, FESTIVAL_ID);
        verify(platoService, never()).preparar("intruso@epicentro.com");
    }

    @Test
    void getRecaudacionSinSeleccionUsaUsernameAutenticadoYMuestraContexto() throws Exception {
        ReporteResponsableService.ContextoReportes contexto = contextoReportes();
        when(reporteResponsableService.preparar(USERNAME)).thenReturn(contexto);

        mockMvc.perform(get("/responsable/reportes/recaudacion")
                        .session(sesionResponsable()))
                .andExpect(status().isOk())
                .andExpect(view().name("responsable/reportes/recaudacion"))
                .andExpect(model().attribute("unidad", "Ruedas de Fuego"))
                .andExpect(model().attribute("festivales", contexto.festivales()))
                .andExpect(model().attributeDoesNotExist(
                        "festivalId", "reporteVenta", "sinRecaudacion"));

        verify(reporteResponsableService).preparar(USERNAME);
        verifyNoInteractions(festivalService);
    }

    @Test
    void responsableSinUnidadVeErrorFuncionalEnRecaudacion() throws Exception {
        String mensaje = "No se encontró una unidad de venta para el responsable autenticado.";
        when(reporteResponsableService.preparar(USERNAME))
                .thenThrow(new ReporteResponsableException(mensaje));

        mockMvc.perform(get("/responsable/reportes/recaudacion")
                        .session(sesionResponsable()))
                .andExpect(status().isOk())
                .andExpect(view().name("responsable/reportes/recaudacion"))
                .andExpect(model().attribute("error", mensaje))
                .andExpect(model().attributeDoesNotExist("unidad", "reporteVenta"));

        verifyNoInteractions(festivalService);
    }

    @Test
    void getRecaudacionConFestivalConservaSeleccionFechasYTotal() throws Exception {
        ReporteResponsableService.ContextoReportes contexto = contextoReportes();
        ReporteVentaDTO reporte = new ReporteVentaDTO("Festival Sur", 450000.5f);
        when(reporteResponsableService.preparar(USERNAME)).thenReturn(contexto);
        when(festivalService.reporteRecaudacion(USERNAME, FESTIVAL_ID)).thenReturn(reporte);

        mockMvc.perform(get("/responsable/reportes/recaudacion")
                        .session(sesionResponsable())
                        .param("festivalId", String.valueOf(FESTIVAL_ID)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("festivalId", FESTIVAL_ID))
                .andExpect(model().attribute("festivalSeleccionado", contexto.festivales().get(0)))
                .andExpect(model().attribute("consultaRealizada", true))
                .andExpect(model().attribute("reporteVenta", reporte))
                .andExpect(model().attributeDoesNotExist("sinRecaudacion"))
                .andExpect(content().string(containsString("01/10/2026")))
                .andExpect(content().string(containsString("05/10/2026")));

        verify(festivalService).reporteRecaudacion(USERNAME, FESTIVAL_ID);
    }

    @Test
    void recaudacionCeroMuestraMensajeInformativoSinAfirmarAusenciaDePedidos() throws Exception {
        when(reporteResponsableService.preparar(USERNAME)).thenReturn(contextoReportes());
        when(festivalService.reporteRecaudacion(USERNAME, FESTIVAL_ID))
                .thenReturn(new ReporteVentaDTO("Festival Sur", 0.0f));

        mockMvc.perform(get("/responsable/reportes/recaudacion")
                        .session(sesionResponsable())
                        .param("festivalId", String.valueOf(FESTIVAL_ID)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sinRecaudacion", true))
                .andExpect(content().string(containsString(
                        "El Festival no registra recaudación en el período")))
                .andExpect(content().string(not(containsString("no existen pedidos"))));
    }

    @Test
    void festivalInvalidoEnRecaudacionMuestraErrorSeguro() throws Exception {
        String mensaje = "El festival seleccionado no existe o no está asociado a la unidad administrada.";
        when(reporteResponsableService.preparar(USERNAME)).thenReturn(contextoReportes());
        when(festivalService.reporteRecaudacion(USERNAME, 999))
                .thenThrow(new ReporteResponsableException(mensaje));

        mockMvc.perform(get("/responsable/reportes/recaudacion")
                        .session(sesionResponsable())
                        .param("festivalId", "999"))
                .andExpect(status().isOk())
                .andExpect(view().name("responsable/reportes/recaudacion"))
                .andExpect(model().attribute("error", mensaje))
                .andExpect(model().attributeDoesNotExist("reporteVenta"));
    }

    @Test
    void parametrosManipuladosNoCambianIdentidadUnidadFechasNiImporteDeRecaudacion() throws Exception {
        when(reporteResponsableService.preparar(USERNAME)).thenReturn(contextoReportes());
        when(festivalService.reporteRecaudacion(USERNAME, FESTIVAL_ID))
                .thenReturn(new ReporteVentaDTO("Festival Sur", 1000.0f));

        mockMvc.perform(get("/responsable/reportes/recaudacion")
                        .session(sesionResponsable())
                        .param("festivalId", String.valueOf(FESTIVAL_ID))
                        .param("unidadId", "999")
                        .param("username", "intruso@epicentro.com")
                        .param("fechaInicio", "2000-01-01")
                        .param("fechaFin", "2099-01-01")
                        .param("recaudacionTotal", "1"))
                .andExpect(status().isOk());

        verify(reporteResponsableService).preparar(USERNAME);
        verify(festivalService).reporteRecaudacion(USERNAME, FESTIVAL_ID);
        verify(reporteResponsableService, never()).preparar("intruso@epicentro.com");
        verify(festivalService, never()).reporteRecaudacion("intruso@epicentro.com", FESTIVAL_ID);
    }

    @Test
    void recaudacionNoExponeEndpointPost() throws Exception {
        mockMvc.perform(post("/responsable/reportes/recaudacion")
                        .session(sesionResponsable()))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(reporteResponsableService, festivalService);
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

    private PlatoService.ContextoPlatoEstrella contexto() {
        return new PlatoService.ContextoPlatoEstrella(
                "Ruedas de Fuego",
                List.of(new PlatoService.FestivalOpcion(
                        FESTIVAL_ID,
                        "Festival Sur",
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 5))));
    }

    private ReporteResponsableService.ContextoReportes contextoReportes() {
        return new ReporteResponsableService.ContextoReportes(
                "Ruedas de Fuego",
                List.of(new ReporteResponsableService.FestivalOpcion(
                        FESTIVAL_ID,
                        "Festival Sur",
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 5))));
    }
}
