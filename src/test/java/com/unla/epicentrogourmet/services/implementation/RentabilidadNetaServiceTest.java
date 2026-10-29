package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.ReporteRentabilidadNetaDTO;
import com.unla.epicentrogourmet.entities.Cajero;
import com.unla.epicentrogourmet.entities.Costo;
import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.entities.FoodTruck;
import com.unla.epicentrogourmet.exceptions.ReporteResponsableException;
import com.unla.epicentrogourmet.repositories.IPedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentabilidadNetaServiceTest {

    private static final String USERNAME = "responsable@epicentro.com";

    @Mock
    private ReporteResponsableService reporteResponsableService;

    @Mock
    private IPedidoRepository pedidoRepository;

    private RentabilidadNetaService service;
    private FoodTruck unidad;
    private Festival festival;

    @BeforeEach
    void setUp() {
        service = new RentabilidadNetaService(reporteResponsableService, pedidoRepository);

        Costo costo = new Costo();
        costo.setCostoSuperficie(500);
        costo.setPlusElectricidad(2000);
        costo.setSueldoBase(100000);
        costo.setPlusAntiguedad(5000);

        festival = new Festival();
        festival.setId(1);
        festival.setNombre("Festival de la Pizza");
        festival.setFechaInicio(LocalDate.of(2026, 3, 1));
        festival.setFechaFin(LocalDate.of(2026, 3, 15)); // 15 dias -> factor 0.5
        festival.setCosto(costo);

        unidad = new FoodTruck();
        unidad.setId(10);
        unidad.setNombreComercial("Ruedas de Fuego");
        unidad.setSuperficieM2(20);
        unidad.setRequiereElectricidad(true); // canon = 20*500 + 2000 = 12.000
    }

    @Test
    void calculaPedidosMenosCostosMenosSueldosMenosCanon() {
        Cajero activo = cajero(false);
        Cajero dadoDeBaja = cajero(true);
        unidad.getStaff().add(activo);
        unidad.getStaff().add(dadoDeBaja);

        when(reporteResponsableService.autorizarFestival(USERNAME, 1))
                .thenReturn(new ReporteResponsableService.ContextoFestivalAutorizado(unidad, festival));
        when(pedidoRepository.sumTotalByUnidadAndFechas(10, festival.getFechaInicio(), festival.getFechaFin()))
                .thenReturn(200000.0);
        when(pedidoRepository.sumCostoPlatosByUnidadAndFechas(10, festival.getFechaInicio(), festival.getFechaFin()))
                .thenReturn(60000.0);

        ReporteRentabilidadNetaDTO reporte = service.calcular(USERNAME, 1);

        // sueldo del cajero activo: 0.5 * 100.000 = 50.000 (ingreso el dia del festival: antiguedad 0)
        assertEquals(140000.0, reporte.gananciaBruta(), 0.01);
        assertEquals(50000.0, reporte.sueldos(), 0.01);
        assertEquals(12000.0, reporte.canon(), 0.01);
        assertEquals(78000.0, reporte.rentabilidadNeta(), 0.01);
    }

    @Test
    void sinPedidosLaRentabilidadEsNegativaPorSueldosYCanon() {
        unidad.getStaff().add(cajero(false));
        when(reporteResponsableService.autorizarFestival(USERNAME, 1))
                .thenReturn(new ReporteResponsableService.ContextoFestivalAutorizado(unidad, festival));
        when(pedidoRepository.sumTotalByUnidadAndFechas(any(Integer.class), any(), any())).thenReturn(null);
        when(pedidoRepository.sumCostoPlatosByUnidadAndFechas(any(Integer.class), any(), any())).thenReturn(null);

        ReporteRentabilidadNetaDTO reporte = service.calcular(USERNAME, 1);

        assertEquals(0.0, reporte.pedidosTotales(), 0.01);
        assertEquals(-62000.0, reporte.rentabilidadNeta(), 0.01);
    }

    @Test
    void festivalNoAutorizadoPropagaLaExcepcionSinConsultarPedidos() {
        when(reporteResponsableService.autorizarFestival(USERNAME, 99))
                .thenThrow(new ReporteResponsableException("no autorizado"));

        assertThrows(ReporteResponsableException.class, () -> service.calcular(USERNAME, 99));
        verifyNoInteractions(pedidoRepository);
    }

    private Cajero cajero(boolean baja) {
        Cajero cajero = new Cajero();
        cajero.setFechaIngreso(festival.getFechaInicio());
        cajero.setBaja(baja);
        return cajero;
    }
}
