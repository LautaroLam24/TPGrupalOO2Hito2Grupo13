package com.unla.epicentrogourmet.session;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class SeguimientoPedidosSesion {

    private static final String PREFIJO_ATRIBUTO =
            SeguimientoPedidosSesion.class.getName() + ".pedidos.";

    public void registrar(HttpSession session, String username, int pedidoId) {
        validarSessionYUsername(session, username);
        if (pedidoId <= 0) {
            throw new IllegalArgumentException("El ID del pedido debe ser positivo.");
        }

        synchronized (session) {
            LinkedHashSet<Integer> ids = copiarIds(session, clave(username));
            ids.add(pedidoId);
            session.setAttribute(clave(username), ids);
        }
    }

    public Set<Integer> obtenerIds(HttpSession session, String username) {
        validarSessionYUsername(session, username);
        synchronized (session) {
            return Collections.unmodifiableSet(copiarIds(session, clave(username)));
        }
    }

    public void limpiar(HttpSession session, String username) {
        validarSessionYUsername(session, username);
        synchronized (session) {
            session.removeAttribute(clave(username));
        }
    }

    private LinkedHashSet<Integer> copiarIds(HttpSession session, String clave) {
        Object atributo = session.getAttribute(clave);
        if (atributo == null) {
            return new LinkedHashSet<>();
        }
        if (!(atributo instanceof Set<?> valores)) {
            throw new IllegalStateException("El seguimiento de pedidos de la sesión es inválido.");
        }

        LinkedHashSet<Integer> copia = new LinkedHashSet<>();
        for (Object valor : valores) {
            if (!(valor instanceof Integer id)) {
                throw new IllegalStateException("El seguimiento de pedidos de la sesión es inválido.");
            }
            copia.add(id);
        }
        return copia;
    }

    private void validarSessionYUsername(HttpSession session, String username) {
        if (session == null) {
            throw new IllegalArgumentException("La sesión HTTP es obligatoria.");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El username autenticado es obligatorio.");
        }
    }

    private String clave(String username) {
        return PREFIJO_ATRIBUTO + username;
    }
}
