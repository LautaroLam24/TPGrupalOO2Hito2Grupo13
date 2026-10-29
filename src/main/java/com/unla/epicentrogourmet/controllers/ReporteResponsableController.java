package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.dto.PlatoDTO;
import com.unla.epicentrogourmet.dto.ReporteRentabilidadNetaDTO;
import com.unla.epicentrogourmet.dto.ReporteVentaDTO;
import com.unla.epicentrogourmet.exceptions.PlatoEstrellaException;
import com.unla.epicentrogourmet.exceptions.ReporteResponsableException;
import com.unla.epicentrogourmet.helpers.ViewRouteHelper;
import com.unla.epicentrogourmet.services.IFestivalService;
import com.unla.epicentrogourmet.services.implementation.PlatoService;
import com.unla.epicentrogourmet.services.implementation.RentabilidadNetaService;
import com.unla.epicentrogourmet.services.implementation.ReporteResponsableService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Optional;

@Controller
@RequestMapping(ViewRouteHelper.ROUTE_REPORTES_RESPONSABLE)
@PreAuthorize("hasRole('RESPONSABLE')")
public class ReporteResponsableController {

    private final PlatoService platoService;
    private final ReporteResponsableService reporteResponsableService;
    private final IFestivalService festivalService;
    private final RentabilidadNetaService rentabilidadNetaService;

    public ReporteResponsableController(PlatoService platoService,
                                        ReporteResponsableService reporteResponsableService,
                                        IFestivalService festivalService,
                                        RentabilidadNetaService rentabilidadNetaService) {
        this.platoService = platoService;
        this.reporteResponsableService = reporteResponsableService;
        this.festivalService = festivalService;
        this.rentabilidadNetaService = rentabilidadNetaService;
    }

    @GetMapping
    public String mostrarReportes() {
        return ViewRouteHelper.REPORTES_RESPONSABLE;
    }

    @GetMapping("/plato-estrella")
    public String mostrarPlatoEstrella(
            @RequestParam(required = false) Integer festivalId,
            Authentication authentication,
            Model model) {
        String username = nombreUsuario(authentication);

        try {
            PlatoService.ContextoPlatoEstrella contexto = platoService.preparar(username);
            model.addAttribute("unidad", contexto.unidad());
            model.addAttribute("festivales", contexto.festivales());

            if (festivalId != null) {
                model.addAttribute("festivalId", festivalId);
                contexto.festivales().stream()
                        .filter(festival -> festival.id() == festivalId)
                        .findFirst()
                        .ifPresent(festival -> model.addAttribute("festivalSeleccionado", festival));

                Optional<PlatoDTO> resultado = platoService.platoEstrella(username, festivalId);
                model.addAttribute("consultaRealizada", true);
                resultado.ifPresentOrElse(
                        plato -> model.addAttribute("platoEstrella", plato),
                        () -> model.addAttribute("sinVentas", true));
            }
        } catch (PlatoEstrellaException ex) {
            model.addAttribute("error", ex.getMessage());
        }

        return ViewRouteHelper.PLATO_ESTRELLA;
    }

    @GetMapping("/recaudacion")
    public String mostrarRecaudacion(
            @RequestParam(required = false) Integer festivalId,
            Authentication authentication,
            Model model) {
        String username = nombreUsuario(authentication);

        try {
            ReporteResponsableService.ContextoReportes contexto =
                    reporteResponsableService.preparar(username);
            model.addAttribute("unidad", contexto.unidad());
            model.addAttribute("festivales", contexto.festivales());

            if (festivalId != null) {
                model.addAttribute("festivalId", festivalId);
                contexto.festivales().stream()
                        .filter(festival -> festival.id() == festivalId)
                        .findFirst()
                        .ifPresent(festival -> model.addAttribute("festivalSeleccionado", festival));

                ReporteVentaDTO reporte = festivalService.reporteRecaudacion(username, festivalId);
                model.addAttribute("consultaRealizada", true);
                model.addAttribute("reporteVenta", reporte);
                if (reporte.recaudacionTotal() == 0.0f) {
                    model.addAttribute("sinRecaudacion", true);
                }
            }
        } catch (ReporteResponsableException ex) {
            model.addAttribute("error", ex.getMessage());
        }

        return ViewRouteHelper.RECAUDACION_FESTIVAL;
    }

    @GetMapping("/rentabilidad-neta")
    public String mostrarRentabilidadNeta(
            @RequestParam(required = false) Integer festivalId,
            Authentication authentication,
            Model model) {
        String username = nombreUsuario(authentication);

        try {
            ReporteResponsableService.ContextoReportes contexto =
                    reporteResponsableService.preparar(username);
            model.addAttribute("unidad", contexto.unidad());
            model.addAttribute("festivales", contexto.festivales());

            if (festivalId != null) {
                model.addAttribute("festivalId", festivalId);
                ReporteRentabilidadNetaDTO reporte = rentabilidadNetaService.calcular(username, festivalId);
                model.addAttribute("consultaRealizada", true);
                model.addAttribute("reporteRentabilidad", reporte);
            }
        } catch (ReporteResponsableException ex) {
            model.addAttribute("error", ex.getMessage());
        }

        return ViewRouteHelper.RENTABILIDAD_NETA;
    }

    @GetMapping("/rentabilidad-neta-fechas")
    public String mostrarRentabilidadNetaEntreFechas(
            @RequestParam(required = false) Integer festivalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication authentication,
            Model model) {
        String username = nombreUsuario(authentication);

        try {
            ReporteResponsableService.ContextoReportes contexto =
                    reporteResponsableService.preparar(username);
            model.addAttribute("unidad", contexto.unidad());
            model.addAttribute("festivales", contexto.festivales());

            if (festivalId != null) {
                model.addAttribute("festivalId", festivalId);
                model.addAttribute("desde", desde);
                model.addAttribute("hasta", hasta);
                ReporteRentabilidadNetaDTO reporte =
                        rentabilidadNetaService.calcularEntreFechas(username, festivalId, desde, hasta);
                model.addAttribute("consultaRealizada", true);
                model.addAttribute("reporteRentabilidad", reporte);
            }
        } catch (ReporteResponsableException ex) {
            model.addAttribute("error", ex.getMessage());
        }

        return ViewRouteHelper.RENTABILIDAD_NETA_FECHAS;
    }

    private String nombreUsuario(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }
}
