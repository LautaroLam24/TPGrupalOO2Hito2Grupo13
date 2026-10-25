package com.unla.epicentrogourmet.services.implementation;

import com.unla.epicentrogourmet.dto.AltaPedidoForm;
import com.unla.epicentrogourmet.dto.CierreCajaDTO;
import com.unla.epicentrogourmet.dto.ItemPedidoForm;
import com.unla.epicentrogourmet.dto.PedidoCierreDTO;
import com.unla.epicentrogourmet.entities.Cajero;
import com.unla.epicentrogourmet.entities.Festival;
import com.unla.epicentrogourmet.entities.ItemPedido;
import com.unla.epicentrogourmet.entities.Pedido;
import com.unla.epicentrogourmet.entities.Personal;
import com.unla.epicentrogourmet.entities.Plato;
import com.unla.epicentrogourmet.entities.RoleName;
import com.unla.epicentrogourmet.entities.UnidadDeVenta;
import com.unla.epicentrogourmet.exceptions.AltaPedidoException;
import com.unla.epicentrogourmet.exceptions.CierreCajaException;
import com.unla.epicentrogourmet.repositories.IFestivalRepository;
import com.unla.epicentrogourmet.repositories.IPedidoRepository;
import com.unla.epicentrogourmet.repositories.IPersonalRepository;
import com.unla.epicentrogourmet.repositories.IUnidadDeVentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
public class PedidoService {

    private final IPersonalRepository personalRepository;
    private final IUnidadDeVentaRepository unidadDeVentaRepository;
    private final IFestivalRepository festivalRepository;
    private final IPedidoRepository pedidoRepository;

