package com.unla.epicentrogourmet.controllers;

import com.unla.epicentrogourmet.dto.CierreCajaDTO;
import com.unla.epicentrogourmet.exceptions.CierreCajaException;
import com.unla.epicentrogourmet.helpers.ViewRouteHelper;
import com.unla.epicentrogourmet.session.SeguimientoPedidosSesion;
import com.unla.epicentrogourmet.services.implementation.PedidoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequestMapping(ViewRouteHelper.ROUTE_CIERRE_CAJA)
public class CierreCajaController {

    static final String CIERRE_CONFIRMADO = "cierreConfirmado";
    static final String CIERRE_CONFIRMADO_OWNER = "cierreConfirmadoOwner";

    private final PedidoService pedidoService;
    private final SeguimientoPedidosSesion seguimientoPedidosSesion;

    public CierreCajaController(PedidoService pedidoService,
                                SeguimientoPedidosSesion seguimientoPedidosSesion) {
        this.pedidoService = pedidoService;
        this.seguimientoPedidosSesion = seguimientoPedidosSesion;
    }

    @GetMapping
    public String mostrar(Authentication authentication, HttpSession session, Model model) {
        String username = nombreUsuario(authentication);
        Object cierreConfirmado = model.getAttribute(CIERRE_CONFIRMADO);
        Object owner = model.getAttribute(CIERRE_CONFIRMADO_OWNER);
        boolean perteneceAlUsuario = cierreConfirmado instanceof CierreCajaDTO
                && username != null
                && username.equals(owner);

        model.asMap().remove(CIERRE_CONFIRMADO);
        model.asMap().remove(CIERRE_CONFIRMADO_OWNER);
        if (perteneceAlUsuario) {
            model.addAttribute("cierre", cierreConfirmado);
            model.addAttribute("confirmado", true);
            return ViewRouteHelper.CIERRE_CAJA;
        }

        try {
            CierreCajaDTO cierre;
            synchronized (session) {
                Set<Integer> pedidoIds = seguimientoPedidosSesion.obtenerIds(session, username);
                cierre = pedidoService.cierreCaja(username, pedidoIds);
            }
            model.addAttribute("cierre", cierre);
        } catch (CierreCajaException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return ViewRouteHelper.CIERRE_CAJA;
    }

    @PostMapping
    public String confirmar(Authentication authentication,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        String username = nombreUsuario(authentication);

        try {
            CierreCajaDTO cierre;
            synchronized (session) {
                Set<Integer> pedidoIds = seguimientoPedidosSesion.obtenerIds(session, username);
                if (pedidoIds.isEmpty()) {
                    redirectAttributes.addFlashAttribute(
                            "mensajeInformativo", "No hay pedidos pendientes para cerrar.");
                    return redireccionAlCierre();
                }

                cierre = pedidoService.cierreCaja(username, pedidoIds);
                if (cierre.cantidadPedidos() == 0) {
                    throw new CierreCajaException("No hay pedidos pendientes para cerrar.");
                }
                seguimientoPedidosSesion.limpiar(session, username);
            }

            redirectAttributes.addFlashAttribute(CIERRE_CONFIRMADO, cierre);
            redirectAttributes.addFlashAttribute(CIERRE_CONFIRMADO_OWNER, username);
        } catch (CierreCajaException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return redireccionAlCierre();
    }

    private String nombreUsuario(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }

    private String redireccionAlCierre() {
        return "redirect:" + ViewRouteHelper.ROUTE_CIERRE_CAJA;
    }
}
