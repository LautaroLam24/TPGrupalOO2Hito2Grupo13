package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.configuration.SecurityConfiguration;
import com.unla.epicentrogourmet.dto.CanonUnidadDTO;
import com.unla.epicentrogourmet.dto.ReporteCanonDTO;
import com.unla.epicentrogourmet.exceptions.ReporteResponsableException;
import com.unla.epicentrogourmet.services.implementation.UnidadDeVentaService;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CanonUnidadController.class)
@Import(SecurityConfiguration.class)
class CanonUnidadControllerTest {

    private static final String USERNAME = "responsable1@epicentro.com";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UnidadDeVentaService unidadDeVentaService;

    @MockBean
    private UserService userService;

    @Test
    void anonimoEsRedirigidoAlLogin() throws Exception {
        mockMvc.perform(get("/responsable/reportes/canon"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(unidadDeVentaService);
    }

    @Test
    void empleadoNoPuedeAcceder() throws Exception {
        mockMvc.perform(get("/responsable/reportes/canon")
                        .session(sesionAutenticada("ROLE_EMPLEADO")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(unidadDeVentaService);
    }

    @Test
    void responsableVeElDesgloseYElTotal() throws Exception {
        CanonUnidadDTO canon = new CanonUnidadDTO(
                "Festival de la Cerveza", LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 15), "En curso",
                12f, 500f, 6000d, "Plus por conexión eléctrica", 2000f, 2000d, 8000d);
        ReporteCanonDTO reporte = new ReporteCanonDTO(
                "Ruedas de Fuego", "Food truck", "FTK0000001", 12f, true, null, null, List.of(canon), 8000d);
        when(unidadDeVentaService.canonPorFestival(USERNAME)).thenReturn(reporte);

        mockMvc.perform(get("/responsable/reportes/canon")
                        .session(sesionAutenticada("ROLE_RESPONSABLE")))
                .andExpect(status().isOk())
                .andExpect(view().name("responsable/reportes/canon"))
                .andExpect(model().attribute("reporte", reporte))
                .andExpect(content().string(containsString("Ruedas de Fuego")))
                .andExpect(content().string(containsString("Festival de la Cerveza")))
                .andExpect(content().string(containsString("Plus por conexión eléctrica")))
                .andExpect(content().string(containsString("En curso")));

        verify(unidadDeVentaService).canonPorFestival(USERNAME);
    }

    @Test
    void unidadSinFestivalesMuestraMensaje() throws Exception {
        when(unidadDeVentaService.canonPorFestival(USERNAME)).thenReturn(new ReporteCanonDTO(
                "Ruedas de Fuego", "Food truck", "FTK0000001", 12f, true, null, null, List.of(), 0d));

        mockMvc.perform(get("/responsable/reportes/canon")
                        .session(sesionAutenticada("ROLE_RESPONSABLE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("no participa en ningún festival")));
    }

    @Test
    void errorDelServicioSeMuestraEnLaVista() throws Exception {
        when(unidadDeVentaService.canonPorFestival(USERNAME)).thenThrow(new ReporteResponsableException(
                "No se encontró una unidad de venta para el responsable autenticado."));

        mockMvc.perform(get("/responsable/reportes/canon")
                        .session(sesionAutenticada("ROLE_RESPONSABLE")))
                .andExpect(status().isOk())
                .andExpect(view().name("responsable/reportes/canon"))
                .andExpect(model().attribute("error",
                        "No se encontró una unidad de venta para el responsable autenticado."));
    }

    private MockHttpSession sesionAutenticada(String authority) {
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new TestingAuthenticationToken(USERNAME, null, authority));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                contexto);
        return session;
    }
}