    public PedidoService(IPersonalRepository personalRepository,
                         IUnidadDeVentaRepository unidadDeVentaRepository,
                         IFestivalRepository festivalRepository,
                         IPedidoRepository pedidoRepository) {
        this.personalRepository = personalRepository;
        this.unidadDeVentaRepository = unidadDeVentaRepository;
        this.festivalRepository = festivalRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public ContextoAltaPedido prepararAlta(String username) {
        ContextoAutorizado contexto = obtenerContextoAutorizado(username, LocalDate.now());
        List<Plato> platosActivos = contexto.unidad().getPlatos().stream()
                .filter(plato -> !plato.isBaja())
                .sorted(Comparator.comparing(Plato::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new ContextoAltaPedido(
                contexto.unidad().getNombreComercial(),
                contexto.festival().getNombre(),
                platosActivos);
    }

    @Transactional
    public Pedido registrar(String username, AltaPedidoForm form) {
        LocalDate fechaActual = LocalDate.now();
        ContextoAutorizado contexto = obtenerContextoAutorizado(username, fechaActual);
        UnidadDeVenta unidad = contexto.unidad();
        List<ItemPedidoForm> itemsSeleccionados = obtenerItemsSeleccionados(form);
        Map<Integer, Plato> platosDeLaUnidad = indexarPlatos(unidad);
        Set<Integer> platosIncluidos = new HashSet<>();

        Pedido pedido = new Pedido();
        pedido.setFecha(fechaActual);

        for (ItemPedidoForm itemForm : itemsSeleccionados) {
            validarItem(itemForm, platosIncluidos);

            Plato plato = platosDeLaUnidad.get(itemForm.getPlatoId());
            if (plato == null) {
                throw new AltaPedidoException("El plato seleccionado no existe o no pertenece a la unidad del cajero.");
            }
            if (plato.isBaja()) {
                throw new AltaPedidoException("El plato seleccionado no se encuentra activo.");
            }

            pedido.getItems().add(new ItemPedido(plato, itemForm.getCantidad()));
        }

        pedido.setTotal(pedido.calcularTotal());

        // Pedido contiene y persiste sus ítems por cascade. UnidadDeVenta es el lado
        // propietario de la asociación unidireccional y debe vincularse explícitamente.
        Pedido pedidoGuardado = pedidoRepository.saveAndFlush(pedido);
        unidad.getPedidos().add(pedidoGuardado);
        unidadDeVentaRepository.saveAndFlush(unidad);

        return pedidoGuardado;
    }

    @Transactional(readOnly = true)
    public CierreCajaDTO cierreCaja(String username, Collection<Integer> pedidoIds) {
        ContextoCajero contexto = obtenerContextoCajero(username, CierreCajaException::new);
        Set<Integer> idsUnicos = normalizarPedidoIds(pedidoIds);

        if (idsUnicos.isEmpty()) {
            return construirCierre(contexto, List.of());
        }

        List<Pedido> pedidos = pedidoRepository.findDeUnidadByIds(
                contexto.unidad().getId(), idsUnicos);
        validarPedidosRecuperados(idsUnicos, pedidos);

        List<Pedido> pedidosOrdenados = new ArrayList<>(pedidos);
        pedidosOrdenados.sort(Comparator.comparingInt(Pedido::getId));
        return construirCierre(contexto, pedidosOrdenados);
    }

    private ContextoAutorizado obtenerContextoAutorizado(String username, LocalDate fechaActual) {
        ContextoCajero contexto = obtenerContextoCajero(username, AltaPedidoException::new);
        Festival festival = obtenerFestivalActivo(contexto.unidad(), fechaActual);
        return new ContextoAutorizado(contexto.unidad(), festival);
    }

    private ContextoCajero obtenerContextoCajero(
            String username,
            Function<String, ? extends RuntimeException> crearExcepcion) {
        if (username == null || username.isBlank()) {
            throw crearExcepcion.apply("Se requiere un cajero autenticado.");
        }

        List<Personal> coincidencias = personalRepository.findByUserUsernameAndBajaFalse(username);
        if (coincidencias.size() != 1) {
            throw crearExcepcion.apply("No se encontró un empleado activo para el usuario autenticado.");
        }

        Personal personal = coincidencias.get(0);
        if (!(personal instanceof Cajero cajero)) {
            throw crearExcepcion.apply("El usuario autenticado no es un cajero.");
        }
        if (cajero.isBaja()
                || cajero.getUser() == null
                || !cajero.getUser().isEnabled()
                || cajero.getUser().getRole() == null
                || cajero.getUser().getRole().getRole() != RoleName.ROLE_EMPLEADO) {
            throw crearExcepcion.apply("El cajero autenticado no se encuentra activo o autorizado.");
        }

        UnidadDeVenta unidad = unidadDeVentaRepository.findByPersonal(cajero)
                .orElseThrow(() -> crearExcepcion.apply(
                        "El cajero autenticado no pertenece a una unidad de venta."));
        return new ContextoCajero(cajero, unidad);
    }

    private Festival obtenerFestivalActivo(UnidadDeVenta unidad, LocalDate fechaActual) {
        List<Festival> festivalesAsociados = festivalRepository.findByUnidadId(unidad.getId());
        if (festivalesAsociados.isEmpty()) {
            throw new AltaPedidoException("La unidad no tiene festivales asociados.");
        }

        List<Festival> festivalesActivos = festivalesAsociados.stream()
                .filter(festival -> festival.estaActivo(fechaActual))
                .toList();
        if (festivalesActivos.isEmpty()) {
            throw new AltaPedidoException("La unidad no tiene un festival activo en la fecha actual.");
        }
        if (festivalesActivos.size() > 1) {
            throw new AltaPedidoException("La unidad tiene varios festivales activos; no es posible determinar el contexto del pedido.");
        }
        return festivalesActivos.get(0);
    }

    private List<ItemPedidoForm> obtenerItemsSeleccionados(AltaPedidoForm form) {
        if (form == null || form.getItems() == null) {
            throw new AltaPedidoException("Los datos del pedido son obligatorios.");
        }

        List<ItemPedidoForm> seleccionados = form.getItems().stream()
                .filter(item -> item != null && item.getCantidad() != null)
                .toList();
        if (seleccionados.isEmpty()) {
            throw new AltaPedidoException("El pedido debe contener al menos un ítem.");
        }
        return seleccionados;
    }

    private void validarItem(ItemPedidoForm itemForm, Set<Integer> platosIncluidos) {
        if (itemForm.getPlatoId() == null) {
            throw new AltaPedidoException("El plato seleccionado es inválido.");
        }
        if (itemForm.getCantidad() <= 0) {
            throw new AltaPedidoException("La cantidad de cada plato debe ser mayor que cero.");
        }
        if (!platosIncluidos.add(itemForm.getPlatoId())) {
            throw new AltaPedidoException("No se puede incluir el mismo plato más de una vez.");
        }
    }

    private Map<Integer, Plato> indexarPlatos(UnidadDeVenta unidad) {
        Map<Integer, Plato> platos = new HashMap<>();
        for (Plato plato : unidad.getPlatos()) {
            platos.put(plato.getId(), plato);
        }
        return platos;
    }

    private Set<Integer> normalizarPedidoIds(Collection<Integer> pedidoIds) {
        if (pedidoIds == null) {
            throw new CierreCajaException("El seguimiento de pedidos para el cierre es inválido.");
        }

        Set<Integer> idsUnicos = new LinkedHashSet<>();
        for (Integer pedidoId : pedidoIds) {
            if (pedidoId == null || pedidoId <= 0) {
                throw new CierreCajaException("El seguimiento de pedidos para el cierre es inválido.");
            }
            idsUnicos.add(pedidoId);
        }
        return idsUnicos;
    }

    private void validarPedidosRecuperados(Set<Integer> idsEsperados, List<Pedido> pedidos) {
        if (pedidos == null || pedidos.size() != idsEsperados.size()) {
            throw pedidosInconsistentes();
        }

        Set<Integer> idsRecuperados = new HashSet<>();
        for (Pedido pedido : pedidos) {
            if (pedido == null || !idsEsperados.contains(pedido.getId())
                    || !idsRecuperados.add(pedido.getId())) {
                throw pedidosInconsistentes();
            }
        }
        if (!idsRecuperados.equals(idsEsperados)) {
            throw pedidosInconsistentes();
        }
    }

    private CierreCajaDTO construirCierre(ContextoCajero contexto, List<Pedido> pedidos) {
        List<PedidoCierreDTO> detalle = pedidos.stream()
                .map(pedido -> new PedidoCierreDTO(
                        pedido.getId(), pedido.getFecha(), pedido.getTotal()))
                .toList();
        float total = 0f;
        for (Pedido pedido : pedidos) {
            total += pedido.getTotal();
        }

        return new CierreCajaDTO(
                contexto.unidad().getNombreComercial(),
                nombreCompleto(contexto.cajero()),
                LocalDate.now(),
                detalle.size(),
                total,
                detalle);
    }

    private String nombreCompleto(Cajero cajero) {
        return (cajero.getNombre() + " " + cajero.getApellido()).trim();
    }

    private CierreCajaException pedidosInconsistentes() {
        return new CierreCajaException(
                "No fue posible validar los pedidos pendientes del cierre.");
    }

    public record ContextoAltaPedido(String unidad, String festival, List<Plato> platos) {
        public ContextoAltaPedido {
            platos = List.copyOf(platos);
        }
    }

    private record ContextoAutorizado(UnidadDeVenta unidad, Festival festival) {
    }

    private record ContextoCajero(Cajero cajero, UnidadDeVenta unidad) {
    }
}
