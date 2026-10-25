package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.PlatoDTO;
import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.entities.RoleName;
import com.unla.epicentrogourmet.entities.UnidadDeVenta;
import com.unla.epicentrogourmet.entities.User;
import com.unla.epicentrogourmet.exceptions.PlatoEstrellaException;
import com.unla.epicentrogourmet.repositories.IFestivalRepository;
import com.unla.epicentrogourmet.repositories.IPedidoRepository;
import com.unla.epicentrogourmet.repositories.IUnidadDeVentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PlatoService {

    private final IUnidadDeVentaRepository unidadDeVentaRepository;
    private final IFestivalRepository festivalRepository;
    private final IPedidoRepository pedidoRepository;

    public PlatoService(IUnidadDeVentaRepository unidadDeVentaRepository,
                        IFestivalRepository festivalRepository,
                        IPedidoRepository pedidoRepository) {
        this.unidadDeVentaRepository = unidadDeVentaRepository;
        this.festivalRepository = festivalRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public ContextoPlatoEstrella preparar(String username) {
        UnidadDeVenta unidad = obtenerUnidadAutorizada(username);
        List<FestivalOpcion> festivales = festivalRepository.findByUnidadId(unidad.getId()).stream()
                .map(this::crearOpcion)
                .toList();

        return new ContextoPlatoEstrella(unidad.getNombreComercial(), festivales);
    }

    @Transactional(readOnly = true)
    public Optional<PlatoDTO> platoEstrella(String username, int festivalId) {
        UnidadDeVenta unidad = obtenerUnidadAutorizada(username);
        Festival festival = festivalRepository.findByIdAndUnidadId(festivalId, unidad.getId())
                .orElseThrow(() -> new PlatoEstrellaException(
                        "El festival seleccionado no existe o no está asociado a la unidad administrada."));
        validarFechas(festival);

        List<Object[]> ranking = pedidoRepository.findRankingPlatosPorPedidos(
                unidad.getId(), festival.getFechaInicio(), festival.getFechaFin());
        if (ranking == null) {
            throw resultadoInconsistente();
        }
        if (ranking.isEmpty()) {
            return Optional.empty();
        }

        Object[] ganador = ranking.get(0);
        if (ganador == null
                || ganador.length < 3
                || !(ganador[0] instanceof Number)
                || !(ganador[1] instanceof String nombre)
                || nombre.isBlank()
                || !(ganador[2] instanceof Long cantidad)
                || cantidad <= 0
                || cantidad > Integer.MAX_VALUE) {
            throw resultadoInconsistente();
        }

        return Optional.of(new PlatoDTO(
                nombre,
                cantidad.intValue(),
                unidad.getNombreComercial()));
    }

    private UnidadDeVenta obtenerUnidadAutorizada(String username) {
        if (username == null || username.isBlank()) {
            throw new PlatoEstrellaException("Se requiere un responsable autenticado.");
        }

        List<UnidadDeVenta> unidades = unidadDeVentaRepository.findByResponsableUsername(username);
        if (unidades == null || unidades.isEmpty()) {
            throw new PlatoEstrellaException(
                    "No se encontró una unidad de venta para el responsable autenticado.");
        }
        if (unidades.size() != 1) {
            throw new PlatoEstrellaException(
                    "La configuración de unidades del responsable autenticado es inconsistente.");
        }

        UnidadDeVenta unidad = unidades.get(0);
        User responsable = unidad == null ? null : unidad.getResponsable();
        if (responsable == null
                || !username.equals(responsable.getUsername())
                || !responsable.isEnabled()
                || responsable.getRole() == null
                || responsable.getRole().getRole() != RoleName.ROLE_RESPONSABLE) {
            throw new PlatoEstrellaException(
                    "El responsable autenticado no se encuentra habilitado o autorizado.");
        }
        return unidad;
    }

    private FestivalOpcion crearOpcion(Festival festival) {
        if (festival == null) {
            throw new PlatoEstrellaException(
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
            throw new PlatoEstrellaException(
                    "El festival seleccionado tiene un intervalo de fechas inválido.");
        }
    }

    private PlatoEstrellaException resultadoInconsistente() {
        return new PlatoEstrellaException(
                "No fue posible determinar el plato estrella con los datos disponibles.");
    }

    public record FestivalOpcion(
            int id,
            String nombre,
            LocalDate fechaInicio,
            LocalDate fechaFin) {
    }

    public record ContextoPlatoEstrella(
            String unidad,
            List<FestivalOpcion> festivales) {

        public ContextoPlatoEstrella {
            festivales = List.copyOf(festivales);
        }
    }
}
