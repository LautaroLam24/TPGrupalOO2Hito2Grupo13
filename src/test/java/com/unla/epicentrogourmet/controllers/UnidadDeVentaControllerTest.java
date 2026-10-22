package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.dto.RegistroUnidadResponsableForm;
import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.exceptions.RegistroUnidadException;
import com.unla.epicentrogourmet.services.IFestivalService;
import com.unla.epicentrogourmet.services.implementation.UnidadDeVentaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
class UnidadDeVentaControllerTest {

    @Mock
    private UnidadDeVentaService unidadDeVentaService;

    @Mock
    private IFestivalService festivalService;

    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        UnidadDeVentaController controller = new UnidadDeVentaController(unidadDeVentaService, festivalService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void getMuestraFormularioConFestivalesDisponibles() throws Exception {
        Festival festival = festivalDisponible();
        when(festivalService.traerDisponibles()).thenReturn(List.of(festival));

        mockMvc.perform(get("/registro/unidad"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro/unidad-form"))
                .andExpect(model().attributeExists(UnidadDeVentaController.FORM_ATTRIBUTE))
                .andExpect(model().attribute("festivales", List.of(festival)));
    }

    @Test
    void postValidoRegistraYRedirigeConConfirmacion() throws Exception {
        mockMvc.perform(post("/registro/unidad")
                        .param("festivalId", "1")
                        .param("tipoUnidad", "FOOD_TRUCK")
                        .param("codigo", "abcd123456")
                        .param("nombreComercial", "Sabores sobre ruedas")
                        .param("superficieM2", "18.5")
                        .param("username", "responsable@ejemplo.com")
                        .param("password", "secreta")
                        .param("patente", "aa123bb")
                        .param("requiereElectricidad", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registro/unidad"))
                .andExpect(flash().attribute(
                        "mensajeExito", "La unidad y su responsable se registraron correctamente."));

        ArgumentCaptor<RegistroUnidadResponsableForm> captor =
                ArgumentCaptor.forClass(RegistroUnidadResponsableForm.class);
        verify(unidadDeVentaService).registrarConResponsable(captor.capture());
        assertEquals("responsable@ejemplo.com", captor.getValue().getUsername());
    }

    @Test
    void postInvalidoVuelveAlFormularioSinInvocarServicio() throws Exception {
        when(festivalService.traerDisponibles()).thenReturn(List.of(festivalDisponible()));

        mockMvc.perform(post("/registro/unidad"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro/unidad-form"))
                .andExpect(model().attributeHasFieldErrors(
                        UnidadDeVentaController.FORM_ATTRIBUTE,
                        "festivalId", "tipoUnidad", "codigo", "nombreComercial",
                        "superficieM2", "username", "password"))
                .andExpect(model().attribute(
                        UnidadDeVentaController.FORM_ATTRIBUTE,
                        hasProperty("password", nullValue())));

        verify(unidadDeVentaService, never()).registrarConResponsable(any());
    }

    @Test
    void errorDeNegocioSeMuestraSinRepoblarPassword() throws Exception {
        String mensaje = "El código de unidad ya está registrado.";
        doThrow(new RegistroUnidadException(mensaje))
                .when(unidadDeVentaService).registrarConResponsable(any());
        when(festivalService.traerDisponibles()).thenReturn(List.of(festivalDisponible()));

        mockMvc.perform(post("/registro/unidad")
                        .param("festivalId", "1")
                        .param("tipoUnidad", "PUESTO_DESARMABLE")
                        .param("codigo", "puesto1234")
                        .param("nombreComercial", "Puesto de prueba")
                        .param("superficieM2", "12")
                        .param("username", "responsable@ejemplo.com")
                        .param("password", "secreta")
                        .param("cantCarpas", "2")
                        .param("tiempoMontajeMin", "30"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro/unidad-form"))
                .andExpect(model().attributeHasErrors(UnidadDeVentaController.FORM_ATTRIBUTE))
                .andExpect(model().attribute(
                        UnidadDeVentaController.FORM_ATTRIBUTE,
                        hasProperty("password", nullValue())))
                .andExpect(result -> {
                    BindingResult bindingResult = (BindingResult) result.getModelAndView().getModel().get(
                            BindingResult.MODEL_KEY_PREFIX + UnidadDeVentaController.FORM_ATTRIBUTE);
                    List<String> errores = bindingResult.getGlobalErrors().stream()
                            .map(DefaultMessageSourceResolvable::getDefaultMessage)
                            .toList();
                    assertTrue(errores.contains(mensaje));
                });
    }

    @Test
    void emailInvalidoSeRechazaAntesDelServicio() throws Exception {
        when(festivalService.traerDisponibles()).thenReturn(List.of(festivalDisponible()));

        mockMvc.perform(post("/registro/unidad")
                        .param("festivalId", "1")
                        .param("tipoUnidad", "PUESTO_DESARMABLE")
                        .param("codigo", "puesto1234")
                        .param("nombreComercial", "Puesto de prueba")
                        .param("superficieM2", "12")
                        .param("username", "email-invalido")
                        .param("password", "secreta")
                        .param("cantCarpas", "2")
                        .param("tiempoMontajeMin", "30"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors(
                        UnidadDeVentaController.FORM_ATTRIBUTE, "username"));

        verify(unidadDeVentaService, never()).registrarConResponsable(any());
    }

    private Festival festivalDisponible() {
        Festival festival = new Festival();
        festival.setId(1);
        festival.setNombre("Festival vigente");
        festival.setFechaInicio(LocalDate.now());
        festival.setFechaFin(LocalDate.now().plusDays(2));
        return festival;
    }
}
