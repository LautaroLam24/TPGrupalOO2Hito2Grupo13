package com.unla.epicentrogourmet.controllers.api;

import com.unla.epicentrogourmet.configuration.SecurityConfiguration;
import com.unla.epicentrogourmet.dto.EstadoFestival;
import com.unla.epicentrogourmet.dto.FestivalRequestDTO;
import com.unla.epicentrogourmet.dto.FestivalResponseDTO;
import com.unla.epicentrogourmet.services.IFestivalService;
import com.unla.epicentrogourmet.services.implementation.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FestivalRestController.class)
@Import(SecurityConfiguration.class)
class FestivalRestControllerTest {

    private static final String JSON_VALIDO = """
            {"nombre": "Festival Gourmet de Otoño 2027", "fechaInicio": "2027-04-10", "fechaFin": "2027-04-14",
             "costoSuperficie": 500, "costoMontaje": 10, "plusElectricidad": 2000, "sueldoBase": 100000,
             "plusAntiguedad": 5000, "plusCocinero": 150000, "plusAyudante": 100000, "plusLavaplatos": 50000}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IFestivalService festivalService;

    @MockBean
    private UserService userService;

    @Test
    void getSinLoginDevuelve200ConLosFiltrosRecibidos() throws Exception {
        when(festivalService.filtrar("primavera", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                EstadoFestival.PROXIMO)).thenReturn(List.of(respuesta()));

        mockMvc.perform(get("/api/v1/festivales")
                        .param("nombre", "primavera")
                        .param("desde", "2026-01-01")
                        .param("hasta", "2026-12-31")
                        .param("estado", "PROXIMO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Festival Gourmet de Otoño 2027"))
                .andExpect(jsonPath("$[0].fechaInicio").value("2027-04-10"))
                .andExpect(jsonPath("$[0].estado").value("PROXIMO"));
    }

    @Test
    void getConDesdePosteriorAHastaDevuelve400ConMensaje() throws Exception {
        when(festivalService.filtrar(null, LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1), null))
                .thenThrow(new IllegalArgumentException("La fecha 'desde' no puede ser posterior."));

        mockMvc.perform(get("/api/v1/festivales").param("desde", "2026-12-31").param("hasta", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La fecha 'desde' no puede ser posterior."));
    }

    @Test
    void getConEstadoInexistenteDevuelve400ConValoresPosibles() throws Exception {
        mockMvc.perform(get("/api/v1/festivales").param("estado", "CANCELADO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("EN_CURSO, PROXIMO, FINALIZADO")));
        verifyNoInteractions(festivalService);
    }

    @Test
    void postValidoSinLoginNiCsrfDevuelve201() throws Exception {
        when(festivalService.alta(any(FestivalRequestDTO.class))).thenReturn(respuesta());

        mockMvc.perform(post("/api/v1/festivales").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.duracionDias").value(5));
        verify(festivalService).alta(any(FestivalRequestDTO.class));
    }

    @Test
    void postConErrorDeNegocioDevuelve400() throws Exception {
        when(festivalService.alta(any(FestivalRequestDTO.class))).thenThrow(
                new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio."));

        mockMvc.perform(post("/api/v1/festivales").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La fecha de fin no puede ser anterior a la fecha de inicio."));
    }

    @Test
    void postConCamposFaltantesDevuelve400ListandoLosCamposSinLlamarAlService() throws Exception {
        mockMvc.perform(post("/api/v1/festivales").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"  \", \"fechaInicio\": \"2027-04-10\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("nombre: es obligatorio")))
                .andExpect(jsonPath("$.error").value(containsString("fechaFin: es obligatoria")))
                .andExpect(jsonPath("$.error").value(containsString("plusLavaplatos: es obligatorio")));
        verifyNoInteractions(festivalService);
    }

    @Test
    void postConFechaMalFormadaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/festivales").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO.replace("2027-04-14", "14/04/2027")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("yyyy-MM-dd")));
        verifyNoInteractions(festivalService);
    }

    private FestivalResponseDTO respuesta() {
        return new FestivalResponseDTO(1, "Festival Gourmet de Otoño 2027",
                LocalDate.of(2027, 4, 10), LocalDate.of(2027, 4, 14), 5, EstadoFestival.PROXIMO, 0,
                500f, 10f, 2000f, 100000f, 5000f, 150000f, 100000f, 50000f);
    }
}
