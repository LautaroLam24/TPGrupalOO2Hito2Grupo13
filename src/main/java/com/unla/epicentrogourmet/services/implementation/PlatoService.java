package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.PlatoDTO;
import com.unla.epicentrogourmet.dto.PlatoForm;
import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.entities.Plato;
import com.unla.epicentrogourmet.entities.RoleName;
import com.unla.epicentrogourmet.entities.UnidadDeVenta;
import com.unla.epicentrogourmet.entities.User;
import com.unla.epicentrogourmet.exceptions.GestionPlatoException;
import com.unla.epicentrogourmet.exceptions.PlatoEstrellaException;
import com.unla.epicentrogourmet.exceptions.ReporteResponsableException;
import com.unla.epicentrogourmet.repositories.IPedidoRepository;
import com.unla.epicentrogourmet.repositories.IUnidadDeVentaRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PlatoService {

    private final ReporteResponsableService reporteResponsableService;
    private final IPedidoRepository pedidoRepository;
    private final IUnidadDeVentaRepository unidadDeVentaRepository;

    public PlatoService(ReporteResponsableService reporteResponsableService,
                        IPedidoRepository pedidoRepository,
                        IUnidadDeVentaRepository unidadDeVentaRepository) {
        this.reporteResponsableService = reporteResponsableService;
        this.pedidoRepository = pedidoRepository;
        this.unidadDeVentaRepository = unidadDeVentaRepository;
    }

    @Transactional(readOnly = true)
    public ContextoPlatoEstrella preparar(String username) {
        try {
            ReporteResponsableService.ContextoReportes contexto =
                    reporteResponsableService.preparar(username);
            List<FestivalOpcion> festivales = contexto.festivales().stream()
                    .map(festival -> new FestivalOpcion(
                            festival.id(),
                            festival.nombre(),
                            festival.fechaInicio(),
                            festival.fechaFin()))
                    .toList();
            return new ContextoPlatoEstrella(contexto.unidad(), festivales);
        } catch (ReporteResponsableException ex) {
            throw new PlatoEstrellaException(ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Optional<PlatoDTO> platoEstrella(String username, int festivalId) {
        ReporteResponsableService.ContextoFestivalAutorizado contexto;
        try {
            contexto = reporteResponsableService.autorizarFestival(username, festivalId);
        } catch (ReporteResponsableException ex) {
            throw new PlatoEstrellaException(ex.getMessage());
        }
        UnidadDeVenta unidad = contexto.unidad();
        Festival festival = contexto.festival();

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

    @Transactional(readOnly = true)
    public ContextoGestionPlatos prepararGestion(String username) {
        UnidadDeVenta unidad = obtenerUnidadAutorizada(username);
        List<PlatoResumen> platos = obtenerPlatos(unidad).stream()
                .map(this::crearResumen)
                .sorted(Comparator
                        .comparing(PlatoResumen::nombre, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingInt(PlatoResumen::id))
                .toList();
        return new ContextoGestionPlatos(unidad.getNombreComercial(), platos);
    }

    @Transactional(readOnly = true)
    public ContextoFormularioPlato prepararAlta(String username) {
        UnidadDeVenta unidad = obtenerUnidadAutorizada(username);
        return new ContextoFormularioPlato(
                unidad.getNombreComercial(), new PlatoForm(), false);
    }

    @Transactional(readOnly = true)
    public ContextoFormularioPlato prepararModificacion(String username, int platoId) {
        UnidadDeVenta unidad = obtenerUnidadAutorizada(username);
        Plato plato = obtenerPlatoPropio(unidad, platoId);
        validarActivoParaModificar(plato);

        PlatoForm form = new PlatoForm();
        form.setNombre(plato.getNombre());
        form.setPrecioVenta(plato.getPrecioVenta());
        form.setCostoProduccion(plato.getCostoProduccion());
        return new ContextoFormularioPlato(unidad.getNombreComercial(), form, true);
    }

    @Transactional
    public PlatoResumen alta(String username, PlatoForm form) {
        UnidadDeVenta unidad = obtenerUnidadAutorizada(username);
        DatosPlato datos = validarDatos(form);
        validarNombreDisponible(unidad, datos.nombre(), null);

        Plato plato = new Plato();
        plato.setNombre(datos.nombre());
        plato.setPrecioVenta(datos.precioVenta());
        plato.setCostoProduccion(datos.costoProduccion());
        plato.setBaja(false);
        obtenerPlatos(unidad).add(plato);
        guardarUnidad(unidad);
        return crearResumen(plato);
    }

    @Transactional
    public PlatoResumen modificar(String username, int platoId, PlatoForm form) {
        UnidadDeVenta unidad = obtenerUnidadAutorizada(username);
        Plato plato = obtenerPlatoPropio(unidad, platoId);
        validarActivoParaModificar(plato);
        DatosPlato datos = validarDatos(form);
        validarNombreDisponible(unidad, datos.nombre(), plato.getId());

        plato.setNombre(datos.nombre());
        plato.setPrecioVenta(datos.precioVenta());
        plato.setCostoProduccion(datos.costoProduccion());
        guardarUnidad(unidad);
        return crearResumen(plato);
    }

    @Transactional
    public void baja(String username, int platoId) {
        UnidadDeVenta unidad = obtenerUnidadAutorizada(username);
        Plato plato = obtenerPlatoPropio(unidad, platoId);
        if (plato.isBaja()) {
            throw new GestionPlatoException("El Plato ya se encuentra dado de baja.");
        }

        plato.setBaja(true);
        guardarUnidad(unidad);
    }

    private UnidadDeVenta obtenerUnidadAutorizada(String username) {
        if (username == null || username.isBlank()) {
            throw new GestionPlatoException("Se requiere un responsable autenticado.");
        }

        List<UnidadDeVenta> unidades = unidadDeVentaRepository.findByResponsableUsername(username);
        if (unidades == null || unidades.isEmpty()) {
            throw new GestionPlatoException(
                    "No se encontró una unidad de venta para el responsable autenticado.");
        }
        if (unidades.size() != 1) {
            throw new GestionPlatoException(
                    "La configuración de unidades del responsable autenticado es inconsistente.");
        }

        UnidadDeVenta unidad = unidades.get(0);
        User responsable = unidad == null ? null : unidad.getResponsable();
        if (responsable == null
                || !username.equals(responsable.getUsername())
                || !responsable.isEnabled()
                || responsable.getRole() == null
                || responsable.getRole().getRole() != RoleName.ROLE_RESPONSABLE) {
            throw new GestionPlatoException(
                    "El responsable autenticado no se encuentra habilitado o autorizado.");
        }
        return unidad;
    }

    private Set<Plato> obtenerPlatos(UnidadDeVenta unidad) {
        Set<Plato> platos = unidad.getPlatos();
        if (platos == null) {
            throw new GestionPlatoException(
                    "No fue posible acceder a los Platos de la unidad administrada.");
        }
        return platos;
    }

    private Plato obtenerPlatoPropio(UnidadDeVenta unidad, int platoId) {
        return obtenerPlatos(unidad).stream()
                .filter(plato -> plato != null && plato.getId() == platoId)
                .findFirst()
                .orElseThrow(() -> new GestionPlatoException(
                        "El Plato no existe o no pertenece a la unidad administrada."));
    }

    private void validarActivoParaModificar(Plato plato) {
        if (plato.isBaja()) {
            throw new GestionPlatoException("No se puede modificar un Plato dado de baja.");
        }
    }

    private DatosPlato validarDatos(PlatoForm form) {
        if (form == null) {
            throw new GestionPlatoException("Los datos del Plato son obligatorios.");
        }

        String nombre = form.getNombre() == null ? null : form.getNombre().trim();
        if (nombre == null || nombre.isEmpty()) {
            throw new GestionPlatoException("El nombre del Plato es obligatorio.");
        }
        if (nombre.length() > 100) {
            throw new GestionPlatoException(
                    "El nombre del Plato no puede superar los 100 caracteres.");
        }

        Float precioVenta = form.getPrecioVenta();
        if (precioVenta == null) {
            throw new GestionPlatoException("El precio de venta es obligatorio.");
        }
        if (!Float.isFinite(precioVenta)) {
            throw new GestionPlatoException("El precio de venta debe ser un valor finito.");
        }
        if (precioVenta <= 0) {
            throw new GestionPlatoException("El precio de venta debe ser mayor que cero.");
        }

        Float costoProduccion = form.getCostoProduccion();
        if (costoProduccion == null) {
            throw new GestionPlatoException("El costo de producción es obligatorio.");
        }
        if (!Float.isFinite(costoProduccion)) {
            throw new GestionPlatoException("El costo de producción debe ser un valor finito.");
        }
        if (costoProduccion < 0) {
            throw new GestionPlatoException(
                    "El costo de producción debe ser mayor o igual que cero.");
        }
        if (precioVenta <= costoProduccion) {
            throw new GestionPlatoException(
                    "El precio de venta debe ser mayor que el costo de producción.");
        }

        return new DatosPlato(nombre, precioVenta, costoProduccion);
    }

    private void validarNombreDisponible(UnidadDeVenta unidad,
                                         String nombre,
                                         Integer platoIdExcluido) {
        boolean duplicado = obtenerPlatos(unidad).stream()
                .filter(plato -> plato != null && !plato.isBaja())
                .filter(plato -> platoIdExcluido == null || plato.getId() != platoIdExcluido)
                .map(Plato::getNombre)
                .filter(nombreExistente -> nombreExistente != null)
                .map(String::trim)
                .anyMatch(nombreExistente -> nombreExistente.equalsIgnoreCase(nombre));
        if (duplicado) {
            throw new GestionPlatoException(
                    "Ya existe un Plato activo con ese nombre en la unidad administrada.");
        }
    }

    private PlatoResumen crearResumen(Plato plato) {
        if (plato == null || plato.getNombre() == null) {
            throw new GestionPlatoException(
                    "No fue posible preparar los Platos de la unidad administrada.");
        }
        return new PlatoResumen(
                plato.getId(),
                plato.getNombre(),
                plato.getPrecioVenta(),
                plato.getCostoProduccion(),
                !plato.isBaja());
    }

    private void guardarUnidad(UnidadDeVenta unidad) {
        try {
            unidadDeVentaRepository.saveAndFlush(unidad);
        } catch (DataAccessException ex) {
            throw new GestionPlatoException(
                    "No fue posible guardar los cambios del Plato. Intente nuevamente.", ex);
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

    public record PlatoResumen(
            int id,
            String nombre,
            float precioVenta,
            float costoProduccion,
            boolean activo) {
    }

    public record ContextoGestionPlatos(
            String unidad,
            List<PlatoResumen> platos) {

        public ContextoGestionPlatos {
            platos = List.copyOf(platos);
        }
    }

    public record ContextoFormularioPlato(
            String unidad,
            PlatoForm form,
            boolean edicion) {
    }

    private record DatosPlato(
            String nombre,
            float precioVenta,
            float costoProduccion) {
    }
}
