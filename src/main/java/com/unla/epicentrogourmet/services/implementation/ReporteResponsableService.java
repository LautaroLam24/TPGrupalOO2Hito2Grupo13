package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.entities.RoleName;
import com.unla.epicentrogourmet.entities.UnidadDeVenta;
import com.unla.epicentrogourmet.entities.User;
import com.unla.epicentrogourmet.exceptions.ReporteResponsableException;
import com.unla.epicentrogourmet.repositories.IFestivalRepository;
import com.unla.epicentrogourmet.repositories.IUnidadDeVentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReporteResponsableService {

    private final IUnidadDeVentaRepository unidadDeVentaRepository;
    private final IFestivalRepository festivalRepository;

    public ReporteResponsableService(IUnidadDeVentaRepository unidadDeVentaRepository,
                                     IFestivalRepository festivalRepository) {
        this.unidadDeVentaRepository = unidadDeVentaRepository;
        this.festivalRepository = festivalRepository;
    }

    @Transactional(readOnly = true)
    public ContextoReportes preparar(String username) {
        UnidadDeVenta unidad = obtenerUnidadAutorizada(username);
        List<FestivalOpcion> festivales = festivalRepository.findByUnidadId(unidad.getId()).stream()
                .map(this::crearOpcion)
                .toList();

        return new ContextoReportes(unidad.getNombreComercial(), festivales);
    }

    @Transactional(readOnly = true)
    public ContextoFestivalAutorizado autorizarFestival(String username, int festivalId) {
        UnidadDeVenta unidad = obtenerUnidadAutorizada(username);
        Festival festival = festivalRepository.findByIdAndUnidadId(festivalId, unidad.getId())
                .orElseThrow(() -> new ReporteResponsableException(
                        "El festival seleccionado no existe o no está asociado a la unidad administrada."));
        validarFechas(festival);
        return new ContextoFestivalAutorizado(unidad, festival);
    }

    private UnidadDeVenta obtenerUnidadAutorizada(String username) {
        if (username == null || username.isBlank()) {
            throw new ReporteResponsableException("Se requiere un responsable autenticado.");
        }

        List<UnidadDeVenta> unidades = unidadDeVentaRepository.findByResponsableUsername(username);
        if (unidades == null || unidades.isEmpty()) {
            throw new ReporteResponsableException(
                    "No se encontró una unidad de venta para el responsable autenticado.");
        }
        if (unidades.size() != 1) {
            throw new ReporteResponsableException(
                    "La configuración de unidades del responsable autenticado es inconsistente.");
        }

        UnidadDeVenta unidad = unidades.get(0);
        User responsable = unidad == null ? null : unidad.getResponsable();
        if (responsable == null
                || !username.equals(responsable.getUsername())
                || !responsable.isEnabled()
                || responsable.getRole() == null
                || responsable.getRole().getRole() != RoleName.ROLE_RESPONSABLE) {
            throw new ReporteResponsableException(
                    "El responsable autenticado no se encuentra habilitado o autorizado.");
        }
        return unidad;
    }

    private FestivalOpcion crearOpcion(Festival festival) {
        if (festival == null) {
            throw new ReporteResponsableException(
                    "No fue posible preparar los festivales de la unidad administrada.");
        }
        return new FestivalOpcion(
                festival.getId(),
                festival.getNombre(),
                festival.getFechaInicio(),
                festival.getFechaFin());
    }

    private void validarFechas(Festival festival) {
        LocalDate fechaInicio = festival.getFechaInicio();
        LocalDate fechaFin = festival.getFechaFin();
        if (fechaInicio == null || fechaFin == null || fechaInicio.isAfter(fechaFin)) {
            throw new ReporteResponsableException(
                    "El festival seleccionado tiene un intervalo de fechas inválido.");
        }
    }

    public record FestivalOpcion(
            int id,
            String nombre,
            LocalDate fechaInicio,
            LocalDate fechaFin) {
    }

    public record ContextoReportes(
            String unidad,
            List<FestivalOpcion> festivales) {

        public ContextoReportes {
            festivales = List.copyOf(festivales);
        }
    }

    public record ContextoFestivalAutorizado(
            UnidadDeVenta unidad,
            Festival festival) {
    }
}
