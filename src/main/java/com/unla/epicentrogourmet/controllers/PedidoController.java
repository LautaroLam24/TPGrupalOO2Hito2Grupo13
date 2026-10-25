package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.dto.AltaPedidoForm;
import com.unla.epicentrogourmet.dto.ItemPedidoForm;
import com.unla.epicentrogourmet.entities.Pedido;
import com.unla.epicentrogourmet.exceptions.AltaPedidoException;
import com.unla.epicentrogourmet.helpers.ViewRouteHelper;
import com.unla.epicentrogourmet.session.SeguimientoPedidosSesion;
import com.unla.epicentrogourmet.services.implementation.PedidoService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping(ViewRouteHelper.ROUTE_PEDIDO_ALTA)
public class PedidoController {

    static final String FORM_ATTRIBUTE = "altaPedidoForm";

    private final PedidoService pedidoService;
    private final SeguimientoPedidosSesion seguimientoPedidosSesion;

    public PedidoController(PedidoService pedidoService,
                            SeguimientoPedidosSesion seguimientoPedidosSesion) {
        this.pedidoService = pedidoService;
        this.seguimientoPedidosSesion = seguimientoPedidosSesion;
    }

    @GetMapping
    public String mostrarFormulario(Authentication authentication, Model model) {
        try {
            cargarContexto(authentication, model);
        } catch (AltaPedidoException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute(FORM_ATTRIBUTE, new AltaPedidoForm());
        }
        return ViewRouteHelper.PEDIDO_ALTA;
    }

    @PostMapping
    public String registrar(@Valid @ModelAttribute(FORM_ATTRIBUTE) AltaPedidoForm form,
                            BindingResult bindingResult,
                            Authentication authentication,
                            HttpSession session,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            cargarContextoSeguro(authentication, model);
            return ViewRouteHelper.PEDIDO_ALTA;
        }

        try {
            String username = nombreUsuario(authentication);
            Pedido pedido;
            synchronized (session) {
                pedido = pedidoService.registrar(username, form);
                seguimientoPedidosSesion.registrar(session, username, pedido.getId());
            }
            redirectAttributes.addFlashAttribute(
                    "mensajeExito",
                    "El pedido #" + pedido.getId() + " se registró correctamente. Total: $" + pedido.getTotal());
            return "redirect:" + ViewRouteHelper.ROUTE_PEDIDO_ALTA;
        } catch (AltaPedidoException ex) {
            bindingResult.reject("pedido.alta.error", ex.getMessage());
            cargarContextoSeguro(authentication, model);
            return ViewRouteHelper.PEDIDO_ALTA;
        }
    }

    private void cargarContexto(Authentication authentication, Model model) {
        PedidoService.ContextoAltaPedido contexto = pedidoService.prepararAlta(nombreUsuario(authentication));
        model.addAttribute("unidad", contexto.unidad());
        model.addAttribute("festival", contexto.festival());
        model.addAttribute("platos", contexto.platos());

        AltaPedidoForm form = (AltaPedidoForm) model.getAttribute(FORM_ATTRIBUTE);
        Map<Integer, Integer> cantidadesPorPlato = new HashMap<>();
        if (form != null && form.getItems() != null) {
            for (ItemPedidoForm item : form.getItems()) {
                if (item != null && item.getPlatoId() != null) {
                    cantidadesPorPlato.putIfAbsent(item.getPlatoId(), item.getCantidad());
                }
            }
        }

        AltaPedidoForm formularioSeguro = form == null ? new AltaPedidoForm() : form;
        formularioSeguro.setItems(contexto.platos().stream()
                .map(plato -> new ItemPedidoForm(
                        plato.getId(), cantidadesPorPlato.get(plato.getId())))
                .toList());
        model.addAttribute(FORM_ATTRIBUTE, formularioSeguro);
    }

    private void cargarContextoSeguro(Authentication authentication, Model model) {
        try {
            cargarContexto(authentication, model);
        } catch (AltaPedidoException ex) {
            model.addAttribute("error", ex.getMessage());
        }
    }

    private String nombreUsuario(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }
}
